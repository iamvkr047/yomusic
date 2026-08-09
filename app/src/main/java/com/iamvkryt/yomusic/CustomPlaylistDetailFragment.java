package com.iamvkryt.yomusic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.iamvkryt.yomusic.models.Playlist;
import com.iamvkryt.yomusic.models.VideoItem;
import com.iamvkryt.yomusic.adapters.PlaylistVideoAdapter;
import com.iamvkryt.yomusic.storage.PlaylistStorage;

import java.util.ArrayList;
import java.util.List;

public class CustomPlaylistDetailFragment extends Fragment {

    private static final String ARG_PLAYLIST_ID = "arg_playlist_id";

    private String playlistId;
    private Playlist customPlaylist;

    private TextView customDetailTitle;
    private TextView customDetailSubtitle;
    private MaterialButton btnCustomPlay;
    private RecyclerView rvCustomPlaylistVideos;

    private PlaylistVideoAdapter videoAdapter;
    private final List<VideoItem> videoList = new ArrayList<>();

    private PlaylistStorage storage;

    public static CustomPlaylistDetailFragment newInstance(String playlistId) {
        CustomPlaylistDetailFragment fragment = new CustomPlaylistDetailFragment();
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
        return inflater.inflate(R.layout.fragment_custom_playlist_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        customDetailTitle = view.findViewById(R.id.custom_detail_title);
        customDetailSubtitle = view.findViewById(R.id.custom_detail_subtitle);
        btnCustomPlay = view.findViewById(R.id.btn_custom_play);
        rvCustomPlaylistVideos = view.findViewById(R.id.rv_custom_playlist_videos);

        storage = new PlaylistStorage(requireContext());

        rvCustomPlaylistVideos.setLayoutManager(new LinearLayoutManager(getContext()));
        videoAdapter = new PlaylistVideoAdapter(videoList, new PlaylistVideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(VideoItem video, int position) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playPlaylistQueue(videoList, position);
                }
            }

            @Override
            public void onVideoLongClick(VideoItem video, int position) {
                showRemoveVideoDialog(video, position);
            }
        });
        rvCustomPlaylistVideos.setAdapter(videoAdapter);

        btnCustomPlay.setOnClickListener(v -> {
            if (!videoList.isEmpty() && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playPlaylistQueue(videoList, 0);
            }
        });

        loadCustomPlaylist();
    }

    private void loadCustomPlaylist() {
        List<Playlist> playlists = storage.loadPlaylists();
        customPlaylist = null;
        for (Playlist p : playlists) {
            if (p.getPlaylistId().equals(playlistId)) {
                customPlaylist = p;
                break;
            }
        }

        if (customPlaylist != null) {
            customDetailTitle.setText(customPlaylist.getTitle());
            videoList.clear();
            if (customPlaylist.getVideoItems() != null) {
                videoList.addAll(customPlaylist.getVideoItems());
            }
            updateSubtitle();
            videoAdapter.notifyDataSetChanged();
        }
    }

    private void updateSubtitle() {
        customDetailSubtitle.setText("Custom Playlist • " + videoList.size() + " Videos");
    }

    private void showRemoveVideoDialog(VideoItem video, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove")
                .setMessage("Are you sure Remove this item?")
                .setPositiveButton("Ok", (dialog, which) -> {
                    if (position >= 0 && position < videoList.size()) {
                        videoList.remove(position);
                        videoAdapter.notifyItemRemoved(position);
                        updateSubtitle();

                        if (customPlaylist != null) {
                            customPlaylist.setVideoItems(new ArrayList<>(videoList));
                            List<Playlist> playlists = storage.loadPlaylists();
                            for (int i = 0; i < playlists.size(); i++) {
                                if (playlists.get(i).getPlaylistId().equals(playlistId)) {
                                    playlists.set(i, customPlaylist);
                                    break;
                                }
                            }
                            storage.savePlaylists(playlists);
                        }
                        Toast.makeText(getContext(), "item deleted!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
