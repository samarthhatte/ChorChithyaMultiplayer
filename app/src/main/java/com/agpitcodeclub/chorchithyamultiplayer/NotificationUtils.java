package com.agpitcodeclub.chorchithyamultiplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationUtils {
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    public static boolean isNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }
}