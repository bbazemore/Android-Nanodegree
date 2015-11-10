package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;
import com.android.debug.hv.ViewServer;

// Welcome to the Main Activity for Popular Movies
// Not much happens here except:
// 1. Setting up the application-wide connection to the TMDB database
// and the event bus that is used to request Movies and send back the results
// between the background worker threads and the main UI
//
// 2. Setting up the Settings menu item
//
// 3. Launching the MainActivityFragment that contains the Movie Poster gridview
//

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private View mRootView;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // For debugging - View Hierarchy
        ViewServer.get(this).addWindow(this);

        // Set up the RESTful connection to the movie database
        // using our buddies Retrofit and Otto.
        DispatchTMDB dispatchTMDB = DispatchTMDB.getInstance();
        dispatchTMDB.shareBus().register(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Launch settings activity
            Intent settingsIntent = new Intent ( getBaseContext(), SettingsActivity.class);
            startActivity(settingsIntent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void onPause() {
        super.onPause();
        ViewServer.get(this).removeWindow(this);
    }

    public void onResume() {
        super.onResume();
        ViewServer.get(this).setFocusedWindow(this);
    }

}

