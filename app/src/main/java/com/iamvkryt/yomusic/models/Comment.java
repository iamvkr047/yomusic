package com.iamvkryt.yomusic.models;

public class Comment {
    private String author;
    private String authorInitial; // first letter, for avatar circle
    private String text;
    private int likeCount;
    private String publishedTime; // e.g. "2 days ago"

    public Comment() {
    }

    public Comment(String author, String text, int likeCount, String publishedTime) {
        this.author = author;
        this.authorInitial = extractInitial(author);
        this.text = text;
        this.likeCount = likeCount;
        this.publishedTime = publishedTime;
    }

    public Comment(String author, String authorInitial, String text, int likeCount, String publishedTime) {
        this.author = author;
        this.authorInitial = (authorInitial != null && !authorInitial.isEmpty()) ? authorInitial : extractInitial(author);
        this.text = text;
        this.likeCount = likeCount;
        this.publishedTime = publishedTime;
    }

    private String extractInitial(String author) {
        if (author != null && !author.trim().isEmpty()) {
            return author.trim().substring(0, 1).toUpperCase();
        }
        return "?";
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

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public String getPublishedTime() {
        return publishedTime;
    }

    public void setPublishedTime(String publishedTime) {
        this.publishedTime = publishedTime;
    }
}
