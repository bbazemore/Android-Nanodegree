package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.bazemom.popularmovies.adapters.ViewPagerAdapter;
import com.android.bazemom.popularmovies.moviebusevents.LoadMovieDetailEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadReviewsEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadVideosEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieDetailLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.ReviewsLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.VideosLoadedEvent;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBHelper;
import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;
import com.android.bazemom.popularmovies.moviemodel.ReviewModel;
import com.android.bazemom.popularmovies.moviemodel.VideoModel;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

interface MovieData {
    int getMovieId();

    MovieDetail getMovieDetail();
    List<ReviewModel> getReviewList();
    List<VideoModel> getVideoList();

    String getYouTubeKey(int videoPosition);
    String getYouTubeURL(int videoPosition);
    int getFavorite();
    void setFavorite(int value);
}

/**
 * Display Movie Details.  Send intent with extra integer containing the Movie id
 */
public class DetailActivity extends AppCompatActivity implements MovieData {
    private final static String TAG = DetailActivity.class.getSimpleName();
    static final String MOVIE_ID = "MOVIE_ID";
    private final static int GUARDIANS_OF_GALAXY_ID = 118340; // movie id from TMDB
    private final int TAB_DETAIL = 0;
    private final int TAB_REVIEW = 1;
    private final int TAB_VIDEO = 2;

    private final ReviewModel EMPTY_REVIEW = new ReviewModel();
    private final VideoModel EMPTY_VIDEO = new VideoModel();

    private Bus mBus; // the bus that is used to deliver messages to the TMDB dispatcher
    private DispatchTMDB mDispatchTMDB;

    // Otto gets upset if the Fragment disappears while still subscribed to outstanding events
    // Turn event notification off when we are shutting down, register for events once when
    // starting back up.
    private boolean mReceivingEvents;
    private boolean mDataReceivedDetail = false;
    private boolean mDataReceivedReviewList = false;
    private boolean mDataReceivedVideoList = false;

    private int mMovieId;
    private View mRootView;
    private DetailTabViewHolder mViewHolder;

    protected MovieDetail mMovieDetail;
    protected List<ReviewModel> mReviewList;
    protected List<VideoModel> mVideoList;

    // Movie API management, keep track of how many pages of data we've received
    // so we know if we are asking for more for the current movie, or starting over.
    protected int mReviewPageRequest = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        mRootView = findViewById(R.id.detail_container);

        //setHasOptionsMenu(true);

        // Get the ids of the View elements so we don't have to fetch them over and over
        mViewHolder = new DetailTabViewHolder();
        // slide nerd was doing mRootView.setTag(mViewHolder) - I'm just keeping it in a member variable

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MainActivityFragment.EXTRA_MOVIE_ID)) {
            mMovieId = intent.getIntExtra(MainActivityFragment.EXTRA_MOVIE_ID, GUARDIANS_OF_GALAXY_ID);
        } else mMovieId = GUARDIANS_OF_GALAXY_ID;

        // Initialize the review and trailer lists so we don't have to keep checking for null
        mReviewList = new ArrayList<ReviewModel>();
        mVideoList = new ArrayList<VideoModel>();

        // Create a fragment to handle each tab.  Cache them away so we can poke them
        // when someone selects a tab, and when the data arrives back from the cloud.
        mViewHolder.detailFragment = new DetailFragment();
        mViewHolder.reviewFragment = new ReviewFragment();
        mViewHolder.videoFragment = new VideoFragment();

        // Set up in case we have movies with no reviews or trailers
        EMPTY_REVIEW.setAuthor("");
        EMPTY_REVIEW.setContent(getString(R.string.review_no_reviews));
        EMPTY_VIDEO.setSite(getString(R.string.tmdb_site_value_YouTube));
        EMPTY_VIDEO.setName(getString(R.string.video_no_videos));

        // Start the data cooking
        getDetails(1);
        getReviews(1);
        getVideos(1);

        // Tab layout set up
        setSupportActionBar(mViewHolder.toolbar);
        setupViewPager(mViewHolder.viewPager);
        mViewHolder.tabLayout.setupWithViewPager(mViewHolder.viewPager);
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
            if (null != mViewHolder.videoFragment) {
                return mViewHolder.videoFragment.onShareTrailer(mRootView);
            };
            return false;
        }
        return super.onOptionsItemSelected(item);
    }
    // MovieData interface
    @Override
    public int getMovieId() {
        return mMovieId;
    }

    @Override
    public MovieDetail getMovieDetail() {
        return mMovieDetail;
    }

    @Override
    public List<ReviewModel> getReviewList() {
        return mReviewList;
    }

    @Override
    public List<VideoModel> getVideoList() {
        return mVideoList;
    }

    @Override
    public String getYouTubeKey(int videoPosition) {
        String urlKey = "YouTube key not available for this movie";
        try {
            urlKey = mVideoList.get(videoPosition).getKey();
        } catch (Exception e) {
            Log.d(TAG, "getYouTubeKey failed for video at index: " + videoPosition);
        }
        return urlKey;
    }

    public String getYouTubeURL(int videoPosition) {
        String youtubeTrailerId = getYouTubeKey(videoPosition);
        return Uri.parse("http://www.youtube.com/watch?v=" + youtubeTrailerId).toString();
    }
    @Override
    public int getFavorite() {
        return mMovieDetail.getFavorite();
    }

    @Override
    public void setFavorite(int value) {
        mMovieDetail.setFavorite(value);

        // Persist the favorite setting in the local database
        LocalDBHelper dbHelper = new LocalDBHelper(mRootView.getContext());
        dbHelper.updateMovieInLocalDB(mMovieDetail);
        if (value == 1)
            Toast.makeText(this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
    }

    // Handy dandy little class to cache the View ids so we don't keep looking for them every
    // time we refresh the UI.  We only need to fetch them after the inflate in onCreateView
    class DetailTabViewHolder {
        Toolbar toolbar;
        ViewPager viewPager;
        TabLayout tabLayout;
        DetailFragment detailFragment;
        ReviewFragment reviewFragment;
        VideoFragment videoFragment;

        DetailTabViewHolder() {
            toolbar = (Toolbar) mRootView.findViewById(R.id.tabanim_toolbar);
            viewPager = (ViewPager) mRootView.findViewById(R.id.tabanim_viewpager);
            tabLayout = (TabLayout) mRootView.findViewById(R.id.tabanim_tabs);

            // Set up tab listeners
            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    updateTab(tab);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    updateTab(tab);
                }
            });  // end tab listener

            tabLayout.post(new Runnable() {
                @Override
                public void run() {
                    // Anything we need to fix up in the display once it is up
                    Log.d("TAG", "DetailTabView post-run");
                }
            });
        }
    }

    private void updateTab(TabLayout.Tab tab) {
        mViewHolder.viewPager.setCurrentItem(tab.getPosition());
        switch (tab.getPosition()) {
            case TAB_DETAIL:
                Log.d(TAG, "Tab select detail");
                mViewHolder.detailFragment.updateUI();
                break;
            case TAB_REVIEW:
                Log.d(TAG, "Tab select review");
                mViewHolder.reviewFragment.updateUI();
                break;
            case TAB_VIDEO:
                Log.d(TAG, "Tab select video");
                mViewHolder.videoFragment.updateUI();
                break;
            default:
                Log.d(TAG, "Tab select something different " + tab.getPosition());
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFrag(mViewHolder.detailFragment, getString(R.string.tab_title_detail));
        adapter.addFrag(mViewHolder.reviewFragment, getString(R.string.tab_title_review));
        adapter.addFrag(mViewHolder.videoFragment, getString(R.string.tab_title_trailer));
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
        Log.d(TAG, "on pause");
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

    // moviesLoaded gets called when we get a list of movies back from TMDB
    @Subscribe
    public void movieDetailLoaded(MovieDetailLoadedEvent event) {
        // load the movie data into our movies list
        mMovieDetail = event.movieResult;
        mDataReceivedDetail = true;
        Log.i(TAG, "movie detail Loaded ");
        updateDetailUI();
    }

    private void updateDetailUI() {
        if (null != mViewHolder
                && null != mViewHolder.detailFragment) {
            try {
                mViewHolder.detailFragment.updateUI();
            } catch (Exception e) {
                // not a big deal
                Log.d(TAG, "DetailFragment not ready for update: " + e.getLocalizedMessage());
                return;
            }
            if (null != mMovieDetail)
                mViewHolder.toolbar.setTitle(mMovieDetail.title);
        }
    }

    private void getDetails(int nextPage) {
        // Is this movie cached in the local DB?
        if (mDataReceivedDetail)
            // We have the detail data, we're done.
            return;

        LocalDBHelper dbHelper = new LocalDBHelper(mRootView.getContext());
        mMovieDetail = dbHelper.getMovieDetailFromDB(mMovieId);
        if (null != mMovieDetail) {
            // We are in luck, we have the movie details handy already.
            mDataReceivedDetail = true;
            // We can update the UI right away
            updateDetailUI();
        } else {
            // We have to get the movie from the cloud
            // Start listening for the Movie Detail loaded event
            receiveEvents();

            //  Now request that the movie details be loaded
            String apiKey = mRootView.getContext().getString(R.string.movie_api_key);
            LoadMovieDetailEvent loadMovieRequest = new LoadMovieDetailEvent(apiKey, mMovieId);

            getBus().post(loadMovieRequest);
        }
    }

    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void reviewsLoaded(ReviewsLoadedEvent event) {
        Log.i(TAG, "reviews Loaded callback! Number of reviews: " + event.reviewResults.size());

        // load the movie data into our movies list
        mReviewList.addAll(event.reviewResults);

        if (event.endOfInput) {
            mDataReceivedReviewList = true;
            if (mReviewList.isEmpty()) {
                mReviewList.add(EMPTY_REVIEW);
            }
        }
        else {
            // Ask for another page of reviews
            getReviews(event.currentPage + 1);
        }

        if (null != mViewHolder
                && null != mViewHolder.reviewFragment) {
            try {
                mViewHolder.reviewFragment.updateUI();
            } catch (Exception e) {
                // not a big deal
                Log.d(TAG, "ReviewFragment not ready for update: " + e.getLocalizedMessage());
            }
        }
    }

    protected void getReviews(int nextPage) {
        if (mDataReceivedReviewList)
            // we have all the reviews for this movie,
            // don't keep asking.
            return;

        //  TODO: Is this movie cached in the local DB?
        /*LocalDBHelper dbHelper = new LocalDBHelper(mRootView.getContext());
        mReviewList = dbHelper.getMovieReviewsFromDB(mMovieId);
        if (null != mReviewList) {
            // We are in luck, we have the movie details handy already.
            // We can update the UI right away
            updateUI();
        } else { */
        // We have to get the movie from the cloud
        // Start listening for the Reviews loaded event
        receiveEvents();

        //  Now request that the reviews be loaded
        String apiKey = mRootView.getContext().getString(R.string.movie_api_key);
        LoadReviewsEvent loadReviewsRequest = new LoadReviewsEvent(apiKey, mMovieId, nextPage);

        Log.i(TAG, "request reviews");
        getBus().post(loadReviewsRequest);
        //}
    }

    protected void getVideos(int nextPage) {
        if (mDataReceivedVideoList)
            // we have all the videos for this movie
            return;

        //  TODO: Is this movie cached in the local DB?
        /*LocalDBHelper dbHelper = new LocalDBHelper(mRootView.getContext());
        mVideoList = dbHelper.getMovieVideosFromDB(mMovieId);
        if (null != mReviewList) {
            // We are in luck, we have the movie details handy already.
            mDataReceivedVideoList = true;
            // We can update the UI right away
            updateUI();
        } else { */
        // We have to get the movie from the cloud
        // Start listening for the Reviews loaded event
        receiveEvents();

        //  Now request that the reviews be loaded
        String apiKey = mRootView.getContext().getString(R.string.movie_api_key);
        LoadVideosEvent loadVideosRequest = new LoadVideosEvent(apiKey, mMovieId);

        Log.i(TAG, "request video trailers");
        getBus().post(loadVideosRequest);
    }
    //}


    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void videosLoaded(VideosLoadedEvent event) {
        Log.i(TAG, "videos Loaded callback! Number of trailers: " + event.trailerResults.size());
        // load the movie data into our movies list
        mVideoList.addAll(event.trailerResults);
        mDataReceivedVideoList = true;

        if (mVideoList.isEmpty()) {
            mVideoList.add(EMPTY_VIDEO);
        }
        if (null != mViewHolder
                && null != mViewHolder.videoFragment) {
            try {
                mViewHolder.videoFragment.updateUI();
            } catch (Exception e) {
                Log.d(TAG, "videosLoaded received, but videoFragment not ready yet");
            }
        }
    }

} // end class DetailActivity
