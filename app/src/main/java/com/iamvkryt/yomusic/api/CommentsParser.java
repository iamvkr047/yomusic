package com.iamvkryt.yomusic.api;

import android.util.Log;

import com.iamvkryt.yomusic.models.Comment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comments parser translated from REFRENCE_JS-TS_CODE/parsers/commentsParser.ts.
 * Parses initial comments token, top-level comments list, continuation (next page) comments,
 * and comment subthreads (replies).
 */
public class CommentsParser {
    private static final String TAG = "CommentsParser";

    /**
     * Equivalent to CommentsType in commentsParser.ts.
     */
    public static class CommentItem {
        public final String commentId;
        public final String content;
        public final String publishedTime;
        public final String author;
        public final String avatar;
        public final String channelId;
        public final String likesCount;
        public final String replyCount;
        public final String subThreadToken;

        public CommentItem(String commentId, String content, String publishedTime,
                           String author, String avatar, String channelId,
                           String likesCount, String replyCount, String subThreadToken) {
            this.commentId = commentId != null ? commentId : "";
            this.content = content != null ? content : "";
            this.publishedTime = publishedTime != null ? publishedTime : "";
            this.author = author != null ? author : "";
            this.avatar = avatar != null ? avatar : "";
            this.channelId = channelId != null ? channelId : "";
            this.likesCount = likesCount != null ? likesCount : "";
            this.replyCount = replyCount != null ? replyCount : "";
            this.subThreadToken = subThreadToken != null ? subThreadToken : "";
        }

        /**
         * Helper to convert CommentItem to the app's Comment model.
         */
        public Comment toComment() {
            return new Comment(
                    commentId,
                    author,
                    content,
                    publishedTime,
                    avatar,
                    channelId,
                    likesCount,
                    replyCount,
                    subThreadToken
            );
        }

        private static int parseLikes(String likesStr) {
            if (likesStr == null || likesStr.trim().isEmpty()) return 0;
            String cleaned = likesStr.trim().toUpperCase();
            try {
                if (cleaned.endsWith("K")) {
                    double val = Double.parseDouble(cleaned.substring(0, cleaned.length() - 1));
                    return (int) (val * 1000);
                } else if (cleaned.endsWith("M")) {
                    double val = Double.parseDouble(cleaned.substring(0, cleaned.length() - 1));
                    return (int) (val * 1000000);
                } else {
                    String digitsOnly = cleaned.replaceAll("[^0-9]", "");
                    return digitsOnly.isEmpty() ? 0 : Integer.parseInt(digitsOnly);
                }
            } catch (Exception e) {
                return 0;
            }
        }
    }

    /**
     * Result returned by parseCommentsToken.
     */
    public static class CommentsTokenResult {
        public final String token;
        public final String total;

        public CommentsTokenResult(String token, String total) {
            this.token = token != null ? token : "";
            this.total = total != null ? total : "";
        }
    }

    /**
     * Result returned by parseComments, parseCommentsNext, and parseSubthreads.
     */
    public static class CommentsResult {
        public final List<CommentItem> commentsDataList;
        public final String nextPageToken;

        public CommentsResult(List<CommentItem> commentsDataList, String nextPageToken) {
            this.commentsDataList = commentsDataList != null ? commentsDataList : new ArrayList<>();
            this.nextPageToken = nextPageToken != null ? nextPageToken : "";
        }

        public List<Comment> toComments() {
            List<Comment> list = new ArrayList<>();
            for (CommentItem item : commentsDataList) {
                list.add(item.toComment());
            }
            return list;
        }
    }

    /**
     * Parses the comments continuation token and total count from video page ytInitialData.
     *
     * @param initData JSONObject representing ytInitialData
     * @return CommentsTokenResult or null if not found
     */
    public static CommentsTokenResult parseCommentsToken(JSONObject initData) {
        return parseCommentsToken(initData, "top");
    }

    /**
     * Parses the comments continuation token and total count from video page ytInitialData with sort mode.
     *
     * @param initData JSONObject representing ytInitialData
     * @param sort     "top" or "newest" (defaults to "top")
     * @return CommentsTokenResult or null if not found
     */
    public static CommentsTokenResult parseCommentsToken(JSONObject initData, String sort) {
        if (initData == null) return null;
        try {
            JSONArray engagementPanels = initData.optJSONArray("engagementPanels");
            if (engagementPanels == null) {
                Log.w(TAG, "No engagementPanels found in initData");
                return null;
            }

            JSONObject targetHeader = null;
            for (int i = 0; i < engagementPanels.length(); i++) {
                JSONObject pl = engagementPanels.optJSONObject(i);
                if (pl == null) continue;
                JSONObject panelList = pl.optJSONObject("engagementPanelSectionListRenderer");
                if (panelList == null) continue;

                String tid = panelList.optString("targetId", "");
                if (tid.contains("comment")) {
                    JSONObject header = panelList.optJSONObject("header");
                    if (header != null) {
                        targetHeader = header.optJSONObject("engagementPanelTitleHeaderRenderer");
                        if (targetHeader != null) {
                            break;
                        }
                    }
                }
            }

            if (targetHeader == null) {
                Log.w(TAG, "Comments engagement panel header not found");
                return null;
            }

            // Extract total comments count string
            String totalText = "";
            JSONObject contextualInfo = targetHeader.optJSONObject("contextualInfo");
            if (contextualInfo != null) {
                JSONArray runs = contextualInfo.optJSONArray("runs");
                if (runs != null && runs.length() > 0) {
                    JSONObject firstRun = runs.optJSONObject(0);
                    if (firstRun != null) {
                        totalText = firstRun.optString("text", "");
                    }
                }
            }

            // Extract sort continuation tokens (top vs newest)
            String topToken = "";
            String newestToken = "";

            JSONObject menu = targetHeader.optJSONObject("menu");
            if (menu != null) {
                JSONObject sortMenu = menu.optJSONObject("sortFilterSubMenuRenderer");
                if (sortMenu != null) {
                    JSONArray subMenuItems = sortMenu.optJSONArray("subMenuItems");
                    if (subMenuItems != null) {
                        if (subMenuItems.length() > 0) {
                            topToken = extractSubMenuItemToken(subMenuItems.optJSONObject(0));
                        }
                        if (subMenuItems.length() > 1) {
                            newestToken = extractSubMenuItemToken(subMenuItems.optJSONObject(1));
                        }
                    }
                }
            }

            boolean isNewest = "newest".equalsIgnoreCase(sort);
            String selectedToken = (isNewest && !newestToken.isEmpty()) ? newestToken : topToken;

            if (selectedToken.isEmpty()) {
                Log.w(TAG, "Could not extract continuation token from comments panel");
                return null;
            }

            return new CommentsTokenResult(selectedToken, totalText);
        } catch (Exception e) {
            Log.e(TAG, "Error in parseCommentsToken: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses the initial comments response.
     *
     * @param initData JSONObject response from /youtubei/v1/next
     * @return CommentsResult or null on error
     */
    public static CommentsResult parseComments(JSONObject initData) {
        if (initData == null) return null;
        try {
            String nextPageToken = "";
            Map<String, String> cMap = new HashMap<>();

            JSONArray continuationItems = findContinuationItems(initData, "reloadContinuationItemsCommand");

            if (continuationItems != null) {
                for (int i = 0; i < continuationItems.length(); i++) {
                    JSONObject c = continuationItems.optJSONObject(i);
                    if (c == null) continue;

                    if (c.has("commentThreadRenderer")) {
                        JSONObject commentThread = c.optJSONObject("commentThreadRenderer");
                        if (commentThread == null) continue;

                        String commentId = extractCommentIdFromThread(commentThread);

                        JSONObject replies = commentThread.optJSONObject("replies");
                        if (replies != null) {
                            JSONObject repliesRenderer = replies.optJSONObject("commentRepliesRenderer");
                            if (repliesRenderer != null) {
                                JSONArray subThreads = repliesRenderer.optJSONArray("subThreads");
                                if (subThreads == null) {
                                    subThreads = repliesRenderer.optJSONArray("contents");
                                }
                                if (subThreads != null && subThreads.length() > 0) {
                                    JSONObject firstSubThread = subThreads.optJSONObject(0);
                                    if (firstSubThread != null) {
                                        JSONObject cir = firstSubThread.optJSONObject("continuationItemRenderer");
                                        if (cir != null) {
                                            String token = extractContinuationToken(cir);
                                            if (!commentId.isEmpty() && !token.isEmpty()) {
                                                cMap.put(commentId, token);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (c.has("continuationItemRenderer")) {
                        JSONObject cir = c.optJSONObject("continuationItemRenderer");
                        if (cir != null) {
                            String token = extractContinuationToken(cir);
                            if (!token.isEmpty()) {
                                nextPageToken = token;
                            }
                        }
                    }
                }
            }

            List<CommentItem> commentsDataList = parseMutations(initData, cMap, false);
            return new CommentsResult(commentsDataList, nextPageToken);
        } catch (Exception e) {
            Log.e(TAG, "Error in parseComments: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses the next page of top-level comments.
     *
     * @param initData JSONObject response from /youtubei/v1/next
     * @return CommentsResult or null on error
     */
    public static CommentsResult parseCommentsNext(JSONObject initData) {
        if (initData == null) return null;
        try {
            String nextPageToken = "";
            Map<String, String> cMap = new HashMap<>();

            JSONArray continuationItems = findContinuationItems(initData, "appendContinuationItemsAction");

            if (continuationItems != null) {
                for (int i = 0; i < continuationItems.length(); i++) {
                    JSONObject c = continuationItems.optJSONObject(i);
                    if (c == null) continue;

                    if (c.has("commentThreadRenderer")) {
                        JSONObject commentThread = c.optJSONObject("commentThreadRenderer");
                        if (commentThread == null) continue;

                        String commentId = extractCommentIdFromThread(commentThread);

                        JSONObject replies = commentThread.optJSONObject("replies");
                        if (replies != null) {
                            JSONObject repliesRenderer = replies.optJSONObject("commentRepliesRenderer");
                            if (repliesRenderer != null) {
                                JSONArray subThreads = repliesRenderer.optJSONArray("subThreads");
                                if (subThreads == null) {
                                    subThreads = repliesRenderer.optJSONArray("contents");
                                }
                                if (subThreads != null && subThreads.length() > 0) {
                                    JSONObject firstSubThread = subThreads.optJSONObject(0);
                                    if (firstSubThread != null) {
                                        JSONObject cir = firstSubThread.optJSONObject("continuationItemRenderer");
                                        if (cir != null) {
                                            String token = extractContinuationToken(cir);
                                            if (!commentId.isEmpty() && !token.isEmpty()) {
                                                cMap.put(commentId, token);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (c.has("continuationItemRenderer")) {
                        JSONObject cir = c.optJSONObject("continuationItemRenderer");
                        if (cir != null) {
                            String token = extractContinuationToken(cir);
                            if (!token.isEmpty()) {
                                nextPageToken = token;
                            }
                        }
                    }
                }
            }

            List<CommentItem> commentsDataList = parseMutations(initData, cMap, false);
            return new CommentsResult(commentsDataList, nextPageToken);
        } catch (Exception e) {
            Log.e(TAG, "Error in parseCommentsNext: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses comment subthreads (replies).
     *
     * @param initData JSONObject response from /youtubei/v1/next
     * @return CommentsResult or null on error
     */
    public static CommentsResult parseSubthreads(JSONObject initData) {
        if (initData == null) return null;
        try {
            String nextPageToken = "";

            JSONArray continuationItems = findContinuationItems(initData, "appendContinuationItemsAction");

            if (continuationItems != null) {
                for (int i = 0; i < continuationItems.length(); i++) {
                    JSONObject c = continuationItems.optJSONObject(i);
                    if (c == null) continue;

                    if (c.has("continuationItemRenderer")) {
                        JSONObject cir = c.optJSONObject("continuationItemRenderer");
                        if (cir != null) {
                            String token = extractContinuationToken(cir);
                            if (!token.isEmpty()) {
                                nextPageToken = token;
                            }
                        }
                    }
                }
            }

            List<CommentItem> commentsDataList = parseMutations(initData, null, true);
            return new CommentsResult(commentsDataList, nextPageToken);
        } catch (Exception e) {
            Log.e(TAG, "Error in parseSubthreads: " + e.getMessage(), e);
            return null;
        }
    }

    // --- Private Helper Methods ---

    private static String extractSubMenuItemToken(JSONObject item) {
        if (item == null) return "";
        JSONObject serviceEndpoint = item.optJSONObject("serviceEndpoint");
        if (serviceEndpoint == null) return "";
        JSONObject continuationCommand = serviceEndpoint.optJSONObject("continuationCommand");
        if (continuationCommand == null) return "";
        return continuationCommand.optString("token", "");
    }

    private static String extractCommentIdFromThread(JSONObject commentThread) {
        if (commentThread == null) return "";
        JSONObject cvmWrap = commentThread.optJSONObject("commentViewModel");
        if (cvmWrap != null) {
            JSONObject cvm = cvmWrap.optJSONObject("commentViewModel");
            if (cvm != null && cvm.has("commentId")) {
                return cvm.optString("commentId", "");
            }
            if (cvmWrap.has("commentId")) {
                return cvmWrap.optString("commentId", "");
            }
        }
        return "";
    }

    private static String extractContinuationToken(JSONObject continuationItemRenderer) {
        if (continuationItemRenderer == null) return "";

        // 1. continuationEndpoint.continuationCommand.token
        JSONObject endpoint = continuationItemRenderer.optJSONObject("continuationEndpoint");
        if (endpoint != null) {
            JSONObject cmd = endpoint.optJSONObject("continuationCommand");
            if (cmd != null && cmd.has("token")) {
                return cmd.optString("token", "");
            }
        }

        // 2. button.buttonRenderer.command.continuationCommand.token
        JSONObject button = continuationItemRenderer.optJSONObject("button");
        if (button != null) {
            JSONObject br = button.optJSONObject("buttonRenderer");
            if (br != null) {
                JSONObject cmd = br.optJSONObject("command");
                if (cmd != null) {
                    JSONObject cc = cmd.optJSONObject("continuationCommand");
                    if (cc != null && cc.has("token")) {
                        return cc.optString("token", "");
                    }
                }
            }
        }
        return "";
    }

    private static JSONArray findContinuationItems(JSONObject initData, String preferredCommand) {
        if (initData == null) return null;
        JSONArray endpoints = initData.optJSONArray("onResponseReceivedEndpoints");
        if (endpoints == null) return null;

        // 1. Check preferred command first across all endpoints
        if (preferredCommand != null && !preferredCommand.isEmpty()) {
            for (int i = 0; i < endpoints.length(); i++) {
                JSONObject ep = endpoints.optJSONObject(i);
                if (ep != null && ep.has(preferredCommand)) {
                    JSONObject cmd = ep.optJSONObject(preferredCommand);
                    if (cmd != null && cmd.has("continuationItems")) {
                        return cmd.optJSONArray("continuationItems");
                    }
                }
            }
        }

        // 2. Fallback check reloadContinuationItemsCommand or appendContinuationItemsAction
        for (int i = 0; i < endpoints.length(); i++) {
            JSONObject ep = endpoints.optJSONObject(i);
            if (ep == null) continue;
            if (ep.has("reloadContinuationItemsCommand")) {
                JSONObject cmd = ep.optJSONObject("reloadContinuationItemsCommand");
                if (cmd != null && cmd.has("continuationItems")) {
                    return cmd.optJSONArray("continuationItems");
                }
            }
            if (ep.has("appendContinuationItemsAction")) {
                JSONObject cmd = ep.optJSONObject("appendContinuationItemsAction");
                if (cmd != null && cmd.has("continuationItems")) {
                    return cmd.optJSONArray("continuationItems");
                }
            }
        }
        return null;
    }

    private static List<CommentItem> parseMutations(JSONObject initData, Map<String, String> cMap, boolean isSubThread) {
        List<CommentItem> commentsList = new ArrayList<>();
        if (initData == null) return commentsList;

        JSONObject fwUpdates = initData.optJSONObject("frameworkUpdates");
        if (fwUpdates == null) return commentsList;
        JSONObject entityUpdate = fwUpdates.optJSONObject("entityBatchUpdate");
        if (entityUpdate == null) return commentsList;
        JSONArray mutations = entityUpdate.optJSONArray("mutations");
        if (mutations == null) return commentsList;

        for (int i = 0; i < mutations.length(); i++) {
            JSONObject element = mutations.optJSONObject(i);
            if (element == null) continue;
            JSONObject payload = element.optJSONObject("payload");
            if (payload == null) continue;
            JSONObject commentPayload = payload.optJSONObject("commentEntityPayload");
            if (commentPayload == null || !commentPayload.has("properties")) continue;

            JSONObject properties = commentPayload.optJSONObject("properties");
            if (properties == null) continue;

            JSONObject author = commentPayload.optJSONObject("author");
            JSONObject toolbar = commentPayload.optJSONObject("toolbar");

            String commentId = properties.optString("commentId", "");

            String content = "";
            Object contentObj = properties.opt("content");
            if (contentObj instanceof JSONObject) {
                content = ((JSONObject) contentObj).optString("content", "");
            } else if (contentObj instanceof String) {
                content = (String) contentObj;
            }

            String publishedTime = properties.optString("publishedTime", "");
            String authorName = author != null ? author.optString("displayName", "") : "";
            String avatar = author != null ? author.optString("avatarThumbnailUrl", "") : "";
            String channelId = author != null ? author.optString("channelId", "") : "";
            String likesCount = toolbar != null ? toolbar.optString("likeCountLiked", "") : "";
            String replyCount = toolbar != null ? toolbar.optString("replyCount", isSubThread ? "0" : "") : "";
            String subThreadToken = (!isSubThread && cMap != null && cMap.containsKey(commentId)) ? cMap.get(commentId) : "";

            commentsList.add(new CommentItem(
                    commentId,
                    content,
                    publishedTime,
                    authorName,
                    avatar,
                    channelId,
                    likesCount,
                    replyCount,
                    subThreadToken
            ));
        }
        return commentsList;
    }
}
