package com.cyanogenmod.cmparts.services;

import com.cyanogenmod.cmparts.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;

public class RenderFXService extends Service {
	
	public static final String MSG_TAG = "RenderFXService";
    private NotificationManager mNotificationManager;
    private Notification mNotification;
	
	public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			writeRenderEffect(intent.getIntExtra("widget_render_effect", 1));
		}
		
		mNotification = new Notification(R.drawable.notification_icon, getResources().getString(R.string.notify_render_effect),
                                System.currentTimeMillis());

        startForeground(0, mNotification);
		
		return START_STICKY;
	}
	
	public void onDestroy() {
	    writeRenderEffect(0);
		stopForeground(true);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void writeRenderEffect(int mRenderEffect) {
		try {
			IBinder flinger = ServiceManager.getService("SurfaceFlinger");
			if (flinger != null) {
				Parcel data = Parcel.obtain();
				data.writeInterfaceToken("android.ui.ISurfaceComposer");
				data.writeInt(mRenderEffect);
				flinger.transact(1014, data, null, 0);
				data.recycle();
			}
		} catch (RemoteException ex) {
		}
	}
}
