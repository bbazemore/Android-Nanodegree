package com.android.bazemom.popularmovies.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.android.bazemom.popularmovies.movielocaldb.LocalDBContract;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBHelper;

import java.lang.Exception;
import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(LocalDBHelper.DATABASE_NAME);
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }

    /*
        Note that this only tests that the Movie table has the correct columns.
        This test does not look at the data.
     */
    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(LocalDBContract.MovieEntry.TABLE_NAME);
        //tableNameHashSet.add(LocalDBContract.TrailerEntry.TABLE_NAME);
        //tableNameHashSet.add(LocalDBContract.ReviewEntry.TABLE_NAME);

        mContext.deleteDatabase(LocalDBHelper.DATABASE_NAME);
        SQLiteDatabase db = new LocalDBHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without both the location entry and weather entry tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + LocalDBContract.MovieEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(LocalDBContract.MovieEntry._ID);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_MOVIE_TITLE);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_OVERVIEW);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_RELEASE_DATE);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_POSTER_PATH);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_POPULARITY);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_VOTE_AVERAGE);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_VOTE_COUNT);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_FAVORITE);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_RUNTIME);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_BACKDROP_PATH);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_POSTER_LOCAL_PATH);
        locationColumnHashSet.add(LocalDBContract.MovieEntry.COLUMN_TAGLINE);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        db.close();
    }

    /*
        Test that we can insert and query the movie in the database. Used from multiple tests.
        Uses the ValidateCurrentRecord function from within TestUtilities.
    */
    public void testMovieTable() {
        insertMovie();
    }

    /*
        TODO: implement Trailer table test
     */
     /*public void testTrailerTable() {
        // First insert the movie, and then use the movieRowId to insert
        // the trailer. Make sure to cover as many failure cases as you can.

        // Start out with one known good movie
        long movieRowId = insertMovie();

        // Make sure we have a valid row ID.
        assertFalse("Error: Movie Not Inserted Correctly", movieRowId == -1L);

        // First step: Get reference to writable database
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Second Step (Weather): Create weather values
        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);

        // Third Step (Weather): Insert ContentValues into database and get a row ID back
        long weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue(weatherRowId != -1);

        // Fourth Step: Query the database and receive a Cursor back
        // A cursor is your primary interface to the query results.
        Cursor weatherCursor = db.query(
                WeatherContract.WeatherEntry.TABLE_NAME,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null  // sort order
        );

        // Move the cursor to the first valid database row and check to see if we have any rows
        assertTrue( "Error: No Records returned from location query", weatherCursor.moveToFirst() );

        // Fifth Step: Validate the location Query
        TestUtilities.validateCurrentRecord("testInsertReadDb weatherEntry failed to validate",
                weatherCursor, weatherValues);

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse( "Error: More than one record returned from weather query",
                weatherCursor.moveToNext() );

        // Sixth Step: Close cursor and database
        weatherCursor.close();
        dbHelper.close();
    }
    */


    /*
        Test the Movie table, or a dependence on the Movie table
     */
    public long insertMovie() {
        // First step: Get reference to writable database
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        LocalDBHelper dbHelper = new LocalDBHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Second Step: Create ContentValues of what you want to insert
        // (you can use the createNorthPoleLocationValues if you wish)
        ContentValues testValues = TestUtilities.createMovieTestValues();

        // Third Step: Insert ContentValues into database and get a row ID back
        long movieRowId = -1;

        try {
            movieRowId = db.insertOrThrow(LocalDBContract.MovieEntry.TABLE_NAME, null, testValues);
        }
        catch (Exception e)
        {
            assertFalse("Exception during insertMovie: " + e.getMessage(), true);
        }
        // Verify we got a row back.
        assertTrue(movieRowId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // Fourth Step: Query the database and receive a Cursor back
        // A cursor is your primary interface to the query results.
        Cursor cursor = db.query(
                LocalDBContract.MovieEntry.TABLE_NAME,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        // Move the cursor to a valid database row and check to see if we got any records back
        // from the query
        assertTrue( "Error: No Records returned from location query", cursor.moveToFirst() );

        // Fifth Step: Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)
        TestUtilities.validateCurrentRecord("Error: Location Query Validation Failed",
                cursor, testValues);

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse( "Error: More than one record returned from location query",
                cursor.moveToNext() );

        // Sixth Step: Close Cursor and Database
        cursor.close();
        db.close();
        return movieRowId;
    }
}
