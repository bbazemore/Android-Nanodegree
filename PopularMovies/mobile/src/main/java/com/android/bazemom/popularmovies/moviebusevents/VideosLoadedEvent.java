package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import com.android.bazemom.popularmovies.moviemodel.MovieVideoListModel;
import com.android.bazemom.popularmovies.moviemodel.VideoModel;

import java.util.List;

// Event posted from DispatchTMDB when a set of Movie trailers arrives back from TMDB
public class VideosLoadedEvent {
    private final static String TAG = VideosLoadedEvent.class.getSimpleName();
    public List<VideoModel> trailerResults;

    public VideosLoadedEvent(MovieVideoListModel videosReturned)
    {
        trailerResults = videosReturned.getResults();
        Log.i(TAG, "event created with " + videosReturned.getResults().size() + " results");
    }
}
