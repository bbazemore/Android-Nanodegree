package com.android.bazemom.popularmovies.moviemodel;

//        import javax.annotation.Generated;
        import com.google.gson.annotations.Expose;

//@Generated("org.jsonschema2pojo")
public class Genre {

    @Expose
    private int id;
    @Expose
    private String name;

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

}
