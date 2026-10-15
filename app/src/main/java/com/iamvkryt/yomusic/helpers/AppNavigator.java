package com.iamvkryt.yomusic.helpers;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.iamvkryt.yomusic.CustomPlaylistDetailFragment;
import com.iamvkryt.yomusic.PlayerBottomSheetFragment;
import com.iamvkryt.yomusic.PlaylistDetailFragment;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.SearchFragment;
import com.iamvkryt.yomusic.SearchResultsFragment;
import com.iamvkryt.yomusic.tabs.FavoriteFragment;
import com.iamvkryt.yomusic.tabs.HistoryFragment;
import com.iamvkryt.yomusic.tabs.HomeFragment;
import com.iamvkryt.yomusic.tabs.MoreFragment;
import com.iamvkryt.yomusic.tabs.PlaylistFragment;

public class AppNavigator {

    private final AppCompatActivity activity;
    private final FragmentManager fragmentManager;

    private HomeFragment homeFragment;
    private PlaylistFragment playlistFragment;
    private FavoriteFragment favoriteFragment;
    private MoreFragment moreFragment;
    private Fragment activeFragment;
    private PlayerBottomSheetFragment activePlayerBottomSheet;

    public AppNavigator(AppCompatActivity activity) {
        this.activity = activity;
        this.fragmentManager = activity.getSupportFragmentManager();
    }

    public void initMainTabs(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("home");
            playlistFragment = (PlaylistFragment) fragmentManager.findFragmentByTag("playlist");
            favoriteFragment = (FavoriteFragment) fragmentManager.findFragmentByTag("favorite");
            moreFragment = (MoreFragment) fragmentManager.findFragmentByTag("more");

            if (homeFragment == null) homeFragment = new HomeFragment();
            if (playlistFragment == null) playlistFragment = new PlaylistFragment();
            if (favoriteFragment == null) favoriteFragment = new FavoriteFragment();
            if (moreFragment == null) moreFragment = new MoreFragment();

            activeFragment = homeFragment;
            if (playlistFragment.isAdded() && !playlistFragment.isHidden()) {
                activeFragment = playlistFragment;
            } else if (favoriteFragment.isAdded() && !favoriteFragment.isHidden()) {
                activeFragment = favoriteFragment;
            } else if (moreFragment.isAdded() && !moreFragment.isHidden()) {
                activeFragment = moreFragment;
            }
        } else {
            homeFragment = new HomeFragment();
            playlistFragment = new PlaylistFragment();
            favoriteFragment = new FavoriteFragment();
            moreFragment = new MoreFragment();

            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, homeFragment, "home")
                    .commit();
            activeFragment = homeFragment;
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);
        if (bottomNav == null) return;

        // Prevent double bottom padding by disabling automatic inset handling
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> insets);

        // Hide bottom nav when any detail/overlay page is on the back stack
        fragmentManager.addOnBackStackChangedListener(() -> {
            int count = fragmentManager.getBackStackEntryCount();
            bottomNav.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String tag = "";
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = homeFragment;
                tag = "home";
            } else if (itemId == R.id.navigation_playlist) {
                selectedFragment = playlistFragment;
                tag = "playlist";
            } else if (itemId == R.id.navigation_favorite) {
                selectedFragment = favoriteFragment;
                tag = "favorite";
            } else if (itemId == R.id.navigation_more) {
                selectedFragment = moreFragment;
                tag = "more";
            }

            if (selectedFragment != null) {
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStackImmediate(
                            null,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }

                if (selectedFragment != activeFragment) {
                    androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                    if (!selectedFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, selectedFragment, tag);
                    }
                    transaction.hide(activeFragment).show(selectedFragment).commit();
                    activeFragment = selectedFragment;
                }
                return true;
            }
            return false;
        });
    }

    public boolean handleBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            return true;
        } else if (activeFragment != homeFragment) {
            BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottom_navigation);
            if (bottomNavigation != null) {
                bottomNavigation.setSelectedItemId(R.id.navigation_home);
            }
            return true;
        }
        return false;
    }

    public void openPlaylistDetail(String playlistId) {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlaylistDetailFragment.newInstance(playlistId))
                .addToBackStack(null)
                .commit();
    }

    public void openCustomPlaylistDetail(String playlistId, String title) {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CustomPlaylistDetailFragment.newInstance(playlistId))
                .addToBackStack(null)
                .commit();
    }

    public void openSearchPage() {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, new SearchFragment())
                .addToBackStack(null)
                .commit();
    }

    public void openSearchResults(String query) {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SearchResultsFragment.newInstance(query))
                .addToBackStack(null)
                .commit();
    }

    public void openHistoryPage() {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, new HistoryFragment())
                .addToBackStack(null)
                .commit();
    }

    public void notifyFavoriteChanged() {
        if (favoriteFragment != null && favoriteFragment.isAdded()) {
            favoriteFragment.loadFavoritesData();
        }
    }

    public void showFullPlayerSheet() {
        activePlayerBottomSheet = new PlayerBottomSheetFragment();
        activePlayerBottomSheet.show(fragmentManager, "PlayerBottomSheetFragment");
    }

    public void updatePlayerSheetTrackInfo(String title, String artist, String thumbnailUrl) {
        if (activePlayerBottomSheet != null && activePlayerBottomSheet.isAdded()) {
            activePlayerBottomSheet.updateTrackInfo(title, artist, thumbnailUrl);
        }
    }

    public void updatePlayerSheetPlayState(boolean isPlaying) {
        if (activePlayerBottomSheet != null && activePlayerBottomSheet.isAdded()) {
            activePlayerBottomSheet.updatePlayState(isPlaying);
        }
    }

    public void updatePlayerSheetProgress(int currentTime, int duration) {
        if (activePlayerBottomSheet != null && activePlayerBottomSheet.isAdded()) {
            activePlayerBottomSheet.updateProgress(currentTime, duration);
        }
    }
}
