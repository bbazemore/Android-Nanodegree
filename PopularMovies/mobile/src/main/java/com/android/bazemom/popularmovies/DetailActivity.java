package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


/**
 * Display Movie Details.  Send intent with extra integer containing the Movie id
 */
public class DetailActivity extends AppCompatActivity {
    private final static String TAG = DetailActivity.class.getSimpleName();

    // UI items
    private View mRootView;
    private TabContainerFragment mTabContainerFragment;
    private Toolbar mToolbar;
    private MovieDataService mMovieService;
    private Movie mMovie;
    private boolean mCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        mRootView = findViewById(R.id.detail_container);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (null != mToolbar)
            setSupportActionBar(mToolbar);

        if (savedInstanceState == null) {
            // get Movie detail from argument
            if (setMovieFromIntent()) {
                // pass Movie detail through to the tab container fragment
                FragmentManager fragMan = getSupportFragmentManager();
                mTabContainerFragment = new TabContainerFragment();
                mTabContainerFragment.setArguments(getIntent().getExtras());

                fragMan.beginTransaction()
                        .replace(R.id.detail_container, mTabContainerFragment, TabContainerFragment.TAG)
                        .commit();

                // Activity is set up. Now let's get the data service started getting data about the movie.
                mCreated = true;
                mMovieService = MovieDataService.getInstance(this, mMovie);
            }
        } else {
            // restores state from saved instance
            setMovie((Movie) savedInstanceState.getParcelable(MovieData.MOVIE));
        }
    }

    // Returns true if the movie changed.
    private boolean setMovieFromIntent() {
        Intent intent = getIntent();
       boolean movieChanged = false;
        if (intent != null) {
            movieChanged = setMovie((Movie) intent.getParcelableExtra(MovieData.EXTRA_MOVIE));
        }
        return movieChanged;
    }

    // Return true if the movie changed
    protected boolean setMovie(Movie movie) {
        if (movie == mMovie) return false;
        if (null == movie) {
            Log.d(TAG, "Movie changing from '" + mMovie.title + "' to null. Do we want to notify the tabs?");
        } else {
            if (mMovie == null) {
                Log.d(TAG, "Movie changing from Null to '" + movie.title + "'.");
            } else {
                Log.d(TAG, "Movie changing from '" + mMovie.title + "' to '" + movie.title + "'. Movie service will notify the tabs.");
                mMovieService = MovieDataService.getInstance(this, movie);
            }
        }
        mMovie = movie;
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_share) {
            // Launch share trailer
            if (null != mTabContainerFragment) {
                return mTabContainerFragment.onShareTrailer(mRootView);
            }
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "on resume");
        super.onResume();

        // The movie may have changed while we were out
        setMovieFromIntent();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on pause");
        super.onPause();
    }
} // end class DetailActivity
