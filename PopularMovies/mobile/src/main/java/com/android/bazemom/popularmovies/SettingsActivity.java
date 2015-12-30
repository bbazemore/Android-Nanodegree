package com.android.bazemom.popularmovies;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity {
    private final static String TAG = SettingsActivity.class.getSimpleName();

    SettingsFragment mSettingsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the Settings fragment as the main content
        mSettingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mSettingsFragment)
                .commit();
    }

    // Using a static inner class fragment as suggested by:
    // http://codetheory.in/saving-user-settings-with-android-preferences/
    public static class SettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Make sure default values are applied.  Consider moving this to
            // a shared function that is used to retrieve the
            // SharedPreferences wherever they are needed.
            PreferenceManager.setDefaultValues(getActivity(), R.xml.pref_general, false);
            addPreferencesFromResource(R.xml.pref_general);
            //Log.d(TAG, "Preferences from pref_general added in SettingsFragment.onCreate.");

            // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
            // updated when the preference changes.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.settings_sort_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.settings_image_quality_key)));
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list (since they have separate labels/values).
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(stringValue);

                if (prefIndex >= 0) {
                    // Avoid tripping extra UI updates if nothing changed.
                    CharSequence oldValue = listPreference.getSummary();
                    CharSequence newValue = listPreference.getEntries()[prefIndex];
                    if (oldValue != null && !newValue.equals(oldValue)) {
                        preference.setSummary(newValue);
                    }
                }
            } else {
                // For other preferences, set the summary to the value's simple string representation.
                preference.setSummary(stringValue);
            }
            // Refresh UI with new preferences??
            return true;
        }

        /**
         * Attaches a listener so the summary is always updated with the preference value.
         * Also fires the listener once, to initialize the summary (so it shows up before the value
         * is changed.)
         */
        protected void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            if (null == preference) {
                Log.d(TAG, "Preference is null. Can't bind preference.");
                return;
            }
            preference.setOnPreferenceChangeListener(this);

            // Trigger the listener immediately with the preference's
            // current value.
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));

            //Log.d(TAG, "Preference set for: " + preference.getKey());
        }
    } // end SettingsFragment
}
