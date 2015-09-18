package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import com.android.bazemom.popularmovies.MovieDetail;
import com.android.bazemom.popularmovies.moviemodel.MovieDetailModel;

/**
 *  MovieDetailLoaded event is sent out when the DispatchTMBD get back the detailed data for one movie.
 */
public class MovieDetailLoadedEvent  {
    private final static String TAG = MovieDetailLoadedEvent.class.getSimpleName();
    public MovieDetail movieResult;
    public MovieDetailLoadedEvent(MovieDetailModel movieDetailReturned)
    {
        movieResult = new MovieDetail( movieDetailReturned);
        Log.i(TAG, "MovieDetailLoadedEvent created for " + movieDetailReturned.getOriginalTitle());
    }
}
