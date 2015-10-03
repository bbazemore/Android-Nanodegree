package com.android.bazemom.popularmovies.movielocaldb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.bazemom.popularmovies.MovieDetail;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBContract.MovieEntry;

/**
 * Manages a local database for movie data.
 */
public class LocalDBHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "movie.db";
    private final static String TAG = LocalDBHelper.class.getSimpleName();

    private final static String[] MOVIE_TABLE_COLUMNS = {
            MovieEntry._ID,
            MovieEntry.COLUMN_MOVIE_TMDB_ID,
            MovieEntry.COLUMN_MOVIE_TITLE,
            MovieEntry.COLUMN_OVERVIEW,
            MovieEntry.COLUMN_POPULARITY,
            MovieEntry.COLUMN_VOTE_AVERAGE,
            MovieEntry.COLUMN_VOTE_COUNT,
            MovieEntry.COLUMN_TAGLINE,
            MovieEntry.COLUMN_RUNTIME,
            MovieEntry.COLUMN_RELEASE_DATE,
            MovieEntry.COLUMN_FAVORITE,
            MovieEntry.COLUMN_BACKDROP_PATH,
            MovieEntry.COLUMN_POSTER_PATH,
            MovieEntry.COLUMN_POSTER_LOCAL_PATH};

    static int mDatabaseExists = -1; // -1 = unknown, 0 = false, 1 = true

    public LocalDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold locations.  A location consists of the string supplied in the
        // location setting, the city name, and the latitude and longitude
        final String SQL_CREATE_MOVIE_TABLE = "CREATE TABLE " + MovieEntry.TABLE_NAME + " (" +
                MovieEntry._ID + " INTEGER PRIMARY KEY," +
                MovieEntry.COLUMN_MOVIE_TMDB_ID + " INTEGER UNIQUE NOT NULL, " +
                MovieEntry.COLUMN_MOVIE_TITLE + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_OVERVIEW + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_POPULARITY + " REAL NOT NULL, " +
                MovieEntry.COLUMN_VOTE_AVERAGE + " REAL NOT NULL, " +
                MovieEntry.COLUMN_VOTE_COUNT + " INTEGER NOT NULL, " +
                MovieEntry.COLUMN_TAGLINE + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_RUNTIME + " INTEGER NOT NULL, " +
                MovieEntry.COLUMN_RELEASE_DATE + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_FAVORITE + " INTEGER NOT NULL, " +
                MovieEntry.COLUMN_BACKDROP_PATH + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_POSTER_PATH + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_POSTER_LOCAL_PATH + " TEXT NOT NULL" +
                " );";
            /* To do trailer & review tables */

        sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database stashes the movies that the user identified as favorites, so
        // they have invested some effort into this data. Right now the upgrade policy is
        // to simply to discard the data and start over, which is not ideal.
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out Drop Table
        // statements should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME);
        //sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeatherEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    // Use this helper method to convert a MovieDetail object into the content values
    // to insert into the local db Movie Entry table.
    public static ContentValues getMovieFavoriteContentValue(MovieDetail movie) {
        ContentValues movieValues = new ContentValues();
        movieValues.put(MovieEntry.COLUMN_MOVIE_TMDB_ID, movie.getId());
        movieValues.put(MovieEntry.COLUMN_MOVIE_TITLE, movie.getTitle());
        movieValues.put(MovieEntry.COLUMN_OVERVIEW, movie.getOverview());
        movieValues.put(MovieEntry.COLUMN_POPULARITY, movie.getPopularity());
        movieValues.put(MovieEntry.COLUMN_VOTE_AVERAGE, movie.getVoteAverage());
        movieValues.put(MovieEntry.COLUMN_VOTE_COUNT, movie.getVoteCount());
        movieValues.put(MovieEntry.COLUMN_TAGLINE, movie.getTagline());
        movieValues.put(MovieEntry.COLUMN_RUNTIME, movie.getRuntime());
        movieValues.put(MovieEntry.COLUMN_RELEASE_DATE, movie.getReleaseDate());
        movieValues.put(MovieEntry.COLUMN_FAVORITE, movie.getFavorite());
        movieValues.put(MovieEntry.COLUMN_BACKDROP_PATH, movie.getBackdropPath());
        movieValues.put(MovieEntry.COLUMN_POSTER_PATH, movie.getPosterPath());
        movieValues.put(MovieEntry.COLUMN_POSTER_LOCAL_PATH, movie.getPosterLocalPath());
        return movieValues;
    }

    // Given a movieDetail object, insert or replace it into the local favorite database.
    // Return the row id, it is -1 if it did not work.
    public long updateMovieInLocalDB(MovieDetail movieDetail) {
        //ContentResolver favoriteResolver = mRootView.getContext().getContentResolver();
        SQLiteDatabase db = getWritableDatabase();

        // Add / replace this movie in the favorites database with the new values
        ContentValues movieValues = getMovieFavoriteContentValue(movieDetail);

        long movieRowId = -1;
        try {
            movieRowId = db.replace(LocalDBContract.MovieEntry.TABLE_NAME, null, movieValues);
        }
        catch (Exception e) {
            Log.d(TAG, "Exception updating movie '" + movieDetail.getTitle() + "' in the movie table. " + e.getMessage());
        }
        if (movieRowId != -1)
            mDatabaseExists =1;
        else
            Log.d(TAG, "Unable to update movie entry '" + movieDetail.getTitle() + "' in db.");

        if (null != db) {
            db.close();
        }
        return movieRowId;
    }

    // Given the TMDB id for a movie, try to find it in the local favorite database.
    // If it is there, return the MovieDetail object, otherwise, return null.
    public MovieDetail getMovieDetailFromDB(long movieId) {
        MovieDetail favoriteMovie = null;
        SQLiteDatabase db = getReadableDatabase();

        if (mDatabaseExists == -1) {
            // first time through, see if db has been created on local device
            Cursor dbCursor = db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name == '" +
                    MovieEntry.TABLE_NAME + "'", null);
            if (dbCursor.moveToFirst()) {
                int movieTableCount = dbCursor.getCount();
                if (movieTableCount > 0) {
                    // Movie table has been initialized
                    mDatabaseExists = 1;
                } else {
                    mDatabaseExists = 0; //  db isn't there yet
                }
            }
        }
        // Check if the database exists. It only gets created after we have
        // a favorite to put in it.
        if (mDatabaseExists == 1) {
            String selection =  MovieEntry.COLUMN_MOVIE_TMDB_ID + "=?";
            String[] selectTmdbIdValue = {Long.toString(movieId)};

            Cursor movieCursor = null;
            try {
                movieCursor = db.query(LocalDBContract.MovieEntry.TABLE_NAME,
                        null, // leaving "columns" null just returns all the columns.
                        selection, // cols for "where" clause
                        selectTmdbIdValue, // values for "where" clause
                        null, // columns to group by
                        null, // columns to filter by row groups
                        null); // sort order
            }
            catch (Exception e) {
                Log.d(TAG, "Exception fetching movie by id '" + selectTmdbIdValue + "' from movie table. " + e.getMessage());
            }
            if (null != movieCursor && movieCursor.moveToFirst()) {
                favoriteMovie = new MovieDetail(movieCursor);
            }
        }
        if (null != db) {
            db.close();
        }
        return favoriteMovie;
    }
}
