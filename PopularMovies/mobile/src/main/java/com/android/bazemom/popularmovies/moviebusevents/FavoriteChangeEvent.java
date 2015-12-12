package com.android.bazemom.popularmovies.moviebusevents;

import android.util.Log;

/**
 * Communicate across the app when the favorite setting changes for a movie
 * If added is true, then a favorite was added.
 * If it is false, a favorite was removed.
 */
public class FavoriteChangeEvent {
    private final static String TAG = FavoriteChangeEvent.class.getSimpleName();

    public boolean favoriteAdded;

    public FavoriteChangeEvent(boolean added) {
        Log.i(TAG, "FavoriteChangeEvent, favorite added? " + added);
        favoriteAdded = added;
    }
}
