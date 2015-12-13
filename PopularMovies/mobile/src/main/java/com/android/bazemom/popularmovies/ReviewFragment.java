package com.android.bazemom.popularmovies;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.bazemom.popularmovies.adapters.ReviewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Tab that displays the reviews for the selected movie
 * Requires the caller to support the MovieData interface
 */
public class ReviewFragment extends Fragment implements Observer {
    private static final String TAG = ReviewFragment.class.getSimpleName();
    private View mRootView;
    private ReviewViewHolder mViewHolder;
    ReviewAdapter adapter;
    private boolean mLayoutInitialized = false;
    private boolean mUIInitialized = false;

    public ReviewFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "ReviewFragment.onCreateView called");

        mRootView = inflater.inflate(R.layout.fragment_review, container, false);
        mViewHolder = new ReviewViewHolder();
        Utility.initDetailTitle(mRootView, mViewHolder.favoriteButton);

        // Use the default layout manager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        mViewHolder.recyclerView.setLayoutManager(linearLayoutManager);

        MovieDataService data = MovieDataService.getInstance();
        adapter = new ReviewAdapter(data.getReviewList());
        mViewHolder.recyclerView.setAdapter(adapter);

        // If there is anything we need to fix up after the layout is known,
        // do it in the post-layout lambda
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "ReviewFragment post-run");
                // update the UI when we are likely to have the poster and palette color available
                updateUI();
            }
        });
        mLayoutInitialized = true;
        updateUI();
        return mRootView;
    }

    // Once ReviewList is filled in, get the adapter to fill in the recycler view
    void updateUI() {
        if (mUIInitialized) return;

        MovieDataService data = MovieDataService.getInstance();
        if (null != data && null != mViewHolder) {
            {
                Log.d(TAG, "updateReviewUI for " + data.getMovieTitle());
                mViewHolder.titleView.setText(data.getMovieTitle());
                mViewHolder.titleBackground.setBackgroundColor(data.getDarkBackground());
                Utility.updateFavoriteButton(mViewHolder.favoriteButton, data.getFavorite());

               updateReviewList((ArrayList<Review>)data.getReviewList());
            }
        } else {
            Log.d(TAG, "UpdateReviewUI missing data or viewholder");
        }
    }

    protected void updateReviewList(ArrayList<Review> reviewList) {
        // The first round review may say "no reviews found".
        // Allow for the real reviews to be loaded on a subsequent round.
        adapter = new ReviewAdapter(reviewList);
        mViewHolder.recyclerView.setAdapter(adapter);

        MovieDataService data = MovieDataService.getInstance();
        if ( data.reviewListComplete() && data.reviewCount() <= adapter.getItemCount()) {
            data.deleteObserver(this);
            mUIInitialized = true;
            Log.d(TAG, "updateReviewUI mission accomplished for " + data.getMovieTitle() + adapter.getItemCount());
        } else {
            // Still waiting for more reviews to arrive from the internets
            Log.d(TAG, "updateReviewUI standing by for more reviews for " + data.getMovieTitle());
            data.addObserver(this);
        }
    }
    // If the data service tells us there are new reviews, pay attention
    @Override
    public void update(Observable observable, Object data) {
        Log.d(TAG, "Review update callback");
        // If we haven't been initialized, there is nothing to change.
        if (!mLayoutInitialized) return;

        if (data instanceof ArrayList) {
            List list = (ArrayList) data;
            if (!list.isEmpty()) {
                if (list.get(0) instanceof Review) {
                    Log.d(TAG, "Review update callback with " + list.size() + " reviews");
                    updateReviewList((ArrayList<Review>) data);
                }
            }
        }
    }


    class ReviewViewHolder {
        final TextView titleView;
        View titleBackground;
        final ImageButton favoriteButton;
        final RecyclerView recyclerView;
        final LinearLayout frameLayout;

        ReviewViewHolder() {
            titleView = (TextView) mRootView.findViewById(R.id.detail_movie_title);
            titleBackground = mRootView.findViewById(R.id.detail_movie_title_frame);
            favoriteButton = (ImageButton) mRootView.findViewById(R.id.detail_favorite_button);

            recyclerView = (RecyclerView) mRootView.findViewById(R.id.review_recycler_view);
            //detailLayout = (RelativeLayout) mRootView.findViewById(R.id.detail_movie_background);
            frameLayout = (LinearLayout) mRootView.findViewById(R.id.fragment_review);
        }
    } // end ReviewViewHolder

    @Override
    public void onPause() {
        super.onPause();

        // Don't leave our observer lying around after we're gone
        MovieDataService.getInstance().deleteObserver(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mUIInitialized = false;
        updateUI();
    }
}
