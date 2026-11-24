package com.iamvkryt.yomusic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.imageview.ShapeableImageView;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.Song;

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    public interface OnQueueItemClickListener {
        void onQueueItemClick(Song song, int position);
    }

    private final List<Song> queueList;
    private int currentIndex;
    private final OnQueueItemClickListener listener;

    private int colorPrimary = 0;
    private int colorOnSurface = 0;

    public QueueAdapter(List<Song> queueList, int currentIndex, OnQueueItemClickListener listener) {
        this.queueList = queueList;
        this.currentIndex = currentIndex;
        this.listener = listener;
    }

    public void updateCurrentIndex(int newIndex) {
        int oldIndex = this.currentIndex;
        this.currentIndex = newIndex;
        if (oldIndex >= 0 && oldIndex < getItemCount()) {
            notifyItemChanged(oldIndex);
        }
        if (newIndex >= 0 && newIndex < getItemCount()) {
            notifyItemChanged(newIndex);
        }
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (colorPrimary == 0) {
            colorPrimary = MaterialColors.getColor(parent, androidx.appcompat.R.attr.colorPrimary);
            colorOnSurface = MaterialColors.getColor(parent, com.google.android.material.R.attr.colorOnSurface);
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_song, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Song song = queueList.get(position);
        boolean isCurrent = (position == currentIndex);
        holder.bind(song, position, isCurrent, colorPrimary, colorOnSurface, listener);
    }

    @Override
    public int getItemCount() {
        return queueList != null ? queueList.size() : 0;
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView thumbnail;
        private final TextView title;
        private final TextView artist;
        private final ImageView playingIndicator;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.queue_song_thumbnail);
            title = itemView.findViewById(R.id.queue_song_title);
            artist = itemView.findViewById(R.id.queue_song_artist);
            playingIndicator = itemView.findViewById(R.id.queue_playing_indicator);
        }

        public void bind(Song song, int position, boolean isCurrent, int colorPrimary, int colorOnSurface, OnQueueItemClickListener listener) {
            title.setText(song.getTitle());
            artist.setText(song.getArtist());

            if (isCurrent) {
                title.setTextColor(colorPrimary);
                playingIndicator.setVisibility(View.VISIBLE);
            } else {
                title.setTextColor(colorOnSurface);
                playingIndicator.setVisibility(View.GONE);
            }

            String thumbUrl = song.getThumbnailUrl();
            if (thumbUrl == null || thumbUrl.isEmpty()) {
                thumbUrl = "https://i.ytimg.com/vi/" + song.getVideoId() + "/mqdefault.jpg";
            }

            Glide.with(itemView.getContext())
                    .load(thumbUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(thumbnail);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQueueItemClick(song, position);
                }
            });
        }
    }
}
