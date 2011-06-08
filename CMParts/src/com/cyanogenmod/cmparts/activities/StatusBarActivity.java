package com.cyanogenmod.cmparts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.cyanogenmod.cmparts.R;

public class StatusBarActivity extends PreferenceActivity {

    /* Display Battery Percentage */
    private static final String BATTERY_PERCENTAGE_PREF = "pref_battery_percentage";
    private CheckBoxPreference mBatteryPercentagePref;
    /* Battery Percentage Font Color */
    private static final String UI_BATTERY_PERCENT_COLOR = "battery_status_color_title";
    private Preference mBatteryPercentColorPreference;
    /* Display Clock */
    private static final String UI_SHOW_STATUS_CLOCK = "show_status_clock";
    private CheckBoxPreference mShowClockPref;
    private static final String UI_SHOW_CLOCK_AM_PM = "show_clock_am_pm";
    private CheckBoxPreference mShowAmPmPref;
    /* Clock Font Color */
    private static final String UI_CLOCK_COLOR = "clock_color";
    private Preference mClockColorPref;
    /* Display dBm Signal */
    private static final String UI_SHOW_STATUS_DBM = "show_status_dbm";
    private CheckBoxPreference mShowDbmPref;
    /* Hide Signal Strength */
    private static final String UI_HIDE_SIGNAL = "hide_signal_dbm";
    private CheckBoxPreference mHideSignalPref;
    /* dBm Font Color */
    private static final String UI_DBM_COLOR = "dbm_color";
    private Preference mDbmColorPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.status_bar_title);
        addPreferencesFromResource(R.xml.status_bar_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        /* Battery Percentage */
        mBatteryPercentagePref = (CheckBoxPreference) prefSet.findPreference(BATTERY_PERCENTAGE_PREF);
        mBatteryPercentagePref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.BATTERY_PERCENTAGE_STATUS_ICON, 0) == 1);
        /* Battery Percentage Color */
        mBatteryPercentColorPreference = prefSet.findPreference(UI_BATTERY_PERCENT_COLOR);
        /* Show Clock */
        mShowClockPref = (CheckBoxPreference) prefSet.findPreference(UI_SHOW_STATUS_CLOCK);
        mShowClockPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SHOW_STATUS_CLOCK, 1) != 0);
        mShowAmPmPref = (CheckBoxPreference) prefSet.findPreference(UI_SHOW_CLOCK_AM_PM);
        mShowAmPmPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SHOW_TWELVE_HOUR_CLOCK_PERIOD, 1) != 0);
        /* Clock Color */
        mClockColorPref = prefSet.findPreference(UI_CLOCK_COLOR);
        /* Show dBm Signal */
        mShowDbmPref = (CheckBoxPreference) prefSet.findPreference(UI_SHOW_STATUS_DBM);
        mShowDbmPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SHOW_STATUS_DBM, 0) != 0);
        /* Hide signal */
        mHideSignalPref = (CheckBoxPreference) prefSet.findPreference(UI_HIDE_SIGNAL);
        mHideSignalPref.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SHOW_STATUS_HIDE_SIGNAL, 0) != 0);

        /* dBm Signal Color */
        mDbmColorPref = prefSet.findPreference(UI_DBM_COLOR);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        /* Display Battery Percentage */
        if (preference == mBatteryPercentagePref) {
            value = mBatteryPercentagePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.BATTERY_PERCENTAGE_STATUS_ICON, value ? 1 : 0);
        }
        /* Battery Font Color */
        else if (preference == mBatteryPercentColorPreference) {
            showColorPicker(mBatteryColorHandler);
        }
        /* Display Clock */
        else if (preference == mShowClockPref) {
            value = mShowClockPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_STATUS_CLOCK, value ? 1 : 0);
        }
        else if (preference == mShowAmPmPref) {
            value = mShowAmPmPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_TWELVE_HOUR_CLOCK_PERIOD, value ? 1 : 0);
            timeUpdated();
        }
        /* Clock Font Color */
        else if (preference == mClockColorPref) {
            showColorPicker(mClockColorHandler);
        }
        /* Display dBm Signal */
        else if (preference == mShowDbmPref) {
            value = mShowDbmPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_STATUS_DBM, value ? 1 : 0);
        }
        else if (preference == mHideSignalPref) {
            value = mHideSignalPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_STATUS_HIDE_SIGNAL, value ? 1 : 0);
        }
        /* dBm Signal Font Color */
        else if (preference == mDbmColorPref) {
            showColorPicker(mDbmColorHandler);
        }
        return true;
    }

    private void showColorPicker(SettingsColorHandler handler) {
        ColorPickerDialog cp = new ColorPickerDialog(this, handler, handler.readColor());
        cp.show();
    }
    
    private void timeUpdated() {
        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        sendBroadcast(timeChanged);
    }

    /* Battery Font Color */
    SettingsColorHandler mBatteryColorHandler = new SettingsColorHandler(Settings.System.BATTERY_PERCENTAGE_STATUS_COLOR, -1);

    /* Clock Font Color */
    SettingsColorHandler mClockColorHandler = new SettingsColorHandler(Settings.System.CLOCK_COLOR, -16777216);

    /* dBm Signal Font Color */
    SettingsColorHandler mDbmColorHandler = new SettingsColorHandler(Settings.System.DBM_COLOR, -16777216);

    private class SettingsColorHandler implements ColorPickerDialog.OnColorChangedListener {

        private final String mSetting;
        private final int mDefaultColor;

        private SettingsColorHandler(String setting, int defaultColor) {
            mSetting = setting;
            mDefaultColor = defaultColor;
        }

        public void colorChanged(int color) {
            Settings.System.putInt(getContentResolver(), mSetting, color);
        }
	public void colorUpdate(int color) {
        }

        private int readColor() {
            try {
                return Settings.System.getInt(getContentResolver(), mSetting);
            }
            catch (SettingNotFoundException e) {
                return mDefaultColor;
            }
        }
    }
}
