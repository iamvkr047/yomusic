package com.iamvkryt.yomusic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.models.Playlist;
import com.iamvkryt.yomusic.models.SearchResultItem;
import com.iamvkryt.yomusic.models.VideoItem;

import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ResultViewHolder> {

    public interface OnResultClickListener {
        void onVideoClick(VideoItem video);
        void onPlaylistClick(Playlist playlist);
    }

    private final List<SearchResultItem> items;
    private final OnResultClickListener listener;

    public SearchResultsAdapter(List<SearchResultItem> items, OnResultClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ResultViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        SearchResultItem item = items.get(position);

        if (item.getType() == SearchResultItem.Type.VIDEO && item.getVideo() != null) {
            VideoItem v = item.getVideo();
            holder.tvTitle.setText(v.getTitle());
            holder.tvSubtitle.setText(v.getChannel());
            holder.tvBadge.setText("Song");
            holder.ivIcon.setImageResource(R.drawable.ic_music_note);
            // Load thumbnail from YouTube
            String thumbUrl = "https://i.ytimg.com/vi/" + v.getVideoId() + "/hqdefault.jpg";
            Glide.with(holder.ivIcon.getContext())
                    .load(thumbUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .into(holder.ivIcon);
            holder.itemView.setOnClickListener(vv -> listener.onVideoClick(v));

        } else if (item.getType() == SearchResultItem.Type.PLAYLIST && item.getPlaylist() != null) {
            Playlist p = item.getPlaylist();
            holder.tvTitle.setText(p.getTitle());
            holder.tvSubtitle.setText(p.getChannel());
            holder.tvBadge.setText("Playlist");
            if (p.getThumbnail() != null && !p.getThumbnail().isEmpty()) {
                Glide.with(holder.ivIcon.getContext())
                        .load(p.getThumbnail())
                        .placeholder(R.drawable.ic_playlist)
                        .into(holder.ivIcon);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_playlist);
            }
            holder.itemView.setOnClickListener(vv -> listener.onPlaylistClick(p));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvBadge;

        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_result_icon);
            tvTitle = itemView.findViewById(R.id.tv_result_title);
            tvSubtitle = itemView.findViewById(R.id.tv_result_subtitle);
            tvBadge = itemView.findViewById(R.id.tv_result_badge);
        }
    }
}
