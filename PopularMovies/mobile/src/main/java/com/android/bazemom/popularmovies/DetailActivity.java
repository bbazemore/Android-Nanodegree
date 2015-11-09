package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.android.bazemom.popularmovies.moviebusevents.ReviewsLoadedEvent;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;


/**
 * Display Movie Details.  Send intent with extra integer containing the Movie id
 */
public class DetailActivity extends AppCompatActivity {
    private final static String TAG = DetailActivity.class.getSimpleName();
    private final static int GUARDIANS_OF_GALAXY_ID = 118340; // movie id from TMDB

    private Bus mBus; // the bus that is used to deliver messages to the TMDB dispatcher
    private DispatchTMDB mDispatchTMDB;

    // Otto gets upset if the Fragment disappears while still subscribed to outstanding events
    // Turn event notification off when we are shutting down, register for events once when
    // starting back up.
    private boolean mReceivingEvents;

    private int mMovieId;
    private View mRootView;
    private DetailTabViewHolder mViewHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        mRootView = findViewById(R.id.detail_container);
        //setHasOptionsMenu(true);

        // First time through, fill in a fragment for each of the three detail tabs:
        // Detail, Review, Trailers
        /*if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_container, new DetailFragment())
                    .commit();
        } */

        // Get the ids of the View elements so we don't have to fetch them over and over
        mViewHolder = new DetailTabViewHolder();
        // slide nerd was doing mRootView.setTag(mViewHolder) - I'm just keeping it in a member variable

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MainActivityFragment.EXTRA_MOVIE_ID)) {
            mMovieId = intent.getIntExtra(MainActivityFragment.EXTRA_MOVIE_ID, GUARDIANS_OF_GALAXY_ID);
        }
        else mMovieId = GUARDIANS_OF_GALAXY_ID;

        // Tab layout set up
        setSupportActionBar(mViewHolder.toolbar);
        setupViewPager(mViewHolder.viewPager);
        mViewHolder.tabLayout.setupWithViewPager(mViewHolder.viewPager);

        // Allow the tabs to get movie details, reviews and trailers through our shared bus
        receiveEvents();
    }

    // Handy dandy little class to cache the View ids so we don't keep looking for them every
    // time we refresh the UI.  We only need to fetch them after the inflate in onCreateView
    class DetailTabViewHolder {
        Toolbar toolbar;
        ViewPager viewPager;
        TabLayout tabLayout;

        DetailTabViewHolder() {
            toolbar = (Toolbar) mRootView.findViewById(R.id.tabanim_toolbar);
            viewPager = (ViewPager) mRootView.findViewById(R.id.tabanim_viewpager);
            tabLayout = (TabLayout) mRootView.findViewById(R.id.tabanim_tabs);

            // Set up tab listeners
            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition());
                   /* switch (tab.getPosition()) {
                        case 0:
                            showToast("One");
                            break;
                        case 1:
                            showToast("Two");
                            break;
                        case 2:
                            showToast("Three");
                            break;
                    } */

                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }
                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });  // end tab listener

            // Get the height and width once, and only once after the fragment is laid out
            tabLayout.post(new Runnable() {
                @Override
                public void run() {
                    // Anything we need to fix up in the display once it is up
                    Log.d("TAG", String.format("DetailTabView post-run"));
                }
            });
        }
    }

   /* @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get the layout adjusted to the new orientation / device
        mRootView = inflater.inflate(R.layout.activity_detail, container, false);

        // Get the ids of the View elements so we don't have to fetch them over and over
        mViewHolder = new DetailTabViewHolder();
        // slide nerd was doing mRootView.setTag(mViewHolder) - I'm just keeping it in a member variable

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MainActivityFragment.EXTRA_MOVIE_ID)) {
            mMovieId = intent.getIntExtra(MainActivityFragment.EXTRA_MOVIE_ID, GUARDIANS_OF_GALAXY_ID);
        }
        else mMovieId = GUARDIANS_OF_GALAXY_ID;

        // Tab layout set up
        setSupportActionBar(mViewHolder.toolbar);
        setupViewPager(mViewHolder.viewPager);
        mViewHolder.tabLayout.setupWithViewPager(mViewHolder.viewPager);

        // Allow the tabs to get movie details, reviews and trailers through our shared bus
        receiveEvents();
        return mRootView;
    } */

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new DetailFragment(this, mMovieId), getString(R.string.tab_title_detail));
        adapter.addFrag(new ReviewFragment(this, mMovieId), getString(R.string.tab_title_review));
        //adapter.addFrag(new TrailerFragment(), getString(R.string.tab_title_trailer));
        viewPager.setAdapter(adapter);
    }
    @Override
    public void onResume() {
        Log.d(TAG, "on resume");
        super.onResume();

        // We are back on display. Pay attention to movie results again.
        receiveEvents();
        //updateUI();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on resume");
        super.onPause();

        // Don't bother processing results when we aren't on display.
        stopReceivingEvents();
    }

    // Use some kind of injection, so that we can swap in a mock for tests.
    // Here we just use simple getter/setter injection for simplicity.
    protected Bus getBus() {
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
                Log.d(TAG, "DetailActivity Events on");
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
                Log.d(TAG, "DetailActivity Events off");
                getBus().unregister(this);
                mReceivingEvents = false;
            } catch (Exception e) {
                Log.i(TAG, "stopReceivingEvents could not unregister with Otto bus");
            }
        }
    }
    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void reviewsLoaded(ReviewsLoadedEvent event) {
        Log.i(TAG, "reviews Loaded callback! ");
    }
} // end class DetailActivity
