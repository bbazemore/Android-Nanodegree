package com.android.bazemom.popularmovies;

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

/**
 * Tab that displays the reviews for the selected movie
 * Requires the caller to support the MovieData interface
 */
public class ReviewFragment extends Fragment {
    private static final String TAG = ReviewFragment.class.getSimpleName();
    private View mRootView;
    private ReviewViewHolder mViewHolder;

    ReviewAdapter adapter;

    public ReviewFragment() {
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

        return mRootView;
    }

    // Once ReviewList is filled in, get the adapter to fill in the recycler view
    void updateUI() {
        Log.d(TAG, "updateUI");
        MovieData data = (MovieData) getActivity();
        adapter = new ReviewAdapter(data.getReviewList());
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
}
