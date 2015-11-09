package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

/**
 * Request the videos / trailers for a particular movie
 */
public class LoadVideosEvent {
    private final static String TAG = LoadVideosEvent.class.getSimpleName();

    public int movieId;
    public String api_key;

    public LoadVideosEvent(String key, int movieIdin )
    {
        Log.i(TAG, "LoadVideosEvent created");
        movieId = movieIdin;
        api_key = key;
    }
}
