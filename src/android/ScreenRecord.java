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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
  
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Binder;
import android.os.IBinder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.util.DisplayMetrics;
import android.content.pm.PackageManager;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.provider.MediaStore;

import android.content.ContentValues;
import android.content.ContentResolver;

import android.view.Surface;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import android.net.Uri;

import rs.prdc.screenrecord.ScreenRecordService;

// ScreenRecord class
// Class for cordova plugin
// --------------------
public class ScreenRecord extends CordovaPlugin implements ServiceConnection {

  private final static String TAG = "ScreenRecord";
  private MediaProjectionManager mProjectionManager;
  private MediaRecorder mMediaRecorder;
  private MediaProjection mMediaProjection;
  private VirtualDisplay mVirtualDisplay;
  private ScreenRecordService mScreenRecordService;
  
  protected final static String permission = Manifest.permission.RECORD_AUDIO;
  private Context context;
  
  private static final int FRAME_RATE = 60; // fps
  private final static int SCREEN_RECORD_CODE = 1000;
  private final static int WRITE_EXTERNAL_STORAGE_CODE = 1001;
  
  private CallbackContext callbackContext;
  private JSONObject options;
  private boolean recordAudio;
  private String fileName;
  private String filePath;
  private String notifTitle;
  private String notifText;
  private int mWidth;
  private int mHeight;
  private int mBitRate;
  private int mDpi;
  private int mScreenDensity;
  private boolean serviceStarted = false;
  private Uri mUri;
  
  // execute()
  // Execute functionality based on javascript action
  // --------------------
  @Override
  public boolean execute(String action, JSONArray args, 
      CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
    // Start recording
    if(action.equals("startRecord")) {
      options = args.getJSONObject(0);
      Log.d(TAG, options.toString());
      recordAudio = options.getBoolean("recordAudio");
      mBitRate = options.getInt("bitRate");
      notifTitle = options.getString("title");
      notifText = options.getString("text");
      fileName = args.getString(1);
      this.startRecord();
      return true;
    }
    // Stop recording
    else if(action.equals("stopRecord")) {
      this.stopRecord();
      return true;
    }
    return false;
  }
  
  // startRecord()
  // Start screen recording
  // --------------------
  private void startRecord() {
    Log.d(TAG, "Start recording");
    if(cordova != null) {
      try {
        if(!serviceStarted) {
          startForegroundService();
        } else {
          callScreenRecord();
        }
      } catch(IllegalArgumentException e) {
        callbackContext.error("Illegal Argument Exception.");
        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
        callbackContext.sendPluginResult(r);
      }
    }
  }
  
  // startForegroundService()
  // Start foreground service
  // --------------------
  public void startForegroundService() {
    Activity activity = cordova.getActivity();
    Intent bindIntent = new Intent(activity, ScreenRecordService.class);
    activity.getApplicationContext().bindService(bindIntent, this, 0);
    activity.getApplicationContext().startService(bindIntent);
  }
  
  // onServiceConnected()
  // Start recording when service is connected
  // --------------------
  @Override
  public void onServiceConnected(ComponentName name, IBinder service){
      ScreenRecordService.LocalBinder binder = 
        (ScreenRecordService.LocalBinder) service;
      mScreenRecordService = binder.getService();
      serviceStarted = true;
      callScreenRecord();
  }
  
  // onServiceDisconnected()
  // Service disconnected
  // --------------------
  @Override
  public void onServiceDisconnected(ComponentName name) {
      Log.i(TAG, "Service disconnected");
      serviceStarted = false;
  }
    
  // callScreenRecord()
  // Configures screen recording, called from startRecord()
  // --------------------
  private void callScreenRecord() {
    Activity activity = cordova.getActivity();
    
    // Create notification
    Resources activityRes = activity.getResources();
    int notifkResId = activityRes.getIdentifier("ic_notification", 
      "drawable", activity.getPackageName());
    mScreenRecordService.showNotification(notifTitle, notifText,
      activity.getApplicationContext(), notifkResId);
          
    // Get display metrics
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager()
      .getDefaultDisplay().getMetrics(displayMetrics);
    mScreenDensity = displayMetrics.densityDpi;
    mWidth = displayMetrics.widthPixels;
    mHeight = displayMetrics.heightPixels;
    
    // Create Media Recorder object
    mMediaRecorder = new MediaRecorder();
    mProjectionManager = (MediaProjectionManager)
      activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    
    // Ask for write to external storage permission
    cordova.requestPermission(this, WRITE_EXTERNAL_STORAGE_CODE, 
      Manifest.permission.WRITE_EXTERNAL_STORAGE);
  
    // Ask for screen recording permission
    Intent captureIntent = mProjectionManager.createScreenCaptureIntent();
    cordova.startActivityForResult(this, captureIntent, SCREEN_RECORD_CODE);
  }
  
  // onRequestPermissionsResult()
  // Real start of recording, called from callScreenRecord()
  // --------------------
  @Override
  public void onRequestPermissionResult(int requestCode, 
      String[] permissions, int[] grantResults) throws JSONException {
    if(requestCode == WRITE_EXTERNAL_STORAGE_CODE) {
      if(grantResults.length == 1 && grantResults[0] == 
          PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "Permission for external storage write granted.");
      } else {
        Log.d(TAG, "Permission for external storage write denied.");
        callbackContext.error("Permission for external storage write denied.");
      }
    }
  }
    
  // onActivityResult()
  // After permission for screen recording granted, called from callScreenRecord()
  // --------------------
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == SCREEN_RECORD_CODE) {
      context = cordova.getActivity().getApplicationContext();
      
      // Create output file path
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "PR-DC");
        contentValues.put(MediaStore.Video.Media.IS_PENDING, true);
        contentValues.put(MediaStore.Video.Media.TITLE, fileName);
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        mUri = context.getContentResolver()
          .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        Log.d(TAG, "Output file: " + mUri.toString());
      } else {
        File file = new File(context.getExternalFilesDir("PR-DC"), fileName);
        filePath = file.getAbsolutePath();
        Log.d(TAG, "Output file: " + filePath);
      }
      
      // Set MediaRecorder options
      try {
        if(recordAudio) {
          mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
          mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          mMediaRecorder.setOutputFile(context.getContentResolver()
            .openFileDescriptor(mUri, "rw")
            .getFileDescriptor());
        } else {
          mMediaRecorder.setOutputFile(filePath);
        }
        mMediaRecorder.setVideoSize(mWidth, mHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setVideoEncodingBitRate(mBitRate);
        mMediaRecorder.setVideoFrameRate(FRAME_RATE); // fps
        mMediaRecorder.prepare();
      } catch(Exception e) {
        e.printStackTrace();
      }
      
      // Create virtual display
      mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
      mMediaProjection.createVirtualDisplay("MainActivity",
        mWidth, mHeight, mScreenDensity,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mMediaRecorder.getSurface(), null, null);
        
      // MediaRecorder onError callback
      mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mediaRecorder, int what, int extra) {
          callbackContext.error("Error: " + what + ", extra = " + what);
          Log.d(TAG, "onError: what = " + what + " extra = " + what);
        }
      });
      
      // MediaRecorder onInfo callback
      mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
         Log.i(TAG, "onInfo: what = " + what + " extra = " + what);
        }
      });
        
      // Start recording
      mMediaRecorder.start();
      Log.d(TAG, "Screenrecord service is running");
      callbackContext.success("Screenrecord service is running");
      cordova.getActivity().moveTaskToBack(true);
             
      if(mMediaProjection == null) {
        Log.e(TAG, "No screen recording in process");
        callbackContext.error("No screen recording in process");
        return;
      }
    }
  }
  
  // stopRecord()
  // Stop screen recording
  // --------------------
  private void stopRecord() {
    if(mVirtualDisplay != null) {
      mVirtualDisplay.release();
      mVirtualDisplay = null;
    }
    if(mMediaProjection != null) {
      mMediaProjection.stop();
      mMediaProjection = null;
    }
    if(mMediaRecorder != null) {
      mMediaRecorder.setOnErrorListener(null);
      mMediaRecorder.setOnInfoListener(null);
      mMediaRecorder.stop();
      mMediaRecorder.reset();
      mMediaRecorder.release();
    } else {
      callbackContext.error("No screen recording in process");
    }
    mScreenRecordService.removeNotification();
    callbackContext.success("Screen recording finished.");
    Log.d(TAG, "Screen recording finished.");

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      // Update video in gallery
      ContentValues contentValues = new ContentValues();
      contentValues.put(MediaStore.Video.Media.IS_PENDING, false);
      context.getContentResolver().update(mUri, contentValues, null, null);
      filePath = mUri.toString();
    } else {
      // Add video to gallery
      ContentValues contentValues = new ContentValues();
      contentValues.put(MediaStore.Video.Media.TITLE, fileName);
      contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
      contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
      contentValues.put(MediaStore.Video.Media.DATA, filePath);
      context.getContentResolver()
        .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
    }
    
    // Pass video file to media scanner service 
    // (in order to be shown in file system)
    MediaScannerConnection.scanFile(
      context,
      new String[]{filePath},
      new String[]{"video/mp4"},
      null);
  }
}
