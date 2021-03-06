package com.android.bazemom.popularmovies.moviemodel;

import android.support.annotation.NonNull;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * RESTful interface to the move database using RetroFit library
 * Based on article by Josip Jurisic https://medium.com/android-news/so-retrofit-6e00670aaeb2
 *
 * Used to access TMDB API endpoint at https://api.themoviedb.org/3
 */
public interface MovieDBService {
    // Callback for the parsed response is the last parameter
    //String videoRequest = "/videos";  // for V2 trailers
    // http://api.themoviedb.org/3/movie/550/videos?api_key=
    // http://api.themoviedb.org/3/movie/118340/reviews?api_key=
    //

    String KEY_PARAM = "api_key";
    String PAGE_LIMIT_PARAM = "page"; // range = 1-1,000.

    // Get a list of movies of the type requested in movieType: popular, top_rated, or now_playing
    @GET("/movie/{movieType}")
    void getMoviesList(@Path("movieType") String sortType, @Query(PAGE_LIMIT_PARAM) int page, @Query(KEY_PARAM) String apiKey, @NonNull Callback<MovieResults> callback);

    // Get details for an individual movie, identified in movieId
    // TODO: get reviews & trailers in one go?: reviews might be overly long "append_to_response=reviews,trailers"
    @GET("/movie/{movieId}")
    void getMovieDetails(@Path("movieId") long movieId, @Query(KEY_PARAM) String apiKey, @NonNull Callback<MovieDetailModel> callback);

    @GET("/movie/{movieId}/reviews")
    void getMovieReviews(@Path("movieId") long movieId, @Query(PAGE_LIMIT_PARAM) int page, @Query(KEY_PARAM) String apiKey, @NonNull Callback<MovieReviewListModel> callback);

    @GET("/movie/{movieId}/videos")
    void getMovieVideos(@Path("movieId") long movieId, @Query(KEY_PARAM) String apiKey, @NonNull Callback<MovieVideoListModel> callback);

}
