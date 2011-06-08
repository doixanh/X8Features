package com.cyanogenmod.cmparts.activities;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import java.util.List;
import android.widget.Toast;
import com.cyanogenmod.cmparts.R;


public class HapticTweaksActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "HapticTweaks";

    private static final String HAPTIC_FEEDBACK_PREF = "haptic_feedback";
    private static final String HAPTIC_FEEDBACK_UP_PREF = "haptic_feedback_up";
    private static final String HAPTIC_FEEDBACK_ALL_PREF = "haptic_feedback_all";
    private static final String HAPTIC_FEEDBACK_DOWN_VAL_PREF = "haptic_down_decider";
    private static final String HAPTIC_FEEDBACK_UP_VAL_PREF = "haptic_up_decider";
    private static final String HAPTIC_FEEDBACK_LONG_VAL_PREF = "haptic_long_decider";
    private static final String HAPTIC_FEEDBACK_TAP_VAL_PREF = "haptic_tap_decider";

    private Preference mHapticDownSetter;
    private Preference mHapticUpSetter;
    private Preference mHapticLongSetter;
    private Preference mHapticTapSetter;

    private CheckBoxPreference mHapticFeedbackPref;
    private CheckBoxPreference mHapticFeedbackUpPref;
    private CheckBoxPreference mHapticFeedbackAllPref;

    private static final int HAPTIC_DOWN_INTENT = 1;
    private static final int HAPTIC_UP_INTENT = 2;
    private static final int HAPTIC_LONG_INTENT = 3;
    private static final int HAPTIC_TAP_INTENT = 4;

    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {

        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {

                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));

                    return true;
                }
            }
        }
        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);
        return true;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.haptic_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        mHapticFeedbackPref = (CheckBoxPreference) prefSet.findPreference(HAPTIC_FEEDBACK_PREF);
        mHapticFeedbackUpPref = (CheckBoxPreference) prefSet.findPreference(HAPTIC_FEEDBACK_UP_PREF);
        mHapticFeedbackAllPref = (CheckBoxPreference) prefSet.findPreference(HAPTIC_FEEDBACK_ALL_PREF);
        mHapticDownSetter = prefSet.findPreference(HAPTIC_FEEDBACK_DOWN_VAL_PREF);
        mHapticUpSetter = prefSet.findPreference(HAPTIC_FEEDBACK_UP_VAL_PREF);
        mHapticLongSetter = prefSet.findPreference(HAPTIC_FEEDBACK_LONG_VAL_PREF);
        mHapticTapSetter = prefSet.findPreference(HAPTIC_FEEDBACK_TAP_VAL_PREF);
        
//        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        final PreferenceGroup parentPreference = getPreferenceScreen();
        parentPreference.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        
    }

    private void updateToggles() {
            mHapticFeedbackPref.setChecked(Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0);
            mHapticFeedbackUpPref.setChecked(Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_UP_ENABLED, 0) != 0);
            mHapticFeedbackAllPref.setChecked(Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ALL_ENABLED, 0) != 0);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        // always let the preference setting proceed.
        return true;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mHapticDownSetter) {
            String starting_string = Settings.System.getString(getContentResolver(), Settings.System.HAPTIC_DOWN_ARRAY);
            Intent hapticIntent = new Intent();
            hapticIntent.setClass(HapticTweaksActivity.this, HapticAdjust.class);
            Bundle bundle = new Bundle();
            bundle.putString("start_string", starting_string);
            bundle.putInt("hap_type", HAPTIC_DOWN_INTENT);
            hapticIntent.putExtras(bundle);
            startActivityForResult(hapticIntent,HAPTIC_DOWN_INTENT);
            return true;
        }
        else if (preference == mHapticUpSetter) {
            String starting_string = Settings.System.getString(getContentResolver(), Settings.System.HAPTIC_UP_ARRAY);
            Intent hapticIntent = new Intent();
            hapticIntent.setClass(HapticTweaksActivity.this, HapticAdjust.class);
            Bundle bundle = new Bundle();
            bundle.putString("start_string", starting_string);
            bundle.putInt("hap_type", HAPTIC_UP_INTENT);
            hapticIntent.putExtras(bundle);
            startActivityForResult(hapticIntent, HAPTIC_UP_INTENT);
            return true;
        }
        else if (preference == mHapticLongSetter) {
            String starting_string = Settings.System.getString(getContentResolver(), Settings.System.HAPTIC_LONG_ARRAY);
            Intent hapticIntent = new Intent();
            hapticIntent.setClass(HapticTweaksActivity.this, HapticAdjust.class);
            Bundle bundle = new Bundle();
            bundle.putString("start_string", starting_string);
            bundle.putInt("hap_type", HAPTIC_LONG_INTENT);
            hapticIntent.putExtras(bundle);
            startActivityForResult(hapticIntent, HAPTIC_LONG_INTENT);
            return true;
        }
        else if (preference == mHapticTapSetter) {
            String starting_string = Settings.System.getString(getContentResolver(), Settings.System.HAPTIC_TAP_ARRAY);
            Intent hapticIntent = new Intent();
            hapticIntent.setClass(HapticTweaksActivity.this, HapticAdjust.class);
            Bundle bundle = new Bundle();
            bundle.putString("start_string", starting_string);
            bundle.putInt("hap_type", HAPTIC_TAP_INTENT);
            hapticIntent.putExtras(bundle);
            startActivityForResult(hapticIntent, HAPTIC_TAP_INTENT);
            return true;
        }
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch(requestCode) {
        case HAPTIC_DOWN_INTENT:
            if (resultCode == RESULT_OK) {
                  String string = data.getStringExtra("returnval");
                  String localized = getString(R.string.haptic_save_toast);
                  Toast.makeText(this, localized + string, Toast.LENGTH_LONG).show();
                  Settings.System.putString(getContentResolver(), Settings.System.HAPTIC_DOWN_ARRAY, string);
                  break;
            }	else  	{
                        Toast.makeText(this, getString(R.string.haptic_cancel_toast), Toast.LENGTH_LONG).show();
                        break;
                        }

        case HAPTIC_UP_INTENT:
            if (resultCode == RESULT_OK) {
                String string = data.getStringExtra("returnval");
                String localized = getString(R.string.haptic_save_toast);
                Toast.makeText(this, localized + string, Toast.LENGTH_LONG).show();
                Settings.System.putString(getContentResolver(), Settings.System.HAPTIC_UP_ARRAY, string);
                break;
            }	else	{
                        Toast.makeText(this, getString(R.string.haptic_cancel_toast), Toast.LENGTH_LONG).show();
                        break;
                        }
        case HAPTIC_LONG_INTENT:
            if (resultCode == RESULT_OK) {
                String string = data.getStringExtra("returnval");
                String localized = getString(R.string.haptic_save_toast);
                Toast.makeText(this, localized + string, Toast.LENGTH_LONG).show();
                Settings.System.putString(getContentResolver(), Settings.System.HAPTIC_LONG_ARRAY, string);
                break;
            }	else	{
                    	Toast.makeText(this, getString(R.string.haptic_cancel_toast), Toast.LENGTH_LONG).show();
                    	break;
                        }
        case HAPTIC_TAP_INTENT:
            if (resultCode == RESULT_OK) {
                String string = data.getStringExtra("returnval");
                String localized = getString(R.string.haptic_save_toast);
                Toast.makeText(this, localized + string, Toast.LENGTH_LONG).show();
                Settings.System.putString(getContentResolver(), Settings.System.HAPTIC_TAP_ARRAY, string);
                break;
            }	else	{
                    	Toast.makeText(this, getString(R.string.haptic_cancel_toast), Toast.LENGTH_LONG).show();
                    	break;
                        }
        }


    }

    private void setHapticButtonDep() {
        mHapticFeedbackUpPref.setDependency(mHapticFeedbackPref.getKey());
        mHapticFeedbackAllPref.setDependency(mHapticFeedbackUpPref.getKey());
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (HAPTIC_FEEDBACK_PREF.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    mHapticFeedbackPref.isChecked() ? 1 : 0);
        } else if (HAPTIC_FEEDBACK_UP_PREF.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_UP_ENABLED,
                    mHapticFeedbackUpPref.isChecked() ? 1 : 0);
        } else if (HAPTIC_FEEDBACK_ALL_PREF.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ALL_ENABLED,
                    mHapticFeedbackAllPref.isChecked() ? 1 : 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setHapticButtonDep();
        updateToggles();
    }
}
