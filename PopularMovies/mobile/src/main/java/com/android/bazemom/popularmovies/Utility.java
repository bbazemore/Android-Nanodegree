package com.android.bazemom.popularmovies;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.android.bazemom.popularmovies.moviebusevents.FavoriteChangeEvent;
import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;

import java.util.Arrays;

/**
 * Courtesy https://github.com/vickychijwani/udacity-p1-p2-popular-movies/blob/project-1/app/src/main/java/me/vickychijwani/popularmovies/util/Util.java
 */
public class Utility {
    private static final String TAG = Utility.class.getSimpleName();

    public static int getScreenWidth(@NonNull Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public static String getSortType(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getString(activity.getString(R.string.settings_sort_key),
                activity.getString(R.string.settings_sort_default));
    }
    public static void initDetailTitle(View rootView, ImageButton favoriteButton) {
        // Set the title frame to be partially opaque so the Favorite star will show up better
        View titleBackground = rootView.findViewById(R.id.detail_movie_title_frame);
        if (null != titleBackground) {
            Drawable background = titleBackground.getBackground();
            // 0-255, 255 is opaque, 204 = 80%
            if (null != background) {
                background.setAlpha(0xCC);
            }
        }

        // Set up the click listeners for the Detail fragment.  Unfortunately the
        // onClick attribute in the xml can only be used if the handler is in the Activity proper,
        // not the fragment
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.onClickFavoriteButton(v);
            }
        });
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void updateFavoriteButton(ImageButton favoriteButton, int favoriteValue) {
        try {
            Drawable favorite = (favoriteValue == 1) ?
                    ContextCompat.getDrawable(favoriteButton.getContext(), R.drawable.ic_favorite_on) :
                    ContextCompat.getDrawable(favoriteButton.getContext(), R.drawable.ic_favorite_off);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                favoriteButton.setBackgroundDrawable(favorite);
            } else {
                favoriteButton.setBackground(favorite);
            }
        } catch (Exception e) {
            Log.d(TAG, "updateFavoriteButton got an exception: " + e.getLocalizedMessage());
        }
    }

    public static void onClickFavoriteButton(View view) {
        // toggle favorite on/off in database
        MovieDataService data = MovieDataService.getInstance();
        Context context = view.getContext();
        boolean isFavorite;

        if (data.getFavorite() == 0) {
            Log.d(TAG, "buttonFavoriteClick  add Favorite");
            data.setFavorite(context, 1);  // we may order favorites later so this is an int, for now it is just on/off
            isFavorite = true;
        } else {
            Log.d(TAG, "buttonFavoriteClick  remove Favorite");
            data.setFavorite(context, 0);
            isFavorite = false;
        }
        Utility.updateFavoriteButton((ImageButton) view, data.getFavorite());
        DispatchTMDB.getInstance().shareBus().post(new FavoriteChangeEvent(isFavorite));
    }

    public static String setToolbarTitle(AppCompatActivity activity, Toolbar toolbar, String sortType) {
        // we have the sort type value - which is what we hand to the UI.
        // Convert it to the user friendly label. They don't make this easy :(
        Log.d(TAG, "Have a toolbar, we can change sort type");
        Resources resources = toolbar.getResources();
        String newTitle = "";
        String[] sortKeyStrings = resources.getStringArray(R.array.settings_sort_values);
        int index = Arrays.asList(sortKeyStrings).indexOf(sortType);
        if (index >= 0) {
            String[] sortFriendlyStrings = resources.getStringArray(R.array.settings_sort_labels);
            newTitle = sortFriendlyStrings[index];
            if (null != activity && null != activity.getSupportActionBar())
                activity.getSupportActionBar().setTitle(newTitle);
            else
                toolbar.setTitle(newTitle);
        } else {
            Log.d(TAG, "Error updateToolbarTitle for " + sortType + " index was " + index);
        }
        return newTitle;
    }
}
