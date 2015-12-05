package com.android.bazemom.popularmovies;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.bazemom.popularmovies.adapters.MovieAdapter;
import com.android.bazemom.popularmovies.moviebusevents.LoadMoviesEvent;
import com.android.bazemom.popularmovies.moviebusevents.MoviesLoadedEvent;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBContract;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBHelper;
import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is where we will display a grid view of movies
 * See About.txt for more detail.
 */
public class MainFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final static String TAG = MainFragment.class.getSimpleName();
    private static final int FAVORITE_LOADER = 0;
    public static final int MAX_VISIBLE_ITEM_COUNT = 30;
    private static final int MOVIE_COLUMN_WIDTH_DP = 300;

    private ArrayList<Movie> mMovieList;
    private MovieAdapter mAdapter;
    private GridView mGridView;
    private TextView mHintView;

    private int mGridviewPosition = GridView.INVALID_POSITION;
    DispatchTMDB dispatchTMDB = null;
    boolean mTwoPane = false;
    View mRootView = null;
    Toolbar mToolbar = null;
    Bus mBus = null;
    Boolean mReceivingEvents = false;
    private String mCurrentlyDisplayedSortType = "";
    private String mCurrentlyDisplayedSortTitle = "";
    private String mCurrentlyDisplayedPosterQuality = "";
    private int mPageRequest = 1; // 1 = first page of the movie results, 2 = next page


    public MainFragment() {
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * Notify the activity when an item has been selected.
         */
        void onItemSelected(Movie movie);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set up the RESTful connection to the movie database
        // using our buddies Retrofit and Otto.
        receiveEvents();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // retain this fragment across configuration changes
        setRetainInstance(true);
        Log.d(TAG, "onCreate with retain instance");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);

        mHintView = (TextView) mRootView.findViewById(R.id.favorite_hint);
        mHintView.setVisibility(View.GONE);

        mTwoPane = getResources().getBoolean(R.bool.has_two_panes);

        if (savedInstanceState == null || !savedInstanceState.containsKey(getString(R.string.key_movielist))) {
            // MoviesAvailableEvent (load whatever we have now)
            mMovieList = new ArrayList<>();

            // Fill the list with movies from TMDB
            //updateMovies();
        } else {
            // Restore the movie list as we last saw it.
            restoreState(savedInstanceState);
        }
        // Things to do once and only once the view is up and running
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "RootView post-run lambda");
              /*  if (mTwoPane) {
                    // compute optimal number of movie poster columns based on available width
                    setOptimalColumnWidth();
                    //RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), optimalColumnCount);
                    //mGridView.setLayoutManager(layoutManager);
                    // Force re-render
                    mGridView.setAdapter(mAdapter);

                } */
                // Make sure the sort title is accurate
                updateSortType(getSortType());
                // update the UI now we can scroll the last selected movie into position
                updatePosition();
            }
        });

        // Connect the UI with our fine list of movies
        mAdapter = new MovieAdapter(getActivity(), mMovieList);
        mGridView = (GridView) mRootView.findViewById(R.id.movies_grid);

        setOptimalColumnWidth();

        mGridView.setAdapter(mAdapter);

        // In Master-Detail two pane mode, keep the movie in the
        // master grid-view selected to indicate it is associated
        // with the detail view
        mGridView.setChoiceMode(
                mTwoPane ? ListView.CHOICE_MODE_SINGLE
                        : ListView.CHOICE_MODE_NONE);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // User clicked on a movie at "position".
                // Display the details for that movie
                Movie movie = mAdapter.getItem(position);

                // remember where we were in the list for when we return
                mGridviewPosition = position;
                ((Callback) getActivity())
                        .onItemSelected(movie);
            }
        });
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // When the user gets within 2 screens of the bottom of the list, get some more movies
                if (totalItemCount == 0)
                    // can't scroll past bottom if there is nothing in the list, don't loop through the workflow on this condition
                    return;
                // cap the count at 50. In the landscape case the count increases seemingly without end.
                visibleItemCount = Math.min(visibleItemCount, MAX_VISIBLE_ITEM_COUNT);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // The toolbar may be in the detail fragment, which may be laid out after this master.
        // Be patient and fill this in when it is likely to be there
        if (null == mToolbar) {
            Log.d(TAG, "OnViewCreated setting toolbar.");
            mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
            updateSortType(getSortType());
        }
        // Fill the list with movies from TMDB
        if (mMovieList.size() == 0) {
            Log.d(TAG, "OnViewCreated updateMovies.");
            updateMovies();
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "on resume");
        super.onResume();

        // We are back on display. Pay attention to movie results again.
        receiveEvents();

        // In this app the settings are full page, so
        // we only see the settings change during our resume
        // See if the sort type changed on us in updateMovies
        updateMovies();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // our custom save to parcelablearraylist here
        outState.putParcelableArrayList(getString(R.string.key_movielist), mMovieList);
        outState.putString(getString(R.string.settings_sort_key), mCurrentlyDisplayedSortType);
        outState.putString(getString(R.string.settings_image_quality_key), mCurrentlyDisplayedPosterQuality);
        outState.putInt(getString(R.string.key_gridview_position), mGridviewPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        Log.d(TAG, "view restored");
        super.onViewStateRestored(savedInstanceState);
        restoreState(savedInstanceState);
        receiveEvents();
    }

    public void updateMovies() {
        String sortType = getSortType();
        // Clear the list of movies if the sort type is changing.  The sort is a misnomer since
        // the app is fetching a different set of movies depending on the type requested.
        if (!sortType.contentEquals(mCurrentlyDisplayedSortType)) {
            Log.d(TAG, "updateMovies, change sort type");
            if (!mCurrentlyDisplayedSortType.isEmpty()) {
                // Really starting a new set of movies, rather than restoring the last one
                Log.d(TAG, "updateMovies, start new sort type");
                mMovieList.clear();
                mPageRequest = 1; // start from the beginning of the new movie type
                mGridviewPosition = ListView.INVALID_POSITION;
            }

            updateSortType(sortType);

            // Special case fetching favorite movies from database.
            // only do the fetch once when the setting changes.
            // Since all the favorites are fetched in one go, we don't
            // have to ask for more pages of movie results.
            if (sortType.equals(getString(R.string.settings_sort_favorite))) {
                // populate the movies from the database
                loadFavoriteMovies();
                return;
            }
        } else {
            // Make sure title in the toolbar is current, even first time around
            updateSortType(sortType);
        }
        // TODO: check for end of data?
        // Create an event requesting that the movie list with the new sort order be updated from the
        // Movie DB web service
        String apiKey = getResources().getString(R.string.movie_api_key);
        LoadMoviesEvent loadMoviesRequest = new LoadMoviesEvent(apiKey, sortType, mPageRequest++);
        getBus().post(loadMoviesRequest);
    }


    @Subscribe
    public void onMoviesLoaded(MoviesLoadedEvent event) {
        Log.i(TAG, "onMoviesLoaded gridposition = " + Integer.toString(mGridviewPosition));
        if (!event.movieResults.getResults().isEmpty()) {
            // Mash new movie results into the View that is displayed to user
            mAdapter.addAll(event.movieResults);
            updatePosition();
        }
    }

    public String getSortType() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String apiKey = mRootView.getContext().getString(R.string.movie_api_key);
        return prefs.getString(getString(R.string.settings_sort_key), getString(R.string.settings_sort_now_playing));
    }

    protected void updateSortType(String sortType) {
        if (mCurrentlyDisplayedSortType.equals(sortType))
            return;
        Log.d(TAG, "updateSortType from " + mCurrentlyDisplayedSortType + " to " + sortType);

        // Display sort type in the title bar
        if (null == mToolbar)
            mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (null != mToolbar) {
            // we have the sort type value - which is what we hand to the UI.
            // Convert it to the user friendly label. They don't make this easy :(
            Log.d(TAG, "Have a toolbar, we can change sort type");
            String[] sortKeyStrings = getResources().getStringArray(R.array.settings_sort_values);
            int index = Arrays.asList(sortKeyStrings).indexOf(sortType);
            if (index >= 0) {
                String[] sortFriendlyStrings = getResources().getStringArray(R.array.settings_sort_labels);
                mCurrentlyDisplayedSortTitle = sortFriendlyStrings[index];
                mToolbar.setTitle(mCurrentlyDisplayedSortTitle);

                // And now this sort type is being displayed
                mCurrentlyDisplayedSortType = sortType;
            } else {
                Log.d(TAG, "Error updateSortType for " + sortType + " index was " + index);
            }
        }
    }

    private void updatePosition() {
        Log.d(TAG, "updatePosition: " + mGridviewPosition);
        if (null == mGridView) {
            Log.d(TAG, "updatePosition: no gridview to process");
            return;
        }

        if (mGridView.getChildCount() == 0) {
            Log.d(TAG, "updatePosition: no children to process");
            return;
        }
        // Initialize the detail view in 2pane mode
        if (mTwoPane
            && mGridviewPosition == GridView.INVALID_POSITION
                && mGridView.getFirstVisiblePosition() != GridView.INVALID_POSITION) {
            Log.d(TAG, "updatePosition 2pane, first visible position is: " + mGridView.getFirstVisiblePosition());
            // When 2 pane view starts up, select the first visible movie in the list
            mGridView.performItemClick(mGridView, mGridView.getFirstVisiblePosition(), 0);
        }
        if (mGridviewPosition != mGridView.getFirstVisiblePosition()
                && mGridviewPosition != GridView.INVALID_POSITION) {
            Log.d(TAG, "updatePosition firstVisiblePosition is: " + mGridView.getFirstVisiblePosition());
            // The position we want is different than the position we have
            // Back to where we were in the list the last time the user clicked
            mGridView.smoothScrollToPosition(mGridviewPosition);
        }
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
            if (dispatchTMDB == null) {
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

    private void stopReceivingEvents() {

        if (mReceivingEvents) {
            try {
                getBus().unregister(this);
                mReceivingEvents = false;
            } catch (Exception e) {
                Log.i(TAG, "stopReceivingEvents could not unregister with Otto bus");
            }
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        mToolbar = null;
        mCurrentlyDisplayedSortType = ""; // Force Sort type title to update
        if (savedInstanceState != null) {
            updateSortType(savedInstanceState.getString(getString(R.string.settings_sort_key)));
            //mCurrentlyDisplayedSortTitle = savedInstanceState.getString(getString(R.string.settings_sort_label));
            mMovieList = savedInstanceState.getParcelableArrayList(getString(R.string.key_movielist));
            mCurrentlyDisplayedPosterQuality = savedInstanceState.getString(getString(R.string.settings_image_quality_key));
            mGridviewPosition = savedInstanceState.getInt(getString(R.string.key_gridview_position));
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
        return new CursorLoader(getActivity(), favoriteUri, null, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished ");

        // add each movie in favorites to the adapter
        if (null != data) {
            displayFavoriteMovies(data);
        }
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
        if (mCurrentlyDisplayedSortType.equals(getString(R.string.settings_sort_favorite))) {
            mMovieList.clear();
            // Convert from rows of data to movie object
            if (null != movieCursor && movieCursor.moveToFirst()) {
                do {
                    mMovieList.add(new Movie(movieCursor));
                } while (movieCursor.moveToNext());
            }

            if (null != mAdapter)
                mAdapter.addAll(mMovieList);

            // Give the user a hint if the list is empty
            mHintView.setVisibility(mMovieList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset");
        // not currently used. May use for favorites
        // mAdapter.clear();
        // mMovieList.clear();
        //mAdapter.swapCursor(null);
    }

    protected void setOptimalColumnWidth() {
        int viewWidth = Utility.getScreenWidth(getContext());
        Log.d(TAG, "setOptimalColumnWidth width is " + viewWidth);
        int optimalColumnCount = Math.round(viewWidth / MOVIE_COLUMN_WIDTH_DP);
        int actualPosterViewWidth = viewWidth / optimalColumnCount;
        //mGridView.setLayoutParams((actualPosterViewWidth * 1.3);
        mGridView.setNumColumns(optimalColumnCount);
        mGridView.setColumnWidth(actualPosterViewWidth);
        mGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        Log.d(TAG, "Grid view columns: " + optimalColumnCount + " width: " + actualPosterViewWidth);

    }
}

