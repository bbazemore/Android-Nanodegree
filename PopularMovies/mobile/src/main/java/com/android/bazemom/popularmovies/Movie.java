package com.android.bazemom.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.bazemom.popularmovies.moviemodel.MovieModel;

/*
 *  Movie holds all the data we care about for one movie.
 */
public class Movie implements Parcelable {
    String title;
    int id;
    String releaseDate;
    String overview;
    String posterPath;
    String backdropPath;
    //int runtime;

    // sort related items
    double  popularity;
    double voteAverage;
    int voteCount;
    //int revenue;
    int favorite;  // 0, not a favorite, 1 or larger leaves room for ranking

    public Movie(MovieModel movieInput)
    {
        title = movieInput.getTitle();
        id = movieInput.getId();
        releaseDate = movieInput.getReleaseDate();
        overview = movieInput.getOverview();
        posterPath = movieInput.getPosterPath();
        backdropPath = movieInput.getBackdropPath();

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
        backdropPath = in.readString();

        popularity = in.readDouble();
        voteAverage = in.readDouble();
        voteCount = in.readInt();
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
        parcel.writeString(posterPath);
        parcel.writeString(backdropPath);

        parcel.writeDouble(popularity);
        parcel.writeDouble(voteAverage);
        parcel.writeInt(voteCount);

        parcel.writeInt(favorite);
    }

    public final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
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
