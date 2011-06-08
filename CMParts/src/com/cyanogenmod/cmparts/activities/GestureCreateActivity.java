/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;
import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.gesture.GestureOverlayView;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.graphics.RectF;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;

public class GestureCreateActivity extends Activity {
    private static final float LENGTH_THRESHOLD = 120.0f;

    private static final int REQUEST_PICK_SHORTCUT = 1;
    private static final int REQUEST_PICK_APPLICATION = 2;
    private static final int REQUEST_CREATE_SHORTCUT = 3;

    private Gesture mGesture;
    private View mDoneButton;
    private Button mShortcutButton;

    private String mUri;
    private String mFriendlyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gesture_create);
        mDoneButton = findViewById(R.id.done);
        mShortcutButton = (Button) findViewById(R.id.shortcut_picker);
        GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        overlay.addOnGestureListener(new GesturesProcessor());
        // Remove flashlight button if Torch app isn't on the phone
        PackageManager pm = this.getBaseContext().getPackageManager();
        List<ResolveInfo> l = pm.queryBroadcastReceivers(new Intent("net.cactii.flash2.TOGGLE_FLASHLIGHT"), 0);
        if (l.isEmpty()) {
            Button flashlight = (Button) findViewById(R.id.flashlight_pick);
            flashlight.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mGesture != null) {
            outState.putParcelable("gesture", mGesture);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mGesture = savedInstanceState.getParcelable("gesture");
        if (mGesture != null) {
            final GestureOverlayView overlay =
                    (GestureOverlayView) findViewById(R.id.gestures_overlay);
            overlay.post(new Runnable() {
                public void run() {
                    overlay.setGesture(mGesture);
                }
            });

            mDoneButton.setEnabled(true);
        }
    }


    public void addGesture(View v) {
        if (mGesture != null) {
            if (mUri == null) {
                Toast.makeText(this, R.string.gestures_error_missing_shortcut, Toast.LENGTH_SHORT).show();
                return;
            }

            final GestureLibrary store = GestureListActivity.getStore();
            store.addGesture(mUri, mGesture);
            store.save();
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();

    }

    public void cancelGesture(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }
    private class GesturesProcessor implements GestureOverlayView.OnGestureListener {
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            mDoneButton.setEnabled(false);
            mGesture = null;
        }

        public void onGesture(GestureOverlayView overlay, MotionEvent event) {
        }

        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            mGesture = overlay.getGesture();
            if (mGesture.getLength() < LENGTH_THRESHOLD) {
                overlay.clear(false);
            }
            mDoneButton.setEnabled(true);
        }

        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        }
    }

    public void pickShortcut(View v) {
        Bundle bundle = new Bundle();

        ArrayList<String> shortcutNames = new ArrayList<String>();
        shortcutNames.add(getString(R.string.group_applications));
        bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

        ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
        shortcutIcons.add(ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_application));
        bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

        Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
        pickIntent.putExtra(Intent.EXTRA_TITLE, getText(R.string.select_custom_app_title));
        pickIntent.putExtras(bundle);

        startActivityForResult(pickIntent, REQUEST_PICK_SHORTCUT);
    }

    public void pickUnlockOnly(View v) {
        mShortcutButton.setText(getString(R.string.gestures_unlock_only));
        mUri = "UNLOCK___UNLOCK";
    }

    public void pickSoundOnly(View v) {
        mShortcutButton.setText(getString(R.string.gestures_toggle_sound));
        mUri = "SOUND___SOUND";
    }

    public void pickFlashlight(View v) {
        mShortcutButton.setText(getString(R.string.gestures_flashlight));
        mUri = "FLASHLIGHT___FLASHLIGHT";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_APPLICATION:
                    completeSetCustomApp(data);
                    break;
                case REQUEST_CREATE_SHORTCUT:
                    completeSetCustomShortcut(data);
                    break;
                case REQUEST_PICK_SHORTCUT:
                    processShortcut(data, REQUEST_PICK_APPLICATION, REQUEST_CREATE_SHORTCUT);
                    break;
            }
        }
    }

    void processShortcut(Intent intent, int requestCodeApplication, int requestCodeShortcut) {
        String applicationName = getResources().getString(R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            startActivityForResult(pickIntent, requestCodeApplication);
        } else {
            startActivityForResult(intent, requestCodeShortcut);
        }
    }

    void completeSetCustomShortcut(Intent data) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        mFriendlyName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        if (mFriendlyName == null) {
            mFriendlyName = "null";
        }
        mShortcutButton.setText(mFriendlyName);
        mUri = mFriendlyName + "___" + intent.toUri(0);
    }

    void completeSetCustomApp(Intent data) {
        mFriendlyName = data.toUri(0);
        if (mFriendlyName == null) {
            mFriendlyName = "null";
        }
        mShortcutButton.setText(mFriendlyName);
        mUri = mFriendlyName + "___" + data.toUri(0);
    }

}
