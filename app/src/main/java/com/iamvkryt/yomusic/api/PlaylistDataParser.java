package com.iamvkryt.yomusic.api;

import android.util.Log;

import com.iamvkryt.yomusic.models.Playlist;
import com.iamvkryt.yomusic.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDataParser {
    private static final String TAG = "PlaylistDataParser";

    public static class PlaylistNextResult {
        public final List<VideoItem> items;
        public final String nextPage;

        public PlaylistNextResult(List<VideoItem> items, String nextPage) {
            this.items = items;
            this.nextPage = nextPage != null ? nextPage : "";
        }
    }

    public static Playlist parsePlaylistVideos(JSONObject initData, String playlistId) {
        if (initData == null) return null;
        try {
            JSONArray sectionContents = initData
                    .getJSONObject("contents")
                    .getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs")
                    .getJSONObject(0)
                    .getJSONObject("tabRenderer")
                    .getJSONObject("content")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents");

            JSONArray kContents = null;
            try {
                JSONObject firstSection = sectionContents.getJSONObject(0).getJSONObject("itemSectionRenderer");
                JSONArray sectionItems = firstSection.getJSONArray("contents");
                if (sectionItems.length() > 0 && sectionItems.getJSONObject(0).has("playlistVideoListRenderer")) {
                    kContents = sectionItems.getJSONObject(0).getJSONObject("playlistVideoListRenderer").getJSONArray("contents");
                } else {
                    kContents = sectionItems;
                }
            } catch (Exception ignored) {}

            if (kContents == null && sectionContents.length() > 0) {
                kContents = sectionContents.getJSONObject(0).optJSONObject("itemSectionRenderer") != null ?
                        sectionContents.getJSONObject(0).getJSONObject("itemSectionRenderer").optJSONArray("contents") : null;
            }

            String title = "";
            try {
                title = initData.getJSONObject("metadata")
                        .getJSONObject("playlistMetadataRenderer")
                        .optString("title", "");
            } catch (Exception ignored) {}

            String channel = "";
            try {
                channel = initData.getJSONObject("sidebar")
                        .getJSONObject("playlistSidebarRenderer")
                        .getJSONArray("items")
                        .getJSONObject(1)
                        .getJSONObject("playlistSidebarSecondaryInfoRenderer")
                        .getJSONObject("videoOwner")
                        .getJSONObject("videoOwnerRenderer")
                        .getJSONObject("title")
                        .getJSONArray("runs")
                        .getJSONObject(0)
                        .optString("text", "");
            } catch (Exception ignored) {}

            if (channel.isEmpty()) {
                try {
                    channel = initData.getJSONObject("header")
                            .getJSONObject("playlistHeaderRenderer")
                            .getJSONObject("ownerText")
                            .getJSONArray("runs")
                            .getJSONObject(0)
                            .optString("text", "");
                } catch (Exception ignored) {}
            }

            int total = 0;
            try {
                String totalStr = initData.getJSONObject("sidebar")
                        .getJSONObject("playlistSidebarRenderer")
                        .getJSONArray("items")
                        .getJSONObject(0)
                        .getJSONObject("playlistSidebarPrimaryInfoRenderer")
                        .getJSONArray("stats")
                        .getJSONObject(0)
                        .getJSONArray("runs")
                        .getJSONObject(0)
                        .optString("text", "");
                total = Integer.parseInt(totalStr.replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}

            if (total == 0) {
                try {
                    String totalStr = initData.getJSONObject("header")
                            .getJSONObject("playlistHeaderRenderer")
                            .getJSONObject("numVideosText")
                            .getJSONArray("runs")
                            .getJSONObject(0)
                            .optString("text", "");
                    total = Integer.parseInt(totalStr.replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}
            }

            String thumbnail = "";
            try {
                JSONObject primaryInfo = initData.getJSONObject("sidebar")
                        .getJSONObject("playlistSidebarRenderer")
                        .getJSONArray("items")
                        .getJSONObject(0)
                        .getJSONObject("playlistSidebarPrimaryInfoRenderer");
                JSONObject thumbRenderer = primaryInfo.getJSONObject("thumbnailRenderer");
                JSONArray thumbs = null;
                if (thumbRenderer.has("playlistVideoThumbnailRenderer")) {
                    thumbs = thumbRenderer.getJSONObject("playlistVideoThumbnailRenderer")
                            .getJSONObject("thumbnail").getJSONArray("thumbnails");
                } else if (thumbRenderer.has("playlistCustomThumbnailRenderer")) {
                    thumbs = thumbRenderer.getJSONObject("playlistCustomThumbnailRenderer")
                            .getJSONObject("thumbnail").getJSONArray("thumbnails");
                }
                if (thumbs != null && thumbs.length() > 0) {
                    thumbnail = thumbs.getJSONObject(thumbs.length() - 1).optString("url", "");
                }
            } catch (Exception ignored) {}

            List<VideoItem> itemsResult = new ArrayList<>();
            String nextPage = "";

            if (kContents != null) {
                for (int i = 0; i < kContents.length(); i++) {
                    JSONObject element = kContents.getJSONObject(i);
                    if (element.has("playlistVideoRenderer")) {
                        JSONObject renderer = element.getJSONObject("playlistVideoRenderer");
                        String vId = renderer.optString("videoId", "");
                        String vTitle = "";
                        try {
                            vTitle = renderer.getJSONObject("title").getJSONArray("runs").getJSONObject(0).optString("text", "");
                        } catch (Exception ignored) {}
                        int duration = 0;
                        try {
                            duration = Integer.parseInt(renderer.optString("lengthSeconds", "0"));
                        } catch (Exception ignored) {}
                        String vChannel = channel;
                        try {
                            vChannel = renderer.getJSONObject("shortBylineText").getJSONArray("runs").getJSONObject(0).optString("text", channel);
                        } catch (Exception ignored) {}

                        itemsResult.add(new VideoItem(vId, vTitle, vChannel, duration));
                    } else if (element.has("lockupViewModel")) {
                        VideoItem item = parseLockupVideo(element.getJSONObject("lockupViewModel"));
                        if (item != null) {
                            itemsResult.add(item);
                        }
                    } else {
                        String token = parseNextToken(element);
                        if (!token.isEmpty()) {
                            nextPage = token;
                        }
                    }
                }
            }

            return new Playlist(playlistId, title, thumbnail, channel, false, itemsResult, nextPage, total);
        } catch (Exception e) {
            Log.e(TAG, "parsePlaylistVideos Error: " + e.getMessage());
            return null;
        }
    }

    public static PlaylistNextResult parsePlaylistVideosNext(JSONObject data) {
        if (data == null) return null;
        try {
            JSONArray items = data.getJSONArray("onResponseReceivedActions")
                    .getJSONObject(0)
                    .getJSONObject("appendContinuationItemsAction")
                    .getJSONArray("continuationItems");

            String channel = "";
            try {
                channel = data.getJSONObject("sidebar")
                        .getJSONObject("playlistSidebarRenderer")
                        .getJSONArray("items")
                        .getJSONObject(1)
                        .getJSONObject("playlistSidebarSecondaryInfoRenderer")
                        .getJSONObject("videoOwner")
                        .getJSONObject("videoOwnerRenderer")
                        .getJSONObject("title")
                        .getJSONArray("runs")
                        .getJSONObject(0)
                        .optString("text", "");
            } catch (Exception ignored) {}

            List<VideoItem> itemsResult = new ArrayList<>();
            String newToken = "";

            for (int i = 0; i < items.length(); i++) {
                JSONObject element = items.getJSONObject(i);
                if (element.has("playlistVideoRenderer")) {
                    JSONObject renderer = element.getJSONObject("playlistVideoRenderer");
                    String vId = renderer.optString("videoId", "");
                    String vTitle = "";
                    try {
                        vTitle = renderer.getJSONObject("title").getJSONArray("runs").getJSONObject(0).optString("text", "");
                    } catch (Exception ignored) {}
                    int duration = 0;
                    try {
                        duration = Integer.parseInt(renderer.optString("lengthSeconds", "0"));
                    } catch (Exception ignored) {}
                    String vChannel = channel;
                    try {
                        vChannel = renderer.getJSONObject("shortBylineText").getJSONArray("runs").getJSONObject(0).optString("text", channel);
                    } catch (Exception ignored) {}

                    itemsResult.add(new VideoItem(vId, vTitle, vChannel, duration));
                } else if (element.has("lockupViewModel")) {
                    VideoItem item = parseLockupVideo(element.getJSONObject("lockupViewModel"));
                    if (item != null) {
                        itemsResult.add(item);
                    }
                } else {
                    String token = parseNextToken(element);
                    if (!token.isEmpty()) {
                        newToken = token;
                    }
                }
            }

            return new PlaylistNextResult(itemsResult, newToken);
        } catch (Exception e) {
            Log.e(TAG, "parsePlaylistVideosNext Error: " + e.getMessage());
            return null;
        }
    }

    private static VideoItem parseLockupVideo(JSONObject lock) {
        try {
            String contentId = lock.optString("contentId", "");
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
                for (int i = 0; i < rows.length(); i++) {
                    JSONArray parts = rows.getJSONObject(i).optJSONArray("metadataParts");
                    if (parts != null && parts.length() > 0) {
                        String text = parts.getJSONObject(0).getJSONObject("text").optString("content", "");
                        if (!text.contains("views")) {
                            channel = text;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}

            return new VideoItem(contentId, title, channel, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private static String parseNextToken(JSONObject element) {
        try {
            if (element.has("continuationItemViewModel")) {
                return element.getJSONObject("continuationItemViewModel")
                        .getJSONObject("continuationCommand")
                        .getJSONObject("innertubeCommand")
                        .getJSONObject("continuationCommand")
                        .optString("token", "");
            } else if (element.has("continuationItemRenderer")) {
                JSONObject endpoint = element.getJSONObject("continuationItemRenderer").getJSONObject("continuationEndpoint");
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
        } catch (Exception ignored) {}
        return "";
    }
}
