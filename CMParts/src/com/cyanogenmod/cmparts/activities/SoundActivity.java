package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

public class SoundActivity extends PreferenceActivity implements
        OnPreferenceChangeListener {

    private static final int DIALOG_QUIET_HOURS_START = 1;
    private static final int DIALOG_QUIET_HOURS_END = 2;

    private static final String NOTIFICATIONS_FOCUS = "notif-focus";
    private static final String NOTIFICATIONS_SPEAKER = "notif-speaker";
    private static final String NOTIFICATIONS_ATTENUATION = "notif-attn";
    private static final String NOTIFICATIONS_LIMITVOL = "notif-limitvol";
    private static final String RINGS_SPEAKER = "ring-speaker";
    private static final String RINGS_ATTENUATION = "ring-attn";
    private static final String RINGS_LIMITVOL = "ring-limitvol";
    private static final String ALARMS_SPEAKER = "alarm-speaker";
    private static final String ALARMS_ATTENUATION = "alarm-attn";
    private static final String ALARMS_LIMITVOL = "alarm-limitvol";
    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
    private static final String KEY_QUIET_HOURS_END = "quiet_hours_end";
    private static final String KEY_QUIET_HOURS_MUTE = "quiet_hours_mute";
    private static final String KEY_QUIET_HOURS_STILL = "quiet_hours_still";
    private static final String KEY_QUIET_HOURS_DIM = "quiet_hours_dim";

    private static final String PREFIX = "persist.sys.";

    private CheckBoxPreference mQuietHoursEnabled;
    private Preference mQuietHoursStart;
    private Preference mQuietHoursEnd;
    private CheckBoxPreference mQuietHoursMute;
    private CheckBoxPreference mQuietHoursStill;
    private CheckBoxPreference mQuietHoursDim;

    private static String getKey(String suffix) {
        return PREFIX + suffix;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.sound_title);
        addPreferencesFromResource(R.xml.sound_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        CheckBoxPreference p = (CheckBoxPreference) prefSet.findPreference(NOTIFICATIONS_FOCUS);
        p.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATIONS_AUDIO_FOCUS, 1) != 0);
        p.setOnPreferenceChangeListener(this);

        p = (CheckBoxPreference) prefSet.findPreference(NOTIFICATIONS_SPEAKER);
        p.setChecked(SystemProperties.getBoolean(getKey(NOTIFICATIONS_SPEAKER), false));
        p.setOnPreferenceChangeListener(this);

        p = (CheckBoxPreference) prefSet.findPreference(RINGS_SPEAKER);
        p.setChecked(SystemProperties.getBoolean(getKey(RINGS_SPEAKER), false));
        p.setOnPreferenceChangeListener(this);

        p = (CheckBoxPreference) prefSet.findPreference(ALARMS_SPEAKER);
        p.setChecked(SystemProperties.getBoolean(getKey(ALARMS_SPEAKER), false));
        p.setOnPreferenceChangeListener(this);

        ListPreference lp = (ListPreference) prefSet.findPreference(NOTIFICATIONS_ATTENUATION);
        lp.setValue(String.valueOf(SystemProperties.getInt(getKey(NOTIFICATIONS_ATTENUATION), 6)));
        lp.setSummary(lp.getEntry());
        lp.setOnPreferenceChangeListener(this);

        lp = (ListPreference) prefSet.findPreference(RINGS_ATTENUATION);
        lp.setValue(String.valueOf(SystemProperties.getInt(getKey(RINGS_ATTENUATION), 6)));
        lp.setSummary(lp.getEntry());
        lp.setOnPreferenceChangeListener(this);

        lp = (ListPreference) prefSet.findPreference(ALARMS_ATTENUATION);
        lp.setValue(String.valueOf(SystemProperties.getInt(getKey(ALARMS_ATTENUATION), 6)));
        lp.setSummary(lp.getEntry());
        lp.setOnPreferenceChangeListener(this);

        lp = (ListPreference) prefSet.findPreference(NOTIFICATIONS_LIMITVOL);
        lp.setValue(String.valueOf(SystemProperties.getInt(getKey(NOTIFICATIONS_LIMITVOL), 1)));
        lp.setSummary(lp.getEntry());
        lp.setOnPreferenceChangeListener(this);

        lp = (ListPreference) prefSet.findPreference(RINGS_LIMITVOL);
        lp.setValue(String.valueOf(SystemProperties.getInt(getKey(RINGS_LIMITVOL), 1)));
        lp.setSummary(lp.getEntry());
        lp.setOnPreferenceChangeListener(this);

        lp = (ListPreference) prefSet.findPreference(ALARMS_LIMITVOL);
        lp.setValue(String.valueOf(SystemProperties.getInt(getKey(ALARMS_LIMITVOL), 1)));
        lp.setSummary(lp.getEntry());
        lp.setOnPreferenceChangeListener(this);

        mQuietHoursEnabled = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_ENABLED);
        mQuietHoursStart = findPreference(KEY_QUIET_HOURS_START);
        mQuietHoursEnd = findPreference(KEY_QUIET_HOURS_END);
        mQuietHoursMute = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_MUTE);
        mQuietHoursStill = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_STILL);
        mQuietHoursDim = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_DIM);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mQuietHoursEnabled) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUIET_HOURS_ENABLED,
                    mQuietHoursEnabled.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursMute) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUIET_HOURS_MUTE,
                    mQuietHoursMute.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursStill) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUIET_HOURS_STILL,
                    mQuietHoursStill.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursDim) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUIET_HOURS_DIM,
                    mQuietHoursDim.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursStart) {
            showDialog(DIALOG_QUIET_HOURS_START);
            return true;
        } else if (preference == mQuietHoursEnd) {
            showDialog(DIALOG_QUIET_HOURS_END);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key.equals(NOTIFICATIONS_FOCUS)) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.NOTIFICATIONS_AUDIO_FOCUS, getBoolean(newValue) ? 1 : 0);
        }
        else if (key.equals(NOTIFICATIONS_SPEAKER) ||
                key.equals(RINGS_SPEAKER) ||
                key.equals(ALARMS_SPEAKER)) {
            SystemProperties.set(getKey(key), getBoolean(newValue) ? "1" : "0");
        } else {
            SystemProperties.set(getKey(key), String.valueOf(getInt(newValue)));
            mHandler.sendMessage(mHandler.obtainMessage(0, key));
        }

        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_QUIET_HOURS_START:
            return createTimePicker(Settings.System.QUIET_HOURS_START);
        case DIALOG_QUIET_HOURS_END:
            return createTimePicker(Settings.System.QUIET_HOURS_END);
        }
        return super.onCreateDialog(id);
    }

    private TimePickerDialog createTimePicker(final String key) {
        int value = Settings.System.getInt(getContentResolver(), key, -1);
        int hour;
        int minutes;
        if (value < 0) {
            Calendar calendar = Calendar.getInstance();
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minutes = calendar.get(Calendar.MINUTE);
        } else {
            hour = value / 60;
            minutes = value % 60;
        }
        TimePickerDialog dlg = new TimePickerDialog(
            this, /* context */
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker v, int hours, int minutes) {
                    Settings.System.putInt(
                            getContentResolver(),
                            key,
                            hours * 60 + minutes
                    );
                };
            },
            hour,
            minutes,
            DateFormat.is24HourFormat(this)
        );
        return dlg;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (msg.obj != null) {
                        ListPreference p = (ListPreference) findPreference(msg.obj.toString());
                        p.setSummary(p.getEntry());
                    }
                    break;
            }
        }
    };

    private boolean getBoolean(Object o) {
        return Boolean.valueOf(o.toString());
    }

    private int getInt(Object o) {
        return Integer.valueOf(o.toString());
    }
}
