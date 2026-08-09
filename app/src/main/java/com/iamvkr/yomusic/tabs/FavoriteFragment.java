package com.iamvkr.yomusic.tabs;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iamvkr.yomusic.R;
import com.iamvkr.yomusic.MainActivity;
import com.iamvkr.yomusic.adapters.PlaylistVideoAdapter;
import com.iamvkr.yomusic.models.VideoItem;
import com.iamvkr.yomusic.storage.FavoriteStorage;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {

    private View emptyStateContainer;
    private RecyclerView rvFavorites;
    private PlaylistVideoAdapter adapter;
    private final List<VideoItem> favoriteList = new ArrayList<>();
    private FavoriteStorage favoriteStorage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        rvFavorites = view.findViewById(R.id.rv_favorites);

        favoriteStorage = new FavoriteStorage(requireContext());

        adapter = new PlaylistVideoAdapter(favoriteList, new PlaylistVideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(VideoItem video, int position) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playPlaylistQueue(favoriteList, position);
                }
            }

            @Override
            public void onVideoLongClick(VideoItem video, int position) {
                showRemoveFavoriteDialog(video, position);
            }
        });

        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(adapter);

        loadFavoritesData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadFavoritesData();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadFavoritesData();
    }

    public void loadFavoritesData() {
        List<VideoItem> saved = favoriteStorage.loadFavorites();
        favoriteList.clear();
        favoriteList.addAll(saved);
        adapter.notifyDataSetChanged();

        if (favoriteList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.VISIBLE);
        }
    }

    private void showRemoveFavoriteDialog(VideoItem video, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove from Favorites?")
                .setMessage("Are you sure you want to remove '" + video.getTitle() + "' from your liked songs?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    favoriteStorage.removeFavorite(video.getVideoId());
                    favoriteList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, favoriteList.size());
                    Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();

                    if (favoriteList.isEmpty()) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        rvFavorites.setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
