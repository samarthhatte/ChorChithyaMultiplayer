package com.agpitcodeclub.chorchithyamultiplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class ThemeUtils {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";

    public static final int THEME_DEFAULT = 0;
    public static final int THEME_BLUE = 1;
    public static final int THEME_PURPLE = 2;
    public static final int THEME_TEAL = 3;

    public static void applyTheme(Activity activity) {
        int theme = getSelectedTheme(activity);
        switch (theme) {
            case THEME_BLUE:
                activity.setTheme(R.style.Theme_ChorChithyaMultiplayer_Blue);
                break;
            case THEME_PURPLE:
                activity.setTheme(R.style.Theme_ChorChithyaMultiplayer_Purple);
                break;
            case THEME_TEAL:
                activity.setTheme(R.style.Theme_ChorChithyaMultiplayer_Teal);
                break;
            default:
                activity.setTheme(R.style.Theme_ChorChithyaMultiplayer);
                break;
        }
    }

    public static void setSelectedTheme(Context context, int theme) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_THEME, theme);
        editor.apply();
    }

    public static int getSelectedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_DEFAULT);
    }
}
