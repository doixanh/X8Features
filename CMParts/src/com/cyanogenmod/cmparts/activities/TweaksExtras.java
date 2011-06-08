package com.cyanogenmod.cmparts.activities;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;


import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Environment;
import android.os.AsyncTask;
import android.app.ProgressDialog;


import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlSerializer;
import java.util.ArrayList;
import java.io.IOException;
import android.graphics.Color;
import java.io.FileReader;
import java.io.FileNotFoundException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;
import android.content.Intent;
import android.content.res.AssetManager;

import java.io.FileInputStream;
import java.io.DataInputStream;

import com.cyanogenmod.cmparts.R;

public class TweaksExtras extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String UI_RESET_TO_DEFAULTS = "reset_ui_tweaks_to_defaults";

    private static final String UI_EXPORT_TO_XML = "save_theme";
    private static final String UI_IMPORT_FROM_XML = "apply_theme";
    private static final String NAMESPACE = "com.cyanogenmod.cmparts";

    private static final int WHITE = -1;
    private static final int BLACK = -16777216;
    private static final int SET_ON = 1;
    private static final int SET_OFF = 0;

    private Preference mResetToDefaults;
    private Preference mSaveTheme;
    private static ListPreference mApplyTheme;
    private EditText nameInput;

    public static int gotFileList = 0;
    public static String pickedTheme;
    private static String[] filePickNames = {""};
    private static String[] filePickValues = {""};

    private ImportThemeTask mTask;

    static Context mContext;
    static AssetManager mAssetManager;
    static ContentResolver cr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getBaseContext();
        mAssetManager = getAssets();
        cr = getContentResolver();
        setTitle(R.string.te_title);

        mTask = (ImportThemeTask) getLastNonConfigurationInstance();
        if (mTask != null) {
            mTask.mActivity = this;
        }

        addPreferencesFromResource(R.xml.tweaks_extras);
        PreferenceScreen prefSet = getPreferenceScreen();
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(getApplicationContext(), R.string.xml_sdcard_unmounted, Toast.LENGTH_SHORT).show();
            onDestroy();
        }
        mResetToDefaults = prefSet.findPreference(UI_RESET_TO_DEFAULTS);
        mSaveTheme = prefSet.findPreference(UI_EXPORT_TO_XML);
        mApplyTheme = (ListPreference)prefSet.findPreference(UI_IMPORT_FROM_XML);

        mApplyTheme.setEntries(filePickNames);
        mApplyTheme.setEntryValues(filePickValues);
        mApplyTheme.setOnPreferenceChangeListener(this);

        Intent getList = new Intent("com.cyanogenmod.cmparts.GET_THEME_LIST");
        sendBroadcast(getList);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mTask;
    }

    /**
     * This builds the master file list for the load theme listpreference -
     * it collects the files on the sd card under /CMTheme as well as built-in
     * themes from CMParts/res/assets.
     *
     * This is called from .intent.catchThemeListReceiver with the
     * @param sdList    file list from sdcard (or null)
     * @param gotSDThemes    whether we were successful or not at getting this list
     * @hide
     */

    public static void buildFileList(String[] sdList, boolean gotSDThemes) {
        ArrayList<String> masterFileNames = new ArrayList<String>();
        ArrayList<String> masterFileValues = new ArrayList<String>();
        // First, grab built-in xml files
        String[] builtinThemes = null;
        try {
            builtinThemes = mAssetManager.list("CMTheme");
            } catch (IOException e) {}
        int numthemes = builtinThemes.length;
        if (builtinThemes != null && numthemes > 0) {
            for (int i = 0; i < numthemes; i++) {
                masterFileNames.add(builtinThemes[i].split(".xml")[0]);
                masterFileValues.add("CMTheme/" + builtinThemes[i]);
            }
        }
        // If we were successful at grabbing a list off the SD Card, add those to the arraylists
        if (gotSDThemes) {
            int numsdthemes = sdList.length;
            for (int i = 0; i < numsdthemes; i++) {
                try {
                    masterFileNames.add(sdList[i].split(".xml")[0]);
                    masterFileValues.add(sdList[i]);
                } catch (ArrayIndexOutOfBoundsException e) { }// if name is no good, skip it
            }
        }
        filePickNames = new String [masterFileNames.size()];
        masterFileNames.toArray(filePickNames);
        filePickValues = new String [masterFileValues.size()];
        masterFileValues.toArray(filePickValues);
        mApplyTheme.setEntryValues(filePickValues);
        mApplyTheme.setEntries(filePickNames);
        mApplyTheme.setEnabled(true);

    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /* Reset to Defaults */
        if (preference == mResetToDefaults) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getResources().getString(R.string.title_dialog_ui_interface));
            alertDialog.setMessage(getResources().getString(R.string.message_dialog_reset));
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    resetUITweaks();
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            alertDialog.show();
        }
        else if (preference == mSaveTheme) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getResources().getString(R.string.title_dialog_ui_interface));
            alertDialog.setMessage(getResources().getString(R.string.message_dialog_export));

            nameInput = new EditText(this);
            alertDialog.setView(nameInput);

            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String nameUnformatted = nameInput.getText().toString();
                    if (nameUnformatted != null) {
                        nameUnformatted = nameUnformatted.trim();
                        if (nameUnformatted.length() < 1)
                            nameUnformatted = "unnamed";
                        String nameFormatted = nameUnformatted.replace(" ", "_");
                        nameFormatted = nameFormatted.concat(".xml");
                        writeUIValuesToXML(nameFormatted);
                    }
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            alertDialog.show();
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object picked) {
        if (preference == mApplyTheme) {
            if (picked != null) {
                pickedTheme = (String)picked;
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(getResources().getString(R.string.title_dialog_ui_interface));
                alertDialog.setMessage(getResources().getString(R.string.message_dialog_import));
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mTask = new ImportThemeTask(TweaksExtras.this);
                        mTask.execute();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alertDialog.show();
                return true;
            }
        }
        return false;
    }

    private static class ImportThemeTask extends AsyncTask<Void, Integer, Void> {
        public TweaksExtras mActivity;
        public ProgressDialog mProgress;

        public ImportThemeTask(TweaksExtras activity) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            mProgress = ProgressDialog.show(mActivity, "", "Loading theme. Please wait...", true);
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            switch (params[0]) {
            case 0:
                Toast.makeText(mContext, R.string.xml_sdcard_unmounted, Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(mContext, R.string.xml_file_not_found, Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(mContext, R.string.xml_io_exception, Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(mContext, R.string.xml_parse_error, Toast.LENGTH_SHORT).show();
                break;
            case 4:
                Toast.makeText(mContext, R.string.xml_invalid_color, Toast.LENGTH_SHORT).show();
                break;
            case 5:
                Toast.makeText(mContext, R.string.xml_import_success, Toast.LENGTH_SHORT).show();
                break;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (pickedTheme.contains("STOCK")) {
                boolean sd = (pickedTheme == null);
                File xmlFile = null;
                FileReader freader = null;
                InputStream iStream = null;
                InputStreamReader isreader = null;
                if (sd) {
                    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        publishProgress(0);
                        return null;
                    }
                    String fPath = "/data/" + NAMESPACE + "/tempfile.xml";
                    xmlFile = new File(Environment.getDataDirectory() + fPath);
                    try {
                        freader = new FileReader(xmlFile);
                    } catch (FileNotFoundException e) { }
                } else {
                    try {
                        iStream = mAssetManager.open(pickedTheme);
                        isreader = new InputStreamReader(iStream);
                    } catch (IOException e) { }
                }
                boolean success = false;
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();

                    if (sd) {
                        parser.setInput(freader);
                    } else {
                        parser.setInput(isreader);
                    }
                    int eventType = parser.getEventType();
                    String uiType = null;

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        switch (eventType) {
                            case XmlPullParser.START_TAG:
                                uiType = parser.getName().trim();
                                if (!uiType.equalsIgnoreCase("cmparts")) {
                                    String val = parser.nextText();
                                    if (val.contains("#")) {
                                        Settings.System.putInt(cr, uiType, Color.parseColor(val));
                                    } else {
                                        Settings.System.putInt(cr, uiType, Integer.parseInt(val));
                                    }
                                }
                                break;
                        }
                        eventType = parser.next();
                    }
                    success = true;
                } catch (FileNotFoundException e) {
                    publishProgress(1);
                } catch (IOException e) {
                    publishProgress(2);
                } catch (XmlPullParserException e) {
                    publishProgress(3);
                } catch (IllegalArgumentException e) {
                    publishProgress(4);
                } finally {
                    if (freader != null) {
                        try {
                            freader.close();
                        } catch (IOException e) {
                        }
                    }
                    if (isreader != null) {
                        try {
                            isreader.close();
                        } catch (IOException e) {
                        }
                    }
                }
                if (success) {
                    publishProgress(5);
                }
                if (xmlFile != null && xmlFile.exists()) {
                    xmlFile.delete();
                }
            } else {
                Intent mvSdUi = new Intent("com.cyanogenmod.cmparts.RESTORE_CMPARTS_UI");
                mvSdUi.putExtra("filename", pickedTheme);
                mActivity.sendBroadcast(mvSdUi);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mProgress.dismiss();
        }
    }

    private void resetUITweaks() {
        Settings.System.putInt(getContentResolver(), Settings.System.CLOCK_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.DBM_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.DATE_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.SPN_LABEL_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.PLMN_LABEL_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_COLOR, WHITE);
        Settings.System.putInt(getContentResolver(), Settings.System.NEW_NOTIF_TICKER_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_COUNT_COLOR, WHITE);
        Settings.System.putInt(getContentResolver(), Settings.System.NO_NOTIF_COLOR, WHITE);
        Settings.System.putInt(getContentResolver(), Settings.System.CLEAR_BUTTON_LABEL_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.ONGOING_NOTIF_COLOR, WHITE);
        Settings.System.putInt(getContentResolver(), Settings.System.LATEST_NOTIF_COLOR, WHITE);
        Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TITLE_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TEXT_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_ITEM_TIME_COLOR, BLACK);
        Settings.System.putInt(getContentResolver(), Settings.System.BATTERY_PERCENTAGE_STATUS_ICON, SET_OFF);
        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_STATUS_CLOCK, SET_ON);
        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_STATUS_DBM, SET_OFF);
        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_PLMN_LS, SET_ON);
        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_SPN_LS, SET_ON);
        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_PLMN_SB, SET_ON);
        Settings.System.putInt(getContentResolver(), Settings.System.SHOW_SPN_SB, SET_ON);
        Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_BAR_COLOR, WHITE);
        Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_BAR_CUSTOM, SET_OFF);
        Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_EXPANDED_BAR_COLOR, WHITE);
        Settings.System.putInt(getContentResolver(), Settings.System.NOTIF_EXPANDED_BAR_CUSTOM, SET_OFF);
        Settings.System.putInt(getContentResolver(), Settings.System.HDPI_BATTERY_ALIGNMENT, SET_OFF);
        Toast.makeText(getApplicationContext(), R.string.reset_ui_success, Toast.LENGTH_SHORT).show();
    }
    private void writeUIValuesToXML(String filename) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(getApplicationContext(), R.string.xml_sdcard_unmounted, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent mvUiSd = new Intent("com.cyanogenmod.cmparts.SAVE_CMPARTS_UI");
        FileWriter writer = null;
        File outFile = new File(Environment.getDataDirectory() + "/data/" + NAMESPACE + "/tempfile.xml");
        boolean success = false;
        try {
            outFile.createNewFile();
            writer = new FileWriter(outFile);
            XmlSerializer serializer = Xml.newSerializer();

            ArrayList<String> elements = new ArrayList<String>();
            elements.add(Settings.System.BATTERY_PERCENTAGE_STATUS_COLOR);
            elements.add(Settings.System.CLOCK_COLOR);
            elements.add(Settings.System.DBM_COLOR);
            elements.add(Settings.System.DATE_COLOR);
            elements.add(Settings.System.PLMN_LABEL_COLOR);
            elements.add(Settings.System.SPN_LABEL_COLOR);
            elements.add(Settings.System.NEW_NOTIF_TICKER_COLOR);
            elements.add(Settings.System.NOTIF_COUNT_COLOR);
            elements.add(Settings.System.NO_NOTIF_COLOR);
            elements.add(Settings.System.CLEAR_BUTTON_LABEL_COLOR);
            elements.add(Settings.System.ONGOING_NOTIF_COLOR);
            elements.add(Settings.System.LATEST_NOTIF_COLOR);
            elements.add(Settings.System.NOTIF_ITEM_TITLE_COLOR);
            elements.add(Settings.System.NOTIF_ITEM_TEXT_COLOR);
            elements.add(Settings.System.NOTIF_ITEM_TIME_COLOR);
            elements.add(Settings.System.BATTERY_PERCENTAGE_STATUS_ICON);
            elements.add(Settings.System.SHOW_STATUS_CLOCK);
            elements.add(Settings.System.SHOW_STATUS_DBM);
            elements.add(Settings.System.SHOW_PLMN_LS);
            elements.add(Settings.System.SHOW_SPN_LS);
            elements.add(Settings.System.SHOW_PLMN_SB);
            elements.add(Settings.System.SHOW_SPN_SB);
            elements.add(Settings.System.NOTIF_BAR_COLOR);
            elements.add(Settings.System.NOTIF_BAR_CUSTOM);
            elements.add(Settings.System.NOTIF_EXPANDED_BAR_COLOR);
            elements.add(Settings.System.NOTIF_EXPANDED_BAR_CUSTOM);
            elements.add(Settings.System.HDPI_BATTERY_ALIGNMENT);

            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "cmparts");

            int color = WHITE;

            for (String s : elements) {
                try {
                    color = Settings.System.getInt(getContentResolver(), s);
                }
                catch (SettingNotFoundException e) {
                    if (s.equals(Settings.System.BATTERY_PERCENTAGE_STATUS_COLOR) ||  // all white colors
                            s.equals(Settings.System.NOTIF_COUNT_COLOR) ||
                            s.equals(Settings.System.NO_NOTIF_COLOR) ||
                            s.equals(Settings.System.ONGOING_NOTIF_COLOR) ||
                            s.equals(Settings.System.LATEST_NOTIF_COLOR) ||
                            s.equals(Settings.System.NOTIF_BAR_COLOR) ||
                            s.equals(Settings.System.NOTIF_EXPANDED_BAR_COLOR)) {
                        color = WHITE;
                    } else
                    if (s.equals(Settings.System.SHOW_STATUS_CLOCK) ||        // all default on items
                            s.equals(Settings.System.SHOW_PLMN_LS) ||
                            s.equals(Settings.System.SHOW_SPN_LS) ||
                            s.equals(Settings.System.SHOW_PLMN_SB) ||
                            s.equals(Settings.System.SHOW_SPN_SB)) {
                        color = SET_ON;
                    } else
                    if (s.equals(Settings.System.BATTERY_PERCENTAGE_STATUS_ICON) ||         // all default off items
                            s.equals(Settings.System.SHOW_STATUS_DBM) ||
                            s.equals(Settings.System.NOTIF_BAR_CUSTOM) ||
                            s.equals(Settings.System.NOTIF_EXPANDED_BAR_CUSTOM) ||
                            s.equals(Settings.System.HDPI_BATTERY_ALIGNMENT)) {
                        color = SET_OFF;
                    }
                    else {
                        color = BLACK;        // all black colors
                    }
                }

                serializer.startTag("", s);
                serializer.text((color < 0) ? convertToARGB(color) : Integer.toString(color)); // neg is a color, 0 or 1 is a switch
                serializer.endTag("", s);
            }

            serializer.endTag("", "cmparts");
            serializer.endDocument();
            serializer.flush();
            success = true;
        }
        catch (Exception e) {
            android.util.Log.d("XMLOUTPUT", e.toString());
            Toast.makeText(getApplicationContext(), R.string.xml_write_error, Toast.LENGTH_SHORT).show();
        }
        finally {
            if (writer != null) {
        		try {
	        	    writer.close();
	        	} catch (IOException e) {
	        	}
	        }
        }

        if (success) {
            try {
              FileInputStream infile = new FileInputStream(outFile);
              DataInputStream in = new DataInputStream(infile);
              byte[] b = new byte[in.available()];
              in.readFully(b);
              in.close();
              String result = new String(b, 0, b.length);
              mvUiSd.putExtra("xmldata", result);
              mvUiSd.putExtra("filename", filename);
            } catch (Exception e) {
              e.printStackTrace();
            }
            sendBroadcast(mvUiSd);
        }
        if (outFile.exists())
            outFile.delete();
    }

    public static void readUIValuesFromXML(String name) {
        boolean sd = (name == null);
        File xmlFile = null;
        FileReader freader = null;
        InputStream iStream = null;
        InputStreamReader isreader = null;
        if (sd) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Toast.makeText(mContext, R.string.xml_sdcard_unmounted, Toast.LENGTH_SHORT).show();
                return;
            }
            xmlFile = new File(Environment.getDataDirectory() + "/data/" + NAMESPACE + "/tempfile.xml");
            try {
                freader = new FileReader(xmlFile);
            } catch (FileNotFoundException e) { }
        } else {
            try {
                iStream = mAssetManager.open(pickedTheme);
                isreader = new InputStreamReader(iStream);
            } catch (IOException e) { }
        }
        boolean success = false;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();

            if (sd) {
                parser.setInput(freader);
            } else {
                parser.setInput(isreader);
            }
            int eventType = parser.getEventType();
            String uiType = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                case XmlPullParser.START_TAG:
                    uiType = parser.getName().trim();
                    if (!uiType.equalsIgnoreCase("cmparts")) {
                        String val = parser.nextText();
                        if (val.contains("#")) {
                            Settings.System.putInt(cr, uiType, Color.parseColor(val));
                        } else {
                            Settings.System.putInt(cr, uiType, Integer.parseInt(val));
                        }
                    }
                    break;
                }
                eventType = parser.next();
            }
            success = true;
        } catch (FileNotFoundException e) {
            Toast.makeText(mContext, R.string.xml_file_not_found, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(mContext, R.string.xml_io_exception, Toast.LENGTH_SHORT).show();
        } catch (XmlPullParserException e) {
            Toast.makeText(mContext, R.string.xml_parse_error, Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(mContext, R.string.xml_invalid_color, Toast.LENGTH_SHORT).show();
        } finally {
            if (freader != null) {
                try {
                    freader.close();
                } catch (IOException e) { }
            }
            if (isreader != null) {
                try {
                    isreader.close();
                } catch (IOException e) { }
            }
        }
        if (success) {
            Toast.makeText(mContext, R.string.xml_import_success, Toast.LENGTH_SHORT).show();
        }
        if (xmlFile != null && xmlFile.exists())
            xmlFile.delete();
    }

    private String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }
        if (red.length() == 1) {
            red = "0" + red;
        }
        if (green.length() == 1) {
            green = "0" + green;
        }
        if (blue.length() == 1) {
            blue = "0" + blue;
        }
        return "#" + alpha + red + green + blue;
    }
}

