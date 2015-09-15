package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import retrofit.RetrofitError;

/**
 * Oh joy, we have an error from the TMDB api by way of Retrofit.
 */
public class MovieApiErrorEvent {
    private final static String TAG = MovieApiErrorEvent.class.getSimpleName();
    public RetrofitError error;
    public MovieApiErrorEvent(RetrofitError apiError)
    {
        Log.w(TAG, "MovieApiErrorEvent created with " + apiError.toString());
        error = apiError;
    }
}
