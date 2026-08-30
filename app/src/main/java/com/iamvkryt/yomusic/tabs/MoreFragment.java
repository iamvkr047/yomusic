package com.iamvkryt.yomusic.tabs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iamvkryt.yomusic.AppUpdateManager;
import com.iamvkryt.yomusic.MainActivity;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.models.Playlist;
import com.iamvkryt.yomusic.models.VideoItem;
import com.iamvkryt.yomusic.storage.FavoriteStorage;
import com.iamvkryt.yomusic.storage.PlaylistStorage;
import com.iamvkryt.yomusic.storage.PlaylistSessionCache;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MoreFragment extends Fragment {

    private static final String PREFS_NAME = "YoMusicPrefs";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_CACHED_HOME_FEED = "cachedHomeFeed";
    private static final String KEY_EXPIRES_IN = "ExpiresIn";

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            uri -> {
                if (uri != null) {
                    exportDataToUri(uri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    importDataFromUri(uri);
                }
            }
    );

    private static final String[] COUNTRY_NAMES = {
        "Algeria", "Argentina", "Australia", "Austria", "Azerbaijan", "Bahrain", "Bangladesh",
        "Belarus", "Belgium", "Bolivia", "Bosnia and Herzegovina", "Brazil", "Bulgaria", "Cambodia",
        "Canada", "Chile", "Colombia", "Costa Rica", "Croatia", "Cyprus", "Czechia", "Denmark",
        "Dominican Republic", "Ecuador", "Egypt", "El Salvador", "Estonia", "Finland", "France",
        "Georgia", "Germany", "Ghana", "Greece", "Guatemala", "Honduras", "Hong Kong", "Hungary",
        "Iceland", "India", "Indonesia", "Iraq", "Ireland", "Israel", "Italy", "Japan", "Jamaica",
        "Jordan", "Kazakhstan", "Kenya", "Kuwait", "Laos", "Latvia", "Lebanon", "Libya", "Liechtenstein",
        "Lithuania", "Luxembourg", "North Macedonia", "Malaysia", "Malta", "Mexico", "Moldova",
        "Montenegro", "Morocco", "Nepal", "Netherlands", "New Zealand", "Nicaragua", "Nigeria",
        "Norway", "Oman", "Pakistan", "Panama", "Paraguay", "Peru", "Philippines", "Poland",
        "Portugal", "Puerto Rico", "Qatar", "Romania", "Saudi Arabia", "Senegal", "Serbia",
        "Singapore", "Slovakia", "Slovenia", "South Africa", "South Korea", "Spain", "Sri Lanka",
        "Sweden", "Switzerland", "Taiwan", "Tanzania", "Thailand", "Tunisia", "Türkiye", "Uganda",
        "Ukraine", "United Arab Emirates", "United Kingdom", "United States of America", "Uruguay",
        "Venezuela", "Vietnam", "Yemen", "Zimbabwe"
    };

    private static final String[] COUNTRY_CODES = {
        "DZ", "AR", "AU", "AT", "AZ", "BH", "BD", "BY", "BE", "BO", "BA", "BR", "BG", "KH", "CA",
        "CL", "CO", "CR", "HR", "CY", "CZ", "DK", "DO", "EC", "EG", "SV", "EE", "FI", "FR", "GE",
        "DE", "GH", "GR", "GT", "HN", "HK", "HU", "IS", "IN", "ID", "IQ", "IE", "IL", "IT", "JP",
        "JM", "JO", "KZ", "KE", "KW", "LA", "LV", "LB", "LY", "LI", "LT", "LU", "MK", "MY", "MT",
        "MX", "MD", "ME", "MA", "NP", "NL", "NZ", "NI", "NG", "NO", "OM", "PK", "PA", "PY", "PE",
        "PH", "PL", "PT", "PR", "QA", "RO", "SA", "SN", "RS", "SG", "SK", "SI", "ZA", "KR", "ES",
        "LK", "SE", "CH", "TW", "TZ", "TH", "TN", "TR", "UG", "UA", "AE", "GB", "US", "UY", "VE",
        "VN", "YE", "ZW"
    };

    private TextView tvCurrentRegion;
    private TextView tvThemeStatus;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCurrentRegion = view.findViewById(R.id.tv_current_region);
        tvThemeStatus = view.findViewById(R.id.tv_theme_status);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        updateRegionLabel();
        updateThemeLabel();

        view.findViewById(R.id.btn_region).setOnClickListener(v -> showRegionSelectionDialog());
        view.findViewById(R.id.btn_theme).setOnClickListener(v -> showThemeSelectionDialog());
        view.findViewById(R.id.btn_your_data).setOnClickListener(v -> showImportExportDialog());
        view.findViewById(R.id.btn_history).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openHistoryPage();
            }
        });
        view.findViewById(R.id.btn_update).setOnClickListener(v -> checkUpdates(true));
        view.findViewById(R.id.btn_privacy_policy).setOnClickListener(v -> showPrivacyPolicyDialog());
        view.findViewById(R.id.btn_share_app).setOnClickListener(v -> shareApp());
        view.findViewById(R.id.btn_about).setOnClickListener(v -> showAboutDialog());

        TextView tvAboutVersion = view.findViewById(R.id.tv_about_version);
        tvAboutVersion.setText("YoMusic v" + getCurrentAppVersionName());
    }

    private void updateThemeLabel() {
        boolean isDarkMode = prefs.getBoolean("is_dark_mode", false);
        tvThemeStatus.setText(isDarkMode ? "Dark" : "Light");
    }

    private void showThemeSelectionDialog() {
        String[] options = {"Light Theme", "Dark Theme"};
        boolean isDarkMode = prefs.getBoolean("is_dark_mode", false);
        int checkedItem = isDarkMode ? 1 : 0;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select App Theme")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    boolean newMode = (which == 1);
                    prefs.edit().putBoolean("is_dark_mode", newMode).apply();
                    
                    if (newMode) {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    
                    updateThemeLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateRegionLabel() {
        String currentCode = prefs.getString(KEY_COUNTRY, "IN");
        String currentName = "India";
        for (int i = 0; i < COUNTRY_CODES.length; i++) {
            if (COUNTRY_CODES[i].equalsIgnoreCase(currentCode)) {
                currentName = COUNTRY_NAMES[i];
                break;
            }
        }
        tvCurrentRegion.setText(currentName + " (" + currentCode.toUpperCase() + ")");
    }

    private void showRegionSelectionDialog() {
        String currentCode = prefs.getString(KEY_COUNTRY, "IN");
        int checkedItem = 38; // Default to India
        for (int i = 0; i < COUNTRY_CODES.length; i++) {
            if (COUNTRY_CODES[i].equalsIgnoreCase(currentCode)) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select your country or region")
                .setSingleChoiceItems(COUNTRY_NAMES, checkedItem, (dialog, which) -> {
                    String selectedCode = COUNTRY_CODES[which];
                    String selectedName = COUNTRY_NAMES[which];

                    // Save to prefs
                    prefs.edit()
                            .putString(KEY_COUNTRY, selectedCode)
                            .remove(KEY_CACHED_HOME_FEED)
                            .remove(KEY_EXPIRES_IN)
                            .apply();

                    // Clear playlists session cache
                    PlaylistSessionCache.clear();

                    // Update UI label
                    updateRegionLabel();

                    Toast.makeText(getContext(), "Region updated to " + selectedName, Toast.LENGTH_SHORT).show();
                    
                    // Reload MainActivity to propagate changes
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Download YoMusic - The premium Youtube Music player app!");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share App via"));
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("About YoMusic")
                .setMessage("A beautiful, premium client for YouTube Music built with native Android and JavaScript APIs.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showImportExportDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Import Export")
                .setMessage("Import or export your data")
                .setPositiveButton("Import", (dialog, which) -> {
                    importLauncher.launch(new String[]{"*/*"});
                })
                .setNegativeButton("Export", (dialog, which) -> {
                    exportLauncher.launch("YoMusic_Backup.ymb");
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void exportDataToUri(Uri uri) {
        new Thread(() -> {
            try {
                SharedPreferences yomusicPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String playlistsJson = yomusicPrefs.getString("saved_playlists", "[]");
                String favoritesJson = yomusicPrefs.getString("saved_favorites", "[]");

                JSONObject backupObj = new JSONObject();
                backupObj.put("playlists", new JSONArray(playlistsJson));
                backupObj.put("favourites", new JSONArray(favoritesJson));

                String jsonString = backupObj.toString(4);

                try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                    if (os != null) {
                        os.write(jsonString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        os.flush();
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Exported successfully!", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            } catch (Exception e) {
                Log.e("MoreFragment", "Export failed", e);
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void importDataFromUri(Uri uri) {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }

                String jsonString = sb.toString();
                requireActivity().runOnUiThread(() -> validateAndImportData(jsonString));

            } catch (Exception e) {
                Log.e("MoreFragment", "Import failed", e);
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Failed to read backup file", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void validateAndImportData(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            
            JSONArray playlistsArray = root.optJSONArray("playlists");
            JSONArray favoritesArray = root.optJSONArray("favourites");
            if (favoritesArray == null) {
                favoritesArray = root.optJSONArray("favorites"); // fallback
            }

            boolean playlistsValid = false;
            boolean favoritesValid = false;

            List<Playlist> playlistsToSave = new ArrayList<>();
            List<VideoItem> favoritesToSave = new ArrayList<>();

            if (playlistsArray != null) {
                playlistsValid = true;
                for (int i = 0; i < playlistsArray.length(); i++) {
                    JSONObject obj = playlistsArray.getJSONObject(i);
                    String playlistId = obj.optString("playlistId", "");
                    String title = obj.optString("title", "");
                    if (playlistId.isEmpty() || title.isEmpty()) {
                        playlistsValid = false;
                        break;
                    }
                    
                    String thumbnail = obj.optString("thumbnail", "");
                    String channel = obj.optString("channel", "");
                    boolean isCustom = obj.optBoolean("isCustomPlaylist", obj.optBoolean("isCustomChannel", false));
                    String nextPage = obj.optString("nextPage", "");

                    List<VideoItem> videoItems = new ArrayList<>();
                    JSONArray videosArray = null;
                    // Handle both new and legacy data:
                    Object videoItemsObj = obj.opt("videoItems");
                    if (videoItemsObj instanceof JSONArray) {
                        videosArray = (JSONArray) videoItemsObj;
                    } else if (videoItemsObj instanceof JSONObject) {
                        // legacy data:
                        JSONObject videoItemsJson = (JSONObject) videoItemsObj;
                        videosArray = videoItemsJson.optJSONArray("items");
                        if (nextPage.isEmpty()) {
                            nextPage = videoItemsJson.optString("nextPage", "");
                        }
                    }

                    int total = obj.optInt("total", 0);
                    if (total == 0 && videosArray != null) {
                        // legacy data to create total field for new format
                        total = videosArray.length();
                    }
                    if (videosArray != null) {
                        for (int j = 0; j < videosArray.length(); j++) {
                            JSONObject vObj = videosArray.getJSONObject(j);
                            videoItems.add(new VideoItem(
                                    vObj.optString("videoId", ""),
                                    vObj.optString("title", ""),
                                    vObj.optString("channel", ""),
                                    vObj.optInt("duration", 0)
                            ));
                        }
                    }
                    playlistsToSave.add(new Playlist(playlistId, title, thumbnail, channel, isCustom, videoItems, nextPage, total));
                }
            }

            if (favoritesArray != null) {
                favoritesValid = true;
                for (int i = 0; i < favoritesArray.length(); i++) {
                    JSONObject obj = favoritesArray.getJSONObject(i);
                    String videoId = obj.optString("videoId", "");
                    String title = obj.optString("title", "");
                    if (videoId.isEmpty() || title.isEmpty()) {
                        favoritesValid = false;
                        break;
                    }
                    String channel = obj.optString("channel", "");
                    int duration = obj.optInt("duration", 0);
                    favoritesToSave.add(new VideoItem(videoId, title, channel, duration));
                }
            }

            if (!playlistsValid && !favoritesValid) {
                Toast.makeText(getContext(), "Invalid file format", Toast.LENGTH_LONG).show();
                return;
            }

            // Save valid components
            if (playlistsValid) {
                PlaylistStorage playlistStorage = new PlaylistStorage(requireContext());
                playlistStorage.savePlaylists(playlistsToSave);
            }
            if (favoritesValid) {
                FavoriteStorage favoriteStorage = new FavoriteStorage(requireContext());
                favoriteStorage.saveFavorites(favoritesToSave);
            }

            // Show result dialog
            showImportResultDialog(playlistsValid, favoritesValid);

        } catch (Exception e) {
            Log.e("MoreFragment", "Validation failed", e);
            Toast.makeText(getContext(), "Invalid JSON format", Toast.LENGTH_LONG).show();
        }
    }

    private void showImportResultDialog(boolean playlistsValid, boolean favoritesValid) {
        String message = (favoritesValid ? "✔" : "✖") + " Favorites\n" +
                         (playlistsValid ? "✔" : "✖") + " Playlists";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Import Complete")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Reload MainActivity to propagate changes
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    private String getCurrentAppVersionName() {
        try {
            return requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    private void checkUpdates(boolean showToastIfLatest) {
        if (showToastIfLatest) {
            Toast.makeText(getContext(), "Checking for updates...", Toast.LENGTH_SHORT).show();
        }
        AppUpdateManager.checkForUpdates(requireContext(), true, new AppUpdateManager.UpdateCheckCallback() {
            @Override
            public void onUpdateStatusChecked(boolean updateAvailable, String tagName, String changelog, String releaseUrl) {
                if (!isAdded()) return;
                if (updateAvailable) {
                    AppUpdateManager.showUpdateAvailableDialog(requireContext(), tagName, changelog, releaseUrl);
                } else {
                    if (showToastIfLatest) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("App Update")
                                .setMessage("You are on the latest version (" + AppUpdateManager.getCurrentAppVersionName(requireContext()) + ").")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error checking updates: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPrivacyPolicyDialog() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        scrollView.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        TextView textView = new TextView(requireContext());
        textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        
        android.util.TypedValue typedValue = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
        textView.setTextColor(typedValue.data);

        String privacyPolicyText = "Iamvkr built the Yo Music app as a Free app. This SERVICE is provided by Iamvkr at no cost and is intended for use as is.\n\n" +
                "This page is used to inform visitors regarding my policies with the collection, use, and disclosure of Personal Information if anyone decided to use my Service.\n\n" +
                "If you choose to use my Service, then you agree to the collection and use of information in relation to this policy. The Personal Information that I collect is used for providing and improving the Service. I will not use or share your information with anyone except as described in this Privacy Policy. The terms used in this Privacy Policy have the same meanings as in our Terms and Conditions, which are accessible at Yo Music unless otherwise defined in this Privacy Policy.\n\n" +
                "Information Collection and Use\n" +
                "For a better experience, while using our Service, I may require you to provide us with certain personally identifiable information. The information that I request will be retained on your device and is not collected by me in any way. The app does use third-party services that may collect information used to identify you.\n\n" +
                "Link to the privacy policy of third-party service providers used by the app:\n" +
                "• Google Play Services\n\n" +
                "Log Data\n" +
                "I want to inform you that whenever you use my Service, in a case of an error in the app I collect data and information (through third-party products) on your phone called Log Data. This Log Data may include information such as your device Internet Protocol (“IP”) address, device name, operating system version, the configuration of the app when utilizing my Service, the time and date of your use of the Service, and other statistics.\n\n" +
                "Cookies\n" +
                "Cookies are files with a small amount of data that are commonly used as anonymous unique identifiers. These are sent to your browser from the websites that you visit and are stored on your device's internal memory.\n\n" +
                "This Service does not use these “cookies” explicitly. However, the app may use third-party code and libraries that use “cookies” to collect information and improve their services. You have the option to either accept or refuse these cookies and know when a cookie is being sent to your device. If you choose to refuse our cookies, you may not be able to use some portions of this Service.\n\n" +
                "Service Providers\n" +
                "I may employ third-party companies and individuals due to the following reasons:\n" +
                "• To facilitate our Service;\n" +
                "• To provide the Service on our behalf;\n" +
                "• To perform Service-related services; or\n" +
                "• To assist us in analyzing how our Service is used.\n\n" +
                "I want to inform users of this Service that these third parties have access to their Personal Information. The reason is to perform the tasks assigned to them on our behalf. However, they are obligated not to disclose or use the information for any other purpose.\n\n" +
                "Security\n" +
                "I value your trust in providing us your Personal Information, thus we are striving to use commercially acceptable means of protecting it. But remember that no method of transmission over the internet, or method of electronic storage is 100% secure and reliable, and I cannot guarantee its absolute security.\n\n" +
                "Links to Other Sites\n" +
                "This Service may contain links to other sites. If you click on a third-party link, you will be directed to that site. Note that these external sites are not operated by me. Therefore, I strongly advise you to review the Privacy Policy of these websites. I have no control over and assume no responsibility for the content, privacy policies, or practices of any third-party sites or services.\n\n" +
                "Children’s Privacy\n" +
                "These Services do not address anyone under the age of 13. I do not knowingly collect personally identifiable information from children under 13 years of age. In the case I discover that a child under 13 has provided me with personal information, I immediately delete this from our servers. If you are a parent or guardian and you are aware that your child has provided us with personal information, please contact me so that I will be able to do the necessary actions.\n\n" +
                "Changes to This Privacy Policy\n" +
                "I may update our Privacy Policy from time to time. Thus, you are advised to review this page periodically for any changes. I will notify you of any changes by posting the new Privacy Policy on this page. This policy is effective as of 1st June 2026.\n\n" +
                "Contact Us\n" +
                "If you have any questions or suggestions about my Privacy Policy, do not hesitate to contact me at beecoder260@proton.me.";

        textView.setText(privacyPolicyText);
        scrollView.addView(textView);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Privacy Policy")
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
