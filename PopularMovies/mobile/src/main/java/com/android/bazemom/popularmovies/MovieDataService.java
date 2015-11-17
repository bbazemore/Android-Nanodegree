package com.android.bazemom.popularmovies;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.bazemom.popularmovies.moviebusevents.LoadMovieDetailEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadReviewsEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadVideosEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieDetailLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.ReviewsLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.VideosLoadedEvent;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBHelper;
import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;
import com.android.bazemom.popularmovies.moviemodel.ReviewModel;
import com.android.bazemom.popularmovies.moviemodel.VideoModel;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Fragments pull from the read-only MovieData interface
 * Keeps the details, reviews and videos for one Movie handy
 * so the various Activities and Fragments can access it.
 */
interface MovieData {
    //public final static String EXTRA_MOVIE_ID = "com.android.bazemom.popularmovies.app.MovieId";
    String EXTRA_MOVIE = "com.android.bazemom.popularmovies.app.Movie";
    String packageName = DetailActivity.class.getPackage().getName();
    String MOVIE = packageName + ".Movie";
    String REVIEW_KEY = packageName + ".ReviewList";
    String MOVIE_DETAIL = packageName + ".MovieDetail";
    String TRAILER_KEY = packageName + ".TrailerList";

    int getMovieId();
    Movie getMovie();
    MovieDetail getMovieDetail();
    List<Review> getReviewList();
    List<Video> getVideoList();

    // String getYouTubeKey(int videoPosition);
    String getYouTubeURL(int videoPosition);

    int getFavorite();
    void setFavorite(int value);
}

// The MovieDataService pulls data from a local database and from the TMDB RESTful Web API
// as data model (moviemodel) objects.  The service makes the data available to Observers
// as parcelable Movie, MovieData, etc objects via the MovieData interface.
public class MovieDataService extends Observable implements MovieData {
    private final static String TAG = MovieDataService.class.getSimpleName();

    private final ReviewModel EMPTY_REVIEW = new ReviewModel();
    private final VideoModel EMPTY_VIDEO = new VideoModel();

    private Context mContext;
    private Bus mBus; // the bus that is used to deliver messages to the TMDB dispatcher
    private DispatchTMDB mDispatchTMDB;

    // Otto gets upset if the Fragment disappears while still subscribed to outstanding events
    // Turn event notification off when we are shutting down, register for events once when
    // starting back up.
    private boolean requestData = true;
    private boolean mReceivingEvents;
    private boolean mDataReceivedDetail = false;
    private boolean mDataReceivedReviewList = false;
    private boolean mDataReceivedVideoList = false;

    // Movie API management, keep track of how many pages of data we've received
    // so we know if we are asking for more for the current movie, or starting over.
    protected int mReviewPageRequest = 0;

    // Data items
    Movie mMovie;
    protected MovieDetail mMovieDetail;
    protected ArrayList<Review> mReviewList;
    protected ArrayList<Video> mVideoList;

    private MovieDataService() {
    }

    private void initializeConstructor(Context context, Movie movie) {
        // so we can get to string resources
        mContext = context;
        setMovie(movie);
    }

    private static class SingletonHelper {
        private static final MovieDataService INSTANCE = new MovieDataService();
    }

    // Master or Detail Activity calls this flavor constructor
    public static MovieDataService getInstance(Context context, Movie movie) {
        MovieDataService service = SingletonHelper.INSTANCE;
        service.initializeConstructor(context, movie);
        return service;
    }

    // Fragments call this constructor, preferably after the Activity has set up context
    public static MovieDataService getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public void readInstanceState(Context context, Bundle savedInstanceState) {
        mContext = context;
        setMovie((Movie) savedInstanceState.getParcelable(MOVIE));
        setMovieDetail((MovieDetail) savedInstanceState.getParcelable(MOVIE_DETAIL));
        if (null != mMovieDetail) {
            List<Review> existingReviews = savedInstanceState.getParcelableArrayList(REVIEW_KEY);
            setReviewList(existingReviews);
            List<Video> existingVideos = savedInstanceState.getParcelableArrayList(TRAILER_KEY);
            setVideoList(existingVideos);
        } else {
            initialize();
        }
    }

    public Bundle saveInstanceState(Bundle outState) {
        outState.putParcelable(MOVIE_DETAIL, mMovieDetail);
        outState.putParcelableArrayList(REVIEW_KEY, mReviewList);
        outState.putParcelableArrayList(TRAILER_KEY, mVideoList);
        return outState;
    }

    public MovieDataService(Context context, Bundle savedInstanceState) {
        requestData = true;
        readInstanceState(context, savedInstanceState);
    }

    private void initialize() {
        mDataReceivedDetail = false;
        mDataReceivedReviewList = false;
        mDataReceivedVideoList = false;
        mMovieDetail = null;
        mReviewPageRequest = 1;

        requestData = true;
        mReviewList = new ArrayList<Review>();
        mVideoList = new ArrayList<Video>();

        // Set up in case we have movies with no reviews or trailers
        EMPTY_REVIEW.setAuthor("");
        EMPTY_REVIEW.setContent(mContext.getString(R.string.review_no_reviews));

        // Start the data cooking
        getDetails(1);
        getReviews(1);
        getVideos(1);
    }


    ////////////////////////////////////////////////////
    // MovieData interface, mixed in with underlying Observable
    ///////////////////////////////////////////////////
    @Override
    public int getMovieId() {
        return mMovie.id;
    }

    @Override
    public Movie getMovie() {
        return mMovie;
    }
    private boolean setMovie(Movie movie) {
        boolean movieChanged = false;
        if (movie == null ){
            if (mMovie != null) {
                movieChanged = true;
                Log.d(TAG, "setMovie to null when old movie was " + mMovie.title);
                // clear out whatever we had
                initialize();
            } // else both are null, nothing new
        } else // We have a non-null movie
            if (mMovie != null) {
                if (mMovie.id != movie.id) {
                    movieChanged = true;
                    Log.d(TAG, "setMovie to '" + movie.title + "' when old movie was: " + mMovie.title);
                }
                // else the movies are the same
            } else {  // old movie empty, new movie non-null
                Log.d(TAG, "New movie: " + movie.title);
                movieChanged = true;
            }
        // wrap it up without raising false change notifications
        if (movieChanged) {
            mMovie = movie;
            setChanged();
            initialize();
            notifyObservers(mMovie);
        }
        return movieChanged;
    }

    @Override
    public MovieDetail getMovieDetail() {
        return mMovieDetail;
    }

    private void setMovieDetail(MovieDetail movieDetail) {
        if (mMovieDetail != movieDetail) {
            Log.d(TAG, "movie detail changed to " + movieDetail.title);
            setChanged();
            mMovieDetail = movieDetail;
            notifyObservers(mMovieDetail);
        }
    }

    @Override
    public List<Review> getReviewList() {
        return mReviewList;
    }

    private void setReviewList(List<Review> reviewList) {
        if (!reviewList.isEmpty()) {
            mReviewList.addAll(reviewList);
            setChanged();
            notifyObservers(mReviewList);
        }
    }

    @Override
    public List<Video> getVideoList() {
        return mVideoList;
    }

    private void setVideoList(List<Video> videoList) {
        if (!videoList.isEmpty()) {
            mVideoList.addAll(videoList);
            setChanged();
            notifyObservers(mVideoList);
        }
    }

    public String getYouTubeURL(int videoPosition) {
        String youtubeURL = "YouTube key not available for this movie";
        try {
            youtubeURL = mVideoList.get(videoPosition).key;
            youtubeURL = Uri.parse("http://www.youtube.com/watch?v=" + youtubeURL).toString();
        } catch (Exception e) {
            Log.d(TAG, "getYouTubeKey failed for video at index: " + videoPosition);
        }

        return youtubeURL;
    }

    @Override
    public int getFavorite() {
        return mMovieDetail.getFavorite();
    }

    @Override
    public void setFavorite(int value) {
        mMovieDetail.setFavorite(value);

        // Persist the favorite setting in the local database
        LocalDBHelper dbHelper = new LocalDBHelper(mContext);
        dbHelper.updateMovieInLocalDB(mMovieDetail);
        if (value == 1)
            Toast.makeText(mContext, R.string.favorite_added, Toast.LENGTH_SHORT).show();
    }
    /// end MovieData interface

    public void onResume() {
        // Log.d(TAG, "on resume");
        // We are back on display. Pay attention to movie results again.
        receiveEvents();
    }

    public void onPause() {
        //Log.d(TAG, "on pause");
        // Don't bother processing results when we aren't on display.
        stopReceivingEvents();
    }

    // Use some kind of injection, so that we can swap in a mock for tests.
    // Here we just use simple getter/setter injection for simplicity.
    protected Bus getBus() {
        if (mBus == null) {
            if (mDispatchTMDB == null) {
                mDispatchTMDB = DispatchTMDB.getInstance();
            }
            setBus(mDispatchTMDB.shareBus()); // can get fancy with an injector later BusProvider.getInstance();
        }
        return mBus;
    }

    private void setBus(Bus bus) {
        mBus = bus;
    }

    private void receiveEvents() {
        if (!mReceivingEvents) {
            try {
                Log.d(TAG, "DetailActivity Events on");
                getBus().register(this);
                mReceivingEvents = true;
            } catch (Exception e) {
                Log.i(TAG, "receiveEvents could not register with Otto bus");
            }
        }
    }

    private void stopReceivingEvents() {
        if (mReceivingEvents) {
            try {
                Log.d(TAG, "DetailActivity Events off");
                getBus().unregister(this);
                mReceivingEvents = false;
            } catch (Exception e) {
                Log.i(TAG, "stopReceivingEvents could not unregister with Otto bus");
            }
        }
    }

    // moviesLoaded gets called when we get a list of movies back from TMDB
    @Subscribe
    public void movieDetailLoaded(MovieDetailLoadedEvent event) {
        // load the movie data into our movies list
        setMovieDetail(event.movieResult);
        mDataReceivedDetail = true;
        Log.i(TAG, "movie detail Loaded ");
    }

    private void getDetails(int nextPage) {
        // Is this movie cached in the local DB?
        if (mDataReceivedDetail)
            // We have the detail data, we're done.
            return;

        if (null == mMovie)
            return;

        LocalDBHelper dbHelper = new LocalDBHelper(mContext);
        MovieDetail dbMovieDetail = dbHelper.getMovieDetailFromDB(mMovie.id);
        if (null != dbMovieDetail) {
            // We are in luck, we have the movie details handy already.
            mDataReceivedDetail = true;
            // Let the activities know the detail is available
            setMovieDetail(dbMovieDetail);
        } else {
            // We have to get the movie from the cloud
            // Start listening for the Movie Detail loaded event
            receiveEvents();

            //  Now request that the movie details be loaded
            String apiKey = mContext.getString(R.string.movie_api_key);
            LoadMovieDetailEvent loadMovieRequest = new LoadMovieDetailEvent(apiKey, mMovie.id);
            getBus().post(loadMovieRequest);
        }
    }

    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void reviewsLoaded(ReviewsLoadedEvent event) {
        Log.d(TAG, "reviews Loaded callback! Number of reviews: " + event.reviewResults.size());

        List<Review> newReviews = new ArrayList<Review>();
        // load the review data into our movies list
        for (ReviewModel data : event.reviewResults) {
            newReviews.add(new Review(data));
        }

        if (event.endOfInput || newReviews.isEmpty()) {
            mDataReceivedReviewList = true;
            if (mReviewList.isEmpty() && newReviews.isEmpty()) {
                newReviews.add(new Review(EMPTY_REVIEW));
            }
        } else {
            // Ask for another page of reviews
            getReviews(event.currentPage + 1);
        }
        setReviewList(newReviews);
    }

    protected void getReviews(int nextPage) {
        if (mDataReceivedReviewList)
            // we have all the reviews for this movie,
            // don't keep asking.
            return;

        //  TODO: Is this movie cached in the local DB?
        /*LocalDBHelper dbHelper = new LocalDBHelper(mRootView.getContext());
        mReviewList = dbHelper.getMovieReviewsFromDB(mMovieId);
        if (null != mReviewList) {
            // We are in luck, we have the movie details handy already.
            // We can update the UI right away
            updateDetailUI();
        } else { */
        // We have to get the movie from the cloud
        // Start listening for the Reviews loaded event
        receiveEvents();

        //  Now request that the reviews be loaded
        if (null != mMovie) {
            String apiKey = mContext.getString(R.string.movie_api_key);
            LoadReviewsEvent loadReviewsRequest = new LoadReviewsEvent(apiKey, mMovie.id, nextPage);

            Log.i(TAG, "request reviews");
            getBus().post(loadReviewsRequest);
        }
    }

    protected void getVideos(int nextPage) {
        if (mDataReceivedVideoList)
            // we have all the videos for this movie
            return;

        //  TODO: Is this movie cached in the local DB?
        /*LocalDBHelper dbHelper = new LocalDBHelper(mRootView.getContext());
        mVideoList = dbHelper.getMovieVideosFromDB(mMovieId);
        if (null != mReviewList) {
            // We are in luck, we have the movie details handy already.
            mDataReceivedVideoList = true;
            // We can update the UI right away
            updateDetailUI();
        } else { */
        // We have to get the movie from the cloud
        // Start listening for the Reviews loaded event
        receiveEvents();

        //  Now request that the trailers be loaded
        if (null != mMovie) {
            String apiKey = mContext.getString(R.string.movie_api_key);
            LoadVideosEvent loadVideosRequest = new LoadVideosEvent(apiKey, mMovie.id);

            Log.i(TAG, "request video trailers");
            getBus().post(loadVideosRequest);
        }
    }

    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void videosLoaded(VideosLoadedEvent event) {
        Log.i(TAG, "videos Loaded callback! Number of trailers: " + event.trailerResults.size());
        List<Video> newVideos = new ArrayList<Video>();
        // load the movie data into our movies list
        for (VideoModel data : event.trailerResults) {
            newVideos.add(new Video(data));
        }
        mDataReceivedVideoList = true;

        if (newVideos.isEmpty()) {
            newVideos.add(new Video(EMPTY_VIDEO));
        }
        setVideoList(newVideos);
    }
}
