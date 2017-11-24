package le1.mediaplayback.callback;

import android.support.v4.media.MediaMetadataCompat;

public interface PlaybackStateListener {

    void onMetadataLoaded(MediaMetadataCompat metadata);

    void onLoading(String message);

    void onPaused(String message);

    void onPlaying(String message);

    void onStopped(String message);

    void onError(String message);
}
