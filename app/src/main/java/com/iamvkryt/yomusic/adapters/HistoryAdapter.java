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
import com.iamvkryt.yomusic.models.VideoItem;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryItemClickListener {
        void onItemClick(VideoItem video, int position);
        void onRemoveClick(VideoItem video, int position);
    }

    private final List<VideoItem> videoList;
    private final OnHistoryItemClickListener listener;

    public HistoryAdapter(List<VideoItem> videoList, OnHistoryItemClickListener listener) {
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_video, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        VideoItem video = videoList.get(position);
        holder.bind(video, position, listener);
    }

    @Override
    public int getItemCount() {
        return videoList != null ? videoList.size() : 0;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView thumbnail;
        private final TextView title;
        private final TextView channel;
        private final ImageView btnRemove;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.video_thumbnail);
            title = itemView.findViewById(R.id.video_title);
            channel = itemView.findViewById(R.id.video_channel);
            btnRemove = itemView.findViewById(R.id.btn_remove_history);
        }

        public void bind(VideoItem video, int position, OnHistoryItemClickListener listener) {
            title.setText(video.getTitle());
            channel.setText(video.getChannel());

            String thumbUrl = "https://i.ytimg.com/vi/" + video.getVideoId() + "/mqdefault.jpg";
            Glide.with(itemView.getContext())
                    .load(thumbUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(thumbnail);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(video, position);
                }
            });

            if (btnRemove != null) {
                btnRemove.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRemoveClick(video, position);
                    }
                });
            }
        }
    }
}
