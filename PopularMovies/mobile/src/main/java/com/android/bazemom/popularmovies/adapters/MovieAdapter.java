package com.android.bazemom.popularmovies.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.android.bazemom.popularmovies.Movie;
import com.android.bazemom.popularmovies.R;
import com.android.bazemom.popularmovies.moviemodel.MovieModel;
import com.android.bazemom.popularmovies.moviemodel.MovieResults;
import com.squareup.picasso.Picasso;

import java.util.List;
// Take a list of movies (Model), mash (control) the poster image for each one into an ImageGridView
// A nice clean implementation of the Model View Controller (MVC) pattern.
// I love that components exist that make this so easy.
//
public class MovieAdapter extends ArrayAdapter<Movie> {
    private static final String TAG = MovieAdapter.class.getSimpleName();
    // The movie list can be Favorite, Now_Playing, Popular, or Top_Rated
    private String mFlavor = "";

    /**
     * This is our own custom constructor (it doesn't mirror a superclass constructor).
     * The context is used to inflate the layout file, and the List is the data we want
     * to populate into the lists
     *
     * @param context   The current context. Used to inflate the layout file.
     * @param movieList A List of Movie objects to display in a list
     */
    public MovieAdapter(Activity context, String sortType, List<Movie> movieList) {
        // Here, we initialize the ArrayAdapter's internal storage for the context and the list.
        // the second argument is used when the ArrayAdapter is populating a single TextView.
        // Because this is a custom adapter for two TextViews and an ImageView, the adapter is not
        // going to use this second argument, so it can be any value. Here, we used 0.
        super(context, 0, movieList);
        mFlavor = sortType;
        Log.d(TAG, "constructor received " + movieList.size() + " movies of type " + mFlavor);
    }

    public String getFlavor() { return mFlavor; }

    public void addAll(MovieResults movieResults) {
        // because we add one page worth of data at a time, this is incremental.
        // don't clear the old data from previous pages
        for ( MovieModel movieData : movieResults.getResults())
        {
            add(new Movie(movieData));
        }
    }

    /**
     * Provides a view for an AdapterView (ListView, GridView, etc.)
     *
     * @param position    The AdapterView position that is requesting a view
     * @param convertView The recycled view to populate.
     *                    (search online for "android view recycling" to learn more)
     * @param parent      The parent ViewGroup that is used for inflation.
     * @return The View for the position in the AdapterView.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Gets the AndroidFlavor object from the ArrayAdapter at the appropriate position
        final Movie movie = getItem(position);

        // Adapters recycle views to AdapterViews.
        // If this is a new View object we're getting, then inflate the layout.
        // If not, this view already has the layout inflated from a previous call to getView,
        // and we modify the View widgets as usual.
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_movie, parent, false);
        }

        final ImageView posterView = (ImageView) convertView.findViewById(R.id.list_item_movie_imageview);

        // update the UI. The imageview may be bigger than needed and waste memory.
        // hold off on loading the image until we know how big the item in the gridview will be.
        loadPoster(getContext(), posterView, movie.getPosterUrl(getContext()));

        // Wait until the ImageView has a size before filling it in
     /*   posterView.post(new Runnable() {
            @Override
            public void run() {
                //Log.d("TAG", "MovieAdapter post-run");
                // update the UI now we can put the poster up with the right aspect ratio
                //oadPoster(getContext(), posterView, movie.getPosterUrl(getContext()));
            }});
   */
        return convertView;
    }

    private static void loadPoster(Context context, ImageView posterView, String posterURL) {
        // Here’s an example URL: http://image.tmdb.org/t/p/w500/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg
        if (!posterURL.isEmpty()) {
            Picasso.with(context)
                    .load(posterURL)
                            //.placeholder(R.mipmap.ic_launcher) too busy looking
                    .error(R.mipmap.ic_error_fallback)         // optional]
                    .fit()
                    .into(posterView);
        }
    }
}
