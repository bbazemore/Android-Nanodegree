package com.android.bazemom.popularmovies.moviemodel;

import android.support.annotation.NonNull;
import android.util.Log;

import com.android.bazemom.popularmovies.MovieDBService;
import com.android.bazemom.popularmovies.moviebusevents.LoadMoviesEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieApiErrorEvent;
import com.android.bazemom.popularmovies.moviebusevents.MoviesAvailableEvent;
import com.android.bazemom.popularmovies.moviebusevents.MoviesLoadedEvent;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Singleton class to create one connection to the movie database service, since
 * it is expensive to set up.
 * This class also stores the GSON from the most recent fetch of the Movie list.
 * Based on article by Josip Jurisic https://medium.com/android-news/so-retrofit-6e00670aaeb2
 */
public class DispatchTMDB {
    private final static String TAG = DispatchTMDB.class.getSimpleName();

    private static DispatchTMDB sInstance;   // singleton holder

    private final MovieDBService movieDBService;  // Retrofit to TMDB API
    private static Bus mBus; // Otto bus

    private MovieResults mLastMovieSet = null; // cache results for now
    private Boolean mAPIRequestInProcess = false;
    private int mPageRequested = 0;

    public synchronized static DispatchTMDB getInstance(@NonNull  MovieDBService movieAPI, Bus bus) {
        if (sInstance == null) {
            sInstance = new DispatchTMDB(movieAPI, bus);
        }
        return sInstance;
    }

    private DispatchTMDB(@NonNull MovieDBService movieAPI, Bus bus) {
        movieDBService = movieAPI;
        mBus = bus;
    }

    public static DispatchTMDB getInstance() {
        if (sInstance == null) {
            Log.e(TAG, "DispatchTMDB should have been instantiated by the main application before now.");
        }
        return sInstance;
    }

    public static Bus shareBus() {return mBus;}

    @Subscribe
    public void onLoadMovies(LoadMoviesEvent event) {
        // Avoid having multiple calls out at once. Patience is a virtue.
        if (mAPIRequestInProcess)
        {
            // I think we are going to end up asking for all the movies, regardless
            // of sort order. So for now let's skip checking which sort order was requested.
            if (mPageRequested == event.page)
                return;
        }
        // Keep track of the last outstanding request
        mAPIRequestInProcess = true;
        mPageRequested = event.page;

        movieDBService.getMoviesList(/*event.sortType,*/ event.page, event.api_key, new retrofit.Callback<MovieResults>() {
            @Override
            public void success (MovieResults response, Response rawResponse){
                mLastMovieSet = response;
                mBus.post(new MoviesLoadedEvent(response));
                mAPIRequestInProcess = false;
            }

            @Override
            public void failure (RetrofitError error){
                mAPIRequestInProcess = false;
                mBus.post(new MovieApiErrorEvent(error));
            }
        });

    }
   /* When subscribing to events it is often desired to also fetch the current known value for
    * specific events (e.g., current list of movies). To address this common paradigm,
    * Otto adds the concept of 'Producers' which provide an immediate callback to any subscribers upon their registration.
    */
    @Produce
    public MoviesAvailableEvent getMoviesNow() {
        // Assuming 'lastMovieSet' exists.
        return new MoviesAvailableEvent(this.mLastMovieSet);
    }
}
