package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import com.android.bazemom.popularmovies.moviemodel.MovieReviewListModel;
import com.android.bazemom.popularmovies.moviemodel.ReviewModel;

import java.util.ArrayList;
import java.util.List;

// Event posted from DispatchTMDB when a set of Movies arrives back from TMDB
public class ReviewsLoadedEvent {
    private final static String TAG = ReviewsLoadedEvent.class.getSimpleName();
    public List<ReviewModel> reviewResults;
    public int totalPages = 0;  // how many pages of reviews are there?
    public int currentPage = 0;
    public boolean endOfInput = false;
    public ReviewsLoadedEvent(MovieReviewListModel reviewsReturned)
    {
        int numReviews = 0;
        if (reviewsReturned != null) {
            reviewResults = reviewsReturned.getResults();
            totalPages = reviewsReturned.getTotalPages();  // this is sometimes 0
            currentPage = reviewsReturned.getPage();
            endOfInput = (totalPages - currentPage) <= 0;
        }
        if (null == reviewResults) {
            reviewResults = new ArrayList<ReviewModel>(0);
        } else {
            numReviews = reviewResults.size();
        }
        Log.i(TAG, "ReviewsLoadedEvent created with " + numReviews + " results");
    }

}
