package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;
import com.cyanogenmod.cmparts.provider.RenderFXWidgetProvider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.RemoteViews;

public class WidgetOptionsActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    public int mAppWidgetId;
    public Context mContext;
    public SharedPreferences mPreferences;
    private ListPreference mRenderFxPref;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.options);
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.mContext = this;
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        mRenderFxPref = (ListPreference) findPreference("widget_render_effect");
        mRenderFxPref.setSummary(mRenderFxPref.getEntry());
        mRenderFxPref.setOnPreferenceChangeListener(this);

        Preference mSave = findPreference("save_settings");
        mSave.setOnPreferenceClickListener(savePrefListener);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRenderFxPref) {
            if (newValue != null) {
                int index = mRenderFxPref.findIndexOfValue(newValue.toString());
                Editor editor = mPreferences.edit();
                editor.putString("widget_render_effect", newValue.toString());
                editor.commit();
                mRenderFxPref.setSummary(mRenderFxPref.getEntries()[index]);
            }
        }
        return false;
    }

    private OnPreferenceClickListener savePrefListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Editor editor = mPreferences.edit();
            editor.putInt("widget_render_effect_" + mAppWidgetId,
                    Integer.parseInt(mPreferences.getString("widget_render_effect", "1")));
            editor.commit();

            Context context = getApplicationContext();
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            Intent launchIntent = new Intent();
            launchIntent.setClass(context, RenderFXWidgetProvider.class);
            launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            launchIntent.setData(Uri.parse("custom:" + mAppWidgetId + "/0"));
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.btn, pi);
            int renderFx = mPreferences.getInt("widget_render_effect_" + mAppWidgetId, 1);
            switch (renderFx) {
            case 1:
                views.setTextViewText(R.id.ind, context.getResources().getString(R.string.night));
                break;
            case 2:
                views.setTextViewText(R.id.ind, context.getResources().getString(R.string.terminal));
                break;
            case 3:
                views.setTextViewText(R.id.ind, context.getResources().getString(R.string.blue));
                break;
            case 4:
                views.setTextViewText(R.id.ind, context.getResources().getString(R.string.amber));
                break;
            case 5:
                views.setTextViewText(R.id.ind, context.getResources().getString(R.string.salmon));
                break;
            case 6:
                views.setTextViewText(R.id.ind, context.getResources().getString(R.string.fuscia));
                break;
            default:
                views.setTextViewText(R.id.ind,
                        context.getResources().getString(R.string.renderfx_temp));
            }
            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(mAppWidgetId, views);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();

            return false;
        }
    };
}
