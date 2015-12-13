package com.android.bazemom.popularmovies.moviemodel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

//import javax.annotation.Generated;

//@Generated("org.jsonschema2pojo")
public class ReviewModel {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("author")
    @Expose
    private String author;
    @SerializedName("content")
    @Expose
    private String content;
    @SerializedName("url")
    @Expose
    private String url;

    /**
     *
     * @return
     * The id
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     * The id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     * The author
     */
    public String getAuthor() {
        return author;
    }

    /**
     *
     * @param author
     * The author
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     *
     * @return
     * The content
     */
    public String getContent() {
        if (null == content) return "";
        return content;
    }

    /**
     *
     * @param content
     * The content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     *
     * @return
     * The url
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     * @param url
     * The url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /* items for error handling and reporting */
    public ReviewModel() {
        id = "0";
        author = "";
        content = "";
        url = "";
    }
    public ReviewModel(ReviewModel in) {
        id = in.id;
        author = in.author;
        content = in.content;
        url = in.url;
    }
}

