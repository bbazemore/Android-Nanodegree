package com.android.bazemom.popularmovies.movielocaldb;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
/**
 * Defines table and column names for the movie database stored on the phone / device.
 */
public class LocalDBContract {

        // The "Content authority" is a name for the entire content provider, similar to the
        // relationship between a domain name and its website.  A convenient string to use for the
        // content authority is the package name for the app, which is guaranteed to be unique on the
        // device.
        public static final String CONTENT_AUTHORITY = "com.android.bazemom.popularmovies";

        // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
        // the content provider.
        public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.android.bazemom.popularmovies/movie/ is a valid path for
        // looking at movie data. content://com.android.bazemom.popularmovies/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String PATH_MOVIE = "movie";
        public static final String PATH_TRAILER = "trailer";
        public static final String PATH_REVIEW = "review";


        /* Inner class that defines the table contents of the movie table */
        public static final class MovieEntry implements BaseColumns {

            public static final Uri CONTENT_URI =
                    BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE).build();

            public static final String CONTENT_TYPE =
                    ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE;
            public static final String CONTENT_ITEM_TYPE =
                    ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE;

            // table name
            public static final String TABLE_NAME = "movie";

            // The location setting string is what will be sent to openweathermap
            // as the location query.
            //public static final String COLUMN_MOVIE_KEY = "_ID";
            public static final String COLUMN_MOVIE_TMDB_ID = "tmdb_id";

            // Keep track of all of the things we want to display about the movie when offline
            public static final String COLUMN_MOVIE_TITLE = "title";
            public static final String COLUMN_OVERVIEW = "overview";
            public static final String COLUMN_POPULARITY = "popularity"; // rating float #
            public static final String COLUMN_VOTE_AVERAGE = "vote_average";
            public static final String COLUMN_VOTE_COUNT = "vote_count";
            public static final String COLUMN_TAGLINE = "tagline";
            public static final String COLUMN_RUNTIME = "runtime";
            public static final String COLUMN_RELEASE_DATE = "release_date";
            public static final String COLUMN_FAVORITE = "favorite";
            public static final String COLUMN_BACKDROP_PATH = "backdrop_path";
            public static final String COLUMN_POSTER_PATH = "poster_path";
            public static final String COLUMN_POSTER_LOCAL_PATH = "poster_local_path";


            // Now create shortcuts to the column indexes - these must be in the same order as the
            // create table call in LocalDBHelper
            public static final int COL_INDEX_MOVIE_TMDB_ID = 1;
            public static final int COL_INDEX_MOVIE_TITLE = 2;
            public static final int COL_INDEX_OVERVIEW = 3;
            public static final int COL_INDEX_POPULARITY = 4; // rating float #
            public static final int COL_INDEX_VOTE_AVERAGE = 5;
            public static final int COL_INDEX_VOTE_COUNT = 6;
            public static final int COL_INDEX_TAGLINE = 7;
            public static final int COL_INDEX_RUNTIME = 8;
            public static final int COL_INDEX_RELEASE_DATE = 9;
            public static final int COL_INDEX_FAVORITE = 10;
            public static final int COL_INDEX_BACKDROP_PATH = 11;
            public static final int COL_INDEX_POSTER_PATH = 12;
            public static final int COL_INDEX_POSTER_LOCAL_PATH = 13;

            public static Uri buildMovieWithTmdbIdUri(long id) {
                return ContentUris.withAppendedId(CONTENT_URI, id);
            }
            public static String getMovieTmdbIdFromUri(Uri uri) {
                return uri.getPathSegments().get(1);
            }
        }

        /* TODO - define Trailer and Review tables */
    /*
        public static final class TrailerEntry implements BaseColumns {

            public static final Uri CONTENT_URI =
                    BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRAILER).build();
   */

}
