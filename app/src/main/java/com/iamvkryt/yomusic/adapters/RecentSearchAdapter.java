package com.iamvkryt.yomusic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iamvkryt.yomusic.R;

import java.util.List;

public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.RecentViewHolder> {

    public interface OnRecentSearchClickListener {
        void onRecentClick(String query);
        void onRecentRemove(String query);
    }

    private final List<String> recentSearches;
    private final OnRecentSearchClickListener listener;

    public RecentSearchAdapter(List<String> recentSearches, OnRecentSearchClickListener listener) {
        this.recentSearches = recentSearches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_search, parent, false);
        return new RecentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
        String query = recentSearches.get(position);
        holder.tvRecentSearch.setText(query);
        holder.itemView.setOnClickListener(v -> listener.onRecentClick(query));
        holder.btnRemoveRecent.setOnClickListener(v -> listener.onRecentRemove(query));
    }

    @Override
    public int getItemCount() { return recentSearches.size(); }

    static class RecentViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecentSearch;
        ImageButton btnRemoveRecent;

        RecentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecentSearch = itemView.findViewById(R.id.tv_recent_search);
            btnRemoveRecent = itemView.findViewById(R.id.btn_remove_recent);
        }
    }
}
