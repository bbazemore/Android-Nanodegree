package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import retrofit.RetrofitError;

/**
 * Oh joy, we have an error from the TMDB api by way of Retrofit.
 */
public class MovieApiErrorEvent {
    private final static String TAG = MovieApiErrorEvent.class.getSimpleName();
    public String objectTypeName;
    public RetrofitError error;
    public MovieApiErrorEvent(String simpleClassName, RetrofitError apiError)
    {
        Log.w(TAG, "MovieApiErrorEvent created for" + simpleClassName + " " + (null == apiError ? "" : apiError.toString()));
        objectTypeName = simpleClassName;
        error = apiError;
    }
}
