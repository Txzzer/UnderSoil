package com.example.undersoil.data.sync;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class SyncManager {

    public static final String SYNC_WORK_NAME = "undersoil_one_time_sync";
    public static final String SYNC_WORK_TAG = "undersoil_sync_tag";

    /** Queues a sync that runs as soon as the device is online, replacing any sync already waiting. */
    public static void scheduleSync(Context context) {
        if (context == null) return;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .addTag(SYNC_WORK_TAG)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
        );
    }
}
