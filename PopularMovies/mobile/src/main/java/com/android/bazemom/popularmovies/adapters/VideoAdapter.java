package com.android.bazemom.popularmovies.adapters;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.bazemom.popularmovies.R;
import com.android.bazemom.popularmovies.moviemodel.VideoModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Map the video / trailer data returned from TMDB into our view.
 */
public class VideoAdapter  extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {
    private List<VideoModel> mDataset;

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

            // If there is anything we need to fix up after the layout is known,
            // do it in the post-layout lambda
            cardView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("TAG", "VideoFragment post-run");
                    // update the UI now we can put the poster up with the right aspect ratio
                    //updateUI();
                }
            });
        }
        @Override
        public void onClick(View view) {
            Toast.makeText(view.getContext(), "position = " + getPosition(), Toast.LENGTH_SHORT).show();
        }
        public void setItem(VideoModel trailer) {
            trailerNameView.setText(trailer.getName());

            String youtubeTrailerId = trailer.getKey();
            String youtubeThumbnailURL = "http://img.youtube.com/vi/" + youtubeTrailerId + "/0.jpg";
            Uri youtubeURL = Uri.parse("http://www.youtube.com/watch?v=" + youtubeTrailerId);

            Picasso.with(thumbnailPlayView.getContext())
                    .load(youtubeThumbnailURL)
                            //.placeholder(R.mipmap.ic_launcher) too busy looking
                    .error(R.mipmap.ic_error_fallback)         // optional
                    .into(thumbnailPlayView);

            cardView.setTag(youtubeURL);
        }
    } // end VideoViewHolder

    //////////
    // Accept all the videos we can play
    /////////
    public VideoAdapter(List<VideoModel> myDataset) {
        if (null == mDataset)
            mDataset = new ArrayList<VideoModel>();

        // Only include Youtube videos since those are the only ones we
        // support playing
        for (VideoModel video : myDataset) {
            // why is it so awkward to get a string resource in an Adapter?
            // getString(R.string.tmdb_site_value_YouTube))
            if (video.getSite().contentEquals("YouTube")) {
                mDataset.add(video);
            }
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public VideoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_video, parent, false);
        // TODO: set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder(v);
        return vh;
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

}
