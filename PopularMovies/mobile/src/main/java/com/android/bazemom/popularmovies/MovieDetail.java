package com.android.bazemom.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

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
    String posterPath;
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
        posterPath = movieInput.getPosterPath();
        backdropPath = movieInput.getBackdropPath();

        runtime = movieInput.getRuntime();
        popularity = movieInput.getPopularity();
        voteAverage = movieInput.getVoteAverage();
        voteCount = movieInput.getVoteCount();
        revenue = movieInput.getRevenue();
        favorite = 0; // not a favorite by default
    }

    private MovieDetail(Parcel in){
        title = in.readString();
        id = in.readInt();
        releaseDate = in.readString();
        overview = in.readString();
        posterPath = in.readString();
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
        parcel.writeString(posterPath);
        parcel.writeString(backdropPath);
        parcel.writeInt(runtime);
        parcel.writeDouble(popularity);
        parcel.writeDouble(voteAverage);
        parcel.writeInt(voteCount);
        parcel.writeInt(revenue);
        parcel.writeInt(favorite);
    }

    public final Parcelable.Creator<MovieDetail> CREATOR = new Parcelable.Creator<MovieDetail>() {
        @Override
        public MovieDetail createFromParcel(Parcel parcel) {
            return new MovieDetail(parcel);
        }

        @Override
        public MovieDetail[] newArray(int i) {
            return new MovieDetail[i];
        }

    };
}

