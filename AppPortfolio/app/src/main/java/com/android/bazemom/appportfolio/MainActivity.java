package com.android.bazemom.appportfolio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.concurrent.ConcurrentHashMap; // thread safe
import java.lang.String;
import java.lang.Integer;


public class MainActivity extends AppCompatActivity {


protected static final ConcurrentHashMap<Integer, String> buttonToasts;
static  // Create constant message for each button on the app
{
    buttonToasts = new ConcurrentHashMap<Integer, String>();
    buttonToasts.put(R.id.buttonMovies1, "This button will launch Movies App part 1");
    buttonToasts.put(R.id.buttonMovies2, "This button will launch Movies App part 2");
    buttonToasts.put(R.id.buttonBuildBigger , "This button will launch Movies App part 2");
    buttonToasts.put(R.id.buttonCapstone , "This button will launch my capstone project");
    buttonToasts.put(R.id.buttonLibrary , "This button will launch my Library App");
    buttonToasts.put(R.id.buttonScoresApp , "This button will launch my Scores App");
    buttonToasts.put(R.id.buttonXYZReader, "This button will launch the XYZ Reader App");
}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button callbacks set up via activity_main.xml to buttonShowToast
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void buttonShowToast(View view)
    {
        CharSequence message = buttonToasts.get(view.getId());
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
