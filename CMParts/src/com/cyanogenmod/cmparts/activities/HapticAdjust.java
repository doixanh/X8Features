package com.cyanogenmod.cmparts.activities;


import com.cyanogenmod.cmparts.R;
import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableRow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Vibrator;


public class HapticAdjust extends Activity {

    Vibrator mVibrator;
    Button addButton;
    Button subButton;
    Button saveButton;
    Button cancelButton;
    TestButton mTestButton;
    Button revertButton;
    Button defaultButton;

    int hapType = 0;
    int counter=0;
    String startString = "0";
    String defString = "0";
    
static final String TAG = "HapticAdjust";
static final int DOWN_TEXT = 1;
static final int UP_TEXT = 2;
static final int LONG_TEXT = 3;
static final int TAP_TEXT = 4;

    OnClickListener myClickListener = new OnClickListener() {

        public void onClick(View v) {
            if(v==addButton) { addRow(0);}
            if(v==subButton) { subRow();}
            if(v==saveButton) { saveChanges();}
            if(v==cancelButton) {cancelChanges();}
            if(v==mTestButton) {testVibe();}
            if(v==revertButton) {revertChanges();}
            if(v==defaultButton) {defSet(hapType);}
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (HapticAdjust.this.getIntent().getExtras() != null)
        {
        Bundle bundle = this.getIntent().getExtras();
        startString = bundle.getString("start_string");
        hapType = bundle.getInt("hap_type");
        }
        else {
            Log.i(TAG, "haptic adjust something wrong with bundle");
        }

       // set explainer text at top of view depending on where hapticadjust was called from (up, down, or longpress pattern
        setContentView(R.layout.hapticadjust);


        if (hapType == DOWN_TEXT) {
            TextView downDesc;
            String str;
            str = getString(R.string.haptic_down_description);
            downDesc = (TextView) findViewById(R.id.type_desc);
            downDesc.setText(str);
            downDesc.requestLayout();
        } else
        if (hapType == UP_TEXT) {
            TextView downDesc;
            String str;
            str = getString(R.string.haptic_up_description);
            downDesc = (TextView) findViewById(R.id.type_desc);
            downDesc.setText(str);
            downDesc.requestLayout();
        } else
        if (hapType == LONG_TEXT) {
            TextView downDesc;
            String str;
            str = getString(R.string.haptic_long_description);
            downDesc = (TextView) findViewById(R.id.type_desc);
            downDesc.setText(str);
            downDesc.requestLayout();
        } else
        if (hapType == TAP_TEXT) {
            TextView downDesc;
            String str;
            str = getString(R.string.haptic_tap_description);
            downDesc = (TextView) findViewById(R.id.type_desc);
            downDesc.setText(str);
            downDesc.requestLayout();
            }


        addButton = (Button) findViewById(R.id.add_button);
        subButton = (Button) findViewById(R.id.sub_button);
        saveButton = (Button) findViewById(R.id.save_button);
        mTestButton = (TestButton) findViewById(R.id.test_button);
        cancelButton = (Button) findViewById(R.id.cancel_button);
        revertButton = (Button) findViewById(R.id.revert_button);
        defaultButton = (Button) findViewById(R.id.default_button);
        addButton.setOnClickListener(myClickListener);
        subButton.setOnClickListener(myClickListener);
        saveButton.setOnClickListener(myClickListener);
        cancelButton.setOnClickListener(myClickListener);
        revertButton.setOnClickListener(myClickListener);
        defaultButton.setOnClickListener(myClickListener);
        mTestButton.setOnClickListener(myClickListener);
        mVibrator = new Vibrator();
        setupRows(startString);
    }


   // take value passed from parent activity (spare parts) and populate rows
    private boolean setupRows (String ss) {
        if (ss == null) {
            return false;
            }
        int[] vals = stringToInt(ss);
        int length = vals.length;
        int i;
        for (i=0; i < length; i++ ) {
        addRow(vals[i]);
        }
        checkRowColors();
        return true;
    }



    // adds a row - either at user request (+) or generated upon call based on stored values

    public boolean addRow(int val) {
        boolean output;
        TableLayout table = (TableLayout) findViewById(R.id.TableLayout01);

        TableRow row = (TableRow) new TableRow(this);
        int curid = 0;
        curid = counter+201;
        row.setId(curid);
        SeekBar sb = new SeekBar(this);
        sb.setLayoutParams(
            new
            TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1)
        );
        sb.setFocusable(true);
        sb.setMax(100);
        sb.setProgress(val);
        sb.setPadding(10,0,0,0);
        sb.setId(counter+101);
        row.addView(sb);
        sb.setOnSeekBarChangeListener(sbChange);

        TextView t = new TextView(this);
        String s = Integer.toString(sb.getProgress());
        t.setText(s);
        t.setId(counter+1001);
        t.setTextSize(20);
        t.setMinEms(3);
        t.setPadding(10, 0, 0, 0);
        t.setInputType(InputType.TYPE_CLASS_NUMBER);
        row.addView(t);

        TextView onoff = new TextView(this);
        String str = "  "+getString(R.string.haptic_vibe_desc);
        onoff.setTextColor(Color.WHITE);
        if (counter % 2 == 0) {
            str = "  "+getString(R.string.haptic_delay_desc);
            onoff.setTextColor(Color.RED);
        }
        onoff.setPadding(0, 0, 15, 0);
        onoff.setText(str);
        onoff.setId(counter+401);
        row.addView(onoff);

        table.addView(row);
        counter = table.getChildCount();
        output=true;
        checkRowColors();
        return output;
    }

// removes bottom row - at user request (-)

    public void subRow() {
        if (counter==1) {return;}; // can't have less than 1 row
        TableLayout table = (TableLayout) findViewById(R.id.TableLayout01);
        int target = counter + 200;
        table.removeView(findViewById(target));
        counter = table.getChildCount(); //update number of rows
        checkRowColors();
        return;
    }

// save settings: trim off any trailing 0's, return to haptictweaks with bundle
    private void saveChanges() {
        int[] array = new int[counter];
        array = getArray();
        String output = intToString(array);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("returnval", output);
        setResult(RESULT_OK,returnIntent);
        mVibrator.cancel();
        finish();
        
    }

    private void cancelChanges() {
        String output = startString;
        Intent returnIntent = new Intent();
        returnIntent.putExtra("returnval", output);
        setResult(RESULT_CANCELED, returnIntent);
        mVibrator.cancel();
        finish();
    }

    private void revertChanges() {
        clearTable();
        Toast.makeText(this, getString(R.string.haptic_revert_toast), Toast.LENGTH_LONG).show();
        setupRows(startString);
        checkRowColors();
    }

    private void defSet(int type) {
        clearTable();
        Toast.makeText(this, getString(R.string.haptic_default_toast), Toast.LENGTH_LONG).show();
        if (type == DOWN_TEXT) {
            defString = Settings.System.getString(getContentResolver(), Settings.System.HAPTIC_DOWN_ARRAY_DEFAULT);
        } else
        if (type == UP_TEXT) {
            defString = Settings.System.getString(getContentResolver(), Settings.System.HAPTIC_UP_ARRAY_DEFAULT);
        } else
        if (type == LONG_TEXT) {
            defString = Settings.System.getString(getContentResolver(), Settings.System.HAPTIC_LONG_ARRAY_DEFAULT);
        }
        if (type == TAP_TEXT) {
            defString = Settings.System.getString(getContentResolver(), Settings.System.HAPTIC_TAP_ARRAY_DEFAULT);
        }
        boolean worked = setupRows(defString);
        if (!worked) {
        	revertChanges();
        	return;
        }
        checkRowColors();
    }

    private void testVibe() {
        mVibrator = new Vibrator();
        int i;
        int[] array = getArray();
        clearTable();
        setupRows(intToString(array));
        long[] vibePattern = new long[array.length];
        for (i = 0; i < array.length; i++) {
            vibePattern[i] = array[i];
        }
        if (vibePattern.length == 1) {
        	mVibrator.vibrate(vibePattern[0]);
            }
        else {
            mVibrator.vibrate(vibePattern, -1);
            }
        
    }

    private void clearTable() {
        TableLayout table = (TableLayout) findViewById(R.id.TableLayout01);
        table.removeAllViews();
        counter = 0;
    }


    private int[] getArray() {
        int[] result = new int[counter];
        int i;
        int j;
        int k;
        int trimPos = 0;
        int finalLength;
        for (i=0; i < counter; i++) {
            TextView target;
            target = (TextView) findViewById(i+1001);
            result[i] = Integer.parseInt(target.getText().toString());
        }
        int startLen = result.length;
        for (j=(startLen-1); j > 0; j--) {
            if (result[j] == 0)    {
                trimPos = j;
            } else break;
        }
        if (trimPos == 0)
            {
            return result;
            }
        else
            {
            finalLength = trimPos;
            int[] trimmed = new int[finalLength];
            for (k = 0; k < finalLength; k++) {
                trimmed[k] = result[k];
            }
            return trimmed;
        }
    }

    OnSeekBarChangeListener sbChange = new OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar s, int progress, boolean touch){
            if(touch){
                TableRow row = (TableRow)s.getParent();
                TextView changeval = (TextView)row.getChildAt(1);
                changeval.setText(Integer.toString(progress));
            }
        }

        public void onStartTrackingTouch(SeekBar s){
        }

        public void onStopTrackingTouch(SeekBar s){
        }
    };
    
    private void checkRowColors() {
    	if (counter == 1) {
    		int target = counter + 400;
    		TextView tv = (TextView) findViewById(target);
            tv.setTextColor(Color.WHITE);
            String str = "  "+getString(R.string.haptic_vibe_desc);
            tv.setText(str);
            tv.requestLayout();
    	} 
    	else if (counter > 1) {
    		int target = 401;
    		TextView tv = (TextView) findViewById(target);
            tv.setTextColor(Color.RED);
            String str = "  "+getString(R.string.haptic_delay_desc);
            tv.setText(str);
            tv.requestLayout();
    	}
    	else {
    		revertChanges();
    	}
    }

    private int[] stringToInt(String inpstring) {
        String[] splitstr = inpstring.split(",");
        int los = splitstr.length;
        int[] returnint = new int[los];
        int i;
        for (i=0; i < los; i++ ) {
            returnint[i] = Integer.parseInt(splitstr[i].trim());
        }
        return returnint;
    }

    private String intToString(int[] inpint) {
        String returnstring = null;
        int lol = inpint.length;
        String workstring = "";
        int i;
        for (i=0; i < lol; i++ ) {
            if (i>0) { workstring = workstring + ","; }
            workstring = workstring + String.valueOf(inpint[i]);
        }
        returnstring = workstring;
        return returnstring;
    }

    static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i=0; i<ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }
    
    
    public static class TestButton extends Button {
        public TestButton(Context context) {
           super(context);
            }
        public TestButton(Context context, AttributeSet attrs){
            super(context, attrs);
            }
        @Override
        public void playSoundEffect(int effectId) {
            return;
        }
    } 
}
