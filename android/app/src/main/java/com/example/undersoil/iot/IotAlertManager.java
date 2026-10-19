package com.example.undersoil.iot;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.undersoil.SplashActivity;
import com.example.undersoil.auth.LoginActivity;
import com.example.undersoil.auth.SignUpActivity;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.db.IotAlertDatabaseHelper;
import com.example.undersoil.data.model.IotAlert;
import com.example.undersoil.data.sync.SyncManager;
import com.example.undersoil.network.ApiClient;
import com.example.undersoil.network.ApiService;
import com.example.undersoil.network.SensorData;
import com.example.undersoil.network.ValveRequest;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * App-wide IoT telemetry poller. Started/stopped by {@link UnderSoilApp}'s lifecycle callbacks
 * while any Activity is visible, and shows the unintended-flow dialog on the foreground Activity.
 */
public class IotAlertManager {

    private static final String TAG = "IotAlertManager";

    private static final long POLLING_INTERVAL_MS = 3000;
    private static final long MAX_BACKOFF_MS = 30_000;
    private static final long SNOOZE_MS = 30_000;
    private static final int FAILURE_NOTIFY_THRESHOLD = 3;
    private static final double FLOW_THRESHOLD = 0.05;

    private static final String ALERT_CHANNEL_ID = "iot_flow_alerts";
    private static final String ALERT_CHANNEL_NAME = "IoT Flow Alerts";
    private static final int ALERT_NOTIFICATION_ID = 2001;

    public interface Listener {
        void onTelemetry(SensorData data);

        /** The backend is reachable but the device has stopped reporting; {@code lastKnown} is its last reading. */
        void onDeviceOffline(SensorData lastKnown);

        void onPollError(String message);

        void onValveToggleResult(boolean success, boolean targetState, String message);
    }

    /** Called on the main thread after a new alert has been written to the local database. */
    public interface AlertSavedListener {
        void onAlertSaved();
    }

    private static IotAlertManager instance;

    private final Context appContext;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final List<AlertSavedListener> alertSavedListeners = new CopyOnWriteArrayList<>();

    // DEBUG-TRIGGER: null = real telemetry; true/false = fake high/normal flow (debug builds only).
    // Remove everything tagged DEBUG-TRIGGER (plus the Gson/ApplicationInfo imports) to drop it.
    private Boolean debugFakeFlow = null;

    private WeakReference<Activity> currentActivity = new WeakReference<>(null);
    private AlertDialog activeDialog;

    private boolean isPolling = false;
    private boolean isTogglingValve = false;
    private boolean alertActive = false;
    private long snoozedUntilMs = 0;
    private int consecutiveFailures = 0;
    private boolean failureNotified = false;
    // Posted once per alert episode (and again after each snooze expires), not on every poll
    private boolean alertNotificationPosted = false;
    private SensorData lastData;

    private final Runnable pollRunnable = this::poll;

    private IotAlertManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new IotAlertManager(context);
        }
    }

    public static IotAlertManager get() {
        if (instance == null) {
            throw new IllegalStateException("IotAlertManager not initialized. Is UnderSoilApp registered in the manifest?");
        }
        return instance;
    }

    public void startPolling() {
        if (isPolling) return;
        isPolling = true;
        Log.d(TAG, "Polling started");
        handler.removeCallbacks(pollRunnable);
        handler.post(pollRunnable);
    }

    public void stopPolling() {
        if (!isPolling) return;
        isPolling = false;
        Log.d(TAG, "Polling stopped");
        handler.removeCallbacks(pollRunnable);
    }

    public void setCurrentActivity(Activity activity) {
        currentActivity = new WeakReference<>(activity);
        // Re-surface an alert that was dismissed when the previous Activity paused
        if (alertActive && System.currentTimeMillis() > snoozedUntilMs) {
            showAlertDialog();
        }
    }

    public void clearCurrentActivity(Activity activity) {
        if (currentActivity.get() == activity) {
            dismissDialog();
            currentActivity = new WeakReference<>(null);
        }
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void addAlertSavedListener(AlertSavedListener listener) {
        if (!alertSavedListeners.contains(listener)) alertSavedListeners.add(listener);
    }

    public void removeAlertSavedListener(AlertSavedListener listener) {
        alertSavedListeners.remove(listener);
    }

    // DEBUG-TRIGGER: flips fake flow between high and normal. No-op in release builds.
    public boolean toggleDebugFakeFlow() {
        if ((appContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return false;
        debugFakeFlow = (debugFakeFlow == null) || !debugFakeFlow;
        Log.d(TAG, "Debug fake flow: " + (debugFakeFlow ? "HIGH" : "NORMAL"));
        return debugFakeFlow;
    }

    public SensorData getLastData() {
        return lastData;
    }

    public boolean isTogglingValve() {
        return isTogglingValve;
    }

    public static boolean hasFlow(SensorData data) {
        return data.getFlowRateLitersPerMin() > FLOW_THRESHOLD;
    }

    /** Water is moving while the valve reports closed: a leak, a stuck valve or a faulty sensor. */
    public static boolean isUnintendedFlow(SensorData data) {
        return hasFlow(data) && !data.isValveIsOpen();
    }

    private void poll() {
        if (!isPolling) return;

        String token = SessionManager.getInstance(appContext).getToken();
        if (token == null || token.trim().isEmpty()) {
            // Not logged in: skip the request so ApiClient's 401 handler doesn't loop to LoginActivity
            scheduleNext(POLLING_INTERVAL_MS);
            return;
        }

        // DEBUG-TRIGGER: feed fake telemetry instead of calling the backend
        if (debugFakeFlow != null) {
            String json = String.format(Locale.US,
                    "{\"deviceId\":\"debug-device\",\"flowRateLitersPerMin\":%.1f,\"valveIsOpen\":false}",
                    debugFakeFlow ? 5.0 : 0.0);
            lastData = new Gson().fromJson(json, SensorData.class);
            for (Listener l : listeners) l.onTelemetry(lastData);
            evaluateAlert(lastData);
            scheduleNext(POLLING_INTERVAL_MS);
            return;
        }

        ApiService api = ApiClient.getClient(appContext).create(ApiService.class);
        api.getLatestSensorData().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<SensorData> call, Response<SensorData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    consecutiveFailures = 0;
                    failureNotified = false;
                    lastData = response.body();
                    if (lastData.isOnline()) {
                        for (Listener l : listeners) l.onTelemetry(lastData);
                        evaluateAlert(lastData);
                    } else {
                        // Stale reading: don't show it as live or keep alerting on it
                        for (Listener l : listeners) l.onDeviceOffline(lastData);
                        clearAlert();
                    }
                    scheduleNext(POLLING_INTERVAL_MS);
                } else {
                    Log.w(TAG, "Telemetry poll HTTP " + response.code());
                    handlePollFailure(String.format(Locale.US, "Error (%d)", response.code()));
                }
            }

            @Override
            public void onFailure(Call<SensorData> call, Throwable t) {
                Log.e(TAG, "Telemetry poll failed", t);
                handlePollFailure("Offline");
            }
        });
    }

    private void handlePollFailure(String message) {
        consecutiveFailures++;
        for (Listener l : listeners) l.onPollError(message);

        if (consecutiveFailures >= FAILURE_NOTIFY_THRESHOLD && !failureNotified) {
            failureNotified = true;
            Activity activity = currentActivity.get();
            if (isUsable(activity)) {
                Toast.makeText(activity, "IoT device unreachable — retrying", Toast.LENGTH_SHORT).show();
            }
        }

        long backoff = POLLING_INTERVAL_MS << Math.min(consecutiveFailures, 4);
        scheduleNext(Math.min(backoff, MAX_BACKOFF_MS));
    }

    private void scheduleNext(long delayMs) {
        if (!isPolling) return;
        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, delayMs);
    }

    private void evaluateAlert(SensorData data) {
        if (isUnintendedFlow(data)) {
            if (!alertActive) {
                alertActive = true;
                persistAlert(data);
            }
            if (System.currentTimeMillis() > snoozedUntilMs) {
                showAlertNotification(data);
                showAlertDialog();
            }
        } else {
            clearAlert();
        }
    }

    private void clearAlert() {
        alertActive = false;
        snoozedUntilMs = 0;
        dismissDialog();
        cancelAlertNotification();
    }

    private void snooze() {
        snoozedUntilMs = System.currentTimeMillis() + SNOOZE_MS;
        alertNotificationPosted = false;
    }

    private void persistAlert(SensorData data) {
        try {
            String userId = SessionManager.getInstance(appContext).getUserIdString();
            String desc = String.format(Locale.US,
                    "Unintended flow of %.1f L/min while valve CLOSED", data.getFlowRateLitersPerMin());
            String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

            IotAlert alert = new IotAlert(data.getDeviceId(), userId, desc, dateTime);
            String savedId = IotAlertDatabaseHelper.getInstance(appContext).insertAlert(alert, userId);
            SyncManager.scheduleSync(appContext);
            if (savedId != null) {
                for (AlertSavedListener l : alertSavedListeners) l.onAlertSaved();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to persist IoT alert", e);
        }
    }

    private boolean isUsable(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private boolean isExcluded(Activity activity) {
        return activity instanceof SplashActivity
                || activity instanceof LoginActivity
                || activity instanceof SignUpActivity;
    }

    private void showAlertDialog() {
        if (activeDialog != null && activeDialog.isShowing()) return;

        Activity activity = currentActivity.get();
        if (!isUsable(activity) || isExcluded(activity) || lastData == null) return;

        try {
            Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle("🚨 Warning: Unintended Flow Detected!");
        builder.setMessage(String.format(Locale.US,
                "The sensor is detecting active fluid flow of %.1f L/min, but the valve is reported CLOSED.\n\nThis may indicate a pipe leak, stuck valve actuator, or sensor mismatch.",
                lastData.getFlowRateLitersPerMin()));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(false);

        builder.setPositiveButton("Open Valve Now", (dialog, which) -> toggleValve(true));

        builder.setNegativeButton("Acknowledge (Snooze 30s)", (dialog, which) -> {
            snooze();
            dialog.dismiss();
        });

        builder.setNeutralButton("Open IoT Dashboard", (dialog, which) -> {
            // Snooze so the dialog doesn't immediately re-appear over the dashboard
            snooze();
            dialog.dismiss();
            activity.startActivity(buildDashboardIntent(activity));
        });

        activeDialog = builder.create();
        activeDialog.show();
    }

    private void dismissDialog() {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }
        activeDialog = null;
    }

    private Intent buildDashboardIntent(Context context) {
        Intent intent = new Intent(context, IotScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    private void showAlertNotification(SensorData data) {
        if (alertNotificationPosted) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; skipping alert notification");
            return;
        }

        createAlertChannel();

        Intent intent = buildDashboardIntent(appContext);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = String.format(Locale.US,
                "%.1f L/min detected while the valve is CLOSED. Tap to open the IoT dashboard.",
                data.getFlowRateLitersPerMin());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Unintended Flow Detected")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(appContext).notify(ALERT_NOTIFICATION_ID, builder.build());
            alertNotificationPosted = true;
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to post alert notification", e);
        }
    }

    private void cancelAlertNotification() {
        alertNotificationPosted = false;
        NotificationManagerCompat.from(appContext).cancel(ALERT_NOTIFICATION_ID);
    }

    private void createAlertChannel() {
        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(ALERT_CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Warnings about unintended water flow while the valve is closed");
        manager.createNotificationChannel(channel);
    }

    public void toggleValve(boolean targetState) {
        if (isTogglingValve) return;
        isTogglingValve = true;

        ApiService api = ApiClient.getClient(appContext).create(ApiService.class);
        api.toggleValve(new ValveRequest(targetState)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isTogglingValve = false;
                if (response.isSuccessful()) {
                    dismissDialog();
                    String state = targetState ? "OPEN" : "CLOSED";
                    notifyToggle(true, targetState, "Valve toggled to " + state);
                } else {
                    Log.w(TAG, "Valve toggle HTTP " + response.code());
                    notifyToggle(false, targetState, "Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isTogglingValve = false;
                Log.e(TAG, "Valve toggle failed", t);
                notifyToggle(false, targetState, "Network Error: " + t.getMessage());
            }
        });
    }

    private void notifyToggle(boolean success, boolean targetState, String message) {
        if (listeners.isEmpty()) {
            // Toggled from the global dialog on a screen with no listener: tell the user directly
            Activity activity = currentActivity.get();
            if (isUsable(activity)) {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        }
        for (Listener l : listeners) l.onValveToggleResult(success, targetState, message);
    }
}
