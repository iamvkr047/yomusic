package com.iamvkryt.yomusic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.Song;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    private final List<Song> songList;
    private final OnSongClickListener listener;

    public SongAdapter(List<Song> songList, OnSongClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.bind(song, listener);
    }

    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView songThumbnail;
        private final TextView songTitle;
        private final TextView songArtist;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songThumbnail = itemView.findViewById(R.id.song_thumbnail);
            songTitle = itemView.findViewById(R.id.song_title);
            songArtist = itemView.findViewById(R.id.song_artist);
        }

        public void bind(Song song, OnSongClickListener listener) {
            songTitle.setText(song.getTitle());
            songArtist.setText(song.getArtist());

            if (song.getThumbnailUrl() != null && !song.getThumbnailUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(song.getThumbnailUrl())
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(songThumbnail);
            } else {
                songThumbnail.setImageResource(R.drawable.ic_music_note);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSongClick(song);
                }
            });
        }
    }
}
