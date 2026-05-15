package com.example.bai3.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.bai3.utils.Constants;

/**
 * Singleton wrapper around SharedPreferences for app-wide settings.
 * Provides typed getters/setters for all preference keys.
 */
public class AppPreferences {

    private static volatile AppPreferences instance;
    private final SharedPreferences prefs;

    private AppPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get the singleton instance. Must be called with Application context first.
     */
    public static AppPreferences getInstance(Context context) {
        if (instance == null) {
            synchronized (AppPreferences.class) {
                if (instance == null) {
                    instance = new AppPreferences(context);
                }
            }
        }
        return instance;
    }

    // ── Onboarding ───────────────────────────────────────────

    public boolean isOnboardingComplete() {
        return prefs.getBoolean(Constants.PREF_ONBOARDING_COMPLETE, false);
    }

    public void setOnboardingComplete(boolean complete) {
        prefs.edit().putBoolean(Constants.PREF_ONBOARDING_COMPLETE, complete).apply();
    }

    // ── Last Device ──────────────────────────────────────────

    public int getLastDeviceId() {
        return prefs.getInt(Constants.PREF_LAST_DEVICE_ID, -1);
    }

    public void setLastDeviceId(int deviceId) {
        prefs.edit().putInt(Constants.PREF_LAST_DEVICE_ID, deviceId).apply();
    }

    // ── Haptic Feedback ──────────────────────────────────────

    public boolean isHapticEnabled() {
        return prefs.getBoolean(Constants.PREF_HAPTIC_ENABLED, true);
    }

    public void setHapticEnabled(boolean enabled) {
        prefs.edit().putBoolean(Constants.PREF_HAPTIC_ENABLED, enabled).apply();
    }

    // ── Compact Layout ───────────────────────────────────────

    public boolean isCompactLayout() {
        return prefs.getBoolean(Constants.PREF_COMPACT_LAYOUT, false);
    }

    public void setCompactLayout(boolean compact) {
        prefs.edit().putBoolean(Constants.PREF_COMPACT_LAYOUT, compact).apply();
    }

    // ── Theme Mode ───────────────────────────────────────────

    /** @return "auto", "dark", or "light" */
    public String getThemeMode() {
        return prefs.getString(Constants.PREF_THEME_MODE, "dark");
    }

    public void setThemeMode(String mode) {
        prefs.edit().putString(Constants.PREF_THEME_MODE, mode).apply();
    }

    // ── Disconnect Alert ─────────────────────────────────────

    public boolean isDisconnectAlertEnabled() {
        return prefs.getBoolean(Constants.PREF_DISCONNECT_ALERT, true);
    }

    public void setDisconnectAlertEnabled(boolean enabled) {
        prefs.edit().putBoolean(Constants.PREF_DISCONNECT_ALERT, enabled).apply();
    }

    // ── Client Key Storage (per IP) ──────────────────────────

    /**
     * Store the WebOS client key for a specific TV IP address.
     */
    public void saveClientKey(String ipAddress, String clientKey) {
        prefs.edit().putString("client_key_" + ipAddress, clientKey).apply();
    }

    /**
     * Retrieve the stored WebOS client key for a specific TV IP address.
     */
    public String getClientKey(String ipAddress) {
        return prefs.getString("client_key_" + ipAddress, null);
    }

    /**
     * Remove the stored client key for a specific TV IP address.
     */
    public void removeClientKey(String ipAddress) {
        prefs.edit().remove("client_key_" + ipAddress).apply();
    }
}
