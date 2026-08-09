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
import com.iamvkryt.yomusic.models.VideoItem;

import java.util.List;

public class PlaylistVideoAdapter extends RecyclerView.Adapter<PlaylistVideoAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(VideoItem video, int position);
        void onVideoLongClick(VideoItem video, int position);
    }

    private final List<VideoItem> videoList;
    private final OnVideoClickListener listener;

    public PlaylistVideoAdapter(List<VideoItem> videoList, OnVideoClickListener listener) {
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videoList.get(position);
        holder.bind(video, position, listener);
    }

    @Override
    public int getItemCount() {
        return videoList != null ? videoList.size() : 0;
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView thumbnail;
        private final TextView title;
        private final TextView channel;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.video_thumbnail);
            title = itemView.findViewById(R.id.video_title);
            channel = itemView.findViewById(R.id.video_channel);
        }

        public void bind(VideoItem video, int position, OnVideoClickListener listener) {
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
                    listener.onVideoClick(video, position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onVideoLongClick(video, position);
                    return true;
                }
                return false;
            });
        }
    }
}
