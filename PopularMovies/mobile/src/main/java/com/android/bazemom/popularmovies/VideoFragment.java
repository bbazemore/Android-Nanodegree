package com.android.bazemom.popularmovies;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.bazemom.popularmovies.adapters.VideoAdapter;

/**
 * Tab that displays the video trailers for the selected movie
 * Requires the caller to support the MovieData interface
 */
public class VideoFragment extends Fragment {
    private static final String TAG = VideoFragment.class.getSimpleName();
    private View mRootView;
    private RecyclerView mRecyclerView;
    VideoAdapter adapter;

    public VideoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "VideoFragment.onCreateView called");

        mRootView = inflater.inflate(R.layout.fragment_video, container, false);
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.video_recycler_view);

        // Use the default layout manager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);

        // If there is anything we need to fix up after the layout is known,
        // do it in the post-layout lambda
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "VideoFragment post-run");
                // update the UI now we can put the poster up with the right aspect ratio
                updateUI();
            }
        });

        return mRootView;
    }

    // Once VideoList is filled in, get the adapter to fill in the recycler view
    void updateUI() {
        MovieData data = (MovieData) getActivity();
        Log.d(TAG, "updateUI from Activity: " + getActivity().getLocalClassName());
        if (null != data && null != mRecyclerView) {
            adapter = new VideoAdapter(data.getVideoList());
            mRecyclerView.setAdapter(adapter);
            Log.d(TAG, "updateUI mission accomplished");
        }
    }

}
