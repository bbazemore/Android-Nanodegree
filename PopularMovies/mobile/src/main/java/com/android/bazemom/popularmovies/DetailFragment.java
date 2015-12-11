package com.android.bazemom.popularmovies;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.bazemom.popularmovies.adapters.PaletteTransformation;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Observable;
import java.util.Observer;

// This class is effectively nested inside the DetailActivity class
// Requires the caller to implement the MovieData interface so it can
// get the movie detail data
public final class DetailFragment extends Fragment implements Observer {
    private static final String TAG = DetailFragment.class.getSimpleName();
    private Movie mMovie;
    private MovieDetail mMovieDetail;
    private View mRootView;
    private DetailViewHolder mViewHolder;
    private boolean mLayoutInitialized = false;
    private boolean mUIInitialized = false;
    private boolean mBackgroundInitialzed = false;

    public DetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        // Get the layout adjusted to the new orientation / device
        mRootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // Get the ids of the View elements so we don't have to fetch them over and over
        mViewHolder = new DetailViewHolder();
        // slide nerd was doing mRootView.setTag(mViewHolder) - I'm just keeping it in a member variable

        Utility.initDetailTitle(getContext(), mRootView, mViewHolder.favoriteButton);

        boolean haveGoodMovieDetail = false;
        if (null != savedInstanceState) {
            Movie cachedMovie = savedInstanceState.getParcelable(MovieData.MOVIE);
            setMovie(cachedMovie);
            MovieDetail cachedDetail = savedInstanceState.getParcelable(MovieData.MOVIE_DETAIL);
            haveGoodMovieDetail = setMovieDetail(cachedDetail);
        }
        // Get the latest and greatest movie info, which may be more recent than what was saved.
        // Register for updates when the movie data changes
        MovieDataService dataService = MovieDataService.getInstance();
        if (!haveGoodMovieDetail) {
            if (setMovie(dataService.getMovie())) {
                // fill in the simple things
                updateMovieUI(mMovie);
            }
            if (setMovieDetail(dataService.getMovieDetail())) {
                updateDetailUI(mMovieDetail);
            }
        }
        // If there is anything we need to fix up after the layout is known,
        // do it in the post-layout lambda
        mRootView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "DetailFragment post-run");
                // update the UI now we can put the poster up with the right aspect ratio
                updateDetailUI(mMovieDetail);
            }
        });
        // Pay attention to movie & detail changes from the data service
        dataService.addObserver(this);
        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(MovieData.MOVIE, mMovie);
        outState.putParcelable(MovieData.MOVIE_DETAIL, mMovieDetail);
    }

    @Override
    public void onPause() {
        super.onPause();

        // fragment can't receive events, so don't try to deliver them
        MovieDataService.getInstance().deleteObserver(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mUIInitialized = false;

        // Make sure we are pointing at the currently selected movie
        MovieDataService dataService = MovieDataService.getInstance();
        setMovie(dataService.getMovie());
        setMovieDetail(dataService.getMovieDetail());

        // We are back on display. Pay attention to movie results again.
        if (null != mMovieDetail) {
            // We have what we need to fill in the UI in this fragment
            updateDetailUI(mMovieDetail);
        } else {
            // fill in the minimum UI and keep an eye out for the Detail info
            updateMovieUI(mMovie);
            dataService.addObserver(this);
        }
    }

    // Update a little bit of the UI right away with the movie that was passed
    // in.
    protected void updateMovieUI(Movie movie) {
        if (!mLayoutInitialized) {
            if (null != movie) {
                Log.d(TAG, "updateMovieUI no layout, skipping update for " + movie.title);
            }
            return;
        }
        if (null == movie) {
            Log.d(TAG, "updateMovieUI: movie data is null.  Skip the update but clear the UI");
            clearUI();
            return;
        }
        // We have a layout and we have a movie, refresh the UI
        mViewHolder.titleView.setText(movie.title);
        mViewHolder.overview.setText(movie.overview);
        if (null != getResources()) {
            mViewHolder.releaseDate.setText(getResources().getString(R.string.detail_release_date) + movie.releaseDate);
            mViewHolder.rating.setText(String.format(getResources().getString(R.string.detail_movie_user_rating), movie.voteAverage));
        }
        if (mMovieDetail != null && mMovieDetail.id != movie.id) {
            // encourage movie detail to reset to new movie
            setMovieDetail(null);
        }
    }

    // Full UI update with backgrounds
    protected void updateDetailUI(MovieDetail movieDetail) {
        final Context context = getContext();
        // If everything is null, cut the update short
        if (!setMovieDetail(movieDetail)) return;

        // If the UI isn't laid out yet, there is nothing to update
        if (!mLayoutInitialized) return;

        // No sense going through all the motions twice. Kills performance.
        if (!mUIInitialized) {
            mViewHolder.titleView.setText(mMovieDetail.title);
            mViewHolder.overview.setText(mMovieDetail.overview);
            mViewHolder.releaseDate.setText(context.getString(R.string.detail_release_date)
                    + mMovieDetail.releaseDate);

            // A little convoluted here to support internationalization later. Stuff the runtime
            // in minutes into a formatted string.  The placement of the number may vary in other languages.
            String runtimeText = String.format(context.getString(R.string.detail_runtime_format), mMovieDetail.runtime);
            mViewHolder.runtime.setText(runtimeText);
            mViewHolder.rating.setText(String.format(context.getString(R.string.detail_movie_user_rating),
                    mMovieDetail.voteAverage));

            Utility.updateFavoriteButton(mViewHolder.favoriteButton, mMovieDetail.getFavorite());

            String posterURL = Movie.getPosterURLFromPath(context,mMovieDetail.getPosterPath());

            if (posterURL.length() > 0) {
                final PaletteTransformation paletteTransformation = PaletteTransformation.instance();

                // Here’s an example URL: http://image.tmdb.org/t/p/w500/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg
                Picasso.with(context)
                        .load(posterURL)
                        .transform(paletteTransformation)
                                //.placeholder(R.mipmap.ic_launcher) too busy looking
                        .error(R.mipmap.ic_error_fallback)         // optional
                        .into(mViewHolder.posterView, new Callback.EmptyCallback() {
                            @Override
                            public void onSuccess() {
                                Bitmap bitmap = ((BitmapDrawable) mViewHolder.posterView.getDrawable()).getBitmap(); // Ew!
                                Palette palette = PaletteTransformation.getPalette(bitmap);

                                Log.d(TAG, "Poster loaded into detail, width: " + bitmap.getWidth() + " height " + bitmap.getHeight());

                                // Now get a matching color for the background
                                MovieDataService data = MovieDataService.getInstance();
                                data.setLightBackground(palette.getLightMutedColor(data.getLightBackground()));
                                mViewHolder.detailLayout.setBackgroundColor(data.getLightBackground());

                                data.setDarkBackground(palette.getDarkVibrantColor(data.getDarkBackground()));
                                mViewHolder.titleBackground.setBackgroundColor(data.getDarkBackground());
                                mBackgroundInitialzed = true;
                            }
                        });
            }
        }

        mUIInitialized = true;
    }


    @Override
    public void update(Observable observable, Object data) {
        // If we haven't been initialized, there is nothing to change.
        if (!mLayoutInitialized) return;

        Log.d(TAG, "Update from MovieDataService: " + data.getClass().getSimpleName());
        if (data instanceof MovieDetail) {
            if (setMovieDetail((MovieDetail) data))
                updateDetailUI(mMovieDetail);
        } else if (data instanceof Movie) {
            if (setMovie((Movie) data)) {
                updateMovieUI((Movie) data);
            }
        }
        // otherwise it is some other type of object being updated. None of our business.
    }


    // Handy dandy little class to cache the View ids so we don't keep looking for them every
    // time we refresh the UI.  We only need to fetch them after the inflate in onCreateView
    class DetailViewHolder {
        TextView titleView;
        View titleBackground;
        TextView overview;
        TextView releaseDate;
        TextView runtime;
        TextView rating;
        ImageView posterView;
        //FloatingActionButton favoriteButton;
        ImageButton favoriteButton;
        RelativeLayout detailLayout;
        int backgroundWidth;
        int backgroundHeight;
        Target backgroundTarget;

        DetailViewHolder() {
            titleView = (TextView) mRootView.findViewById(R.id.detail_movie_title);
            titleBackground = mRootView.findViewById(R.id.detail_movie_title_frame);
            overview = (TextView) mRootView.findViewById(R.id.detail_movie_overview);
            releaseDate = (TextView) mRootView.findViewById(R.id.detail_movie_release_date);
            runtime = (TextView) mRootView.findViewById(R.id.detail_movie_runtime);
            rating = (TextView) mRootView.findViewById(R.id.detail_movie_user_rating);
            posterView = (ImageView) mRootView.findViewById(R.id.detail_movie_poster);
            favoriteButton = (ImageButton) mRootView.findViewById(R.id.detail_favorite_button);
            detailLayout = (RelativeLayout) mRootView.findViewById(R.id.fragment_detail);

            // Detail movie background set up
            backgroundTarget = new Target() {
                @Override
                @SuppressWarnings("deprecation")
                @SuppressLint("NewApi")
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    // use deprecated setBackgroundDrawable for API 11 compatibility.
                    // setBackground requires 16 which is a bit too new for now

                    Drawable poster = new BitmapDrawable(mRootView.getContext().getResources(),
                            bitmap);

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        mViewHolder.detailLayout.setBackgroundDrawable(poster);
                    } else {
                        mViewHolder.detailLayout.setBackground(poster);
                    }
                }

                @Override
                public void onBitmapFailed(final Drawable errorDrawable) {
                    Log.d("TAG", "Picasso background image load failed");
                }

                @Override
                public void onPrepareLoad(final Drawable placeHolderDrawable) {
                    Log.d("TAG", "Prepare background image Load");
                }
            };

            // Get the height and width once, and only once after the fragment is laid out
            detailLayout.post(new Runnable() {
                @Override
                public void run() {
                    backgroundHeight = detailLayout.getHeight(); //height is ready
                    backgroundWidth = detailLayout.getWidth();
                    mBackgroundInitialzed = false;
                    Log.d(TAG, String.format("Background width, height from run: %d %d", backgroundWidth, backgroundHeight));

                    // update the UI now we can put the poster up with the right aspect ratio
                    updateDetailUI(mMovieDetail);
                }
            });

            // We now have a layout. It is safe to fill in the content
            mLayoutInitialized = true;
        }
    } // end DetailViewHolder


    // Make way for a new movie
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void clearUI() {
        mUIInitialized = false;
        mBackgroundInitialzed = false;
        if (mLayoutInitialized) {
            mViewHolder.titleView.setText("");
            mViewHolder.rating.setText("");
            mViewHolder.runtime.setText("");
            mViewHolder.rating.setText("");
            mViewHolder.overview.setText("");
            mViewHolder.posterView.setImageDrawable(null);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                mViewHolder.detailLayout.setBackgroundDrawable(null);
            } else {
                mViewHolder.detailLayout.setBackground(null);
            }
        }
        // Make sure these get reset in case the set to null changed them
        mUIInitialized = false;
        mBackgroundInitialzed = false;
    }

    private boolean setMovie(Movie movie) {
        boolean movieChanged = false;
        if (mMovie != null) {
            if (movie != null) {
                if (mMovie.id != movie.id) {
                    Log.d(TAG, "Movie is changing from: " + mMovie.title + "' to: " + movie.title);
                    movieChanged = true;
                } // else they are the same
            } else {
                Log.d(TAG, "Movie is changing from '" + mMovie.title + "' to null.");
                movieChanged = true;
            }
        } else {
            if (movie != null) {
                Log.d(TAG, "Movie is changing to '" + movie.title + "', old setting was null");
                movieChanged = true;
            }
        }
        if (movieChanged) {
            mMovie = movie;
            if (null != mMovie
                    && mMovieDetail != null
                    && mMovieDetail.id != mMovie.id) {
                setMovieDetail(null);
            }
            clearUI();
        }
        return movieChanged;
    }

    private boolean setMovieDetail(MovieDetail movieDetail) {
        // Sometimes we get the movie details on a silver platter,
        // sometimes we don't.  If we don't, go ask the data service for them
        MovieDataService dataService = MovieDataService.getInstance();
        if (null == dataService) {
            // bug out, we're screwed, probably out of memory
            return false;
        }
        // Is the data passed in stale? if it doesn't match the movie
        // in the current data service, switch to the current movie.
        if (movieDetail != null) {
            if (movieDetail.id == dataService.getMovieId()) {
                // the cached details are up to date, use them.
                // the movie hasn't changed, we're good to go.
                mMovieDetail = movieDetail;
                return true;
            }
        }
        Log.d(TAG, "setMovieDetail: movie details null or out of date, retrieving from MovieDataService");
        boolean haveGoodMovieDetail = false;
        movieDetail = dataService.getMovieDetail();

        if (null == movieDetail) {
            Log.d(TAG, "setMovieDetail: movie detail data is null.  Skip the update");
        } else haveGoodMovieDetail = true;

        if (null != mMovieDetail && null != movieDetail && mMovieDetail.id != movieDetail.id) {
            Log.d(TAG, "setMovieDetail: switching from '" + mMovieDetail.getTitle() + "' to " + movieDetail.getTitle());
            // We are changing movies, we need to clear out the old movie
            clearUI();
        }

        mMovieDetail = movieDetail;
        return haveGoodMovieDetail;
    }
} // end DetailFragment
