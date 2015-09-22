package com.android.bazemom.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.bazemom.popularmovies.moviebusevents.LoadMovieDetailEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieDetailLoadedEvent;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Display Movie Details.  Send intent with extra integer containing the Movie id
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

        // Otto gets upset if the Fragment disappears while still subscribed to outstanding events
        // Turn event notification off when we are shutting down, register for events once when
        // starting back up.
        private boolean mReceivingEvents;

        private int mMovieId;
        private View mRootView;
        private MovieDetail mMovieDetail;

        // private ShareActionProvider mShareActionProvider;  // V2?

        public DetailFragment() {
            //setHasOptionsMenu(true);
            receiveEvents();
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

        @Override
        public void onResume(){
            super.onResume();

            // We are back on display. Pay attention to movie results again.
            receiveEvents();
            updateUI();
        }

        @Override
        public void onPause() {
            super.onPause();

            // Don't bother processing results when we aren't on display.
            stopReceivingEvents();
        }

        private void updateUI() {
            if (mRootView == null)
                return;

            if (mMovieDetail == null)
                // not ready for any more detail right now
                // If I decided to pass the Movie object as the Extra in the intent,
                // I would be able to fill in most of the info now.  If the detail is too
                // slow to initialize, I will go that route.
                return;

            final Context context = mRootView.getContext();
            TextView titleView = (TextView) mRootView.findViewById(R.id.detail_movie_title);
            titleView.setText(mMovieDetail.title);
            TextView overview = (TextView) mRootView.findViewById(R.id.detail_movie_overview);
            overview.setText(mMovieDetail.overview);

            TextView releaseDate = (TextView) mRootView.findViewById(R.id.detail_movie_release_date);
            releaseDate.setText(context.getString(R.string.detail_release_date) + mMovieDetail.releaseDate);
            // A little convoluted here to support internationalization later. Stuff the runtime
            // in minutes into a formatted string.  The placement of the number may vary in other languages.
            TextView runtime = (TextView) mRootView.findViewById(R.id.detail_movie_runtime);
            String runtimeText = String.format(context.getString(R.string.detail_runtime_format), mMovieDetail.runtime);
            runtime.setText(runtimeText);

            TextView rating = (TextView) mRootView.findViewById(R.id.detail_movie_user_rating);
            rating.setText(context.getString(R.string.detail_movie_user_rating) + Double.toString(mMovieDetail.voteAverage));


            // To build an image URL, we need 3 pieces of data. The baseurl, size and filepath.
            // First get the size from the preferences, user can select high, medium or low
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String posterSize = prefs.getString(getString(R.string.settings_image_quality_key), getString(R.string.settings_poster_quality_high));

            if (mMovieDetail.posterPath != null && !mMovieDetail.posterPath.isEmpty()) {
                String posterURL = context.getString(R.string.TMDB_image_base_url) + posterSize + mMovieDetail.posterPath;
                ImageView posterView = (ImageView) mRootView.findViewById(R.id.detail_movie_poster);

                // Here’s an example URL: http://image.tmdb.org/t/p/w500/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg
                Picasso.with(context)
                        .load(posterURL)
                                //.placeholder(R.mipmap.ic_launcher) too busy looking
                        .error(R.mipmap.ic_error_fallback)         // optional
                        .into(posterView);
            }

            // Now set the background image for the whole frame in the Detail View
            // Stolen from http://stackoverflow.com/questions/29777354/how-do-i-set-background-image-with-picasso-in-code
            // Note the image quality values are different for posters and backdrops, so fix up equivalent high, medium, and low values here.
            if (mMovieDetail.backdropPath != null && !mMovieDetail.backdropPath.isEmpty()) {
                final RelativeLayout detailLayout = (RelativeLayout) mRootView.findViewById(R.id.detail_movie_background);
                int backgroundSizeId = R.string.settings_backddrop_quality_high;
                if (posterSize.equals(getString(R.string.settings_poster_quality_medium))) {
                    backgroundSizeId = R.string.settings_backdrop_quality_medium;
                } else if (posterSize.equals(getString(R.string.settings_poster_quality_low))) {
                    backgroundSizeId = R.string.settings_backdrop_quality_low;
                }

                String backgroundURL = context.getString(R.string.TMDB_image_base_url);
                backgroundURL +=  context.getString(backgroundSizeId);
                backgroundURL +=  mMovieDetail.backdropPath;

                Picasso.with(getActivity()).load(backgroundURL).into(new Target() {

                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        // use deprecated setBackgroundDrawable for API 11 compatibility.
                        // setBackground requires 16 which is a bit too new for now
                        detailLayout.setBackgroundDrawable(new BitmapDrawable(context.getResources(), bitmap));
                    }

                    @Override
                    public void onBitmapFailed(final Drawable errorDrawable) {
                        Log.d("TAG", "Picasso background image load failed");
                    }

                    @Override
                    public void onPrepareLoad(final Drawable placeHolderDrawable) {
                        Log.d("TAG", "Prepare background image Load");
                    }
                });
            }
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
            updateUI();
        }
    }}
