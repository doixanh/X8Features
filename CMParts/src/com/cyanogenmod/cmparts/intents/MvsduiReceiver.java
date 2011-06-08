package com.cyanogenmod.cmparts.intents;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.widget.Toast;
import java.io.IOException;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import com.cyanogenmod.cmparts.activities.TweaksExtras;
import com.cyanogenmod.cmparts.R;


public class MvsduiReceiver extends BroadcastReceiver {

    public static final String mvUiSd = "com.cyanogenmod.cmpartshelper.RESTORE_CMPARTS_UI";
    private static final String NAMESPACE = "com.cyanogenmod.cmparts";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(mvUiSd)) {
            boolean success = true;
            Bundle extras = intent.getExtras();
            String value = extras.getString("xmldataret");
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
              try {
                BufferedWriter out = new BufferedWriter(new FileWriter(Environment.getDataDirectory() + "/data/" + NAMESPACE + "/tempfile.xml"));
                out.write(value);
                out.close();
              } catch (IOException e) {
                e.printStackTrace();
                success = false;
                Toast.makeText(context, R.string.xml_write_error, Toast.LENGTH_SHORT).show();
              }
              TweaksExtras.readUIValuesFromXML(null);
            } else {
                Toast.makeText(context, R.string.xml_sdcard_unmounted, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
