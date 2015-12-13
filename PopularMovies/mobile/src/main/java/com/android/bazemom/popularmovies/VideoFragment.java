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
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.bazemom.popularmovies.adapters.VideoAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Tab that displays the video trailers for the selected movie
 * Requires the caller to support the MovieData interface
 */
public class VideoFragment extends Fragment implements Observer {
    private static final String TAG = VideoFragment.class.getSimpleName();
    private View mRootView;
    VideoViewHolder mViewHolder;
    VideoAdapter adapter;
    private boolean mLayoutInitialized = false;
    private boolean mUIInitialized = false;

    public VideoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "VideoFragment.onCreateView called");

        mRootView = inflater.inflate(R.layout.fragment_video, container, false);
        mViewHolder = new VideoViewHolder();
        Utility.initDetailTitle(mRootView, mViewHolder.favoriteButton);

        // Use the default layout manager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        mViewHolder.recyclerView.setLayoutManager(linearLayoutManager);

        MovieDataService data = MovieDataService.getInstance();
        updateVideoList((ArrayList<Video>) data.getVideoList());
        mLayoutInitialized = true;

        updateUI();
        // If there is anything we need to fix up after the layout is known,
        // do it in the post-layout lambda
    /*   mRootView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "VideoFragment post-run");
                // update the UI now we can put the poster up with the right aspect ratio
                updateUI();
            }
        }); */

        return mRootView;
    }

    // Once VideoList is filled in, get the adapter to fill in the recycler view
    void updateUI() {
        if (mUIInitialized) return;

        MovieDataService data = MovieDataService.getInstance();
        Log.d(TAG, "updateVideoUI from Activity: " + getActivity());
        if (null != data && null != mViewHolder.recyclerView) {
            mViewHolder.titleView.setText(data.getMovie().title);
            mViewHolder.titleBackground.setBackgroundColor(data.getDarkBackground());
            Utility.updateFavoriteButton(mViewHolder.favoriteButton, data.getFavorite());

            updateVideoList((ArrayList<Video>) data.getVideoList());
        }
    }

    protected void updateVideoList(ArrayList<Video> videoList) {
        MovieDataService data = MovieDataService.getInstance();

        adapter = new VideoAdapter(videoList);
        mViewHolder.recyclerView.setAdapter(adapter);

        if (data.videoListComplete()  && data.videoCount() <= adapter.getItemCount()) {
            data.deleteObserver(this);
            mUIInitialized = true;
            Log.d(TAG, "updateVideoUI mission accomplished for " + data.getMovieTitle());
        } else {
            // Still waiting for more video results
            Log.d(TAG, "updateVideoUI standing by for more videos for " + data.getMovieTitle() + adapter.getItemCount());
            mUIInitialized = false;
            data.addObserver(this);
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
            MovieDataService dataService = MovieDataService.getInstance();
            if (null == dataService) {
                // bug out, we're screwed, probably out of memory
                return false;
            }
            trailerLink = dataService.getYouTubeURL(0);
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

    // If the data service tells us there are new reviews, pay attention
    @Override
    public void update(Observable observable, Object data) {
        Log.d(TAG, "Video update callback");
        // If we haven't been initialized, there is nothing to change.
        if (!mLayoutInitialized) return;

        if (data instanceof ArrayList) {
            List list = (ArrayList) data;
            if (!list.isEmpty()) {
                if (list.get(0) instanceof Video) {
                    Log.d(TAG, "Video update callback with " + list.size() + " reviews");
                    updateVideoList((ArrayList<Video>) data);
                }
            }
        }
    }
    class VideoViewHolder {
        final TextView titleView;
        View titleBackground;
        final ImageButton favoriteButton;
        final RecyclerView recyclerView;

        VideoViewHolder() {
            titleView = (TextView) mRootView.findViewById(R.id.detail_movie_title);
            titleBackground = mRootView.findViewById(R.id.detail_movie_title_frame);
            favoriteButton = (ImageButton) mRootView.findViewById(R.id.detail_favorite_button);
            recyclerView = (RecyclerView) mRootView.findViewById(R.id.video_recycler_view);
        }
    }
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
