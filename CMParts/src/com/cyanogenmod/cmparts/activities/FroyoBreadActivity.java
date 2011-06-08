package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.content.Context;

import android.content.pm.IPackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

public class FroyoBreadActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	private final String TAG="froyobread";

    private static final String LOCKSCREEN_STYLE_PREF = "pref_lockscreen_style";
    private static final String LOCKSCREEN_HIDE_ARROW_PREF = "pref_lockscreen_hide_arrow";
    private static final String LOCKSCREEN_HIDE_CLOCK_PREF = "pref_lockscreen_hide_clock";
    private static final String LED_DISABLED_PREF = "pref_led_disabled";
    private static final String EDGE_GLOW_PREF = "pref_edge_glow";

    static Context mContext;

    private ListPreference mLockscreenPref;
    private CheckBoxPreference mLockscreenHideArrowPref;
    private CheckBoxPreference mLockscreenHideClockPref;
    private CheckBoxPreference mLedDisabledPref;
    private CheckBoxPreference mEdgeGlowPref;
    

		// dx : constants in Settings.java, add these to your Settings.java
        /**
         * Whether the clock in lockscreen is hidden or not
         * The value is boolean (1 or 0).
         * @hide
         */
        public static final String LOCKSCREEN_HIDE_CLOCK = "lock_screen_hide_clock";

        /**
         * Whether the arrow in lockscreen is hidden or not
         * The value is boolean (1 or 0).
         * @hide
         */
        public static final String LOCKSCREEN_HIDE_ARROW = "lock_screen_hide_arrrow";

        /**
         * Whether the edge glow is enabled or not
         * The value is boolean (1 or 0).
         * @hide
         */
        public static final String EDGE_GLOW = "edge_glow";

        /**
         * Whether the notification LED is disabled during nights or not
         * The value is boolean (1 or 0).
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_DISABLED = "notification_light_disabled";

        /**
         * Start time for LED disabled night
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_DISABLED_START = "notification_light_disabled_start";

        /**
         * End time for LED disabled night
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_DISABLED_END = "notification_light_disabled_end";
        // dx end
        
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mContext = this.getBaseContext();
        
        setTitle(R.string.froyobread_settings_title);
        addPreferencesFromResource(R.xml.froyobread_settings);
        
        PreferenceScreen prefSet = getPreferenceScreen();
        
        mLockscreenPref = (ListPreference) prefSet.findPreference(LOCKSCREEN_STYLE_PREF);
        mLockscreenPref.setValue(String.valueOf(Settings.System.getInt(mContext.getContentResolver(),
         Settings.System.LOCKSCREEN_STYLE_PREF, 3)));	
        mLockscreenPref.setOnPreferenceChangeListener(this);
        
        mLedDisabledPref = (CheckBoxPreference) prefSet.findPreference(LED_DISABLED_PREF);
        mLedDisabledPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_DISABLED, 0) == 1);
            
        mEdgeGlowPref = (CheckBoxPreference) prefSet.findPreference(EDGE_GLOW_PREF);
        mEdgeGlowPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.EDGE_GLOW, 1) == 1);

        mLockscreenHideArrowPref = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_HIDE_ARROW_PREF);
        mLockscreenHideArrowPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_HIDE_ARROW, 0) == 1);

        mLockscreenHideClockPref = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_HIDE_CLOCK_PREF);
        mLockscreenHideClockPref.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_HIDE_CLOCK, 0) == 1);
    }
        
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mLedDisabledPref) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_DISABLED, mLedDisabledPref.isChecked() ? 1 : 0);
            return true;
        }
        else if (preference == mEdgeGlowPref) {
            Settings.System.putInt(getContentResolver(), Settings.System.EDGE_GLOW, mEdgeGlowPref.isChecked() ? 1 : 0);
            return true;
        }
        else if (preference == mLockscreenHideArrowPref) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_HIDE_ARROW, mLockscreenHideArrowPref.isChecked() ? 1 : 0);
            return true;
        }
        else if (preference == mLockscreenHideClockPref) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_HIDE_CLOCK, mLockscreenHideClockPref.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
    	// we have a lock screen preference change?
        if (preference == mLockscreenPref) {
            if (newValue != null) {
				int val = Integer.parseInt(String.valueOf(newValue));
            	Settings.System.putInt(mContext.getContentResolver(), Settings.System.LOCKSCREEN_STYLE_PREF, val);
            	
            	mLockscreenPref.setValue(String.valueOf(val));
            }
        }
        return false;
    }

}
