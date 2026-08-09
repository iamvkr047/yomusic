## Progress File

### GOAL  - BUILDING YOUTUBE MUSIC PLAYER
- using yt iframe api and webview for player
- communicating of yt player to native components using js interface bridge.

reference : take reference from react_refrence folder that contain react js code that we have to functionally and visually clone it.

### Approach - 
step by step build one feature at a time.
need approval and fixes after each step/phase, to move on next phase.

### Current progress:
- Phase 1: Completed setup of YouTube WebView playback engine, custom vector drawables, Material 3 bottom navigation bar and active fragment switching logic.
- Phase 2: Completed persistent Mini Player Bar, Full-screen Player BottomSheet UI (Material 3 Slider), real-time JavaScript bridge progress & state synchronization, and HomeFragment sample music card carousels with instant tap-to-play playback.
- Phase 3: Completed YouTube Data Engine - live home feed scraping from YouTube Music channel page using OkHttp + JSONObject parser with SharedPreferences caching (4hr expiry). Created api/ package with YtInitDataExtractor, MusicDataParser, and YtRepository. HomeFragment now dynamically populates from live YouTube data.
- Phase 4: Completed Playlist functionality (Playlist tab page matching design reference, full support for both user-created custom playlists and actual YouTube playlists, oEmbed playlist link validation, `PlaylistDataParser`, SharedPreferences persistence, `PlaylistDetailFragment` with header, video list, save/share actions & pagination continuation, and `CustomPlaylistDetailFragment` with long-press item removal). Albums on Home feed now open YouTube playlist view.

- Phase 5: Completed Search functionality (SearchFragment with 600ms debounced autocomplete, recent search history, SearchResultsFragment with video/playlist results, pagination via `getSearchNext`) and Country/Region selector in MoreFragment (108 countries, SharedPreferences persistence, automatic cache eviction). Fixed tab navigation backstack overlapping and edge-to-edge system window inset handling.

- Phase 6: Completed Favorite / Liked Songs functionality (`FavoriteStorage` SharedPreferences persistence, `FavoriteFragment` with list binding and long-press Material 3 delete confirmation dialog, empty state view, `onHiddenChanged` tab lifecycle sync, real-time heart toggle in expanded `PlayerBottomSheetFragment`, and `notifyFavoriteChanged` instant update callback).

- Phase 7: Completed Custom Playlist Integration & Single-Row Expanded Player Controls (Rearranged bottom sheet player controls to `<plus>` `<prev>` `<play/pause>` `<next>` `<favorite>`, implemented `ic_add` vector drawable, Material 3 Custom Playlist chooser dialog on plus click, duplicate prevention check, persistent `PlaylistStorage` saving, and seamless local video list display in `CustomPlaylistDetailFragment`).

- Phase 8: Completed Media Notification with playback controls (Play/Pause, Next, Previous), live track title, artist name, and dynamic Glide thumbnail binding. Integrated `androidx.media` dependency (`1.7.0`), initialized `MediaSessionCompat`, created `MediaButtonReceiver` broadcast receiver registered in `AndroidManifest.xml`, handled runtime `POST_NOTIFICATIONS` permission for Android 13+, and bound notification actions directly to `MainActivity.handleNotificationAction`.

- Phase 9:
  - **Part 1 (Import/Export "Your Data"):** Implemented settings item in `MoreFragment` to backup and restore playlists/favorites using Android's Storage Access Framework (`CreateDocument` and `OpenDocument`) for permission-less backups via custom `.ymb` (JSON format) files.
  - **Part 2 (App Updates):** Added "Check for Updates" item in `MoreFragment`. Fetches the latest release from the GitHub releases API asynchronously, performs safe SemVer tag comparison (`compareVersions`), and prompts users to open the download URL in their external web browser if a newer version is available.
  - **Part 3 (Privacy Policy):** Added "Privacy Policy" item in `MoreFragment`. Clicking it opens a Material 3 dialog presenting the full privacy policy document inside a scrollable view with a tiny text layout.

### Improvments:
- used Material alert dialog AlertDialog in PlaylistFragment.java and CustomPlaylistDetailFragment.java (for the long press playlist/item remove dialog) 
- Updated `item_song.xml` to use `match_parent` height, making horizontal list cards even in the home feed.
- Increased title margin start in `activity_main.xml` to give more gap between the logo and title in the app bar.
- Implemented full-screen video playback capability in the expanded player bottom sheet via a video toggle icon. The layout features a translucent backdrop overlay (`#B3000000` - 70% opacity black) with a centered `MaterialCardView` (`250dp` height with `16dp` rounded corners). Configured a custom `MyWebView` component to override visibility queries (intercepting window visibility changes to always report `View.VISIBLE`) and `onDetachedFromWindow` (omitting super call to prevent Chromium engine from pausing media playback during detach), ensuring that re-parenting transitions and background tasks never trigger video/audio pauses. Enabled native YouTube player controls in `youtube_player.html` for direct video interactions.
- Implemented persistent theme toggling (Light/Dark mode) inside the **More** settings tab, preventing accidental WebView destruction and audio stoppage during active song playback from the app bar. Toggling shows a Material 3 single-choice dialog, saves the preference inside SharedPreferences, and applies it using `AppCompatDelegate` theme reconstruction. Added safety checks to find and restore existing fragment instances (using tags) if `savedInstanceState != null` to prevent unattached fragment mismatches and crashes during theme switch refreshes. Fixed icon coloring inconsistencies in Dark Mode by applying dynamic `?attr/colorOnSurface` tints to the app bar search menu icon (via `app:iconTint` directly on the menu item inside `toolbar_menu.xml`) and the search suggestion/recent search icons (via `app:tint` inside the list item layout).
- Refactored Region Change logic in `MoreFragment` to trigger an instant activity recreation (via `Intent.FLAG_ACTIVITY_CLEAR_TASK`) immediately applying the newly selected country feed without requiring a manual app restart.
- Enhanced YouTube Player Full-screen and link behavior: 
  - Overrode `WebChromeClient` to natively handle iframe fullscreen actions. Flips orientation to landscape, enables immersive mode, and temporarily hides all active `DialogFragment` layers so the `DecorView` isn't obstructed by dialog windows. Hooked into `handleOnBackPressed` to cleanly exit fullscreen state.
  - Intercepted stray YouTube iframe clicks via `WebViewClient.shouldOverrideUrlLoading` (ensuring that tapping the YouTube logo or video title redirects to an external browser `ACTION_VIEW` instead of destroying the local video player state).