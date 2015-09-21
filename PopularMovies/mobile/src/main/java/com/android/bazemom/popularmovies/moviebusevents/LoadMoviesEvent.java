package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

/**
 * Request to load Movies from TMDB service
 * Pass in the type of movie list to fetch - popular, highly ranked, recent
 * and which page of the results.
 */
public class LoadMoviesEvent {
    private final static String TAG = LoadMoviesEvent.class.getSimpleName();

    //public MovieDBService.MovieSortType sortType;
    public String sortType; // must match one of MovieSortType values
    public int page;  // 1 means first page, 2 means whatever the next page is, the UI doesn't need any more detail than that.
    public String api_key;

        public LoadMoviesEvent(String key, String sortTypeIn , int pageIn )
        {
            Log.i(TAG, "LoadMoviesEvent created");
            sortType = sortTypeIn;
            page = pageIn;
            api_key = key;
        }
}
