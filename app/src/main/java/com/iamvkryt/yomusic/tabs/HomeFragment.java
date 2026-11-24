package com.iamvkryt.yomusic.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.Song;
import com.iamvkryt.yomusic.MainActivity;
import com.iamvkryt.yomusic.adapters.SongAdapter;
import com.iamvkryt.yomusic.api.MusicDataParser;
import com.iamvkryt.yomusic.api.YtRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.iamvkryt.yomusic.storage.KeepListeningStorage;

public class HomeFragment extends Fragment {

    private static MusicDataParser.HomeFeedResult cachedHomeFeedResult = null;
    private static List<Song> cachedKeepListeningSongs = null;

    public static void invalidateKeepListeningCache() {
        cachedKeepListeningSongs = null;
    }

    private View loadingContainer;
    private View errorContainer;
    private View contentScroll;
    private LinearLayout sectionsContainer;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private YtRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingContainer = view.findViewById(R.id.loading_container);
        errorContainer = view.findViewById(R.id.error_container);
        contentScroll = view.findViewById(R.id.content_scroll);
        sectionsContainer = view.findViewById(R.id.sections_container);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        btnRetry = view.findViewById(R.id.btn_retry);

        repository = new YtRepository(requireContext());

        btnRetry.setOnClickListener(v -> loadHomeFeed(true));
        loadHomeFeed(false);
    }

    private void loadHomeFeed(boolean forceRefresh) {
        if (forceRefresh) {
            cachedKeepListeningSongs = null;
        }
        if (!forceRefresh && cachedHomeFeedResult != null) {
            displayHomeFeed(cachedHomeFeedResult);
            return;
        }

        showLoading();

        repository.getMusicHomeFeed(new YtRepository.OnHomeFeedCallback() {
            @Override
            public void onSuccess(MusicDataParser.HomeFeedResult result) {
                if (!isAdded()) return;

                if (result != null) {
                    if (result.musicSections != null) {
                        for (MusicDataParser.MusicSection section : result.musicSections) {
                            if (section.content != null) {
                                Collections.shuffle(section.content);
                            }
                        }
                    }
                    if (result.albumSections != null) {
                        for (MusicDataParser.AlbumSection section : result.albumSections) {
                            if (section.content != null) {
                                Collections.shuffle(section.content);
                            }
                        }
                    }
                }

                cachedHomeFeedResult = result;
                displayHomeFeed(result);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showError(message);
            }
        });
    }

    private void displayHomeFeed(MusicDataParser.HomeFeedResult result) {
        sectionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Music video sections
        for (MusicDataParser.MusicSection section : result.musicSections) {
            addSectionHeader(section.title);

            List<Song> songs = new ArrayList<>();
            for (MusicDataParser.VideoItem item : section.content) {
                String thumbnailUrl = "https://img.youtube.com/vi/" + item.videoId + "/hqdefault.jpg";
                songs.add(new Song(item.videoId, item.title, "", thumbnailUrl));
            }

            RecyclerView recyclerView = new RecyclerView(requireContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            recyclerView.setClipToPadding(false);

            SongAdapter adapter = new SongAdapter(songs, song -> {
                if (getActivity() instanceof MainActivity) {
                    List<com.iamvkryt.yomusic.models.VideoItem> videoQueue = new ArrayList<>();
                    for (Song s : songs) {
                        videoQueue.add(new com.iamvkryt.yomusic.models.VideoItem(s.getVideoId(), s.getTitle(), s.getArtist(), 0));
                    }
                    int clickIndex = songs.indexOf(song);
                    if (clickIndex == -1) clickIndex = 0;
                    ((MainActivity) getActivity()).playPlaylistQueue(videoQueue, clickIndex);
                }
            });
            recyclerView.setAdapter(adapter);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dpToPx(24));
            recyclerView.setLayoutParams(params);

            sectionsContainer.addView(recyclerView);
        }

        // Keep listening section from qualified recommendations (cached per app session)
        if (cachedKeepListeningSongs == null) {
            KeepListeningStorage keepListeningStorage = new KeepListeningStorage(requireContext());
            List<Song> loaded = keepListeningStorage.getTopRecommendations(10);
            if (!loaded.isEmpty()) {
                Collections.shuffle(loaded);
                cachedKeepListeningSongs = loaded;
            } else {
                cachedKeepListeningSongs = new ArrayList<>();
            }
        }

        if (cachedKeepListeningSongs != null && !cachedKeepListeningSongs.isEmpty()) {
            addSectionHeader("Keep listening");
            List<Song> keepListeningSongs = cachedKeepListeningSongs;

            RecyclerView historyRecycler = new RecyclerView(requireContext());
            historyRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            historyRecycler.setClipToPadding(false);

            SongAdapter historyAdapter = new SongAdapter(keepListeningSongs, song -> {
                if (getActivity() instanceof MainActivity) {
                    List<com.iamvkryt.yomusic.models.VideoItem> videoQueue = new ArrayList<>();
                    for (Song s : keepListeningSongs) {
                        videoQueue.add(new com.iamvkryt.yomusic.models.VideoItem(s.getVideoId(), s.getTitle(), s.getArtist(), 0));
                    }
                    int clickIndex = keepListeningSongs.indexOf(song);
                    if (clickIndex == -1) clickIndex = 0;
                    ((MainActivity) getActivity()).playPlaylistQueue(videoQueue, clickIndex);
                }
            });
            historyRecycler.setAdapter(historyAdapter);

            LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            historyParams.setMargins(0, 0, 0, dpToPx(24));
            historyRecycler.setLayoutParams(historyParams);

            sectionsContainer.addView(historyRecycler);
        }

        // Album/Playlist sections
        for (MusicDataParser.AlbumSection section : result.albumSections) {
            addSectionHeader(section.title);

            List<Song> albumSongs = new ArrayList<>();
            for (MusicDataParser.AlbumItem item : section.content) {
                String thumbnail = item.thumbnail.isEmpty()
                        ? "https://img.youtube.com/vi/default/hqdefault.jpg"
                        : item.thumbnail;
                albumSongs.add(new Song(item.playlistId, item.title, "", thumbnail));
            }

            RecyclerView recyclerView = new RecyclerView(requireContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            recyclerView.setClipToPadding(false);

            SongAdapter adapter = new SongAdapter(albumSongs, song -> {
                // Album/Playlist click -> navigate to PlaylistDetailFragment
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openPlaylistDetail(song.getVideoId());
                }
            });
            recyclerView.setAdapter(adapter);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dpToPx(24));
            recyclerView.setLayoutParams(params);

            sectionsContainer.addView(recyclerView);
        }

        showContent();
    }

    private void addSectionHeader(String title) {
        TextView header = new TextView(requireContext());
        header.setText(title);
        header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(12));
        header.setLayoutParams(params);

        sectionsContainer.addView(header);
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        contentScroll.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        contentScroll.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        loadingContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        contentScroll.setVisibility(View.GONE);
        tvErrorMessage.setText(message);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
