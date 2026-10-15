package com.iamvkryt.yomusic.models;

public class Comment {
    private String commentId;
    private String author;
    private String authorInitial; // first letter, for avatar circle
    private String text;
    private int likeCount;
    private String likesCount; // raw formatted string e.g. "1.2K"
    private String publishedTime; // e.g. "2 days ago"
    private String avatar;
    private String channelId;
    private String replyCount;
    private String subThreadToken;

    public Comment() {
    }

    /**
     * Primary constructor matching all real YouTube API comment properties.
     */
    public Comment(String commentId, String author, String text, String publishedTime,
                   String avatar, String channelId, String likesCount, String replyCount, String subThreadToken) {
        this.commentId = commentId != null ? commentId : "";
        this.author = author != null ? author : "";
        this.authorInitial = extractInitial(this.author);
        this.text = text != null ? text : "";
        this.publishedTime = publishedTime != null ? publishedTime : "";
        this.avatar = avatar != null ? avatar : "";
        this.channelId = channelId != null ? channelId : "";
        this.likesCount = likesCount != null ? likesCount : "";
        this.likeCount = parseLikes(this.likesCount);
        this.replyCount = replyCount != null ? replyCount : "";
        this.subThreadToken = subThreadToken != null ? subThreadToken : "";
    }

    /**
     * Convenience constructor with legacy parameters.
     */
    public Comment(String author, String text, int likeCount, String publishedTime) {
        this("", author, text, publishedTime, "", "", String.valueOf(likeCount), "0", "");
    }

    public static int parseLikes(String likesStr) {
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

    private String extractInitial(String author) {
        if (author != null && !author.trim().isEmpty()) {
            return author.trim().substring(0, 1).toUpperCase();
        }
        return "?";
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
        if (this.authorInitial == null || this.authorInitial.isEmpty()) {
            this.authorInitial = extractInitial(author);
        }
    }

    public String getAuthorInitial() {
        if (authorInitial == null || authorInitial.isEmpty()) {
            return extractInitial(author);
        }
        return authorInitial;
    }

    public void setAuthorInitial(String authorInitial) {
        this.authorInitial = authorInitial;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getContent() {
        return text;
    }

    public void setContent(String content) {
        this.text = content;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public String getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(String likesCount) {
        this.likesCount = likesCount;
        if (this.likeCount == 0 && likesCount != null && !likesCount.isEmpty()) {
            this.likeCount = parseLikes(likesCount);
        }
    }

    public String getPublishedTime() {
        return publishedTime;
    }

    public void setPublishedTime(String publishedTime) {
        this.publishedTime = publishedTime;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(String replyCount) {
        this.replyCount = replyCount;
    }

    public String getSubThreadToken() {
        return subThreadToken;
    }

    public void setSubThreadToken(String subThreadToken) {
        this.subThreadToken = subThreadToken;
    }
}
