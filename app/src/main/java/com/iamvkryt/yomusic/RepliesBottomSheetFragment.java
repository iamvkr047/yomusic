package com.iamvkryt.yomusic;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.iamvkryt.yomusic.adapters.CommentAdapter;
import com.iamvkryt.yomusic.api.CommentsParser;
import com.iamvkryt.yomusic.api.YtRepository;
import com.iamvkryt.yomusic.models.Comment;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class RepliesBottomSheetFragment extends BottomSheetDialogFragment {

    private static final int[] AVATAR_COLORS = {
            0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF9575CD,
            0xFF7986CB, 0xFF64B5F6, 0xFF4FC3F7, 0xFF4DD0E1,
            0xFF4DB6AC, 0xFF81C784, 0xFFAED581, 0xFFFFB74D,
            0xFFFF8A65, 0xFFA1887F, 0xFF90A4AE
    };

    private static final String ARG_AUTHOR = "author";
    private static final String ARG_INITIAL = "initial";
    private static final String ARG_TEXT = "text";
    private static final String ARG_TIME = "time";
    private static final String ARG_AVATAR = "avatar";
    private static final String ARG_LIKES = "likes";
    private static final String ARG_SUBTHREAD_TOKEN = "subthread_token";
    private static final String ARG_CLIENT_CONTEXT = "client_context";
    private static final String ARG_API_TOKEN = "api_token";

    private YtRepository ytRepository;
    private CommentAdapter repliesAdapter;

    private String subThreadToken = "";
    private JSONObject clientContext = null;
    private String apiToken = "";
    private String nextPageToken = "";

    private boolean isLoading = false;
    private boolean isLoadingMore = false;

    private View pbRepliesLoader;
    private TextView tvRepliesEmpty;

    public static RepliesBottomSheetFragment newInstance(Comment parentComment, JSONObject clientContext, String apiToken) {
        RepliesBottomSheetFragment fragment = new RepliesBottomSheetFragment();
        Bundle args = new Bundle();
        if (parentComment != null) {
            args.putString(ARG_AUTHOR, parentComment.getAuthor());
            args.putString(ARG_INITIAL, parentComment.getAuthorInitial());
            args.putString(ARG_TEXT, parentComment.getText());
            args.putString(ARG_TIME, parentComment.getPublishedTime());
            args.putString(ARG_AVATAR, parentComment.getAvatar());
            args.putString(ARG_LIKES, parentComment.getLikesCount());
            args.putString(ARG_SUBTHREAD_TOKEN, parentComment.getSubThreadToken());
        }
        if (clientContext != null) {
            args.putString(ARG_CLIENT_CONTEXT, clientContext.toString());
        }
        args.putString(ARG_API_TOKEN, apiToken != null ? apiToken : "");
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(dialogInterface -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(false); // Disables swipe-down to dismiss
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setMaxHeight((int) (screenHeight * 0.85));
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_comment_replies, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ytRepository = new YtRepository(requireContext());

        Bundle args = getArguments();
        String author = args != null ? args.getString(ARG_AUTHOR, "") : "";
        String initial = args != null ? args.getString(ARG_INITIAL, "?") : "?";
        String text = args != null ? args.getString(ARG_TEXT, "") : "";
        String time = args != null ? args.getString(ARG_TIME, "") : "";
        String avatar = args != null ? args.getString(ARG_AVATAR, "") : "";
        String likes = args != null ? args.getString(ARG_LIKES, "") : "";
        subThreadToken = args != null ? args.getString(ARG_SUBTHREAD_TOKEN, "") : "";
        apiToken = args != null ? args.getString(ARG_API_TOKEN, "") : "";

        String contextJson = args != null ? args.getString(ARG_CLIENT_CONTEXT, "") : "";
        if (contextJson != null && !contextJson.isEmpty()) {
            try {
                clientContext = new JSONObject(contextJson);
            } catch (Exception ignored) {}
        }

        ImageButton btnClose = view.findViewById(R.id.btn_close_replies_sheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        // Bind pinned parent comment
        bindParentComment(view, author, initial, text, time, avatar, likes);

        // Setup replies RecyclerView
        RecyclerView rvReplies = view.findViewById(R.id.replies_recycler_view);
        pbRepliesLoader = view.findViewById(R.id.pb_replies_loader);
        tvRepliesEmpty = view.findViewById(R.id.tv_replies_empty);

        repliesAdapter = new CommentAdapter();
        repliesAdapter.setListener(new CommentAdapter.OnCommentInteractionListener() {
            @Override
            public void onReplyClick(Comment comment) {
                // Replies do not nest deeper
            }

            @Override
            public void onLoadMoreClick() {
                loadMoreReplies();
            }
        });

        if (rvReplies != null) {
            rvReplies.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvReplies.setAdapter(repliesAdapter);
        }

        // Fetch initial batch of replies
        loadInitialReplies();
    }

    private void bindParentComment(View root, String author, String initial, String text, String time, String avatar, String likes) {
        TextView tvAuthor = root.findViewById(R.id.tv_parent_author);
        TextView tvTime = root.findViewById(R.id.tv_parent_time);
        TextView tvText = root.findViewById(R.id.tv_parent_text);
        TextView tvLikes = root.findViewById(R.id.tv_parent_likes);
        MaterialCardView cardAvatar = root.findViewById(R.id.card_parent_avatar);
        TextView tvAvatar = root.findViewById(R.id.tv_parent_avatar);
        ImageView ivAvatar = root.findViewById(R.id.iv_parent_avatar);

        if (tvAuthor != null) tvAuthor.setText(author);
        if (tvTime != null) tvTime.setText(time);
        if (tvText != null) tvText.setText(text);
        if (tvLikes != null) tvLikes.setText(likes != null && !likes.isEmpty() ? likes : "0");

        if (tvAvatar != null) {
            tvAvatar.setText(initial != null && !initial.isEmpty() ? initial : "?");
            tvAvatar.setTextColor(Color.WHITE);
        }

        if (cardAvatar != null) {
            int colorIndex = 0;
            if (author != null && !author.isEmpty()) {
                colorIndex = Math.abs(author.hashCode()) % AVATAR_COLORS.length;
            }
            cardAvatar.setCardBackgroundColor(AVATAR_COLORS[colorIndex]);
        }

        if (avatar != null && !avatar.trim().isEmpty() && ivAvatar != null) {
            ivAvatar.setVisibility(View.VISIBLE);
            if (tvAvatar != null) tvAvatar.setVisibility(View.GONE);

            Glide.with(this)
                    .load(avatar)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
                            if (tvAvatar != null) tvAvatar.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            if (ivAvatar != null) ivAvatar.setVisibility(View.VISIBLE);
                            if (tvAvatar != null) tvAvatar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(ivAvatar);
        } else {
            if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
            if (tvAvatar != null) tvAvatar.setVisibility(View.VISIBLE);
        }
    }

    private void loadInitialReplies() {
        if (subThreadToken == null || subThreadToken.trim().isEmpty()) {
            if (tvRepliesEmpty != null) {
                tvRepliesEmpty.setText("No replies found");
                tvRepliesEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }

        isLoading = true;
        if (pbRepliesLoader != null) pbRepliesLoader.setVisibility(View.VISIBLE);
        if (tvRepliesEmpty != null) tvRepliesEmpty.setVisibility(View.GONE);

        ytRepository.getVideoCommentsSubThreads(subThreadToken, clientContext, apiToken, new YtRepository.OnCommentsSubThreadsCallback() {
            @Override
            public void onSuccess(CommentsParser.CommentsResult commentsResult) {
                isLoading = false;
                if (pbRepliesLoader != null) pbRepliesLoader.setVisibility(View.GONE);

                if (commentsResult != null) {
                    nextPageToken = commentsResult.nextPageToken;
                    List<Comment> list = commentsResult.toComments();

                    if (list.isEmpty()) {
                        if (tvRepliesEmpty != null) {
                            tvRepliesEmpty.setText("No replies found");
                            tvRepliesEmpty.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (tvRepliesEmpty != null) tvRepliesEmpty.setVisibility(View.GONE);
                    }

                    boolean hasMore = nextPageToken != null && !nextPageToken.trim().isEmpty();
                    if (repliesAdapter != null) {
                        repliesAdapter.setComments(list, hasMore);
                    }
                }
            }

            @Override
            public void onError(String message) {
                isLoading = false;
                if (pbRepliesLoader != null) pbRepliesLoader.setVisibility(View.GONE);
                if (tvRepliesEmpty != null) {
                    tvRepliesEmpty.setText(message != null ? message : "Failed to load replies");
                    tvRepliesEmpty.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadMoreReplies() {
        if (nextPageToken == null || nextPageToken.trim().isEmpty() || isLoadingMore) {
            return;
        }

        isLoadingMore = true;
        if (repliesAdapter != null) {
            repliesAdapter.setLoadingMore(true);
        }

        ytRepository.getVideoCommentsSubThreads(nextPageToken, clientContext, apiToken, new YtRepository.OnCommentsSubThreadsCallback() {
            @Override
            public void onSuccess(CommentsParser.CommentsResult commentsResult) {
                isLoadingMore = false;
                if (commentsResult != null) {
                    nextPageToken = commentsResult.nextPageToken;
                    boolean hasMore = nextPageToken != null && !nextPageToken.trim().isEmpty();
                    if (repliesAdapter != null) {
                        repliesAdapter.appendComments(commentsResult.toComments(), hasMore);
                    }
                } else {
                    if (repliesAdapter != null) {
                        repliesAdapter.setLoadingMore(false);
                    }
                }
            }

            @Override
            public void onError(String message) {
                isLoadingMore = false;
                if (repliesAdapter != null) {
                    repliesAdapter.setLoadingMore(false);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load more replies: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
