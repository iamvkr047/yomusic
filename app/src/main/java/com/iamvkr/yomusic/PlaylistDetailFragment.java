package com.iamvkr.yomusic;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.iamvkr.yomusic.api.PlaylistDataParser;
import com.iamvkr.yomusic.api.YtRepository;
import com.iamvkr.yomusic.models.Playlist;
import com.iamvkr.yomusic.models.VideoItem;
import com.iamvkr.yomusic.adapters.PlaylistVideoAdapter;
import com.iamvkr.yomusic.storage.PlaylistStorage;
import com.iamvkr.yomusic.storage.PlaylistSessionCache;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailFragment extends Fragment {

    private static final String ARG_PLAYLIST_ID = "arg_playlist_id";

    private String playlistId;
    private Playlist loadedPlaylist;
    private JSONObject cachedContext;
    private String cachedApiToken;

    private View loadingContainer;
    private View errorContainer;
    private View contentScroll;
    private ShapeableImageView detailThumbnail;
    private TextView detailTitle;
    private TextView detailChannelTotal;
    private MaterialButton btnPlayAll;
    private ImageButton btnFavoriteToggle;
    private ImageButton btnShare;
    private RecyclerView rvPlaylistVideos;
    private MaterialButton btnLoadMore;
    private View loadNextProgress;

    private PlaylistVideoAdapter videoAdapter;
    private final List<VideoItem> videoList = new ArrayList<>();

    private YtRepository repository;
    private PlaylistStorage storage;
    private boolean isSaved = false;

    public static PlaylistDetailFragment newInstance(String playlistId) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAYLIST_ID, playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlistId = getArguments().getString(ARG_PLAYLIST_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingContainer = view.findViewById(R.id.loading_container);
        errorContainer = view.findViewById(R.id.error_container);
        contentScroll = view.findViewById(R.id.content_scroll);
        detailThumbnail = view.findViewById(R.id.detail_thumbnail);
        detailTitle = view.findViewById(R.id.detail_title);
        detailChannelTotal = view.findViewById(R.id.detail_channel_total);
        btnPlayAll = view.findViewById(R.id.btn_play_all);
        btnFavoriteToggle = view.findViewById(R.id.btn_favorite_toggle);
        btnShare = view.findViewById(R.id.btn_share);
        rvPlaylistVideos = view.findViewById(R.id.rv_playlist_videos);
        btnLoadMore = view.findViewById(R.id.btn_load_more);
        loadNextProgress = view.findViewById(R.id.load_next_progress);

        repository = new YtRepository(requireContext());
        storage = new PlaylistStorage(requireContext());

        rvPlaylistVideos.setLayoutManager(new LinearLayoutManager(getContext()));
        videoAdapter = new PlaylistVideoAdapter(videoList, new PlaylistVideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(VideoItem video, int position) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playPlaylistQueue(videoList, position);
                }
            }

            @Override
            public void onVideoLongClick(VideoItem video, int position) {
                // No long-press action for YouTube playlist videos
            }
        });
        rvPlaylistVideos.setAdapter(videoAdapter);

        checkIsSaved();

        btnFavoriteToggle.setOnClickListener(v -> toggleFavorite());
        btnShare.setOnClickListener(v -> sharePlaylist());
        btnPlayAll.setOnClickListener(v -> {
            if (!videoList.isEmpty() && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playPlaylistQueue(videoList, 0);
            }
        });

        btnLoadMore.setOnClickListener(v -> loadNextPage());

        fetchPlaylistData();
    }

    private void checkIsSaved() {
        List<Playlist> saved = storage.loadPlaylists();
        isSaved = false;
        for (Playlist p : saved) {
            if (p.getPlaylistId().equals(playlistId)) {
                isSaved = true;
                break;
            }
        }
        btnFavoriteToggle.setImageResource(isSaved ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }

    private void toggleFavorite() {
        if (loadedPlaylist == null) return;

        if (isSaved) {
            storage.removePlaylist(playlistId);
            isSaved = false;
            Toast.makeText(getContext(), "Removed from playlists", Toast.LENGTH_SHORT).show();
        } else {
            // For YT playlists, save with empty video list as per React spec
            Playlist toSave = new Playlist(
                    loadedPlaylist.getPlaylistId(),
                    loadedPlaylist.getTitle(),
                    loadedPlaylist.getThumbnail(),
                    loadedPlaylist.getChannel(),
                    false,
                    new ArrayList<>(),
                    "",
                    loadedPlaylist.getTotal()
            );
            storage.addPlaylist(toSave);
            isSaved = true;
            Toast.makeText(getContext(), "Saved to playlists", Toast.LENGTH_SHORT).show();
        }
        btnFavoriteToggle.setImageResource(isSaved ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }

    private void sharePlaylist() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Watch this playlist at https://www.youtube.com/playlist?list=" + playlistId);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Playlist"));
    }

    private void fetchPlaylistData() {
        PlaylistSessionCache.CacheEntry cachedEntry = PlaylistSessionCache.get(playlistId);
        if (cachedEntry != null) {
            displayPlaylistData(cachedEntry.playlist, cachedEntry.context, cachedEntry.apiToken);
            return;
        }

        loadingContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        contentScroll.setVisibility(View.GONE);

        repository.getPlaylistVideos(playlistId, new YtRepository.OnPlaylistCallback() {
            @Override
            public void onSuccess(Playlist playlist, JSONObject context, String apiToken) {
                if (!isAdded()) return;
                PlaylistSessionCache.put(playlistId, playlist, context, apiToken);
                displayPlaylistData(playlist, context, apiToken);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                loadingContainer.setVisibility(View.GONE);
                errorContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayPlaylistData(Playlist playlist, JSONObject context, String apiToken) {
        loadedPlaylist = playlist;
        cachedContext = context;
        cachedApiToken = apiToken;

        detailTitle.setText(playlist.getTitle());
        String channelText = playlist.getChannel() != null && !playlist.getChannel().isEmpty()
                ? playlist.getChannel() : "playlist";
        String totalText = playlist.getTotal() > 0 ? " • " + playlist.getTotal() + " Videos" : "";
        detailChannelTotal.setText(channelText + totalText);

        if (playlist.getThumbnail() != null && !playlist.getThumbnail().isEmpty()) {
            Glide.with(requireContext())
                    .load(playlist.getThumbnail())
                    .placeholder(R.drawable.ic_music_note)
                    .into(detailThumbnail);
        } else {
            detailThumbnail.setImageResource(R.drawable.ic_music_note);
        }

        videoList.clear();
        if (playlist.getVideoItems() != null) {
            videoList.addAll(playlist.getVideoItems());
        }
        videoAdapter.notifyDataSetChanged();

        if (playlist.getNextPage() != null && !playlist.getNextPage().isEmpty()) {
            btnLoadMore.setVisibility(View.VISIBLE);
        } else {
            btnLoadMore.setVisibility(View.GONE);
        }

        loadingContainer.setVisibility(View.GONE);
        contentScroll.setVisibility(View.VISIBLE);
    }

    private void loadNextPage() {
        if (loadedPlaylist == null || loadedPlaylist.getNextPage().isEmpty()) return;

        btnLoadMore.setVisibility(View.GONE);
        loadNextProgress.setVisibility(View.VISIBLE);

        repository.getPlaylistVideosNext(loadedPlaylist.getNextPage(), cachedContext, cachedApiToken, new YtRepository.OnPlaylistNextCallback() {
            @Override
            public void onSuccess(PlaylistDataParser.PlaylistNextResult result) {
                if (!isAdded()) return;
                loadNextProgress.setVisibility(View.GONE);

                if (result.items != null && !result.items.isEmpty()) {
                    int startPos = videoList.size();
                    videoList.addAll(result.items);
                    videoAdapter.notifyItemRangeInserted(startPos, result.items.size());
                    if (loadedPlaylist.getVideoItems() != null) {
                        loadedPlaylist.getVideoItems().addAll(result.items);
                    }
                }

                loadedPlaylist.setNextPage(result.nextPage);
                PlaylistSessionCache.put(playlistId, loadedPlaylist, cachedContext, cachedApiToken);

                if (result.nextPage != null && !result.nextPage.isEmpty()) {
                    btnLoadMore.setVisibility(View.VISIBLE);
                } else {
                    btnLoadMore.setVisibility(View.GONE);
                }
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
