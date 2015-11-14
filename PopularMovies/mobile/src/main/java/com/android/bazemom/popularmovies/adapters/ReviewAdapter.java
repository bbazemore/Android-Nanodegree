package com.android.bazemom.popularmovies.adapters;

import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.bazemom.popularmovies.R;
import com.android.bazemom.popularmovies.moviemodel.ReviewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Recycler adapter for the Review and Trailer card view tabs
 * From: http://developer.android.com/training/material/lists-cards.html#RVExamples
 */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private List<ReviewModel> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public TextView author;
        public TextView reviewContent;
        boolean isExpanded;

        public ViewHolder(View v) {
            super(v);
            author = (TextView) v.findViewById(R.id.review_author);
            reviewContent = (TextView) v.findViewById(R.id.review_text);
            isExpanded = false;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int newMaxLines;

            // Toggle back and forth
            // Expand the review text if truncated, or contract if it isn't
            if (isExpanded) {
                newMaxLines = 4;
                reviewContent.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                // Setting max value does not allow the animation to look good,
                // keep it down to a dull roar.
                newMaxLines = 1000;
                reviewContent.setEllipsize(null);
            }
            // Make a graceful transition to the new card size
            ObjectAnimator animation = ObjectAnimator.ofInt(
                    reviewContent,
                    "maxLines",
                    newMaxLines);
            animation.setDuration(2000);
            animation.start();
            isExpanded = !isExpanded;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ReviewAdapter(List<ReviewModel> myDataset) {
        if (null == myDataset)
            myDataset = new ArrayList<ReviewModel>();
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ReviewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_review, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.author.setText(mDataset.get(position).getAuthor());
        holder.reviewContent.setText(mDataset.get(position).getContent());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }


}
