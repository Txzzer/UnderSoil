package com.example.undersoil.weather;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

// Coarse location via the platform LocationManager (no Play Services needed).
// Callers must hold ACCESS_COARSE_LOCATION before calling getLocation.
public final class LocationHelper {

    private LocationHelper() {
    }

    // Delivers the current location on the main thread, or null if none could be found.
    @SuppressWarnings("MissingPermission")
    public static void getLocation(Context context, Consumer<Location> callback) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            callback.accept(null);
            return;
        }

        String provider = null;
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        }

        if (provider == null) {
            callback.accept(lastKnownLocation(lm));
            return;
        }

        Executor mainExecutor = ContextCompat.getMainExecutor(context);
        try {
            lm.getCurrentLocation(provider, new CancellationSignal(), mainExecutor, location ->
                    callback.accept(location != null ? location : lastKnownLocation(lm)));
        } catch (SecurityException | IllegalArgumentException e) {
            // e.g. GPS provider with only coarse permission on Android 11
            callback.accept(lastKnownLocation(lm));
        }
    }

    @Nullable
    @SuppressWarnings("MissingPermission")
    private static Location lastKnownLocation(LocationManager lm) {
        for (String provider : new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER}) {
            try {
                Location location = lm.getLastKnownLocation(provider);
                if (location != null) return location;
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    // Blocking network/disk call: run off the main thread. Falls back to rounded coordinates.
    @SuppressWarnings("deprecation")
    public static String placeName(Context context, double latitude, double longitude) {
        if (Geocoder.isPresent()) {
            try {
                List<Address> results = new Geocoder(context, Locale.getDefault())
                        .getFromLocation(latitude, longitude, 1);
                if (results != null && !results.isEmpty()) {
                    String name = formatAddress(results.get(0));
                    if (name != null) return name;
                }
            } catch (Exception ignored) {
            }
        }
        return String.format(Locale.US, "%.2f, %.2f", latitude, longitude);
    }

    // "Suburb, Town" when available, e.g. "Summerstrand, Gqeberha"
    @Nullable
    private static String formatAddress(Address address) {
        String area = firstNonEmpty(address.getSubLocality(), address.getLocality());
        String town = firstNonEmpty(address.getLocality(), address.getSubAdminArea(), address.getAdminArea());

        if (area != null && town != null && !area.equals(town)) return area + ", " + town;
        if (area != null) return area;
        return town;
    }

    @Nullable
    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }
}
