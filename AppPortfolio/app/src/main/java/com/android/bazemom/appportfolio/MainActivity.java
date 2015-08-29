package com.android.bazemom.appportfolio;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.util.concurrent.ConcurrentHashMap; // thread safe
import java.lang.String;
import java.lang.Integer;


public class MainActivity extends AppCompatActivity {

    protected ConcurrentHashMap<Integer, String> mButtonToasts;
    protected void initButtonText() // Create constant message for each button on the app
    {
        Context app = getApplicationContext();
        mButtonToasts = new ConcurrentHashMap<Integer, String>();
        System.out.println("Initializing buttonToasts");
        mButtonToasts.put(R.id.buttonMovies1, app.getString(R.string.strMovies1));
        mButtonToasts.put(R.id.buttonMovies2, app.getString(R.string.strMovies2));
        mButtonToasts.put(R.id.buttonBuildBigger , app.getString(R.string.strBuildBigger));
        mButtonToasts.put(R.id.buttonCapstone , app.getString(R.string.strCapstone));
        mButtonToasts.put(R.id.buttonLibrary , app.getString(R.string.strLibrary));
        mButtonToasts.put(R.id.buttonScoresApp ,app.getString(R.string.strScoresApp));
        mButtonToasts.put(R.id.buttonXYZReader, app.getString(R.string.strXYZReader));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initButtonText();
        setContentView(R.layout.activity_main);

        // Button callbacks set up via activity_main.xml to buttonShowToast
    }

    public void buttonShowToast(View view)
    {
        CharSequence message =  mButtonToasts.get(view.getId());
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
