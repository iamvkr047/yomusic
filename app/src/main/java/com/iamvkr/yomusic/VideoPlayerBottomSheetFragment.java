package com.iamvkr.yomusic;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.google.android.material.card.MaterialCardView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class VideoPlayerBottomSheetFragment extends BottomSheetDialogFragment {

    private MaterialCardView videoContainer;
    private ImageButton btnCloseVideo;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                // Clear solid surface color background
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
                if (lp != null) {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    bottomSheet.setLayoutParams(lp);
                }
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_video_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        videoContainer = view.findViewById(R.id.video_container);
        btnCloseVideo = view.findViewById(R.id.btn_close_video);

        btnCloseVideo.setOnClickListener(v -> dismiss());

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            WebView webView = activity.getWebView();
            if (webView != null) {
                activity.detachWebView();
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                webView.setLayoutParams(lp);
                videoContainer.addView(webView);
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        restoreWebView();
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        restoreWebView();
        super.onDestroyView();
    }

    private void restoreWebView() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            WebView webView = activity.getWebView();
            if (webView != null && webView.getParent() == videoContainer) {
                activity.attachWebViewToHiddenContainer();
            }
        }
    }
}
