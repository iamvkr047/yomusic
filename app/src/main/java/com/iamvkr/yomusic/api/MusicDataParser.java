package com.iamvkr.yomusic.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the YouTube Music channel home page data from ytInitialData JSON.
 * This is a direct port of react_refrence/src/api/parsers/musicDataParser.ts
 *
 * The YouTube Music channel page structure:
 *   initdata.contents.twoColumnBrowseResultsRenderer.tabs[0].tabRenderer.content
 *     .richGridRenderer.contents[] -> richSectionRenderer -> richShelfRenderer
 *       -> contents[] -> richItemRenderer -> lockupViewModel
 */
public class MusicDataParser {
    private static final String TAG = "MusicDataParser";

    /**
     * Represents a section of music videos (title + list of video items).
     */
    public static class MusicSection {
        public final String title;
        public final List<VideoItem> content;

        public MusicSection(String title, List<VideoItem> content) {
            this.title = title;
            this.content = content;
        }
    }

    /**
     * Represents a single video item with videoId and title.
     */
    public static class VideoItem {
        public final String videoId;
        public final String title;

        public VideoItem(String videoId, String title) {
            this.videoId = videoId;
            this.title = title;
        }
    }

    /**
     * Represents a section of album/playlist items.
     */
    public static class AlbumSection {
        public final String title;
        public final List<AlbumItem> content;

        public AlbumSection(String title, List<AlbumItem> content) {
            this.title = title;
            this.content = content;
        }
    }

    /**
     * Represents a single album/playlist item.
     */
    public static class AlbumItem {
        public final String playlistId;
        public final String title;
        public final String thumbnail;

        public AlbumItem(String playlistId, String title, String thumbnail) {
            this.playlistId = playlistId;
            this.title = title;
            this.thumbnail = thumbnail;
        }
    }

    /**
     * Result containing parsed music sections and album sections.
     */
    public static class HomeFeedResult {
        public final List<MusicSection> musicSections;
        public final List<AlbumSection> albumSections;

        public HomeFeedResult(List<MusicSection> musicSections, List<AlbumSection> albumSections) {
            this.musicSections = musicSections;
            this.albumSections = albumSections;
        }
    }

    /**
     * Parses the YouTube Music home feed from ytInitialData.
     * Mirrors: parseMusicData() in musicDataParser.ts
     *
     * @param initData The ytInitialData JSONObject
     * @return HomeFeedResult with music and album sections, or null on failure
     */
    public static HomeFeedResult parseMusicData(JSONObject initData) {
        try {
            // Navigate: contents.twoColumnBrowseResultsRenderer.tabs[0].tabRenderer.content.richGridRenderer.contents
            JSONArray richContents = initData
                    .getJSONObject("contents")
                    .getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs")
                    .getJSONObject(0)
                    .getJSONObject("tabRenderer")
                    .getJSONObject("content")
                    .getJSONObject("richGridRenderer")
                    .getJSONArray("contents");

            List<MusicSection> musicSections = new ArrayList<>();
            List<AlbumSection> albumSections = new ArrayList<>();

            for (int i = 0; i < richContents.length(); i++) {
                JSONObject section = richContents.getJSONObject(i);

                // Each section is wrapped in richSectionRenderer.content.richShelfRenderer
                JSONObject shelf = optNestedObject(section,
                        "richSectionRenderer", "content", "richShelfRenderer");
                if (shelf == null) continue;

                String sectionTitle = getSectionTitle(shelf.optJSONObject("title"));
                JSONArray shelfContents = shelf.optJSONArray("contents");
                if (shelfContents == null) continue;

                List<VideoItem> videos = new ArrayList<>();
                List<AlbumItem> albums = new ArrayList<>();

                for (int j = 0; j < shelfContents.length(); j++) {
                    JSONObject c = shelfContents.getJSONObject(j);

                    // Navigate: richItemRenderer.content.lockupViewModel
                    JSONObject lock = optNestedObject(c,
                            "richItemRenderer", "content", "lockupViewModel");
                    if (lock == null) continue;

                    String contentType = lock.optString("contentType", "");
                    String contentId = lock.optString("contentId", "");
                    String itemTitle = getItemTitle(lock);

                    if (contentId.isEmpty()) continue;

                    if ("LOCKUP_CONTENT_TYPE_VIDEO".equals(contentType)) {
                        videos.add(new VideoItem(contentId, itemTitle));
                    } else if ("LOCKUP_CONTENT_TYPE_ALBUM".equals(contentType)
                            || "LOCKUP_CONTENT_TYPE_PLAYLIST".equals(contentType)) {
                        String thumbnail = getCollectionThumbnail(lock);
                        albums.add(new AlbumItem(contentId, itemTitle, thumbnail));
                    }
                }

                if (!videos.isEmpty()) {
                    musicSections.add(new MusicSection(sectionTitle, videos));
                }
                if (!albums.isEmpty()) {
                    albumSections.add(new AlbumSection(sectionTitle, albums));
                }
            }

            return new HomeFeedResult(musicSections, albumSections);
        } catch (Exception e) {
            Log.e(TAG, "parseMusicData Error: " + e.getMessage());
            return null;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Section titles use either `simpleText` (most sections) or `runs[0].text`
     * (sections that have a navigation link attached to their title).
     * Mirrors: getSectionTitle() in musicDataParser.ts
     */
    private static String getSectionTitle(JSONObject titleObj) {
        if (titleObj == null) return "";

        String simpleText = titleObj.optString("simpleText", "");
        if (!simpleText.isEmpty()) return simpleText;

        try {
            JSONArray runs = titleObj.optJSONArray("runs");
            if (runs != null && runs.length() > 0) {
                return runs.getJSONObject(0).optString("text", "");
            }
        } catch (Exception ignored) {}

        return "";
    }

    /**
     * Extracts item title from lockupViewModel.metadata.lockupMetadataViewModel.title.content
     */
    private static String getItemTitle(JSONObject lock) {
        try {
            return lock
                    .getJSONObject("metadata")
                    .getJSONObject("lockupMetadataViewModel")
                    .getJSONObject("title")
                    .optString("content", "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extracts album/playlist thumbnail from the collection thumbnail view model.
     * Mirrors: getCollectionThumbnail() in musicDataParser.ts
     *
     * Path: lock.contentImage.collectionThumbnailViewModel.primaryThumbnail
     *         .thumbnailViewModel.image.sources[1].url (medium quality)
     */
    private static String getCollectionThumbnail(JSONObject lock) {
        try {
            JSONArray sources = lock
                    .getJSONObject("contentImage")
                    .getJSONObject("collectionThumbnailViewModel")
                    .getJSONObject("primaryThumbnail")
                    .getJSONObject("thumbnailViewModel")
                    .getJSONObject("image")
                    .getJSONArray("sources");

            if (sources.length() >= 2) {
                return sources.getJSONObject(1).optString("url", "");
            }
            return sources.getJSONObject(sources.length() - 1).optString("url", "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Safely navigates nested JSON objects by a sequence of keys.
     * Returns null if any key is missing along the path.
     */
    private static JSONObject optNestedObject(JSONObject root, String... keys) {
        JSONObject current = root;
        for (String key : keys) {
            if (current == null) return null;
            current = current.optJSONObject(key);
        }
        return current;
    }
}
