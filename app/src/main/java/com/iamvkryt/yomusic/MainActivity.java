package com.iamvkryt.yomusic;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.iamvkryt.yomusic.models.VideoItem;
import com.iamvkryt.yomusic.helpers.VideoCommentsHelper;
import com.iamvkryt.yomusic.helpers.MiniPlayerController;
import com.iamvkryt.yomusic.helpers.AppNavigator;
import com.iamvkryt.yomusic.helpers.IntentHandler;
import com.iamvkryt.yomusic.helpers.PlaybackServiceBridge;
import com.iamvkryt.yomusic.helpers.QueueManager;
import com.iamvkryt.yomusic.helpers.YouTubePlayerEngine;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance;
    private PlaybackServiceBridge playbackServiceBridge;

    public static MainActivity getInstance() {
        return instance;
    }

    private VideoCommentsHelper videoCommentsHelper;
    private YouTubePlayerEngine playerEngine;
    private AppNavigator appNavigator;
    private IntentHandler intentHandler;
    private MiniPlayerController miniPlayerController;
    private QueueManager queueManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        queueManager = new QueueManager(this);
        

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appNavigator = new AppNavigator(this);
        appNavigator.initMainTabs(savedInstanceState);

        setupToolbar();
        setupMiniPlayerUI();
        videoCommentsHelper = new VideoCommentsHelper(this);
        setupYouTubePlayerEngine();
        setupBackPressHandling();
        
        playbackServiceBridge = new PlaybackServiceBridge(this, new PlaybackServiceBridge.PlaybackCallback() {
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
        playbackServiceBridge.bind();

        intentHandler = new IntentHandler(this, new IntentHandler.IntentActionCallback() {
            @Override
            public void onPlaySong(Song song) {
                playSong(song);
            }

            @Override
            public void onOpenPlaylist(String playlistId) {
                openPlaylistDetail(playlistId);
            }
        });
        intentHandler.handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intentHandler != null) {
            intentHandler.handleIncomingIntent(intent);
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
                if (playerEngine != null && playerEngine.isCustomViewShowing()) {
                    playerEngine.onHideCustomView();
                } else if (isVideoCommentsVisible()) {
                    hideVideoCommentsOverlay();
                } else if (appNavigator != null && appNavigator.handleBackPressed()) {
                    // Handled by navigator (popped backstack or switched to home tab)
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    private void setupMiniPlayerUI() {
        miniPlayerController = new MiniPlayerController(this, new MiniPlayerController.MiniPlayerListener() {
            @Override
            public void onPlayPauseClicked() {
                if (isPlaying()) {
                    pauseVideo();
                } else {
                    playVideo();
                }
            }

            @Override
            public void onSkipNextClicked() {
                playNextSong();
            }

            @Override
            public void onContainerClicked() {
                showFullPlayerSheet();
            }
        });
    }

    private void setupYouTubePlayerEngine() {
        playerEngine = new YouTubePlayerEngine(this, new YouTubePlayerEngine.PlayerEngineListener() {
            @Override
            public void onPlayerReady() {
            }

            @Override
            public void onPlaybackStateChanged(boolean isPlaying) {
                if (miniPlayerController != null) {
                    miniPlayerController.updatePlayState(isPlaying);
                }
                if (appNavigator != null) {
                    appNavigator.updatePlayerSheetPlayState(isPlaying);
                }
                showMediaNotification();
            }

            @Override
            public void onTrackEnded() {
                playNextSong();
            }

            @Override
            public void onProgressUpdated(int currentPosition, int duration) {
                if (miniPlayerController != null) {
                    miniPlayerController.updateProgress(currentPosition, duration);
                }
                if (appNavigator != null) {
                    appNavigator.updatePlayerSheetProgress(currentPosition, duration);
                }
            }

            @Override
            public void onVideoMetadataLoaded(String title, String author, String videoId) {
                if (miniPlayerController != null) {
                    miniPlayerController.updateTrackInfo(title, author, getCurrentThumbnailUrl());
                }
                if (appNavigator != null) {
                    appNavigator.updatePlayerSheetTrackInfo(title, author, getCurrentThumbnailUrl());
                }
                showMediaNotification();
            }

            @Override
            public void onError(int errorCode) {
                Toast.makeText(MainActivity.this, "Playback Error: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showVideoCommentsOverlay() {
        if (videoCommentsHelper != null) {
            videoCommentsHelper.showVideoCommentsOverlay();
        }
    }

    public void hideVideoCommentsOverlay() {
        if (videoCommentsHelper != null) {
            videoCommentsHelper.hideVideoCommentsOverlay();
        }
    }

    public boolean isVideoCommentsVisible() {
        return videoCommentsHelper != null && videoCommentsHelper.isVideoCommentsVisible();
    }

    public void openPlaylistDetail(String playlistId) {
        if (appNavigator != null) {
            appNavigator.openPlaylistDetail(playlistId);
        }
    }

    public void openCustomPlaylistDetail(String playlistId, String title) {
        if (appNavigator != null) {
            appNavigator.openCustomPlaylistDetail(playlistId, title);
        }
    }

    public void openSearchPage() {
        if (appNavigator != null) {
            appNavigator.openSearchPage();
        }
    }

    public void openSearchResults(String query) {
        if (appNavigator != null) {
            appNavigator.openSearchResults(query);
        }
    }

    public void openHistoryPage() {
        if (appNavigator != null) {
            appNavigator.openHistoryPage();
        }
    }

    public void notifyFavoriteChanged() {
        if (appNavigator != null) {
            appNavigator.notifyFavoriteChanged();
        }
    }

    public void playPlaylistQueue(List<VideoItem> items, int startIndex) {
        if (queueManager != null) {
            Song targetSong = queueManager.setQueue(items, startIndex);
            if (targetSong != null) {
                playSong(targetSong);
            }
        }
    }

    public void playSong(Song song) {
        if (song == null) return;

        if (queueManager != null) {
            queueManager.enqueueSong(song);
        }

        if (playerEngine != null) {
            playerEngine.loadSong(song);
        }

        // Show Mini Player UI
        if (miniPlayerController != null) {
            miniPlayerController.show();
            miniPlayerController.updateTrackInfo(getCurrentTitle(), getCurrentArtist(), getCurrentThumbnailUrl());
        }

        if (appNavigator != null) {
            appNavigator.updatePlayerSheetTrackInfo(getCurrentTitle(), getCurrentArtist(), getCurrentThumbnailUrl());
        }
        showMediaNotification();
    }

    public void playNextSong() {
        if (queueManager != null) {
            Song next = queueManager.getNextSong();
            if (next != null) {
                playSong(next);
            }
        }
    }

    public void playPreviousSong() {
        if (queueManager != null) {
            Song prev = queueManager.getPreviousSong();
            if (prev != null) {
                playSong(prev);
            }
        }
    }

    private void showFullPlayerSheet() {
        if (appNavigator != null) {
            appNavigator.showFullPlayerSheet();
        }
    }

    public void loadVideo(String videoId) {
        if (playerEngine != null) {
            playerEngine.loadVideo(videoId);
        }
    }

    public void playVideo() {
        if (playerEngine != null) {
            playerEngine.playVideo();
        }
    }

    public void pauseVideo() {
        if (playerEngine != null) {
            playerEngine.pauseVideo();
        }
    }

    public void seekTo(int seconds) {
        if (playerEngine != null) {
            playerEngine.seekTo(seconds);
        }
    }

    public String getCurrentVideoId() { return playerEngine != null ? playerEngine.getCurrentVideoId() : ""; }
    public String getCurrentTitle() { return playerEngine != null ? playerEngine.getCurrentTitle() : "No Song Selected"; }
    public String getCurrentArtist() { return playerEngine != null ? playerEngine.getCurrentArtist() : "Yo Music"; }
    public String getCurrentThumbnailUrl() { return playerEngine != null ? playerEngine.getCurrentThumbnailUrl() : ""; }
    public boolean isPlaying() { return playerEngine != null && playerEngine.isPlaying(); }
    public int getCurrentPosition() { return playerEngine != null ? playerEngine.getCurrentPosition() : 0; }
    public int getDuration() { return playerEngine != null ? playerEngine.getDuration() : 0; }

    public void handleNotificationAction(String action) {
        if (playbackServiceBridge != null) {
            playbackServiceBridge.handleNotificationAction(action, isPlaying());
        }
    }

    public void showMediaNotification() {
        updateServicePlaybackState();
    }

    public void updateServicePlaybackState() {
        if (playbackServiceBridge != null) {
            playbackServiceBridge.updatePlaybackState(getCurrentTitle(), getCurrentArtist(), getCurrentThumbnailUrl(), isPlaying());
        }
    }

    @Override
    protected void onDestroy() {
        if (playbackServiceBridge != null) {
            playbackServiceBridge.unbind();
        }
        if (instance == this) {
            instance = null;
        }
        if (playerEngine != null) {
            playerEngine.destroy();
        }
        super.onDestroy();
    }
}