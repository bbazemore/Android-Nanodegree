package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import com.android.bazemom.popularmovies.moviemodel.MovieResults;

// Event posted from DispatchTMDB to get whatever movies are available from the local stash.
public class MoviesAvailableEvent {
    private final static String TAG = MoviesAvailableEvent.class.getSimpleName();
    public MovieResults movieResults = null;
    public MoviesAvailableEvent(MovieResults movies)
    {
        Log.i(TAG, "MoviesAvailableEvent");
        movieResults = movies;
    }
}
