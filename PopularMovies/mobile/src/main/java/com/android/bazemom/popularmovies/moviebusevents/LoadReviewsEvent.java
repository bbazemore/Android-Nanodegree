package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

/**
 * Request the reviews for a particular movie
 */
public class LoadReviewsEvent {
    private final static String TAG = LoadReviewsEvent.class.getSimpleName();

    public int movieId;
    public int page;  // 1 means first page, 2 means whatever the next page is, the UI doesn't need any more detail than that.
    public String api_key;

    public LoadReviewsEvent(String key, int movieIdin , int pageIn )
    {
        Log.i(TAG, "LoadReviewsEvent created");
        movieId = movieIdin;
        page = pageIn;
        api_key = key;
    }
}

