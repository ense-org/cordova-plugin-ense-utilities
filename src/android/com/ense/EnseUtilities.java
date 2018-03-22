
/**
 */
package com.ense;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.Manifest;

import android.util.Log;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class EnseUtilities extends CordovaPlugin {
  private static final String TAG = "EnseUtilities";
  public static final String PREFS_NAME = "EnseAccountSettings";
  public static final String PREFS_DEVICE_SECRET_KEY = "device_secret_key";
  public static final String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
  public static final int RECORD_AUDIO_REQ_CODE = 0;
  public static MediaRecorder audioRecorder = null;
  public static MediaPlayer audioPlayer = null;
  public static String currentFilePath = null;
  public static CallbackContext currentCallbackContext;
  private static final String AWS_ACCESS_KEY_ID = preferences.getString("AWS_ACCESS_KEY_ID", "");
  private static final String ENSE_API_KEY = preferences.getString("ENSE_API_KEY", "");

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    Log.d(TAG, "Initializing EnseUtilities");
  }

  @Override
  public void onPause(boolean multitasking) {
      super.onPause(multitasking);  // Always call the superclass method first

      // Release the audio recorder because we don't need it when paused
      // and other activities might need to use it.
      if (audioRecorder != null) {
          audioRecorder.release();
          audioRecorder = null;
      }

  }

  @Override
  public void onStop() {
      // call the superclass method first
      super.onStop();
      // Release the audio recorder because we don't need it when paused
      // and other activities might need to use it.
      if (audioRecorder != null) {
          // TODO: Save any recording as a draft for next time
          audioRecorder.release();
          audioRecorder = null;
      }
      if (audioPlayer != null) {
          audioPlayer.release();
          audioPlayer = null;
      }

  }


  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    currentCallbackContext = callbackContext;
    if(action.equals("echo")) {
        String phrase = args.getString(0);
        // Echo back the first argument
        Log.d(TAG, phrase);
    } else if(action.equals("getDate")) {
        // An example of returning data back to the web layer
        final PluginResult result = new PluginResult(PluginResult.Status.OK, (new Date()).toString());
        callbackContext.sendPluginResult(result);
    } else if(action.equals("getDeviceSecretKey")) {
        String deviceSecretKey = getDeviceSecretKey();
        final PluginResult result = new PluginResult(PluginResult.Status.OK, deviceSecretKey);
        callbackContext.sendPluginResult(result);
    } else if(action.equals("startRecording")) {
        if(cordova.hasPermission(RECORD_AUDIO)) {
          try {
              currentFilePath = setupRecorder();
              audioRecorder.start();
              final PluginResult result = new PluginResult(PluginResult.Status.OK, currentFilePath);
              callbackContext.sendPluginResult(result);
          } catch (IOException e) {
              e.printStackTrace();
          }
        } else {
              cordova.requestPermission(this, RECORD_AUDIO_REQ_CODE, RECORD_AUDIO);
        }
    } else if(action.equals("stopRecording")) {
        try {
            audioRecorder.stop();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        final PluginResult result = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(result);
      } else if(action.equals("playAudioFile")) {
          String filePath = args.getString(0);
          audioPlayer = MediaPlayer.create(this.cordova.getContext(), Uri.parse(filePath));
          audioPlayer.setOnPreparedListener((MediaPlayer preppedMediaPlayer) -> preppedMediaPlayer.start());
          audioPlayer.prepareAsync();
          final PluginResult result = new PluginResult(PluginResult.Status.OK);
          callbackContext.sendPluginResult(result);
      } else if(action.equals("pauseAudioPlayback")) {
          audioPlayer.pause();
          final PluginResult result = new PluginResult(PluginResult.Status.OK);
          callbackContext.sendPluginResult(result);
      } else if(action.equals("resumeAudioPlayback")) {
          audioPlayer.start();
          final PluginResult result = new PluginResult(PluginResult.Status.OK);
          callbackContext.sendPluginResult(result);
      } else if(action.equals("stopAudioPlayback")) {
          audioPlayer.stop();
          final PluginResult result = new PluginResult(PluginResult.Status.OK);
          callbackContext.sendPluginResult(result);
      } else if(action.equals("getPlaybackPosition")) {
          final PluginResult result = new PluginResult(PluginResult.Status.OK, audioPlayer.getCurrentPosition());
          callbackContext.sendPluginResult(result);
      } else if(action.equals("getPlaybackDuration")) {
          final PluginResult result = new PluginResult(PluginResult.Status.OK, audioPlayer.getDuration());
          callbackContext.sendPluginResult(result);
      } else if(action.equals("uploadFile")) {
          cordova.getThreadPool().execute(new Runnable() {
            public void run() {
              String fileURL = uploadFile(args.getString(0), args.getString(1), args.getString(2), args.getString(3), args.getString(4))
              final PluginResult result = new PluginResult(PluginResult.Status.OK, fileURL);
              callbackContext.sendPluginResult(result);
            }
          }
      } else if(action.equals("logOut")) {
          final PluginResult result = new PluginResult(PluginResult.Status.OK);
          callbackContext.sendPluginResult(result);
      }
    return true;
  }

  public String getDeviceSecretKey () {
 //retrieve or request device secret key
     SharedPreferences prefs = this.cordova.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
     String deviceSecretKey = prefs.getString(PREFS_DEVICE_SECRET_KEY, null);
     if (deviceSecretKey != null && !deviceSecretKey.isEmpty()) {
       return deviceSecretKey;
     } else {
         //request new key from API
         return AJAX.post("https://api.ense.nyc/device/register", AJAX.m("api_key", ENSE_API_KEY), new AJAX.X() {
             public String success(int code, final String data) {
                     SharedPreferences.Editor editor = cordova.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                     editor.putString(PREFS_DEVICE_SECRET_KEY, data);
                     editor.apply();
                     return data;
             }
             public void failure(int code, final String data) {
                 //somehow handle not having a device key at this point
             }
         });
     }
  }

  public String setupRecorder () throws IOException {
    audioRecorder = new MediaRecorder();
    audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    audioRecorder.setAudioSamplingRate(44100);
    audioRecorder.setAudioEncodingBitRate(96000);
    String filepath = this.cordova.getContext().getFilesDir().getPath() + "/" + System.currentTimeMillis() + ".m4a";
    audioRecorder.setOutputFile(filepath);
    audioRecorder.prepare();
    return filepath;

  }

  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                           int[] grantResults) throws JSONException {
      for(int r:grantResults) {
          if(r == PackageManager.PERMISSION_DENIED) {
              this.currentCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "PERMISSION_DENIED_ERROR"));
              return;
          }
      }
      switch(requestCode) {
          case RECORD_AUDIO_REQ_CODE:
              try {
                  currentFilePath = setupRecorder();
                  audioRecorder.start();
                  this.currentCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, currentFilePath));
              } catch (IOException e) {
                  e.printStackTrace();
              }
              break;
      }
  }


public static void optimizeFileForStreaming(String uploadFilePath) {
  File sourceFile = new File(uploadFilePath);
  String fileName = sourceFile.getName();
  File recordingFile = new File(uploadFilePath);
  File optimizedRecordingFile = new File(uploadFilePath+".optimized");
  try {
      QtFastStart.fastStart(recordingFile, optimizedRecordingFile);
  } catch (Exception e) {
      // Handle
      e.printStackTrace();
  }
  optimizedRecordingFile.renameTo(sourceFile);

}
  public static String uploadFile(String uploadFilePath, String mimetype, String uploadKey, String policyDoc, String policySig) {
      optimizeFileForStreaming(uploadFilePath);
      File audioFile = new File(uploadFilePath);
      if (!audioFile.isFile()) {
          Log.e("uploadFile", "Source File not exist :"
                  +uploadFilePath);
          return null;
      } else {
          try {
              String uploadUrlString = "https://s3.amazonaws.com/media.ense.nyc/";
              // Open a HTTP  connection to  the URL
              HttpsURLConnection connection = (HttpsURLConnection) new URL(uploadUrlString).openConnection();
              String boundary = "---------------------------boundary";
              String tail = "\r\n--" + boundary + "--\r\n";
              connection.setDoOutput(true); // Allow Outputs
              connection.setUseCaches(false); // Don't use a Cached Copy
              connection.setRequestMethod("POST");
              connection.setRequestProperty("Connection", "Keep-Alive");
              connection.setRequestProperty("ENCTYPE", "multipart/form-data");
              connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
              connection.setDoOutput(true);

              StringBuffer metadataPart = new StringBuffer();
              Map<String, String> params = new HashMap<>();
              params.put("key", uploadKey);
              params.put("acl", "public-read");
              params.put("Content-Type", mimetype);
              params.put("AWSAccessKeyId", AWS_ACCESS_KEY_ID);
              params.put("Policy", policyDoc);
              params.put("Signature", policySig);

              for (Map.Entry<String, String> pair : params.entrySet())
              {
                  metadataPart.append("--" + boundary + "\r\n");
                  metadataPart.append("Content-Disposition: form-data; name=\"" + pair.getKey() + "\"\r\n\r\n");
                  metadataPart.append(pair.getValue() + "\r\n");
              }

              String fileHeader1 = "--" + boundary + "\r\n"
                      + "Content-Disposition: form-data; name=\"file\"; filename=\""
                      + audioFile.getName() + "\"\r\n"
                      + "Content-Type: application/octet-stream\r\n"
                      + "Content-Transfer-Encoding: binary\r\n";

              long fileLength = audioFile.length() + tail.length();
              String fileHeader2 = "Content-length: " + fileLength + "\r\n";
              String fileHeader = fileHeader1 + fileHeader2 + "\r\n";
              String stringData = metadataPart + fileHeader;

              long requestLength = stringData.length() + fileLength;
              connection.setRequestProperty("Content-length", "" + requestLength);
              connection.setFixedLengthStreamingMode((int) requestLength);
              connection.connect();

              DataOutputStream out = new DataOutputStream(connection.getOutputStream());
              out.writeBytes(stringData);
              out.flush();

              int bytesRead;
              byte buf[] = new byte[1024];
              BufferedInputStream bufInput = new BufferedInputStream(new FileInputStream(audioFile));
              while ((bytesRead = bufInput.read(buf)) != -1) {
                  // write output
                  out.write(buf, 0, bytesRead);
                  out.flush();
              }

              // Write closing boundary and close stream
              out.writeBytes(tail);
              out.flush();
              out.close();

              // Responses from the server (code and message)
              int serverResponseCode = connection.getResponseCode();

              Log.i("uploadFile", "HTTP Response is : "
                      + connection.getResponseMessage() + ": " + serverResponseCode);

              String result = null;
              if(serverResponseCode >= 200 && serverResponseCode < 300){
                  result = uploadUrlString + uploadKey;
                  /*
                               TODO: UPLOAD IS COMPLETED
                   */
              } else if(serverResponseCode >= 400) {
                  String response = "";
                  String line;
                  BufferedReader br=new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                  while ((line=br.readLine()) != null) {
                      response+=line;
                  }
                  Log.i("uploader", response);
              }

              return result;
          } catch (MalformedURLException ex) {

              ex.printStackTrace();

              //TODO: upload has failed by url problem

              Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
          } catch (Exception e) {

              e.printStackTrace();

              //TODO: upload has failed in some other way
              Log.e("Upload Exception", "Exception : " + e.getMessage(), e);
          }
          return null;

      } // End else block
  }

}
