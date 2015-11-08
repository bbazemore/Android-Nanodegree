package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.android.bazemom.popularmovies.moviebusevents.LoadMoviesEvent;
import com.android.bazemom.popularmovies.moviebusevents.MoviesLoadedEvent;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBContract;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;


/**
 * This is where we will display a grid view of movies
 * See About.txt for more detail.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final static String TAG = MainActivityFragment.class.getSimpleName();
    public final static String EXTRA_MOVIE_ID = "com.android.bazemom.popularmovies.app.MovieId";

    private static final int FAVORITE_LOADER = 0;

    private ArrayList<Movie> mMovieList;
    private MovieAdapter mAdapter;
    private GridView mGridView;
    private TextView mHintView;

    DispatchTMDB dispatchTMDB = null;
    View mRootView = null;
    Bus mBus = null;
    Boolean mReceivingEvents = false;
    private String mCurrentlyDisplayedSortType = "";
    private String mCurrentlyDisplayedPosterQuality = "";
    private int mPageRequest= 1; // 1 = first page of the movie results, 2 = next page


    public MainActivityFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set up the RESTful connection to the movie database
        // using our buddies Retrofit and Otto.
        receiveEvents();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView =  inflater.inflate(R.layout.fragment_main, container, false);

        mHintView = (TextView) mRootView.findViewById(R.id.favorite_hint);

        if(savedInstanceState == null || !savedInstanceState.containsKey(getString(R.string.key_movielist))) {
            // MoviesAvailableEvent (load whatever we have now)
            mMovieList = new ArrayList<>();

            // Fill the list with movies from TMDB
            updateMovies();
        }
        else {
            // Restore the movie list as we last saw it.
            restoreState(savedInstanceState);
        }

        // Connect the UI with our fine list of movies
        mAdapter = new MovieAdapter(getActivity(), mMovieList);
        mGridView = (GridView) mRootView.findViewById(R.id.movies_grid);
        mGridView.setAdapter(mAdapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // User clicked on a movie at "position".
                // Display the details for that movie
                Movie movie = mAdapter.getItem(position);

                Intent detailIntent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(MainActivityFragment.EXTRA_MOVIE_ID, movie.id);

                // Pass the movieId as an integer to the DetailActivity
                startActivity(detailIntent);
            }
        });
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // When the user gets within 2 screens of the bottom of the list, get some more movies
                if (totalItemCount == 0)
                    // can't scroll past bottom if there is nothing in the list, don't loop through the workflow on this condition
                    return;
                if (totalItemCount - firstVisibleItem < (visibleItemCount * 2 + 1)) {
                    updateMovies();
                }
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // don't care
            }
        });

        return mRootView;
    }
    @Override
    public void onResume(){
        super.onResume();

        // We are back on display. Pay attention to movie results again.
        receiveEvents();
        updateMovies();
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // our custom save to parcelablearraylist here
       outState.putParcelableArrayList(getString(R.string.key_movielist), mMovieList);
        outState.putString(getString(R.string.settings_sort_key), mCurrentlyDisplayedSortType);
        outState.putString(getString(R.string.settings_image_quality_key), mCurrentlyDisplayedPosterQuality);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        restoreState(savedInstanceState);
        receiveEvents();
    }

    public void updateMovies() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String apiKey = mRootView.getContext().getString(R.string.movie_api_key);
        String sortType = prefs.getString(getString(R.string.settings_sort_key), getString(R.string.settings_sort_now_playing) );

        // Clear the list of movies if the sort type is changing.  The sort is a misnomer since
        // the app is fetching a different set of movies depending on the type requested.
        if (!sortType.equals(mCurrentlyDisplayedSortType))
        {
            mMovieList.clear();
            mPageRequest = 1; // start from the beginning of the new movie type
            mCurrentlyDisplayedSortType = sortType;
        }
        if (sortType.equals(getString(R.string.settings_sort_favorite))) {
            // populate the movies from the database
            loadFavoriteMovies();
        }
        else {
            // Create an event requesting that the movie list be updated
            LoadMoviesEvent loadMoviesRequest = new LoadMoviesEvent(apiKey, sortType, mPageRequest++);
            getBus().post(loadMoviesRequest);

            // When the request finishes we'll get called at onMoviesLoaded
        }
    }


    @Subscribe
    public void onMoviesLoaded(MoviesLoadedEvent event) {
        Log.i(TAG, "onMoviesLoaded ");

        // Mash new movie results into the View that is displayed to user
        mAdapter.addAll(event.movieResults);
    }


    @Override
    public void onPause() {
        super.onPause();

        // Don't bother processing results when we aren't on display.
        stopReceivingEvents();
    }


    // Use some kind of injection, so that we can swap in a mock for tests.
    // Here we just use simple getter/setter injection for simplicity.
    private Bus getBus() {
        if (mBus == null) {
            if (dispatchTMDB == null)
            {
                dispatchTMDB = DispatchTMDB.getInstance();
            }
            setBus(dispatchTMDB.shareBus()); // can get fancy with an injector later BusProvider.getInstance();
        }
        return mBus;
    }

    private void setBus(Bus bus) {
        mBus = bus;
    }

    private void receiveEvents() {
        if (!mReceivingEvents) {
            try {
                getBus().register(this);
                mReceivingEvents = true;
            } catch (Exception e) {
                Log.i(TAG, "receiveEvents could not register with Otto bus");
            }
        }
    }

    private void stopReceivingEvents(){

        if (mReceivingEvents) {
            try {
                getBus().unregister(this);
                mReceivingEvents = false;
            } catch (Exception e) {
                Log.i(TAG, "stopReceivingEvents could not unregister with Otto bus");
            }
        }
    }
    private void restoreState(Bundle savedInstanceState){
        if (savedInstanceState != null) {
            mMovieList = savedInstanceState.getParcelableArrayList(getString(R.string.key_movielist));
            mCurrentlyDisplayedSortType = savedInstanceState.getString(getString(R.string.settings_sort_key));
            mCurrentlyDisplayedPosterQuality = savedInstanceState.getString(getString(R.string.settings_image_quality_key));
        }
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        getLoaderManager().initLoader(FAVORITE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader for favorites, so we don't care about checking the id.
        Log.d(TAG, "onCreateLoader ");

        // Sort order:  Most recent first
        String sortOrder = LocalDBContract.MovieEntry.COLUMN_RELEASE_DATE + " DSC";
        Uri favoriteUri = LocalDBContract.getFavoriteUri();
        return new CursorLoader(getActivity(), favoriteUri,null,null,null, sortOrder );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished ");

        // add each movie in favorites to the adapter
        displayFavoriteMovies(data);
    }
    private void loadFavoriteMovies() {

        LocalDBHelper dbHelper = new LocalDBHelper(getContext());
        mMovieList = dbHelper.getMovieFavoritesFromDB();
        if (null != mAdapter)
            mAdapter.addAll(mMovieList);
        // Give the user a hint if the list is empty
        mHintView.setVisibility(mMovieList.isEmpty() ? View.VISIBLE : View.GONE);
        /*
        String sortOrder = LocalDBContract.MovieEntry.COLUMN_RELEASE_DATE + " DSC";
        Uri favoriteUri = LocalDBContract.getFavoriteUri();
        Cursor cursor = getActivity().getContentResolver().query(favoriteUri,
                null, null, null, sortOrder);

        Log.d(TAG, "loadFavoriteMovies cursor  ");
        displayFavoriteMovies(cursor);
        */

    }

    private void displayFavoriteMovies(Cursor movieCursor) {
        mMovieList.clear();
        // Convert from rows of data to movie object
        if (null!= movieCursor && movieCursor.moveToFirst() ) {
            do {
                mMovieList.add(new Movie(movieCursor));
            } while (movieCursor.moveToNext());
        }

        if (null != mAdapter)
            mAdapter.addAll(mMovieList);
          /*  Gridview automatically takes care of position while switching
            if (mPosition != ListView.INVALID_POSITION) {
                // If we don't need to restart the loader, and there's a desired position to restore
                // to, do so now.
                mGridView.smoothScrollToPosition(mPosition);
            } */
        // Give the user a hint if the list is empty
        mHintView.setVisibility( mMovieList.isEmpty() ? View.VISIBLE : View.GONE);
    }
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset, clearing display and movie list");
        // ??
        mAdapter.clear();
        mMovieList.clear();
        //mAdapter.swapCursor(null);
    }
}

