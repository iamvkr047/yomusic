package com.iamvkr.yomusic;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.view.View;

public class MyWebView extends WebView {

    // Required for programmatic creation in Java: new MyWebView(context)
    public MyWebView(Context context) {
        super(context);
        init();
    }

    // Required for inflation from XML layout files
    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // Required for style applications via XML layouts
    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // Optional helper method to encapsulate default configurations
    private void init() {
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setDomStorageEnabled(true);
    }
    
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        // Force the parent class to always receive the VISIBLE state
        super.onWindowVisibilityChanged(View.VISIBLE);
    }

    @Override
    protected void onDetachedFromWindow() {
        // Do not call super.onDetachedFromWindow() to prevent the WebView from pausing javascript/media
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        // Force overall visibility changes to remain VISIBLE
        super.onVisibilityChanged(changedView, View.VISIBLE);
    }

    @Override
    public int getWindowVisibility() {
        // Intercept getter queries and return VISIBLE
        return View.VISIBLE;
    }

    @Override
    public int getVisibility() {
        // Intercept getter queries and return VISIBLE
        return View.VISIBLE;
    }
}
