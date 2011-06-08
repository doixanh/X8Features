package com.cyanogenmod.cmparts.activities;

import java.io.File;
import java.util.ArrayList;

import com.cyanogenmod.cmparts.R;

import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

public class InputActivity extends PreferenceActivity implements OnPreferenceChangeListener {

	//dx
	private static final String LOCKSCREEN_CATEGORY = "lockscreen_category";
	
    private static final String LOCKSCREEN_MUSIC_CONTROLS = "lockscreen_music_controls";
    private static final String LOCKSCREEN_ALWAYS_MUSIC_CONTROLS = "lockscreen_always_music_controls";
    private static final String TRACKBALL_WAKE_PREF = "pref_trackball_wake";
    private static final String TRACKBALL_UNLOCK_PREF = "pref_trackball_unlock";
    private static final String MENU_UNLOCK_PREF = "pref_menu_unlock";
    private static final String BUTTON_CATEGORY = "pref_category_button_settings";
    private static final String LOCKSCREEN_STYLE_PREF = "pref_lockscreen_style";
    private static final String LOCKSCREEN_QUICK_UNLOCK_CONTROL = "lockscreen_quick_unlock_control";
    private static final String LOCKSCREEN_PHONE_MESSAGING_TAB = "lockscreen_phone_messaging_tab";
    private static final String LOCKSCREEN_DISABLE_UNLOCK_TAB = "lockscreen_disable_unlock_tab";
    private static final String USER_DEFINED_KEY1 = "pref_user_defined_key1";
    private static final String USER_DEFINED_KEY2 = "pref_user_defined_key2";
    private static final String USER_DEFINED_KEY3 = "pref_user_defined_key3";
    private static final String MESSAGING_TAB_APP = "pref_messaging_tab_app";

    private CheckBoxPreference mMusicControlPref;
    private CheckBoxPreference mAlwaysMusicControlPref;
    private CheckBoxPreference mTrackballWakePref;
    private CheckBoxPreference mTrackballUnlockPref;
    private CheckBoxPreference mMenuUnlockPref;
    private CheckBoxPreference mQuickUnlockScreenPref;
    private CheckBoxPreference mPhoneMessagingTabPref;
    private CheckBoxPreference mDisableUnlockTab;

    private ListPreference mLockscreenStylePref;

    private Preference mUserDefinedKey1Pref;
    private Preference mUserDefinedKey2Pref;
    private Preference mUserDefinedKey3Pref;
    private Preference mMessagingTabApp;
    private int mKeyNumber = 1;

    private static final int REQUEST_PICK_SHORTCUT = 1;
    private static final int REQUEST_PICK_APPLICATION = 2;
    private static final int REQUEST_CREATE_SHORTCUT = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.input_settings_title);
        addPreferencesFromResource(R.xml.input_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
                
        /* Music Controls */
        mMusicControlPref = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_MUSIC_CONTROLS);
        mMusicControlPref.setChecked(Settings.System.getInt(getContentResolver(), 
                Settings.System.LOCKSCREEN_MUSIC_CONTROLS, 1) == 1);

        /* Always Display Music Controls */
        mAlwaysMusicControlPref = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_ALWAYS_MUSIC_CONTROLS);
        mAlwaysMusicControlPref.setChecked(Settings.System.getInt(getContentResolver(), 
                Settings.System.LOCKSCREEN_ALWAYS_MUSIC_CONTROLS, 0) == 1);

        /* Quick Unlock Screen Control */
        mQuickUnlockScreenPref = (CheckBoxPreference)
                prefSet.findPreference(LOCKSCREEN_QUICK_UNLOCK_CONTROL);
        mQuickUnlockScreenPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0) == 1);

        /* Lockscreen Phone Messaging Tab */
        mPhoneMessagingTabPref = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_PHONE_MESSAGING_TAB);
        mPhoneMessagingTabPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_PHONE_MESSAGING_TAB, 0) == 1);

        /* Lockscreen Style */
        mLockscreenStylePref = (ListPreference) prefSet.findPreference(LOCKSCREEN_STYLE_PREF);
        int lockscreenStyle = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_STYLE_PREF, 1);
        mLockscreenStylePref.setValue(String.valueOf(lockscreenStyle));
        mLockscreenStylePref.setOnPreferenceChangeListener(this);

        // dx: remove lockscreen style, we have it in froyobread settings
        ((PreferenceCategory)prefSet.findPreference(LOCKSCREEN_CATEGORY)).removePreference(mLockscreenStylePref);
        
        if (!isDefaultLockscreenStyle()) {
            mPhoneMessagingTabPref.setEnabled(false);
            mPhoneMessagingTabPref.setChecked(false);
        } else {
            mPhoneMessagingTabPref.setEnabled(true);
        }

        /* Trackball Wake */
        mTrackballWakePref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_WAKE_PREF);
        mTrackballWakePref.setChecked(Settings.System.getInt(getContentResolver(), 
                Settings.System.TRACKBALL_WAKE_SCREEN, 0) == 1);

        /* Trackball Unlock */
        mTrackballUnlockPref = (CheckBoxPreference) prefSet.findPreference(TRACKBALL_UNLOCK_PREF);
        mTrackballUnlockPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.TRACKBALL_UNLOCK_SCREEN, 0) == 1);
        /* Menu Unlock */
        mMenuUnlockPref = (CheckBoxPreference) prefSet.findPreference(MENU_UNLOCK_PREF);
        mMenuUnlockPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.MENU_UNLOCK_SCREEN, 0) == 1);

        /* Disabling of unlock tab on lockscreen */
        mDisableUnlockTab = (CheckBoxPreference)
        prefSet.findPreference(LOCKSCREEN_DISABLE_UNLOCK_TAB);
        if (!doesUnlockAbilityExist()) {
            mDisableUnlockTab.setEnabled(false);
            mDisableUnlockTab.setChecked(false);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_GESTURES_DISABLE_UNLOCK, 0);
        } else {
            mDisableUnlockTab.setEnabled(true);
        }

        PreferenceCategory buttonCategory = (PreferenceCategory)prefSet.findPreference(BUTTON_CATEGORY);

        if (!getResources().getBoolean(R.bool.has_trackball)) {
            buttonCategory.removePreference(mTrackballWakePref);
            buttonCategory.removePreference(mTrackballUnlockPref);
        }
        mUserDefinedKey1Pref = (Preference) prefSet.findPreference(USER_DEFINED_KEY1);
        mUserDefinedKey2Pref = (Preference) prefSet.findPreference(USER_DEFINED_KEY2);
        mUserDefinedKey3Pref = (Preference) prefSet.findPreference(USER_DEFINED_KEY3);
        mMessagingTabApp = (Preference) prefSet.findPreference(MESSAGING_TAB_APP);

        if (!"vision".equals(Build.DEVICE)) {
            buttonCategory.removePreference(mUserDefinedKey1Pref);
            buttonCategory.removePreference(mUserDefinedKey2Pref);
            buttonCategory.removePreference(mUserDefinedKey3Pref);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mUserDefinedKey1Pref.setSummary(Settings.System.getString(getContentResolver(),
                Settings.System.USER_DEFINED_KEY1_APP));
        mUserDefinedKey2Pref.setSummary(Settings.System.getString(getContentResolver(),
                Settings.System.USER_DEFINED_KEY2_APP));
        mUserDefinedKey3Pref.setSummary(Settings.System.getString(getContentResolver(),
                Settings.System.USER_DEFINED_KEY3_APP));
        mMessagingTabApp.setSummary(Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_MESSAGING_TAB_APP));
        if (!doesUnlockAbilityExist()) {
            mDisableUnlockTab.setEnabled(false);
            mDisableUnlockTab.setChecked(false);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_GESTURES_DISABLE_UNLOCK, 0);
        } else {
            mDisableUnlockTab.setEnabled(true);
        }
        if (!isDefaultLockscreenStyle()) {
            mPhoneMessagingTabPref.setEnabled(false);
            mPhoneMessagingTabPref.setChecked(false);
        } else {
            mPhoneMessagingTabPref.setEnabled(true);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mMusicControlPref) {
            value = mMusicControlPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MUSIC_CONTROLS, value ? 1 : 0);
            return true;
        } else if (preference == mAlwaysMusicControlPref) {
            value = mAlwaysMusicControlPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_ALWAYS_MUSIC_CONTROLS, value ? 1 : 0);
            return true;
        } else if (preference == mQuickUnlockScreenPref) {
            value = mQuickUnlockScreenPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, value ? 1 : 0);
            return true;
        } else if (preference == mPhoneMessagingTabPref) {
            value = mPhoneMessagingTabPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_PHONE_MESSAGING_TAB, value ? 1 : 0);
            return true;
        } else if (preference == mTrackballWakePref) {
            value = mTrackballWakePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.TRACKBALL_WAKE_SCREEN, value ? 1 : 0);
            return true;
        } else if (preference == mTrackballUnlockPref) {
            value = mTrackballUnlockPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.TRACKBALL_UNLOCK_SCREEN, value ? 1 : 0);
            return true;
        } else if (preference == mMenuUnlockPref) {
            value = mMenuUnlockPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.MENU_UNLOCK_SCREEN, value ? 1 : 0);
            return true;
        } else if (preference == mDisableUnlockTab) {
            value = mDisableUnlockTab.isChecked();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_GESTURES_DISABLE_UNLOCK, value ? 1 : 0);
        } else if (preference == mUserDefinedKey1Pref) {
            pickShortcut(1);
            return true;
        } else if (preference == mUserDefinedKey2Pref) {
            pickShortcut(2);
            return true;
        } else if (preference == mUserDefinedKey3Pref) {
            pickShortcut(3);
            return true;
        } else if (preference == mMessagingTabApp) {
            pickShortcut(4);
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLockscreenStylePref) {
            int lockscreenStyle = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_STYLE_PREF,
                    lockscreenStyle);
            if (!isDefaultLockscreenStyle()) {
                mPhoneMessagingTabPref.setEnabled(false);
                mPhoneMessagingTabPref.setChecked(false);
            } else {
                mPhoneMessagingTabPref.setEnabled(true);
            }
            return true;
        }
        return false;
    }

    private void pickShortcut(int keyNumber) {
        mKeyNumber = keyNumber;
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
        int keyNumber = mKeyNumber;
        if (keyNumber == 1){
            if (Settings.System.putString(getContentResolver(), Settings.System.USER_DEFINED_KEY1_APP, intent.toUri(0))) {
                mUserDefinedKey1Pref.setSummary(intent.toUri(0));
            }
        } else if (keyNumber == 2){
            if (Settings.System.putString(getContentResolver(), Settings.System.USER_DEFINED_KEY2_APP, intent.toUri(0))) {
                mUserDefinedKey2Pref.setSummary(intent.toUri(0));
            }
        } else if (keyNumber == 3){
            if (Settings.System.putString(getContentResolver(), Settings.System.USER_DEFINED_KEY3_APP, intent.toUri(0))) {
                mUserDefinedKey3Pref.setSummary(intent.toUri(0));
            }
        } else if (keyNumber == 4){
            if (Settings.System.putString(getContentResolver(), Settings.System.LOCKSCREEN_MESSAGING_TAB_APP, intent.toUri(0))) {
                mMessagingTabApp.setSummary(intent.toUri(0));
            }
        }
    }

    void completeSetCustomApp(Intent data) {
        int keyNumber = mKeyNumber;
        if (keyNumber == 1){
            if (Settings.System.putString(getContentResolver(), Settings.System.USER_DEFINED_KEY1_APP, data.toUri(0))) {
                mUserDefinedKey1Pref.setSummary(data.toUri(0));
            }
        } else if (keyNumber == 2){
            if (Settings.System.putString(getContentResolver(), Settings.System.USER_DEFINED_KEY2_APP, data.toUri(0))) {
                mUserDefinedKey2Pref.setSummary(data.toUri(0));
            }
        } else if (keyNumber == 3){
            if (Settings.System.putString(getContentResolver(), Settings.System.USER_DEFINED_KEY3_APP, data.toUri(0))) {
                mUserDefinedKey3Pref.setSummary(data.toUri(0));
            }
        } else if (keyNumber == 4){
            if (Settings.System.putString(getContentResolver(), Settings.System.LOCKSCREEN_MESSAGING_TAB_APP, data.toUri(0))) {
                mMessagingTabApp.setSummary(data.toUri(0));
            }
        }
    }

    private boolean doesUnlockAbilityExist() {
        final File mStoreFile = new File(Environment.getDataDirectory(), "/misc/lockscreen_gestures");
        boolean GestureCanUnlock = false;
        boolean trackCanUnlock = Settings.System.getInt(getContentResolver(),
                Settings.System.TRACKBALL_UNLOCK_SCREEN, 0) == 1;
        boolean menuCanUnlock = Settings.System.getInt(getContentResolver(),
                Settings.System.MENU_UNLOCK_SCREEN, 0) == 1;
        GestureLibrary gl = GestureLibraries.fromFile(mStoreFile);
        if (gl.load()) {
            for (String name : gl.getGestureEntries()) {
                if ("UNLOCK___UNLOCK".equals(name)) {
                    GestureCanUnlock = true;
                    break;
                }
            }
        }
        if (GestureCanUnlock || trackCanUnlock || menuCanUnlock) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isDefaultLockscreenStyle() {
        int lockscreenStyle = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_STYLE_PREF, 1);
        if (lockscreenStyle == 1) {
            return true;
        } else {
            return false;
        }
    }
}
