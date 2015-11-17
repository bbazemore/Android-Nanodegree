package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

import com.android.bazemom.popularmovies.moviemodel.MovieReviewListModel;
import com.android.bazemom.popularmovies.moviemodel.ReviewModel;

import java.util.List;

// Event posted from DispatchTMDB when a set of Movies arrives back from TMDB
public class ReviewsLoadedEvent {
    private final static String TAG = ReviewsLoadedEvent.class.getSimpleName();
    public List<ReviewModel> reviewResults;
    public int totalPages;  // how many pages of reviews are there?
    public int currentPage;
    public boolean endOfInput;
    public ReviewsLoadedEvent(MovieReviewListModel reviewsReturned)
    {
        reviewResults = reviewsReturned.getResults();
        totalPages = reviewsReturned.getTotalPages();  // this is sometimes 0
        currentPage = reviewsReturned.getPage();
        endOfInput = (totalPages-currentPage) <= 0 ;
        Log.i(TAG, "ReviewsLoadedEvent created with " + reviewsReturned.getResults().size() + " results");
    }

}
