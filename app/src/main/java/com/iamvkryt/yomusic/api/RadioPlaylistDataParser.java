package com.iamvkryt.yomusic.api;

import android.util.Log;

import com.iamvkryt.yomusic.models.Playlist;
import com.iamvkryt.yomusic.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RadioPlaylistDataParser {
    private static final String TAG = "RadioPlaylistParser";

    /**
     * Converts a duration string like "11:15:54", "32:12", "3:14" to seconds.
     */
    public static int durationToSeconds(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        try {
            String[] parts = text.trim().split(":");
            if (parts.length == 3) {
                int h = Integer.parseInt(parts[0].trim());
                int m = Integer.parseInt(parts[1].trim());
                int s = Integer.parseInt(parts[2].trim());
                return h * 3600 + m * 60 + s;
            } else if (parts.length == 2) {
                int m = Integer.parseInt(parts[0].trim());
                int s = Integer.parseInt(parts[1].trim());
                return m * 60 + s;
            } else if (parts.length == 1) {
                return Integer.parseInt(parts[0].trim());
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Parses initial data for a Short RD Radio playlist.
     */
    public static Playlist parseRadioPlaylistVideos(JSONObject initData, String playlistId) {
        if (initData == null) return null;
        try {
            JSONObject contentsObj = initData.optJSONObject("contents");
            if (contentsObj == null) return null;

            JSONObject twoColumn = contentsObj.optJSONObject("twoColumnWatchNextResults");
            if (twoColumn == null) return null;

            JSONObject playlistWrapper = twoColumn.optJSONObject("playlist");
            if (playlistWrapper == null) return null;

            JSONObject playlistInfo = playlistWrapper.optJSONObject("playlist");
            if (playlistInfo == null) return null;

            String title = "Radio";
            Object titleObj = playlistInfo.opt("title");
            if (titleObj instanceof String) {
                title = (String) titleObj;
            } else if (titleObj instanceof JSONObject) {
                JSONObject tJson = (JSONObject) titleObj;
                if (tJson.has("simpleText")) {
                    title = tJson.optString("simpleText", "Radio");
                } else if (tJson.has("runs")) {
                    JSONArray runs = tJson.optJSONArray("runs");
                    if (runs != null && runs.length() > 0) {
                        title = runs.optJSONObject(0).optString("text", "Radio");
                    }
                }
            }

            JSONArray kContents = playlistInfo.optJSONArray("contents");
            List<VideoItem> itemsResult = new ArrayList<>();
            String nextPage = "";

            if (kContents != null && kContents.length() > 0) {
                for (int i = 0; i < kContents.length(); i++) {
                    JSONObject element = kContents.optJSONObject(i);
                    if (element != null && element.has("playlistPanelVideoRenderer")) {
                        JSONObject renderer = element.optJSONObject("playlistPanelVideoRenderer");
                        VideoItem item = parsePanelVideoItem(renderer);
                        if (item != null) {
                            itemsResult.add(item);
                        }
                    }
                }

                // Extract next page token from the last panel item
                for (int i = kContents.length() - 1; i >= 0; i--) {
                    JSONObject element = kContents.optJSONObject(i);
                    if (element != null && element.has("playlistPanelVideoRenderer")) {
                        JSONObject renderer = element.optJSONObject("playlistPanelVideoRenderer");
                        String token = extractNextToken(renderer, playlistId, i);
                        if (!token.isEmpty()) {
                            nextPage = token;
                            break;
                        }
                    }
                }
            }

            return new Playlist(playlistId, title, "", "YouTube", false, itemsResult, nextPage, 0);
        } catch (Exception e) {
            Log.e(TAG, "parseRadioPlaylistVideos Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the next page response from get_watch endpoint for a Short RD Radio playlist.
     */
    public static PlaylistDataParser.PlaylistNextResult parseRadioPlaylistNext(String responseJsonStr, String tokenJson) {
        if (responseJsonStr == null || responseJsonStr.trim().isEmpty()) return null;
        try {
            String trimmed = responseJsonStr.trim();
            JSONObject playlistInfo = null;

            if (trimmed.startsWith("[")) {
                JSONArray array = new JSONArray(trimmed);
                if (array.length() > 0) {
                    JSONObject first = array.optJSONObject(0);
                    if (first != null) {
                        if (first.has("watchNextResponse")) {
                            playlistInfo = first.optJSONObject("watchNextResponse")
                                    .optJSONObject("contents")
                                    .optJSONObject("twoColumnWatchNextResults")
                                    .optJSONObject("playlist")
                                    .optJSONObject("playlist");
                        } else if (first.has("contents")) {
                            playlistInfo = first.optJSONObject("contents")
                                    .optJSONObject("twoColumnWatchNextResults")
                                    .optJSONObject("playlist")
                                    .optJSONObject("playlist");
                        }
                    }
                }
            } else if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                if (obj.has("watchNextResponse")) {
                    playlistInfo = obj.optJSONObject("watchNextResponse")
                            .optJSONObject("contents")
                            .optJSONObject("twoColumnWatchNextResults")
                            .optJSONObject("playlist")
                            .optJSONObject("playlist");
                } else if (obj.has("contents")) {
                    JSONObject contents = obj.optJSONObject("contents");
                    if (contents != null && contents.has("twoColumnWatchNextResults")) {
                        playlistInfo = contents.optJSONObject("twoColumnWatchNextResults")
                                .optJSONObject("playlist")
                                .optJSONObject("playlist");
                    }
                }
            }

            if (playlistInfo == null) {
                Log.e(TAG, "parseRadioPlaylistNext: Failed to find playlistInfo in response");
                return null;
            }

            String targetVideoId = "";
            String playlistId = "";
            if (tokenJson != null && !tokenJson.isEmpty()) {
                try {
                    JSONObject tokenObj = new JSONObject(tokenJson);
                    targetVideoId = tokenObj.optString("videoId", "");
                    playlistId = tokenObj.optString("playlistId", "");
                } catch (Exception ignored) {}
            }

            JSONArray allContents = playlistInfo.optJSONArray("contents");
            if (allContents == null) {
                return new PlaylistDataParser.PlaylistNextResult(new ArrayList<>(), "");
            }

            // Deduplicate: Find target video index from previous page
            int targetIndex = -1;
            if (!targetVideoId.isEmpty()) {
                for (int i = 0; i < allContents.length(); i++) {
                    JSONObject element = allContents.optJSONObject(i);
                    if (element != null && element.has("playlistPanelVideoRenderer")) {
                        JSONObject renderer = element.optJSONObject("playlistPanelVideoRenderer");
                        if (renderer != null && targetVideoId.equals(renderer.optString("videoId", ""))) {
                            targetIndex = i;
                            break;
                        }
                    }
                }
            }

            int startIdx = (targetIndex != -1) ? (targetIndex + 1) : 0;
            List<VideoItem> itemsResult = new ArrayList<>();
            String newToken = "";

            for (int i = startIdx; i < allContents.length(); i++) {
                JSONObject element = allContents.optJSONObject(i);
                if (element != null && element.has("playlistPanelVideoRenderer")) {
                    JSONObject renderer = element.optJSONObject("playlistPanelVideoRenderer");
                    VideoItem item = parsePanelVideoItem(renderer);
                    if (item != null) {
                        itemsResult.add(item);
                    }
                }
            }

            // Build new next page token from the last panel item in sliced contents
            if (allContents.length() > startIdx) {
                for (int i = allContents.length() - 1; i >= startIdx; i--) {
                    JSONObject element = allContents.optJSONObject(i);
                    if (element != null && element.has("playlistPanelVideoRenderer")) {
                        JSONObject renderer = element.optJSONObject("playlistPanelVideoRenderer");
                        String token = extractNextToken(renderer, playlistId, i);
                        if (!token.isEmpty()) {
                            newToken = token;
                            break;
                        }
                    }
                }
            }

            return new PlaylistDataParser.PlaylistNextResult(itemsResult, newToken);
        } catch (Exception e) {
            Log.e(TAG, "parseRadioPlaylistNext Error: " + e.getMessage());
            return null;
        }
    }

    private static VideoItem parsePanelVideoItem(JSONObject renderer) {
        if (renderer == null) return null;
        try {
            String vId = renderer.optString("videoId", "");
            if (vId.isEmpty()) return null;

            String vTitle = "";
            JSONObject titleObj = renderer.optJSONObject("title");
            if (titleObj != null) {
                if (titleObj.has("simpleText")) {
                    vTitle = titleObj.optString("simpleText", "");
                } else if (titleObj.has("runs")) {
                    JSONArray runs = titleObj.optJSONArray("runs");
                    if (runs != null && runs.length() > 0) {
                        vTitle = runs.optJSONObject(0).optString("text", "");
                    }
                }
            } else if (renderer.has("title")) {
                vTitle = renderer.optString("title", "");
            }

            int duration = 0;
            if (renderer.has("lengthText")) {
                JSONObject lengthObj = renderer.optJSONObject("lengthText");
                if (lengthObj != null) {
                    String durStr = lengthObj.optString("simpleText", "");
                    if (durStr.isEmpty() && lengthObj.has("runs")) {
                        JSONArray runs = lengthObj.optJSONArray("runs");
                        if (runs != null && runs.length() > 0) {
                            durStr = runs.optJSONObject(0).optString("text", "");
                        }
                    }
                    duration = durationToSeconds(durStr);
                }
            } else if (renderer.has("lengthSeconds")) {
                try {
                    duration = Integer.parseInt(renderer.optString("lengthSeconds", "0"));
                } catch (Exception ignored) {}
            }

            String vChannel = "YouTube";
            if (renderer.has("shortBylineText")) {
                JSONObject bylineObj = renderer.optJSONObject("shortBylineText");
                if (bylineObj != null) {
                    JSONArray runs = bylineObj.optJSONArray("runs");
                    if (runs != null && runs.length() > 0) {
                        vChannel = runs.optJSONObject(0).optString("text", "YouTube");
                    } else if (bylineObj.has("simpleText")) {
                        vChannel = bylineObj.optString("simpleText", "YouTube");
                    }
                }
            }

            return new VideoItem(vId, vTitle, vChannel, duration);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractNextToken(JSONObject renderer, String fallbackPlaylistId, int defaultIndex) {
        if (renderer == null) return "";
        try {
            JSONObject nav = renderer.optJSONObject("navigationEndpoint");
            if (nav != null && nav.has("watchEndpoint")) {
                JSONObject watch = nav.optJSONObject("watchEndpoint");
                if (watch != null) {
                    String params = watch.optString("params", "");
                    int index = watch.optInt("index", defaultIndex);
                    String videoId = watch.optString("videoId", renderer.optString("videoId", ""));
                    String plId = watch.optString("playlistId", fallbackPlaylistId);

                    JSONObject body = new JSONObject();
                    body.put("params", params);
                    body.put("playlistId", plId);
                    body.put("playlistIndex", index);
                    body.put("videoId", videoId);
                    return body.toString();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
