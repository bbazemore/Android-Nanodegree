package com.android.bazemom.popularmovies;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Courtesy https://github.com/vickychijwani/udacity-p1-p2-popular-movies/blob/project-1/app/src/main/java/me/vickychijwani/popularmovies/util/Util.java
 */
public class Utility {
    public static int getScreenWidth(@NonNull Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }
}
