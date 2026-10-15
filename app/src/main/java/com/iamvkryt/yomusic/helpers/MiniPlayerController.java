package com.iamvkryt.yomusic.helpers;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.iamvkryt.yomusic.R;

public class MiniPlayerController {

    public interface MiniPlayerListener {
        void onPlayPauseClicked();
        void onSkipNextClicked();
        void onContainerClicked();
    }

    private final Activity activity;
    private final MiniPlayerListener listener;

    private ConstraintLayout miniPlayerContainer;
    private ShapeableImageView miniPlayerThumbnail;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private LinearProgressIndicator miniPlayerProgress;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniSkipNext;

    public MiniPlayerController(Activity activity, MiniPlayerListener listener) {
        this.activity = activity;
        this.listener = listener;
        initViews();
    }

    private void initViews() {
        miniPlayerContainer = activity.findViewById(R.id.mini_player_container);
        miniPlayerThumbnail = activity.findViewById(R.id.mini_player_thumbnail);
        miniPlayerTitle = activity.findViewById(R.id.mini_player_title);
        miniPlayerArtist = activity.findViewById(R.id.mini_player_artist);
        miniPlayerProgress = activity.findViewById(R.id.mini_player_progress);
        btnMiniPlayPause = activity.findViewById(R.id.btn_mini_play_pause);
        btnMiniSkipNext = activity.findViewById(R.id.btn_mini_skip_next);

        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayPauseClicked();
                }
            });
        }

        if (btnMiniSkipNext != null) {
            btnMiniSkipNext.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSkipNextClicked();
                }
            });
        }

        if (miniPlayerContainer != null) {
            miniPlayerContainer.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContainerClicked();
                }
            });
        }
    }

    public void show() {
        if (miniPlayerContainer != null) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        if (miniPlayerContainer != null) {
            miniPlayerContainer.setVisibility(View.GONE);
        }
    }

    public void updateTrackInfo(String title, String artist, String thumbnailUrl) {
        if (miniPlayerTitle != null && title != null && !title.isEmpty()) {
            miniPlayerTitle.setText(title);
        }
        if (miniPlayerArtist != null && artist != null && !artist.isEmpty()) {
            miniPlayerArtist.setText(artist);
        }
        if (miniPlayerThumbnail != null && thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            Glide.with(activity)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .into(miniPlayerThumbnail);
        }
    }

    public void updatePlayState(boolean isPlaying) {
        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    public void updateProgress(int currentTime, int duration) {
        if (miniPlayerProgress != null && duration > 0) {
            int percent = (currentTime * 100) / duration;
            miniPlayerProgress.setProgress(percent);
        }
    }
}
