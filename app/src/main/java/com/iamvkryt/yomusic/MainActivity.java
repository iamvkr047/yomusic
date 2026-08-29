package com.iamvkryt.yomusic;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.content.ContextCompat;
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
import com.iamvkryt.yomusic.utils.YouTubeLinkParser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance;
    private PlaybackService playbackService;
    private boolean isServiceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            isServiceBound = true;
            playbackService.setCallback(new PlaybackService.Callback() {
                @Override
                public void onPlay() {
                    playVideo();
                }

                @Override
                public void onPause() {
                    pauseVideo();
                }

                @Override
                public void onNext() {
                    playNextSong();
                }

                @Override
                public void onPrevious() {
                    playPreviousSong();
                }
            });
            updateServicePlaybackState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            isServiceBound = false;
        }
    };

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
    private boolean isPlayerReady = false;
    private String pendingVideoId = null;

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
        
        checkNotificationPermission();
        bindPlaybackService();

        // Hide bottom nav when any detail/overlay page is on the back stack
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            int count = getSupportFragmentManager().getBackStackEntryCount();
            bottomNav.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
        });

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action)) {
            YouTubeLinkParser.ParseResult result = YouTubeLinkParser.parseIntent(intent);
            if (result.type == YouTubeLinkParser.ResultType.SONG) {
                String videoId = result.id;
                Song song = new Song(videoId, "Loading song...", "YouTube", "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg");
                playSong(song);
                Toast.makeText(this, "Playing shared YouTube song...", Toast.LENGTH_SHORT).show();
            } else if (result.type == YouTubeLinkParser.ResultType.PLAYLIST) {
                String playlistId = result.id;
                openPlaylistDetail(playlistId);
                Toast.makeText(this, "Loading shared YouTube playlist...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid or unsupported YouTube link", Toast.LENGTH_SHORT).show();
            }
            // Clear action so orientation change / recreation doesn't trigger intent again
            intent.setAction(null);
        }
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
                isPlayerReady = true;
                if (pendingVideoId != null) {
                    loadVideo(pendingVideoId);
                    pendingVideoId = null;
                }
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
        if (isPlayerReady) {
            if (webView != null) {
                webView.post(() -> webView.evaluateJavascript("loadVideo('" + videoId + "');", null));
            }
        } else {
            pendingVideoId = videoId;
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

    private void bindPlaybackService() {
        Intent intent = new Intent(this, PlaybackService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    public void handleNotificationAction(String action) {
        if (playbackService != null) {
            playbackService.handleAction(action);
        } else {
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
    }

    public void showMediaNotification() {
        updateServicePlaybackState();
    }

    public void updateServicePlaybackState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        if (isServiceBound && playbackService != null) {
            playbackService.updatePlaybackState(currentTitle, currentArtist, currentThumbnailUrl, isPlaying);
        } else {
            Intent serviceIntent = new Intent(this, PlaybackService.class);
            if (isPlaying) {
                ContextCompat.startForegroundService(this, serviceIntent);
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
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