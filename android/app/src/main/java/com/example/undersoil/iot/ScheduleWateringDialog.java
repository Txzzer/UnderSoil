package com.example.undersoil.iot;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.undersoil.R;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.db.WateringScheduleDatabaseHelper;
import com.example.undersoil.data.model.WateringSchedule;
import com.example.undersoil.data.sync.SyncManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Lets the user pick a date, time and duration, then saves a Pending schedule offline and queues a sync. */
public class ScheduleWateringDialog {

    private static final int MIN_DURATION_MINUTES = 1;
    private static final int MAX_DURATION_MINUTES = 120;

    private final Activity activity;
    private final String sectorId;
    private final Calendar startTime = Calendar.getInstance();
    private boolean dateChosen = false;
    private boolean timeChosen = false;

    private ScheduleWateringDialog(Activity activity, String sectorId) {
        this.activity = activity;
        this.sectorId = sectorId;
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);
    }

    public static void show(Activity activity, String sectorId) {
        new ScheduleWateringDialog(activity, sectorId).show();
    }

    private void show() {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_schedule_watering, null);
        Button btnPickDate = view.findViewById(R.id.btnPickDate);
        Button btnPickTime = view.findViewById(R.id.btnPickTime);
        TextInputLayout tilDuration = view.findViewById(R.id.tilDuration);
        TextInputEditText etDuration = view.findViewById(R.id.etDuration);
        TextView tvExpectedVolume = view.findViewById(R.id.tvExpectedVolume);

        btnPickDate.setOnClickListener(v -> new DatePickerDialog(activity, (picker, year, month, day) -> {
            startTime.set(year, month, day);
            dateChosen = true;
            btnPickDate.setText(new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(startTime.getTime()));
        }, startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH), startTime.get(Calendar.DAY_OF_MONTH)).show());

        btnPickTime.setOnClickListener(v -> new TimePickerDialog(activity, (picker, hour, minute) -> {
            startTime.set(Calendar.HOUR_OF_DAY, hour);
            startTime.set(Calendar.MINUTE, minute);
            timeChosen = true;
            btnPickTime.setText(DateFormat.getTimeFormat(activity).format(startTime.getTime()));
        }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(activity)).show());

        etDuration.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilDuration.setError(null);
                Integer minutes = parseDuration(s.toString());
                tvExpectedVolume.setText(minutes == null
                        ? "Expected volume: -"
                        : String.format(Locale.US, "Expected volume: %.1f L",
                        minutes * WateringSchedule.FLOW_RATE_LITERS_PER_MIN));
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Schedule Watering")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        // Override the positive button so invalid input keeps the dialog open
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!dateChosen || !timeChosen) {
                Toast.makeText(activity, "Please select a date and time", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startTime.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(activity, "Start time must be in the future", Toast.LENGTH_SHORT).show();
                return;
            }
            String durationText = etDuration.getText() != null ? etDuration.getText().toString() : "";
            Integer minutes = parseDuration(durationText);
            if (minutes == null) {
                tilDuration.setError(String.format(Locale.US, "Enter %d–%d minutes",
                        MIN_DURATION_MINUTES, MAX_DURATION_MINUTES));
                return;
            }

            if (saveSchedule(minutes)) {
                Toast.makeText(activity, "Watering scheduled", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(activity, "Failed to save schedule", Toast.LENGTH_SHORT).show();
            }
        }));

        dialog.show();
    }

    private boolean saveSchedule(int durationMinutes) {
        String userId = SessionManager.getInstance(activity).getUserIdString();
        // Calendar millis are already UTC epoch time, regardless of the device time zone
        WateringSchedule schedule = new WateringSchedule(userId, sectorId, startTime.getTimeInMillis(), durationMinutes);

        boolean saved = WateringScheduleDatabaseHelper.getInstance(activity).insertSchedule(schedule);
        if (saved) {
            SyncManager.scheduleSync(activity);
        }
        return saved;
    }

    private Integer parseDuration(String text) {
        try {
            int minutes = Integer.parseInt(text.trim());
            return (minutes >= MIN_DURATION_MINUTES && minutes <= MAX_DURATION_MINUTES) ? minutes : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
