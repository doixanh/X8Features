package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.widget.EditText;
import android.view.LayoutInflater;
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public class TrackballNotificationActivity extends PreferenceActivity implements
            Preference.OnPreferenceChangeListener {

    private static final int NOTIFICATION_ID = 400;
    public static String[] mPackage;
    public String mPackageSource;
    public Handler mHandler = new Handler();
    public ProgressDialog pbarDialog;
    public String mGlobalPackage;
    public int mGlobalPulse = 0;
    public int mGlobalSuccession = 0;
    public int mGlobalBlend = 0;
    public CheckBoxPreference globalSuccession;
    public CheckBoxPreference globalRandom;
    public CheckBoxPreference globalOrder;
    public CheckBoxPreference globalBlend;
    public Preference globalCustom;
    public Preference globalTest;

    public String mCatListString;
    public SharedPreferences mPrefs;

    private static final String CAT_PRIMARY = "Miscellaneous";
    private static final String CAT_KEY = "category_list";

    private static final String ADVANCED_SCREEN =  "advanced_screen";
    private static final String CATEGORY_SCREEN = "category_screen";
    private static final String ADD_CATEGORY = "add_category";
    private static final String REMOVE_CATEGORY = "remove_category";
    private static final String ALWAYS_PULSE = "always_pulse";
    private static final String BLEND_COLORS = "blend_colors";
    private static final String PULSE_SUCCESSION = "pulse_succession";
    private static final String PULSE_RANDOM = "pulse_random_colors";
    private static final String PULSE_IN_ORDER = "pulse_colors_in_order";
    private static final String RESET_NOTIFS = "reset_notifications";

    private static final boolean SHOLES_DEVICE = Build.DEVICE.contains("sholes");

    public List<String> uniqueArray(String[] array) {
        Set<String> set = new HashSet<String>(Arrays.asList(array));
        List<String> array2 = new ArrayList<String>(set);
        return array2;
    }

    public boolean isNull(String mString) {
        if (mString == null || mString.matches("null") || mString.length() == 0
                || mString.matches("|") || mString.matches("")) {
            return true;
        } else {
            return false;
        }
    }

    public String[] getArray(String mGetFrom) {
        if (isNull(mGetFrom)) {
            String[] tempfalse = new String[20];
            return tempfalse;
        }
        String[] temp;
        temp = mGetFrom.split("\\|");
        return temp;
    }

    public String createString(String[] mArray) {
        int i;
        String temp = "";
        for (i = 0; i < mArray.length; i++) {
            if (isNull(mArray[i]))
                continue;
            temp = temp + "|" + mArray[i];
        }
        return temp;
    }

    public String[] getPackageAndColorAndBlink(String mString) {
        if (isNull(mString)) {
            return null;
        }
        String[] temp;
        temp = mString.split("=");
        return temp;
    }

    public String[] findPackage(String pkg) {
        String mBaseString = Settings.System.getString(getContentResolver(),
                             Settings.System.NOTIFICATION_PACKAGE_COLORS);
        String[] mBaseArray = getArray(mBaseString);
        if (mBaseArray == null)
            return null;
        for (int i = 0; i < mBaseArray.length; i++) {
            if (isNull(mBaseArray[i])) {
                continue;
            }
            if (mBaseArray[i].contains(pkg)) {
                return getPackageAndColorAndBlink(mBaseArray[i]);
            }
        }
        return null;
    }

    public void updatePackage(String pkg, String color, String blink, String cat) {
        String stringtemp = Settings.System.getString(getContentResolver(),
                            Settings.System.NOTIFICATION_PACKAGE_COLORS);
        String[] temp = getArray(stringtemp);
        int i;
        String[] temp2;
        temp2 = new String[5];
        boolean found = false;
        for (i = 0; i < temp.length; i++) {
            temp2 = getPackageAndColorAndBlink(temp[i]);
            if (temp2 == null) {
                continue;
            }
            if (temp2[0].matches(pkg)) {
                if (!cat.matches("0")) {
                    temp2[3] = cat;
                } else if (!blink.matches("0")) {
                    temp2[2] = blink;
                } else {
                    temp2[1] = color;
                }
                found = true;
                break;
            }
        }
        if (found) {
            try {
                String tempcolor = temp2[0] + "=" + temp2[1] + "=" + temp2[2] + "=" + temp2[3];
                temp[i] = tempcolor;
            } catch (ArrayIndexOutOfBoundsException e) {
                // Making array changes, if they aren't new, they will error.
                // So we force the category to go to Miscelaneous and inform them.
                String tempcolor = temp2[0] + "=" + temp2[1] + "=" + temp2[2] + "=" + CAT_PRIMARY;
                temp[i] = tempcolor;
                Toast.makeText(
                    this,
                    R.string.trackball_array_error,
                    Toast.LENGTH_LONG).show();
            }
        } else {
            int x = 0;
            // Get the last one
            for (x = 0; x < temp.length; x++) {
                if (isNull(temp[x]))
                    break;
            }
            String tempcolor;
            if (!cat.matches("0")) {
                tempcolor = pkg + "=none=2=" + cat;
            } else if (!blink.matches("0")) {
                tempcolor = pkg + "=none=" + blink + "="+CAT_PRIMARY;
            } else {
                tempcolor = pkg + "=" + color + "=2="+CAT_PRIMARY;
            }
            temp[x] = tempcolor;
        }
        Settings.System.putString(getContentResolver(),
                                  Settings.System.NOTIFICATION_PACKAGE_COLORS, createString(temp));
    }

    private String[] colorList = {
        "green", "white", "red", "blue", "yellow", "cyan", "#800080", "#ffc0cb", "#ffa500",
        "#add8e6"
    };

    public void testPackage(String pkg) {
        final int mAlwaysPulse = Settings.System.getInt(getContentResolver(),
                                 Settings.System.TRACKBALL_SCREEN_ON, 0);
        String[] mTestPackage = findPackage(pkg);
        if (mTestPackage == null || mTestPackage[1].equals("none")) {
            return;
        }
        final Notification notification = new Notification();
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        int mBlinkRate = Integer.parseInt(mTestPackage[2]);

        if (mTestPackage[1].equals("random")) {
            Random generator = new Random();
            int x = generator.nextInt(colorList.length - 1);
            notification.ledARGB = Color.parseColor(colorList[x]);
        } else {
            notification.ledARGB = Color.parseColor(mTestPackage[1]);
        }
        notification.ledOnMS = 500;
        notification.ledOffMS = mBlinkRate * 1000;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (mAlwaysPulse != 1) {
            Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_SCREEN_ON, 1);
        }
        nm.notify(NOTIFICATION_ID, notification);

        AlertDialog.Builder endFlash = new AlertDialog.Builder(this);
        endFlash.setMessage(R.string.dialog_clear_flash).setCancelable(false)
        .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                NotificationManager dialogNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                dialogNM.cancel(NOTIFICATION_ID);
                if (mAlwaysPulse != 1) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_SCREEN_ON, 0);
                }
            }
        });
        endFlash.show();
    }

    public String knownPackage(String pkg) {
        if (pkg.equals("com.android.email"))
            return "Email";
        else if (pkg.equals("com.android.mms"))
            return "Messaging";
        else if (pkg.equals("com.google.android.apps.googlevoice"))
            return "Google Voice";
        else if (pkg.equals("com.google.android.gm"))
            return "Gmail";
        else if (pkg.equals("com.google.android.gsf"))
            return "GTalk";
        else if (pkg.equals("com.twitter.android"))
            return "Twitter";
        else if (pkg.equals("jp.r246.twicca"))
            return "Twicca";
        else if (pkg.equals("com.android.phone"))
            return "Missed Call"; // Say Missed Call instead of "Dialer" as
        // people think its missing.

        return null;
    }

    public String getPackageName(PackageInfo p) {
        String knownPackage = knownPackage(p.packageName);
        return knownPackage == null ?
               p.applicationInfo.loadLabel(getPackageManager()).toString() : knownPackage;
    }

    public List<PackageInfo> getPackageList() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        List<PackageInfo> list = new ArrayList<PackageInfo>();
        for (PackageInfo p : packs) {
            try {
                Context appContext = createPackageContext(p.packageName, 0);
                boolean exists = (new File(appContext.getFilesDir(), "trackball_lights")).exists();
                if (exists || (knownPackage(p.packageName) != null)) {
                    list.add(p);
                }
            } catch (Exception e) {
                Log.d("GetPackageList", e.toString());
            }
        }
        return list;
    }

    private List<String> getCategoryList() {
        String mBaseString = Settings.System.getString(getContentResolver(),
                             Settings.System.NOTIFICATION_PACKAGE_COLORS);
        String[] mBaseArray = getArray(mBaseString);
        String[] catList = new String[30];
        int x = 1;
        catList[0] = CAT_PRIMARY;
        for (int i = 0; i < mBaseArray.length; i++, x++) {
            String[] temp = getPackageAndColorAndBlink(mBaseArray[i]);
            if (temp == null) {
                continue;
            }
            if (isNull(temp[3])) {
                continue;
            }
            catList[x] = temp[3];
        }
        return (uniqueArray(catList));
    }

    private void manageCatList(String value, boolean add) {
        List<String> newList = new ArrayList<String>();
        String[] tempList = mCatListString.split("\\|");
        for(int i = 0; i < tempList.length; i++) {
            newList.add(tempList[i]);
        }

        if (!add) {
           int indexRemove = newList.indexOf(value);
           newList.remove(indexRemove);
        } else {
            newList.add(value);
        }

        String newCatList = new String();
        for (String name : newList) {
            if (isNull(name))
                continue;
            int index = newList.indexOf(name);
            if(index == (newList.size() - 1)) {
                newCatList = newCatList + name;
            } else {
                newCatList = newCatList + name + "|";
            }
        }

        Editor mEdit = mPrefs.edit();
        mEdit.putString(CAT_KEY, newCatList);
        mEdit.commit();
    }

    private PreferenceScreen createPreferenceScreen() {
        // The root of our system
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        /* Advanced */
        PreferenceScreen advancedScreen = getPreferenceManager().createPreferenceScreen(this);
        advancedScreen.setKey(ADVANCED_SCREEN);
        advancedScreen.setTitle(R.string.trackball_advanced_title);
        root.addPreference(advancedScreen);

        /* Manage Categories */
        PreferenceScreen catScreen = getPreferenceManager().createPreferenceScreen(this);
        catScreen.setKey(CATEGORY_SCREEN);
        catScreen.setTitle(R.string.trackball_category_screen);
        advancedScreen.addPreference(catScreen);

        Preference addCat = new Preference(this);
        addCat.setKey(ADD_CATEGORY);
        addCat.setSummary(R.string.trackball_category_add_summary);
        addCat.setTitle(R.string.trackball_category_add_title);
        catScreen.addPreference(addCat);

        String[] mRemoveList = getArray(mCatListString);
        ListPreference removeCatList = new ListPreference(this);
        removeCatList.setKey(REMOVE_CATEGORY);
        removeCatList.setTitle(R.string.trackball_category_remove_title);
        removeCatList.setSummary(R.string.trackball_category_remove_summary);
        removeCatList.setDialogTitle(R.string.trackball_category_list_summary);
        removeCatList.setEntries(mRemoveList);
        removeCatList.setEntryValues(mRemoveList);
        removeCatList.setOnPreferenceChangeListener(this);
        catScreen.addPreference(removeCatList);

        CheckBoxPreference alwaysPulse = new CheckBoxPreference(this);
        alwaysPulse.setKey(ALWAYS_PULSE);
        alwaysPulse.setSummary(R.string.pref_trackball_screen_summary);
        alwaysPulse.setTitle(R.string.pref_trackball_screen_title);
        advancedScreen.addPreference(alwaysPulse);

         // Advanced options only relevant to RGB lights
         if (!getResources().getBoolean(R.bool.has_dual_notification_led)) {
            CheckBoxPreference blendPulse = new CheckBoxPreference(this);
            blendPulse.setKey(BLEND_COLORS);
            blendPulse.setSummary(R.string.pref_trackball_blend_summary);
            blendPulse.setTitle(R.string.pref_trackball_blend_title);
            blendPulse.setEnabled(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_NOTIFICATION_SUCCESSION, 0) == 1 ? false : true);
            advancedScreen.addPreference(blendPulse);
            if (SHOLES_DEVICE) {
                blendPulse.setEnabled(false);
                blendPulse.setChecked(false);
                Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_NOTIFICATION_BLEND_COLOR, 0);
            }

            CheckBoxPreference successionPulse = new CheckBoxPreference(this);
            successionPulse.setKey(PULSE_SUCCESSION);
            successionPulse.setSummary(R.string.pref_trackball_sucess_summary);
            successionPulse.setTitle(R.string.pref_trackball_sucess_title);
            successionPulse.setEnabled(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_NOTIFICATION_BLEND_COLOR, 0) == 1 ? false : true);
            advancedScreen.addPreference(successionPulse);

            CheckBoxPreference randomPulse = new CheckBoxPreference(this);
            randomPulse.setKey(PULSE_RANDOM);
            randomPulse.setSummary(R.string.pref_trackball_random_summary);
            randomPulse.setTitle(R.string.pref_trackball_random_title);
            randomPulse.setEnabled(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_NOTIFICATION_BLEND_COLOR, 0) == 1? false : true);
            advancedScreen.addPreference(randomPulse);

            CheckBoxPreference orderPulse = new CheckBoxPreference(this);
            orderPulse.setKey(PULSE_IN_ORDER);
            orderPulse.setSummary(R.string.pref_trackball_order_summary);
            orderPulse.setTitle(R.string.pref_trackball_order_title);
            orderPulse.setEnabled(Settings.System.getInt(getContentResolver(), Settings.System.TRACKBALL_NOTIFICATION_BLEND_COLOR, 0) == 1 ? false : true);
            advancedScreen.addPreference(orderPulse);
        }

        Preference resetColors = new Preference(this);
        resetColors.setKey(RESET_NOTIFS);
        resetColors.setSummary(R.string.pref_trackball_reset_summary);
        resetColors.setTitle(R.string.pref_trackball_reset_title);
        advancedScreen.addPreference(resetColors);

        // Add each application
        PreferenceCategory cat = new PreferenceCategory(this);
        cat.setTitle(R.string.group_applications);
        root.addPreference(cat);

        Map<String, PackageInfo> sortedPackages = new TreeMap<String, PackageInfo>();
        for (PackageInfo pkgInfo : getPackageList()) {
            sortedPackages.put(getPackageName(pkgInfo), pkgInfo);
        }

        for (String catList : getCategoryList()) {
            if (isNull(catList)) {
                continue;
            }

            PreferenceScreen catName = getPreferenceManager().createPreferenceScreen(this);
            catName.setKey(catList + "_screen");
            catName.setTitle(catList);
            cat.addPreference(catName);

            for (Map.Entry<String, PackageInfo> pkgEntry : sortedPackages.entrySet()) {
                String pkg = pkgEntry.getValue().packageName;
                if (isNull(pkg))
                    continue;

                String[] packageValues = findPackage(pkg);
                if (packageValues == null || packageValues[3] == null) {
                    if (!catList.matches(CAT_PRIMARY))
                        continue;
                } else {
                    if (!catList.matches(packageValues[3]))
                        continue;
                }

                String shortPackageName = pkgEntry.getKey();
                PreferenceScreen appName = getPreferenceManager().createPreferenceScreen(this);
                appName.setKey(pkg + "_screen");
                appName.setTitle(shortPackageName);
                catName.addPreference(appName);

                String[] mList = getArray(mCatListString);
                ListPreference categoryList = new ListPreference(this);
                categoryList.setKey(pkg + "_category");
                categoryList.setTitle(R.string.trackball_category_list_title);
                categoryList.setSummary(R.string.trackball_category_list_summary);
                categoryList.setDialogTitle(R.string.trackball_category_list_summary);
                categoryList.setEntries(mList);
                categoryList.setEntryValues(mList);
                categoryList.setOnPreferenceChangeListener(this);
                appName.addPreference(categoryList);

                ListPreference colorList = new ListPreference(this);
                colorList.setKey(pkg + "_color");
                colorList.setTitle(R.string.color_trackball_flash_title);
                colorList.setSummary(R.string.color_trackball_flash_summary);
                colorList.setDialogTitle(R.string.dialog_color_trackball);
                if (getResources().getBoolean(R.bool.has_dual_notification_led)) {
                    colorList.setEntries(R.array.entries_dual_led_colors);
                    colorList.setEntryValues(R.array.values_dual_led_colors);
                } else {
                    colorList.setEntries(R.array.entries_trackball_colors);
                    colorList.setEntryValues(R.array.pref_trackball_colors_values);
                }
                colorList.setOnPreferenceChangeListener(this);
                appName.addPreference(colorList);

                if (!getResources().getBoolean(R.bool.has_dual_notification_led)) {
                    ListPreference blinkList = new ListPreference(this);
                    blinkList.setKey(pkg + "_blink");
                    blinkList.setTitle(R.string.color_trackball_blink_title);
                    blinkList.setSummary(R.string.color_trackball_blink_summary);
                    blinkList.setDialogTitle(R.string.dialog_blink_trackball);
                    blinkList.setEntries(R.array.pref_trackball_blink_rate_entries);
                    blinkList.setEntryValues(R.array.pref_trackball_blink_rate_values);
                    blinkList.setOnPreferenceChangeListener(this);
                    appName.addPreference(blinkList);

                    Preference customColor = new Preference(this);
                    customColor.setKey(pkg + "_custom");
                    customColor.setSummary(R.string.color_trackball_custom_summary);
                    customColor.setTitle(R.string.color_trackball_custom_title);
                    if (packageValues != null) {
                        // Check if the color is none, if it is disable custom.
                        customColor.setEnabled(!packageValues[1].equals("none"));
                    }
                    appName.addPreference(customColor);
                }

                Preference testColor = new Preference(this);
                testColor.setKey(pkg + "_test");
                testColor.setSummary(R.string.color_trackball_test_summary);
                testColor.setTitle(R.string.color_trackball_test_title);
                if (packageValues != null) {
                    // Check if the color is none, if it isdisable Test.
                    testColor.setEnabled(!packageValues[1].equals("none"));
                }
                appName.addPreference(testColor);

                Preference notice = new Preference(this);
                notice.setKey("NULL");
                notice.setTitle(R.string.trackball_color_notice_title);
                notice.setSummary(R.string.trackball_color_notice_summary);
                appName.addPreference(notice);
            }
        }

        return root;
    }

    final Runnable mFinishLoading = new Runnable() {
        public void run() {
            pbarDialog.dismiss();
        }
    };

    public void loadPrefs() {
        pbarDialog = ProgressDialog.show(this, getString(R.string.dialog_trackball_loading), getString(R.string.dialog_trackball_packagelist), true, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCatListString = mPrefs.getString(CAT_KEY, CAT_PRIMARY + "|");
        Thread t = new Thread() {
            public void run() {
                setPreferenceScreen(createPreferenceScreen());
                mHandler.post(mFinishLoading);
            }
        };
        t.start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.trackball_notifications_title);
        loadPrefs();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String value = objValue.toString();
        String key = preference.getKey().toString();
        String pkg = key.substring(0, key.lastIndexOf("_"));
        if (key.endsWith("_blink")) {
            updatePackage(pkg, "", value, "0");
        } else if (key.endsWith("_color")) {
            updatePackage(pkg, value, "0", "0");
            PreferenceScreen prefSet = getPreferenceScreen();
            globalCustom = prefSet.findPreference(pkg + "_custom");
            if (globalCustom != null) {
                globalCustom.setEnabled(!value.matches("none"));
            }
            globalTest = prefSet.findPreference(pkg + "_test");
            globalTest.setEnabled(!value.matches("none"));
        } else if (key.equals(REMOVE_CATEGORY)) {
            if(value.equals(CAT_PRIMARY)) {
                Toast.makeText(this, R.string.trackball_remove_default_toast, Toast.LENGTH_LONG).show();
                return false;
	        }

            manageCatList(value, false);
            loadPrefs();
            PreferenceScreen prefSet = getPreferenceScreen();
            ListPreference removeList = (ListPreference)prefSet.findPreference(REMOVE_CATEGORY);
            removeList.setEntries(getArray(mCatListString));
            removeList.setEntryValues(getArray(mCatListString));
        } else if (key.endsWith("_category")) {
            updatePackage(pkg, "", "0", value);
            loadPrefs();
        }

        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final boolean value;
        AlertDialog alertDialog;
        if (preference.getKey().toString().equals(RESET_NOTIFS)) {
            Settings.System.putString(getContentResolver(), Settings.System.NOTIFICATION_PACKAGE_COLORS, "");
            Editor mEdit = mPrefs.edit();
            mEdit.putString(CAT_KEY, CAT_PRIMARY + "|");
            mEdit.commit();
            Toast.makeText(this, R.string.trackball_reset_all, Toast.LENGTH_LONG).show();
        } else if (preference.getKey().toString().equals(ADD_CATEGORY)) {
            alertDialog = new AlertDialog.Builder(this).create();
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.add_cat, null);
            alertDialog.setTitle(R.string.trackball_category_add_title);
            alertDialog.setView(textEntryView);
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //updateCatList(value.toString(), false);
                    EditText textBox = (EditText) textEntryView.findViewById(R.id.cat_text);
                    manageCatList(textBox.getText().toString(), true);
                    loadPrefs();
                    PreferenceScreen prefSet = getPreferenceScreen();
                    ListPreference removeList = (ListPreference)prefSet.findPreference(REMOVE_CATEGORY);
                    removeList.setEntries(getArray(mCatListString));
                    removeList.setEntryValues(getArray(mCatListString));
                    return;
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    return;
                }
            });
            alertDialog.show();
        } else if (preference.getKey().toString().equals(ALWAYS_PULSE)) {
            CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_SCREEN_ON,
                                   value ? 1 : 0);
        } else if (preference.getKey().toString().equals(PULSE_SUCCESSION)) {
            final CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if (!value) {
                blendToggle(true);
            } else {
                blendToggle(false);
            }
            if (value == false) {
                Settings.System.putInt(getContentResolver(),
                                       Settings.System.TRACKBALL_NOTIFICATION_SUCCESSION, 0);
                return true;
            }

            alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(R.string.notification_battery_warning_title);
            alertDialog.setMessage(getResources().getString(R.string.notification_battery_warning));
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_NOTIFICATION_SUCCESSION, value ? 1 : 0);
                    return;
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_NOTIFICATION_SUCCESSION, value ? 1 : 0);
                    keyPref.setChecked(false);
                    return;
                }
            });
            alertDialog.show();
        } else if (preference.getKey().toString().equals(PULSE_RANDOM)) {
            final CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if (!value) {
                blendToggle(true);
            } else {
                blendToggle(false);
            }
            if (value == false) {
                Settings.System.putInt(getContentResolver(),
                                       Settings.System.TRACKBALL_NOTIFICATION_RANDOM, 0);
                return true;
            }
            alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(R.string.notification_battery_warning_title);
            alertDialog.setMessage(getResources().getString(R.string.notification_battery_warning));
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_NOTIFICATION_RANDOM, value ? 1 : 0);
                    return;

                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_NOTIFICATION_RANDOM, 0);
                    keyPref.setChecked(false);
                    return;
                }
            });
            alertDialog.show();
        } else if (preference.getKey().toString().equals(PULSE_IN_ORDER)) {
            final CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            if (!value) {
                blendToggle(true);
            } else {
                blendToggle(false);
            }
            if (value == false) {
                Settings.System.putInt(getContentResolver(),
                                       Settings.System.TRACKBALL_NOTIFICATION_PULSE_ORDER, 0);
                return true;
            }

            alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(R.string.notification_battery_warning_title);
            alertDialog.setMessage(getResources().getString(R.string.notification_battery_warning));
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_NOTIFICATION_PULSE_ORDER, value ? 1 : 0);
                    return;
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_NOTIFICATION_PULSE_ORDER, 0);
                    keyPref.setChecked(false);
                    return;
                }
            });
            alertDialog.show();
        } else if (preference.getKey().toString().equals(BLEND_COLORS)) {
            final CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            value = keyPref.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_NOTIFICATION_BLEND_COLOR, value ? 1 : 0);
            if (!value) {
                PreferenceScreen prefSet = getPreferenceScreen();
                CheckBoxPreference disablePref = (CheckBoxPreference)prefSet.findPreference(PULSE_SUCCESSION);
                disablePref.setEnabled(true);
                disablePref = (CheckBoxPreference)prefSet.findPreference(PULSE_RANDOM);
                disablePref.setEnabled(true);
                disablePref = (CheckBoxPreference)prefSet.findPreference(PULSE_IN_ORDER);
                disablePref.setEnabled(true);

            } else {
                PreferenceScreen prefSet = getPreferenceScreen();
                CheckBoxPreference disablePref = (CheckBoxPreference)prefSet.findPreference(PULSE_SUCCESSION);
                disablePref.setEnabled(false);
                disablePref = (CheckBoxPreference)prefSet.findPreference(PULSE_RANDOM);
                disablePref.setEnabled(false);
                disablePref = (CheckBoxPreference)prefSet.findPreference(PULSE_IN_ORDER);
                disablePref.setEnabled(false);
            }
        } else if (preference.getKey().toString().endsWith("_custom")) {
            String pkg = preference.getKey().toString().substring(0, preference.getKey().toString().lastIndexOf("_"));
            mGlobalPackage = pkg;
            ColorPickerDialog cp = new ColorPickerDialog(this,
                    mPackageColorListener,
                    readPackageColor());
            cp.show();
        } else if (preference.getKey().toString().endsWith("_test")) {
            String pkg = preference.getKey().toString()
                         .substring(0, preference.getKey().toString().lastIndexOf("_"));
            testPackage(pkg);
        }
        return false;
    }

    private int readPackageColor() {
        String[] mPackage = findPackage(mGlobalPackage);
        if (mPackage == null) {
            return -16777216;
        }
        if (mPackage[1].equals("random")) {
            return -16777216;
        } else {
            try {
                return Color.parseColor(mPackage[1]);
            } catch (IllegalArgumentException e) {
                return -16777216;
            }
        }
    }

    public void pulseLight(int color) {
        mGlobalPulse = Settings.System.getInt(getContentResolver(),
                                              Settings.System.TRACKBALL_SCREEN_ON, 0);
        mGlobalSuccession = Settings.System.getInt(getContentResolver(),
                            Settings.System.TRACKBALL_NOTIFICATION_SUCCESSION, 0);
        mGlobalBlend = Settings.System.getInt(getContentResolver(),
                                              Settings.System.TRACKBALL_NOTIFICATION_BLEND_COLOR, 0);
        Notification notification = new Notification();
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB = color;
        notification.ledOnMS = 500;
        notification.ledOffMS = 0;
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (mGlobalPulse != 1) {
            Settings.System.putInt(getContentResolver(), Settings.System.TRACKBALL_SCREEN_ON, 1);
        }
        if (mGlobalSuccession != 1) {
            Settings.System.putInt(getContentResolver(),
                                   Settings.System.TRACKBALL_NOTIFICATION_SUCCESSION, 1);
        }
        if (mGlobalBlend == 1) {
            Settings.System.putInt(getContentResolver(),
                                   Settings.System.TRACKBALL_NOTIFICATION_BLEND_COLOR, 0);
        }
        nm.notify(NOTIFICATION_ID, notification);
        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // shouldn't happen
                }

                nm.cancel(NOTIFICATION_ID);
                if (mGlobalPulse != 1) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_SCREEN_ON, 0);
                }
                if (mGlobalSuccession != 1) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_NOTIFICATION_SUCCESSION, 0);
                }
                if (mGlobalBlend == 1) {
                    Settings.System.putInt(getContentResolver(),
                                           Settings.System.TRACKBALL_NOTIFICATION_BLEND_COLOR, 1);
                }
            }
        };
        t.start();
    }

    ColorPickerDialog.OnColorChangedListener mPackageColorListener = new ColorPickerDialog.OnColorChangedListener() {
        public void colorUpdate(int color) {
            pulseLight(color);
        }

        public void colorChanged(int color) {
            updatePackage(mGlobalPackage, convertToARGB(color), "0", "0");
        }
    };

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

    private void blendToggle(boolean toggle) {
        if (!SHOLES_DEVICE) {
            CheckBoxPreference disablePref = (CheckBoxPreference) getPreferenceScreen().findPreference(BLEND_COLORS);
            disablePref.setEnabled(toggle);
        }
    }
}
