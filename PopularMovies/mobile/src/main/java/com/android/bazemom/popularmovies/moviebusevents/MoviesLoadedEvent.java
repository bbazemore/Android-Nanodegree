package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import com.android.bazemom.popularmovies.moviemodel.MovieResults;

// Event posted from DispatchTMDB when a set of Movies arrives back from TMDB
public class MoviesLoadedEvent {
    private final static String TAG = MoviesLoadedEvent.class.getSimpleName();
    public MovieResults movieResults;
    public MoviesLoadedEvent(MovieResults moviesReturned)
    {
        movieResults = moviesReturned;
        Log.i(TAG, "MoviesLoadedEvent created with " + movieResults.getTotalResults() + " results");
    }
}

