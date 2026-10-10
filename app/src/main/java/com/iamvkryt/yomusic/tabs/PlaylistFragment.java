package com.iamvkryt.yomusic.tabs;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.MainActivity;
import com.iamvkryt.yomusic.adapters.PlaylistAdapter;
import com.iamvkryt.yomusic.api.YtRepository;
import com.iamvkryt.yomusic.models.Playlist;
import com.iamvkryt.yomusic.storage.PlaylistStorage;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment {

    private EditText etSearchPlaylist;
    private View emptyStateContainer;
    private RecyclerView rvPlaylists;

    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlists = new ArrayList<>();

    private PlaylistStorage storage;
    private YtRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearchPlaylist = view.findViewById(R.id.et_search_playlist);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        rvPlaylists = view.findViewById(R.id.rv_playlists);

        storage = new PlaylistStorage(requireContext());
        repository = new YtRepository(requireContext());

        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        playlistAdapter = new PlaylistAdapter(playlists, new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                if (playlist.isCustomPlaylist()) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openCustomPlaylistDetail(playlist.getPlaylistId(), playlist.getTitle());
                    }
                } else {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openPlaylistDetail(playlist.getPlaylistId());
                    }
                }
            }

            @Override
            public void onPlaylistLongClick(Playlist playlist) {
                showRemoveDialog(playlist);
            }
        });
        rvPlaylists.setAdapter(playlistAdapter);

        etSearchPlaylist.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                handleAddPlaylist();
                return true;
            }
            return false;
        });

        loadPlaylists();
    }

    private void loadPlaylists() {
        playlists.clear();
        playlists.addAll(storage.loadPlaylists());
        updateUI();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadPlaylists();
        }
    }

    private void updateUI() {
        if (playlists.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            rvPlaylists.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            rvPlaylists.setVisibility(View.VISIBLE);
        }
        playlistAdapter.notifyDataSetChanged();
    }

    private void handleAddPlaylist() {
        String searchQuery = etSearchPlaylist.getText().toString().trim();
        if (searchQuery.isEmpty()) {
            Toast.makeText(getContext(), "Values cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (searchQuery.contains("https://")) {
            repository.validatePlaylistLink(searchQuery, new YtRepository.OnPlaylistValidateCallback() {
                @Override
                public void onSuccess(Playlist newPl) {
                    if (!isAdded()) return;
                    for (Playlist p : playlists) {
                        if (p.getPlaylistId().equals(newPl.getPlaylistId())) {
                            Toast.makeText(getContext(), "Already Exist!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    storage.addPlaylist(newPl);
                    playlists.add(0, newPl);
                    updateUI();
                    etSearchPlaylist.setText("");
                    Toast.makeText(getContext(), "Playlist Added!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            for (Playlist p : playlists) {
                if (p.getTitle().trim().equalsIgnoreCase(searchQuery)) {
                    Toast.makeText(getContext(), "Already Exist!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Playlist customPl = new Playlist(
                    String.valueOf(System.currentTimeMillis()),
                    searchQuery,
                    "",
                    "Custom list",
                    true,
                    new ArrayList<>(),
                    "",
                    0
            );

            storage.addPlaylist(customPl);
            playlists.add(0, customPl);
            updateUI();
            etSearchPlaylist.setText("");
            Toast.makeText(getContext(), "Playlist Added", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRemoveDialog(Playlist playlist) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove")
                .setMessage("Are you sure Remove this item?")
                .setPositiveButton("Ok", (dialog, which) -> {
                    storage.removePlaylist(playlist.getPlaylistId());
                    playlists.remove(playlist);
                    updateUI();
                    Toast.makeText(getContext(), "item removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
