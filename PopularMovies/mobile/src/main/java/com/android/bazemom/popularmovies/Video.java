package com.android.bazemom.popularmovies;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.bazemom.popularmovies.moviemodel.VideoModel;

/**
 * Holds one trailer video 
 *   initialize from VideoModel or parcel 
 */
public class Video implements Parcelable {
    public static final String TAG = "Video";
    public final String id;
    public final String iso6391;
    public final String key;
    public final String name;
    public final String site;
    public final Integer size;
    public final String type;
    
    public Video(VideoModel in) {
        id = in.getId();
        iso6391 = in.getIso6391();
        key = in.getKey();
        name = in.getName();
        site = in.getSite();
        size = in.getSize();
        type = in.getType();
    }
    public Video(Parcel in) {
        id =  in.readString();
        iso6391 = in.readString();
        key = in.readString();
        name = in.readString();
        site = in.readString();
        size = in.readInt();
        type = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(iso6391);
        dest.writeString(key);
        dest.writeString(name);
        dest.writeString(site);
        dest.writeInt(size);
        dest.writeString(type);
    }
    public static final Parcelable.Creator<Video> CREATOR = new Parcelable.Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel parcel) {
            return new Video(parcel);
        }

        @Override
        public Video[] newArray(int i) {
            return new Video[i];
        }

    };
    @Override
    public int describeContents() {
        return 0;
    }

}
