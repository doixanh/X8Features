package com.cyanogenmod.cmparts.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.cyanogenmod.cmparts.R;

public class DateProviderActivity extends PreferenceActivity {

    /* Date Font Color */
    private static final String UI_DATE_COLOR = "date_color";
    private Preference mDateColorPref;
    /* PLMN Lock Screen, Taskbar, and Color */
    private static final String SHOW_PLMN_LS_PREF = "show_plmn_ls";
    private static final String SHOW_PLMN_SB_PREF = "show_plmn_sb";
    private static final String UI_PLMN_LABEL_COLOR = "plmn_label_color";
    private CheckBoxPreference mShowPlmnLsPref;
    private CheckBoxPreference mShowPlmnSbPref;
    private Preference mPlmnLabelColorPref;
    /* SPN Lock Screen, Tackbar, and Color */
    private static final String SHOW_SPN_LS_PREF = "show_spn_ls";
    private static final String SHOW_SPN_SB_PREF = "show_spn_sb";
    private static final String UI_SPN_LABEL_COLOR = "spn_label_color";
    private CheckBoxPreference mShowSpnLsPref;
    private CheckBoxPreference mShowSpnSbPref;
    private Preference mSpnLabelColorPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.dp_title);
        addPreferencesFromResource(R.xml.date_provider_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        /* Date Font Color */
        mDateColorPref = prefSet.findPreference(UI_DATE_COLOR);
        /* PLMN */
        mShowPlmnLsPref = (CheckBoxPreference) prefSet.findPreference(SHOW_PLMN_LS_PREF);
        mShowPlmnSbPref = (CheckBoxPreference) prefSet.findPreference(SHOW_PLMN_SB_PREF);
        mPlmnLabelColorPref = prefSet.findPreference(UI_PLMN_LABEL_COLOR);
        /* SPN */
        mShowSpnLsPref = (CheckBoxPreference) prefSet.findPreference(SHOW_SPN_LS_PREF);
        mShowSpnSbPref = (CheckBoxPreference) prefSet.findPreference(SHOW_SPN_SB_PREF);
        mSpnLabelColorPref = prefSet.findPreference(UI_SPN_LABEL_COLOR);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        /* Date Font Color */
        if (preference == mDateColorPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mDateFontColorListener,
                readDateFontColor());
            cp.show();
        }
        /* PLMN */
        else if (preference == mShowPlmnLsPref) {
            value = mShowPlmnLsPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_PLMN_LS, value ? 1 : 0);
        }
        else if (preference == mShowPlmnSbPref) {
            value = mShowPlmnSbPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_PLMN_SB, value ? 1 : 0);
        }
        else if (preference == mPlmnLabelColorPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mPlmnLabelColorListener,
                readPlmnLabelColor());
            cp.show();
        }
        /* SPN */
        else if (preference == mShowSpnLsPref) {
            value = mShowSpnLsPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_SPN_LS, value ? 1 : 0);
        }
        else if (preference == mShowSpnSbPref) {
            value = mShowSpnSbPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_SPN_SB, value ? 1 : 0);
        }
        else if (preference == mSpnLabelColorPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mSpnLabelColorListener,
                readSpnLabelColor());
            cp.show();
        }
        return true;
    }
    /* Date Font Color */
    private int readDateFontColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.DATE_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mDateFontColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.DATE_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
    /* PLMN Font Color */
    private int readPlmnLabelColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.PLMN_LABEL_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mPlmnLabelColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.PLMN_LABEL_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
    /* PLMN Font Color */
    private int readSpnLabelColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SPN_LABEL_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mSpnLabelColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.SPN_LABEL_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
}
