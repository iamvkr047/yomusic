package com.iamvkryt.yomusic;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebViewAssetLoader;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.iamvkryt.yomusic.models.VideoItem;
import com.iamvkryt.yomusic.tabs.HomeFragment;
import com.iamvkryt.yomusic.tabs.PlaylistFragment;
import com.iamvkryt.yomusic.tabs.FavoriteFragment;
import com.iamvkryt.yomusic.tabs.MoreFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance;
    private static final String CHANNEL_ID = "yomusic_playback_channel";
    private static final int NOTIFICATION_ID = 1001;
    private MediaSessionCompat mediaSession;

    public static MainActivity getInstance() {
        return instance;
    }

    private WebView webView;
    private WebChromeClient customWebChromeClient;
    private boolean isCustomViewShowing = false;

    // Fragments
    private HomeFragment homeFragment;
    private PlaylistFragment playlistFragment;
    private FavoriteFragment favoriteFragment;
    private MoreFragment moreFragment;
    private PlayerBottomSheetFragment activePlayerBottomSheet;
    private Fragment activeFragment;

    // Mini Player Views
    private ConstraintLayout miniPlayerContainer;
    private ShapeableImageView miniPlayerThumbnail;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private LinearProgressIndicator miniPlayerProgress;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniSkipNext;

    // Playback State
    private String currentTitle = "No Song Selected";
    private String currentArtist = "Yo Music";
    private String currentThumbnailUrl = "";
    private String currentVideoId = "";
    private boolean isPlaying = false;
    private int currentPosition = 0;
    private int duration = 0;

    // Playlist Queue
    private final List<Song> playlist = new ArrayList<>();
    private int currentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState != null) {
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("home");
            playlistFragment = (PlaylistFragment) getSupportFragmentManager().findFragmentByTag("playlist");
            favoriteFragment = (FavoriteFragment) getSupportFragmentManager().findFragmentByTag("favorite");
            moreFragment = (MoreFragment) getSupportFragmentManager().findFragmentByTag("more");

            if (homeFragment == null) homeFragment = new HomeFragment();
            if (playlistFragment == null) playlistFragment = new PlaylistFragment();
            if (favoriteFragment == null) favoriteFragment = new FavoriteFragment();
            if (moreFragment == null) moreFragment = new MoreFragment();

            // Find the visible fragment that was active before recreation
            activeFragment = homeFragment;
            if (playlistFragment.isAdded() && !playlistFragment.isHidden()) {
                activeFragment = playlistFragment;
            } else if (favoriteFragment.isAdded() && !favoriteFragment.isHidden()) {
                activeFragment = favoriteFragment;
            } else if (moreFragment.isAdded() && !moreFragment.isHidden()) {
                activeFragment = moreFragment;
            }
        } else {
            homeFragment = new HomeFragment();
            playlistFragment = new PlaylistFragment();
            favoriteFragment = new FavoriteFragment();
            moreFragment = new MoreFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, homeFragment, "home")
                    .commit();
            activeFragment = homeFragment;
        }

        setupToolbar();
        setupBottomNavigation();
        setupMiniPlayerUI();
        setupYouTubeWebView();
        setupBackPressHandling();
        
        createNotificationChannel();
        checkNotificationPermission();
        setupMediaSession();

        // Hide bottom nav when any detail/overlay page is on the back stack
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            int count = getSupportFragmentManager().getBackStackEntryCount();
            bottomNav.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUpdateBadgeVisibility();
    }

    public void updateUpdateBadgeVisibility() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null && toolbar.getMenu() != null) {
            android.view.MenuItem updateItem = toolbar.getMenu().findItem(R.id.action_update);
            if (updateItem != null) {
                boolean show = AppUpdateManager.isUpdateAvailable(this);
                updateItem.setVisible(show);
            }
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_search) {
                openSearchPage();
                return true;
            } else if (itemId == R.id.action_update) {
                AppUpdateManager.showUpdateDialog(this);
                return true;
            }
            return false;
        });
        updateUpdateBadgeVisibility();
    }

    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isCustomViewShowing && customWebChromeClient != null) {
                    customWebChromeClient.onHideCustomView();
                } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else if (activeFragment != homeFragment) {
                    BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
                    bottomNavigation.setSelectedItemId(R.id.navigation_home);
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        
        // Prevent double bottom padding by disabling automatic inset handling on BottomNavigationView
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> insets);

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String tag = "";
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = homeFragment;
                tag = "home";
            } else if (itemId == R.id.navigation_playlist) {
                selectedFragment = playlistFragment;
                tag = "playlist";
            } else if (itemId == R.id.navigation_favorite) {
                selectedFragment = favoriteFragment;
                tag = "favorite";
            } else if (itemId == R.id.navigation_more) {
                selectedFragment = moreFragment;
                tag = "more";
            }

            if (selectedFragment != null) {
                // Clear any detail pages (search, playlist detail, etc.) from backstack
                // before switching tabs — standard mobile tab behaviour
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStackImmediate(
                            null,
                            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }

                if (selectedFragment != activeFragment) {
                    androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    if (!selectedFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, selectedFragment, tag);
                    }
                    transaction.hide(activeFragment).show(selectedFragment).commit();
                    activeFragment = selectedFragment;
                }
                return true;
            }
            return false;
        });
    }

    private void setupMiniPlayerUI() {
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        miniPlayerThumbnail = findViewById(R.id.mini_player_thumbnail);
        miniPlayerTitle = findViewById(R.id.mini_player_title);
        miniPlayerArtist = findViewById(R.id.mini_player_artist);
        miniPlayerProgress = findViewById(R.id.mini_player_progress);
        btnMiniPlayPause = findViewById(R.id.btn_mini_play_pause);
        btnMiniSkipNext = findViewById(R.id.btn_mini_skip_next);

        btnMiniPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseVideo();
            } else {
                playVideo();
            }
        });

        btnMiniSkipNext.setOnClickListener(v -> playNextSong());

        // Open Full Screen Player BottomSheet on clicking Mini Player container
        miniPlayerContainer.setOnClickListener(v -> showFullPlayerSheet());
    }

    private void setupYouTubeWebView() {
        webView = findViewById(R.id.youtube_webview);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://appassets.androidplatform.net/")) {
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("https://appassets.androidplatform.net/")) {
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        customWebChromeClient = new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            private int mOriginalSystemUiVisibility;
            private final List<Dialog> mHiddenDialogs = new ArrayList<>();

            @Override
            public Bitmap getDefaultVideoPoster() {
                return null;
            }

            @Override
            public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback viewCallback) {
                if (mCustomView != null) {
                    onHideCustomView();
                    return;
                }

                mCustomView = paramView;
                mCustomViewCallback = viewCallback;

                mOriginalOrientation = MainActivity.this.getRequestedOrientation();
                MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                isCustomViewShowing = true;

                mOriginalSystemUiVisibility = MainActivity.this.getWindow().getDecorView().getSystemUiVisibility();

                // Hide all active DialogFragments (bottom sheets) to prevent them from drawing over the fullscreen view
                mHiddenDialogs.clear();
                for (Fragment fragment : MainActivity.this.getSupportFragmentManager().getFragments()) {
                    if (fragment instanceof DialogFragment) {
                        Dialog dialog = ((DialogFragment) fragment).getDialog();
                        if (dialog != null && dialog.isShowing()) {
                            dialog.hide();
                            mHiddenDialogs.add(dialog);
                        }
                    }
                }

                ((FrameLayout) MainActivity.this.getWindow().getDecorView())
                    .addView(mCustomView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, 
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ));

                MainActivity.this.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                );
            }
            
            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;

                ((FrameLayout) MainActivity.this.getWindow().getDecorView())
                    .removeView(mCustomView);
                mCustomView = null;

                isCustomViewShowing = false;

                // Restore any dialogs we hid when entering fullscreen
                for (Dialog dialog : mHiddenDialogs) {
                    try {
                        dialog.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                mHiddenDialogs.clear();

                MainActivity.this.getWindow().getDecorView()
                    .setSystemUiVisibility(mOriginalSystemUiVisibility);

                MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 

                mCustomViewCallback.onCustomViewHidden();
                mCustomViewCallback = null;
            }
        };

        webView.setWebChromeClient(customWebChromeClient);

        webView.addJavascriptInterface(new YoutubePlayerBridge(new YoutubePlayerBridge.YoutubePlayerListener() {
            @Override
            public void onReady() {
                // Engine ready
            }

            @Override
            public void onStateChange(String state) {
                if ("Playing".equalsIgnoreCase(state)) {
                    isPlaying = true;
                    btnMiniPlayPause.setImageResource(R.drawable.ic_pause);
                } else if ("Paused".equalsIgnoreCase(state) || "Ended".equalsIgnoreCase(state)) {
                    isPlaying = false;
                    btnMiniPlayPause.setImageResource(R.drawable.ic_play);
                    if ("Ended".equalsIgnoreCase(state)) {
                        playNextSong();
                    }
                }

                if (activePlayerBottomSheet != null && activePlayerBottomSheet.isAdded()) {
                    activePlayerBottomSheet.updatePlayState(isPlaying);
                }
                showMediaNotification();
            }

            @Override
            public void onProgress(int currentTime, int dur) {
                currentPosition = currentTime;
                duration = dur;

                if (dur > 0) {
                    int percent = (currentTime * 100) / dur;
                    miniPlayerProgress.setProgress(percent);
                }

                if (activePlayerBottomSheet != null && activePlayerBottomSheet.isAdded()) {
                    activePlayerBottomSheet.updateProgress(currentTime, dur);
                }
            }

            @Override
            public void onVideoLoaded(String title, String author, String videoId) {
                if (title != null && !title.isEmpty()) {
                    currentTitle = title;
                    miniPlayerTitle.setText(title);
                }
                if (author != null && !author.isEmpty()) {
                    currentArtist = author;
                    miniPlayerArtist.setText(author);
                }
                if (activePlayerBottomSheet != null && activePlayerBottomSheet.isAdded()) {
                    activePlayerBottomSheet.updateTrackInfo(currentTitle, currentArtist, currentThumbnailUrl);
                }
                showMediaNotification();
            }

            @Override
            public void onError(int errorCode) {
                Toast.makeText(MainActivity.this, "Playback Error: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        }), "AndroidInterface");

        webView.loadUrl("https://appassets.androidplatform.net/assets/youtube_player.html");
    }

    public void openPlaylistDetail(String playlistId) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, PlaylistDetailFragment.newInstance(playlistId))
                .addToBackStack(null)
                .commit();
    }

    public void openCustomPlaylistDetail(String playlistId, String title) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, CustomPlaylistDetailFragment.newInstance(playlistId))
                .addToBackStack(null)
                .commit();
    }

    public void openSearchPage() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SearchFragment())
                .addToBackStack(null)
                .commit();
    }

    public void openSearchResults(String query) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, SearchResultsFragment.newInstance(query))
                .addToBackStack(null)
                .commit();
    }

    public void notifyFavoriteChanged() {
        if (favoriteFragment != null && favoriteFragment.isAdded()) {
            favoriteFragment.loadFavoritesData();
        }
    }

    public void playPlaylistQueue(List<VideoItem> items, int startIndex) {
        if (items == null || items.isEmpty()) return;

        playlist.clear();
        for (VideoItem item : items) {
            String thumb = "https://i.ytimg.com/vi/" + item.getVideoId() + "/hqdefault.jpg";
            playlist.add(new Song(item.getVideoId(), item.getTitle(), item.getChannel(), thumb));
        }

        int targetIndex = (startIndex >= 0 && startIndex < playlist.size()) ? startIndex : 0;
        currentIndex = targetIndex;
        playSong(playlist.get(currentIndex));
    }

    public void playSong(Song song) {
        if (song == null) return;

        // Update playlist queue if necessary
        if (!playlist.contains(song)) {
            playlist.add(song);
        }
        currentIndex = playlist.indexOf(song);

        currentVideoId = song.getVideoId();
        currentTitle = song.getTitle();
        currentArtist = song.getArtist();
        currentThumbnailUrl = song.getThumbnailUrl();

        // Show Mini Player UI
        miniPlayerContainer.setVisibility(View.VISIBLE);
        miniPlayerTitle.setText(currentTitle);
        miniPlayerArtist.setText(currentArtist);

        if (currentThumbnailUrl != null && !currentThumbnailUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentThumbnailUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .into(miniPlayerThumbnail);
        }

        loadVideo(currentVideoId);

        if (activePlayerBottomSheet != null && activePlayerBottomSheet.isAdded()) {
            activePlayerBottomSheet.updateTrackInfo(currentTitle, currentArtist, currentThumbnailUrl);
        }
        showMediaNotification();
    }

    public void playNextSong() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex + 1) % playlist.size();
        playSong(playlist.get(currentIndex));
    }

    public void playPreviousSong() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        playSong(playlist.get(currentIndex));
    }

    private void showFullPlayerSheet() {
        activePlayerBottomSheet = new PlayerBottomSheetFragment();
        activePlayerBottomSheet.show(getSupportFragmentManager(), "PlayerBottomSheetFragment");
    }

    public void loadVideo(String videoId) {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript("loadVideo('" + videoId + "');", null));
        }
    }

    public void playVideo() {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript("playVideo();", null));
        }
    }

    public void pauseVideo() {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript("pauseVideo();", null));
        }
    }

    public void seekTo(int seconds) {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript("seekTo(" + seconds + ");", null));
        }
    }

    public String getCurrentVideoId() { return currentVideoId; }
    public String getCurrentTitle() { return currentTitle; }
    public String getCurrentArtist() { return currentArtist; }
    public String getCurrentThumbnailUrl() { return currentThumbnailUrl; }
    public boolean isPlaying() { return isPlaying; }
    public int getCurrentPosition() { return currentPosition; }
    public int getDuration() { return duration; }

    public WebView getWebView() {
        return webView;
    }

    public void detachWebView() {
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }
        }
    }

    public void attachWebViewToHiddenContainer() {
        if (webView != null) {
            detachWebView();
            FrameLayout container = findViewById(R.id.webview_container);
            if (container != null) {
                container.addView(webView);
            }
        }
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "YoMusicMediaSession");
        mediaSession.setActive(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("YoMusic player notification controls");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    public void handleNotificationAction(String action) {
        if (action == null) return;
        switch (action) {
            case MediaButtonReceiver.ACTION_PREVIOUS:
                playPreviousSong();
                break;
            case MediaButtonReceiver.ACTION_PLAY_PAUSE:
                if (isPlaying) {
                    pauseVideo();
                } else {
                    playVideo();
                }
                break;
            case MediaButtonReceiver.ACTION_NEXT:
                playNextSong();
                break;
        }
    }

    public void showMediaNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, MediaButtonReceiver.class).setAction(MediaButtonReceiver.ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(
                this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MediaButtonReceiver.class).setAction(MediaButtonReceiver.ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(
                this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MediaButtonReceiver.class).setAction(MediaButtonReceiver.ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(
                this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;

        if (currentThumbnailUrl != null && !currentThumbnailUrl.isEmpty()) {
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(currentThumbnailUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            buildAndPostNotification(resource, pendingContentIntent, prevPendingIntent, playPausePendingIntent, nextPendingIntent, playPauseIcon);
                        }

                        @Override
                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}

                        @Override
                        public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                            buildAndPostNotification(null, pendingContentIntent, prevPendingIntent, playPausePendingIntent, nextPendingIntent, playPauseIcon);
                        }
                    });
        } else {
            buildAndPostNotification(null, pendingContentIntent, prevPendingIntent, playPausePendingIntent, nextPendingIntent, playPauseIcon);
        }
    }

    private void buildAndPostNotification(Bitmap thumbnail, PendingIntent contentIntent,
                                          PendingIntent prevIntent, PendingIntent playPauseIntent,
                                          PendingIntent nextIntent, int playPauseIcon) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(thumbnail)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setContentIntent(contentIntent)
                .setOngoing(isPlaying)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_skip_previous, "Previous", prevIntent)
                .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", playPauseIntent)
                .addAction(R.drawable.ic_skip_next, "Next", nextIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession != null ? mediaSession.getSessionToken() : null));

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
        if (instance == this) {
            instance = null;
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}