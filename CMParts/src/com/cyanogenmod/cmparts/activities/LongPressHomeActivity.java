package com.cyanogenmod.cmparts.activities;

import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.cyanogenmod.cmparts.R;
import com.cyanogenmod.cmparts.provider.SettingsProvider;

import java.util.ArrayList;

public class LongPressHomeActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String RECENT_APPS_SHOW_TITLE_PREF = "pref_show_recent_apps_title";
    private static final String RECENT_APPS_NUM_PREF= "pref_recent_apps_num";
    private static final String USE_CUSTOM_APP_PREF = "pref_use_custom_app";
    private static final String SELECT_CUSTOM_APP_PREF = "pref_select_custom_app";    
    
    private CheckBoxPreference mShowRecentAppsTitlePref;
    private ListPreference mRecentAppsNumPref;
    private CheckBoxPreference mUseCustomAppPref;
    private Preference mSelectCustomAppPref;
    
    private static final int REQUEST_PICK_SHORTCUT = 1;
    private static final int REQUEST_PICK_APPLICATION = 2;
    private static final int REQUEST_CREATE_SHORTCUT = 3;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.long_press_home_title);
        addPreferencesFromResource(R.xml.long_press_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        
        mShowRecentAppsTitlePref = (CheckBoxPreference) prefSet.findPreference(RECENT_APPS_SHOW_TITLE_PREF);        

        mRecentAppsNumPref = (ListPreference) prefSet.findPreference(RECENT_APPS_NUM_PREF);
        mRecentAppsNumPref.setOnPreferenceChangeListener(this);
        
        mUseCustomAppPref = (CheckBoxPreference) prefSet.findPreference(USE_CUSTOM_APP_PREF);        
        mSelectCustomAppPref = (Preference) prefSet.findPreference(SELECT_CUSTOM_APP_PREF);

        //final PreferenceGroup parentPreference = getPreferenceScreen();
        //parentPreference.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mUseCustomAppPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.USE_CUSTOM_APP, 0) == 1);
        mSelectCustomAppPref.setSummary(Settings.System.getString(getContentResolver(), Settings.System.SELECTED_CUSTOM_APP));
        readRecentAppsNumPreference();
    }
    
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mRecentAppsNumPref) {
            writeRecentAppsNumPreference(objValue);
        }
        
        // always let the preference setting proceed.
        return true;
    }
    
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mUseCustomAppPref) {
            Settings.System.putInt(getContentResolver(), Settings.System.USE_CUSTOM_APP, mUseCustomAppPref.isChecked() ? 1 : 0);
        }
        else if (preference == mShowRecentAppsTitlePref) {
            Settings.System.putInt(getContentResolver(), Settings.System.RECENT_APPS_SHOW_TITLE , mShowRecentAppsTitlePref.isChecked() ? 1 : 0);
        }
        else if (preference == mSelectCustomAppPref) {
            pickShortcut();
        }
        return true;
    }    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_APPLICATION:
                    completeSetCustomApp(data);
                    break;
                case REQUEST_CREATE_SHORTCUT:
                    completeSetCustomShortcut(data);
                    break;
                case REQUEST_PICK_SHORTCUT:
                    processShortcut(data, REQUEST_PICK_APPLICATION, REQUEST_CREATE_SHORTCUT);
                    break;
            }
        }
    }
    
    public void readRecentAppsNumPreference() {
        try {
            int value = Settings.System.getInt(getContentResolver(), Settings.System.RECENT_APPS_NUMBER);
            mRecentAppsNumPref.setValue(Integer.toString(value));
        } catch (SettingNotFoundException e) {
            mRecentAppsNumPref.setValue("8");
        }
    }
    
    private void writeRecentAppsNumPreference(Object objValue) {
        try {
            int val = Integer.parseInt(objValue.toString());
            Settings.System.putInt(getContentResolver(), Settings.System.RECENT_APPS_NUMBER, val);
        } catch (NumberFormatException e) {
        }
    }
    
     private void pickShortcut() {
        Bundle bundle = new Bundle();

        ArrayList<String> shortcutNames = new ArrayList<String>();
        shortcutNames.add(getString(R.string.group_applications));
        bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

        ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
        shortcutIcons.add(ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_application));
        bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

        Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
        pickIntent.putExtra(Intent.EXTRA_TITLE, getText(R.string.select_custom_app_title));
        pickIntent.putExtras(bundle);

        startActivityForResult(pickIntent, REQUEST_PICK_SHORTCUT);
    }
    
    void processShortcut(Intent intent, int requestCodeApplication, int requestCodeShortcut) {
        // Handle case where user selected "Applications"
        String applicationName = getResources().getString(R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            startActivityForResult(pickIntent, requestCodeApplication);
        } else {
            startActivityForResult(intent, requestCodeShortcut);
        }
    }
    
    void completeSetCustomShortcut(Intent data) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        
        if (Settings.System.putString(getContentResolver(), Settings.System.SELECTED_CUSTOM_APP, intent.toUri(0))) {
            mSelectCustomAppPref.setSummary(intent.toUri(0));
        }
    }
    
    void completeSetCustomApp(Intent data) {
        if (Settings.System.putString(getContentResolver(), Settings.System.SELECTED_CUSTOM_APP, data.toUri(0))) {
            mSelectCustomAppPref.setSummary(data.toUri(0));
        }        
    }    
}
