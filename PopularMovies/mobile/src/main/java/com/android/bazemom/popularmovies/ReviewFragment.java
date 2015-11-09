package com.android.bazemom.popularmovies;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.bazemom.popularmovies.moviebusevents.LoadReviewsEvent;
import com.android.bazemom.popularmovies.moviebusevents.ReviewsLoadedEvent;
import com.android.bazemom.popularmovies.moviemodel.ReviewModel;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Tab that displays the reviews for the selected movie
 */
@SuppressLint("ValidFragment")
public class ReviewFragment extends Fragment {
    private static final String TAG = ReviewFragment.class.getSimpleName();

    final WeakReference< DetailActivity> mDetailActivity;
    private int mMovieId;
    private View mRootView;
    private ReviewViewHolder mViewHolder;
    private int mPageRequest = 0;
    private List<ReviewModel> mReviewList;

    ReviewAdapter adapter;

    @SuppressLint("ValidFragment")
    public ReviewFragment(DetailActivity outer, int movieId) {
        Log.d(TAG, "ReviewFragment constructor called");
        mDetailActivity = new WeakReference<DetailActivity>( outer );
        mMovieId = movieId;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "ReviewFragment.onCreateView called");
        mRootView = inflater.inflate(R.layout.fragment_review, container, false);
        mViewHolder = new ReviewViewHolder();
        //frameLayout.setBackgroundColor(color);

        // Use the default layout manager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());

        mViewHolder.recyclerView.setLayoutManager(linearLayoutManager);
        updateUI();

        // get the reviews, starting at page 1
        getReviews(1);

        return mRootView;
    }

    // Once mReviewList is filled in, get the adapter to fill in the recycler view
    void updateUI() {
        Log.d(TAG, "updateUI");
        adapter = new ReviewAdapter(mReviewList);
        mViewHolder.recyclerView.setAdapter(adapter);
    }
    class ReviewViewHolder {
        final TextView titleView;
        final RecyclerView recyclerView;
        final LinearLayout frameLayout;

        RelativeLayout detailLayout;

        ReviewViewHolder() {
            titleView = (TextView) mRootView.findViewById(R.id.detail_movie_title);
            recyclerView = (RecyclerView) mRootView.findViewById(R.id.review_recycler_view);
            //detailLayout = (RelativeLayout) mRootView.findViewById(R.id.detail_movie_background);
            frameLayout = (LinearLayout) mRootView.findViewById(R.id.review_frame);

            // Get the height and width once, and only once after the fragment is laid out
            frameLayout.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("TAG", String.format("ReviewFragment post-run"));
                    // update the UI now we can put the poster up with the right aspect ratio
                    //updateUI();
                }
            });
        }
    } // end ReviewViewHolder

    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void reviewsLoaded(ReviewsLoadedEvent event) {
        Log.i(TAG, "reviews Loaded callback! ");

        // load the movie data into our movies list
        mReviewList.addAll(event.reviewResults);

        // if we know the number of reviews and they aren't going to change
        if (event.endOfInput)
            mViewHolder.recyclerView.setHasFixedSize(true);
        else {
            // Ask for another page of reviews
            getReviews(event.currentPage+1);
        }
        updateUI();
    }

    private void getReviews(int nextPage) {
        //  TODO: Is this movie cached in the local DB?
        /*LocalDBHelper dbHelper = new LocalDBHelper(mRootView.getContext());
        mReviewList = dbHelper.getMovieReviewsFromDB(mMovieId);
        if (null != mReviewList) {
            // We are in luck, we have the movie details handy already.
            // We can update the UI right away
            updateUI();
        } else { */
        // We have to get the movie reviews from the cloud
        //  Now request that the reviews be loaded
        String apiKey = mRootView.getContext().getString(R.string.movie_api_key);
        LoadReviewsEvent loadReviewsRequest = new LoadReviewsEvent(apiKey, mMovieId, nextPage);

        final DetailActivity da = mDetailActivity.get();
        Log.i(TAG, "request reviews");
        da.getBus().post(loadReviewsRequest);
        //}

    }
}
