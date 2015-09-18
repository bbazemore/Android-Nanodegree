package com.android.bazemom.popularmovies.moviemodel;

import com.google.gson.annotations.Expose;

//@Generated("org.jsonschema2pojo")
public class ProductionCompany {

    @Expose
    private String name;
    @Expose
    private int id;

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

}
