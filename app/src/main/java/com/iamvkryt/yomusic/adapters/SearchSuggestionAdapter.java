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

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
        void onSuggestionCopyToInput(String suggestion);
    }

    private final List<String> suggestions;
    private final OnSuggestionClickListener listener;

    public SearchSuggestionAdapter(List<String> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new SuggestionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        holder.tvSuggestion.setText(suggestion);
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(suggestion));
        holder.btnCopyToInput.setOnClickListener(v -> listener.onSuggestionCopyToInput(suggestion));
    }

    @Override
    public int getItemCount() { return suggestions.size(); }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSuggestion;
        ImageButton btnCopyToInput;

        SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSuggestion = itemView.findViewById(R.id.tv_suggestion);
            btnCopyToInput = itemView.findViewById(R.id.btn_copy_to_input);
        }
    }
}
