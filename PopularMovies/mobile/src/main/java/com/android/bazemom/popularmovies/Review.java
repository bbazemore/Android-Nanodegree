package com.android.bazemom.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.bazemom.popularmovies.moviemodel.ReviewModel;

/**
 * Holds one Movie Review
 */
public class Review  implements Parcelable{
    final public String id;
    final public String author;
    final public String content;
    final public String url;

    public Review(ReviewModel in) {
        id = in.getId();
        author = in.getAuthor();
        content = in.getContent();
        url = in.getUrl();
    }
    public Review(Parcel in) {
        id =  in.readString();
        author = in.readString();
        content = in.readString();
        url = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(author);
        dest.writeString(content);
        dest.writeString(url);
    }
    public final Parcelable.Creator<Review> CREATOR = new Parcelable.Creator<Review>() {
        @Override
        public Review createFromParcel(Parcel parcel) {
            return new Review(parcel);
        }

        @Override
        public Review[] newArray(int i) {
            return new Review[i];
        }

    };
    @Override
    public int describeContents() {
        return 0;
    }
}
