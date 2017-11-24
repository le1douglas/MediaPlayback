package le1.mediaplayback.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.util.Log;

import le1.mediaplayback.MainActivity;
import le1.mediaplayback.R;


public class MusicNotification {
    private static final int NOTIFICATION_ID = 666;
    private static final String CHANNEL_ID = "music";
    private static final String TAG = ("LE1_" + MusicNotification.class.getSimpleName());

    private static Action fastForwardAction = null;
    private static Action playPauseAction = null;
    private static Action rewindAction = null;
    private static Action skipToNextAction = null;
    private static Action skipToPreviousAction = null;

    private static int state;

    /**
     * Build and Update the notification based on the playback state
     * Should be called after every {@link MediaSessionManager#setPlaybackState(int, long)} call.
     *
     * @param context      Application context
     * @param service      The music service tied to the notification
     * @param mediaSession The {@link MediaSessionManager} tied to the music service
     */
    static void updateNotification(Context context, Service service, MediaSessionManager mediaSession) {
        MusicNotification.state = mediaSession.getPlaybackState();
        Builder builder = new Builder(context, CHANNEL_ID);

        Bitmap image = null;
        String title = null;
        String artist = null;
        String mediaId = null;
        if (mediaSession.getMetadata() != null) {
            image = mediaSession.getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
            if (image==null) image = BitmapFactory.decodeResource(context.getResources(),
                    R.mipmap.ic_launcher);

            title =  mediaSession.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            if (title==null){
                title = "error getting title";
            }

            artist =  mediaSession.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            if (artist==null){
                artist = "error getting artist";
            }

            mediaId =  mediaSession.getMetadata().getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            if (mediaId==null){
                mediaId = "error getting id";
            }
        }


        switch (state) {
            case PlaybackStateCompat.STATE_PAUSED:
                updateButtons(context, true);
                builder.setContentTitle(title)
                        .setContentText(artist)
                        .setSubText(mediaId);
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                updateButtons(context, true);
                builder.setContentTitle(title)
                        .setContentText(artist)
                        .setSubText(mediaId);
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                updateButtons(context, false);
                builder.setContentTitle("loading")
                        .setContentText("loading")
                        .setSubText("loading");
                break;
            case PlaybackStateCompat.STATE_ERROR:
                updateButtons(context, false);
                builder.setContentTitle("error")
                        .setContentText("error")
                        .setSubText("error");
                break;
            case PlaybackStateCompat.STATE_NONE:
                service.stopForeground(true);
                return;
            case PlaybackStateCompat.STATE_STOPPED:
                service.stopForeground(true);
                return;
            default:
                return;
        }

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(contentIntent)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setLargeIcon(image)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                .setShowWhen(false)
                .addAction(rewindAction)
                .addAction(skipToPreviousAction)
                .addAction(playPauseAction)
                .addAction(skipToNextAction)
                .addAction(fastForwardAction)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession
                                .getToken())
                        .setShowActionsInCompactView(1, 2, 3)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));

        service.startForeground(NOTIFICATION_ID, builder.build());

        // if paused or error we want the notification to be visible but dismissible
        if (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_ERROR)
            service.stopForeground(false);
    }

    /**
     * Enable or disable notification buttons, and update notification icons accordingly
     *
     * @param context Application context
     * @param enabled Whether the buttons are enabled or not
     */
    private static void updateButtons(Context context, boolean enabled) {
        if (enabled) {
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                playPauseAction = new Action(R.drawable.ic_pause_black_24dp, "play", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE));
            } else {
                playPauseAction = new Action(R.drawable.ic_play_arrow_black_24dp, "pause", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE));
            }
            skipToPreviousAction = new Action(R.drawable.ic_skip_previous_black_24dp, "previous", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
            skipToNextAction = new Action(R.drawable.ic_skip_next_black_24dp, "next", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
            rewindAction = new Action(R.drawable.ic_fast_rewind_black_24dp, "rewind", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_REWIND));
            fastForwardAction = new Action(R.drawable.ic_fast_forward_black_24dp, "fast forward", MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_FAST_FORWARD));

        } else {
            playPauseAction = new Action(R.drawable.ic_play_arrow_disabled_24dp, "play", null);
            skipToPreviousAction = new Action(R.drawable.ic_skip_previous_disabled_24dp, "previous", null);
            skipToNextAction = new Action(R.drawable.ic_skip_next_disabled_24dp, "next", null);
            rewindAction = new Action(R.drawable.ic_fast_rewind_disabled_24dp, "rewind", null);
            fastForwardAction = new Action(R.drawable.ic_fast_forward_disabled_24dp, "fast forward", null);
        }
    }
}