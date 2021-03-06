package com.android.bazemom.popularmovies;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.bazemom.popularmovies.moviebusevents.LoadMovieDetailEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadReviewsEvent;
import com.android.bazemom.popularmovies.moviebusevents.LoadVideosEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieApiErrorEvent;
import com.android.bazemom.popularmovies.moviebusevents.MovieDetailLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.ReviewsLoadedEvent;
import com.android.bazemom.popularmovies.moviebusevents.VideosLoadedEvent;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBHelper;
import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;
import com.android.bazemom.popularmovies.moviemodel.MovieDetailModel;
import com.android.bazemom.popularmovies.moviemodel.MovieModel;
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
    String DARK_BACKGROUND_COLOR = packageName + ".DarkBackgroundColor";
    String LIGHT_BACKGROUND_COLOR = packageName + ".LightBackgroundColor";
    int getMovieId();

    Movie getMovie();
    String getMovieTitle();
    MovieDetail getMovieDetail();
    boolean movieDetailComplete();

    List<Review> getReviewList();
    boolean reviewListComplete();
    int reviewCount();

    List<Video> getVideoList();
    boolean videoListComplete();
    int videoCount();

    // String getYouTubeKey(int videoPosition);
    String getYouTubeURL(int videoPosition);

    int getFavorite();

    void setFavorite(Context context, int value);

    int getDarkBackground();
    void setDarkBackground(int darkBackround);
    int getLightBackground();
    void setLightBackground(int lightBackround);

}

// The MovieDataService pulls data from a local database and from the TMDB RESTful Web API
// as data model (moviemodel) objects.  The service makes the data available to Observers
// as parcelable Movie, MovieData, etc objects via the MovieData interface.
public class MovieDataService extends Observable implements MovieData {
    private final static String TAG = MovieDataService.class.getSimpleName();

    private static final MovieModel EMPTY_MOVIE = new MovieModel();
    private static final MovieDetailModel EMPTY_DETAIL = new MovieDetailModel();
    private static final ReviewModel EMPTY_REVIEW = new ReviewModel();
    private static final VideoModel EMPTY_VIDEO = new VideoModel();
    private static final MovieDetailModel ERROR_DETAIL = new MovieDetailModel();
    private static final ReviewModel ERROR_REVIEW = new ReviewModel();
    private static final VideoModel ERROR_VIDEO = new VideoModel();

    private static Bus mBus; // the bus that is used to deliver messages to the TMDB dispatcher
    private static DispatchTMDB mDispatchTMDB;
    private static String mAPIKey;
    private static LocalDBHelper mDBHelper;

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

    private int mDarkBackground;
    private int mLightBackround;

    private MovieDataService() {
    }

    private void initializeConstructor(Context context, Movie movie) {
        // initialize the things that require context, like strings
        mAPIKey = context.getString(R.string.movie_api_key);
        mDBHelper = new LocalDBHelper(context);

        // Set up in case we have movies with no reviews or trailers
        // This one little get of a string from resources is the reason we need
        // to keep passing the current context around.  Yeesh.
        EMPTY_MOVIE.setId(0);
        EMPTY_MOVIE.setTitle(context.getString(R.string.detail_no_detail_title));

        EMPTY_DETAIL.setId(0);
        EMPTY_DETAIL.setTitle(context.getString(R.string.detail_no_detail_title));
        EMPTY_DETAIL.setOverview(context.getString(R.string.detail_no_detail_overview));
        EMPTY_REVIEW.setAuthor("");
        EMPTY_REVIEW.setContent(context.getString(R.string.review_no_reviews));

        EMPTY_VIDEO.setSite(context.getString(R.string.tmdb_site_value_YouTube));
        EMPTY_VIDEO.setName(context.getString(R.string.video_no_videos));

        ERROR_DETAIL.setOverview(context.getString(R.string.detail_movie_error));
        ERROR_REVIEW.setContent(context.getString(R.string.review_error));
        ERROR_VIDEO.setName(context.getString(R.string.video_error));
        ERROR_VIDEO.setSite(context.getString(R.string.tmdb_site_value_YouTube));

        // and finally...
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

    public void readInstanceState(Bundle savedInstanceState) {
        setMovie((Movie) savedInstanceState.getParcelable(MOVIE));
        setMovieDetail((MovieDetail) savedInstanceState.getParcelable(MOVIE_DETAIL));
        setDarkBackground(savedInstanceState.getInt(DARK_BACKGROUND_COLOR));
        setLightBackground(savedInstanceState.getInt(LIGHT_BACKGROUND_COLOR));

        if (null != mMovieDetail) {
            mDataReceivedDetail = true;
            // Restore reviews from cache
            List<Review> existingReviews = savedInstanceState.getParcelableArrayList(REVIEW_KEY);
            setReviewList(existingReviews);

            // Restore trailer info from cache
            List<Video> existingVideos = savedInstanceState.getParcelableArrayList(TRAILER_KEY);
            setVideoList(existingVideos);
        } else {
            // Start the data cooking
            initialize();
        }
    }

    public Bundle saveInstanceState(Bundle outState) {
        outState.putParcelable(MOVIE_DETAIL, mMovieDetail);
        outState.putParcelableArrayList(REVIEW_KEY, mReviewList);
        outState.putParcelableArrayList(TRAILER_KEY, mVideoList);
        outState.putInt(DARK_BACKGROUND_COLOR, mDarkBackground);
        outState.putInt(LIGHT_BACKGROUND_COLOR, mLightBackround);
        return outState;
    }

    public MovieDataService(Bundle savedInstanceState) {
        requestData = true;
        readInstanceState(savedInstanceState);
    }

    // Must set the API key before this is called.
    @SuppressWarnings("Convert2Diamond")
    private void initialize() {
        mDataReceivedDetail = false;
        mDataReceivedReviewList = false;
        mDataReceivedVideoList = false;
        mMovieDetail = null;
        mReviewPageRequest = 1;

        requestData = true;
        mReviewList = new ArrayList<Review>();
        mVideoList = new ArrayList<Video>();

        // UI colors
        mDarkBackground = R.color.background_material_dark;
        mLightBackround = R.color.background_material_light;

        // Start the data cooking
        getDetails();
        getReviews(1);
        getVideos(1);
    }


    ////////////////////////////////////////////////////
    // MovieData interface, mixed in with underlying Observable
    ///////////////////////////////////////////////////
    @Override
    public int getMovieId() {
        if (null == mMovie) return 0;
        return mMovie.id;
    }

    @Override
    public Movie getMovie() {
        if (null == mMovie) {
            return new Movie(EMPTY_MOVIE);
        }
        return mMovie;
    }

    @Override
    public String getMovieTitle() {
        if (null == mMovie) return "";
        return mMovie.title;
    }
    private boolean setMovie(Movie movie) {
        boolean movieChanged = false;
        if (movie == null) {
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
        if (null == mMovieDetail)
            return new MovieDetail(EMPTY_DETAIL);
        return mMovieDetail;
    }

    @Override
    public boolean movieDetailComplete() {
        return mDataReceivedDetail;
    }

    private void setMovieDetail(MovieDetail movieDetail) {
        if (mMovieDetail != movieDetail) {
            //Log.d(TAG, "movie detail changed to " + movieDetail.title);
            setChanged();
            mMovieDetail = movieDetail;
            notifyObservers(mMovieDetail);
        }
    }

    @Override
    public List<Review> getReviewList() {
        return mReviewList;
    }

    @Override
    public boolean reviewListComplete() {
        return mDataReceivedReviewList;
    }

    @Override
    public int reviewCount() {
        if (null == mReviewList) return 0;
        return mReviewList.size();
    }
    private void setReviewList(List<Review> reviewList) {
        if (reviewList != null && !reviewList.isEmpty()) {
            mReviewList.addAll(reviewList);
            setChanged();
            notifyObservers(mReviewList);
        }
    }

    @Override
    public List<Video> getVideoList() {
        return mVideoList;
    }

    @Override
    public boolean videoListComplete() {
        return mDataReceivedVideoList;
    }

    @Override
    public int videoCount() {
        if (null == mVideoList) return 0;
        return mVideoList.size();
    }

    private void setVideoList(List<Video> videoList) {
        if (videoList != null && !videoList.isEmpty()) {
            mVideoList.addAll(videoList);
            setChanged();
            Log.d(TAG, "stashed videos " + mVideoList.size());
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
        return (mMovieDetail == null ? 0 : mMovieDetail.getFavorite());
    }

    @Override
    public void setFavorite(Context context, int value) {
        if (null == mMovieDetail) {
            Toast.makeText(context, R.string.favorite_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean favoriteChanged =  mMovieDetail.getFavorite() != value;
        mMovieDetail.setFavorite(value);

        // Persist the favorite setting in the local database
        LocalDBHelper dbHelper = new LocalDBHelper(context);
        dbHelper.updateMovieInLocalDB(mMovieDetail);
        if (favoriteChanged) {
            setChanged();
            notifyObservers(mMovieDetail);
        }
        if (value == 1)
            Toast.makeText(context, R.string.favorite_added, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getDarkBackground() {
        return mDarkBackground;
    }
    @Override
    public void setDarkBackground(int darkBackground) {
        Log.d(TAG, "setDarkBackground " + darkBackground);
        mDarkBackground = darkBackground;
    }
    @Override
    public int getLightBackground() {
        return mLightBackround;
    }
    @Override
    public void setLightBackground(int lightBackround) {
        mLightBackround = lightBackround;
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
            setBus(DispatchTMDB.shareBus()); // can get fancy with an injector later BusProvider.getInstance();
        }
        return mBus;
    }

    private void setBus(Bus bus) {
        mBus = bus;
    }

    private void receiveEvents() {
        if (!mReceivingEvents) {
            try {
                Log.d(TAG, "Events on");
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
        Log.d(TAG, "movie detail Loaded ");
    }

    private void getDetails() {
        // Is this movie cached in the local DB?
        if (mDataReceivedDetail)
            // We have the detail data, we're done.
            return;

        if (null == mMovie)
            return;

        MovieDetail dbMovieDetail = mDBHelper.getMovieDetailFromDB(mMovie.id);
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
            LoadMovieDetailEvent loadMovieRequest = new LoadMovieDetailEvent(mAPIKey, mMovie.id);
            getBus().post(loadMovieRequest);
        }
    }

    // reviewsLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    @SuppressWarnings("Convert2Diamond")
    public void reviewsLoaded(ReviewsLoadedEvent event) {
        Log.d(TAG, "reviews Loaded callback! Number of reviews: " + ((event.reviewResults == null) ? "0" : event.reviewResults.size()) + " " + getMovieTitle());

        List<Review> newReviews = new ArrayList<Review>();
        // load the review data into our movies list
        for (ReviewModel newReview : event.reviewResults) {
            newReviews.add(new Review(newReview));
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
        Log.d(TAG, "reviewsLoaded setting reviews list with " + newReviews.size() + " reviews for " + getMovieTitle());
        setReviewList(newReviews);
    }

    protected void getReviews(int nextPage) {
        if (null == mMovie) return;
        if (mDataReceivedReviewList)
            // we have all the reviews for this movie,
            // don't keep asking.
            return;

        //  TODO: Is this movie cached in the local DB?
        /*LocalDBHelper mDBHelper = new LocalDBHelper(mRootView.getContext());
        mReviewList = mDBHelper.getMovieReviewsFromDB(mMovieId);
        if (null != mReviewList) {
            // We are in luck, we have the movie details handy already.
            // We can update the UI right away
            updateDetailUI();
        } else { */
        // We have to get the movie from the cloud
        // Start listening for the Reviews loaded event
        receiveEvents();

        //  Now request that the reviews be loaded
        LoadReviewsEvent loadReviewsRequest = new LoadReviewsEvent(mAPIKey, mMovie.id, nextPage);

        Log.d(TAG, "request reviews");
        getBus().post(loadReviewsRequest);
    }

    protected void getVideos(int nextPage) {
        if (null == mMovie) return;
        if (mDataReceivedVideoList)
            // we have all the videos for this movie
            return;

        //  TODO: Is this movie cached in the local DB?
        /*LocalDBHelper mDBHelper = new LocalDBHelper(mRootView.getContext());
        mVideoList = mDBHelper.getMovieVideosFromDB(mMovieId);
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
        LoadVideosEvent loadVideosRequest = new LoadVideosEvent(mAPIKey, mMovie.id);

        Log.d(TAG, "request video trailers");
        getBus().post(loadVideosRequest);
    }

    // videosLoaded gets called when we get a list of reviews back from TMDB
    @Subscribe
    public void videosLoaded(VideosLoadedEvent event) {
        Log.d(TAG, "videos Loaded callback! Number of trailers: " + event.trailerResults.size());
        List<Video> newVideos = new ArrayList<>();
        // load the movie data into our movies list
        for (VideoModel data : event.trailerResults) {
            newVideos.add(new Video(data));
        }

        if (newVideos.isEmpty()) {
            newVideos.add(new Video(EMPTY_VIDEO));
        }
        setVideoList(newVideos);
        mDataReceivedVideoList = true;
    }

    // When we get an error, set up a dummy card with the error message and post it
    // like regular results so the user can see it.
    @Subscribe
    public void movieApiError(MovieApiErrorEvent event) {
        String eventMessage = "";
        if (null == event) {
            Log.d(TAG, "Null event received in movieApiError");
            return;
        }
        if (null != event.error) {
            eventMessage = event.error.getLocalizedMessage();
            if (eventMessage == null || eventMessage.isEmpty()) {
                eventMessage = event.error.getMessage();
            }
            if (eventMessage == null || eventMessage.isEmpty()) {
                eventMessage = "";
            }

        }
        switch (event.objectTypeName) {
            case (MovieDetailModel.TAG): {
                MovieDetailModel errorDetail = new MovieDetailModel();
                errorDetail.setId(this.getMovieId());
                if (null != mMovie)
                    errorDetail.setTitle(this.mMovie.title);
                errorDetail.setOverview(errorDetail.getOverview().concat(eventMessage));
                mDataReceivedDetail = true;
                mBus.post(new MovieDetailLoadedEvent(errorDetail));
                break;
            }
            case (Review.TAG): {
                ReviewModel errorReview = new ReviewModel(ERROR_REVIEW);
                errorReview.setContent(errorReview.getContent().concat(eventMessage));
                mDataReceivedReviewList = true;
                mBus.post(new ReviewsLoadedEvent(errorReview));
                break;
            }
            case (Video.TAG): {
                VideoModel errorVideo = new VideoModel(ERROR_VIDEO);
                errorVideo.setName(errorVideo.getName().concat(eventMessage));
                mDataReceivedVideoList = true;
                mBus.post(new VideosLoadedEvent(errorVideo));
                break;
            }
            default:
                Log.d(TAG, "movieApiError: " + event.objectTypeName + " " + eventMessage);
        }
    }
}
