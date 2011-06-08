package com.cyanogenmod.cmparts.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.cyanogenmod.cmparts.R;

public class NotificationsActivity extends PreferenceActivity {

    private static final String UI_NO_NOTIF_COLOR = "no_notifications_color";
    private static final String UI_LATEST_NOTIF_COLOR = "latest_notifications_color";
    private static final String UI_ONGOING_NOTIF_COLOR = "ongoing_notifications_color";
    private static final String UI_CLEAR_LABEL_COLOR = "clear_button_label_color";
    private static final String UI_BATTERY_PERCENT_COLOR = "battery_status_color_title";
    private static final String UI_NOTIF_TICKER_COLOR = "new_notifications_ticker_color";
    private static final String UI_NOTIF_COUNT_COLOR = "notifications_count_color";
    private static final String UI_NOTIF_ITEM_TITLE_COLOR = "notifications_title_color";
    private static final String UI_NOTIF_ITEM_TEXT_COLOR = "notifications_text_color";
    private static final String UI_NOTIF_ITEM_TIME_COLOR = "notifications_time_color";
    private static final String UI_NOTIF_BAR_COLOR = "not_bar_color_mask";
    private static final String UI_CUSTOM_NOT_BAR = "custom_not_bar";
    private static final String UI_CUSTOM_EXPANDED_BAR = "custom_exp_not_bar";
    private static final String UI_EXP_BAR_COLOR = "not_exp_bar_color_mask";

    private Preference mNotifTickerColor;
    private Preference mNotifCountColor;
    private Preference mNoNotifColorPref;
    private Preference mClearLabelColorPref;
    private Preference mOngoingNotifColorPref;
    private Preference mLatestNotifColorPref;
    private Preference mNotifItemTitlePref;
    private Preference mNotifItemTextPref;
    private Preference mNotifItemTimePref;
    private Preference mNotifBarColorPref;
    private Preference mExpBarColorPref;
    private CheckBoxPreference mCustomNotBar;
    private CheckBoxPreference mCustomExpBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.title_notifications_color_tweaks);
        addPreferencesFromResource(R.xml.notifications_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mNotifTickerColor = prefSet.findPreference(UI_NOTIF_TICKER_COLOR);
        mNotifCountColor = prefSet.findPreference(UI_NOTIF_COUNT_COLOR);
        mNoNotifColorPref = prefSet.findPreference(UI_NO_NOTIF_COLOR);
        mClearLabelColorPref = prefSet.findPreference(UI_CLEAR_LABEL_COLOR);
        mOngoingNotifColorPref = prefSet.findPreference(UI_ONGOING_NOTIF_COLOR);
        mLatestNotifColorPref = prefSet.findPreference(UI_LATEST_NOTIF_COLOR);
        mNotifItemTitlePref = prefSet.findPreference(UI_NOTIF_ITEM_TITLE_COLOR);
        mNotifItemTextPref = prefSet.findPreference(UI_NOTIF_ITEM_TEXT_COLOR);
        mNotifItemTimePref = prefSet.findPreference(UI_NOTIF_ITEM_TIME_COLOR);
        mNotifBarColorPref = prefSet.findPreference(UI_NOTIF_BAR_COLOR);
        mCustomNotBar = (CheckBoxPreference) prefSet.findPreference(UI_CUSTOM_NOT_BAR);
        mExpBarColorPref = prefSet.findPreference(UI_EXP_BAR_COLOR);
        mCustomExpBar = (CheckBoxPreference) prefSet.findPreference(UI_CUSTOM_EXPANDED_BAR);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mNotifTickerColor) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mNotifTickerColorListener,
                readNotifTickerColor());
            cp.show();
        }
        else if (preference == mNotifCountColor) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mNotifCountColorListener,
                readNotifCountColor());
            cp.show();
        }
        else if (preference == mNoNotifColorPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mNoNotifColorListener,
                readNoNotifColor());
            cp.show();
        }
        else if (preference == mClearLabelColorPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mClearLabelColorListener,
                readClearLabelColor());
            cp.show();
        }
        else if (preference == mOngoingNotifColorPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mOngoingNotifColorListener,
                readOngoingNotifColor());
            cp.show();
        }
        else if (preference == mLatestNotifColorPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mLatestNotifColorListener,
                readLatestNotifColor());
            cp.show();
        }
        else if (preference == mNotifItemTitlePref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mNotifItemTitleColorListener,
                readNotifItemTitleColor());
            cp.show();
        }
        else if (preference == mNotifItemTextPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mNotifItemTextColorListener,
                readNotifItemTextColor());
            cp.show();
        }
        else if (preference == mNotifItemTimePref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mNotifItemTimeColorListener,
                readNotifItemTimeColor());
            cp.show();
        }
        else if (preference == mNotifBarColorPref) {
            ColorPickerDialog cp = new ColorPickerDialog(this,
                mNotifBarColorListener,
                readNotifBarColor());
            cp.show();
        }
        else if (preference == mCustomNotBar) {
        	value = mCustomNotBar.isChecked();
        	Settings.System.putInt(getContentResolver(), 
        			Settings.System.NOTIF_BAR_CUSTOM, value ? 1 : 0);
        }
        else if (preference == mCustomExpBar) {
        	value = mCustomExpBar.isChecked();
        	Settings.System.putInt(getContentResolver(), 
        			Settings.System.NOTIF_EXPANDED_BAR_CUSTOM, value ? 1 : 0);
        }
        else if (preference == mExpBarColorPref) {
        	ColorPickerDialog cp = new ColorPickerDialog(this,
        		mExpBarColorListener,
        		readExpBarColor());
        	cp.show();
        }
        return true;
    }

    private int readNotifTickerColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.NEW_NOTIF_TICKER_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mNotifTickerColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.NEW_NOTIF_TICKER_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    private int readNotifCountColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_COUNT_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -1;
        }
    }
    ColorPickerDialog.OnColorChangedListener mNotifCountColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_COUNT_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    private int readNoNotifColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.NO_NOTIF_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -1;
        }
    }
    ColorPickerDialog.OnColorChangedListener mNoNotifColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.NO_NOTIF_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    private int readClearLabelColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.CLEAR_BUTTON_LABEL_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mClearLabelColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.CLEAR_BUTTON_LABEL_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    private int readOngoingNotifColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.ONGOING_NOTIF_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -1;
        }
    }
    ColorPickerDialog.OnColorChangedListener mOngoingNotifColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.ONGOING_NOTIF_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    private int readLatestNotifColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.LATEST_NOTIF_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -1;
        }
    }
    ColorPickerDialog.OnColorChangedListener mLatestNotifColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.LATEST_NOTIF_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    private int readNotifItemTitleColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_ITEM_TITLE_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mNotifItemTitleColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TITLE_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    private int readNotifItemTextColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_ITEM_TEXT_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mNotifItemTextColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TEXT_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    private int readNotifItemTimeColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_ITEM_TIME_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mNotifItemTimeColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TIME_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
    private int readNotifBarColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_BAR_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mNotifBarColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
            	Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_BAR_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
    private int readExpBarColor() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.NOTIF_EXPANDED_BAR_COLOR);
        }
        catch (SettingNotFoundException e) {
            return -16777216;
        }
    }
    ColorPickerDialog.OnColorChangedListener mExpBarColorListener = 
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_EXPANDED_BAR_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
}
