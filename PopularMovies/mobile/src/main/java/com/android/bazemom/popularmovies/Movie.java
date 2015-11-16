package com.android.bazemom.popularmovies;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.bazemom.popularmovies.movielocaldb.LocalDBContract;
import com.android.bazemom.popularmovies.moviemodel.MovieModel;

/*
 *  Movie holds all the data we care about for one movie.
 */
public class Movie implements Parcelable {
    public String title;
    public int id;
    public String releaseDate;
    public String overview;
    public String posterPath;
    public String posterLocalPath;  // for when we aren't connected to the network
    public String backdropPath;
    //int runtime;

    // sort related items
    public double  popularity;
    public double voteAverage;
    public int voteCount;
    //int revenue;
    public int favorite;  // 0, not a favorite, 1 or larger leaves room for ranking

    public Movie(MovieModel movieInput)
    {
        title = movieInput.getTitle();
        id = movieInput.getId();
        releaseDate = movieInput.getReleaseDate();
        overview = movieInput.getOverview();
        posterPath = movieInput.getPosterPath();
        backdropPath = movieInput.getBackdropPath();
        posterLocalPath = "";

        popularity = movieInput.getPopularity();
        voteAverage = movieInput.getVoteAverage();
        voteCount = movieInput.getVoteCount();

        favorite = 0; // not a favorite by default
    }

    private Movie(Parcel in){
        title = in.readString();
        id = in.readInt();
        releaseDate = in.readString();
        overview = in.readString();
        posterPath = in.readString();
        posterLocalPath = in.readString();
        backdropPath = in.readString();

        popularity = in.readDouble();
        voteAverage = in.readDouble();
        voteCount = in.readInt();
        favorite =  in.readInt();
    }
    // Initialize from local database
    public Movie(Cursor favoriteCursor) {
        if (null != favoriteCursor) {
            title = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_MOVIE_TITLE);
            id = favoriteCursor.getInt(LocalDBContract.MovieEntry.COL_INDEX_MOVIE_TMDB_ID);
            releaseDate = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_RELEASE_DATE);
            overview = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_OVERVIEW);
            posterPath = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_POSTER_PATH);
            posterLocalPath = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_POSTER_LOCAL_PATH);
            backdropPath = favoriteCursor.getString(LocalDBContract.MovieEntry.COL_INDEX_BACKDROP_PATH);

            popularity = favoriteCursor.getDouble(LocalDBContract.MovieEntry.COL_INDEX_POPULARITY);
            voteAverage = favoriteCursor.getDouble(LocalDBContract.MovieEntry.COL_INDEX_VOTE_AVERAGE);
            voteCount = favoriteCursor.getInt(LocalDBContract.MovieEntry.COL_INDEX_VOTE_COUNT);
            favorite = favoriteCursor.getInt(LocalDBContract.MovieEntry.COL_INDEX_FAVORITE);
        }
        else
        {
            title = "*uninitialized*";  // no context to do the proper thing and use a resource string here.
        }
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
        parcel.writeString(posterPath);
        parcel.writeString(posterLocalPath);
        parcel.writeString(backdropPath);

        parcel.writeDouble(popularity);
        parcel.writeDouble(voteAverage);
        parcel.writeInt(voteCount);

        parcel.writeInt(favorite);
    }

    public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel parcel) {
            return new Movie(parcel);
        }

        @Override
        public Movie[] newArray(int i) {
            return new Movie[i];
        }

    };
}
