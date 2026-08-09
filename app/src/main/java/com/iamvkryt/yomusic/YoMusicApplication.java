package com.iamvkryt.yomusic;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class YoMusicApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        SharedPreferences themePrefs = getSharedPreferences("YoMusicPrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("is_dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
