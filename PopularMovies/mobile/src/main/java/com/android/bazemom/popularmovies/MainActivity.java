package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.debug.hv.ViewServer;

// Welcome to the Main Activity for Popular Movies
// Not much happens here except:
// 1. Setting up the application-wide connection to the TMDB database
// and the event bus that is used to request Movies and send back the results
// between the background worker threads and the main UI
//
// 2. Setting up the Settings menu item
//
// 3. Launching the MainFragment that contains the Movie Poster gridview
//

public class MainActivity extends AppCompatActivity implements MainFragment.Callback {
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final String MASTERFRAGMENT_TAG = "MSTRTAG";
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    public final static int GUARDIANS_OF_GALAXY_ID = 118340; // movie id from TMDB we use by default

    private View mRootView;
    private TabContainerFragment mTabContainerFragment;
    private Toolbar mToolbar;
    private Movie mMovie;
    public boolean mTwoPane = false;
    private MainFragment mMainFragment;
    private DetailFragment mDetailFragment;


    protected void initMasterPane() {
        // Fill in the first pane
        if (mMainFragment == null) {
            Log.d(TAG, "Initialize master pane.");
            FragmentManager fragMan = getSupportFragmentManager();
            if (null == fragMan) return;
            mMainFragment = new MainFragment();
            fragMan.beginTransaction()
                    .add(R.id.master_container, mMainFragment, MASTERFRAGMENT_TAG)
                    .commit();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        mTwoPane = getResources().getBoolean(R.bool.has_two_panes);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (savedInstanceState == null)
            initMasterPane();

        // For debugging - View Hierarchy
        ViewServer.get(this).addWindow(this);

        if (mTwoPane) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-w600dp-land). If this view is present, then the activity should be
            // in two-pane mode.
            Log.d(TAG, "TwoPane mode");
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null && mDetailFragment == null) {
                mDetailFragment = new DetailFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.detail_container, mDetailFragment, DETAILFRAGMENT_TAG)
                        .commit();
            }
        } /*else {
            getSupportActionBar().setElevation(0f);
        } */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Launch settings activity
            Intent settingsIntent = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(settingsIntent);

            return true;
        }
        if (id == R.id.action_share) {
            // Launch share trailer
            if (null != mTabContainerFragment) {
                return mTabContainerFragment.onShareTrailer(mRootView);
            }
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onPause() {
        super.onPause();
        ViewServer.get(this).removeWindow(this);
    }

    public void onResume() {
        super.onResume();
        ViewServer.get(this).setFocusedWindow(this);
    }

    @Override
    public void onItemSelected(Movie movie) {
        // In this app the only thing selected in the main view is a movie
        onMovieSelected(movie);
    }

    protected void onMovieSelected(Movie movie) {
        // Start the process of getting the details for the selected movie
        MovieDataService.getInstance(this, movie);

        if (mTwoPane) {
            // Add or replace the detail view for the selected movie
            FragmentManager fragMan = getSupportFragmentManager();
            if (null == fragMan) return;

            // pass Movie detail through to the tab container fragment
            Fragment oldDetailFragment = fragMan.findFragmentByTag(DETAILFRAGMENT_TAG);
            Fragment detailFragment = new TabContainerFragment();
            Bundle detailArgs = new Bundle();
            detailArgs.putParcelable(MovieData.MOVIE, movie);
            detailFragment.setArguments(detailArgs);

            if (null == oldDetailFragment) {
                // first time through, create the detail fragment
                fragMan.beginTransaction()
                        .add(R.id.detail_container, detailFragment, DETAILFRAGMENT_TAG)
                        .commit();
            } else {
                fragMan.beginTransaction()
                        .replace(R.id.detail_container, detailFragment, DETAILFRAGMENT_TAG)
                        .commit();

            }
        } else { // one pane view
            // Pass the Movie to the Detail Activity that holds the tab container
            Intent detailIntent = new Intent(this, DetailActivity.class);
            detailIntent.putExtra(MovieData.EXTRA_MOVIE, movie);
            startActivity(detailIntent);
        }
    }

}

