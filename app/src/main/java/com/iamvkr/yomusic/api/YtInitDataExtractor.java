package com.iamvkr.yomusic.api;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fetches YouTube page HTML and extracts ytInitialData, apiToken, and INNERTUBE_CONTEXT.
 * Mirrors getYoutubeInitData() from the React reference code.
 */
public class YtInitDataExtractor {
    private static final String TAG = "YtInitDataExtractor";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public static class YtInitResult {
        public final JSONObject initData;
        public final String apiToken;
        public final JSONObject context;

        public YtInitResult(JSONObject initData, String apiToken, JSONObject context) {
            this.initData = initData;
            this.apiToken = apiToken != null ? apiToken : "";
            this.context = context;
        }
    }

    private final OkHttpClient client;

    public YtInitDataExtractor() {
        this.client = new OkHttpClient();
    }

    public YtInitResult getYoutubeInitData(String url, String country) {
        try {
            String separator = url.contains("?") ? "&" : "?";
            String fullUrl = url + separator + "gl=" + country + "&app=desktop";

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Sec-Ch-Ua", "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "Windows")
                    .header("Origin", "https://www.youtube.com")
                    .header("Referer", "https://www.youtube.com")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "HTTP request failed: " + response.code());
                    return null;
                }

                String pageHtml = response.body().string();
                return extractInitResult(pageHtml);
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error fetching YouTube init data: " + e.getMessage());
            return null;
        }
    }

    private YtInitResult extractInitResult(String pageHtml) {
        try {
            String[] parts = pageHtml.split("var ytInitialData =");
            if (parts.length < 2) {
                Log.e(TAG, "Cannot find ytInitialData in page source");
                return null;
            }

            String jsonPart = parts[1].split("</script>")[0].trim();
            if (jsonPart.endsWith(";")) {
                jsonPart = jsonPart.substring(0, jsonPart.length() - 1);
            }

            JSONObject initData = new JSONObject(jsonPart);

            String apiToken = "";
            if (pageHtml.contains("innertubeApiKey")) {
                try {
                    String apiKeyPart = pageHtml.split("innertubeApiKey")[1];
                    if (apiKeyPart != null) {
                        apiToken = apiKeyPart.trim().split(",")[0].split("\"")[2];
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to extract apiToken: " + e.getMessage());
                }
            }

            JSONObject context = null;
            if (pageHtml.contains("INNERTUBE_CONTEXT")) {
                try {
                    String contextPart = pageHtml.split("INNERTUBE_CONTEXT")[1];
                    if (contextPart != null) {
                        String rawContext = contextPart.trim();
                        // Usually format: `: {...}` or `": {...}`
                        int firstBrace = rawContext.indexOf('{');
                        int lastBrace = rawContext.lastIndexOf('}');
                        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                            String contextJsonStr = rawContext.substring(firstBrace, lastBrace + 1);
                            context = new JSONObject(contextJsonStr);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to extract INNERTUBE_CONTEXT: " + e.getMessage());
                }
            }

            return new YtInitResult(initData, apiToken, context);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ytInitialData JSON: " + e.getMessage());
            return null;
        }
    }
}
