package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.bazemom.popularmovies.moviebusevents.MovieDetailLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.ReviewsLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.VideosLoadedEvent;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


/**
 * Display Movie Details.  Send intent with extra integer containing the Movie id
 */
public class DetailActivity extends AppCompatActivity  implements Observer, MovieData {
    private final static String TAG = DetailActivity.class.getSimpleName();

    // UI items
    private View mRootView;
    TabContainerFragment mTabContainerFragment;
    public MovieDataService mMovieService;
    private Movie mMovie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        mRootView = findViewById(R.id.detail_container);

        if (savedInstanceState == null) {
            // get Movie detail from argument
            Intent intent = getIntent();
            Bundle tabArg = null;
            if (intent != null) {
                mMovie = intent.getParcelableExtra(MovieData.EXTRA_MOVIE);
                tabArg = intent.getExtras();
                if (null== mMovie)
                    Log.d(TAG, "Why don't we have a movie in Detail onCreate?");
            }
            mMovieService = MovieDataService.getInstance(getApplicationContext(), mMovie);
            mMovieService.addObserver(this);

            // pass Movie detail through to the tab container fragment
            FragmentManager fragMan = getSupportFragmentManager();
            mTabContainerFragment = new TabContainerFragment();
            mTabContainerFragment.setArguments(tabArg);
            fragMan.beginTransaction()
                    .add(R.id.detail_container, mTabContainerFragment, TabContainerFragment.TAG )
                    .commit();
        }
        else {
            // restores state from saved instance
            mMovie = savedInstanceState.getParcelable(MovieData.MOVIE);
            mMovieService = savedInstanceState.getParcelable(MovieData.MOVIE_SERVICE);
        }
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMovieService.saveInstanceState(outState);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "on resume");
        super.onResume();

        mMovieService = MovieDataService.getInstance(this, mMovie);
        // We are back on display. Pay attention to movie results again.
        if (null != mMovieService)
            mMovieService.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on pause");
        super.onPause();

        // Don't bother processing results when we aren't on display.
        mMovieService.onPause();
    }

    // If we have the basic movie information, use that to start filling in the UI
    private void updateMovieUI() {
        if (null != mTabContainerFragment)
            mTabContainerFragment.updateMovieUI(mMovie);
    }
    public void updateMovieDetail(MovieDetail movieDetail) {
        if (null != mTabContainerFragment)
            mTabContainerFragment.updateDetailUI(movieDetail);
    }

    public MovieDataService getMovieDataService() { return mMovieService; }

    // moviesLoaded gets called when we get a list of movies back from TMDB
    @Subscribe
    public void movieDetailLoaded(MovieDetailLoadedEvent event) {
        Log.i(TAG, "movie detail Loaded ");
        updateMovieDetail(event.movieResult);
    }
    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void reviewsLoaded(ReviewsLoadedEvent event) {
        Log.d(TAG, "reviews Loaded callback! Number of reviews: " + event.reviewResults.size());

        if (null != mTabContainerFragment)
            mTabContainerFragment.updateReviewList();
    }
    public void updateReviewList(List<Review> reviewList) {
        mTabContainerFragment.updateReviewList();
    }


    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void videosLoaded(VideosLoadedEvent event) {
        Log.i(TAG, "videos Loaded callback! Number of trailers: " + event.trailerResults.size());
        if (event.trailerResults.size() == 0)
            return;

        if (null != mTabContainerFragment) {
            mTabContainerFragment.updateVideoList();
        }
    }
    public void updateVideoList(List<Video> videoList) {
        mTabContainerFragment.updateVideoList();
    }

    @Override
    public void update(Observable publisher, Object data) {
        Log.d(TAG, "object updated" + data.toString());
        if (data instanceof MovieDetail) {
            mTabContainerFragment.updateDetailUI((MovieDetail) data);
        } else if (data instanceof ArrayList) {
            List list = (ArrayList) data;
            if (!list.isEmpty()) {
                if (list.get(0) instanceof Review) {
                    mTabContainerFragment.updateReviewList();
                } else if (list.get(0) instanceof Video) {
                    mTabContainerFragment.updateVideoList();
                }
            } else {
                Log.d(TAG, "Unexpected data type in Observer update: " + data.getClass().toString());
            }
        }
    }

    @Override
    public MovieDetail getMovieDetail() {
        return mMovieService.getMovieDetail();
    }

    @Override
    public List<Review> getReviewList() {
        return mMovieService.getReviewList();
    }

    @Override
    public List<Video> getVideoList() {
        return mMovieService.getVideoList();
    }

    @Override
    public String getYouTubeURL(int videoPosition) {
        return mMovieService.getYouTubeURL(videoPosition);
    }

    @Override
    public int getFavorite() {
        return mMovieService.getFavorite();
    }

    @Override
    public void setFavorite(int value) {
        mMovieService.setFavorite(value);
    }
} // end class DetailActivity
