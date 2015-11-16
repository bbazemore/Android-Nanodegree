package com.android.bazemom.popularmovies;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.bazemom.popularmovies.adapters.ViewPagerAdapter;


/**
 * Master-Detail view needs a fragment that contains the Tabs - that each have a fragment.
 * This is that fragment.
 * Initialize by passing a Movie.  This object will initialize the fetch of the details, reviews, and videos
 * then display them.
 */
public class TabContainerFragment extends Fragment  {
    public final static String TAG = TabContainerFragment.class.getSimpleName();

    private final int TAB_DETAIL = 0;
    private final int TAB_REVIEW = 1;
    private final int TAB_VIDEO = 2;

    // Our cache of the views & data to display
    private DetailTabViewHolder mViewHolder;
    private View mRootView;

    private Movie mMovie;

    public TabContainerFragment() {
    Log.d(TAG, "TabContainerFragment constructor");
    }
    // Master view and Detail activity can tell us to change the movie here
    public void onMovieChanged(Movie movie) {
        // TODO: if the movie is different, clear the mMovieDetail, mReviewList & mVideoList
        // and request a new set.
        // reset to tab position 1
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreateView");
        if (null != savedInstanceState) {
            // restores state from saved instance
            mMovie = savedInstanceState.getParcelable(DetailActivity.MOVIE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mRootView = inflater.inflate(R.layout.fragment_tab_container, container, false);
        Log.d(TAG, "onCreateView");
        // Get the ids of the View elements so we don't have to fetch them over and over
        mViewHolder = new DetailTabViewHolder();

        // get Movie detail from argument
        Bundle args = getArguments();
        if (args != null ) {
            mMovie = args.getParcelable(DetailActivity.EXTRA_MOVIE);
        }

        // Create a fragment to handle each tab.  Cache them away so we can poke them
        // when someone selects a tab, and when the data arrives back from the cloud.
        if (null == mViewHolder.detailFragment) {
                mViewHolder.detailFragment = new DetailFragment();
                mViewHolder.reviewFragment = new ReviewFragment();
                mViewHolder.videoFragment = new VideoFragment();

            // Give each fragment / tab the basics about the movie so they
            // have the freedom to use the title and such.
            mViewHolder.detailFragment.setArguments(args);
            mViewHolder.reviewFragment.setArguments(args);
            mViewHolder.videoFragment.setArguments(args);
        }

        // Tab layout set up
        ((AppCompatActivity)getActivity()).setSupportActionBar(mViewHolder.toolbar);
        setupViewPager(mViewHolder.viewPager);
        mViewHolder.tabLayout.setupWithViewPager(mViewHolder.viewPager);
        return mRootView;
    }

    public boolean onShareTrailer(View rootView) {
        if (null != mViewHolder.videoFragment) {
            return mViewHolder.videoFragment.onShareTrailer(mRootView);
        }
        return false;
    }


    /////////////////////////////////////////////////////////
    // Handy dandy little class to cache the View ids so we don't keep looking for them every
    // time we refresh the UI.  We only need to fetch them after the inflate in onCreateView
    class DetailTabViewHolder {
        Toolbar toolbar;
        ViewPager viewPager;
        TabLayout tabLayout;
        DetailFragment detailFragment;
        ReviewFragment reviewFragment;
        VideoFragment videoFragment;

        DetailTabViewHolder() {
            toolbar = (Toolbar) mRootView.findViewById(R.id.tabanim_toolbar);
            viewPager = (ViewPager) mRootView.findViewById(R.id.tabanim_viewpager);
            tabLayout = (TabLayout) mRootView.findViewById(R.id.tabanim_tabs);

            // Set up tab listeners
            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    updateTab(tab);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    updateTab(tab);
                }
            });  // end tab listener

            android.support.v4.app.FragmentManager fragMan = getChildFragmentManager();
            detailFragment =  (DetailFragment) fragMan.findFragmentById(R.id.fragment_detail);

            if (null != detailFragment) {
                // cache the locations of the rest of the existing fragments
                mViewHolder.reviewFragment = (ReviewFragment) fragMan.findFragmentById(R.id.fragment_review);
                mViewHolder.videoFragment = (VideoFragment) fragMan.findFragmentById(R.id.fragment_video);
            }
            tabLayout.post(new Runnable() {
                @Override
                public void run() {
                    // Anything we need to fix up in the display once it is up
                    Log.d("TAG", "DetailTabView post-run");
                }
            });
        }
    }

    private void updateTab(TabLayout.Tab tab) {
        mViewHolder.viewPager.setCurrentItem(tab.getPosition());
        switch (tab.getPosition()) {
            case TAB_DETAIL:
                Log.d(TAG, "Tab select detail");
                mViewHolder.detailFragment.updateDetailUI(null);
                break;
            case TAB_REVIEW:
                Log.d(TAG, "Tab select review");
                mViewHolder.reviewFragment.updateUI();
                break;
            case TAB_VIDEO:
                Log.d(TAG, "Tab select video");
                mViewHolder.videoFragment.updateUI();
                break;
            default:
                Log.d(TAG, "Tab select something different " + tab.getPosition());
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        // was getSupportFragmentManager
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());

        adapter.addFrag(mViewHolder.detailFragment, getString(R.string.tab_title_detail));
        adapter.addFrag(mViewHolder.reviewFragment, getString(R.string.tab_title_review));
        adapter.addFrag(mViewHolder.videoFragment, getString(R.string.tab_title_trailer));
        viewPager.setAdapter(adapter);
    }
    // If we have the basic movie information, use that to start filling in the UI
    public void updateMovieUI(Movie movie) {
        if (null == mMovie || (mMovie.id != movie.id)) {
            // Movie changed, clear out the old data
            mMovie = movie;
            if (null != mViewHolder
                    && null != mViewHolder.detailFragment) {
                try {
                    mViewHolder.detailFragment.updateMovieUI(mMovie);
                } catch (Exception e) {
                    // not a big deal
                    Log.d(TAG, "not ready for updateMovieUI: " + e.getLocalizedMessage());
                    return;
                }
                if (null != mMovie)
                    mViewHolder.toolbar.setTitle(mMovie.title);
            }
        }
    }
    public void updateDetailUI(MovieDetail movieDetail) {
        if (null != mViewHolder
                && null != mViewHolder.detailFragment) {
            try {
                mViewHolder.detailFragment.updateDetailUI(movieDetail);
            } catch (Exception e) {
                // not a big deal
                Log.d(TAG, "Not ready for updateDetailUI: " + e.getLocalizedMessage());
                return;
            }
            if (null != movieDetail)
                mViewHolder.toolbar.setTitle(movieDetail.title);
        }
    }
    public void updateReviewList() {
        if (null != mViewHolder && null != mViewHolder.reviewFragment) {
            try {
                mViewHolder.reviewFragment.updateUI();
            } catch (Exception e) {
                // not a big deal
                Log.d(TAG, "ReviewFragment not ready for update: " + e.getLocalizedMessage());
            }
        }
    }
    public void updateVideoList() {
        if (null != mViewHolder
                && null != mViewHolder.videoFragment) {
            try {
                mViewHolder.videoFragment.updateUI();
            } catch (Exception e) {
                Log.d(TAG, "videosLoaded received, but videoFragment not ready yet");
            }
        }
    }
}
