package com.android.bazemom.popularmovies;

import android.support.annotation.NonNull;

import com.android.bazemom.popularmovies.moviemodel.MovieDetailModel;
import com.android.bazemom.popularmovies.moviemodel.MovieResults;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * RESTful interface to the move database using RetroFit library
 * Based on article by Josip Jurisic https://medium.com/android-news/so-retrofit-6e00670aaeb2
 */
public interface MovieDBService {
    // Request method and URL specified in the annotation
    // Callback for the parsed response is the last parameter
    //String movieSelectURL = "/movie"; //"/discover/movie"; // get pages of movie results
    enum MovieSortType {popular, upcoming, top_rated, now_playing, latest}
    //String videoRequest = "/videos";

    String KEY_PARAM = "api_key";
    String PAGE_LIMIT_PARAM = "page"; // range = 1-1,000. Let's default to 10 then allow that to change in the settings
    // Movie type: /movie/popular, /movie/upcoming, /movie/top_rated, /movie/now_playing, /movie/latest
    //final String SORT_PARAM = "sort_by";
    //final String SORT_POPULAR = .getString(R.string.movie_sort_by_popularity); //// apparently the "popular.desc" sort does not return expected results. Let's go with the tried and true capitalist method - who made the most money?
    //final String posterSizeURL = "w185";  // todo make the size a setting:  "w92", "w154", "w185", "w342", "w500", "w780", or "original"
    @GET("/movie/{movieType}")
    void getMoviesList(@Path("movieType") String sortType, @Query(PAGE_LIMIT_PARAM) int page, @Query(KEY_PARAM) String apiKey, @NonNull Callback<MovieResults> callback);
/*
    @GET("/movie/{movieType}")
    void getMoviesList(@Path("movieType") MovieSortType sortType, @Query(PAGE_LIMIT_PARAM) int page, @Query(KEY_PARAM) String apiKey, @NonNull Callback<MovieResults> callback);
*/
    @GET("/movie/{movieId}")
    void getMovieDetails(@Path("movieId") long movieId, @Query(KEY_PARAM) String apiKey, @NonNull Callback<MovieDetailModel> callback);

 /*   @GET("/movie/{movieId}")
    void getMovieTrailer(@Path("movieId") long movieId, @Path(videoRequest) String hardCodeVideoRequest, @Query(KEY_PARAM) String apiKey, @NonNull Callback<MovieVideoModel> callback); )
*/
}
