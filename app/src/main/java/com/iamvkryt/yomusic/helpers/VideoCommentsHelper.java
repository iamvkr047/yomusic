package com.iamvkryt.yomusic.helpers;

import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iamvkryt.yomusic.R;
import com.iamvkryt.yomusic.adapters.CommentAdapter;
import com.iamvkryt.yomusic.models.Comment;

import java.util.ArrayList;
import java.util.List;

public class VideoCommentsHelper {

    private final AppCompatActivity activity;
    private LinearLayout webviewContainer;
    private View commentsHeader;
    private RecyclerView commentsRecyclerView;
    private CommentAdapter commentAdapter;
    private ImageButton btnCloseVideoOverlay;
    private boolean isVideoCommentsVisible = false;

    public VideoCommentsHelper(AppCompatActivity activity) {
        this.activity = activity;
        initViews();
    }

    private void initViews() {
        webviewContainer = activity.findViewById(R.id.webview_container);
        commentsHeader = activity.findViewById(R.id.layout_comments_header);
        commentsRecyclerView = activity.findViewById(R.id.comments_recycler_view);
        btnCloseVideoOverlay = activity.findViewById(R.id.btn_close_video_overlay);

        commentAdapter = new CommentAdapter();
        if (commentsRecyclerView != null) {
            commentsRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
            commentsRecyclerView.setAdapter(commentAdapter);
            commentsRecyclerView.setVisibility(View.INVISIBLE);
        }

        // Keep comments list and header invisible while overlay is behind main UI
        if (commentsHeader != null) {
            commentsHeader.setVisibility(View.INVISIBLE);
        }

        if (btnCloseVideoOverlay != null) {
            btnCloseVideoOverlay.setOnClickListener(v -> hideVideoCommentsOverlay());
        }
    }

    public void showVideoCommentsOverlay() {
        if (isVideoCommentsVisible) return;

        if (webviewContainer != null) {
            webviewContainer.bringToFront();
            float density = activity.getResources().getDisplayMetrics().density;
            webviewContainer.setTranslationZ(50f * density); // Above miniPlayer (6dp) & bottomNav (8dp)
            webviewContainer.requestLayout();
        }

        if (commentsHeader != null) {
            commentsHeader.setVisibility(View.VISIBLE);
        }
        if (commentsRecyclerView != null) {
            commentsRecyclerView.setVisibility(View.VISIBLE);
        }

        if (commentAdapter != null) {
            commentAdapter.setComments(getDummyComments());
        }

        isVideoCommentsVisible = true;
    }

    public void hideVideoCommentsOverlay() {
        if (!isVideoCommentsVisible) return;

        if (webviewContainer != null) {
            webviewContainer.setTranslationZ(0f);
        }

        // Hide comments list and header so they don't bleed through margins/shadows around floating views
        if (commentsHeader != null) {
            commentsHeader.setVisibility(View.INVISIBLE);
        }
        if (commentsRecyclerView != null) {
            commentsRecyclerView.setVisibility(View.INVISIBLE);
        }

        // Bring all other views to front (restores original draw order)
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
        isVideoCommentsVisible = false;
    }

    public boolean isVideoCommentsVisible() {
        return isVideoCommentsVisible;
    }

    public List<Comment> getDummyComments() {
        List<Comment> comments = new ArrayList<>();
        comments.add(new Comment("Alex Rivera", "This track hits differently late at night 🔥", 1240, "2 hours ago"));
        comments.add(new Comment("Elena Rostova", "The production quality on this is insane. Bass is so clean!", 843, "5 hours ago"));
        comments.add(new Comment("Marcus Chen", "Been on repeat all week! Never gets old.", 320, "1 day ago"));
        comments.add(new Comment("Sarah Jenkins", "That transition at 1:45 gave me chills 🙌", 512, "2 days ago"));
        comments.add(new Comment("David Kim", "Can't believe this only has so few views, underrated masterpiece.", 189, "3 days ago"));
        comments.add(new Comment("Chloe Bennett", "Listening to this while coding, perfect focus vibe.", 94, "4 days ago"));
        comments.add(new Comment("Zack Taylor", "Best release of this year by far. 10/10!", 415, "1 week ago"));
        comments.add(new Comment("Maya Patel", "The vocals and melodies blend so smoothly together ❤️", 267, "1 week ago"));
        comments.add(new Comment("Liam Wilson", "Found this gem randomly and now I'm hooked.", 78, "2 weeks ago"));
        comments.add(new Comment("Sophia Martinez", "Need a 10 hour version of this immediately please!", 621, "2 weeks ago"));
        return comments;
    }
}
