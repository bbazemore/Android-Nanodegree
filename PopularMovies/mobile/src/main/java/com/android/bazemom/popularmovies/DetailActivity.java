package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.bazemom.popularmovies.moviebusevents.LoadMovieDetailEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieDetailLoadedEvent;
import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

/**
 * Display Movie Details.  Send intent with extra text containing the Movie id
 */
public class DetailActivity extends AppCompatActivity {
    private final static String TAG = DetailActivity.class.getSimpleName();
    private final static int GUARDIANS_OF_GALAXY_ID = 118340; // movie id from TMDB


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_container, new DetailFragment())
                    .commit();
        }
    }

    public static class DetailFragment extends Fragment {
        private static final String TAG = DetailFragment.class.getSimpleName();

        private Bus mBus; // the bus that is used to deliver messages to the TMDB dispatcher
        private DispatchTMDB mDispatchTMDB;
        private boolean mReceivingEvents;

        private int mMovieId;
        private View mRootView;
        private MovieDetail mMovieDetail;

        // private ShareActionProvider mShareActionProvider;

        public DetailFragment() {
            //setHasOptionsMenu(true);
           // receiveEvents();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.fragment_detail, container, false);
            int movieId;

            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra(MainActivityFragment.EXTRA_MOVIE_ID)) {
                mMovieId = intent.getIntExtra(MainActivityFragment.EXTRA_MOVIE_ID, GUARDIANS_OF_GALAXY_ID);

                // Start listening for the Movie Detail loaded event
                receiveEvents();

                //  Now request that the movie details be loaded
                String apiKey = mRootView.getContext().getString(R.string.movie_api_key);
                LoadMovieDetailEvent loadMovieRequest = new LoadMovieDetailEvent(apiKey, mMovieId);
                getBus().post(loadMovieRequest);
            }
            return mRootView;
        }

        private void UpdateUI() {
            if (mRootView == null)
                return;

            if (mMovieDetail == null)
                // not ready for any more detail right now
                // If I decided to pass the Movie object as the Extra in the intent,
                // I would be able to fill in most of the info now.  If the detail is too
                // slow to initialize, I will go that route.
                return;

            TextView titleView = (TextView) mRootView.findViewById(R.id.detail_movie_title);
            titleView.setText(mMovieDetail.title);
            TextView overview = (TextView) mRootView.findViewById(R.id.detail_movie_overview);
            overview.setText(mMovieDetail.overview);

            // A little convoluted here to support internationalization later. Stuff the runtime
            // in minutes into a formatted string.  The placement of the number may vary in other languages.
            TextView runtime = (TextView) mRootView.findViewById(R.id.detail_movie_runtime);
            String runtimeText = String.format(mRootView.getContext().getString(R.string.detail_runtime_format), mMovieDetail.runtime);
            runtime.setText(runtimeText);

            // To build an image URL, we need 3 pieces of data. The baseurl, size and filepath.
            String posterURL = "http://image.tmdb.org/t/p/w500" + mMovieDetail.posterPath;
            ImageView posterView = (ImageView) mRootView.findViewById(R.id.detail_movie_poster);

            // Here’s an example URL: http://image.tmdb.org/t/p/w500/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg
            Picasso.with(mRootView.getContext())
                    .load(posterURL)
                            //.placeholder(R.mipmap.ic_launcher) too busy looking
                    .error(R.mipmap.ic_error_fallback)         // optional
                    .into(posterView);
        }

        // Use some kind of injection, so that we can swap in a mock for tests.
        // Here we just use simple getter/setter injection for simplicity.
        private Bus getBus() {
            if (mBus == null) {
                if (mDispatchTMDB == null) {
                    mDispatchTMDB = DispatchTMDB.getInstance();
                }
                setBus(mDispatchTMDB.shareBus()); // can get fancy with an injector later BusProvider.getInstance();
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

        // moviesLoaded gets called when we get a list of movies back from TMDB
        @Subscribe
        public void movieDetailLoaded(MovieDetailLoadedEvent event) {
            // load the movie data into our movies list
            mMovieDetail = event.movieResult;
            Log.i(TAG, "movie detail Loaded ");
            UpdateUI();
        }
    }}
