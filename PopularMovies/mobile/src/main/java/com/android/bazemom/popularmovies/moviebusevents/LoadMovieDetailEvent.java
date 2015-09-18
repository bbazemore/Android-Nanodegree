package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

/**
 * When the UI wants to get detailed information about one movie, send
 * the LoadMovieDetailEvent.
 */
public class LoadMovieDetailEvent {
    private final static String TAG = LoadMovieDetailEvent.class.getSimpleName();

    public int movieId;
    public String api_key;

    public LoadMovieDetailEvent(String key, int id)
    {
        Log.i(TAG, "LoadMovieDetailEvent created");
        api_key = key;
        movieId = id;
    }
}
