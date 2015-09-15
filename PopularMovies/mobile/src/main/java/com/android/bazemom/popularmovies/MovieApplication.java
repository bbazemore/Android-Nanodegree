package com.android.bazemom.popularmovies;

import android.app.Application;
import android.util.Log;

import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.GsonConverter;

/**
 * This is where we are keeping global context for Movies Project Part 1,
 * which is mostly the list of movies so we don't have to keep fetching them over and over.
 * In part 2 I will probably move the data into a lightweight database
 *
 * This is using Retrofit to access The Movie Database, TMDB, in a RESTful manner.
 * Then it uses Otto to send messages around via the bus to any activity subscribed.
 * Design based on an article by Matt Swanson: http://www.mdswanson.com/blog/2014/04/07/durable-android-rest-clients.html
 */
public class MovieApplication extends Application {
    private final static String TAG = MovieApplication.class.getSimpleName();

    private DispatchTMDB mMovieService;
    public static Bus bus = new Bus(); // we can get fancy later and allow injection BusProvider.getInstance();

    @Override
    public void onCreate() {
        super.onCreate();

        mMovieService = DispatchTMDB.getInstance(buildApi(), bus);
        bus.register(mMovieService);

        bus.register(this); //listen for "global" events
    }

    private MovieDBService buildApi() {
        String baseUrl = getString(R.string.TMDB_endpoint_url);

        Gson gson = new GsonBuilder().setDateFormat(getString(R.string.date_format)).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(baseUrl)
                .setConverter(new GsonConverter(gson))
                .build();

        return restAdapter.create(MovieDBService.class);
    }

    @Subscribe
    public void onApiError(RetrofitError retrofitError) {
        // Provide as much clue as possible what went wrong in the log to aid in debugging.
        // Ratchet it back to a log warning when things are working reasonably well.
        StringBuilder message = new StringBuilder(retrofitError.getMessage())
                    .append(retrofitError.getKind().toString())
                    .append(retrofitError.getUrl())
                    .append(retrofitError.getStackTrace());
        Log.e(TAG, message.toString() );
    }
}

