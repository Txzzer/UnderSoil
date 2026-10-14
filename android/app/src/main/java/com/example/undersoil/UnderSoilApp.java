package com.example.undersoil;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import network.ApiClient;

public class UnderSoilApp extends Application {

    private static final String TAG = "UnderSoilApp";

    private int startedActivityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application created");

        ApiClient.getClient(this);
        IoTAlertManager.init(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                startedActivityCount++;
                if (startedActivityCount == 1) {
                    IoTAlertManager.get().startPolling();
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                startedActivityCount--;
                if (startedActivityCount <= 0) {
                    startedActivityCount = 0;
                    IoTAlertManager.get().stopPolling();
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                IoTAlertManager.get().setCurrentActivity(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                IoTAlertManager.get().clearCurrentActivity(activity);
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }
}
