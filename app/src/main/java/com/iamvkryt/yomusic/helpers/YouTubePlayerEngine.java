package com.iamvkryt.yomusic.helpers;

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.Song;
import com.iamvkryt.yomusic.YoutubePlayerBridge;

public class YouTubePlayerEngine {

    public interface PlayerEngineListener {
        void onPlayerReady();
        void onPlaybackStateChanged(boolean isPlaying);
        void onTrackEnded();
        void onProgressUpdated(int currentPosition, int duration);
        void onVideoMetadataLoaded(String title, String author, String videoId);
        void onError(int errorCode);
    }

    private final AppCompatActivity activity;
    private final PlayerEngineListener listener;

    private WebView webView;
    private YouTubeChromeClient youTubeChromeClient;

    private String currentTitle = "No Song Selected";
    private String currentArtist = "Yo Music";
    private String currentThumbnailUrl = "";
    private String currentVideoId = "";
    private boolean isPlaying = false;
    private int currentPosition = 0;
    private int duration = 0;
    private boolean isPlayerReady = false;
    private String pendingVideoId = null;

    public YouTubePlayerEngine(AppCompatActivity activity, PlayerEngineListener listener) {
        this.activity = activity;
        this.listener = listener;
        initWebView();
    }

    private void initWebView() {
        webView = activity.findViewById(R.id.youtube_webview);
        if (webView == null) return;

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(activity))
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

        youTubeChromeClient = new YouTubeChromeClient(activity);
        webView.setWebChromeClient(youTubeChromeClient);

        webView.addJavascriptInterface(new YoutubePlayerBridge(new YoutubePlayerBridge.YoutubePlayerListener() {
            @Override
            public void onReady() {
                isPlayerReady = true;
                if (pendingVideoId != null) {
                    loadVideo(pendingVideoId);
                    pendingVideoId = null;
                }
                if (listener != null) {
                    listener.onPlayerReady();
                }
            }

            @Override
            public void onStateChange(String state) {
                if ("Playing".equalsIgnoreCase(state)) {
                    isPlaying = true;
                    if (listener != null) {
                        listener.onPlaybackStateChanged(true);
                    }
                } else if ("Paused".equalsIgnoreCase(state) || "Ended".equalsIgnoreCase(state)) {
                    isPlaying = false;
                    if (listener != null) {
                        listener.onPlaybackStateChanged(false);
                    }
                    if ("Ended".equalsIgnoreCase(state)) {
                        if (listener != null) {
                            listener.onTrackEnded();
                        }
                    }
                }
            }

            @Override
            public void onProgress(int currentTime, int dur) {
                currentPosition = currentTime;
                duration = dur;
                if (listener != null) {
                    listener.onProgressUpdated(currentTime, dur);
                }
            }

            @Override
            public void onVideoLoaded(String title, String author, String videoId) {
                if (title != null && !title.isEmpty()) {
                    currentTitle = title;
                }
                if (author != null && !author.isEmpty()) {
                    currentArtist = author;
                }
                if (listener != null) {
                    listener.onVideoMetadataLoaded(currentTitle, currentArtist, videoId);
                }
            }

            @Override
            public void onError(int errorCode) {
                if (listener != null) {
                    listener.onError(errorCode);
                }
            }
        }), "AndroidInterface");

        webView.loadUrl("https://appassets.androidplatform.net/assets/youtube_player.html");
    }

    public void loadSong(Song song) {
        if (song == null) return;
        currentVideoId = song.getVideoId();
        currentTitle = song.getTitle();
        currentArtist = song.getArtist();
        currentThumbnailUrl = song.getThumbnailUrl();
        loadVideo(currentVideoId);
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

    public boolean isCustomViewShowing() {
        return youTubeChromeClient != null && youTubeChromeClient.isCustomViewShowing();
    }

    public void onHideCustomView() {
        if (youTubeChromeClient != null) {
            youTubeChromeClient.onHideCustomView();
        }
    }

    public boolean isPlayerReady() { return isPlayerReady; }
    public boolean isPlaying() { return isPlaying; }
    public String getCurrentVideoId() { return currentVideoId; }
    public String getCurrentTitle() { return currentTitle; }
    public String getCurrentArtist() { return currentArtist; }
    public String getCurrentThumbnailUrl() { return currentThumbnailUrl; }
    public int getCurrentPosition() { return currentPosition; }
    public int getDuration() { return duration; }

    public void destroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
