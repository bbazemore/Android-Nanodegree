package com.android.bazemom.popularmovies.moviemodel;

import android.support.annotation.NonNull;
import android.util.Log;

import com.android.bazemom.popularmovies.moviebusevents.LoadMovieDetailEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadMoviesEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadReviewsEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadVideosEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieApiErrorEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieDetailLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.MoviesAvailableEvent;
import com.android.bazemom.popularmovies.moviebusevents.MoviesLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.ReviewsLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.VideosLoadedEvent;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Singleton class to create one connection to the movie database service, since
 * it is expensive to set up.
 * This class also stores the MovieResults object from the most recent fetch of the Movie list.
 * Based on article by Josip Jurisic https://medium.com/android-news/so-retrofit-6e00670aaeb2
 */
/*
 * Next up - connect to a local database, store the movies that are favorites,
 * Use ContentProvider, SyncAdapter, Loader, Cursor
 * http://www.svenkapudija.com/2014/08/14/restful-android/
 */
public class DispatchTMDB {
    private final static String TAG = DispatchTMDB.class.getSimpleName();

    private static DispatchTMDB sInstance;   // singleton holder

    private final MovieDBService movieDBService;  // Retrofit to TMDB API
    private static Bus mBus; // Otto bus

    private MovieResults mLastMovieSet = null; // cache results for now
    private Boolean mAPIRequestInProcess = false;
    private int mAPIDetailRequestMovieId = 0;

    private int mPageRequested = 1;  // request the next page each time
    private int mAPIReviewRequestMovieId = 0;
    private int mReviewPageRequested = 1;
    private int mAPITrailerRequestMovieId = 0;


    public synchronized static DispatchTMDB getInstance(@NonNull MovieDBService movieAPI, Bus bus) {
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

    public static Bus shareBus() {
        return mBus;
    }

    @Subscribe
    public void onLoadMovies(LoadMoviesEvent event) {

        // Avoid having multiple calls out at once. Patience is a virtue.
        if (mAPIRequestInProcess) {
            // Special case, we don't want to miss a start over request
            if (!(event.page == 1 && mPageRequested != 2))
                // Just a duplicate run-of-the-mill request
                // The pages requested from the UI could get out of synch with
                // the pages requested from this dispatch, no worries. It will work anyway.
                return;
        }
        // Keep track of the last outstanding request
        mAPIRequestInProcess = true;

        // The UI might ask us to start over from the beginning.
        // If it doesn't, then page=2+ and we keep on with the next page requested, which this class tracks.
        if (event.page == 1) {
            // If we've just requested page 1, then our pageRequested counter will show 2.
            // Only request page 1 if we haven't just requested it.
            if (mPageRequested != 2)
                mPageRequested = 1;
        }

        // We could check here to make sure we don't request more than the maximum number of pages from TMDB,
        // but we'll let TMDB tell us that in the failure, in case the limit changes later.
        /*if (mPageRequested > 1000)
            return;
        */

        // Get the next page worth of data, and prep to advance to the next page
        movieDBService.getMoviesList(event.sortType, mPageRequested++, event.api_key, new retrofit.Callback<MovieResults>() {
            @Override
            public void success(MovieResults response, Response rawResponse) {
                mLastMovieSet = response;
                mBus.post(new MoviesLoadedEvent(response));
                mAPIRequestInProcess = false;
            }

            @Override
            public void failure(RetrofitError error) {
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

    ////////////////////////////////////////////
    // Start of Movie Detail support
    ///////////////////////////////////////////
    @Subscribe
    public void onLoadMovieDetail(LoadMovieDetailEvent event) {

        if (mAPIDetailRequestMovieId == event.movieId)
            // If we already have an outstanding request for this movie, don't send out a duplicate request
            // There is no sense in having multiple network calls and updating the UI multiple times
            return;
        mAPIDetailRequestMovieId = event.movieId;

        // Get the detailed info for one movie
        movieDBService.getMovieDetails(event.movieId, event.api_key, new retrofit.Callback<MovieDetailModel>() {
            @Override
            public void success(MovieDetailModel response, Response rawResponse) {
                mAPIDetailRequestMovieId = 0;  // request is no longer outstanding
                try {
                    mBus.post(new MovieDetailLoadedEvent(response));
                } catch (Exception e) {
                    Log.e(TAG, "MovieDetails Callback failed to post MovieDetailLoadedEvent: " + e.getLocalizedMessage());
                }
            }

            @Override
            public void failure(RetrofitError error) {
                mBus.post(new MovieApiErrorEvent(error));
                mAPIDetailRequestMovieId = 0;  // request is no longer outstanding
            }
        });

    }

    /* When subscribing to events it is often desired to also fetch the current known value for
     * specific events (e.g., current list of movies). To address this common paradigm,
     * Otto adds the concept of 'Producers' which provide an immediate callback to any subscribers upon their registration.

    @Produce
    public MovieDetailLoadedEvent getMoviesNow() {
        // Assuming 'lastMovieSet' exists.
        return new MoviesAvailableEvent(this.mLastMovieDetail);
    } */
    ////////////////////////////////////////////
    // Start of Movie Reviews support
    ///////////////////////////////////////////
    @Subscribe
    public void onLoadReviewsEvent(LoadReviewsEvent event) {
        if (mAPIReviewRequestMovieId == event.movieId) {
            // If we already have an outstanding request for this movie, don't send out a duplicate request
            // There is no sense in having multiple network calls and updating the UI multiple times
            Log.d(TAG, "Ignore duplicate review request for movie id: " + mAPIReviewRequestMovieId);
            return;
        }
        mAPIReviewRequestMovieId = event.movieId;

        // The UI might ask us to start over from the beginning.
        // If it doesn't, then page=2+ and we keep on with the next page requested, which this class tracks.
        if (event.page == 1) {
            // If we've just requested page 1, then our pageRequested counter will show 2.
            // Only request page 1 if we haven't just requested it.
            if (mReviewPageRequested != 2)
                mReviewPageRequested = 1;
        }

        // Get the detailed info for one movie, if we aren't already past the last page of results
        if (mReviewPageRequested == -1) {
            Log.d(TAG, "Ignoring onLoadReviews request because we are past end of input.");
            return;
        }
        movieDBService.getMovieReviews(event.movieId, mReviewPageRequested, event.api_key, new retrofit.Callback<MovieReviewListModel>() {
            @Override
            public void success(MovieReviewListModel response, Response rawResponse) {
                Log.d(TAG, "Reviews received!");
                mAPIReviewRequestMovieId = 0;  // request is no longer outstanding
                if (mReviewPageRequested > response.getTotalPages()) {
                    Log.d(TAG, "Reviews starting over at page 1 after page requested was " + mReviewPageRequested);
                    // mReviewPageRequested = -1;  // end of the line
                    mReviewPageRequested = 1;  // start over

                } else
                    mReviewPageRequested++;
                try {
                    //mLastMovieDetail = response;
                    mBus.post(new ReviewsLoadedEvent(response));
                } catch (Exception e) {
                    Log.e(TAG, "Reviews Callback failed to post ReviewsLoadedEvent: " + e.getLocalizedMessage());
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Reviews failed");
                mBus.post(new MovieApiErrorEvent(error));
                mAPIReviewRequestMovieId = 0;  // request is no longer outstanding
            }
        });
    }

    ////////////////////////////////////////////
    // Start of Movie Videos support
    ///////////////////////////////////////////
    @Subscribe
    public void onLoadVideosEvent(LoadVideosEvent event) {
        if (mAPITrailerRequestMovieId == event.movieId) {
            // If we already have an outstanding request for this movie, don't send out a duplicate request
            // There is no sense in having multiple network calls and updating the UI multiple times
            Log.d(TAG, "Ignore duplicate video request for movie id: " + mAPITrailerRequestMovieId);
            return;
        }
        mAPITrailerRequestMovieId = event.movieId;

        // Get the video urls for one movie. They are all returned at once, not paged
        movieDBService.getMovieVideos(event.movieId, event.api_key, new retrofit.Callback<MovieVideoListModel>() {
            @Override
            public void success(MovieVideoListModel response, Response rawResponse) {
                Log.d(TAG, "Videos received!");
                mAPITrailerRequestMovieId = 0;  // request is no longer outstanding
                try {
                    //mLastMovieDetail = response;
                    mBus.post(new VideosLoadedEvent(response));
                } catch (Exception e) {
                    Log.e(TAG, "Videos Callback failed to post VideosLoadedEvent: " + e.getLocalizedMessage());
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Videos failed");
                mBus.post(new MovieApiErrorEvent(error));
                mAPITrailerRequestMovieId = 0;  // request is no longer outstanding
            }
        });
    }
}
