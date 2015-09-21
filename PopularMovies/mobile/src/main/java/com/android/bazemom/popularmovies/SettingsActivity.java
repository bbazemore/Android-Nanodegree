package com.android.bazemom.popularmovies;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
        * A {@link PreferenceActivity} that presents a set of application settings.
        * <p>
        * See <a href="http://developer.android.com/design/patterns/settings.html">
        * Android Design: Settings</a> for design guidelines and the <a
        * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
        * API Guide</a> for more information on developing a Settings UI.
        */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file.
        // Todo: this old style addPreferences is only needed if we are supporting API 10 or earlier.
        // Since this app claims 11 or better this should get updated to use PreferenceFragments
        // http://developer.android.com/reference/android/preference/PreferenceActivity.html
        addPreferencesFromResource(R.xml.pref_general);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
       bindPreferenceSummaryToValue(findPreference(getString(R.string.settings_sort_key)));
       bindPreferenceSummaryToValue(findPreference(getString(R.string.settings_image_quality_key)));
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
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
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
        // Refresh UI with new preferences??
        return true;
    }

}
