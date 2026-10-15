package com.iamvkryt.yomusic.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.models.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

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

    private List<Comment> comments;

    public CommentAdapter() {
        this.comments = new ArrayList<>();
    }

    public CommentAdapter(List<Comment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Comment> getComments() {
        return comments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments != null ? comments.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardAvatar;
        private final TextView tvAvatar;
        private final TextView tvAuthor;
        private final TextView tvTime;
        private final TextView tvText;
        private final ImageView ivLike;
        private final TextView tvLikes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAvatar = itemView.findViewById(R.id.card_avatar);
            tvAvatar = itemView.findViewById(R.id.tv_comment_avatar);
            tvAuthor = itemView.findViewById(R.id.tv_comment_author);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvText = itemView.findViewById(R.id.tv_comment_text);
            ivLike = itemView.findViewById(R.id.iv_comment_like);
            tvLikes = itemView.findViewById(R.id.tv_comment_likes);
        }

        public void bind(Comment comment) {
            if (comment == null) return;

            String author = comment.getAuthor();
            tvAuthor.setText(author != null ? author : "");
            tvTime.setText(comment.getPublishedTime() != null ? comment.getPublishedTime() : "");
            tvText.setText(comment.getText() != null ? comment.getText() : "");
            tvLikes.setText(formatLikes(comment.getLikeCount()));

            // Avatar initial and circle background color
            String initial = comment.getAuthorInitial();
            tvAvatar.setText(initial != null ? initial : "?");
            tvAvatar.setTextColor(Color.WHITE);

            int colorIndex = 0;
            if (author != null && !author.isEmpty()) {
                colorIndex = Math.abs(author.hashCode()) % AVATAR_COLORS.length;
            }
            cardAvatar.setCardBackgroundColor(AVATAR_COLORS[colorIndex]);
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
}
