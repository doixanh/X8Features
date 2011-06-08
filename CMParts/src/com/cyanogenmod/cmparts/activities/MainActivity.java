package com.cyanogenmod.cmparts.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.cyanogenmod.cmparts.R;


public class MainActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.cmparts);
    }
}

