package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.net.Uri;
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
        MovieDataService data = MovieDataService.getInstance(getContext(), null);
        Log.d(TAG, "updateVideoUI from Activity: " + getActivity());
        if (null != data && null != mRecyclerView) {
            adapter = new VideoAdapter(data.getVideoList());
            mRecyclerView.setAdapter(adapter);
            Log.d(TAG, "updateDetailUI mission accomplished");
        }
    }

    public static void onClickTrailer(View v) {
        Log.d(TAG, "Trailer card clicked");
        // We stashed the URI of the Youtube video in the cardView during VideoAdapter onBind
        Uri trailerLink = (Uri) v.getTag();
        if (null != trailerLink) {
            v.getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                    trailerLink));
        } else
            Log.d(TAG, "Trailer card container did not have trailer link.");
    }

    // return true on successful share of a video trailer link
    public boolean onShareTrailer(View v) {
        Log.d(TAG, "Trailer share");
        // We stashed the URI of the Youtube video in the cardView during VideoAdapter onBind
        Uri trailerUri = (Uri) v.getTag();
        String trailerLink = "";
        if (null != trailerUri)
            trailerLink = trailerUri.toString();
        if (trailerLink.isEmpty()) {
            // default to the first trailer
            MovieData movieData = (MovieData) getActivity();
            trailerLink = movieData.getYouTubeURL(0);
        }
        if (!trailerLink.isEmpty()) {
            Log.d(TAG, "Share trailer link " + trailerLink);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, trailerLink);
            v.getContext().startActivity(shareIntent);
            return true;
        } else
            Log.d(TAG, "Trailer card container did not have trailer link.");
        return false;
    }
}
