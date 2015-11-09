package com.android.bazemom.popularmovies;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView author;
        public TextView reviewContent;
        public ViewHolder(View v) {
            super(v);
            author = (TextView) v.findViewById(R.id.review_author);
            reviewContent = (TextView) v.findViewById(R.id.review_text);
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
        // TODO: set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder((TextView) v);
        return vh;
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
