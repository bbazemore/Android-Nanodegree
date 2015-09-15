package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import com.android.bazemom.popularmovies.MovieDBService;

/**
 * Request to load Movies from TMDB service
 * Pass in the type of movie list to fetch - popular, highly ranked, recent
 * and which page of the results.
 */
public class LoadMoviesEvent {
    private final static String TAG = LoadMoviesEvent.class.getSimpleName();

    public MovieDBService.MovieSortType sortType;
    public int page;  // must be between 1 and 1000
    public String api_key;

        public LoadMoviesEvent(String key, MovieDBService.MovieSortType sortTypeIn, int pageIn)
        {
            Log.i(TAG, "LoadMoviesEvent created");
            sortType = sortTypeIn;
            page = pageIn;
            api_key = key;
        }
}
