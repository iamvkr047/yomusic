package com.iamvkryt.yomusic.helpers;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.iamvkryt.yomusic.Song;
import com.iamvkryt.yomusic.utils.YouTubeLinkParser;

public class IntentHandler {

    public interface IntentActionCallback {
        void onPlaySong(Song song);
        void onOpenPlaylist(String playlistId);
    }

    private final Context context;
    private final IntentActionCallback callback;

    public IntentHandler(Context context, IntentActionCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action)) {
            YouTubeLinkParser.ParseResult result = YouTubeLinkParser.parseIntent(intent);
            if (result.type == YouTubeLinkParser.ResultType.SONG) {
                String videoId = result.id;
                Song song = new Song(videoId, "Loading song...", "YouTube", "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg");
                if (callback != null) {
                    callback.onPlaySong(song);
                }
                Toast.makeText(context, "Playing shared YouTube song...", Toast.LENGTH_SHORT).show();
            } else if (result.type == YouTubeLinkParser.ResultType.PLAYLIST) {
                String playlistId = result.id;
                if (callback != null) {
                    callback.onOpenPlaylist(playlistId);
                }
                Toast.makeText(context, "Loading shared YouTube playlist...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Invalid or unsupported YouTube link", Toast.LENGTH_SHORT).show();
            }
            // Clear action so orientation change / recreation doesn't trigger intent again
            intent.setAction(null);
        }
    }
}
