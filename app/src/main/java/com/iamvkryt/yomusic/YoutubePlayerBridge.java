package com.iamvkryt.yomusic;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class YoutubePlayerBridge {
    private static final String TAG = "YoutubePlayerBridge";

    public interface YoutubePlayerListener {
        void onReady();
        void onStateChange(String state);
        void onProgress(int currentTime, int duration);
        void onVideoLoaded(String title, String author, String videoId);
        void onError(int errorCode);
    }

    private final YoutubePlayerListener listener;
    private final Handler mainHandler;

    public YoutubePlayerBridge(YoutubePlayerListener listener) {
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @JavascriptInterface
    public void onReady() {
        Log.d(TAG, "onReady received from JS");
        if (listener != null) {
            mainHandler.post(listener::onReady);
        }
    }

    @JavascriptInterface
    public void onStateChange(String state) {
        Log.d(TAG, "onStateChange: " + state);
        if (listener != null) {
            mainHandler.post(() -> listener.onStateChange(state));
        }
    }

    @JavascriptInterface
    public void onProgress(int currentTime, int duration) {
        if (listener != null) {
            mainHandler.post(() -> listener.onProgress(currentTime, duration));
        }
    }

    @JavascriptInterface
    public void onVideoLoaded(String title, String author, String videoId) {
        Log.d(TAG, "onVideoLoaded: " + title + " by " + author);
        if (listener != null) {
            mainHandler.post(() -> listener.onVideoLoaded(title, author, videoId));
        }
    }

    @JavascriptInterface
    public void onError(int errorCode) {
        Log.e(TAG, "onError: " + errorCode);
        if (listener != null) {
            mainHandler.post(() -> listener.onError(errorCode));
        }
    }
}
