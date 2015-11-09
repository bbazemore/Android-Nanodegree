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

// This class is effectively nested inside the DetailActivity class
// Requires the caller to implement the MovieData interface so it can
// get the movie detail data
 public final class DetailFragment extends Fragment {
    private static final String TAG = DetailFragment.class.getSimpleName();
    private View mRootView;
    private DetailViewHolder mViewHolder;

    // private ShareActionProvider mShareActionProvider;  // V2?

    public DetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        // Get the layout adjusted to the new orientation / device
        mRootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // Get the ids of the View elements so we don't have to fetch them over and over
        mViewHolder = new DetailViewHolder();
        // slide nerd was doing mRootView.setTag(mViewHolder) - I'm just keeping it in a member variable

        // Set the title frame to be partially opaque so the star will show up better
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

        return mRootView;
    }

    public void onResume() {
        super.onResume();
        // We are back on display. Pay attention to movie results again.
        updateUI();
    }
    protected void updateUI() {
        if (mRootView == null) {
            Log.d(TAG, "updateUI no root view");
            return;
        }
        MovieData data = (MovieData) getActivity();
        MovieDetail movieDetail = data.getMovieDetail();

        if (null == movieDetail) {
            Log.d(TAG, "updateUI: movie detail data is null.  Skip the update");
            return;
        }
        final Context context = mRootView.getContext();
        mViewHolder.titleView.setText(movieDetail.title);
        mViewHolder.overview.setText(movieDetail.overview);
        mViewHolder.releaseDate.setText(context.getString(R.string.detail_release_date) + movieDetail.releaseDate);

        // A little convoluted here to support internationalization later. Stuff the runtime
        // in minutes into a formatted string.  The placement of the number may vary in other languages.
        String runtimeText = String.format(context.getString(R.string.detail_runtime_format), movieDetail.runtime);
        mViewHolder.runtime.setText(runtimeText);

        mViewHolder.rating.setText(String.format(context.getString(R.string.detail_movie_user_rating), movieDetail.voteAverage));

        updateFavoriteButton(mViewHolder.favoriteButton, movieDetail.getFavorite());

        // To build an image URL, we need 3 pieces of data. The baseurl, size and filepath.
        // First get the size from the preferences, user can select high, medium or low
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String posterSize = prefs.getString(getString(R.string.settings_image_quality_key), getString(R.string.settings_poster_quality_high));

        if (movieDetail.posterPath != null && !movieDetail.posterPath.isEmpty()) {
            String posterURL = context.getString(R.string.TMDB_image_base_url) + posterSize + movieDetail.posterPath;

            // Here’s an example URL: http://image.tmdb.org/t/p/w500/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg
            Picasso.with(context)
                    .load(posterURL)
                            //.placeholder(R.mipmap.ic_launcher) too busy looking
                    .error(R.mipmap.ic_error_fallback)         // optional
                    .into(mViewHolder.posterView);
        }

        // Now set the background image for the whole frame in the Detail View
        // Stolen from http://stackoverflow.com/questions/29777354/how-do-i-set-background-image-with-picasso-in-code
        // Note the image quality values are different for posters and backdrops, so fix up equivalent high, medium, and low values here.
        if (movieDetail.backdropPath != null && !movieDetail.backdropPath.isEmpty()
                && mViewHolder.backgroundWidth > 0) {
            int backgroundSizeId = R.string.settings_backddrop_quality_high;
            if (posterSize.equals(getString(R.string.settings_poster_quality_medium))) {
                backgroundSizeId = R.string.settings_backdrop_quality_medium;
            } else if (posterSize.equals(getString(R.string.settings_poster_quality_low))) {
                backgroundSizeId = R.string.settings_backdrop_quality_low;
            }

            String backgroundURL = context.getString(R.string.TMDB_image_base_url);
            backgroundURL += context.getString(backgroundSizeId);
            backgroundURL += movieDetail.backdropPath;


            Picasso.with(getActivity()).load(backgroundURL)
                    .resize(mViewHolder.backgroundWidth, mViewHolder.backgroundHeight)
                    .centerInside()
                    .into(mViewHolder.backgroundTarget);
        }
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
            detailLayout = (RelativeLayout) mRootView.findViewById(R.id.detail_movie_background);

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
                    Log.d("TAG", String.format("Background width, height from run: %d %d", backgroundWidth, backgroundHeight));

                    // update the UI now we can put the poster up with the right aspect ratio
                    updateUI();
                }
            });
        }
    } // end DetailViewHolder


    public void onClickFavoriteButton(View view) {
        // toggle favorite on/off in database
        MovieData data = (MovieData) getActivity();

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

} // end DetailFragment
