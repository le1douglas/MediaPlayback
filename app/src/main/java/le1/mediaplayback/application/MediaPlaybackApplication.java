package le1.mediaplayback.application;

import android.app.Application;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.util.Log;

import le1.mediaplayback.MusicControl;

public class MediaPlaybackApplication extends Application {
    private static final String TAG = "MediaPlaybackApp";

    /**
     * The only instance of {@link MusicControl} used by every component in the app
     */
    private MusicControl musicControl;

    private static boolean isAppOpen;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver(this));
        musicControl = new MusicControl(this);
    }

    /**
     * @return The only {@link MusicControl} instance to be used
     */
    public MusicControl getMusicControl() {
        return musicControl;
    }


    /**
     * @return True if app it's on the recent task screen or in the foreground,
     * false otherwise (i.e the service is the only thing running)
     */
    public static boolean isAppOpen() {
        return isAppOpen;
    }

    public static void setAppOpen(boolean appOpen) {
        Log.d(TAG, "setAppOpen: " + appOpen);
        isAppOpen = appOpen;
    }
}
