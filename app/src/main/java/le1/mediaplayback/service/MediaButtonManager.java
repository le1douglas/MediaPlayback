package le1.mediaplayback.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.session.MediaButtonReceiver;

/**
 * Catches media button actions, mainly headphones buttons and similar
 */
class MediaButtonManager {
    private final MediaSessionManager mediaSession;

    MediaButtonManager(Context context, MediaSessionManager mediaSession){
        this.mediaSession = mediaSession;
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(context, MediaButtonReceiver.class);
        PendingIntent mediaButtonPendingIntent = PendingIntent.getBroadcast
                (context, 0, mediaButtonIntent, 0);
        this.mediaSession.setMediaButtonReceiver(mediaButtonPendingIntent);
    }

    void handleIntent(Intent intent){
        mediaSession.handleMediaButtonReceiverIntent(intent);
    }
}
