package com.android.bazemom.popularmovies.moviemodel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

// Class that holds Movie Detail
//@Generated("org.jsonschema2pojo")
public class BelongsToCollection {

    @Expose
    private int id;
    @Expose
    private String name;
    @SerializedName("poster_path")
    @Expose
    private Object posterPath;
    @SerializedName("backdrop_path")
    @Expose
    private Object backdropPath;

    /**
     *
     * @return
     * The id
     */
    public int getId() {
        return id;
    }

    /**
     *
     * @param id
     * The id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     *
     * @return
     * The name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     * The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     * The posterPath
     */
    public Object getPosterPath() {
        return posterPath;
    }

    /**
     *
     * @param posterPath
     * The poster_path
     */
    public void setPosterPath(Object posterPath) {
        this.posterPath = posterPath;
    }

    /**
     *
     * @return
     * The backdropPath
     */
    public Object getBackdropPath() {
        return backdropPath;
    }

    /**
     *
     * @param backdropPath
     * The backdrop_path
     */
    public void setBackdropPath(Object backdropPath) {
        this.backdropPath = backdropPath;
    }

}
