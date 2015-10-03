package com.android.bazemom.popularmovies.data;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.android.bazemom.popularmovies.movielocaldb.LocalDBContract;

public class TestMovieContract extends AndroidTestCase {

    // intentionally includes a slash to make sure Uri is getting quoted correctly
    private static final String TEST_MOVIE_ID = Integer.toString(TestUtilities.TEST_TMDB_ID);

    /*
        Test movie URI
     */
    public void testBuildMovieUri() {
        Uri movieUri = LocalDBContract.MovieEntry.buildMovieWithTmdbIdUri(TestUtilities.TEST_TMDB_ID);
        assertNotNull("Error: Null Uri returned.  You must fill-in buildMovieLocation in " +
                        "LocalDBContract.",
                movieUri);
        assertEquals("Error: Movie location not properly appended to the end of the Uri",
                TEST_MOVIE_ID, movieUri.getLastPathSegment());
        assertEquals("Error: Movie location Uri doesn't match our expected result",
                movieUri.toString(),
                "content://com.android.bazemom.popularmovies/movie/309086");
    }
}
