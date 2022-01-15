/**
 * ScreenRecord Java class for cordova-plugin-screen-recording
 * Author: Milos Petrasinovic <mpetrasinovic@pr-dc.com>
 * PR-DC, Republic of Serbia
 * info@pr-dc.com
 * 
 * --------------------
 * Copyright (C) 2022 PR-DC <info@pr-dc.com>
 *
 * This file is part of TestSerialUSB.
 *
 * TestSerialUSB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * TestSerialUSB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TestSerialUSB.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
 
package rs.prdc.screenrecord;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.os.Binder;
import android.os.IBinder;

import android.content.pm.ServiceInfo;

// ScreenRecordService class
// Class for ScreenRecord service
// --------------------
public class ScreenRecordService extends Service {
  private static final String TAG = "ScreenRecordService";
  private NotificationManager mNotificationManager;
  private static final int NOTIFICATION = 1000;
  private String NOTIFICATION_CHANNEL_ID = "rs.prdc.screenrecord";
  private PendingIntent pendingIntent;
  private final IBinder mBinder = new LocalBinder();
  
  // LocalBinder class
  // Class for service binder
  // --------------------
  public class LocalBinder extends Binder {
    ScreenRecordService getService() {
      // Return this instance of LocalService so clients can call public methods
      return ScreenRecordService .this;
    }
  }

  // onBind
  // --------------------
  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }
  
  // ScreenRecordService
  // Constructor
  // --------------------
	public ScreenRecordService() {
		super();
	}

  // onCreate
  // --------------------
	@Override
	public void onCreate() {
		super.onCreate();
		mNotificationManager = (NotificationManager) 
      getSystemService(NOTIFICATION_SERVICE);

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        String channelName = "Screen Recording";
        NotificationChannel chan = new NotificationChannel(
          NOTIFICATION_CHANNEL_ID, channelName, 
          NotificationManager.IMPORTANCE_DEFAULT);
        mNotificationManager.createNotificationChannel(chan);
    }
	}

  // showNotification()
  // Start foreground and show notification
  // --------------------
  public void showNotification(final CharSequence title, 
      final CharSequence text, Context context, int icon) {

    try {
      // Get MainActivity of application
      Class mainActivity;
      Intent launchIntent = context.getPackageManager()
        .getLaunchIntentForPackage(context.getPackageName());
      String className = launchIntent.getComponent().getClassName();
      mainActivity = Class.forName(className);
      
      // Create Intent for notification
      Intent notificationIntent = new Intent(this, mainActivity);
      pendingIntent = PendingIntent.getActivity(this, 0, 
        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
      Builder notiBuilder;
      // Create notification
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notiBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
      } else {
        notiBuilder = new Notification.Builder(this);
      }
          
      Notification notification = notiBuilder.setSmallIcon(icon)
        .setTicker(text)
        .setWhen(System.currentTimeMillis())
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(pendingIntent)
        .build();
          
      startForeground(NOTIFICATION, notification);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  // removeNotification()
  // Remove notification and stop foreground
  // --------------------
  public void removeNotification() {
		stopForeground(NOTIFICATION);
    mNotificationManager.cancel(NOTIFICATION);
  }
}
