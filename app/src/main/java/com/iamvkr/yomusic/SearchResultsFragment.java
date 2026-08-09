package com.iamvkr.yomusic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.iamvkr.yomusic.adapters.SearchResultsAdapter;
import com.iamvkr.yomusic.api.SearchDataParser;
import com.iamvkr.yomusic.api.YtRepository;
import com.iamvkr.yomusic.models.Playlist;
import com.iamvkr.yomusic.models.SearchResultItem;
import com.iamvkr.yomusic.models.VideoItem;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsFragment extends Fragment {

    private static final String ARG_QUERY = "arg_query";

    private String query;
    private JSONObject cachedContext;
    private String cachedApiToken;
    private String nextPageToken = "";

    private View loadingContainer;
    private View errorContainer;
    private View contentScroll;
    private TextView tvResultsTitle;
    private RecyclerView rvSearchResults;
    private MaterialButton btnLoadMore;
    private CircularProgressIndicator loadNextProgress;

    private SearchResultsAdapter adapter;
    private final List<SearchResultItem> resultList = new ArrayList<>();

    private YtRepository repository;

    public static SearchResultsFragment newInstance(String query) {
        SearchResultsFragment f = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            query = getArguments().getString(ARG_QUERY, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingContainer = view.findViewById(R.id.loading_container);
        errorContainer = view.findViewById(R.id.error_container);
        contentScroll = view.findViewById(R.id.content_scroll);
        tvResultsTitle = view.findViewById(R.id.tv_results_title);
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        btnLoadMore = view.findViewById(R.id.btn_load_more);
        loadNextProgress = view.findViewById(R.id.load_next_progress);

        repository = new YtRepository(requireContext());

        adapter = new SearchResultsAdapter(resultList, new SearchResultsAdapter.OnResultClickListener() {
            @Override
            public void onVideoClick(VideoItem video) {
                if (getActivity() instanceof MainActivity) {
                    // Build a single-song queue and play
                    List<VideoItem> queue = new ArrayList<>();
                    queue.add(video);
                    ((MainActivity) getActivity()).playPlaylistQueue(queue, 0);
                }
            }

            @Override
            public void onPlaylistClick(Playlist playlist) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openPlaylistDetail(playlist.getPlaylistId());
                }
            }
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(adapter);

        btnLoadMore.setOnClickListener(v -> loadNextPage());

        fetchSearchResults();
    }

    private void fetchSearchResults() {
        loadingContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        contentScroll.setVisibility(View.GONE);

        repository.search(query, new YtRepository.OnSearchCallback() {
            @Override
            public void onSuccess(SearchDataParser.SearchResult result, JSONObject context, String apiToken) {
                if (!isAdded()) return;
                cachedContext = context;
                cachedApiToken = apiToken;
                nextPageToken = result.nextPage;

                resultList.clear();
                resultList.addAll(result.contents);
                adapter.notifyDataSetChanged();

                tvResultsTitle.setText("Search results for '" + query + "' • " + resultList.size() + " items");
                btnLoadMore.setVisibility(!nextPageToken.isEmpty() ? View.VISIBLE : View.GONE);

                loadingContainer.setVisibility(View.GONE);
                contentScroll.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                loadingContainer.setVisibility(View.GONE);
                errorContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadNextPage() {
        if (nextPageToken.isEmpty()) return;
        btnLoadMore.setVisibility(View.GONE);
        loadNextProgress.setVisibility(View.VISIBLE);

        repository.getSearchNext(nextPageToken, cachedContext, cachedApiToken, new YtRepository.OnSearchNextCallback() {
            @Override
            public void onSuccess(SearchDataParser.SearchResult result) {
                if (!isAdded()) return;
                loadNextProgress.setVisibility(View.GONE);
                int start = resultList.size();
                resultList.addAll(result.contents);
                adapter.notifyItemRangeInserted(start, result.contents.size());
                nextPageToken = result.nextPage;
                btnLoadMore.setVisibility(!nextPageToken.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                loadNextProgress.setVisibility(View.GONE);
                btnLoadMore.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
