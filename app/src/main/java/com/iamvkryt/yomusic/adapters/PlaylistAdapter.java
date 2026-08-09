package com.iamvkryt.yomusic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.models.Playlist;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
        void onPlaylistLongClick(Playlist playlist);
    }

    private final List<Playlist> playlistList;
    private final OnPlaylistClickListener listener;

    public PlaylistAdapter(List<Playlist> playlistList, OnPlaylistClickListener listener) {
        this.playlistList = playlistList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        holder.bind(playlist, listener);
    }

    @Override
    public int getItemCount() {
        return playlistList != null ? playlistList.size() : 0;
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView thumbnail;
        private final View customBg;
        private final ImageView customIcon;
        private final TextView title;
        private final TextView subtitle;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.playlist_thumbnail);
            customBg = itemView.findViewById(R.id.custom_playlist_bg);
            customIcon = itemView.findViewById(R.id.ic_custom_playlist);
            title = itemView.findViewById(R.id.playlist_title);
            subtitle = itemView.findViewById(R.id.playlist_subtitle);
        }

        public void bind(Playlist playlist, OnPlaylistClickListener listener) {
            title.setText(playlist.getTitle());
            subtitle.setText(playlist.getChannel() != null && !playlist.getChannel().isEmpty() ? playlist.getChannel() : "playlist");

            if (playlist.isCustomPlaylist()) {
                thumbnail.setVisibility(View.GONE);
                customBg.setVisibility(View.VISIBLE);
                customIcon.setVisibility(View.VISIBLE);
            } else {
                customBg.setVisibility(View.GONE);
                customIcon.setVisibility(View.GONE);
                thumbnail.setVisibility(View.VISIBLE);

                if (playlist.getThumbnail() != null && !playlist.getThumbnail().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(playlist.getThumbnail())
                            .placeholder(R.drawable.ic_music_note)
                            .error(R.drawable.ic_music_note)
                            .into(thumbnail);
                } else {
                    thumbnail.setImageResource(R.drawable.ic_music_note);
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistLongClick(playlist);
                    return true;
                }
                return false;
            });
        }
    }
}
