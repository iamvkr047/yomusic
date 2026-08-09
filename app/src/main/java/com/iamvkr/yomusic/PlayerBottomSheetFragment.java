package com.iamvkr.yomusic;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iamvkr.yomusic.models.Playlist;
import com.iamvkr.yomusic.models.VideoItem;
import com.iamvkr.yomusic.storage.FavoriteStorage;
import com.iamvkr.yomusic.storage.PlaylistStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerBottomSheetFragment extends BottomSheetDialogFragment {

    private ShapeableImageView playerArtwork;
    private TextView playerTitle;
    private TextView playerArtist;
    private Slider playerSlider;
    private TextView tvCurrentTime;
    private TextView tvTotalDuration;
    private FloatingActionButton btnPlayPause;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private ImageButton btnCollapse;
    private ImageButton btnPlayerFavorite;
    private ImageButton btnAddToPlaylist;
    private ImageButton btnVideoToggle;

    private boolean isUserSeeking = false;
    private FavoriteStorage favoriteStorage;
    private PlaylistStorage playlistStorage;

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
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnCollapse = view.findViewById(R.id.btn_collapse);
        playerArtwork = view.findViewById(R.id.player_artwork);
        playerTitle = view.findViewById(R.id.player_title);
        playerArtist = view.findViewById(R.id.player_artist);
        playerSlider = view.findViewById(R.id.player_slider);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvTotalDuration = view.findViewById(R.id.tv_total_duration);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnPrevious = view.findViewById(R.id.btn_previous);
        btnNext = view.findViewById(R.id.btn_next);
        btnPlayerFavorite = view.findViewById(R.id.btn_player_favorite);
        btnAddToPlaylist = view.findViewById(R.id.btn_add_to_playlist);
        btnVideoToggle = view.findViewById(R.id.btn_video_toggle);

        favoriteStorage = new FavoriteStorage(requireContext());
        playlistStorage = new PlaylistStorage(requireContext());

        btnCollapse.setOnClickListener(v -> dismiss());

        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            updateTrackInfo(activity.getCurrentTitle(), activity.getCurrentArtist(), activity.getCurrentThumbnailUrl());
            updatePlayState(activity.isPlaying());
            updateProgress(activity.getCurrentPosition(), activity.getDuration());

            btnPlayPause.setOnClickListener(v -> {
                if (activity.isPlaying()) {
                    activity.pauseVideo();
                } else {
                    activity.playVideo();
                }
            });

            btnNext.setOnClickListener(v -> activity.playNextSong());
            btnPrevious.setOnClickListener(v -> activity.playPreviousSong());

            btnAddToPlaylist.setOnClickListener(v -> handleAddToCustomPlaylist(activity));

            btnVideoToggle.setOnClickListener(v -> {
                VideoPlayerBottomSheetFragment videoSheet = new VideoPlayerBottomSheetFragment();
                videoSheet.show(getParentFragmentManager(), "VideoPlayerBottomSheetFragment");
            });

            btnPlayerFavorite.setOnClickListener(v -> {
                String videoId = activity.getCurrentVideoId();
                if (videoId == null || videoId.isEmpty()) return;

                VideoItem video = new VideoItem(
                        videoId,
                        activity.getCurrentTitle(),
                        activity.getCurrentArtist(),
                        activity.getDuration()
                );

                boolean isFav = favoriteStorage.toggleFavorite(video);
                btnPlayerFavorite.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                activity.notifyFavoriteChanged();
                android.widget.Toast.makeText(getContext(), isFav ? "Added to Liked Songs" : "Removed from Liked Songs", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        playerSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                tvCurrentTime.setText(formatTime((int) value));
            }
        });

        playerSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                isUserSeeking = false;
                if (activity != null) {
                    activity.seekTo((int) slider.getValue());
                }
            }
        });
    }

    public void updateTrackInfo(String title, String artist, String thumbnailUrl) {
        if (playerTitle != null) {
            playerTitle.setText(title != null ? title : "Unknown Title");
        }
        if (playerArtist != null) {
            playerArtist.setText(artist != null ? artist : "Yo Music");
        }
        if (playerArtwork != null && getContext() != null) {
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                Glide.with(getContext())
                        .load(thumbnailUrl)
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(playerArtwork);
            } else {
                playerArtwork.setImageResource(R.drawable.ic_music_note);
            }
        }

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null && favoriteStorage != null) {
            boolean isFav = favoriteStorage.isFavorite(activity.getCurrentVideoId());
            if (btnPlayerFavorite != null) {
                btnPlayerFavorite.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            }
        }
    }

    public void updatePlayState(boolean isPlaying) {
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    public void updateProgress(int currentTime, int duration) {
        if (isUserSeeking) return;

        if (playerSlider != null) {
            playerSlider.setValueTo(duration > 0 ? duration : 1.0f);
            playerSlider.setValue(Math.min(currentTime, duration));
        }
        if (tvCurrentTime != null) {
            tvCurrentTime.setText(formatTime(currentTime));
        }
        if (tvTotalDuration != null) {
            tvTotalDuration.setText(formatTime(duration));
        }
    }

    private void handleAddToCustomPlaylist(MainActivity activity) {
        if (activity == null || getContext() == null) return;
        String videoId = activity.getCurrentVideoId();
        if (videoId == null || videoId.isEmpty()) return;

        List<Playlist> allPlaylists = playlistStorage.loadPlaylists();
        List<Playlist> customPlaylists = new ArrayList<>();
        for (Playlist p : allPlaylists) {
            if (p.isCustomPlaylist() || "Custom list".equalsIgnoreCase(p.getChannel())) {
                customPlaylists.add(p);
            }
        }

        if (customPlaylists.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "Playlist not Found! Create new", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] titles = new CharSequence[customPlaylists.size()];
        for (int i = 0; i < customPlaylists.size(); i++) {
            titles[i] = customPlaylists.get(i).getTitle();
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select a Playlist to add")
                .setItems(titles, (dialog, which) -> {
                    Playlist targetPlaylist = customPlaylists.get(which);
                    List<VideoItem> items = targetPlaylist.getVideoItems();
                    if (items == null) {
                        items = new ArrayList<>();
                        targetPlaylist.setVideoItems(items);
                    }

                    for (VideoItem item : items) {
                        if (videoId.equals(item.getVideoId())) {
                            android.widget.Toast.makeText(getContext(), "Item Already Exist", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    VideoItem newVideo = new VideoItem(
                            videoId,
                            activity.getCurrentTitle(),
                            activity.getCurrentArtist(),
                            activity.getDuration()
                    );

                    items.add(newVideo);

                    for (int i = 0; i < allPlaylists.size(); i++) {
                        if (allPlaylists.get(i).getPlaylistId().equals(targetPlaylist.getPlaylistId())) {
                            allPlaylists.set(i, targetPlaylist);
                            break;
                        }
                    }
                    playlistStorage.savePlaylists(allPlaylists);
                    android.widget.Toast.makeText(getContext(), "Item Added", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs);
    }
}
