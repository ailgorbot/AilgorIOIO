package fr.ailgor.ailgorioio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

public class AilgorIOIOService extends IOIOService {

    public final String TAG = "AilgorIOIOService";

    public static int speed_=500;

    String message = "Not connected";
    ServerSocket serverSocket;
    Socket clientSocket;
    int SocketServerPORT = 8090;
    String messageCommand = "STOP";
    String messageReply = "Message Received";
    Handler handler;



    // Declare PIN
    private DigitalOutput PinDO0; // LED STAT

    // Motor DC : Right
    private DigitalOutput PinDIO36; // L293D In 1
    private DigitalOutput PinDIO37; // L293D In 2
    private PwmOutput PinPWM38; // L293D Enable 1

    // Motor DC : Left
    private DigitalOutput PinDIO40; // L293D In 3
    private DigitalOutput PinDIO41; // L293D In 4
    private PwmOutput PinPWM39; // L293D Enable 2

    private static boolean motorLeft=false;
    private static boolean motorRight=false;
    private static float RCSpeed = 0;


    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            private DigitalOutput led_;

            @Override
            protected void setup() throws ConnectionLostException,
                    InterruptedException {
                PinDO0 = ioio_.openDigitalOutput(IOIO.LED_PIN);

                // Motor DC : Right
                PinDIO36 = ioio_.openDigitalOutput(36);
                PinDIO37 = ioio_.openDigitalOutput(37);
                PinPWM38 = ioio_.openPwmOutput(38,100);


                // Motor DC : Left
                PinDIO40 = ioio_.openDigitalOutput(40);
                PinDIO41 = ioio_.openDigitalOutput(41);
                PinPWM39 = ioio_.openPwmOutput(39,100);
            }

            @Override
            public void loop() throws ConnectionLostException,
                    InterruptedException {


                PinDO0.write(true);
                if (speed_==100) {
                    PinDO0.write(false);
                    Thread.sleep(speed_);
                    PinDO0.write(true);
                    Thread.sleep(speed_);
                }

                PinPWM39.setDutyCycle(RCSpeed);
                PinDIO41.write(motorLeft);
                PinDIO40.write(!motorLeft);

                PinPWM38.setDutyCycle(RCSpeed);
                PinDIO37.write(motorRight);
                PinDIO36.write(!motorRight);
            }
        };
    }


    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        String  port = "8090";

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent != null && intent.getAction() != null
                && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            nm.cancel(0);
            stopSelf();
        } else {
            // Service starting. Create a notification.
            Notification notification = new Notification(
                    R.drawable.ic_launcher, "IOIO service running",
                    System.currentTimeMillis());
            notification
                    .setLatestEventInfo(this, "IOIO Service", "Click to stop",
                            PendingIntent.getService(this, 0, new Intent(
                                    "stop", null, this, this.getClass()), 0));
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            nm.notify(0, notification);

            }
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    public static void ledSpeed(boolean speed)
    {
        if (speed)
            speed_=100;
        else
            speed_=500;
    }

    public static void messageCommandClient(String commandClient)
    {
        switch (commandClient) {
            case "STOP":
                speed_=500;
                RCSpeed = 0;
                break;
            case "IOIOLed":
                speed_=100;
                break;
            case "RCDOWN":
                motorLeft = false;
                motorRight = true;
                RCSpeed = 1;
                break;
            case "RCUP":
                motorLeft = true;
                motorRight = false;
                RCSpeed = 1;
                break;
            case "RCLEFT":
                motorLeft = true;
                motorRight = true;
                RCSpeed = 1;
                break;
            case "RCRIGHT":
                motorLeft = false;
                motorRight = false;
                RCSpeed = 1;
                break;
        }
    }

}
