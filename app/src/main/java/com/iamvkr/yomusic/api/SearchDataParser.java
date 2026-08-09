package com.iamvkr.yomusic.api;

import android.util.Log;

import com.iamvkr.yomusic.models.Playlist;
import com.iamvkr.yomusic.models.SearchResultItem;
import com.iamvkr.yomusic.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchDataParser {
    private static final String TAG = "SearchDataParser";

    public static class SearchResult {
        public final List<SearchResultItem> contents;
        public final String nextPage;

        public SearchResult(List<SearchResultItem> contents, String nextPage) {
            this.contents = contents;
            this.nextPage = nextPage != null ? nextPage : "";
        }
    }

    // ─── Autocomplete ─────────────────────────────────────────────────────────

    /**
     * Parses the JSONP autocomplete response from:
     * https://suggestqueries-clients6.youtube.com/complete/search?client=youtube&q=...
     *
     * Format: window.google.ac.h(["query", [["sug1",0,[512]], ...], {...}])
     */
    public static List<String> parseAutoComplete(String text) {
        List<String> suggestions = new ArrayList<>();
        try {
            int start = text.indexOf('(');
            int end = text.lastIndexOf(')');
            if (start == -1 || end == -1 || end <= start) return suggestions;

            String jsonStr = text.substring(start + 1, end);
            JSONArray root = new JSONArray(jsonStr);
            JSONArray items = root.getJSONArray(1);
            for (int i = 0; i < items.length(); i++) {
                JSONArray entry = items.getJSONArray(i);
                String sug = entry.getString(0);
                if (!sug.isEmpty()) suggestions.add(sug);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseAutoComplete error: " + e.getMessage());
        }
        return suggestions;
    }

    // ─── Initial search results ────────────────────────────────────────────────

    /**
     * Parses YouTube search results page initData:
     *   initData.contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents
     * Picks videoRenderer and lockupViewModel; skips everything else.
     */
    public static SearchResult parseSearchResults(JSONObject initData) {
        List<SearchResultItem> contents = new ArrayList<>();
        String nextPage = "";
        try {
            JSONArray sectionContents = initData
                    .getJSONObject("contents")
                    .getJSONObject("twoColumnSearchResultsRenderer")
                    .getJSONObject("primaryContents")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents");

            for (int i = 0; i < sectionContents.length(); i++) {
                JSONObject section = sectionContents.getJSONObject(i);

                // Continuation token
                if (section.has("continuationItemRenderer")) {
                    nextPage = parseNextToken(section);
                    continue;
                }

                if (!section.has("itemSectionRenderer")) continue;
                JSONArray items = section.getJSONObject("itemSectionRenderer")
                        .getJSONArray("contents");

                for (int j = 0; j < items.length(); j++) {
                    JSONObject item = items.getJSONObject(j);
                    if (item.has("videoRenderer")) {
                        VideoItem v = parseVideoRenderer(item.getJSONObject("videoRenderer"));
                        if (v != null) contents.add(new SearchResultItem(v));
                    } else if (item.has("lockupViewModel")) {
                        Object parsed = parseLockupViewModel(item.getJSONObject("lockupViewModel"));
                        if (parsed instanceof VideoItem) {
                            contents.add(new SearchResultItem((VideoItem) parsed));
                        } else if (parsed instanceof Playlist) {
                            contents.add(new SearchResultItem((Playlist) parsed));
                        }
                    }
                    // shorts, ads, shelves intentionally skipped
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseSearchResults error: " + e.getMessage());
        }
        return new SearchResult(contents, nextPage);
    }

    // ─── Next page search results ──────────────────────────────────────────────

    /**
     * Parses POST continuation response from:
     * https://www.youtube.com/youtubei/v1/search?key=...
     */
    public static SearchResult parseSearchResultsNext(JSONObject data) {
        List<SearchResultItem> contents = new ArrayList<>();
        String nextPage = "";
        try {
            JSONArray continuationItems = data
                    .getJSONArray("onResponseReceivedCommands")
                    .getJSONObject(0)
                    .getJSONObject("appendContinuationItemsAction")
                    .getJSONArray("continuationItems");

            // Continuation token
            for (int i = 0; i < continuationItems.length(); i++) {
                JSONObject ci = continuationItems.getJSONObject(i);
                if (ci.has("continuationItemRenderer")) {
                    nextPage = parseNextToken(ci);
                }
            }

            JSONArray items = continuationItems.getJSONObject(0)
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents");

            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.getJSONObject(j);
                if (item.has("videoRenderer")) {
                    VideoItem v = parseVideoRenderer(item.getJSONObject("videoRenderer"));
                    if (v != null) contents.add(new SearchResultItem(v));
                } else if (item.has("lockupViewModel")) {
                    Object parsed = parseLockupViewModel(item.getJSONObject("lockupViewModel"));
                    if (parsed instanceof VideoItem) {
                        contents.add(new SearchResultItem((VideoItem) parsed));
                    } else if (parsed instanceof Playlist) {
                        contents.add(new SearchResultItem((Playlist) parsed));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseSearchResultsNext error: " + e.getMessage());
        }
        return new SearchResult(contents, nextPage);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static VideoItem parseVideoRenderer(JSONObject v) {
        try {
            String videoId = v.optString("videoId", "");
            String title = v.getJSONObject("title").getJSONArray("runs")
                    .getJSONObject(0).getString("text");
            String channel = "";
            if (v.has("ownerText")) {
                channel = v.getJSONObject("ownerText")
                        .getJSONArray("runs").getJSONObject(0).optString("text", "");
            } else if (v.has("longBylineText")) {
                channel = v.getJSONObject("longBylineText")
                        .getJSONArray("runs").getJSONObject(0).optString("text", "");
            }
            int duration = parseDuration(v.optJSONObject("lengthText") != null
                    ? v.getJSONObject("lengthText").optString("simpleText", "") : "");
            if (videoId.isEmpty()) return null;
            return new VideoItem(videoId, title, channel, duration);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object parseLockupViewModel(JSONObject lock) {
        try {
            String contentId = lock.optString("contentId", "");
            String contentType = lock.optString("contentType", "").toLowerCase();
            String title = lock.getJSONObject("metadata")
                    .getJSONObject("lockupMetadataViewModel")
                    .getJSONObject("title")
                    .optString("content", "");

            String channel = "";
            try {
                JSONArray rows = lock.getJSONObject("metadata")
                        .getJSONObject("lockupMetadataViewModel")
                        .getJSONObject("metadata")
                        .getJSONObject("contentMetadataViewModel")
                        .getJSONArray("metadataRows");
                for (int r = 0; r < rows.length(); r++) {
                    JSONObject row = rows.getJSONObject(r);
                    if (row.has("metadataParts")) {
                        JSONArray parts = row.getJSONArray("metadataParts");
                        if (parts.length() > 0) {
                            String ch = parts.getJSONObject(0)
                                    .optJSONObject("text") != null
                                    ? parts.getJSONObject(0).getJSONObject("text").optString("content", "")
                                    : "";
                            if (!ch.contains(" views")) {
                                channel = ch;
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (contentType.contains("video")) {
                if (contentId.isEmpty()) return null;
                return new VideoItem(contentId, title, channel, 0);
            }

            // Playlist
            String thumbnail = "";
            try {
                JSONArray sources = lock.getJSONObject("contentImage")
                        .getJSONObject("collectionThumbnailViewModel")
                        .getJSONObject("primaryThumbnail")
                        .getJSONObject("thumbnailViewModel")
                        .getJSONObject("image")
                        .getJSONArray("sources");
                thumbnail = sources.getJSONObject(sources.length() - 1).optString("url", "");
            } catch (Exception ignored) {}

            if (contentId.isEmpty()) return null;
            return new Playlist(contentId, title, thumbnail, channel, false, new ArrayList<>(), "", 0);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses the continuation token from a continuationItemRenderer JSON object.
     * Mirrors the React nextTokenParser logic.
     */
    static String parseNextToken(JSONObject element) {
        try {
            if (element.has("continuationItemViewModel")) {
                return element.getJSONObject("continuationItemViewModel")
                        .getJSONObject("continuationCommand")
                        .getJSONObject("innertubeCommand")
                        .getJSONObject("continuationCommand")
                        .optString("token", "");
            } else if (element.has("continuationItemRenderer")) {
                JSONObject endpoint = element.getJSONObject("continuationItemRenderer")
                        .getJSONObject("continuationEndpoint");
                if (endpoint.has("continuationCommand")) {
                    return endpoint.getJSONObject("continuationCommand").optString("token", "");
                } else if (endpoint.has("commandExecutorCommand")) {
                    return endpoint.getJSONObject("commandExecutorCommand")
                            .getJSONArray("commands")
                            .getJSONObject(1)
                            .getJSONObject("continuationCommand")
                            .optString("token", "");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "parseNextToken error: " + e.getMessage());
        }
        return "";
    }

    static int parseDuration(String text) {
        if (text == null || text.isEmpty()) return 0;
        try {
            String[] parts = text.trim().split(":");
            if (parts.length == 3) return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            if (parts.length == 2) return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception ignored) {}
        return 0;
    }
}
