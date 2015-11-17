package com.android.bazemom.popularmovies;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

        // Set the title frame to be partially opaque so the Favorite star will show up better
        View titleBackground = mRootView.findViewById(R.id.detail_movie_title_frame);
        Drawable background = titleBackground.getBackground();
        // 0-255, 255 is opaque, 204 = 80%
        if (null != background) {
            background.setAlpha(0xCC);
        }

        // Set up the click listeners for the Detail fragment.  Unfortunately the
        // onClick attribute in the xml can only be used if the handler is in the Activity proper,
        // not the fragment
        mViewHolder.favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickFavoriteButton(v);
            }
        });

        if (null != savedInstanceState) {
            Movie cachedMovie = savedInstanceState.getParcelable(MovieData.MOVIE);
            setMovie(cachedMovie);
            MovieDetail cachedDetail = savedInstanceState.getParcelable(MovieData.MOVIE_DETAIL);
            setMovieDetail(cachedDetail);
        }
        // Get the latest and greatest movie info, which may be more recent than what was saved.
        // Register for updates when the movie data changes
        MovieDataService dataService = MovieDataService.getInstance();
        setMovie(dataService.getMovie());
        setMovieDetail(dataService.getMovieDetail());
        // fill in the simple things
        updateMovieUI(mMovie);

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
        final Context context = getActivity().getBaseContext();
        mViewHolder.titleView.setText(movie.title);
        mViewHolder.overview.setText(movie.overview);
        mViewHolder.releaseDate.setText(context.getString(R.string.detail_release_date) + movie.releaseDate);
        mViewHolder.rating.setText(String.format(context.getString(R.string.detail_movie_user_rating), movie.voteAverage));

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

            updateFavoriteButton(mViewHolder.favoriteButton, mMovieDetail.getFavorite());

            // To build an image URL, we need 3 pieces of data. The baseurl, size and filepath.
            // First get the size from the preferences, user can select high, medium or low
            // Initialize it up here because it is needed in the general UI initialization
            // and the big background init section.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String posterSize = prefs.getString(getString(R.string.settings_image_quality_key), getString(R.string.settings_poster_quality_high));

            if (movieDetail.posterPath != null && !movieDetail.posterPath.isEmpty()) {
                String posterURL = context.getString(R.string.TMDB_image_base_url) + posterSize + mMovieDetail.posterPath;

                // Here’s an example URL: http://image.tmdb.org/t/p/w500/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg
                Picasso.with(context)
                        .load(posterURL)
                                //.placeholder(R.mipmap.ic_launcher) too busy looking
                        .error(R.mipmap.ic_error_fallback)         // optional
                        .into(mViewHolder.posterView);
            }
        } else if (mBackgroundInitialzed) {
            Log.d(TAG, "UI background for " + movieDetail.getTitle() + " already initialized. Skipping.");
            return;
        }

        // Now set the background image for the whole frame in the Detail View
        // Stolen from http://stackoverflow.com/questions/29777354/how-do-i-set-background-image-with-picasso-in-code
        // Note the image quality values are different for posters and backdrops, so fix up equivalent high, medium, and low values here.
        if (movieDetail.backdropPath != null && !mMovieDetail.backdropPath.isEmpty()
                && mViewHolder.backgroundWidth > 0) {
            int backgroundSizeId = R.string.settings_backddrop_quality_high;

            // Tine to build the image URL again. Yes, this code looks familiar.
            // We do the same look up in the basic UI fill in, but that is not usually
            // done in the same invocation of this method. If I call these outside
            // the if clauses they get called a lot more times.  Not good for performance.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String posterSize = prefs.getString(getString(R.string.settings_image_quality_key), getString(R.string.settings_poster_quality_high));

            // drop down the resolution on larger devices to keep from getting out of memory
            // sadly the devices with best resolution get the lowest quality image.
            if (posterSize.equals(getString(R.string.settings_poster_quality_medium))) {
                backgroundSizeId = R.string.settings_backdrop_quality_medium;
            } else if (posterSize.equals(getString(R.string.settings_poster_quality_low))) {
                backgroundSizeId = R.string.settings_backdrop_quality_low;
            }

            String backgroundURL = context.getString(R.string.TMDB_image_base_url);
            backgroundURL += context.getString(backgroundSizeId);
            backgroundURL += mMovieDetail.backdropPath;

            Picasso.with(getActivity()).load(backgroundURL)
                    //.memoryPolicy(MemoryPolicy.NO_CACHE) // we run out of memory on tablets
                    .resize(mViewHolder.backgroundWidth, mViewHolder.backgroundHeight)
                    .onlyScaleDown()  // the image will only be resized if it is too big
                    .centerInside()
                    .error(R.mipmap.ic_launcher)         // optional
                    .into(mViewHolder.backgroundTarget);
            // The background image is especially expensive, make sure we do it once and only once
            mBackgroundInitialzed = true;
        }
        mUIInitialized = true;
    }


    @Override
    public void update(Observable observable, Object data) {
        // If we haven't been initialized, there is nothing to change.
        if (!mLayoutInitialized) return;

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
                @SuppressLint("Deprecation")
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
                    Log.d("TAG", String.format("Background width, height from run: %d %d", backgroundWidth, backgroundHeight));

                    // update the UI now we can put the poster up with the right aspect ratio
                    updateDetailUI(mMovieDetail);
                }
            });

            // We now have a layout. It is safe to fill in the content
            mLayoutInitialized = true;
        }
    } // end DetailViewHolder


    public void onClickFavoriteButton(View view) {
        // toggle favorite on/off in database
        MovieDataService data = MovieDataService.getInstance();

        if (data.getFavorite() == 0) {
            Log.d(TAG, "buttonFavoriteClick  add Favorite");
            data.setFavorite(1);  // we may order favorites later so this is an int, for now it is just on/off

        } else {
            Log.d(TAG, "buttonFavoriteClick  remove Favorite");
            data.setFavorite(0);
        }
        updateFavoriteButton((ImageButton) view, data.getFavorite());
    }

    @SuppressLint("Deprecation")
    private void updateFavoriteButton(ImageButton favoriteButton, int favoriteValue) {
        Drawable favorite = (favoriteValue == 1) ?
                ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_on) :
                ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_off);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            favoriteButton.setBackgroundDrawable(favorite);
        } else {
            favoriteButton.setBackground(favorite);
        }
    }

    // Make way for a new movie
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
        // We are changing movies, we need to clear out the old movie
        clearUI();

        if (null == movieDetail) {
            Log.d(TAG, "setMovieDetail: movie detail data is null.  Skip the update");
        } else haveGoodMovieDetail = true;

        if (null != mMovieDetail && null != movieDetail && mMovieDetail.id != movieDetail.id) {
            Log.d(TAG, "setMovieDetail: switching from '" + mMovieDetail.getTitle() + "' to " + movieDetail.getTitle());
        }

        mMovieDetail = movieDetail;
        return haveGoodMovieDetail;
    }
} // end DetailFragment
