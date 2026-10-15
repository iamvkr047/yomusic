package com.iamvkryt.yomusic.helpers;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class YouTubeChromeClient extends WebChromeClient {

    private final AppCompatActivity activity;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private int mOriginalOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    private int mOriginalSystemUiVisibility;
    private boolean isCustomViewShowing = false;

    public YouTubeChromeClient(AppCompatActivity activity) {
        this.activity = activity;
    }

    public boolean isCustomViewShowing() {
        return isCustomViewShowing;
    }

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

        mOriginalOrientation = activity.getRequestedOrientation();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        isCustomViewShowing = true;

        mOriginalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();

        ((FrameLayout) activity.getWindow().getDecorView())
                .addView(mCustomView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        activity.getWindow().getDecorView().setSystemUiVisibility(
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

        ((FrameLayout) activity.getWindow().getDecorView())
                .removeView(mCustomView);
        mCustomView = null;

        isCustomViewShowing = false;

        activity.getWindow().getDecorView()
                .setSystemUiVisibility(mOriginalSystemUiVisibility);

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (mCustomViewCallback != null) {
            mCustomViewCallback.onCustomViewHidden();
            mCustomViewCallback = null;
        }
    }
}
