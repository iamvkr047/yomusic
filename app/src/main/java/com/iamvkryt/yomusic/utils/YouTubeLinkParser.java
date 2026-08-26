package com.iamvkryt.yomusic.utils;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeLinkParser {

    public enum ResultType {
        SONG,
        PLAYLIST,
        INVALID
    }

    public static class ParseResult {
        public final ResultType type;
        public final String id; // videoId or playlistId

        public ParseResult(ResultType type, String id) {
            this.type = type;
            this.id = id;
        }
    }

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w\\.-]+(?:/[^\\s]*)?", Pattern.CASE_INSENSITIVE);

    public static ParseResult parseIntent(Intent intent) {
        if (intent == null) {
            return new ParseResult(ResultType.INVALID, null);
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                return parseUri(data);
            }
            String dataString = intent.getDataString();
            if (!TextUtils.isEmpty(dataString)) {
                return parseUrl(dataString);
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (TextUtils.isEmpty(sharedText)) {
                sharedText = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            }
            if (!TextUtils.isEmpty(sharedText)) {
                return parseText(sharedText);
            }
        }

        return new ParseResult(ResultType.INVALID, null);
    }

    public static ParseResult parseText(String text) {
        if (TextUtils.isEmpty(text)) {
            return new ParseResult(ResultType.INVALID, null);
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group();
            ParseResult result = parseUrl(url);
            if (result.type != ResultType.INVALID) {
                return result;
            }
        }
        return new ParseResult(ResultType.INVALID, null);
    }

    public static ParseResult parseUrl(String urlString) {
        if (TextUtils.isEmpty(urlString)) {
            return new ParseResult(ResultType.INVALID, null);
        }

        try {
            Uri uri = Uri.parse(urlString);
            return parseUri(uri);
        } catch (Exception e) {
            return new ParseResult(ResultType.INVALID, null);
        }
    }

    public static ParseResult parseUri(Uri uri) {
        if (uri == null) {
            return new ParseResult(ResultType.INVALID, null);
        }

        String host = uri.getHost();
        if (host == null) {
            return new ParseResult(ResultType.INVALID, null);
        }

        host = host.toLowerCase();
        boolean isYouTubeHost = host.endsWith("youtube.com") || host.endsWith("youtu.be");
        if (!isYouTubeHost) {
            return new ParseResult(ResultType.INVALID, null);
        }

        // 1. Check video query param "v" (e.g. watch?v=VIDEO_ID)
        String videoId = uri.getQueryParameter("v");
        if (!TextUtils.isEmpty(videoId)) {
            return new ParseResult(ResultType.SONG, videoId.trim());
        }

        // 2. Check Shorts, Embed, /v/ or youtu.be path
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments != null && !pathSegments.isEmpty()) {
            if (pathSegments.size() >= 2 && "shorts".equalsIgnoreCase(pathSegments.get(0))) {
                String id = pathSegments.get(1);
                if (!TextUtils.isEmpty(id)) {
                    return new ParseResult(ResultType.SONG, id.trim());
                }
            }

            if (pathSegments.size() >= 2 && ("embed".equalsIgnoreCase(pathSegments.get(0)) || "v".equalsIgnoreCase(pathSegments.get(0)))) {
                String id = pathSegments.get(1);
                if (!TextUtils.isEmpty(id)) {
                    return new ParseResult(ResultType.SONG, id.trim());
                }
            }

            if (host.endsWith("youtu.be")) {
                String id = pathSegments.get(0);
                if (!TextUtils.isEmpty(id)) {
                    return new ParseResult(ResultType.SONG, id.trim());
                }
            }
        }

        // 3. Check playlist query param "list" or /playlist path
        String playlistId = uri.getQueryParameter("list");
        if (!TextUtils.isEmpty(playlistId)) {
            return new ParseResult(ResultType.PLAYLIST, playlistId.trim());
        }

        return new ParseResult(ResultType.INVALID, null);
    }
}
