package com.android.bazemom.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.android.bazemom.popularmovies.moviemodel.DispatchTMDB;


public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

}

