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
        // If there is anything we need to fix up after the layout is known,
        // do it in the post-layout lambda
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "ReviewFragment post-run");
                // update the UI now we can put the poster up with the right aspect ratio
                updateUI();
            }
        });
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Make sure we are pointing at the currently selected movie
        updateUI();
    }

    // Once ReviewList is filled in, get the adapter to fill in the recycler view
    void updateUI() {
        Log.d(TAG, "updateReviewUI");
        MovieDataService data = MovieDataService.getInstance();
        if (null != data && null != mViewHolder) {
            {
                adapter = new ReviewAdapter(data.getReviewList());
                mViewHolder.recyclerView.setAdapter(adapter);
                data.addObserver(this);
            }
        }
    }

    // If the data service tells us there are new reviews, pay attention
    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof ArrayList) {
            List list = (ArrayList) data;
            if (!list.isEmpty()) {
                if (list.get(0) instanceof Review) {
                    updateUI();
                }
            }
        }
    }

    class ReviewViewHolder {
        final TextView titleView;
        final RecyclerView recyclerView;
        final LinearLayout frameLayout;

        ReviewViewHolder() {
            titleView = (TextView) mRootView.findViewById(R.id.detail_movie_title);

            recyclerView = (RecyclerView) mRootView.findViewById(R.id.review_recycler_view);
            //detailLayout = (RelativeLayout) mRootView.findViewById(R.id.detail_movie_background);
            frameLayout = (LinearLayout) mRootView.findViewById(R.id.fragment_review);
        }
    } // end ReviewViewHolder
}
