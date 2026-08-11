package com.iamvkryt.yomusic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AppUpdateManager {
    private static final String TAG = "AppUpdateManager";
    private static final String PREFS_NAME = "YoMusicPrefs";
    private static final String KEY_UPDATE_AVAILABLE = "update_available";
    private static final String KEY_UPDATE_TAG_NAME = "update_tag_name";
    private static final String KEY_UPDATE_CHANGELOG = "update_changelog";
    private static final String KEY_UPDATE_URL = "update_url";

    public interface UpdateCheckCallback {
        void onUpdateStatusChecked(boolean updateAvailable, String tagName, String changelog, String releaseUrl);
        void onError(String message);
    }

    public static void checkForUpdates(Context context, boolean forceCheck, UpdateCheckCallback callback) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/iamvkr047/yomusic/releases/latest")
                        .header("User-Agent", "YoMusic-Android")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        if (callback != null) {
                            callback.onError("HTTP error code: " + response.code());
                        }
                        return;
                    }

                    String body = response.body().string();
                    JSONObject data = new JSONObject(body);
                    String tagName = data.optString("tag_name", "");
                    String releaseUrl = data.optString("html_url", "");
                    String changeLog = data.optString("body", "");

                    String currentVersion = getCurrentAppVersionName(context);
                    boolean isUpdateAvailable = !tagName.isEmpty() && compareVersions(currentVersion, tagName) < 0;

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(KEY_UPDATE_AVAILABLE, isUpdateAvailable);
                    if (isUpdateAvailable) {
                        editor.putString(KEY_UPDATE_TAG_NAME, tagName);
                        editor.putString(KEY_UPDATE_CHANGELOG, changeLog);
                        editor.putString(KEY_UPDATE_URL, releaseUrl);
                    } else {
                        editor.remove(KEY_UPDATE_TAG_NAME);
                        editor.remove(KEY_UPDATE_CHANGELOG);
                        editor.remove(KEY_UPDATE_URL);
                    }
                    editor.apply();

                    // Notify toolbar on MainActivity if it exists
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        MainActivity mainActivity = MainActivity.getInstance();
                        if (mainActivity != null) {
                            mainActivity.updateUpdateBadgeVisibility();
                        }
                        if (callback != null) {
                            callback.onUpdateStatusChecked(isUpdateAvailable, tagName, changeLog, releaseUrl);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Update check failed", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
            }
        }).start();
    }

    public static boolean isUpdateAvailable(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean updateAvailable = prefs.getBoolean(KEY_UPDATE_AVAILABLE, false);
        if (updateAvailable) {
            String cachedTag = prefs.getString(KEY_UPDATE_TAG_NAME, "");
            String currentVersion = getCurrentAppVersionName(context);
            if (!cachedTag.isEmpty() && compareVersions(currentVersion, cachedTag) >= 0) {
                // Cache is stale (app was updated). Clear update cache.
                prefs.edit()
                        .putBoolean(KEY_UPDATE_AVAILABLE, false)
                        .remove(KEY_UPDATE_TAG_NAME)
                        .remove(KEY_UPDATE_CHANGELOG)
                        .remove(KEY_UPDATE_URL)
                        .apply();
                return false;
            }
        }
        return updateAvailable;
    }

    public static void showUpdateDialog(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String tagName = prefs.getString(KEY_UPDATE_TAG_NAME, "");
        String changeLog = prefs.getString(KEY_UPDATE_CHANGELOG, "");
        String releaseUrl = prefs.getString(KEY_UPDATE_URL, "");

        if (tagName.isEmpty()) return;

        showUpdateAvailableDialog(context, tagName, changeLog, releaseUrl);
    }

    public static void showUpdateAvailableDialog(Context context, String tagName, String changeLog, String releaseUrl) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Update Available")
                .setMessage("A new version (" + tagName + ") is available.\n\nChangelog:\n" + changeLog)
                .setPositiveButton("Download Update", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }

    public static String getCurrentAppVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    public static int compareVersions(String version1, String version2) {
        String v1 = version1.replaceAll("(?i)^v", "").trim();
        String v2 = version2.replaceAll("(?i)^v", "").trim();

        String[] vals1 = v1.split("\\.");
        String[] vals2 = v2.split("\\.");
        int i = 0;
        while (i < vals1.length || i < vals2.length) {
            int num1 = i < vals1.length ? Integer.parseInt(vals1[i].replaceAll("[^0-9]", "")) : 0;
            int num2 = i < vals2.length ? Integer.parseInt(vals2[i].replaceAll("[^0-9]", "")) : 0;

            if (num1 < num2) {
                return -1;
            } else if (num1 > num2) {
                return 1;
            }
            i++;
        }
        return 0;
    }
}
