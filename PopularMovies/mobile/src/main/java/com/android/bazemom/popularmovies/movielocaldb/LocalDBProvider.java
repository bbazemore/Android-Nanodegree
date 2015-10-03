package com.android.bazemom.popularmovies.movielocaldb;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Expose the Movie database that lives on the phone / device to 
 * anyone implementing the stan
 */
public class LocalDBProvider extends ContentProvider {
    

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private LocalDBHelper mOpenHelper;

    static final int FAVORITE_MOVIE_LIST = 100;
    static final int MOVIE_WITH_ID = 101;
    static final int REVIEW = 200;
    static final int TRAILER = 300;

    // FavoriteList - return all movies marked as favorites that were stored in the local DB
    // Note we continue to store movies even if the user un-favorites them, because hey, you
    // know the testers are going to toggle back and forth and we want it to look fast.
    private static final String sMovieFavoriteList =
            LocalDBContract.MovieEntry.TABLE_NAME+
                    "." + LocalDBContract.MovieEntry.COLUMN_FAVORITE + " != 0";

    // Point at the details for a single movie. Who cares if it is a favorite now, if we have it
    // flaunt it.
    //movie.tmdb_id= ?
    private static final String sMovieTmdbIdSelection =
            LocalDBContract.MovieEntry.TABLE_NAME+
                    "." + LocalDBContract.MovieEntry.COLUMN_MOVIE_TMDB_ID + " = ? ";


        //location.tmdb_id = ? AND trailer >= ?
      /*  private static final String sMovieWithTrailerSelection =
                LocalDBContract.MovieEntry.TABLE_NAME+
                        "." + LocalDBContract.MovieEntry.COLUMN_MOVIE_TMDB_ID + " = ? AND " +
                        LocalDBContract.TrailerEntry.COLUMN_DATE + " >= ? ";
    */


        private Cursor getMovieByTMDBId(Uri uri, String[] projection, String sortOrder) {
            String movieId = LocalDBContract.MovieEntry.getMovieTmdbIdFromUri(uri);

            String[] selectionArgs;
            String selection = sMovieTmdbIdSelection;

            selectionArgs = new String[]{movieId};

            return  mOpenHelper.getReadableDatabase().query(
                    LocalDBContract.MovieEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
        }

        private Cursor getMovieFavoriteList(
                Uri uri, String[] projection, String sortOrder) {

            String selection = sMovieFavoriteList;
            return  mOpenHelper.getReadableDatabase().query(
                    LocalDBContract.MovieEntry.TABLE_NAME,
                    projection,
                    selection,
                    null,
                    null,
                    null,
                    sortOrder
            );
        }

        /*
            This UriMatcher will
            match each URI to the FAVORITE_MOVIE_LIST, MOVIE_WITH_ID, TRAILER or REVIEW
             integer constants defined above.  You can test this through the
            testUriMatcher test within TestUriMatcher.
         */
        static UriMatcher buildUriMatcher() {
            // I know what you're thinking.  Why create a UriMatcher when you can use regular
            // expressions instead?  Because you're not crazy, that's why.

            // All paths added to the UriMatcher have a corresponding code to return when a match is
            // found.  The code passed into the constructor represents the code to return for the root
            // URI.  It's common to use NO_MATCH as the code for this case.
            final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
            final String authority = LocalDBContract.CONTENT_AUTHORITY;

            // For each type of URI you want to add, create a corresponding code.
            matcher.addURI(authority, LocalDBContract.PATH_MOVIE, FAVORITE_MOVIE_LIST);
            matcher.addURI(authority, LocalDBContract.PATH_MOVIE + "/#", MOVIE_WITH_ID);
            matcher.addURI(authority, LocalDBContract.PATH_REVIEW + "/#", REVIEW);
            matcher.addURI(authority, LocalDBContract.PATH_TRAILER + "/#", TRAILER);

            return matcher;
        }

        /*
            We just create a new LocalDBHelper for later use here.
         */
        @Override
        public boolean onCreate() {
            mOpenHelper = new LocalDBHelper(getContext());
            return true;
        }

        /*
            getType function that uses the UriMatcher.  You can
            test this by uncommenting testGetType in TestProvider.
    
         */
        @Override
        public String getType(Uri uri) {

            // Use the Uri Matcher to determine what kind of URI this is.
            final int match = sUriMatcher.match(uri);

            switch (match) {
                // Student: Uncomment and fill out these two cases
           /*     case TRAILER:
                    return LocalDBContract.TrailerEntry.CONTENT_ITEM_TYPE;
                case REVIEW:
                    return LocalDBContract.ReviewEntry.CONTENT_TYPE;
            */
                case FAVORITE_MOVIE_LIST:
                    return LocalDBContract.MovieEntry.CONTENT_TYPE;
                case MOVIE_WITH_ID:
                    return LocalDBContract.MovieEntry.CONTENT_ITEM_TYPE;
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                            String sortOrder) {
            // Here's the switch statement that, given a URI, will determine what kind of request it is,
            // and query the database accordingly.
            Cursor retCursor;
            switch (sUriMatcher.match(uri)) {

                // "trailer"
              /*  case TRAILER: */
                // Fetch all favorite movies
                case FAVORITE_MOVIE_LIST: {
                    retCursor = mOpenHelper.getReadableDatabase().query(
                            LocalDBContract.MovieEntry.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                    break;
                }
                // Fetch one movie
                case MOVIE_WITH_ID: {
                    retCursor = mOpenHelper.getReadableDatabase().query(
                            LocalDBContract.MovieEntry.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                    break;
                }

                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
            retCursor.setNotificationUri(getContext().getContentResolver(), uri);
            return retCursor;
        }

        /*
            Insert movies, trailers, or reviews into the local database
         */
        @Override
        public Uri insert(Uri uri, ContentValues values) {
            final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            final int match = sUriMatcher.match(uri);
            Uri returnUri;

            switch (match) {
    /*            case TRAILER: { */

                case MOVIE_WITH_ID: {
                    long _id = db.insert(LocalDBContract.MovieEntry.TABLE_NAME, null, values);
                    if ( _id > 0 )
                        returnUri = LocalDBContract.MovieEntry.buildMovieWithTmdbIdUri(_id);
                    else
                        throw new android.database.SQLException("Failed to insert row into " + uri);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
            getContext().getContentResolver().notifyChange(uri, null);
            return returnUri;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            final int match = sUriMatcher.match(uri);
            int rowsDeleted;
            // this makes delete all rows return the number of rows deleted
            if ( null == selection ) selection = "1";
            switch (match) {
           /*     case TRAILER:
                    */
                case MOVIE_WITH_ID:
                    rowsDeleted = db.delete(
                            LocalDBContract.MovieEntry.TABLE_NAME, selection, selectionArgs);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
            // Because a null deletes all rows
            if (rowsDeleted != 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return rowsDeleted;
        }



        @Override
        public int update(
                Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            final int match = sUriMatcher.match(uri);
            int rowsUpdated;

            switch (match) {
                case MOVIE_WITH_ID:
                    rowsUpdated = db.update(LocalDBContract.MovieEntry.TABLE_NAME, values, selection,
                            selectionArgs);
                    break;
                           /*     case TRAILER:
                    */
                default:
                    throw new UnsupportedOperationException("Unsupported uri: " + uri);
            }
            if (rowsUpdated != 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return rowsUpdated;
        }

   /*     Bulk insert for trailers and reviews
        @Override
        public int bulkInsert(Uri uri, ContentValues[] values) {
            final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case TRAILER:
                    db.beginTransaction();
                    int returnCount = 0;
                    try {
                        for (ContentValues value : values) {
                            long _id = db.insert(LocalDBContract.TrailerEntry.TABLE_NAME, null, value);
                            if (_id != -1) {
                                returnCount++;
                            }
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                    getContext().getContentResolver().notifyChange(uri, null);
                    return returnCount;
                default:
                    return super.bulkInsert(uri, values);
            }
        }
        */


    /* TODO: bulk remove all movies and locally cached images that are no longer favorites */

        // You do not need to call this method. This is a method specifically to assist the testing
        // framework in running smoothly. You can read more at:
        // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
        @Override
        @TargetApi(11)
        public void shutdown() {
            mOpenHelper.close();
            super.shutdown();
        }
    
}
