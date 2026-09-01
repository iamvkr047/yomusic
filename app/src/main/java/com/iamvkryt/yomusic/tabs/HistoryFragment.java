package com.iamvkryt.yomusic.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iamvkryt.yomusic.MainActivity;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.adapters.HistoryAdapter;
import com.iamvkryt.yomusic.models.VideoItem;
import com.iamvkryt.yomusic.storage.HistoryStorage;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private View emptyStateContainer;
    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private final List<VideoItem> historyList = new ArrayList<>();
    private HistoryStorage historyStorage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        rvHistory = view.findViewById(R.id.rv_history);

        historyStorage = new HistoryStorage(requireContext());

        adapter = new HistoryAdapter(historyList, new HistoryAdapter.OnHistoryItemClickListener() {
            @Override
            public void onItemClick(VideoItem video, int position) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playPlaylistQueue(historyList, position);
                }
            }

            @Override
            public void onRemoveClick(VideoItem video, int position) {
                historyStorage.removeFromHistory(video.getVideoId());
                historyList.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, historyList.size());
                Toast.makeText(getContext(), "Removed from history", Toast.LENGTH_SHORT).show();

                if (historyList.isEmpty()) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                }
            }
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(adapter);

        loadHistoryData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadHistoryData();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadHistoryData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistoryData();
    }

    public void loadHistoryData() {
        if (historyStorage == null) return;
        List<VideoItem> saved = historyStorage.loadHistory();
        historyList.clear();
        historyList.addAll(saved);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (historyList.isEmpty()) {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.VISIBLE);
            if (rvHistory != null) rvHistory.setVisibility(View.GONE);
        } else {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);
            if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
        }
    }
}
