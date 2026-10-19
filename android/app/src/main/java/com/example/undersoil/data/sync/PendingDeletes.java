package com.example.undersoil.data.sync;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Garden plants removed on this phone that the server hasn't been told about yet.
// Kept so a sync can't restore a plant the farmer deleted, even if they were offline.
public final class PendingDeletes {

    private static final String PREF_NAME = "undersoil_pending_deletes";
    private static final String KEY_GARDEN_PREFIX = "garden_";

    private PendingDeletes() {
    }

    public static synchronized void addGardenPlant(Context context, String userId, int plantId) {
        Set<String> ids = new HashSet<>(read(context, userId));
        ids.add(String.valueOf(plantId));
        prefs(context).edit().putStringSet(KEY_GARDEN_PREFIX + userId, ids).apply();
    }

    // The farmer added the plant back, so it should no longer be deleted on the server
    public static synchronized void removeGardenPlant(Context context, String userId, int plantId) {
        Set<String> ids = new HashSet<>(read(context, userId));
        if (ids.remove(String.valueOf(plantId))) {
            prefs(context).edit().putStringSet(KEY_GARDEN_PREFIX + userId, ids).apply();
        }
    }

    public static synchronized List<Integer> getGardenPlants(Context context, String userId) {
        List<Integer> result = new ArrayList<>();
        for (String id : read(context, userId)) {
            try {
                result.add(Integer.parseInt(id));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    // Only clears the ids that were sent, in case more were deleted while the request ran
    public static synchronized void clearGardenPlants(Context context, String userId, List<Integer> sent) {
        Set<String> ids = new HashSet<>(read(context, userId));
        for (Integer id : sent) ids.remove(String.valueOf(id));
        prefs(context).edit().putStringSet(KEY_GARDEN_PREFIX + userId, ids).apply();
    }

    private static Set<String> read(Context context, String userId) {
        return prefs(context).getStringSet(KEY_GARDEN_PREFIX + userId, new HashSet<>());
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
