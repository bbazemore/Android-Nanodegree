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
    private MovieDataService mMovieService;
    private Movie mMovie;
    private boolean mCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        mRootView = findViewById(R.id.detail_container);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (null != toolbar)
            setSupportActionBar(toolbar);

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
            mMovie = savedInstanceState.getParcelable(MovieData.MOVIE);
        }
    }

    // Returns true if the movie changed.
    private boolean setMovieFromIntent() {
        Intent intent = getIntent();
        Movie intentMovie;
        boolean movieChanged = false;

        if (intent != null) {
            intentMovie = intent.getParcelableExtra(MovieData.EXTRA_MOVIE);
            if (null == intentMovie) {
                if (mMovie != null) {
                    //movieChanged = true;
                    Log.d(TAG, "Movie changing from '" + mMovie.title + "' to null. Do we want to notify the tabs?");
                } else {
                    Log.d(TAG, "Why are we getting a null Movie in the Detail intent?");
                }
            } else {
                if (mMovie == null) {
                    // Normal first time through
                    Log.d(TAG, "Movie changing from Null to '" + intentMovie.title + "'.");
                    movieChanged = true;
                    mMovie = intentMovie;
                } else if (mMovie.id != intentMovie.id) {
                    // This is a genuine change in movie, pass it on
                    Log.d(TAG, "Movie changing from '" + mMovie.title + "' to '" + intentMovie.title + "'. Movie service should notify the tabs.");
                    movieChanged = true;
                    mMovie = intentMovie;
                }
            }
        }
        // Tell data service we are focusing on a different movie
        if (mCreated & movieChanged) {
            mMovieService = MovieDataService.getInstance(this, mMovie);
        }
        return movieChanged;
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
