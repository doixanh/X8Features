package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.widget.Toast;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WidgetActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    public static final String TOGGLE_WIFI_STR = "toggleWifi";
    public static final String TOGGLE_GPS_STR = "toggleGPS";
    public static final String TOGGLE_BLUETOOTH_STR = "toggleBluetooth";
    public static final String TOGGLE_BRIGHTNESS_STR = "toggleBrightness";
    public static final String TOGGLE_SOUND_STR = "toggleSound";
    public static final String TOGGLE_SYNC_STR = "toggleSync";
    public static final String TOGGLE_WIFIAP_STR = "toggleWifiAp";
    public static final String TOGGLE_SCREENTIMEOUT_STR = "toggleScreenTimeout";
    public static final String TOGGLE_MOBILEDATA_STR = "toggleMobileData";
    public static final String TOGGLE_LOCKSCREEN_STR = "toggleLockScreen";
    public static final String TOGGLE_NETWORKMODE_STR = "toggleNetworkMode";
    public static final String TOGGLE_AUTOROTATE_STR = "toggleAutoRotate";
    public static final String TOGGLE_AIRPLANE_STR = "toggleAirplane";
    public static final String TOGGLE_FLASHLIGHT_STR = "toggleFlashlight";
    public static final String TOGGLE_SLEEPMODE_STR = "toggleSleepMode";

    private static final String TOGGLE_WIFI = "toggle_wifi";
    private static final String TOGGLE_BLUETOOTH = "toggle_bluetooth";
    private static final String TOGGLE_GPS = "toggle_gps";
    private static final String TOGGLE_SOUND = "toggle_sound";
    private static final String TOGGLE_BRIGHTNESS = "toggle_brightness";
    private static final String TOGGLE_SYNC = "toggle_sync";
    private static final String TOGGLE_WIFIAP = "toggle_wifiap";
    private static final String TOGGLE_SCREENTIMEOUT = "toggle_screentimeout";
    private static final String TOGGLE_MOBILEDATA = "toggle_mobiledata";
    private static final String TOGGLE_LOCKSCREEN = "toggle_lockscreen";
    private static final String TOGGLE_NETWORKMODE = "toggle_networkmode";
    private static final String TOGGLE_AUTOROTATE = "toggle_autorotate";
    private static final String TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String TOGGLE_FLASHLIGHT = "toggle_flashlight";
    private static final String TOGGLE_SLEEPMODE = "toggle_sleep";
    private static final String EXP_BRIGHTNESS_MODE = "pref_brightness_mode";
    private static final String EXP_NETWORK_MODE = "pref_network_mode";
    private static final String EXP_SCREENTIMEOUT_MODE = "pref_screentimeout_mode";
    private static final String EXP_RING_MODE = "pref_ring_mode";
    private static final String EXP_FLASH_MODE = "pref_flash_mode";

    CheckBoxPreference mToggleWifi;
    CheckBoxPreference mToggleBluetooth;
    CheckBoxPreference mToggleGps;
    CheckBoxPreference mToggleSound;
    CheckBoxPreference mToggleBrightness;
    CheckBoxPreference mToggleSync;
    CheckBoxPreference mToggleWifiAp;
    CheckBoxPreference mToggleScreentimeout;
    CheckBoxPreference mToggleMobiledata;
    CheckBoxPreference mToggleLockscreen;
    CheckBoxPreference mToggleNetworkMode;
    CheckBoxPreference mToggleAutoRotate;
    CheckBoxPreference mToggleAirplane;
    CheckBoxPreference mToggleFlashlight;
    CheckBoxPreference mToggleSleepMode;
    ListPreference mBrightnessMode;
    ListPreference mNetworkMode;
    ListPreference mScreentimeoutMode;
    ListPreference mRingMode;
    ListPreference mFlashMode;

    private boolean isNull(String mString) {
        if (mString == null || mString.matches("null") || mString.length() == 0
                || mString.matches("|") || mString.matches("")) {
            return true;
        } else {
            return false;
        }
    }

    private List<String> getList() {
        String list = Settings.System.getString(getContentResolver(), Settings.System.WIDGET_BUTTONS);
        if(list == null) {
            list = "toggleWifi|toggleBluetooth|toggleGPS|toggleSound";
        }
        List<String> newList = new ArrayList<String>();
        String[] tempList = list.split("\\|");
        for(int i = 0; i < tempList.length; i++) {
            newList.add(tempList[i]);
        }

        return newList;
    }

    private String createString(List<String> mArray) {
        String temp = new String();
        for (String name : mArray) {
            if (isNull(name))
                continue;
            int index = mArray.indexOf(name);
            if(index == (mArray.size() - 1)) {
                temp = temp + name;
            } else {
                temp = temp + name + "|";
            }
        }
        return temp;
    }

    private boolean manageList(String toggle, boolean add) {
        List<String> sList = getList();
        if(add) {
            if(sList.size() >= 6) {
                Toast.makeText(this, R.string.widget_max_buttons, Toast.LENGTH_LONG).show();
                return false;
            }
            sList.add(toggle);
        } else {
            int index = sList.indexOf(toggle);
            sList.remove(index);
        }
        Settings.System.putString(getContentResolver(), Settings.System.WIDGET_BUTTONS, createString(sList));
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_widget_buttons);
        addPreferencesFromResource(R.xml.power_widget);

        PreferenceScreen prefSet = getPreferenceScreen();

        mToggleWifi = (CheckBoxPreference) prefSet.findPreference(TOGGLE_WIFI);
        mToggleBluetooth = (CheckBoxPreference) prefSet.findPreference(TOGGLE_BLUETOOTH);
        mToggleSync = (CheckBoxPreference) prefSet.findPreference(TOGGLE_SYNC);
        mToggleGps = (CheckBoxPreference) prefSet.findPreference(TOGGLE_GPS);
        mToggleSound = (CheckBoxPreference) prefSet.findPreference(TOGGLE_SOUND);
        mToggleBrightness = (CheckBoxPreference) prefSet.findPreference(TOGGLE_BRIGHTNESS);
        mToggleWifiAp = (CheckBoxPreference) prefSet.findPreference(TOGGLE_WIFIAP);
        mToggleScreentimeout = (CheckBoxPreference) prefSet.findPreference(TOGGLE_SCREENTIMEOUT);
        mToggleMobiledata = (CheckBoxPreference) prefSet.findPreference(TOGGLE_MOBILEDATA);
        mToggleLockscreen = (CheckBoxPreference) prefSet.findPreference(TOGGLE_LOCKSCREEN);
        mToggleNetworkMode = (CheckBoxPreference) prefSet.findPreference(TOGGLE_NETWORKMODE);
        mToggleAutoRotate = (CheckBoxPreference) prefSet.findPreference(TOGGLE_AUTOROTATE);
        mToggleAirplane = (CheckBoxPreference) prefSet.findPreference(TOGGLE_AIRPLANE);
        mToggleFlashlight = (CheckBoxPreference) prefSet.findPreference(TOGGLE_FLASHLIGHT);
        mToggleSleepMode = (CheckBoxPreference) prefSet.findPreference(TOGGLE_SLEEPMODE);

        mBrightnessMode = (ListPreference) prefSet.findPreference(EXP_BRIGHTNESS_MODE);
        mBrightnessMode.setOnPreferenceChangeListener(this);
        mNetworkMode = (ListPreference) prefSet.findPreference(EXP_NETWORK_MODE);
        mNetworkMode.setOnPreferenceChangeListener(this);
        mScreentimeoutMode = (ListPreference) prefSet.findPreference(EXP_SCREENTIMEOUT_MODE);
        mScreentimeoutMode.setOnPreferenceChangeListener(this);
        mRingMode = (ListPreference) prefSet.findPreference(EXP_RING_MODE);
        mRingMode.setOnPreferenceChangeListener(this);
        mFlashMode = (ListPreference) prefSet.findPreference(EXP_FLASH_MODE);
        mFlashMode.setOnPreferenceChangeListener(this);


        if (!getResources().getBoolean(R.bool.has_led_flash)) {
            mToggleFlashlight.setEnabled(false);
            mFlashMode.setEnabled(false);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final boolean value;
        if (preference == mToggleWifi) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_WIFI_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleBluetooth) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_BLUETOOTH_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleGps) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_GPS_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleSound) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_SOUND_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleBrightness) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_BRIGHTNESS_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleSync) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_SYNC_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleWifiAp) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_WIFIAP_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleScreentimeout) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_SCREENTIMEOUT_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleMobiledata) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_MOBILEDATA_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleLockscreen) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_LOCKSCREEN_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleNetworkMode) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_NETWORKMODE_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleAutoRotate) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_AUTOROTATE_STR, value)) {
                keyPref.setChecked(false);
            }
        } else if (preference == mToggleAirplane) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_AIRPLANE_STR, value)) {
                keyPref.setChecked(false);
            }
        }  else if (preference == mToggleFlashlight) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_FLASHLIGHT_STR, value)) {
                keyPref.setChecked(false);
            }
        }  else if (preference == mToggleSleepMode) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if(!manageList(TOGGLE_SLEEPMODE_STR, value)) {
                keyPref.setChecked(false);
            }
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.valueOf((String)newValue);
        if(preference == mBrightnessMode) {
            Settings.System.putInt(getContentResolver(), Settings.System.EXPANDED_BRIGHTNESS_MODE, value);
        } else if(preference == mNetworkMode) {
            Settings.System.putInt(getContentResolver(), Settings.System.EXPANDED_NETWORK_MODE, value);
        } else if(preference == mScreentimeoutMode) {
            Settings.System.putInt(getContentResolver(), Settings.System.EXPANDED_SCREENTIMEOUT_MODE, value);
        } else if(preference == mRingMode) {
            Settings.System.putInt(getContentResolver(), Settings.System.EXPANDED_RING_MODE, value);
        } else if(preference == mFlashMode) {
            Settings.System.putInt(getContentResolver(), Settings.System.EXPANDED_FLASH_MODE, value);
        }

        return true;
    }
}
