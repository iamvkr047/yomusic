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
import com.iamvkryt.yomusic.storage.KeepListeningStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen displaying all qualified "Keep Listening" songs / suggestions,
 * allowing the user to browse, play, or remove them.
 */
public class ManageSuggestionsFragment extends Fragment {

    private View emptyStateContainer;
    private RecyclerView rvSuggestions;
    private HistoryAdapter adapter;
    private final List<VideoItem> suggestionsList = new ArrayList<>();
    private KeepListeningStorage keepListeningStorage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_suggestions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        rvSuggestions = view.findViewById(R.id.rv_suggestions);

        keepListeningStorage = new KeepListeningStorage(requireContext());

        adapter = new HistoryAdapter(suggestionsList, new HistoryAdapter.OnHistoryItemClickListener() {
            @Override
            public void onItemClick(VideoItem video, int position) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playPlaylistQueue(suggestionsList, position);
                }
            }

            @Override
            public void onRemoveClick(VideoItem video, int position) {
                keepListeningStorage.removeFromKeepListening(video.getVideoId());
                HomeFragment.invalidateKeepListeningCache();

                suggestionsList.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, suggestionsList.size());
                Toast.makeText(getContext(), "Removed from suggestions", Toast.LENGTH_SHORT).show();

                if (suggestionsList.isEmpty()) {
                    if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.VISIBLE);
                    if (rvSuggestions != null) rvSuggestions.setVisibility(View.GONE);
                }
            }
        });

        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSuggestions.setAdapter(adapter);

        loadSuggestionsData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadSuggestionsData();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadSuggestionsData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSuggestionsData();
    }

    public void loadSuggestionsData() {
        if (keepListeningStorage == null) return;
        List<VideoItem> items = keepListeningStorage.loadAllKeepListening();
        suggestionsList.clear();
        suggestionsList.addAll(items);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (suggestionsList.isEmpty()) {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.VISIBLE);
            if (rvSuggestions != null) rvSuggestions.setVisibility(View.GONE);
        } else {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);
            if (rvSuggestions != null) rvSuggestions.setVisibility(View.VISIBLE);
        }
    }
}
