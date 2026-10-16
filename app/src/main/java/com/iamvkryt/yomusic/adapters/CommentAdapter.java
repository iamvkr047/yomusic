package com.iamvkryt.yomusic.adapters;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.models.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_COMMENT = 0;
    private static final int VIEW_TYPE_FOOTER = 1;

    private static final int[] AVATAR_COLORS = {
            0xFFE57373, // Red
            0xFFF06292, // Pink
            0xFFBA68C8, // Purple
            0xFF9575CD, // Deep Purple
            0xFF7986CB, // Indigo
            0xFF64B5F6, // Blue
            0xFF4FC3F7, // Light Blue
            0xFF4DD0E1, // Cyan
            0xFF4DB6AC, // Teal
            0xFF81C784, // Green
            0xFFAED581, // Light Green
            0xFFFFB74D, // Orange
            0xFFFF8A65, // Deep Orange
            0xFFA1887F, // Brown
            0xFF90A4AE  // Blue Grey
    };

    public interface OnCommentInteractionListener {
        void onReplyClick(Comment comment);
        void onLoadMoreClick();
    }

    private final List<Comment> comments = new ArrayList<>();
    private boolean hasMore = false;
    private boolean isLoadingMore = false;
    private OnCommentInteractionListener listener;

    public CommentAdapter() {
    }

    public void setListener(OnCommentInteractionListener listener) {
        this.listener = listener;
    }

    public void setComments(List<Comment> newComments, boolean hasMore) {
        this.comments.clear();
        if (newComments != null) {
            this.comments.addAll(newComments);
        }
        this.hasMore = hasMore;
        this.isLoadingMore = false;
        notifyDataSetChanged();
    }

    public void appendComments(List<Comment> nextBatch, boolean hasMore) {
        if (nextBatch != null && !nextBatch.isEmpty()) {
            this.comments.addAll(nextBatch);
        }
        this.hasMore = hasMore;
        this.isLoadingMore = false;
        notifyDataSetChanged();
    }

    public void setLoadingMore(boolean loading) {
        this.isLoadingMore = loading;
        if (hasMore) {
            notifyItemChanged(comments.size());
        }
    }

    public void clear() {
        this.comments.clear();
        this.hasMore = false;
        this.isLoadingMore = false;
        notifyDataSetChanged();
    }

    public List<Comment> getComments() {
        return comments;
    }

    @Override
    public int getItemViewType(int position) {
        if (hasMore && position == comments.size()) {
            return VIEW_TYPE_FOOTER;
        }
        return VIEW_TYPE_COMMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment_load_more, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CommentViewHolder) {
            Comment comment = comments.get(position);
            ((CommentViewHolder) holder).bind(comment, listener);
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(isLoadingMore, listener);
        }
    }

    @Override
    public int getItemCount() {
        return comments.size() + (hasMore ? 1 : 0);
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardAvatar;
        private final TextView tvAvatar;
        private final ImageView ivAvatar;
        private final TextView tvAuthor;
        private final TextView tvTime;
        private final TextView tvText;
        private final ImageView ivLike;
        private final TextView tvLikes;
        private final TextView btnReplies;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAvatar = itemView.findViewById(R.id.card_avatar);
            tvAvatar = itemView.findViewById(R.id.tv_comment_avatar);
            ivAvatar = itemView.findViewById(R.id.iv_comment_avatar);
            tvAuthor = itemView.findViewById(R.id.tv_comment_author);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvText = itemView.findViewById(R.id.tv_comment_text);
            ivLike = itemView.findViewById(R.id.iv_comment_like);
            tvLikes = itemView.findViewById(R.id.tv_comment_likes);
            btnReplies = itemView.findViewById(R.id.btn_comment_replies);
        }

        public void bind(Comment comment, OnCommentInteractionListener listener) {
            if (comment == null) return;

            String author = comment.getAuthor();
            tvAuthor.setText(author != null ? author : "");
            tvTime.setText(comment.getPublishedTime() != null ? comment.getPublishedTime() : "");
            tvText.setText(comment.getText() != null ? comment.getText() : "");

            // Likes
            if (comment.getLikesCount() != null && !comment.getLikesCount().isEmpty()) {
                tvLikes.setText(comment.getLikesCount());
            } else {
                tvLikes.setText(formatLikes(comment.getLikeCount()));
            }

            // Avatar initial fallback setup
            String initial = comment.getAuthorInitial();
            tvAvatar.setText(initial != null ? initial : "?");
            tvAvatar.setTextColor(Color.WHITE);

            int colorIndex = 0;
            if (author != null && !author.isEmpty()) {
                colorIndex = Math.abs(author.hashCode()) % AVATAR_COLORS.length;
            }
            cardAvatar.setCardBackgroundColor(AVATAR_COLORS[colorIndex]);

            // Avatar image loading via Glide
            String avatarUrl = comment.getAvatar();
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                ivAvatar.setVisibility(View.VISIBLE);
                tvAvatar.setVisibility(View.GONE);

                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                ivAvatar.setVisibility(View.GONE);
                                tvAvatar.setVisibility(View.VISIBLE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource,
                                                           boolean isFirstResource) {
                                ivAvatar.setVisibility(View.VISIBLE);
                                tvAvatar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(ivAvatar);
            } else {
                ivAvatar.setVisibility(View.GONE);
                tvAvatar.setVisibility(View.VISIBLE);
            }

            // Replies button
            String replyCount = comment.getReplyCount();
            if (replyCount != null && !replyCount.trim().isEmpty() && !replyCount.trim().equals("0")) {
                String trimmedCount = replyCount.trim();
                String repliesLabel = "1".equals(trimmedCount) ? "1 Reply" : (trimmedCount + " Replies");
                btnReplies.setText(repliesLabel);
                btnReplies.setVisibility(View.VISIBLE);
                btnReplies.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onReplyClick(comment);
                    }
                });
            } else {
                btnReplies.setVisibility(View.GONE);
                btnReplies.setOnClickListener(null);
            }
        }

        private String formatLikes(int likes) {
            if (likes >= 1_000_000) {
                return String.format(Locale.getDefault(), "%.1fM", likes / 1_000_000.0);
            } else if (likes >= 1000) {
                return String.format(Locale.getDefault(), "%.1fK", likes / 1000.0);
            } else {
                return String.valueOf(likes);
            }
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        private final MaterialButton btnLoadMore;
        private final View pbLoadMore;

        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            btnLoadMore = itemView.findViewById(R.id.btn_load_more);
            pbLoadMore = itemView.findViewById(R.id.pb_load_more);
        }

        public void bind(boolean isLoadingMore, OnCommentInteractionListener listener) {
            if (isLoadingMore) {
                btnLoadMore.setVisibility(View.INVISIBLE);
                if (pbLoadMore != null) pbLoadMore.setVisibility(View.VISIBLE);
            } else {
                btnLoadMore.setVisibility(View.VISIBLE);
                if (pbLoadMore != null) pbLoadMore.setVisibility(View.GONE);
                btnLoadMore.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onLoadMoreClick();
                    }
                });
            }
        }
    }
}
