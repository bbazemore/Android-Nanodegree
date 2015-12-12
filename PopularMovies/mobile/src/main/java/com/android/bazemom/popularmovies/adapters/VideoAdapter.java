package com.android.bazemom.popularmovies.adapters;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.bazemom.popularmovies.MovieApplication;
import com.android.bazemom.popularmovies.R;
import com.android.bazemom.popularmovies.Video;
import com.android.bazemom.popularmovies.VideoFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Map the video / trailer data returned from TMDB into our view.
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {
    private final static String TAG = VideoAdapter.class.getSimpleName();

    // All the movie trailers fit to click
    private List<Video> mDataset;

    //////////
    // Accept all the videos we can play
    /////////
    public VideoAdapter(List<Video> myDataset) {
        if (null == mDataset)
            mDataset = new ArrayList<Video>();

        // Only include Youtube videos since those are the only ones we
        // support playing
        try {
            for (Video video : myDataset) {
                // why is it so awkward to get a string resource in an Adapter?
                // getString(R.string.tmdb_site_value_YouTube))
                if (video.site.contentEquals("YouTube")) {
                    mDataset.add(video);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception while filtering videos in VideoAdapter: " + e.getLocalizedMessage());
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public VideoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_video, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.setItem(mDataset.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        int size = 0;
        if (null != mDataset)
            size = mDataset.size();
        return size;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView trailerNameView;
        ImageView thumbnailPlayView;
        View cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            trailerNameView = (TextView) itemView.findViewById(R.id.video_name);
            thumbnailPlayView = (ImageView) itemView.findViewById(R.id.video_thumbnail);
            cardView = itemView.findViewById(R.id.video_card_item);
        }

        @Override
        public void onClick(View view) {
            VideoFragment.onClickTrailer(view);
            //Toast.makeText(view.getContext(), "position = " + getPosition(), Toast.LENGTH_SHORT).show();
        }

        public void setItem(Video trailer) {
            trailerNameView.setText(trailer.name);

            String youtubeTrailerId = trailer.key;
            String youtubeThumbnailURL = "http://img.youtube.com/vi/" + youtubeTrailerId + "/0.jpg";
            Uri youtubeURL = Uri.parse("http://www.youtube.com/watch?v=" + youtubeTrailerId);

            MovieApplication.getPicasso()
                    .load(youtubeThumbnailURL)
                            //.placeholder(R.mipmap.ic_launcher) too busy looking
                    .error(R.mipmap.ic_error_fallback)         // optional
                    .into(thumbnailPlayView);

            cardView.setTag(youtubeURL);
        }

    } // end VideoViewHolder
}
