package com.iamvkryt.yomusic.helpers;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.iamvkryt.yomusic.MainActivity;
import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.adapters.CommentAdapter;
import com.iamvkryt.yomusic.api.CommentsParser;
import com.iamvkryt.yomusic.api.YtRepository;
import com.iamvkryt.yomusic.models.Comment;

import org.json.JSONObject;

import java.util.List;

public class VideoCommentsHelper {

    private final AppCompatActivity activity;
    private final YtRepository ytRepository;

    private LinearLayout webviewContainer;
    private View layoutVideoInfo;
    private TextView tvOverlayVideoTitle;
    private TextView tvOverlayVideoArtist;
    private ImageButton btnToggleComments;
    private View viewCommentsDivider;

    private LinearLayout layoutCommentsContainer;
    private TextView tvCommentsCount;
    private RecyclerView commentsRecyclerView;
    private View pbCommentsLoader;
    private TextView tvCommentsMessage;

    private CommentAdapter commentAdapter;

    private boolean isOverlayVisible = false;
    private boolean isCommentsExpanded = false;

    private String currentVideoId = "";
    private String loadedCommentsVideoId = null;
    private String nextPageToken = null;
    private JSONObject clientContext = null;
    private String apiToken = null;

    private boolean isLoadingComments = false;
    private boolean isLoadingMore = false;

    public VideoCommentsHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.ytRepository = new YtRepository(activity);
        initViews();
    }

    private void initViews() {
        webviewContainer = activity.findViewById(R.id.webview_container);
        layoutVideoInfo = activity.findViewById(R.id.layout_video_info);
        tvOverlayVideoTitle = activity.findViewById(R.id.tv_overlay_video_title);
        tvOverlayVideoArtist = activity.findViewById(R.id.tv_overlay_video_artist);
        btnToggleComments = activity.findViewById(R.id.btn_toggle_comments);
        viewCommentsDivider = activity.findViewById(R.id.view_comments_divider);

        layoutCommentsContainer = activity.findViewById(R.id.layout_comments_container);
        tvCommentsCount = activity.findViewById(R.id.tv_comments_count);
        commentsRecyclerView = activity.findViewById(R.id.comments_recycler_view);
        pbCommentsLoader = activity.findViewById(R.id.pb_comments_loader);
        tvCommentsMessage = activity.findViewById(R.id.tv_comments_message);

        commentAdapter = new CommentAdapter();
        commentAdapter.setListener(new CommentAdapter.OnCommentInteractionListener() {
            @Override
            public void onReplyClick(Comment comment) {
                String replies = comment.getReplyCount();
                Toast.makeText(activity, "Subthread replies (" + replies + ") will be available in the next phase", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadMoreClick() {
                loadMoreComments();
            }
        });

        if (commentsRecyclerView != null) {
            commentsRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
            commentsRecyclerView.setAdapter(commentAdapter);
        }

        if (btnToggleComments != null) {
            btnToggleComments.setOnClickListener(v -> toggleComments());
        }

        // Hide overlay subcomponents initially while overlay is behind main UI
        if (layoutVideoInfo != null) {
            layoutVideoInfo.setVisibility(View.INVISIBLE);
        }
        if (viewCommentsDivider != null) {
            viewCommentsDivider.setVisibility(View.INVISIBLE);
        }
        if (layoutCommentsContainer != null) {
            layoutCommentsContainer.setVisibility(View.GONE);
        }
    }

    public void updateVideoInfo(String title, String artist, String videoId) {
        if (tvOverlayVideoTitle != null && title != null && !title.isEmpty()) {
            tvOverlayVideoTitle.setText(title);
        }
        if (tvOverlayVideoArtist != null && artist != null && !artist.isEmpty()) {
            tvOverlayVideoArtist.setText(artist);
        }

        if (videoId != null && !videoId.isEmpty() && !videoId.equals(currentVideoId)) {
            currentVideoId = videoId;
            resetCommentsState();
        }
    }

    private void resetCommentsState() {
        loadedCommentsVideoId = null;
        nextPageToken = null;
        clientContext = null;
        apiToken = null;
        isLoadingComments = false;
        isLoadingMore = false;

        if (commentAdapter != null) {
            commentAdapter.clear();
        }
        if (tvCommentsCount != null) {
            tvCommentsCount.setText("Comments");
        }
        if (pbCommentsLoader != null) {
            pbCommentsLoader.setVisibility(View.GONE);
        }
        if (tvCommentsMessage != null) {
            tvCommentsMessage.setVisibility(View.GONE);
        }

        // Collapse comments by default on video change
        setCommentsExpanded(false);
    }

    public void toggleComments() {
        setCommentsExpanded(!isCommentsExpanded);
        if (isCommentsExpanded) {
            if (loadedCommentsVideoId == null || !loadedCommentsVideoId.equals(currentVideoId)) {
                loadComments(currentVideoId);
            }
        }
    }

    private void setCommentsExpanded(boolean expanded) {
        isCommentsExpanded = expanded;
        if (layoutCommentsContainer != null) {
            layoutCommentsContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }

        if (btnToggleComments != null) {
            int activeColor = MaterialColors.getColor(btnToggleComments, androidx.appcompat.R.attr.colorPrimary);
            int inactiveColor = MaterialColors.getColor(btnToggleComments, com.google.android.material.R.attr.colorOnSurface);
            btnToggleComments.setImageTintList(ColorStateList.valueOf(expanded ? activeColor : inactiveColor));
        }
    }

    private void loadComments(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            if (activity instanceof MainActivity) {
                videoId = ((MainActivity) activity).getCurrentVideoId();
            }
        }

        if (videoId == null || videoId.trim().isEmpty()) {
            if (tvCommentsMessage != null) {
                tvCommentsMessage.setText("No video selected");
                tvCommentsMessage.setVisibility(View.VISIBLE);
            }
            return;
        }

        currentVideoId = videoId;
        isLoadingComments = true;
        if (pbCommentsLoader != null) pbCommentsLoader.setVisibility(View.VISIBLE);
        if (tvCommentsMessage != null) tvCommentsMessage.setVisibility(View.GONE);
        if (commentAdapter != null) commentAdapter.clear();

        ytRepository.getVideoComments(videoId, new YtRepository.OnCommentsCallback() {
            @Override
            public void onSuccess(CommentsParser.CommentsResult commentsResult, String total, JSONObject context, String apiTokenVal) {
                isLoadingComments = false;
                if (pbCommentsLoader != null) pbCommentsLoader.setVisibility(View.GONE);

                loadedCommentsVideoId = currentVideoId;
                clientContext = context;
                apiToken = apiTokenVal;

                if (commentsResult != null) {
                    nextPageToken = commentsResult.nextPageToken;
                    List<Comment> list = commentsResult.toComments();

                    if (tvCommentsCount != null) {
                        if (total != null && !total.trim().isEmpty()) {
                            tvCommentsCount.setText("Comments • " + total.trim());
                        } else {
                            tvCommentsCount.setText("Comments (" + list.size() + ")");
                        }
                    }

                    if (list.isEmpty()) {
                        if (tvCommentsMessage != null) {
                            tvCommentsMessage.setText("No comments yet");
                            tvCommentsMessage.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (tvCommentsMessage != null) tvCommentsMessage.setVisibility(View.GONE);
                    }

                    boolean hasMore = nextPageToken != null && !nextPageToken.trim().isEmpty();
                    if (commentAdapter != null) {
                        commentAdapter.setComments(list, hasMore);
                    }
                }
            }

            @Override
            public void onError(String message) {
                isLoadingComments = false;
                if (pbCommentsLoader != null) pbCommentsLoader.setVisibility(View.GONE);
                if (tvCommentsMessage != null) {
                    tvCommentsMessage.setText(message != null ? message : "Failed to load comments");
                    tvCommentsMessage.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadMoreComments() {
        if (nextPageToken == null || nextPageToken.trim().isEmpty() || isLoadingMore) {
            return;
        }

        isLoadingMore = true;
        if (commentAdapter != null) {
            commentAdapter.setLoadingMore(true);
        }

        ytRepository.getVideoCommentsNext(nextPageToken, clientContext, apiToken, new YtRepository.OnCommentsNextCallback() {
            @Override
            public void onSuccess(CommentsParser.CommentsResult commentsResult) {
                isLoadingMore = false;
                if (commentsResult != null) {
                    nextPageToken = commentsResult.nextPageToken;
                    boolean hasMore = nextPageToken != null && !nextPageToken.trim().isEmpty();
                    if (commentAdapter != null) {
                        commentAdapter.appendComments(commentsResult.toComments(), hasMore);
                    }
                } else {
                    if (commentAdapter != null) {
                        commentAdapter.setLoadingMore(false);
                    }
                }
            }

            @Override
            public void onError(String message) {
                isLoadingMore = false;
                if (commentAdapter != null) {
                    commentAdapter.setLoadingMore(false);
                }
                Toast.makeText(activity, "Failed to load more comments: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showVideoCommentsOverlay() {
        if (isOverlayVisible) return;

        if (webviewContainer != null) {
            webviewContainer.bringToFront();
            float density = activity.getResources().getDisplayMetrics().density;
            webviewContainer.setTranslationZ(50f * density); // Above miniPlayer (6dp) & bottomNav (8dp)
            webviewContainer.requestLayout();
        }

        if (layoutVideoInfo != null) {
            layoutVideoInfo.setVisibility(View.VISIBLE);
        }
        if (viewCommentsDivider != null) {
            viewCommentsDivider.setVisibility(View.VISIBLE);
        }

        // Keep comments collapsed by default
        if (layoutCommentsContainer != null) {
            layoutCommentsContainer.setVisibility(isCommentsExpanded ? View.VISIBLE : View.GONE);
        }

        // Sync metadata from activity if available
        if (activity instanceof MainActivity) {
            MainActivity main = (MainActivity) activity;
            updateVideoInfo(main.getCurrentTitle(), main.getCurrentArtist(), main.getCurrentVideoId());
        }

        isOverlayVisible = true;
    }

    public void hideVideoCommentsOverlay() {
        if (!isOverlayVisible) return;

        if (webviewContainer != null) {
            webviewContainer.setTranslationZ(0f);
        }

        if (layoutVideoInfo != null) {
            layoutVideoInfo.setVisibility(View.INVISIBLE);
        }
        if (viewCommentsDivider != null) {
            viewCommentsDivider.setVisibility(View.INVISIBLE);
        }
        if (layoutCommentsContainer != null) {
            layoutCommentsContainer.setVisibility(View.GONE);
        }

        // Bring other UI layers back to front to restore original draw order
        View appBar = activity.findViewById(R.id.appBarLayout);
        if (appBar != null) appBar.bringToFront();

        View fragmentContainer = activity.findViewById(R.id.fragment_container);
        if (fragmentContainer != null) fragmentContainer.bringToFront();

        View miniPlayerContainer = activity.findViewById(R.id.mini_player_container);
        if (miniPlayerContainer != null) miniPlayerContainer.bringToFront();

        View bottomNav = activity.findViewById(R.id.bottom_navigation);
        if (bottomNav != null) bottomNav.bringToFront();

        if (webviewContainer != null) {
            webviewContainer.requestLayout();
        }

        isOverlayVisible = false;
    }

    public boolean isVideoCommentsVisible() {
        return isOverlayVisible;
    }
}
