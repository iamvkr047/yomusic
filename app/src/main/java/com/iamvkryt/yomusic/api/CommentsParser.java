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
 * Parses initial comments token, top-level comments list, continuation (next page) comments,
 * and comment subthreads (replies).
 * Comments parser directly translated from parsers/commentsParser.ts.
 */
public class CommentsParser {
    private static final String TAG = "CommentsParser";

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
    }

    public static class CommentsTokenResult {
        public final String token;
        public final String total;

        public CommentsTokenResult(String token, String total) {
            this.token = token != null ? token : "";
            this.total = total != null ? total : "";
        }
    }

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


    public static CommentsTokenResult parseCommentsToken(JSONObject initData) {
        return parseCommentsToken(initData, "top");
    }

    /**
     * Parses the comments continuation token and total count from video page ytInitialData with sort mode.
     *
     * @param initData JSONObject representing ytInitialData
     * @param sort     "top" or "newest" (defaults to "top")
     * @return CommentsTokenResult or null if not found
     * Translated directly from parseCommentsToken in commentsParser.ts.
     */
    public static CommentsTokenResult parseCommentsToken(JSONObject initData, String sort) {
        if (initData == null) return null;
        try {
            JSONArray engagementPanels = initData.optJSONArray("engagementPanels");
            if (engagementPanels == null) {
                return null;
            }

            JSONObject targetPanel = null;
            for (int i = 0; i < engagementPanels.length(); i++) {
                JSONObject pl = engagementPanels.optJSONObject(i);
                if (pl == null) continue;
                JSONObject renderer = pl.optJSONObject("engagementPanelSectionListRenderer");
                if (renderer == null) continue;

                String tid = renderer.optString("targetId", "");
                if (tid.contains("comment")) {
                    JSONObject header = renderer.optJSONObject("header");
                    if (header != null) {
                        targetPanel = header.optJSONObject("engagementPanelTitleHeaderRenderer");
                        if (targetPanel != null) {
                            break;
                        }
                    }
                }
            }

            if (targetPanel == null) {
                return null;
            }

            String totalText = "";
            JSONObject contextualInfo = targetPanel.optJSONObject("contextualInfo");
            if (contextualInfo != null) {
                JSONArray runs = contextualInfo.optJSONArray("runs");
                if (runs != null && runs.length() > 0) {
                    JSONObject first = runs.optJSONObject(0);
                    if (first != null) {
                        totalText = first.optString("text", "");
                    }
                }
            }

            String topToken = "";
            String newestToken = "";
            JSONObject menu = targetPanel.optJSONObject("menu");
            if (menu != null) {
                JSONObject sortSubMenu = menu.optJSONObject("sortFilterSubMenuRenderer");
                if (sortSubMenu != null) {
                    JSONArray items = sortSubMenu.optJSONArray("subMenuItems");
                    if (items != null) {
                        if (items.length() > 0) {
                            JSONObject item0 = items.optJSONObject(0);
                            if (item0 != null && item0.has("serviceEndpoint")) {
                                JSONObject cmd = item0.optJSONObject("serviceEndpoint").optJSONObject("continuationCommand");
                                if (cmd != null) topToken = cmd.optString("token", "");
                            }
                        }
                        if (items.length() > 1) {
                            JSONObject item1 = items.optJSONObject(1);
                            if (item1 != null && item1.has("serviceEndpoint")) {
                                JSONObject cmd = item1.optJSONObject("serviceEndpoint").optJSONObject("continuationCommand");
                                if (cmd != null) newestToken = cmd.optString("token", "");
                            }
                        }
                    }
                }
            }

            String chosenToken = "newest".equalsIgnoreCase(sort) ? newestToken : topToken;
            if (chosenToken == null || chosenToken.isEmpty()) {
                chosenToken = topToken;
            }

            return new CommentsTokenResult(chosenToken, totalText);
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
     * Translated directly from parseComments in commentsParser.ts.
     */
    public static CommentsResult parseComments(JSONObject initData) {
        if (initData == null) return null;
        String nextPageToken = "";
        Map<String, String> cMap = new HashMap<>();

        try {
            JSONArray continuationItems = null;
            JSONArray endpoints = initData.optJSONArray("onResponseReceivedEndpoints");
            if (endpoints != null) {
                // In TS: initdata.onResponseReceivedEndpoints[1].reloadContinuationItemsCommand.continuationItems
                if (endpoints.length() > 1 && endpoints.optJSONObject(1) != null
                        && endpoints.optJSONObject(1).has("reloadContinuationItemsCommand")) {
                    continuationItems = endpoints.optJSONObject(1)
                            .optJSONObject("reloadContinuationItemsCommand")
                            .optJSONArray("continuationItems");
                }
                // Fallback scan across all endpoints for reloadContinuationItemsCommand
                if (continuationItems == null) {
                    for (int i = 0; i < endpoints.length(); i++) {
                        JSONObject ep = endpoints.optJSONObject(i);
                        if (ep != null && ep.has("reloadContinuationItemsCommand")) {
                            continuationItems = ep.optJSONObject("reloadContinuationItemsCommand")
                                    .optJSONArray("continuationItems");
                            break;
                        }
                    }
                }
            }

            if (continuationItems != null) {
                for (int i = 0; i < continuationItems.length(); i++) {
                    JSONObject c = continuationItems.optJSONObject(i);
                    if (c == null) continue;

                    if (c.has("commentThreadRenderer")) {
                        JSONObject thread = c.optJSONObject("commentThreadRenderer");
                        if (thread == null) continue;

                        String id = extractCommentId(thread);

                        if (thread.has("replies")) {
                            JSONObject replies = thread.optJSONObject("replies");
                            if (replies != null && replies.has("commentRepliesRenderer")) {
                                JSONObject crr = replies.optJSONObject("commentRepliesRenderer");
                                JSONArray subThreads = crr != null ? crr.optJSONArray("subThreads") : null;
                                if (subThreads == null && crr != null) {
                                    subThreads = crr.optJSONArray("contents");
                                }

                                if (subThreads != null && subThreads.length() > 0) {
                                    JSONObject first = subThreads.optJSONObject(0);
                                    if (first != null && first.has("continuationItemRenderer")) {
                                        JSONObject cir = first.optJSONObject("continuationItemRenderer");
                                        if (cir != null && cir.has("continuationEndpoint")) {
                                            JSONObject cep = cir.optJSONObject("continuationEndpoint");
                                            if (cep != null && cep.has("continuationCommand")) {
                                                String token = cep.optJSONObject("continuationCommand").optString("token", "");
                                                if (!id.isEmpty() && !token.isEmpty()) {
                                                    cMap.put(id, token);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (c.has("continuationItemRenderer")) {
                        JSONObject cir = c.optJSONObject("continuationItemRenderer");
                        if (cir != null && cir.has("continuationEndpoint")) {
                            JSONObject cep = cir.optJSONObject("continuationEndpoint");
                            if (cep != null && cep.has("continuationCommand")) {
                                nextPageToken = cep.optJSONObject("continuationCommand").optString("token", "");
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
     * Translated directly from parseCommentsNext in commentsParser.ts.
     */
    public static CommentsResult parseCommentsNext(JSONObject initData) {
        if (initData == null) return null;
        String nextPageToken = "";
        Map<String, String> cMap = new HashMap<>();

        try {
            JSONArray continuationItems = null;
            JSONArray endpoints = initData.optJSONArray("onResponseReceivedEndpoints");
            if (endpoints != null) {
                // In TS: initdata.onResponseReceivedEndpoints[0].appendContinuationItemsAction.continuationItems
                if (endpoints.length() > 0 && endpoints.optJSONObject(0) != null
                        && endpoints.optJSONObject(0).has("appendContinuationItemsAction")) {
                    continuationItems = endpoints.optJSONObject(0)
                            .optJSONObject("appendContinuationItemsAction")
                            .optJSONArray("continuationItems");
                }
                // Fallback scan across all endpoints
                if (continuationItems == null) {
                    for (int i = 0; i < endpoints.length(); i++) {
                        JSONObject ep = endpoints.optJSONObject(i);
                        if (ep != null && ep.has("appendContinuationItemsAction")) {
                            continuationItems = ep.optJSONObject("appendContinuationItemsAction")
                                    .optJSONArray("continuationItems");
                            break;
                        }
                    }
                }
            }

            if (continuationItems != null) {
                for (int i = 0; i < continuationItems.length(); i++) {
                    JSONObject c = continuationItems.optJSONObject(i);
                    if (c == null) continue;

                    if (c.has("commentThreadRenderer")) {
                        JSONObject thread = c.optJSONObject("commentThreadRenderer");
                        if (thread == null) continue;

                        String id = extractCommentId(thread);

                        if (thread.has("replies")) {
                            JSONObject replies = thread.optJSONObject("replies");
                            if (replies != null && replies.has("commentRepliesRenderer")) {
                                JSONObject crr = replies.optJSONObject("commentRepliesRenderer");
                                JSONArray subThreads = crr != null ? crr.optJSONArray("subThreads") : null;
                                if (subThreads == null && crr != null) {
                                    subThreads = crr.optJSONArray("contents");
                                }

                                if (subThreads != null && subThreads.length() > 0) {
                                    JSONObject first = subThreads.optJSONObject(0);
                                    if (first != null && first.has("continuationItemRenderer")) {
                                        JSONObject cir = first.optJSONObject("continuationItemRenderer");
                                        if (cir != null && cir.has("continuationEndpoint")) {
                                            JSONObject cep = cir.optJSONObject("continuationEndpoint");
                                            if (cep != null && cep.has("continuationCommand")) {
                                                String token = cep.optJSONObject("continuationCommand").optString("token", "");
                                                if (!id.isEmpty() && !token.isEmpty()) {
                                                    cMap.put(id, token);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (c.has("continuationItemRenderer")) {
                        JSONObject cir = c.optJSONObject("continuationItemRenderer");
                        if (cir != null && cir.has("continuationEndpoint")) {
                            JSONObject cep = cir.optJSONObject("continuationEndpoint");
                            if (cep != null && cep.has("continuationCommand")) {
                                nextPageToken = cep.optJSONObject("continuationCommand").optString("token", "");
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
     * Translated directly from parseSubthreads in commentsParser.ts.
     */
    public static CommentsResult parseSubthreads(JSONObject initData) {
        if (initData == null) return null;
        String nextPageToken = "";

        try {
            JSONArray continuationItems = null;
            JSONArray endpoints = initData.optJSONArray("onResponseReceivedEndpoints");
            if (endpoints != null && endpoints.length() > 0 && endpoints.optJSONObject(0) != null
                    && endpoints.optJSONObject(0).has("appendContinuationItemsAction")) {
                continuationItems = endpoints.optJSONObject(0)
                        .optJSONObject("appendContinuationItemsAction")
                        .optJSONArray("continuationItems");
            }

            if (continuationItems != null) {
                for (int i = 0; i < continuationItems.length(); i++) {
                    JSONObject c = continuationItems.optJSONObject(i);
                    if (c == null) continue;

                    // In TS: c.continuationItemRenderer.button.buttonRenderer.command.continuationCommand.token
                    if (c.has("continuationItemRenderer")) {
                        JSONObject cir = c.optJSONObject("continuationItemRenderer");
                        if (cir != null && cir.has("button")) {
                            JSONObject btn = cir.optJSONObject("button");
                            if (btn != null && btn.has("buttonRenderer")) {
                                JSONObject br = btn.optJSONObject("buttonRenderer");
                                if (br != null && br.has("command")) {
                                    JSONObject cmd = br.optJSONObject("command");
                                    if (cmd != null && cmd.has("continuationCommand")) {
                                        nextPageToken = cmd.optJSONObject("continuationCommand").optString("token", "");
                                    }
                                }
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

    private static String extractCommentId(JSONObject commentThread) {
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

    /**
     * Parses frameworkUpdates.entityBatchUpdate.mutations matching TS mutations.forEach.
     */
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
