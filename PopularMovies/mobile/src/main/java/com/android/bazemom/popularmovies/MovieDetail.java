package com.android.bazemom.popularmovies;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import com.android.bazemom.popularmovies.movielocaldb.LocalDBContract;
import com.android.bazemom.popularmovies.moviemodel.MovieDetailModel;

/**
 * All the Movie Details we care to display for one movie
 * It is parcelable so we can pass it in an event or stash it in the saved instance if we so choose.
 */
public class MovieDetail implements  Parcelable{

    String title;
    int id;
    String releaseDate;
    String overview;
    String tagline;
    String posterPath;
    String posterLocalPath;
    String backdropPath;
    int runtime;

    // sort related items
    double  popularity;
    double voteAverage;
    int voteCount;
    int revenue;
    int favorite;  // 0, not a favorite, 1 or larger leaves room for ranking
    // /movie/{id}/reviews

    public MovieDetail(MovieDetailModel movieInput)
    {
        title = movieInput.getTitle();
        id = movieInput.getId();
        releaseDate = movieInput.getReleaseDate();
        overview = movieInput.getOverview();
        tagline = movieInput.getTagline();
        posterPath = movieInput.getPosterPath();
        posterLocalPath = "";  // not local yet
        backdropPath = movieInput.getBackdropPath();

        runtime = movieInput.getRuntime();
        popularity = movieInput.getPopularity();
        voteAverage = movieInput.getVoteAverage();
        voteCount = movieInput.getVoteCount();
        revenue = movieInput.getRevenue();
        favorite = 0; // not a favorite by default
    }

    public MovieDetail(Cursor favoriteCursor) {
        if (null != favoriteCursor) {
            title = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_MOVIE_TITLE);
            id = favoriteCursor.getInt(LocalDBContract.MovieEntry.COL_INDEX_MOVIE_TMDB_ID);
            releaseDate = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_RELEASE_DATE);
            overview = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_OVERVIEW);
            tagline = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_TAGLINE);
            posterPath = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_POSTER_PATH);
            posterLocalPath = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_POSTER_LOCAL_PATH);
            backdropPath = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_BACKDROP_PATH);

            runtime = favoriteCursor.getInt(LocalDBContract.MovieEntry.COL_INDEX_RUNTIME);
            popularity = favoriteCursor.getDouble(LocalDBContract.MovieEntry.COL_INDEX_POPULARITY);
            voteAverage = favoriteCursor.getDouble(LocalDBContract.MovieEntry.COL_INDEX_VOTE_AVERAGE);
            voteCount = favoriteCursor.getInt(LocalDBContract.MovieEntry.COL_INDEX_VOTE_COUNT);
            revenue = 0; //movieInput.getRevenue();
            favorite = favoriteCursor.getInt(LocalDBContract.MovieEntry.COL_INDEX_FAVORITE);
        }
        else
        {
            title = "*uninitialized*";  // no context to do the proper thing and use a resource string here.
        }
    }

    private MovieDetail(Parcel in){
        title = in.readString();
        id = in.readInt();
        releaseDate = in.readString();
        overview = in.readString();
        tagline = in.readString();
        posterPath = in.readString();
        posterLocalPath = in.readString();
        backdropPath = in.readString();

        runtime = in.readInt();
        popularity = in.readDouble();
        voteAverage = in.readDouble();
        voteCount = in.readInt();
        revenue = in.readInt();
        favorite =  in.readInt();
    }

    @Override
    public int describeContents() {
        // no special processing due to child classes or whatnot
        return 0;
    }

    public String toString() { return title; }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeInt(id);
        parcel.writeString(releaseDate);
        parcel.writeString(overview);
        parcel.writeString(tagline);
        parcel.writeString(posterPath);
        parcel.writeString(posterLocalPath);
        parcel.writeString(backdropPath);
        parcel.writeInt(runtime);
        parcel.writeDouble(popularity);
        parcel.writeDouble(voteAverage);
        parcel.writeInt(voteCount);
        parcel.writeInt(revenue);
        parcel.writeInt(favorite);
    }

    public static final Parcelable.Creator<MovieDetail> CREATOR = new Parcelable.Creator<MovieDetail>() {
        @Override
        public MovieDetail createFromParcel(Parcel parcel) {
            return new MovieDetail(parcel);
        }

        @Override
        public MovieDetail[] newArray(int i) {
            return new MovieDetail[i];
        }

    };

    public String getTitle() {
        return title;
    }
    public int getId() {
        return id;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getOverview() {
        return overview;
    }

    public String getTagline() {
        return tagline;
    }
    public String getPosterPath() {
        return posterPath;
    }
    public String getPosterLocalPath() {
        return posterLocalPath;
    }
    public String getBackdropPath() {
        return backdropPath;
    }

    public int getRuntime() {
        return runtime;
    }

    public double getPopularity() {
        return popularity;
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public int getRevenue() {
        return revenue;
    }

    public int getFavorite() {
        return favorite;
    }

    // PosterLocalPath and Favorite are set locally rather than fetched from the cloud
    public void setPosterLocalPath(String posterLocalPath) {
        this.posterLocalPath = posterLocalPath;
    }

    public void setFavorite(int favorite) {
        this.favorite = favorite;
    }

    // Returns the URL of the movie background image, or "" if there is no image.
    public String getBackgroundURL(Context context, int backgroundWidth) {
        String backgroundURL = "";

        // Now set the background image for the whole frame in the Detail View
        // Stolen from http://stackoverflow.com/questions/29777354/how-do-i-set-background-image-with-picasso-in-code
        // Note the image quality values are different for posters and backdrops, so fix up equivalent high, medium, and low values here.
        if (backdropPath != null && !backdropPath.isEmpty()
                && backgroundWidth > 0) {
            int backgroundSizeId = R.string.settings_backdrop_quality_high;

            // Tine to build the image URL again. Yes, this code looks familiar.
            // We do the same look up in the basic UI fill in, but that is not usually
            // done in the same invocation of this method. If I call these outside
            // the if clauses they get called a lot more times.  Not good for performance.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            String posterSize = prefs.getString(context.getString(R.string.settings_image_quality_key), context.getString(R.string.settings_poster_quality_high));

            // drop down the resolution on larger devices to keep from getting out of memory
            // sadly the devices with best resolution get the lowest quality image.
            if (posterSize.equals(context.getString(R.string.settings_poster_quality_medium))) {
                backgroundSizeId = R.string.settings_backdrop_quality_medium;
            } else if (posterSize.equals(context.getString(R.string.settings_poster_quality_low))) {
                backgroundSizeId = R.string.settings_backdrop_quality_low;
            }

            backgroundURL = context.getString(R.string.TMDB_image_base_url);
            backgroundURL += context.getString(backgroundSizeId);
            backgroundURL += backdropPath;
        }
        return backgroundURL;
    }
}

