package com.android.bazemom.popularmovies.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import com.android.bazemom.popularmovies.movielocaldb.LocalDBContract;
import com.android.bazemom.popularmovies.movielocaldb.LocalDBHelper;
import com.android.bazemom.popularmovies.utils.PollingCheck;

import java.util.Map;
import java.util.Set;

/*
    These are functions and some test data to make it easier to test the database and
    Content Provider.
 */
public class TestUtilities extends AndroidTestCase {
    static final int TEST_TMDB_ID = 309086; // Dr. Who TMDB id

    static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues) {
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateCurrentRecord(error, valueCursor, expectedValues);
        valueCursor.close();
    }

    static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);

            if (entry.getValue().getClass() == Double.class)
            {
                Double expectedValue = (Double) entry.getValue();
                Double actualValue = valueCursor.getDouble(idx);
                // doubles may be off by a bit after they go through some transforms, allow for that
                assertEquals("Double value '" + actualValue +
                        "' did not match the expected value '" + expectedValue + "'. " + error,
                        expectedValue, actualValue, 0.0000001);
            }
            else {
                String expectedValue = entry.getValue().toString();
                String actualValue = valueCursor.getString(idx);
                assertEquals("Value '" + entry.getValue().toString() +
                        "' did not match the expected value '" +
                        expectedValue + "'. " + error, expectedValue, actualValue);
            }
        }
    }

    /*
        Create a row of trailer values for the database tests.
     */
    static ContentValues createTrailerValues(long movieRowId) {
        ContentValues trailerValues = new ContentValues();
     /*   trailerValues.put(LocalDBContract.TrailerEntry.COLUMN_MOVIE_KEY, movieRowId);
        trailerValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, TEST_DATE);
        trailerValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 1.1);
*/
        return trailerValues;
    }

    /*
        Test the MovieEntry part of the LocalDBContract.
     */
    static ContentValues createMovieTestValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(LocalDBContract.MovieEntry.COLUMN_MOVIE_TMDB_ID, TEST_TMDB_ID);
        testValues.put(LocalDBContract.MovieEntry.COLUMN_MOVIE_TITLE, "Doctor Who: The Face of Evil");
        testValues.put(LocalDBContract.MovieEntry.COLUMN_OVERVIEW, "Landing on a jungle-covered planet, the Doctor discovers two tribes at war with one another, invisible monsters rampaging outside their villages and an attractive exile from one of the tribes who helps him out.");
        testValues.put(LocalDBContract.MovieEntry.COLUMN_RELEASE_DATE, "1977-01-01");
        testValues.put(LocalDBContract.MovieEntry.COLUMN_POSTER_PATH, "/uuTnBF32jSZVLAroMCI1j4PjPlP.jpg");
        testValues.put(LocalDBContract.MovieEntry.COLUMN_POPULARITY, 1.000055);
        testValues.put(LocalDBContract.MovieEntry.COLUMN_VOTE_AVERAGE, 4.0);
        testValues.put(LocalDBContract.MovieEntry.COLUMN_VOTE_COUNT, 42);
        testValues.put(LocalDBContract.MovieEntry.COLUMN_FAVORITE, 1);
        testValues.put(LocalDBContract.MovieEntry.COLUMN_RUNTIME, 120);
        testValues.put(LocalDBContract.MovieEntry.COLUMN_BACKDROP_PATH, "rHU1pOrOX3EJqfY4f11oP14rmB2.jpg");
        testValues.put(LocalDBContract.MovieEntry.COLUMN_POSTER_LOCAL_PATH, "/bogus");
        testValues.put(LocalDBContract.MovieEntry.COLUMN_TAGLINE, "");

        return testValues;
    }

    /*
        Tests MovieEntry, LocalDBContract and LocalDBHelper
     */
    static long insertTestMovieValues(Context context) {
        // insert our test records into the database
        LocalDBHelper dbHelper = new LocalDBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues testValues = TestUtilities.createMovieTestValues();

        long locationRowId;
        locationRowId = db.insert(LocalDBContract.MovieEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue("Error: Failure to insert test movie Values", locationRowId != -1);

        return locationRowId;
    }

    /*
        The functions we provide inside of TestProvider use this utility class to test
        the ContentObserver callbacks using the PollingCheck class that we grabbed from the Android
        CTS tests.

        Note that this only tests that the onChange function is called; it does not test that the
        correct Uri is returned.
     */
    static class TestContentObserver extends ContentObserver {
        final HandlerThread mHT;
        boolean mContentChanged;

        static TestContentObserver getTestContentObserver() {
            HandlerThread ht = new HandlerThread("ContentObserverThread");
            ht.start();
            return new TestContentObserver(ht);
        }

        private TestContentObserver(HandlerThread ht) {
            super(new Handler(ht.getLooper()));
            mHT = ht;
        }

        // On earlier versions of Android, this onChange method is called
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mContentChanged = true;
        }

        public void waitForNotificationOrFail() {
            // Note: The PollingCheck class is taken from the Android CTS (Compatibility Test Suite).
            // It's useful to look at the Android CTS source for ideas on how to test your Android
            // applications.  The reason that PollingCheck works is that, by default, the JUnit
            // testing framework is not running on the main Android application thread.
            new PollingCheck(5000) {
                @Override
                protected boolean check() {
                    return mContentChanged;
                }
            }.run();
            mHT.quit();
        }
    }

    static TestContentObserver getTestContentObserver() {
        return TestContentObserver.getTestContentObserver();
    }
}
