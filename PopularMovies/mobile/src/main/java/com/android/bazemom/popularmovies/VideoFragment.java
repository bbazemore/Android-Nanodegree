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
    //private VideoViewHolder mViewHolder;
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

        updateUI();

        return mRootView;
    }

    // Once VideoList is filled in, get the adapter to fill in the recycler view
    void updateUI() {
        Log.d(TAG, "updateUI");
        MovieData data = (MovieData) getActivity();
        if (null != data && null != mRecyclerView) {
            adapter = new VideoAdapter(data.getVideoList());
            mRecyclerView.setAdapter(adapter);
            Log.d(TAG, "updateUI mission accomplished");
        }
    }


/*
    public void onClickTrailer(View v) {
        Log.d(TAG, "Trailer card clicked");
        // We stashed the URI of the Youtube video in the cardView during VideoAdapter onBind
        Uri trailerLink = (Uri) v.getTag();
        if (null != trailerLink) {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                    trailerLink));
        } else
            Log.d(TAG, "Trailer card container did not have trailer link.");
    }

    public void onShareTrailer(View v) {
        Log.d(TAG, "Trailer share");
        // We stashed the URI of the Youtube video in the cardView during VideoAdapter onBind
        Uri trailerLink = (Uri) v.getTag();
        if (null != trailerLink) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, trailerLink);
            getContext().startActivity(shareIntent);
        } else
            Log.d(TAG, "Trailer card container did not have trailer link.");
    }
    */
}
