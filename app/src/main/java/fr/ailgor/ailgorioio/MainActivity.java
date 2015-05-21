package fr.ailgor.ailgorioio;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public final String TAG = "MainActivity";
    
    private boolean mIsServiceRunning = false;
    private boolean mIsServiceIOIORunning = false;
    private Button mBackgroundButton;
    private Button mIoioButton;
    private Button mLedButton;


    SharedPreferences mSharedPreferences;

    String message = "";
    ServerSocket serverSocket;
    Socket clientSocket;
    int SocketServerPORT = 8090;
    String messageCommand = "";
    String messageReply = "Message Received";
    private TextView textClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBackgroundButton = (Button) findViewById(R.id.backgroundButton);
        mIoioButton = (Button) findViewById(R.id.ioioButton);
        mLedButton = (Button) findViewById(R.id.ledButton);

        if (!initialize()) {
            Toast.makeText(this, "Can not initialize parameters", Toast.LENGTH_LONG).show();
        }

        mLedButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {


                if ((event.getAction() == MotionEvent.ACTION_MOVE)||(event.getActionMasked()!=MotionEvent.ACTION_UP)&&(mIsServiceIOIORunning))
                {
                    AilgorIOIOService.ledSpeed(true);
                }
                else
                {
                    AilgorIOIOService.ledSpeed(false);
                }
                return false;
            }
        });


        textClient = (TextView) findViewById(R.id.textClient);
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

    }
    
    public void onResume() {
        super.onResume();
        
        mIsServiceRunning = isServiceRunning();
        updateButtonCamera(mIsServiceRunning);

        mIsServiceIOIORunning = isServiceIOIORunning();
        updateButtonIOIO(mIsServiceIOIORunning);
    }
    
    public void onStop() {
        super.onStop();
        
        if (mIsServiceRunning) {
            finish();
        }
        if (mIsServiceIOIORunning) {
            finish();
        }
    }

    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onButtonClick(View view) {
        switch (view.getId()) {
        case R.id.settingsButton:
            startActivity(new Intent(this , SettingsActivity.class));
            break;

        case R.id.backgroundButton:
            if (!mIsServiceRunning) {
                //doBindService();
                startService(new Intent(this, BackgroundService.class));
                mIsServiceRunning = true;
            } else {
                //doUnbindService();
                stopService(new Intent(this, BackgroundService.class));
                mIsServiceRunning = false;
            }
            updateButtonCamera(mIsServiceRunning);
            break;
        case R.id.ioioButton:
            if (! mIsServiceIOIORunning) {
                //doBindService();
                startService(new Intent(this, AilgorIOIOService.class));
                mIsServiceIOIORunning= true;
            } else {
                //doUnbindService();
                stopService(new Intent(this, AilgorIOIOService.class));
                mIsServiceIOIORunning = false;
            }
            updateButtonIOIO(mIsServiceIOIORunning);
            break;
        }
    }
    
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ((BackgroundService.class.getName()).equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isServiceIOIORunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ((AilgorIOIOService.class.getName()).equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    private void updateButtonCamera(boolean state) {
        if (state) {
            mBackgroundButton.setText(R.string.stop_running);
        } else {
            mBackgroundButton.setText(R.string.run_background);
        }
    }

    private void updateButtonIOIO(boolean state) {
        if (state) {
            mIoioButton.setText(R.string.stop_ioio);
        } else {
            mIoioButton.setText(R.string.run_ioio);
        }
    }

    private boolean initialize() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
       boolean firstRun = ! mSharedPreferences.contains("settings_camera");
       if (firstRun) {
           Log.v(TAG, "First run");
           
           SharedPreferences.Editor editor = mSharedPreferences.edit();
           
           int cameraNumber = Camera.getNumberOfCameras();
           Log.v(TAG, "Camera number: " + cameraNumber);
           
           /*
            * Get camera name set 
            */
           TreeSet<String> cameraNameSet = new TreeSet<String>();
           if (cameraNumber == 1) {
               cameraNameSet.add("back");
           } else if (cameraNumber == 2) {
               cameraNameSet.add("back");
               cameraNameSet.add("front");
           } else if (cameraNumber > 2) {           // rarely happen
               for (int id = 0; id < cameraNumber; id++) {
                   cameraNameSet.add(String.valueOf(id));
               }
           } else {                                 // no camera available
               Log.v(TAG, "No camrea available");
               Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show();
               
               return false;
           }

           /* 
            * Get camera id set
            */
           String[] cameraIds = new String[cameraNumber];
           TreeSet<String> cameraIdSet = new TreeSet<String>();
           for (int id = 0; id < cameraNumber; id++) {
               cameraIdSet.add(String.valueOf(id));
           }
           
           /*
            * Save camera name set and id set
            */
           editor.putStringSet("camera_name_set", cameraNameSet);
           editor.putStringSet("camera_id_set", cameraIdSet);
           
           /*
            * Get and save camera parameters
            */
           for (int id = 0; id < cameraNumber; id++) {
               Camera camera = Camera.open(id);
               if (camera == null) {
                   String msg = "Camera " + id + " is not available";
                   Log.v(TAG, msg);
                   Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                   
                   return false;
               }
               
               Parameters parameters = camera.getParameters();
               
               /*
                * Get and save preview sizes
                */
               List<Size> sizes = parameters.getSupportedPreviewSizes();
               
               TreeSet<String> sizeSet = new TreeSet<String>(new Comparator<String>() {
                   @Override
                   public int compare(String s1, String s2) {
                       int spaceIndex1 = s1.indexOf(" ");
                       int spaceIndex2 = s2.indexOf(" ");
                       int width1 = Integer.parseInt(s1.substring(0, spaceIndex1));
                       int width2 = Integer.parseInt(s2.substring(0, spaceIndex2));
                       
                       return width2 - width1;
                   }
               });
               for (Size size : sizes) {
                   sizeSet.add(size.width + " x " + size.height);
               }
               editor.putStringSet("preview_sizes_" + id, sizeSet);
               
               Log.v(TAG, sizeSet.toString());
               
               /*
                * Set default preview size, use camera 0
                */
               if (id == 0) {
                   Log.v(TAG, "Set default preview size");
                   
                   Size defaultSize = parameters.getPreviewSize();
                   editor.putString("settings_size", defaultSize.width + " x " + defaultSize.height);
               }
               
               /*
                * Get and save 
                */
               List<int[]> ranges = parameters.getSupportedPreviewFpsRange();
               TreeSet<String> rangeSet = new TreeSet<String>();
               for (int[] range : ranges) {
                   rangeSet.add(range[0] + " ~ " + range[1]);
               }
               editor.putStringSet("preview_ranges_" + id, rangeSet);
               
               if (id == 0) {
                   Log.v(TAG, "Set default fps range");
                   
                   int[] defaultRange = new int[2];
                   parameters.getPreviewFpsRange(defaultRange);
                   editor.putString("settings_range", defaultRange[0] + " ~ " + defaultRange[1]);
               }
               
               camera.release();
               
           }
           
           editor.putString("settings_camera", "0");
           editor.commit();
       }
       
       return true;
    }

    private class SocketServerThread extends Thread {

        public void run() {
            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                MainActivity.this.runOnUiThread(new Runnable() {

                    public void run() {
                        Toast.makeText(MainActivity.this, "IOIO Server Run : "
                                +serverSocket.getInetAddress()
                                +":"
                                +serverSocket.getLocalPort()
                                , Toast.LENGTH_SHORT).show();
                    }
                });

                while (true) {
                    clientSocket = serverSocket.accept();
                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread();
                    socketServerReplyThread.run();


                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            textClient.setText(messageCommand);
                            AilgorIOIOService.messageCommandClient(messageCommand);
                        }
                    });

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private class SocketServerReplyThread extends Thread {

        public void run() {

            messageCommand = "";
            try {

                OutputStream outputStream = clientSocket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                dataOutputStream.writeUTF(messageReply);

                InputStream inputStream = clientSocket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                messageCommand = dataInputStream.readUTF();

            } catch (UnknownHostException e) {
                e.printStackTrace();
                message += "UnknownHostException" + e.toString() + "\n";
                Log.v(TAG, message);
            } catch (IOException e) {
                e.printStackTrace();
                message += "IOException " + e.toString() + "\n";
                Log.v(TAG, message);
            }finally{
                if(clientSocket != null){
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

}
