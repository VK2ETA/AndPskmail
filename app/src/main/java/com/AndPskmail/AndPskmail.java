/*
 * AndPskmail.java
 *   
 * Copyright (C) 2011 John Douyere (VK2ETA)  
 *   
 * This program is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  
 * GNU General Public License for more details.  
 *   
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 */

package com.AndPskmail;

/**
 *
 * @author John Douyere <vk2eta@gmail.com>
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.Time;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

//GPS stuff
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus;

//Audio
import android.media.AudioManager;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

//Contact picker
import android.provider.ContactsContract.Contacts;

//screen transitions
import android.view.animation.*;

//Support library for runtime permissions and notifications
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;



//public class AndPskmail extends Activity {
public class AndPskmail extends AppCompatActivity {

    private static boolean havePassedAllPermissionsTest = false;

    public static Context myContext;

    public static AndPskmail myInstance = null;

    public static Window myWindow = null;

    public static boolean RXParamsChanged = false;

    public static SharedPreferences mysp = null;

    // Last mail screen used (header, inbox, outbox, etc...)
    private int lastEmailScreen = MAILHEADERSVIEW; //default to start with
    private static ArrayList<email> emaillist;
    private static LayoutInflater inflater = null;
    private PopupWindow pw;
    private static String welcomeString = "\n Welcome to AndPskmail " + Processor.version + "\n\n This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.\n\n Swipe across the screen to navigate to the other screens and use the device Menu button to get access to the preferences and additional functions.\n\n Visit www.pskmail.org for more details\n\n 73, The Pskmail Team \n\n";
    private static boolean hasDisplayedWelcome = false;
    private String beaconsAndLinksTime = "";

    //Horizontal Fling detection despite scrollview
    private GestureDetector mGesture;

    // Views values
    private final static int TERMVIEW = 1;
    private final static int APRSVIEW = 2;
    private final static int MODEMVIEWnoWF = 3;
    private final static int MODEMVIEWwithWF = 4;
    private final static int INFOREQUESTVIEW = 5;
    private final static int ABOUTVIEW = 6;
    private final static int MAILHEADERSVIEW = 7;
    private final static int MAILINBOXVIEW = 8;
    private final static int MAILOUTBOXVIEW = 9;
    private final static int MAILSENTITEMSVIEW = 10;
    private final static int NEWMAILVIEW = 11;

    //screen transitions / animations
    private static final int NORMAL = 0;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int TOP = 3;
    private static final int BOTTOM = 4;


    static ProgressBar SignalQuality = null;
    static ProgressBar CpuLoad = null;

    static int currentview = TERMVIEW;

    //Toast text display (declared here to allow for quick replacement rather than queuing)
    private Toast myToast;


    // Layout Views
    private static TextView myTermTV;
    private static ScrollView myTermSC;
    private static TextView myModemTV;
    private static ScrollView myModemSC;
    private static waterfallView myWFView;

    private static TextView mAPRSView;
    private static ScrollView mAPRSSV;
    private ListView mMailListView;
    private ListView WebHeadersView;
    // Generic button variable. Just for callback initialisation
    private Button myButton;

    public static LocationManager locationManager;
    public GPSListener locationListener = new GPSListener();
    public static boolean GPSisON = false;
    public static boolean GPSTimeAcquired = false;
    public static int DeviceToGPSTimeCorrection = 0;

    public static String TerminalBuffer = "";
    public static String ModemBuffer = "";
    public static String APRSBuffer = "";
    public static config myconfig = null;

    public static String serverToCall = "";
    public static String serverAccessPassword = "";

    //Saved APRS text (APRS status message)
    private static String savedAprsMessage = "";
    private static Time nextBeaconTime = null;
    private static Time nextAutolinkTime = null;
    private CheckBox checkbox = null;

    // Member object for processing of Rx and Tx
    // Can be stopped (i.e no RX) to save battery and allow Android to reclaim
    // resources if not visible to the user
    public static boolean ProcessorON = false;
    private static boolean modemPaused = false;

    //Notifications
    private NotificationManager myNotificationManager;
    public static Notification myNotification = null;

    // Array adapter for the APRS messages
    public static ArrayAdapter<String> APRSArrayAdapter;

    // Array adapter for the headers' list
    public static ArrayAdapter<String> MailArrayAdapter;

    // Need handler for callbacks to the UI thread
    public static final Handler mHandler = new Handler();

    // Array adapter for the list of web pages
    public static ArrayAdapter<String> WebArrayAdapter;

    //Contact email picker
    private static final int CONTACT_PICKER_RESULT = 10101;

    // Bluetooth handfree / headset
    public static boolean scoChannelOn = false;
    public static boolean toBluetooth = false;
    public static boolean deviceJustConnected = false;
    public static AudioManager mAudioManager;
    public static BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 1;
    //To monitor the incoming calls and disconnect Bluetooth so that we donlt send the phone call audio to the radio
    private TelephonyManager tmgr = null;
    //Bluetooth Audio devices
    public static BroadcastReceiver mReceiver = null;

    //  Broadcast receiver for Bluetooth  broadcasts
    //	private final BroadcastReceiver myBroadcastReceiver = new mBroadcastReceiver();


	/* Parked code for later
	// Need to start an audio channel with the Bluetooth headset/handsfree
	// Either use startVoiceRecognition() (V3.0 onwards)
	// OR
	// StartBluetoothSCO() (V2.2 Onwards)
	 * BluetoothHeadset mBluetoothHeadset;
	 *
	 * private BluetoothHeadset.ServiceListener mBluetoothHeadsetServiceListener
	 * = new BluetoothHeadset.ServiceListener() { public void
	 * onServiceConnected() { if (mBluetoothHeadset != null &&
	 * mBluetoothHeadset.getState() == BluetoothHeadset.STATE_CONNECTED) { //
	 * Log.e(TAG, "######### STARTING VOICE RECOGNITION ############");
	 * mBluetoothHeadset.startVoiceRecognition();
	 * mAudioManager.setMode(AudioManager.MODE_IN_CALL); } } public void
	 * onServiceDisconnected() { int aaa = 0; }
	 *
	 * };
	 */

    private static final LocationListener mLocationListener = new LocationListener() {


        public void onLocationChanged(Location location) {
            //        Log.i("TestGPSOnSecondThread", "onLocationChanged, location = " + 			location);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    // Handler for periodic beacons and link requests
    public static Handler myHandler;

    //	public static LocationManager autoBeaconlocationManager = null;

    // Runnable for preparing the GPS one minute before sending the beacon
    public static final Runnable prepareGPSforBeacon = new Runnable() {
        public void run() {
            //We arrived here 1 minute before the scheduled beacon TX
            //First, clear the queue of beacon send runnables
            myHandler.removeCallbacks(prepareGPSforBeacon);
            //check that we still want to send
            boolean autobeacon = AndPskmail.myconfig.getPreferenceB("BEACON", false);
            if (autobeacon) {
                //Start the internal GPS and wait 1 minute for a GPS lock
                locationManager = (LocationManager) AndPskmail.myContext.getSystemService(Context.LOCATION_SERVICE);
                if (ContextCompat.checkSelfPermission(myInstance,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, 30000, // 30 sec in milisecs
                            50, // meters
                            mLocationListener);
                }
                //Clear the queue of beacon send runnables
                myHandler.removeCallbacks(sendAndRequeueBeacon);
                //Schedule the beacon TX in 60 seconds (give or take the delay of this runnable)
                myHandler.postDelayed(sendAndRequeueBeacon, 60000);
            } else {
                //We must have de-selected the auto-beacons in the mean time
                //Remove all queued beacons
                myHandler.removeCallbacks(sendAndRequeueBeacon);
            }
        }
    };

    // Runnable for sending beacons
    public static final Runnable sendAndRequeueBeacon = new Runnable() {
        public void run() {
            //Clear the queue of beacon send runnables
            myHandler.removeCallbacks(sendAndRequeueBeacon);
            //check that we still want to send
            boolean autobeacon = AndPskmail.myconfig.getPreferenceB("BEACON", false);
            if (autobeacon) {
                try {
                    // if we are connected or have TX activity, skip this Transmission
                    if (!Processor.TXActive && !Processor.Connected && !Processor.Connecting
                            && !(Processor.Status == "Disconnecting")) {
                        Processor.q.send_beacon(AndPskmail.savedAprsMessage);
                    }
                    //Enqueue the next one
                    long delay = nextBeaconOrLinkDelay(true);
                    myHandler.postDelayed(prepareGPSforBeacon, delay - 60000); //remove 1 minute for the GPS fix
                    // Stop the GPS if running
                    if (locationManager != null) {
                        locationManager.removeUpdates(mLocationListener);
                        //						locationManager = null;
                    }

                } catch (Exception ex) {
                    loggingclass.writelog("Periodic Beacon Send Execution Error: " + ex.getMessage(), null, true);
                }
            }
        }
    };

    // Runnable for sending link requests
    public static final Runnable sendAndRequeueLink = new Runnable() {
        public void run() {
            //Clear the queue of link requests runnables
            myHandler.removeCallbacks(sendAndRequeueLink);
            boolean autolink = AndPskmail.myconfig.getPreferenceB("AUTOLINK", false);
            if (autolink) {
                try {
                    // if we are connected or have TX activity, skip this Transmission
                    if (!Processor.TXActive && !Processor.Connected && !Processor.Connecting
                            && !(Processor.Status == "Disconnecting")) {
                        Processor.q.send_link();
                    }
                    //Enqueue the next one
                    myHandler.postDelayed(sendAndRequeueLink, nextBeaconOrLinkDelay(false));
                } catch (Exception ex) {
                    loggingclass.writelog("Periodic Link Request Send Execution Error: " + ex.getMessage(), null, true);
                }
            }
        }
    };


    // Listener for changes in preferences
    public static OnSharedPreferenceChangeListener splistener;

    // Create runnable for updating the waterfall display
    public static final Runnable updatewaterfall = new Runnable() {
        public void run() {
            if (myWFView != null) {
                myWFView.invalidate();
            }
        }
    };


    // Create runnable for changing the main window's title
    public static final Runnable updatetitle = new Runnable() {
        public void run() {
            if (myWindow != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    if (ProcessorON) {
                        if (Processor.TXActive) {
                            myInstance.getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFF00\">"
                                    + Processor.Status + "/R "
                                    + Processor.RxModem.toString() + "/T "
                                    + Processor.TxModem.toString()
                                    + "</font>")));
                        } else {
                            //myWindow.setTitleColor(Color.CYAN);
                            myInstance.getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#33D6FF\">"
                                    + Processor.Status + "/R "
                                    + Processor.RxModem.toString() + "/T "
                                    + Processor.TxModem.toString()
                                    + "</font>")));
                        }

                    } else {
                        //myWindow.setTitleColor(Color.WHITE);
                        myInstance.getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFFFF\">"
                                + Processor.Status + "/R "
                                + Processor.RxModem.toString() + "/T "
                                + Processor.TxModem.toString()
                                + "</font>")));
                    }

                } else {
                    //For
                    myWindow.setTitle(Processor.Status + "/R "
                            + Processor.RxModem.toString() + "/T "
                            + Processor.TxModem.toString());
                    if (ProcessorON) {
                        if (Processor.TXActive) {
                            myWindow.setTitleColor(Color.YELLOW);
                        } else {
                            myWindow.setTitleColor(Color.CYAN);
                        }

                    } else {
                        myWindow.setTitleColor(Color.WHITE);
                    }
                }
            }
        }
    };

    // Runnable for updating the signal quality bar in Modem Window
    public static final Runnable updatesignalquality = new Runnable() {
        public void run() {
            if ((SignalQuality != null)
                    && ((currentview == MODEMVIEWnoWF) || (currentview == MODEMVIEWwithWF))) {
                SignalQuality.setProgress((int) Processor.avgsnr);
                SignalQuality
                        .setSecondaryProgress((int) Processor.m.squelch);
            }
        }
    };

    // Runnable for updating the CPU load bar in Modem Window
    public static final Runnable updatecpuload = new Runnable() {
        public void run() {
            if ((CpuLoad != null)
                    && ((currentview == MODEMVIEWnoWF) || (currentview == MODEMVIEWwithWF))) {
                CpuLoad.setProgress((int) Processor.cpuload);
            }
        }
    };

    // Create runnable for posting to monitor window
    public static final Runnable addtoAPRS = new Runnable() {
        public void run() {
            if (mAPRSView != null) {
                mAPRSView.append(Processor.APRSwindow);
                APRSBuffer += Processor.APRSwindow;
                if (APRSBuffer.length() > 60000)
                    APRSBuffer = APRSBuffer.substring(5000);
                Processor.APRSwindow = "";
                // Then scroll to the bottom
                if (mAPRSSV != null) {
                    mAPRSSV.post(new Runnable() {
                        public void run() {
                            mAPRSSV.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        }
    };


    // Create runnable for posting to terminal window
    public static final Runnable addtoterminal = new Runnable() {
        public void run() {
            // myTV.setText(Processor.mainwindow);
            if (myTermTV != null) {
                myTermTV.append(Processor.mainwindow);
                TerminalBuffer += Processor.mainwindow;
                if (TerminalBuffer.length() > 60000)
                    TerminalBuffer = TerminalBuffer.substring(5000);
                Processor.mainwindow = "";
                // Then scroll to the bottom
                if (myTermSC != null) {
                    myTermSC.post(new Runnable() {
                        public void run() {
                            myTermSC.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        }
    };

    // Create runnable for posting to modem window
    public static final Runnable addtomodem = new Runnable() {
        public void run() {
            // myTV.setText(Processor.mainwindow);
            if (myModemTV != null) {
                myModemTV.append(Processor.monitor);
                ModemBuffer += Processor.monitor;
                if (ModemBuffer.length() > 60000)
                    ModemBuffer = ModemBuffer.substring(5000);
                Processor.monitor = "";
                // Then scroll to the bottom
                if (myModemSC != null) {
                    myModemSC.post(new Runnable() {
                        public void run() {
                            myModemSC.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        }
    };


    // Calculates the delay between now and the next beacon or Link
    // Parameter beacon: true = beacon, false = link (in the next 5 minutes
    // block and at fixed 20 minutes intervals or less)
    public static long nextBeaconOrLinkDelay(boolean beacon) {
        int targetPeriod = 0;
        int secondsDelay = 0;

        long nowInMilli = System.currentTimeMillis();
        //apply gps time correction if enbled and we have a valid GPS time
        if (AndPskmail.myconfig.getPreferenceB("USEGPSTIME", false)) {
            if (GPSTimeAcquired) {
                nowInMilli += (DeviceToGPSTimeCorrection * 1000); // All in milliseconds
            }
        }
        long timeTarget = nowInMilli;
        Time mytime = new Time();
        mytime.set(timeTarget); //initialized to now
        targetPeriod = Integer.parseInt(AndPskmail.myconfig.getPreference(
                "BEACONPERIOD", "30")); // 30 minutes beacon by default
        //If set to 5 minutes, and both beacon and auto-link are ON, reset to 10 minutes to alternate between the two
        if (targetPeriod == 5 &&
                AndPskmail.myconfig.getPreferenceB("BEACON", false) &&
                AndPskmail.myconfig.getPreferenceB("AUTOLINK", false)) {
                targetPeriod = 10;
        }

        int targetMinute = Integer.parseInt(AndPskmail.myconfig.getPreference(
                "BEACONQRG", "0")); // minutes 0,1,2,3,4
        // Links at 20 minutes fixed period or less if beacon is less than 20
        // minutes
        secondsDelay = Integer.parseInt(AndPskmail.myconfig.getPreference(
                "BEACONSECONDS", "10")); // 10 seconds delay by default
        if (!beacon) {
            //Link period not more than 20 minutes
            targetPeriod = targetPeriod > 20 ? 20 : targetPeriod;
        }
        int minute = mytime.minute;
        //		while ((timeTarget - nowInMilli < 30000)) { // debug version (1 minute interval)
        while ((minute % targetPeriod != targetMinute)
                || (timeTarget - nowInMilli < 2 * 60000)) { // allow 2 minutes for GPS lock before first Tx
            timeTarget += 30000; // one minute in milliseconds
            mytime.set(timeTarget);
            minute = mytime.minute;
        }
        // remove the seconds to re-sync on the first second of the target minute
        timeTarget -= mytime.second * 1000;
        //Add seconds delay (for server change and spacing of beacons)
        timeTarget += (secondsDelay * 1000);
        //Link request are in the next 5 minutes block (5,6,7,8,9)
        if (!beacon) {
            timeTarget += 5 * 60000;
        }
        //Calculate delay from now
        long delay = timeTarget - System.currentTimeMillis();
        mytime.set(timeTarget);
        //apply gps time correction if enbled and we have a valid GPS time
        if (AndPskmail.myconfig.getPreferenceB("USEGPSTIME", false)) {
            if (GPSTimeAcquired) {
                delay -= (DeviceToGPSTimeCorrection * 1000); // All in milliseconds
            }
        }

        //Store for display in terminal (on entry) and APRS screens (on enabling)
        if (beacon) {
            nextBeaconTime = mytime;
        } else {
            nextAutolinkTime = mytime;
        }
        return delay;
    }


    //Phone state listener to disable Bluetooth when receiving a call
    //This is to prevent the phone call's audio to be sent to the radio,
    // as well as stopping the TXing from AndPskmail until the phone call is finished
    //This action is only active when we have enabled Bluetooth in the AndPskmail application
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                //Log.v(TAG, " CALL_STATE_RINGING");
                //If using the bluetooth interface for the digital link,
                //  disable it otherwise the phone call will use it.
                if (AndPskmail.toBluetooth) {
                    AndPskmail.toBluetooth = false;
                    AndPskmail.mAudioManager.setMode(AudioManager.MODE_NORMAL);
                    // mBluetoothHeadset.stopVoiceRecognition();
                    // AndPskmail.mAudioManager.setMode(AudioManager.MODE_NORMAL);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { //Froyo bug
                        AndPskmail.mAudioManager.stopBluetoothSco();
                        if (AndPskmail.mBluetoothAdapter != null) {
                            if (AndPskmail.mBluetoothAdapter.isEnabled()) {
                                AndPskmail.mBluetoothAdapter.disable();
                            }
                        }
                    }
                    AndPskmail.mAudioManager.setBluetoothScoOn(false);
                    //    				AndPskmail.mAudioManager.setSpeakerphoneOn(false);
                }

            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                //Log.v(TAG, " CALL_STATE_IDLE");

            }
        }
    };


    public void DisplayTime() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (currentview == APRSVIEW || currentview == TERMVIEW) {
                    try {
                        TextView txtCurrentTime = (TextView) findViewById(R.id.minsec);
                        long nowInMilli = System.currentTimeMillis();
                        Time mytime = new Time();
                        //Not using GPS time? Make sure the variables are reset
                        if (!AndPskmail.myconfig.getPreferenceB("USEGPSTIME", false)) {
                            DeviceToGPSTimeCorrection = 0;
                            GPSTimeAcquired = false;
                        }
                        mytime.set(nowInMilli + (DeviceToGPSTimeCorrection * 1000)); //now +/- GPS correction if any
                        String MinutesStr = "00" + mytime.minute;
                        MinutesStr = MinutesStr.substring(MinutesStr.length() - 2, MinutesStr.length());
                        String SecondsStr = "00" + mytime.second;
                        SecondsStr = SecondsStr.substring(SecondsStr.length() - 2, SecondsStr.length());
                        txtCurrentTime.setText(MinutesStr + ":" + SecondsStr);
                        if (GPSTimeAcquired) {
                            txtCurrentTime.setTextColor(Color.GREEN);
                        } else {
                            txtCurrentTime.setTextColor(Color.YELLOW);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        });
    }


    class DisplayTimeRunner implements Runnable {
        // @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DisplayTime();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                }
            }
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = super.dispatchTouchEvent(ev);
        handled = mGesture.onTouchEvent(ev);
        return handled;
    }


    private SimpleOnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener() {
        private float xDistance, yDistance, lastX, lastY;

        @Override
        public boolean onDown(MotionEvent e) {
            xDistance = yDistance = 0f;
            lastX = e.getX();
            lastY = e.getY();
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final float curX = e2.getX();
            final float curY = e2.getY();
            xDistance += (curX - lastX);
            yDistance += (curY - lastY);
            lastX = curX;
            lastY = curY;
            if (Math.abs(xDistance) > Math.abs(yDistance) && Math.abs(velocityX) > 1000) {
                //toastText("fling......");
                NavigateScreens((int) xDistance);
                return false;
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }
    };



    private void returnToLastScreen() {
        switch (currentview) {

            case MODEMVIEWnoWF:
            case MODEMVIEWwithWF:
                displayModem(NORMAL, false);
                break;

            case APRSVIEW:
                displayAPRS(NORMAL);
                break;

            case INFOREQUESTVIEW:
                displayInforequest(NORMAL);
                break;

            case MAILHEADERSVIEW:
            case MAILINBOXVIEW:
            case MAILOUTBOXVIEW:
            case MAILSENTITEMSVIEW:
                displayMail(NORMAL, lastEmailScreen);
                break;

            case NEWMAILVIEW:
                displayNewmail();
                break;

            case TERMVIEW:
                default:
                displayTerminal(NORMAL); //Just in case
        }
    }

    //Swipe (fling) handling to move from screen to screen
    private void NavigateScreens(int FlingDirection) {

        //Navigate between screens by gesture (on top of menu button acces)

        if (FlingDirection > 0) { //swipe/fling right

            switch (currentview) {

                case TERMVIEW:
                    displayMail(RIGHT, lastEmailScreen);
                    break;

                case MODEMVIEWnoWF:
                case MODEMVIEWwithWF:
                    displayTerminal(RIGHT);
                    break;

                case APRSVIEW:
                    displayModem(RIGHT, false);
                    break;

                case INFOREQUESTVIEW:
                    displayAPRS(RIGHT);
                    break;

                case MAILHEADERSVIEW:
                case MAILINBOXVIEW:
                case MAILOUTBOXVIEW:
                case MAILSENTITEMSVIEW:
                    displayInforequest(RIGHT);
                    break;

                case NEWMAILVIEW:
                    //do nothing
                    break;

                default:
                    displayTerminal(RIGHT); //Just in case


            }
        } else { //swipe/fling left
            switch (currentview) {

                case TERMVIEW:
                    displayModem(LEFT, false);
                    break;

                case MODEMVIEWnoWF:
                case MODEMVIEWwithWF:
                    displayAPRS(LEFT);
                    break;

                case APRSVIEW:
                    displayInforequest(LEFT);
                    break;

                case INFOREQUESTVIEW:
                    displayMail(LEFT, lastEmailScreen);
                    break;

                case MAILHEADERSVIEW:
                case MAILINBOXVIEW:
                case MAILOUTBOXVIEW:
                case MAILSENTITEMSVIEW:
                    displayTerminal(LEFT);
                    break;

                case NEWMAILVIEW:
                    //do nothing
                    break;

                default:
                    displayTerminal(LEFT); //Just in case

            }

        }

    }

    public static boolean readContactsPermit = false;
    public static boolean fineLocationPermit = false;
    public static boolean writeExtStoragePermit = false;
    public static boolean recordAudioPermit = false;
    public static boolean readPhoneStatePermit = false;

    private final int REQUEST_PERMISSIONS = 15556;
    public final String[] permissionList = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE};

    //Request permission from the user
    private void requestAllCriticalPermissions() {
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(AndPskmail.this);
        myAlertDialog.setMessage("You are about to be presented with a series of permission requests." +
                "\nAll but the \"Read Contacts\" are mandatory to make this app work as intended." +
                "\nIf you do not allow any of the permissions 2. to 5. the app cannot run and will exit." +
                "\n\nExplanations for the permissions: " +
                "\n1. Read Contacts allows the app to access your contacts email addresses when creating a new email (optional)" +
                "\n2. Access Fine Location: The GPS position and data is used for position reporting and for reading GPs time when " +
                "there is no cellular reception." +
                "\n3. Write External Storage: access to the app's directory on the SD card (internal or external)" +
                "\n4. Record Audio: audio input for the modem" +
                "\n5. Read Phone State: to disconnect the Bluetooth interface when receiving a phone call. Amateur regulations generally " +
                "forbid connecting a person to the telephone network or to do so without a warning." +
                "\n\nIf you have previously denied some critical permissions, you will have to go back to the Device's Settings/Apps and re-allow the missing permission."
        );
        myAlertDialog.setCancelable(false);
        myAlertDialog.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions(myInstance, permissionList, REQUEST_PERMISSIONS);
            }
        });
        myAlertDialog.show();
    }

    private boolean allPermissionsOk() {
        final int granted = PackageManager.PERMISSION_GRANTED;

        //ContextCompat.checkSelfPermission(myContext, Manifest.permission.READ_CONTACTS) == granted &&
        return        ContextCompat.checkSelfPermission(myContext, Manifest.permission.ACCESS_FINE_LOCATION) == granted &&
                ContextCompat.checkSelfPermission(myContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == granted &&
                ContextCompat.checkSelfPermission(myContext, Manifest.permission.RECORD_AUDIO) == granted &&
                ContextCompat.checkSelfPermission(myContext, Manifest.permission.READ_PHONE_STATE) == granted;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int granted = PackageManager.PERMISSION_GRANTED;
        for (int i = 0; i < grantResults.length; i++) {
            if (permissions[i].equals(Manifest.permission.READ_CONTACTS)) {
                readContactsPermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                fineLocationPermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                writeExtStoragePermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.RECORD_AUDIO)) {
                recordAudioPermit = grantResults[i] == granted;
            } else if (permissions[i].equals(Manifest.permission.READ_PHONE_STATE)) {
                readPhoneStatePermit = grantResults[i] == granted;
            } else {
                //Nothing so far
            }
        }
        //Re-do overall check
        havePassedAllPermissionsTest = allPermissionsOk();
        if (havePassedAllPermissionsTest &&
                requestCode == REQUEST_PERMISSIONS) { //Only if requested at OnCreate time
            performOnCreate();
            performOnStart();
        }  else {
            //Close that activity and return to previous screen
            finish();
            //Kill the process
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }



    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //Avoid app restart if already running and pressing on the app icon again
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                //Log.w(LOG_TAG, "Main Activity is not the root.  Finishing Main Activity instead of launching.");
                finish();
                return;
            }
        }


        //Debug only: Menu hack to force showing the overflow button (aka menu button) on the
        //   action bar EVEN IF there is a hardware button
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            // presumably, not relevant
        }


        //Get a static copy of the base myContext
        myContext = this.getBaseContext();

        // Get a static copy of the activity instance
        this.myInstance = this;

        //Request all permissions up-front and be done with it.
        //If the app can't perform properly with what is requested then
        // abort rather than have a crippled app running
        //Dangerous permissions groups that need to ne asked for:
        //Contacts: for when creating a new mail if we want to get the email address of a contact. Optional.
        //Location: for GPS to send position and to get accurage time for scanning servers. Essential.
        //Microphone: to get the audio input for the modems. Essential.
        //Phone: to disconnect the Bluetooth audio if a phone call comes in. Otherwise we
        //   send the phone call over the radio. Not allowed in Amateur radio or only with severe restrictions. Essential.
        //Storage: to read and write to the SD card. Essential, otherwise why use the app. There is Tivar for Rx only applications.
        //First check if the app already has the permissions

        havePassedAllPermissionsTest = allPermissionsOk();
        if (havePassedAllPermissionsTest) {
            performOnCreate();
        } else {
            requestAllCriticalPermissions();
        }
    }



    //Could be executed only when all necessary permissions are allowed
    private void performOnCreate() {

        //Get new gesture detector for flings over scrollviews
        mGesture = new GestureDetector(this, mOnGesture);

        //Initialize Toast for use in the Toast display routines below
        myToast = Toast.makeText(AndPskmail.this, "",	Toast.LENGTH_SHORT);

        //Initialize the notification manager
        myNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Bluetooth
        mAudioManager = (AudioManager) myContext
                .getSystemService(Context.AUDIO_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Create handler for periodic beacons and links
        myHandler = new Handler();

        //Monitor the connection of Bluetooth between devices
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String dataString = intent.getDataString();
                String mpackage = intent.getPackage();
                String type = intent.getType();
                int extraSCO = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);


                if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String wantedDevice = myconfig.getPreference("BLUETOOTHDEVICENAME", "noname");
                    if (deviceName != null
                            && toBluetooth
                            && deviceName.equals(wantedDevice)
                            && mBluetoothAdapter != null
                            && myconfig.getPreferenceB("BLUETOOTHAUTOCONNECT", false)) {
                        toBluetooth = false;
                        mAudioManager.stopBluetoothSco();
                        mAudioManager.setMode(AudioManager.MODE_NORMAL);
                        mAudioManager.setBluetoothScoOn(false);
                        topToastText("Bluetooth Device Disconnected. Audio Now Via Speaker/Headphones");
                    }
                } else if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String wantedDevice = myconfig.getPreference("BLUETOOTHDEVICENAME", "noname");
                    //Android 10 bug?? deviceName is sometimes null
                    if (deviceName != null && deviceName.equals(wantedDevice)
                            && mBluetoothAdapter != null
                            && myconfig.getPreferenceB("BLUETOOTHAUTOCONNECT", false)) {
                        deviceJustConnected = true;
                        final Runnable mRunnable = new Runnable() {
                            public void run() {
                                //Wait a few seconds for the device to settle
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                }
                                mAudioManager.startBluetoothSco();
                                mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                                mAudioManager.setBluetoothScoOn(true);
                                toBluetooth = true;
                                topToastText("Started Bluetooth Connection");
                            }
                        };
                        mRunnable.run();
                    }
                } else if (extraSCO == AudioManager.SCO_AUDIO_STATE_CONNECTED){
                    //BluetoothDevice device = intent
                    //        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //String deviceName = device.getName();
                    if (!deviceJustConnected) { //We started the app with the BT device already connected
                        mAudioManager.startBluetoothSco();
                        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                        mAudioManager.setBluetoothScoOn(true);
                        toBluetooth = true;
                        deviceJustConnected = false;
                        topToastText("Re-connected Audio Via Bluetooth Device");
                    } else {
                        topToastText("Connected Audio Via Bluetooth Device");
                    }
                } else if ( extraSCO == AudioManager.SCO_AUDIO_STATE_CONNECTING){
                    //topToastText("Bluetooth SCO Connecting");
                } else if ( extraSCO == AudioManager.SCO_AUDIO_STATE_DISCONNECTED){
                    topToastText("Bluetooth Device Disconnected");
                } else {
                    topToastText("Other Bluetooth Action");
                }
            }

        };

        //Bluetooth File transfers (Receiving listener)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        //if (android.os.Build.VERSION.SDK_INT >= 14) {
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        // } else {
        // test filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED);
        //}
        // Not called filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        this.registerReceiver(mReceiver, filter);

        // Register for phone state monitoring
        tmgr = (TelephonyManager)
                myContext.getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        // Initialize sercreen gestures handling
        // detector = new myGestureFilter(this,this);


        //Launch task for time display (Time is GPS time aligned if requested)
        Thread myThread = null;

        Runnable runnable = new DisplayTimeRunner();
        myThread= new Thread(runnable);
        myThread.start();

        //init NMEA listener for GPS time (to negate the device clock drift when not in mobile reception area)
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(myInstance,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.addNmeaListener(new GpsStatus.NmeaListener() {
                public void onNmeaReceived(long timestamp, String nmea) {
                    if (AndPskmail.myconfig.getPreferenceB("USEGPSTIME", false)) {
                        String[] NmeaArray = nmea.split(",");
                        if (NmeaArray[0].equals("$GPGGA")) {
                            //debug
                            //Processor.APRSwindow += "\n NMEA is :"+nmea;
                            //AndPskmail.mHandler.post(AndPskmail.addtoAPRS);
                            // Some devices do not include decimal seconds
                            //if (NmeaArray[1].indexOf(".") > 4) {
                            if (NmeaArray[1].length() > 5) { //6 or more characters
                                //String GpsTime = NmeaArray[1].substring(0,NmeaArray[1].indexOf("."));
                                String GpsTime = NmeaArray[1].substring(0, 6);
                                GPSTimeAcquired = true; //Mark that we have acquired time (for the clock colour display and autobeacon time)
                                GpsTime = "000000" + GpsTime;
                                //							Processor.APRSwindow += " GpsTime:" + GpsTime + "\n";
                                GpsTime = GpsTime.substring(GpsTime.length() - 4, GpsTime.length());
                                int GpsMin = Integer.parseInt(GpsTime.substring(0, 2));
                                int GpsSec = Integer.parseInt(GpsTime.substring(2, 4));
                                //Apply leap seconds correction: GPS is 16 seconds faster than UTC as of June 2013.
                                //Some devices do not apply this automatically (depends on the internal GPS engine)
                                int leapseconds = Integer.parseInt(AndPskmail.myconfig.getPreference("LEAPSECONDS", "0"));
                                GpsSec -= leapseconds;
                                if (GpsSec < 0) {
                                    GpsSec += 60;
                                    GpsMin--;
                                    if (GpsMin < 0) {
                                        GpsMin += 60;
                                    }
                                }
                                //In case of (unexpected) negative leap seconds values
                                if (GpsSec > 60) {
                                    GpsSec -= 60;
                                    GpsMin++;
                                    if (GpsMin > 60) {
                                        GpsMin -= 60;
                                    }
                                }
                                //Compare to current device time and date and calculate the offset to be applied at display
                                long nowInMilli = System.currentTimeMillis();
                                Time mytime = new Time();
                                mytime.set(nowInMilli); //initialized to now
                                int DeviceTime = mytime.second + (mytime.minute * 60);
                                //Correction (in seconds)
                                DeviceToGPSTimeCorrection = (GpsSec + (GpsMin * 60)) - DeviceTime;
                                //Debug
                                //							Processor.APRSwindow += " Device Time is :" + mytime.minute + ":" + mytime.second + "\n";
                                //							Processor.APRSwindow += " GPS Time is :" + GpsMin + ":" + GpsSec + "\n";
                                //							Processor.APRSwindow += " Correction is :" + DeviceToGPSTimeCorrection + "\n";
                            }
                        }
                        //					AndPskmail.mHandler.post(AndPskmail.addtoAPRS);
                        //loggingclass.writelog("Timestamp is :" +timestamp+"   nmea is :"+nmea,
                        //					  null, true);
                    }
                }
            });

        }
        // Init config
        mysp = PreferenceManager.getDefaultSharedPreferences(this);
        //Ensure we have default values loaded
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //Set home path
        String mypath = Processor.HomePath + Processor.Dirprefix;
        myconfig = new config(mypath);

        // Set default modes
        //Processor.TxModem = Modem.getmode(AndPskmail.myconfig.getTxMode());
        //Processor.RxModem = Modem.getmode(AndPskmail.myconfig.getRxMode());
        Processor.RxModem = Processor.TxModem = Modem.getModeFromName(myconfig.getPreference("LASTMODEUSED"));


        // Automatic beacons
        boolean autobeacon = AndPskmail.myconfig.getPreferenceB("BEACON", false);
        if (autobeacon && !Processor.Connected && !Processor.Connecting) {
            //Queue next beacon, allowing one minute for GPS fix, print next beacon time in terminal
            myHandler.postDelayed(prepareGPSforBeacon, nextBeaconOrLinkDelay(true) - 60000);
        }

        // Automatic Link Requests
        boolean autolink = AndPskmail.myconfig.getPreferenceB("AUTOLINK", false);
        if (autolink && !Processor.Connected && !Processor.Connecting) {
            //Queue next Link request, print next Link time in terminal
            myHandler.postDelayed(sendAndRequeueLink, nextBeaconOrLinkDelay(false));
        }

        //Set server  call to default
        //No: Let the first connect set the variable as we have an access password to initialise too possibly
        // serverToCall = AndPskmail.myconfig.getPreference("SERVER");

        returnToLastScreen(); //Defaults to terminal screen

    }



    /** Called when the activity is (re)started (to foreground) **/
    @Override
    public void onStart() {
        super.onStart();
        //Conditional to having passed the permission tests
        if (havePassedAllPermissionsTest) {
            performOnStart();
        }
    }


    //Could be executed when all necessary permissions are allowed
    private void performOnStart() {
        // Store preference reference for later (config.java)
        mysp = PreferenceManager.getDefaultSharedPreferences(this);
        String mypath = Processor.HomePath + Processor.Dirprefix;
        myconfig = new config(mypath);
        // Refresh defaults since we could be coming back
        // from the preference activity

        // Re-initilize modem when NOT in a SESSION to use the latest parameters
        if (!Processor.Connected && !Processor.Connecting && RXParamsChanged) {
            // Reset flag then stop and restart modem
            RXParamsChanged = false;
            //Cycle modem service off then on
            if (ProcessorON) {
                if (Processor.m.modemState == Modem.RXMODEMRUNNING) {
                    Processor.m.stopRxModem();
                    stopService(new Intent(AndPskmail.this, Processor.class));
                    ProcessorON = false;
                    //Force garbage collection to prevent Out Of Memory errors on small RAM devices
                    System.gc();
                }
            }
            //Wait for modem to stop and then restart
            while (Processor.m.modemState != Modem.RXMODEMIDLE) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            //Force garbage collection to prevent Out Of Memory errors on small RAM devices
            System.gc();
            startService(new Intent(AndPskmail.this,
                    Processor.class));
            ProcessorON = true;

            //Finally, if we were on the modem screen AND we come back to it, then
            //   redisplay in case we changed the waterfall frequency
            if (currentview == MODEMVIEWwithWF) {
                displayModem(NORMAL, true);
            }
        } else { // start if not ON yet AND we haven't paused the modem manually
            if (!ProcessorON && !modemPaused) {
                String NOTIFICATION_CHANNEL_ID = "com.AndPskmail";
                String channelName = "Background Modem";
                NotificationChannel chan = null;
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
                NotificationCompat.Builder mBuilder;
                String chanId = "";
                //New code for support of Android version 8+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
                    chan.setLightColor(Color.BLUE);
                    chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    assert manager != null;
                    manager.createNotificationChannel(chan);
                    chanId = chan.getId();
                }
                mBuilder = new NotificationCompat.Builder(this, chanId)
                        .setSmallIcon(R.drawable.notificationicon)
                        .setContentTitle("Modem ON")
                        .setContentText("Microphone/Bluetooth in use by App")
                        .setOngoing(true);
                // Creates an explicit intent for an Activity in your app
                Intent notificationIntent = new Intent(this, AndPskmail.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                //Google: The stack builder object will contain an artificial back stack for the started Activity.
                // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(AndPskmail.class);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(notificationIntent);
                TaskStackBuilder nstackBuilder = TaskStackBuilder.create(myContext);
                nstackBuilder.addParentStack(AndPskmail.class);
                nstackBuilder.addNextIntent(notificationIntent);
                PendingIntent pIntent = nstackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(pIntent);
                // VK2ETA notification is done in Processor service now
                myNotification = mBuilder.build();
                //Force garbage collection to prevent Out Of Memory errors on small RAM devices
                System.gc();
                startService(new Intent(AndPskmail.this, Processor.class));
                ProcessorON = true;
            }
        }
        AndPskmail.mHandler.post(AndPskmail.updatetitle);
    }



    @Override
    public void onResume() {
        super.onResume();
        //Save the values of key fields when we quit the current application (calling another application or pressing the back button)
        if (currentview == APRSVIEW) {
            TextView view = (TextView) findViewById(R.id.edit_text_out);
            if (view != null) {
                view.setText(savedAprsMessage);
            }
        }
    }



    @Override
    public void onPause() {
        super.onPause();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        //Unregister the receiver as we quit that process
        //VK2ETA: not used yet: myContext.unregisterReceiver(myBroadcastReceiver);
        //Remove the queued beacons and link requests
        myHandler.removeCallbacks(sendAndRequeueLink);
        myHandler.removeCallbacks(sendAndRequeueBeacon);
    }



    // Option Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }


	/* Parked code for monitoring the user-controlled enabling of the Bluetooth interface
	@Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up an audio channel
				if (Double.valueOf(android.os.Build.VERSION.SDK) >= 8) {
					mAudioManager.startBluetoothSco();
				}
				//Android 2.1. Needs testinband.apk to be launched before hand
				mAudioManager.setMode(AudioManager.MODE_IN_CALL);
				mAudioManager.setBluetoothScoOn(true);
				// mAudioManager.setSpeakerphoneOn(false);
				toBluetooth = true;
            } else {
                // User did not enable Bluetooth or an error occured
				toBluetooth = false;
				Toast.makeText(this, "Bluetooth has not been enabled", Toast.LENGTH_LONG).show();
            }
        }
    }
	 */



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    Cursor cursor = null;
                    String email = "";
                    try {
                        Uri result = data.getData();
                        // get the contact id from the Uri
                        String id = result.getLastPathSegment();

                        // query for everything email
                        cursor = getContentResolver().query(Email.CONTENT_URI,
                                null, Email.CONTACT_ID + "=?", new String[] { id },
                                null);

                        int emailIdx = cursor.getColumnIndex(Email.DATA);

                        // let's just get the first email
                        if (cursor.moveToFirst()) {
                            email = cursor.getString(emailIdx);
                        }
                    } catch (Exception ex) {
                        loggingclass.writelog("Failed to get email address. Most likely the permission to read contacts was denied. \n\n" +
                                        "Consider allowing the app to read your contacts.\n\n" ,
                                null, true);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                        EditText emailEntry = (EditText) findViewById(R.id.emailaddress);
                        if (emailEntry != null) emailEntry.setText(email);
                        if (email.length() == 0) {
                            Toast.makeText(this, "No email found for contact.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }

                    break;
            }

        } else {
            //not for now:  Log.w(DEBUG_TAG, "Warning: activity result not ok");
        }
    }


    // Option Screen handler
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(AndPskmail.this);
        switch (item.getItemId()) {
            case R.id.prefs:
                Intent OptionsActivity = new Intent(getBaseContext(),
                        myPreferences.class);
                startActivity(OptionsActivity);
                break;
            case R.id.savePreferences:
                config.saveSharedPreferencesToFile("SettingsBackup.bin");
                break;
            case R.id.restorePreferences:
                config.loadSharedPreferencesFromFile("SettingsBackup.bin");
                break;
            case R.id.defaultPreferences:
                config.restoreSettingsToDefault();
                break;
            case R.id.terminal:
                displayTerminal(NORMAL);
                break;
            case R.id.mail:
                if (lastEmailScreen == -1)
                    lastEmailScreen = MAILHEADERSVIEW;
                displayMail(NORMAL, lastEmailScreen);
                break;
            case R.id.inforequest:
                displayInforequest(NORMAL);
                break;
            case R.id.aprs:
                displayAPRS(NORMAL);
                break;
            case R.id.About:
                displayAbout();
                break;
            case R.id.modem:
                displayModem(NORMAL, false); // no waterfall to start with
                break;
            case R.id.BTon:
                // mBluetoothHeadset.startVoiceRecognition();
                if (mBluetoothAdapter != null) {
				/* New code - not reliable // Device does support Bluetooth
				if (mBluetoothAdapter.isEnabled()) {
					if (scoChannelOn) { //just re-set the redirection of the audio
						mAudioManager.setMode(AudioManager.MODE_IN_CALL);
						mAudioManager.setBluetoothScoOn(true);
						// mAudioManager.setSpeakerphoneOn(false);
						toBluetooth = true;
					} else {
						if (Double.valueOf(android.os.Build.VERSION.SDK) >= 8.0) {
							mAudioManager.startBluetoothSco();
						} else  {
							//Android 2.1. Needs testinband.apk to be launched before hand
							mAudioManager.setMode(AudioManager.MODE_IN_CALL);
							mAudioManager.setBluetoothScoOn(true);
							// mAudioManager.setSpeakerphoneOn(false);
							toBluetooth = true;
						}
					}
				} else { //Launch the BT enabling request and wait for the result
					mBluetoothAdapter.enable();
					//Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					//startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				}
				 */
                    //OLD Code that works (Manual connect to BT device)
                    if (Double.valueOf(android.os.Build.VERSION.SDK) >= 8) {
                        mAudioManager.startBluetoothSco();
                    }
                    mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                    mAudioManager.setBluetoothScoOn(true);
                    // mAudioManager.setSpeakerphoneOn(false);
                    toBluetooth = true;
                }
                break;
            case R.id.BToff:
                // old code that worked!!!!
                toBluetooth = false;
                // mBluetoothHeadset.stopVoiceRecognition();
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
                mAudioManager.setBluetoothScoOn(false);
                // mAudioManager.setSpeakerphoneOn(true);
                if (Double.valueOf(android.os.Build.VERSION.SDK) >= 8) {
                    mAudioManager.stopBluetoothSco();
                }

			/*			//New code - not reliable due to Froyo (2.2) Bluetooth bug
			toBluetooth = false;
			// mBluetoothHeadset.stopVoiceRecognition();
			mAudioManager.setMode(AudioManager.MODE_NORMAL);
			mAudioManager.setBluetoothScoOn(false);
			// mAudioManager.setSpeakerphoneOn(true);
			if (Double.valueOf(android.os.Build.VERSION.SDK) >= 8.0) {
				if (scoChannelOn) {
					mAudioManager.stopBluetoothSco();
					scoChannelOn = false;
				} else {
					//Disable BT when the sco if off only
					if (mBluetoothAdapter != null) {
						if (mBluetoothAdapter.isEnabled()) {
//							mBluetoothAdapter.disable();
						}
					}
				}

			}
			 */
                break;
            case R.id.exit:
                myAlertDialog.setMessage("Are you sure you want to Exit?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //					Toast.makeText(this, "GoodBye", Toast.LENGTH_SHORT).show();
                        // Stop the Modem and Listening Service
                        if (ProcessorON) {
                            stopService(new Intent(AndPskmail.this, Processor.class));
                            ProcessorON = false;
                            //Only for Android 3.0 and UP
                            if (Double.valueOf(android.os.Build.VERSION.SDK) >= 11.0) {
                                //Remove the notification
                                myNotificationManager.cancel(9999);
                                ProcessorON = false;
                            }
                        }
                        // Stop the GPS if running
                        if (locationManager != null) {
                            locationManager.removeUpdates(locationListener);
                        }
                        //Remove the queued beacons and link requests
                        myHandler.removeCallbacks(prepareGPSforBeacon);
                        myHandler.removeCallbacks(sendAndRequeueLink);
                        myHandler.removeCallbacks(sendAndRequeueBeacon);
                        //Close that activity and return to previous screen
                        finish();
                        //Kill the process
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                myAlertDialog.show();
                break;
            case R.id.clearheaders:
                myAlertDialog.setMessage("Are you sure you want to Clear the HEADERS?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Processor.sm.deleteFile("headers");
                        Processor.sm.makeFile("headers");
                        middleToastText("Deleted Mail Headers...");
                    }
                });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                myAlertDialog.show();
                break;
            case R.id.clearmailfile:
                myAlertDialog.setMessage("Are you sure you want to Clear the INBOX?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Processor.sm.deleteFile("Inbox");
                        Processor.sm.makeFile("Inbox");
                        middleToastText("Cleared Inbox...");
                    }
                });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                myAlertDialog.show();
                break;
            case R.id.clearoutbox:
                myAlertDialog.setMessage("Are you sure you want to Clear the OUTBOX?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ClearOutbox();
                        middleToastText("Cleared Outbox...");
                    }
                });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                myAlertDialog.show();
                break;
            case R.id.clearsentmail:
                myAlertDialog.setMessage("Are you sure you want to Clear the SENT Mail?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Processor.sm.deleteFile("Sentmail");
                        Processor.sm.makeFile("Sentmail");
                        middleToastText("Deleted Sent Mail...");
                    }
                });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                myAlertDialog.show();
                break;
            case R.id.cleardownloads:
                myAlertDialog.setMessage("Are you sure you want to Clear the Partial DOWNLOADS?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ClearDownloads();
                        middleToastText("Cleared Pending Downloads...");
                    }
                });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                myAlertDialog.show();
                break;
            case R.id.clearuploads:
                myAlertDialog.setMessage("Are you sure you want to Clear the Partial UPLOADS?");
                myAlertDialog.setCancelable(false);
                myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ClearUploads();
                        middleToastText("Cleared Pending Uploads...");
                    }
                });
                myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                myAlertDialog.show();
                break;
            case R.id.anypending:
                ListPendingTransactions();
                break;
            case R.id.updateserver:
                if (Processor.Connected) {
                    Processor.sm.sendUpdate();
                    middleToastText("Sending record to server...");
                } else {
                    middleToastText("You need to Connect First");
                }
                break;
            case R.id.resetrecord:
                if (Processor.Connected) {
                    myAlertDialog.setMessage("Are you sure you want to Clear Your Credentials on this Server?");
                    myAlertDialog.setCancelable(false);
                    myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            Processor.sm.resetRecord();
                            middleToastText("Request to Clear your Credentials Sent...");
                        }
                    });
                    myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                        }
                    });
                    myAlertDialog.show();
                } else {
                    middleToastText("You need to Connect First");
                }
                break;
            case R.id.setpassword:
                if (Processor.Connected)
                {
                    if (Processor.cr != null)
                    {
                        myAlertDialog.setMessage("Are you sure you want to Update your Session Password for this Server?");
                        myAlertDialog.setCancelable(false);
                        myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                if (Processor.sm.setPassword())
                                {
                                    middleToastText("Session Password Update Sent...");
                                };
                            }
                        });
                        myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                dialog.cancel();
                            }
                        });
                        myAlertDialog.show();
                    } else {
                        middleToastText("You need to Wait for the Server's Identification first");
                    }
                }
                else
                {
                    middleToastText("You need to Connect First");
                }
                break;
        }
        return true;
    }

    // Handles callbacks from GPS service
    private class GPSListener implements LocationListener {

        public void onLocationChanged(Location location) {
        }

        public void onStatusChanged(String s, int i, Bundle b) {
        }

        public void onProviderDisabled(String s) {
            //toastText("GPS turned off - Please turn ON");
            GPSisON = false;
        }

        public void onProviderEnabled(String s) {
            //toastText("GPS turned ON");
            GPSisON = true;
        }
    }


    // Simple text transparent popups (bottom of screen)
    public void topToastText(String message) {
        try {
            myToast.setText(message);
            myToast.setGravity(Gravity.TOP, 0	, 100);
            myToast.show();
        } catch (Exception ex) {
            loggingclass.writelog("Toast Message error: " + ex.getMessage(),
                    null, true);
            // System.out.println(ex.getMessage());
        }
    }



    // Simple text transparent popups TOWARDS MIDDLE OF SCREEN
    public void middleToastText(String message) {
        try {
            myToast.setText(message);
            myToast.setGravity(Gravity.CENTER, 0	, 0);
            myToast.show();
        } catch (Exception ex) {
            loggingclass.writelog("Toast Message error: " + ex.getMessage(),
                    null, true);
            // System.out.println(ex.getMessage());
        }
    }


    // Simple text transparent popups
    public void bottomToastText(String message) {
        try {
            myToast.setText(message);
            myToast.setGravity(Gravity.BOTTOM, 0, 100);
            myToast.show();
        } catch (Exception ex) {
            loggingclass.writelog("Toast Message error: " + ex.getMessage(),
                    null, true);
            // System.out.println(ex.getMessage());
        }
    }



    //For storing Boolean preferences
    public void storePreferenceB(String pref, boolean flag) {
        //store value into preferences
        SharedPreferences.Editor editor = AndPskmail.mysp.edit();
        editor.putBoolean(pref, flag);
        // Commit the edits!
        editor.commit();
    }


    public static void screenAnimation(ViewGroup panel, int screenAnimation) {

        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(100);
        set.addAnimation(animation);

        switch(screenAnimation) {

            case NORMAL:
                return;
            //break;

            case RIGHT:
                animation = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
                );
                break;
            case LEFT:
                animation = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
                );
                break;

            case TOP:
                animation = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                );
                break;

            case BOTTOM:
                animation = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                );
                break;

        }

        animation.setDuration(200);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.25f);
        if (panel != null) {
            panel.setLayoutAnimation(controller);
        }
    }



    //Save last mode used for next app start
    public static void saveLastModeUsed(String currentMode) {
        SharedPreferences.Editor editor = AndPskmail.mysp.edit();
        editor.putString("LASTMODEUSED", currentMode);
        // Commit the edits!
        editor.commit();
    }



    //Custom spinner that does not trigger on layout display
    private class modeSpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        boolean userSelect = false;

        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (userSelect) {
                if (!Processor.TXActive) {
                    String thisMode = (String) parent.getItemAtPosition(position);
                    //Set modem
                    Processor.RxModem = Processor.TxModem = Modem.getModeFromName(thisMode);
                    Processor.m.changemode(Processor.RxModem); // to make the changes effective
                    //Save it for next time
                    saveLastModeUsed(thisMode);
                    AndPskmail.mHandler.post(AndPskmail.updatetitle);
                } else {
                    middleToastText("Not while transmitting");
                    //Reset to previous op-mode
                    int numModes = parent.getCount();
                    String thisSpinnerMode = myconfig.getPreference("LASTMODEUSED");
                    for (int j = 0; j < numModes; j++) {
                        if (parent.getItemAtPosition(j).equals(thisSpinnerMode)) {
                            parent.setSelection(j);
                        }
                    }
                }
            }
            userSelect = false;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }
    }



    //Custom spinner that does not trigger on layout display
    private class periodSpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        boolean userSelect = false;

        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (userSelect) {
                String thisPeriod = (String) parent.getItemAtPosition(position);
                //Save it
                SharedPreferences.Editor editor = AndPskmail.mysp.edit();
                editor.putString("BEACONPERIOD", thisPeriod);
                // Commit the edits!
                editor.commit();
            }
            userSelect = false;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }
    }



    //Custom spinner that does not trigger on layout display
    private class qrgSpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        boolean userSelect = false;

        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (userSelect) {
                String thisQrg = (String) parent.getItemAtPosition(position);
                //Save it
                SharedPreferences.Editor editor = AndPskmail.mysp.edit();
                editor.putString("BEACONQRG", thisQrg);
                // Commit the edits!
                editor.commit();
            }
            userSelect = false;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }
    }



    private void connectDialog() {

        AlertDialog.Builder myAlertBuilder = new AlertDialog.Builder(this);
        LayoutInflater myInflater = LayoutInflater.from(this);
        final View connectDialogView = myInflater.inflate(R.layout.connectdialog, null);
        //Fill-in spinner for mode
        Spinner modeDropdown = (Spinner) connectDialogView.findViewById(R.id.modes_spinner);
        String[] selectedModes = Modem.getSelectedModesArray();
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, selectedModes);
        modeDropdown.setAdapter(modeAdapter);
        String thisSpinnerMode = myconfig.getPreference("LASTMODEUSED");
        //Select last mode if still in the list, otherwise select the first one
        modeDropdown.setSelection(0);
        for (int j = 0; j < selectedModes.length; j++) {
            if (selectedModes[j].equals(thisSpinnerMode)) {
                modeDropdown.setSelection(j);
            }
        }
        modeSpinnerListener mslistener = new modeSpinnerListener();
        modeDropdown.setOnTouchListener(mslistener);
        modeDropdown.setOnItemSelectedListener(mslistener);

        myAlertBuilder.setView(connectDialogView)
                .setCancelable(true).setNegativeButton(
                "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Hide the keyboard since we handle it manually
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                        dialog.cancel();
                    }
                })
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        // Stop the GPS if running (saves batteries and is of no use
                        // while in a session
                        if (locationManager != null) {
                            locationManager.removeUpdates(locationListener);
                        }
                        TextView view = (TextView) connectDialogView.findViewById(R.id.edit_text_connect);
                        serverToCall = view.getText().toString().trim();
                        TextView pwview = (TextView) connectDialogView.findViewById(R.id.edit_text_accesspassword);
                        serverAccessPassword = pwview.getText().toString().trim();
                        if (serverToCall.length() == 0) {
                            // Hide the keyboard since we handle it manually
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                            dialog.cancel();
                            middleToastText("Server Call can't be blank");
                        } else {
                            // Hide the keyboard since we handle it manually
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                            //Send the connect command
                            Processor.Connecting = true;
                            Processor.connectingPhase = true;
                            if (serverAccessPassword.length() == 0) {
                                Processor.Status = "Connecting";
                            } else {
                                Processor.Status = "Connect. w/ pw";
                            }
                            //Reset receiving modem to default centre frequency
                            Processor.m.reset();
                            Processor.q.send_rsid_command("ON");
                            Processor.q.set_txstatus(txstatus.TXConnect);
                            Processor.q.send_frame("");
                            // Processor.q.Message(mainpskmailui.getString("Sending_Connect_request..."),
                            // 5);
                        }
                    }
                });

        AlertDialog myConnectAlert = myAlertBuilder.create();
        EditText view = (EditText) connectDialogView.findViewById(R.id.edit_text_connect);
        EditText pwView = (EditText) connectDialogView.findViewById(R.id.edit_text_accesspassword);
        if (serverToCall.length() == 0) { //use default server call from preferences
            String defaultServerCall = AndPskmail.myconfig.getPreference("SERVER");
            String defaultServerPw = AndPskmail.myconfig.getPreference("SERVERACCESSPASSWORD");
            view.setText(defaultServerCall);
            pwView.setText(defaultServerPw);
        } else { //We had already typed-in a server call. Propose first as we are likely to use it again
            view.setText(serverToCall);
            pwView.setText(serverAccessPassword);
        }
        myConnectAlert.setTitle("Connect to Server:");
        myConnectAlert.show();

    }


    // Display the Terminal layout and associate it's buttons
    private void displayTerminal(int screenMovement) {
        //If we elected to use GPS time, start GPS listening now
        if (AndPskmail.myconfig.getPreferenceB("USEGPSTIME", false)) {
            locationManager = (LocationManager) this
                    .getSystemService(Context.LOCATION_SERVICE);
            if (ContextCompat.checkSelfPermission(myInstance,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        60000, // milisecs
                        0, // meters
                        locationListener);
            }
        }
        // Change layout and remember which one we are on
        currentview = TERMVIEW;
        setContentView(R.layout.terminal);
        screenAnimation((ViewGroup) findViewById(R.id.termscreen), screenMovement);
        myTermTV = (TextView) findViewById(R.id.terminalview);
        myTermTV.setHorizontallyScrolling(false);
        myTermTV.setTextSize(16);
        myWindow = getWindow();
        AndPskmail.mHandler.post(AndPskmail.updatetitle);
        // If blank (on start), display version
        if (TerminalBuffer.length() == 0 && !hasDisplayedWelcome) {
            beaconsAndLinksTime = "";
            if (nextBeaconTime != null)  {
                beaconsAndLinksTime += "\n-> Next Beacon at : "  + nextBeaconTime.format("%H:%M:%S");
                locationManager = (LocationManager) this
                        .getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    beaconsAndLinksTime += "\n**** WARNING: the GPS is OFF ****\n";
                }
            }
            if (nextAutolinkTime != null) beaconsAndLinksTime += "\n-> Next Auto Link at : "  + nextAutolinkTime.format("%H:%M:%S") + "\n";
            TerminalBuffer = welcomeString + beaconsAndLinksTime;
        } else {
            if (TerminalBuffer.equals(welcomeString + beaconsAndLinksTime)) {
                TerminalBuffer = "";
            }
        }
        hasDisplayedWelcome = true;
        // Reset terminal display in case it was blanked out by a new oncreate call
        myTermTV.setText(TerminalBuffer);
        myTermSC = (ScrollView) findViewById(R.id.terminalscrollview);
        // update with whatever we have already accumulated then scroll
        AndPskmail.mHandler.post(AndPskmail.addtoterminal);
        //Advise which screen we are in
        topToastText("Terminal Screen");

        // JD Initialize the Send Text button (commands in connected mode)
        myButton = (Button) findViewById(R.id.button_sendtext);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String intext = view.getText().toString();

                try {
                    if (Processor.Connected) {
                        if (intext.indexOf("~PASS") == 0) {
                            intext = Processor.cr.encrypt(
                                    Processor.sm.hispubkey, intext.substring(5));
                            Processor.TX_Text += (intext + "\n");
                            Processor.PostToTerminal("\n=>>" + intext + "\n");
                        } else if (intext.contains(":SETPASSWORD")) {
                            // test Processor.sm.hispubkey = "1234";
                            String mailpass = AndPskmail.myconfig
                                    .getPreference("POPPASS");
                            if (mailpass.length() > 0
                                    & Processor.Passwrd.length() > 0) {
                                intext = Processor.cr.encrypt(
                                        Processor.sm.hispubkey, mailpass + ","
                                                + Processor.Passwrd);
                                Processor.TX_Text += ("~Msp" + intext + "\n");
                                Processor.PostToTerminal("\n=>>" + intext
                                        + "\n");
                            } else {
                                Processor
                                        .PostToTerminal("\n=>>"
                                                + "No POP password or Link password set?\n");
                            }
                        } else {
                            Processor.TX_Text += (intext + "\n");
                        }
                    } else {
                        bottomToastText("You Must Connect First");
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog("Error Sending Text from Terminal: " + ex.getMessage(),
                            null, true);
                    //System.out.println(ex.getMessage());
                }
            }
        });

        // JD Initialize the Connect button
        myButton = (Button) findViewById(R.id.button_connect);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(AndPskmail.this);
                    myAlertDialog.setMessage("Are you sure you want to Disconnect?");
                    myAlertDialog.setCancelable(false);
                    myAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            Processor.Status = "Disconnecting";
                            Processor.TX_Text += ("~QUIT" + "\n");
                            topToastText("Sending Disconnect command...");
                            // lblStatus.setText(mainpskmailui.getString("Discon"));
                            // Processor.Connecting_time = 0;
                            // JD ??? mysession.FileDownload = false;
                            // try {
                            // if (mysession.pFile != null) {
                            // mysession.pFile.close();
                            // }
                            // }
                            // catch (IOException e) {
                            // myarq.Message("Cannot close pending file", 10);
                            // }
                        }
                    });
                    myAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                        }
                    });
                    myAlertDialog.show();
                } else {
                    //It is a connect command then
                    if (!Processor.TXActive) {
                        connectDialog();
                    }
                }
            }
        });

        // JD Initialize the SEND Mail button
        myButton = (Button) findViewById(R.id.button_sendmail);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    SendButtonAction();
                }
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the ABORT button
        myButton = (Button) findViewById(R.id.button_abort);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    AbortButtonAction();
                }
                catch (Exception ex) {
                    // System.out.println(ex.getMessage());
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the STOP button
        myButton = (Button) findViewById(R.id.button_stop);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {

                    try {
                        Processor.TX_Text += "~STOP:"
                                + Processor.sm.Transaction + "\n";
                        topToastText("Sending STOP command...");
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    topToastText("Only while Connected");
                }
            }
        });

    }


    private void linkDialog() {
        AlertDialog.Builder myAlertBuilder = new AlertDialog.Builder(this);
        LayoutInflater myInflater = LayoutInflater.from(this);
        final View connectDialogView = myInflater.inflate(R.layout.connectdialog, null);
        //Remove the spinner from view as we have already displayed it on the APRS screen
        connectDialogView.findViewById(R.id.modes_spinner).setVisibility(View.GONE);
        myAlertBuilder.setView(connectDialogView)
                .setCancelable(true).setNegativeButton(
                "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Hide the keyboard since we handle it manually
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                        dialog.cancel();
                    }
                })
                .setPositiveButton("Link", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        TextView view = (TextView) connectDialogView.findViewById(R.id.edit_text_connect);
                        serverToCall = view.getText().toString().trim();
                        if (serverToCall.length() == 0) {
                            // Hide the keyboard since we handle it manually
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                            dialog.cancel();
                            middleToastText("Server Call can't be blank");
                        } else {
                            // Hide the keyboard since we handle it manually
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                            //Send the Inquire command
                            try {
                                Processor.q.send_link();
                            }
                            catch (Exception ex) {
                                loggingclass.writelog(
                                        "Button Execution error: " + ex.getMessage(), null,
                                        true);
                            }
                        }
                    }
                });

        AlertDialog myConnectAlert = myAlertBuilder.create();
        EditText view = (EditText) connectDialogView.findViewById(R.id.edit_text_connect);
        if (serverToCall.length() == 0) { //use default server call from preferences
            String defaultServerCall = AndPskmail.myconfig.getPreference("SERVER");
            view.setText(defaultServerCall);
        } else { //We had already typed-in a server call. Propose first as we are likely to use it again
            view.setText(serverToCall);
        }
        myConnectAlert.setTitle("Server to Link to");
        myConnectAlert.show();
    }



    private void inquireDialog() {
        AlertDialog.Builder myAlertBuilder = new AlertDialog.Builder(this);
        LayoutInflater myInflater = LayoutInflater.from(this);
        final View connectDialogView = myInflater.inflate(R.layout.connectdialog, null);
        myAlertBuilder.setView(connectDialogView)
                .setCancelable(true).setNegativeButton(
                "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Hide the keyboard since we handle it manually
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                        dialog.cancel();
                    }
                })
                .setPositiveButton("Inquire", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        TextView view = (TextView) connectDialogView.findViewById(R.id.edit_text_connect);
                        serverToCall = view.getText().toString().trim();
                        if (serverToCall.length() == 0) {
                            // Hide the keyboard since we handle it manually
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                            dialog.cancel();
                            topToastText("Server Call can't be blank");
                        } else {
                            // Hide the keyboard since we handle it manually
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(connectDialogView.getWindowToken(), 0);
                            //Send the Inquire command
                            try {
                                Processor.q.send_inquire();
                            }
                            catch (Exception ex) {
                                loggingclass.writelog(
                                        "Button Execution error: " + ex.getMessage(), null,
                                        true);
                            }
                        }
                    }
                });

        AlertDialog myConnectAlert = myAlertBuilder.create();
        EditText view = (EditText) connectDialogView.findViewById(R.id.edit_text_connect);
        if (serverToCall.length() == 0) { //use default server call from preferences
            String defaultServerCall = AndPskmail.myconfig.getPreference("SERVER");
            view.setText(defaultServerCall);
        } else { //We had already typed-in a server call. Propose first as we are likely to use it again
            view.setText(serverToCall);
        }
        myConnectAlert.setTitle("Server to Inquire From");
        myConnectAlert.show();
    }


    // Display the APRS layout and associate it's buttons
    private void displayAPRS(int screenMovement) {
        currentview = APRSVIEW;

        // Initialise GPS
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(myInstance,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    60000, // milisecs
                    50, // meters
                    locationListener);
        }
        // Open APRS layout by default until we have other activities defined
        setContentView(R.layout.aprs);
        screenAnimation((ViewGroup) findViewById(R.id.aprsscreen), screenMovement);
        mAPRSView = (TextView) findViewById(R.id.aprsview);
        mAPRSView.setHorizontallyScrolling(false);
        mAPRSView.setTextSize(16);
        // Reset terminal display in case it was blanked out by a new oncreate call
        mAPRSView.setText(APRSBuffer);
        mAPRSSV = (ScrollView) findViewById(R.id.aprsscrollview);
        // update with whatever we have already accumulated then scroll
        AndPskmail.mHandler.post(AndPskmail.addtoAPRS);

        //Restore the data entry field at the bottom
        TextView view = (TextView) findViewById(R.id.edit_text_out);
        view.setText(savedAprsMessage);
        //Add a textwatcher to save the text as it is being typed. Used for periodic beacons
        view.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable arg0) {
            }

            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                savedAprsMessage = arg0.toString();
            }

        });

        //Advise which screen we are in
        topToastText("APRS Screen");

        // Initialize the AutoBeacon check box
        checkbox = (CheckBox) findViewById(R.id.autobeacon);
        boolean autobeacon = AndPskmail.myconfig.getPreferenceB("BEACON", false);
        checkbox.setChecked(autobeacon);
        checkbox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    //Store preference
                    storePreferenceB("BEACON", true);
                    //Remove one minute for the GPS fix
                    AndPskmail.myHandler.postDelayed(AndPskmail.prepareGPSforBeacon,
                            AndPskmail.nextBeaconOrLinkDelay(true) - 60000);
                    Processor.PostToAPRS("\n-> Next Beacon at : "  + nextBeaconTime.format("%H:%M:%S") + "\n");
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        Processor.PostToAPRS("\n**** WARNING: the GPS is OFF ****\n");
                    }
                } else {
                    //Store preference
                    storePreferenceB("BEACON", false);
                    //Remove the queued beacon requests
                    myHandler.removeCallbacks(sendAndRequeueBeacon);
                    myHandler.removeCallbacks(prepareGPSforBeacon);
                }
            }
        });


        // Initialize the AutoLink check box
        checkbox = (CheckBox) findViewById(R.id.autolink);
        boolean autolink = AndPskmail.myconfig.getPreferenceB("AUTOLINK", false);
        checkbox.setChecked(autolink);
        checkbox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    //Store preference
                    storePreferenceB("AUTOLINK", true);
                    AndPskmail.myHandler.postDelayed(AndPskmail.sendAndRequeueLink, AndPskmail.nextBeaconOrLinkDelay(false));
                    Processor.PostToAPRS("\n-> Next Auto Link at : "  + nextAutolinkTime.format("%H:%M:%S") + "\n");
                } else {
                    //Store preference
                    storePreferenceB("AUTOLINK", false);
                    //Remove the queued link requests
                    myHandler.removeCallbacks(sendAndRequeueLink);
                }
            }
        });

        //Fill-in spinner for mode
        Spinner modeDropdown = (Spinner) findViewById(R.id.modes_spinner);
        String[] selectedModes = Modem.getSelectedModesArray();
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, selectedModes);
        modeDropdown.setAdapter(modeAdapter);
        String thisSpinnerMode = myconfig.getPreference("LASTMODEUSED");
        //Select last mode if still in the list, otherwise select the first one
        modeDropdown.setSelection(0);
        for (int j = 0; j < selectedModes.length; j++) {
            if (selectedModes[j].equals(thisSpinnerMode)) {
                modeDropdown.setSelection(j);
            }
        }
        modeSpinnerListener mslistener = new modeSpinnerListener();
        modeDropdown.setOnTouchListener(mslistener);
        modeDropdown.setOnItemSelectedListener(mslistener);

        //Fill-in spinner for Period
        Spinner periodDropdown = (Spinner) findViewById(R.id.period_spinner);
        String[] allPeriods = getResources().getStringArray(R.array.period_values);
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, allPeriods);
        periodDropdown.setAdapter(periodAdapter);
        String thisPeriod = myconfig.getPreference("BEACONPERIOD");
        //Select default
        periodDropdown.setSelection(0);
        for (int j = 0; j < allPeriods.length; j++) {
            if (allPeriods[j].equals(thisPeriod)) {
                periodDropdown.setSelection(j);
            }
        }
        periodSpinnerListener periodlistener = new periodSpinnerListener();
        periodDropdown.setOnTouchListener(periodlistener);
        periodDropdown.setOnItemSelectedListener(periodlistener);

        //Fill-in spinner for QRG (Minute in the 5 minutes cycle)
        Spinner qrgDropdown = (Spinner) findViewById(R.id.qrg_spinner);
        String[] allQrgs = getResources().getStringArray(R.array.minutes_values);
        ArrayAdapter<String> qrgAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, allQrgs);
        qrgDropdown.setAdapter(qrgAdapter);
        String thisQrg = myconfig.getPreference("BEACONQRG");
        //Select default
        qrgDropdown.setSelection(0);
        for (int j = 0; j < allQrgs.length; j++) {
            if (allQrgs[j].equals(thisQrg)) {
                qrgDropdown.setSelection(j);
            }
        }
        qrgSpinnerListener qrglistener = new qrgSpinnerListener();
        qrgDropdown.setOnTouchListener(qrglistener);
        qrgDropdown.setOnItemSelectedListener(qrglistener);

        // Initialize the send APRS message button
        myButton = (Button) findViewById(R.id.button_aprsmsg);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                try {
                    // Check is this is an APRS message OR a short email
                    if (message.contains("@")) {
                        Processor.q.send_uimessage(message);
                    } else {
                        Processor.q.send_aprsmessage(message);
                    }
                }
                catch (Exception ex) {
                    // System.out.println(ex.getMessage());
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the beacon button
        myButton = (Button) findViewById(R.id.button_beacon);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();

                try {
                    Processor.q.send_beacon(message);
                }
                catch (Exception ex) {
                    // System.out.println(ex.getMessage());
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the PING button
        myButton = (Button) findViewById(R.id.button_ping);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    Processor.q.send_ping();
                }
                catch (Exception ex) {
                    // System.out.println(ex.getMessage());
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the INQUIRE button
        myButton = (Button) findViewById(R.id.button_inquire);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                inquireDialog();
            }
        });

        // JD Initialize the LINK button
        myButton = (Button) findViewById(R.id.button_link);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                linkDialog();
            }
        });

    }

    // Display the Mail Headers layout and associate it's buttons
    private void displayInforequest(int screenAnimation) {
        currentview = INFOREQUESTVIEW;

        // Open APRS layout by default until we have other activities defined
        setContentView(R.layout.inforequest);
        screenAnimation((ViewGroup) findViewById(R.id.infoscreen), screenAnimation);

        // Initialize the adapter for the exchange thread with server
        WebArrayAdapter = new ArrayAdapter<String>(this, R.layout.webpageslist);
        WebHeadersView = (ListView) findViewById(R.id.webpageslist);
        WebHeadersView.setAdapter(WebArrayAdapter);
        WebHeadersView.setDividerHeight(1);

        // Display mail headers
        LoadWebpages();

        // Set listener for item selection
        WebHeadersView
                .setOnItemLongClickListener(new OnItemLongClickListener() {
                    public boolean onItemLongClick(AdapterView<?> parent,
                                                   View view, int position, long id) {
                        if (Processor.Connected) {
                            // When LONG clicked, send a request to server
                            String url = AndPskmail.myconfig.getPreference(
                                    "URL" + Integer.toString(position + 1), "");
                            String beginstring = AndPskmail.myconfig.getPreference(
                                    "URL" + Integer.toString(position + 1)
                                            + "B", "");
                            if (beginstring.trim().length() > 0) {
                                beginstring = " begin:" + beginstring;
                            }
                            String endstring = AndPskmail.myconfig.getPreference(
                                    "URL" + Integer.toString(position + 1)
                                            + "E", "");
                            if (endstring.trim().length() > 0) {
                                endstring = " end:" + endstring;
                            }
                            if (url.trim().length() > 0) {
                                topToastText("Requesting " + url + beginstring
                                        + endstring + " ...");
                                if (AndPskmail.myconfig.getPreferenceB(
                                        "COMPRESSED", false)) {
                                    Processor.TX_Text += "~TGETZIP " + url
                                            + beginstring + endstring + "\n";
                                } else {
                                    Processor.TX_Text += "~TGET " + url
                                            + beginstring + endstring + "\n";
                                }
                            }
                        } else {
                            bottomToastText("You Must Connect First");
                        }
                        return false;
                    }
                });
        WebHeadersView.performHapticFeedback(MODE_APPEND);
        // Advise user of long pressed required for web pages
        topToastText("Information Request Screen \n\n Long Press in this section for\n requesting WEB Pages");

        // Initialize the GET Messages button
        myButton = (Button) findViewById(R.id.button_getmessages);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        topToastText("Getting list of messages from the web...");
                        Processor.TX_Text += "~/~GETMSG\n";
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET Tide Stations button
        myButton = (Button) findViewById(R.id.button_gettidestations);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        topToastText("Requesting list of tidal reference stations...");
                        Processor.TX_Text += "~GETTIDESTN\n";
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // JD Initialize the Get TIDE number from station button
        myButton = (Button) findViewById(R.id.button_gettide);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();

                if (Processor.Connected) {
                    if (message.length() > 0) {
                        try {
                            Processor.TX_Text += ("~GETTIDE " + message + "\n");
                        }
                        catch (Exception ex) {
                        }
                    } else {
                        topToastText("Need number of the station \n in the text box below...");
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET GRIB File button
        myButton = (Button) findViewById(R.id.button_getgribfile);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String input = view.getText().toString();
                if (Processor.Connected) {
                    try {
                        // Initialise GPS
                        locationManager = (LocationManager) getBaseContext()
                                .getSystemService(Context.LOCATION_SERVICE);
                        if (ContextCompat.checkSelfPermission(myInstance,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER, 60000, // milisecs
                                    50, // meters
                                    locationListener);
                        }
                        // No text input, get the last known GPS position data
                        if (input.length() == 0) {
                            Location location = AndPskmail.locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            String hilatstr = "N";
                            String lolatstr = "N";
                            String hilonstr = "E";
                            String lolonstr = "E";
                            int Intlat = (int) location.getLatitude();
                            int Intlon = (int) location.getLongitude();
                            int hilat = 0;
                            int hilon = 0;
                            int lolat = 0;
                            int lolon = 0;

                            String gribWidthSTR = AndPskmail.myconfig.getPreference("GRIBSQUARE","1");
                            int gribWidth = 0;
                            try {
                                gribWidth = Integer.parseInt(gribWidthSTR);
                            } catch (NumberFormatException e) {
                                gribWidth = 1;
                            }
                            gribWidth = gribWidth < 1 ? 1 : gribWidth;
                            gribWidth = gribWidth > 4 ? 4 : gribWidth;

                            hilat = Intlat + gribWidth;
                            if (hilat < 0) {
                                hilatstr = "S";
                                hilat = Math.abs(hilat);
                            }
                            lolat = Intlat - gribWidth;
                            if (lolat < 0) {
                                lolatstr = "S";
                                lolat = Math.abs(lolat);
                            }
                            hilon = Intlon + gribWidth;
                            if (hilon < 0) {
                                hilonstr = "W";
                                hilon = Math.abs(hilon);
                            }
                            lolon = Intlon - gribWidth;
                            if (lolon < 0) {
                                lolonstr = "W";
                                lolon = Math.abs(lolon);
                            }
                            input = Integer.toString(hilat) + hilatstr + ","
                                    + Integer.toString(lolat) + lolatstr + ","
                                    + Integer.toString(hilon) + hilonstr + ","
                                    + Integer.toString(lolon) + lolonstr;
                        }
                        String gribsquare = "send gfs:" + input;
                        Pattern pgs = Pattern
                                .compile(".*(\\d+\\w,\\d+\\w,\\d+\\w,\\d+\\w).*");
                        Matcher mgs = pgs.matcher(gribsquare);
                        if (!mgs.find()) {
                            topToastText("Format_error in request (" + gribsquare + "). Try a minute later when the GPS location has updated");
                        } else {
                            if (AndPskmail.myconfig.getPreference("GRIBDATA").equals("CUSTOMGRIBREQUEST1")) {
                                gribsquare += AndPskmail.myconfig.getPreference("CUSTOMGRIBREQUEST1");
                            } else if (AndPskmail.myconfig.getPreference("GRIBDATA").equals("CUSTOMGRIBREQUEST2")) {
                                gribsquare += AndPskmail.myconfig.getPreference("CUSTOMGRIBREQUEST2");
                            } else {
                                gribsquare += AndPskmail.myconfig.getPreference("GRIBDATA");
                            }
                            topToastText("Requesting your GRIB File...Check you mail for reply");
                            Processor.TX_Text += "~SEND\nTo: query@saildocs.com\nSubject: none\n\n"
                                    + gribsquare + "\n.\n.\n";
                        }
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                    if (locationManager != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET IAC Fleetcodes button
        myButton = (Button) findViewById(R.id.button_getiacfleetcodes);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        Processor.TX_Text += "~GETIAC\n";
                        topToastText("Getting IAC Fleetcodes...");
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET IAC Forecast button
        myButton = (Button) findViewById(R.id.button_getiacforecast);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        Processor.TX_Text += "~GET2IAC\n";
                        topToastText("Getting IAC Forecast...");
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET WWV button
        myButton = (Button) findViewById(R.id.button_getwwv);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        Processor.TX_Text += "~GETWWV\n";
                        topToastText("Getting WWV Info from the web...");
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET APRS Stations button
        myButton = (Button) findViewById(R.id.button_getaprsstations);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        Processor.TX_Text += "~GETNEAR\n";
                        topToastText("Getting APRS stations near you...");
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET V/UHF Relays button
        myButton = (Button) findViewById(R.id.button_getvhfuhfrelays);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        // Initialise GPS
                        locationManager = (LocationManager) getBaseContext()
                                .getSystemService(Context.LOCATION_SERVICE);
                        if (ContextCompat.checkSelfPermission(myInstance,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER, 60000, // milisecs
                                    50, // meters
                                    locationListener);
                        }
                        // Get the last known GPS position data
                        Location location = AndPskmail.locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        // Format to strings
                        // long for this request
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        String Latitudestr = String.valueOf(lat);
                        String Longitudestr = String.valueOf(lon);
                        Processor.TX_Text += "~GETRELAYS " + Latitudestr + " "
                                + Longitudestr + "\n";
                        topToastText("Requesting list of relays from the web_");
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                    if (locationManager != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET CAMPER Sites button
        myButton = (Button) findViewById(R.id.button_getcampersites);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        // Initialise GPS
                        locationManager = (LocationManager) getBaseContext()
                                .getSystemService(Context.LOCATION_SERVICE);
                        if (ContextCompat.checkSelfPermission(myInstance,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER, 60000, // milisecs
                                    50, // meters
                                    locationListener);
                        }
                        // Get the last known GPS position data
                        Location location = AndPskmail.locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        // Format to strings
                        // long for this request
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        String Latitudestr = String.valueOf(lat);
                        String Longitudestr = String.valueOf(lon);
                        Processor.TX_Text += "~GETCAMP " + Latitudestr + " "
                                + Longitudestr + "\n";
                        topToastText("Requesting list of camp sites from the web (EU only)");
                    }
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                    if (locationManager != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET Server Frequencies button
        myButton = (Button) findViewById(R.id.button_getserverfreqs);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        Processor.TX_Text += "~GETSERVERS\n";
                        topToastText("Getting list of servers from the web...");
                    }
                    // JD fix this catch action
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

        // Initialize the GET Pskmail News button
        myButton = (Button) findViewById(R.id.button_getpskmailnews);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Processor.Connected) {
                    try {
                        Processor.TX_Text += "~GETNEWS\n";
                        topToastText("Trying to get the news from the web...");
                    }
                    // JD fix this catch action
                    catch (Exception ex) {
                        // System.out.println(ex.getMessage());
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

    }


    // Display one of the Mail layouts and associate it's buttons
    @SuppressLint("NewApi")
    private void displayMail(int screenAnimation, int mailscreen) {

        setContentView(R.layout.mail);
        screenAnimation((ViewGroup) findViewById(R.id.mailscreen), screenAnimation);

        // LayoutInflater for popup windows
        inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Initialize the adapter for the exchange thread with server
        MailArrayAdapter = new ArrayAdapter<String>(this, R.layout.maillistview);
        mMailListView = (ListView) findViewById(R.id.maillist);
        mMailListView.setAdapter(MailArrayAdapter);
        mMailListView.setDividerHeight(8);

        currentview = mailscreen;
        lastEmailScreen = currentview; // remember which email screen we were on

        switch (mailscreen) {
            case MAILINBOXVIEW:
                // Load inbox mails
                LoadInbox();
                // Set listener for item selection
                mMailListView
                        .setOnItemLongClickListener(new OnItemLongClickListener() {
                            public boolean onItemLongClick(AdapterView<?> parent,
                                                           View view, final int position, long id) {
                                // When LONG clicked, display the mail
                                try {
                                    View layout = inflater.inflate(R.layout.maildisplaypopup,
                                            (ViewGroup) findViewById(R.id.mail_popup));
                                    // First, get the Display from the WindowManager
                                    Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
						/* Now we can retrieve all display-related infos */
                                    int winWidth = display.getWidth();
                                    int winHeight = display.getHeight();
                                    // int orientation = display.getOrientation();
                                    pw = new PopupWindow(layout, winWidth, winHeight, true);
                                    pw.setBackgroundDrawable(new BitmapDrawable()); // to
                                    // allow click event to be active display the popup in the center
                                    pw.showAtLocation(layout, Gravity.TOP, 0, 0);
                                    TextView emailText = (TextView) layout
                                            .findViewById(R.id.emailtext);
                                    // pw.showAsDropDown (emailText);
                                    emailText.setText(emaillist.get(position)
                                            .getRawmessage());
                                    // Initialize the return button
                                    Button myButton = (Button) layout
                                            .findViewById(R.id.button_return);
                                    myButton.setOnClickListener(new OnClickListener() {
                                        public void onClick(View v) {
                                            pw.dismiss(); // close popup
                                        }
                                    });
                                    // Initialize the copy to clipboard button
                                    myButton = (Button) layout
                                            .findViewById(R.id.button_clipboard);
                                    myButton.setOnClickListener(new OnClickListener() {
                                        public void onClick(View v) {
                                            if (Double.valueOf(android.os.Build.VERSION.SDK) >= 11.0) {
                                                android.content.ClipboardManager myCBM = (android.content.ClipboardManager)	getSystemService(CLIPBOARD_SERVICE);
                                                android.content.ClipData clip = ClipData.newPlainText("label", emaillist.get(position)
                                                        .getRawmessage());
                                                myCBM.setPrimaryClip(clip);
                                                topToastText("Email copied to Clipboard");
                                            } else {
                                                @SuppressWarnings("deprecation")
                                                android.text.ClipboardManager myCBM = (android.text.ClipboardManager)	getSystemService(CLIPBOARD_SERVICE);
                                                myCBM.setText(emaillist.get(position).getRawmessage());
                                                topToastText("Email copied to Clipboard");
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return false;
                            }
                        });
                mMailListView.performHapticFeedback(MODE_APPEND);
                // Advise user of long pressed required for web pages
                topToastText("Mail INBOX Screen\n\nLong Press on a header for display");
                break;
            case MAILOUTBOXVIEW:
                // Load outbox mails
                LoadOutbox();
                // Set listener for item selection
                mMailListView
                        .setOnItemLongClickListener(new OnItemLongClickListener() {
                            public boolean onItemLongClick(AdapterView<?> parent,
                                                           View view, final int position, long id) {
                                // When LONG clicked, display the mail
                                try {
                                    View layout = inflater.inflate(R.layout.maildisplaypopup,
                                            (ViewGroup) findViewById(R.id.mail_popup));
                                    Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
						/* Now we can retrieve all display-related infos */
                                    //int winWidth = display.getWidth();
                                    //int winHeight = display.getHeight();
                                    // int orientation = display.getOrientation();
                                    // create a large PopupWindow (it will be clipped automatically if larger than main window)
                                    //pw = new PopupWindow(layout, winWidth, winHeight, true);
                                    final PopupWindow pw = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
                                    pw.setBackgroundDrawable(new BitmapDrawable()); // to
                                    // allow click event to be active, display the popup in the center
                                    pw.showAtLocation(layout, Gravity.TOP, 0, 0);
                                    TextView emailText = (TextView) layout
                                            .findViewById(R.id.emailtext);
                                    emailText.setText(emaillist.get(position)
                                            .getRawmessage());
                                    // Initialize the return button
                                    Button myButton = (Button) layout
                                            .findViewById(R.id.button_return);
                                    myButton.setOnClickListener(new OnClickListener() {
                                        public void onClick(View v) {
                                            pw.dismiss(); // close popup
                                        }
                                    });
                                    // Initialize the copy to clipboard button
                                    myButton = (Button) layout
                                            .findViewById(R.id.button_clipboard);
                                    myButton.setOnClickListener(new OnClickListener() {
                                        @SuppressWarnings("deprecation")
                                        public void onClick(View v) {
                                            if (Double.valueOf(android.os.Build.VERSION.SDK) >= 11.0) {
                                                android.content.ClipboardManager myCBM = (android.content.ClipboardManager)	getSystemService(CLIPBOARD_SERVICE);
                                                android.content.ClipData clip = ClipData.newPlainText("label", emaillist.get(position)
                                                        .getRawmessage());
                                                myCBM.setPrimaryClip(clip);
                                                topToastText("Email copied to Clipboard");
                                            } else {
                                                android.text.ClipboardManager myCBM = (android.text.ClipboardManager)	getSystemService(CLIPBOARD_SERVICE);
                                                myCBM.setText(emaillist.get(position).getRawmessage());
                                                topToastText("Email copied to Clipboard");
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return false;
                            }
                        });
                mMailListView.performHapticFeedback(MODE_APPEND);
                // Advise user of long pressed required for web pages
                topToastText("Mail OUTBOX Screen \n\nLong Press on a header for display");
                break;

            case MAILSENTITEMSVIEW:
                LoadSentMail();
                break;

            default:
                // Either this is what we wanted or it's not implemented yet.
                // Anyway, go to the header's list by default
                // Display mail headers
                LoadHeaders();
                // Set listener for item selection
                mMailListView
                        .setOnItemLongClickListener(new OnItemLongClickListener() {
                            public boolean onItemLongClick(AdapterView<?> parent,
                                                           View view, int position, long id) {
                                if (Processor.Connected) {
                                    // When LONG clicked, send a request to server
                                    String messagestr = (String) mMailListView
                                            .getItemAtPosition(position);
                                    if (messagestr.trim().length() > 0) {
                                        messagestr = messagestr.substring(0,
                                                messagestr.indexOf(" "));
                                        int messagenum = Integer
                                                .parseInt(messagestr);
                                        if (messagenum > 0) {
                                            topToastText("Requesting your Email number "
                                                    + Integer.toString(messagenum)
                                                    + " ...");
                                            if (AndPskmail.myconfig.getPreferenceB(
                                                    "COMPRESSED", false)) {
                                                Processor.TX_Text += "~READZIP "
                                                        + messagestr + "\n";
                                            } else {
                                                Processor.TX_Text += "~READ "
                                                        + messagestr + "\n";
                                            }
                                        } else {
                                            topToastText("Can't determine your message number! Please type in manually below, then press READ");
                                        }
                                    }
                                } else {
                                    bottomToastText("You Must Connect First");
                                }
                                return false;
                            }
                        });
                mMailListView.performHapticFeedback(MODE_APPEND);
                // Advise user of long pressed required for web pages
                topToastText("Mail Headers Screen\n\nLong Press on a header for download");
                break;
        }

        // Initialize the common buttons

        // Initialize the Mail Header button
        myButton = (Button) findViewById(R.id.button_headers);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Open the mail headers layout
                displayMail(BOTTOM, MAILHEADERSVIEW);
            }
        });

        // Initialize the Mail Inbox button
        myButton = (Button) findViewById(R.id.button_inbox);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Open the mail headers layout
                displayMail(BOTTOM, MAILINBOXVIEW);
            }
        });

        // Initialize the Mail Outbox button
        myButton = (Button) findViewById(R.id.button_outbox);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Open the mail headers layout
                displayMail(BOTTOM, MAILOUTBOXVIEW);
            }
        });

        // Initialize the Mail Sent Items button
        myButton = (Button) findViewById(R.id.button_sentitems);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Open the mail headers layout
                displayMail(BOTTOM, MAILSENTITEMSVIEW);
            }
        });

        // JD Initialize the QTC button
        myButton = (Button) findViewById(R.id.button_qtc);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();

                if (Processor.Connected) {
                    if (message.length() > 0) {
                        try {
                            topToastText("Requesting Headers from "
                                    + message.trim());
                            Processor.TX_Text += ("~QTC " + message.trim() + "+\n");
                        }
                        // JD fix this catch action
                        catch (Exception ex) {
                            // System.out.println(ex.getMessage());
                            loggingclass.writelog("Button Execution error: "
                                    + ex.getMessage(), null, true);
                        }
                    } else {
                        String mailnr = "";
                        mailnr = Processor.sm.getHeaderCount(Processor.HomePath
                                + Processor.Dirprefix + "headers");
                        Processor.sm.sendQTC(mailnr);
                        topToastText("Requesting Headers from " + mailnr);
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }

        });

		/*
		 * removed the read button for now since a long press does the job //JD
		 * Initialize the READ Mail button myButton = (Button)
		 * findViewById(R.id.button_read); myButton.setOnClickListener(new
		 * OnClickListener() { public void onClick(View v) { TextView view =
		 * (TextView) findViewById(R.id.edit_text_out); String message =
		 * view.getText().toString();
		 *
		 * if (Processor.Connected){ if (message.length() > 0) { try {
		 * toastText("Requesting your Email..."); if
		 * (AndPskmail.myconfig.getPreferenceB("COMPRESSED", false)) {
		 * Processor.TX_Text += ( "~READZIP " + message + "\n"); } else {
		 * Processor.TX_Text += ( "~READ " + message + "\n"); } } //JD fix this
		 * catch action catch(Exception ex) {
		 * toastText("Type below the email number to Read"); } } else {
		 * Processor.TX_Text += ( "QTC 0+\n"); } } else {
		 * toastText("You must connect First"); } } });
		 */

        // Initialize the New Mail button
        myButton = (Button) findViewById(R.id.button_newmail);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Open the new mail layout
                displayNewmail();
            }
        });

        // JD Initialize the SEND Mail button
        myButton = (Button) findViewById(R.id.button_sendmail);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    SendButtonAction();
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the DELETE Mail button
        myButton = (Button) findViewById(R.id.button_delete);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();

                if (Processor.Connected) {
                    if (message.length() > 0) {
                        try {
                            Processor.TX_Text += ("~DELETE " + message + "\n");
                            topToastText("Requesting Deletion of your email...");
                        }
                        // JD fix this catch action
                        catch (Exception ex) {
                            topToastText("Type below the email number to delete");
                        }
                    } else {
                        Processor.TX_Text += ("QTC 0+\n");
                    }
                } else {
                    bottomToastText("You Must Connect First");
                }
            }
        });

    }

    // Display the Mail Headers layout and associate it's buttons
    private void displayNewmail() {
        currentview = NEWMAILVIEW;

        // Ensure the keyboard is shown
        setContentView(R.layout.newemail);
        // Bring up the keyboard since we asked the system not to do it by default
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        TextView NewToField = (TextView) findViewById(R.id.emailaddress);
        TextView NewSubjectField = (TextView) findViewById(R.id.emailsubject);

        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        // NewSubjectField.setFocusable(true);
        NewSubjectField.requestFocus();
        // NewToField.setFocusable(true);
        NewToField.requestFocus();

        // JD Initialize the BROWSE Contacts button
        myButton = (Button) findViewById(R.id.button_Browse_Contacts);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                            Contacts.CONTENT_URI);
                    startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Browse Contacts button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the POST to Outbox button
        myButton = (Button) findViewById(R.id.button_tooutbox);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView NewToField = (TextView) findViewById(R.id.emailaddress);
                TextView NewSubjectField = (TextView) findViewById(R.id.emailsubject);
                TextView NewTxtArea = (TextView) findViewById(R.id.emailbody);
                FileWriter out = null;
                // Hide the keyboard since we handle it manually
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(NewTxtArea.getWindowToken(), 0);

                try {
                    String NewMailText = "\n~SEND\n";
                    // JD Check logic if
                    // (!AndPskmail.myconfig.getPreference("RETURNADDRESS",
                    // "").equals("")) {
                    NewMailText += "From: "
                            + AndPskmail.myconfig
                            .getPreference("RETURNADDRESS") + "\n";
                    // }
                    NewMailText += "To: " + NewToField.getText() + "\n";
                    NewMailText += "Subject: " + NewSubjectField.getText()
                            + "\n";
                    NewMailText += NewTxtArea.getText();
					/*
					 * JD attachments: Not yet String s = ""; String att = "";
					 * if (myattachment.length() > 0) { att =
					 * Base64.encodeFromFile(myattachment); File ff = new
					 * File(myattachment); String name = ff.getName();
					 *
					 * if (att.length() > 0) { NewMailText +=
					 * "\nYour attachment: filename=" + Character.toString('"')
					 * + name + Character.toString('"') + "\n"; NewMailText +=
					 * att + "\n"; } myattachment = ""; }
					 * Processor.log.writelog("Button Execution error: " +
					 * ex.getMessage(), null, true);
					 */
                    NewMailText += ".\n.\n";

                    File sent = new File(Processor.HomePath + Processor.Dirprefix + "Sentmail");
                    out = new FileWriter(sent, true);
                    out.write("\nSent at: " + Processor.myTime());
                    out.write(NewMailText);
                    out.close();

                    Random r = new Random();
                    String token = Long.toString(Math.abs(r.nextLong()), 12);
                    token = "tmp" + token;
                    File outFile = new File(Processor.HomePath
                            + Processor.Dirprefix + "Outbox"
                            + Processor.Separator + token);
                    out = new FileWriter(outFile);
                    out.write(NewMailText);
                    out.close();

                    // this.setVisible(false);
                    displayMail(NORMAL, lastEmailScreen);
                    topToastText("Email Sent to Outbox");

                } catch (IOException ex) {
                    // JD Fix this
                    // Logger.getLogger(this.getName()).log(Level.SEVERE, null,
                    // ex);
                }
            }

        });

        // JD Initialize the DISCARD Mail button
        myButton = (Button) findViewById(R.id.button_discard);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Hide the keyboard since we handle it manually
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                TextView NewTxtArea = (TextView) findViewById(R.id.emailbody);
                imm.hideSoftInputFromWindow(NewTxtArea.getWindowToken(), 0);
                // Just return to mail headers
                try {
                    displayMail(NORMAL, lastEmailScreen);
                    topToastText("Email Discarded");
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });
    }

    // Display the Modem layout and associate it's buttons
    @SuppressLint("NewApi")
    private void displayModem(int screenAnimation, boolean withWaterfall) {

        if (withWaterfall) {
            currentview = MODEMVIEWwithWF;
            setContentView(R.layout.modemwithwf);
            screenAnimation((ViewGroup) findViewById(R.id.modemwwfscreen), screenAnimation);
            // Get the waterfall view object for the runnable
            myWFView = (waterfallView) findViewById(R.id.WFbox);
        } else {
            currentview = MODEMVIEWnoWF;
            setContentView(R.layout.modemwithoutwf);
            screenAnimation((ViewGroup) findViewById(R.id.modemnwfscreen), screenAnimation);
            myWFView = null;
        }

        myModemTV = (TextView) findViewById(R.id.modemview);
        myModemTV.setHorizontallyScrolling(false);
        myModemTV.setTextSize(16);

        // initialise CPU load bar display
        CpuLoad = (ProgressBar) findViewById(R.id.cpu_load);

        // initialise squelch and signal quality dislay
        SignalQuality = (ProgressBar) findViewById(R.id.signal_quality);

        // Reset modem display in case it was blanked out by a new oncreate call
        myModemTV.setText(ModemBuffer);
        myModemSC = (ScrollView) findViewById(R.id.modemscrollview);
        // update with whatever we have already accumulated then scroll
        AndPskmail.mHandler.post(AndPskmail.addtomodem);

        // Advise user of which screen we are in
        topToastText("Modem Screen");

        if (withWaterfall) { // initialise two extra buttons

            // JD Initialize the Waterfall Sensitivity UP button
            myButton = (Button) findViewById(R.id.button_wfsensup);
            myButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    try {
                        if (myWFView != null) {
                            myWFView.maxvalue /= 1.25;
                            if (myWFView.maxvalue < 1)
                                myWFView.maxvalue = 1.0;
                            // store value into preferences
                            SharedPreferences.Editor editor = AndPskmail.mysp
                                    .edit();
                            editor.putFloat("WFMAXVALUE",
                                    (float) myWFView.maxvalue);
                            // Commit the edits!
                            editor.commit();
                        }
                    }
                    // JD fix this catch action
                    catch (Exception ex) {
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                }
            });

            // JD Initialize the Waterfall Sensitivity DOWN button
            myButton = (Button) findViewById(R.id.button_wfsensdown);
            myButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    try {
                        if (myWFView != null) {
                            myWFView.maxvalue *= 1.25;
                            if (myWFView.maxvalue > 40)
                                myWFView.maxvalue = 40.0;
                            // store value into preferences
                            SharedPreferences.Editor editor = AndPskmail.mysp
                                    .edit();
                            editor.putFloat("WFMAXVALUE",
                                    (float) myWFView.maxvalue);
                            // Commit the edits!
                            editor.commit();
                        }
                    }
                    // JD fix this catch action
                    catch (Exception ex) {
                        loggingclass.writelog(
                                "Button Execution error: " + ex.getMessage(),
                                null, true);
                    }
                }
            });

        }

        // JD Initialize the MODEM RX ON/OFF button
        myButton = (Button) findViewById(R.id.button_modemONOFF);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (ProcessorON) {
                        if (Processor.m.modemState == Modem.RXMODEMRUNNING &&
                                !Processor.Connected && !Processor.TXActive) {
                            modemPaused = true;
                            Processor.m.stopRxModem();
                            stopService(new Intent(AndPskmail.this, Processor.class));
                            //Force garbage collection to prevent Out Of Memory errors on small RAM devices
                            System.gc();
                        }
                        //Only for Android 3.0 and UP
                        //if (Double.valueOf(android.os.Build.VERSION.SDK) >= 11.0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            //Remove the notification
                            myNotificationManager.cancel(9999);
                        }
                        ProcessorON = false;
                        //Set modem text as selectable
                        TextView myTempModemTV = (TextView) findViewById(R.id.modemview);
                        if (myTempModemTV != null) {
                            //if (Double.valueOf(android.os.Build.VERSION.SDK) >= 11) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                myTempModemTV.setTextIsSelectable(true);
                            }
                        }
                    } else {
                        if (Processor.m.modemState == Modem.RXMODEMIDLE) {
                            modemPaused = false;
                            //Force garbage collection to prevent Out Of Memory errors on small RAM devices
                            System.gc();
                            startService(new Intent(AndPskmail.this,
                                    Processor.class));
                            //Only for Android 3.0 and UP
                        }
                        ProcessorON = true;
                        //Set modem text as NOT selectable
                        TextView myTempModemTV = (TextView) findViewById(R.id.modemview);
                        if (myTempModemTV != null) {
                            if (Double.valueOf(android.os.Build.VERSION.SDK) >= 11) {
                                myTempModemTV.setTextIsSelectable(false);
                            }
                        }
                        if (Double.valueOf(android.os.Build.VERSION.SDK) >= 11.0) {
                            //Issue the notification
                            myNotificationManager.notify(9999, myNotification);
                        }
                    }
                    AndPskmail.mHandler.post(AndPskmail.updatetitle);
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the MODE UP button
        myButton = (Button) findViewById(R.id.button_modeUP);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (!Processor.Connected && ProcessorON
                            && !Processor.TXActive && Processor.m.modemState == Modem.RXMODEMRUNNING) {
                        String mylist = "";
                        // choose connection mode: custom list or symmetric
                        // connect (no RSID receive)
                        boolean modelistconnect = AndPskmail.myconfig
                                .getPreferenceB("CONNECTWITHMODELIST", false);
                        if (modelistconnect) {
                            // List of modes (server 1.4.0 onwards) PSK63 to 500
                            // and PSK robust for now
                            mylist = arq.getmodelist();
                        }
                        // Not using custom list OR nothing selected in
                        // Preferences, use all modes
                        if (mylist.length() == 0)
                            mylist = Modem.modemmodecodeSTR;
                        int pos = -1;
                        // Look into custom list first
                        for (int ii = 0; ii < mylist.length(); ii++) {
                            if (Modem.getmode(mylist.charAt(ii)) == Processor.RxModem) {
                                pos = ii;
                            }
                        }
                        if (pos == -1) { // not in custom list, look into all
                            // modes
                            mylist = Modem.modemmodecodeSTR;
                            for (int ii = 0; ii < mylist.length(); ii++) {
                                if (Modem.getmode(mylist.charAt(ii)) == Processor.RxModem) {
                                    pos = ii;
                                }
                            }
                        }
                        if (--pos >= 0) { // still in the list then move
                            // left, otherwise do nothing
                            Processor.RxModem = Modem.getmode(mylist
                                    .charAt(pos));
                            Processor.TxModem = Modem.getmode(mylist
                                    .charAt(pos));
                            Processor.m.changemode(Processor.RxModem); // to make the changes effective
                            AndPskmail.mHandler.post(AndPskmail.updatetitle);
                        }
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the MODE DOWN button
        myButton = (Button) findViewById(R.id.button_modeDOWN);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (!Processor.Connected && ProcessorON
                            && !Processor.TXActive && Processor.m.modemState == Modem.RXMODEMRUNNING) {
                        String mylist = "";
                        // choose connection mode: custom list or symmetric
                        // connect (no RSID receive)
                        boolean modelistconnect = AndPskmail.myconfig
                                .getPreferenceB("CONNECTWITHMODELIST", false);
                        if (modelistconnect) {
                            // List of modes (server 1.4.0 onwards)
                            mylist = arq.getmodelist(); // updates Processor.modelist
                        }
                        // Not using custom list OR nothing selected in
                        // Preferences, use all modes
                        if (mylist.length() == 0)
                            mylist = Modem.modemmodecodeSTR;
                        int pos = 9999; // arbitrary large value
                        // Look into custom list first
                        for (int ii = 0; ii < mylist.length(); ii++) {
                            if (Modem.getmode(mylist.charAt(ii)) == Processor.RxModem) {
                                pos = ii;
                            }
                        }
                        if (pos == 9999) { // not in custom list, look into all
                            // modes
                            mylist = Modem.modemmodecodeSTR;
                            for (int ii = 0; ii < mylist.length(); ii++) {
                                if (Modem.getmode(mylist.charAt(ii)) == Processor.RxModem) {
                                    pos = ii;
                                }
                            }
                        }
                        if (++pos < mylist.length()) { // still in the list, otherwise do nothing
                            Processor.RxModem = Modem.getmode(mylist
                                    .charAt(pos));
                            Processor.TxModem = Modem.getmode(mylist
                                    .charAt(pos));
                            Processor.m.changemode(Processor.RxModem); // to make the changes effective
                            AndPskmail.mHandler.post(AndPskmail.updatetitle);
                        }
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the Squelch UP button
        myButton = (Button) findViewById(R.id.button_squelchUP);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (Processor.m != null) {
                        Processor.m.AddtoSquelch(5.0);
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the Squelch DOWN button
        myButton = (Button) findViewById(R.id.button_squelchDOWN);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (Processor.m != null) {
                        Processor.m.AddtoSquelch(-5.0);
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the TUNE button
        myButton = (Button) findViewById(R.id.button_tune);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {

                    String tuneLengthSTR = AndPskmail.myconfig.getPreference("TUNELENGTH","3");
                    int tuneLength = Integer.parseInt(tuneLengthSTR);

                    if (tuneLength == 0) {
                        Processor.tune = !Processor.tune;
                        if (Processor.tune) {
                            if (!Processor.TXActive && !Processor.Connected &&
                                    Processor.m.modemState == Modem.RXMODEMRUNNING) {
                                Processor.m.TxTune();
                            } else {
                                Processor.tune = false;
                            }
                        }
                    } else {
                        if (!Processor.TXActive && !Processor.Connected &&
                                Processor.m.modemState == Modem.RXMODEMRUNNING) {
                            Processor.m.TxTune();
                        }
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the WATERFALL ON/OFF button
        myButton = (Button) findViewById(R.id.button_waterfallONOFF);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (currentview == MODEMVIEWnoWF) {
                        displayModem(NORMAL,true);
                    } else {
                        displayModem(NORMAL, false);
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

        // JD Initialize the STOP TX button
        myButton = (Button) findViewById(R.id.button_stopTX);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (Processor.TXActive) {
                        Modem.stopTX = true;
                    }
                }
                // JD fix this catch action
                catch (Exception ex) {
                    loggingclass.writelog(
                            "Button Execution error: " + ex.getMessage(), null,
                            true);
                }
            }
        });

    }

    // Display the About screen
    private void displayAbout() {
        currentview = ABOUTVIEW;

        // Open APRS layout by default until we have other activities defined
        setContentView(R.layout.about);
        TextView myversion = (TextView) findViewById(R.id.versiontextview);
        myversion.setText("          " + Processor.version);

        // JD Initialize the return to terminal button
        myButton = (Button) findViewById(R.id.button_returntoterminal);
        myButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                displayTerminal(BOTTOM);
            }
        });

    }

    // When pressing the send button on the email or terminal screen
    private void SendButtonAction() {
        if (Processor.Connected) {
            // FileReader out = null;
            if (Processor.compressedmail) {
                try {

                    File dir = new File(Processor.HomePath
                            + Processor.Dirprefix + "Outbox");

                    File[] files = dir.listFiles();

                    long files0length = 0;
                    if (files.length > 0) {
                        files0length = files[0].length();
                    }

                    File dir2 = new File(Processor.HomePath
                            + Processor.Dirprefix + "Outpending");

                    File[] files2 = dir2.listFiles();

                    if (files2.length > 0) {
                        topToastText("Trying_to_send_your_email...");
                        if (files2[0].length() > 0) {

                            String FileOut = files2[0].toString();
                            int i = FileOut.lastIndexOf(File.separator);
                            String ffilename = (i > -1) ? FileOut
                                    .substring(i + 1) : FileOut;

                            File fpendir = new File(Processor.Outpendingdir);
                            File[] fpendingfiles = fpendir.listFiles();

                            String fname = "mail";
                            File testTransactions = new File(
                                    Processor.Transactions);
                            if (testTransactions.exists()) {

                                FileReader trf = new FileReader(
                                        Processor.Transactions);
                                BufferedReader bf = new BufferedReader(trf);
                                String s = "";

                                while ((s = bf.readLine()) != null) {
                                    if (s.contains(ffilename)) {
                                        String[] ss = s.split(":");
                                        fname = ss[5];
                                    }
                                }
                            }

                            if (fpendingfiles.length > 0) {
                                String penfile0 = fpendingfiles[0].toString();

                                if (penfile0.length() > 0) {
                                    String outfile = penfile0;
                                    i = outfile.lastIndexOf(File.separator);
                                    ffilename = (i > -1) ? outfile
                                            .substring(i + 1) : outfile;
                                    Processor.TX_Text += "~FO5:"
                                            + Processor.sm.mycall
                                            + ":"
                                            + Processor.sm.myserver
                                            + ":"
                                            + ffilename
                                            + ":u:"
                                            + fname
                                            + ":"
                                            + Long.toString(fpendingfiles[0]
                                            .length()) + "\n";
                                }
                            }
                        }
                        // Prevent Exception on NO FILE: } else if (files.length
                        // > 0 & files[0].length() > 0) {
                    } else if (files.length > 0 && files0length > 0) {
                        topToastText("Trying to send your email...");
                        Processor.Mailoutfile = files[0].toString();
                        int j = Processor.Mailoutfile
                                .lastIndexOf(File.separator);
                        String filename = (j > -1) ? Processor.Mailoutfile
                                .substring(j + 1) : Processor.Mailoutfile;

                        File pendir = new File(Processor.Pendingdir);
                        File[] pendingfiles = pendir.listFiles();

                        if (pendingfiles.length > 0) {
                            String penfile0 = pendingfiles[0].toString();

                            if (penfile0.length() > 0) {
                                String outfile = penfile0;
                                j = outfile.lastIndexOf(File.separator);
                                filename = (j > -1) ? outfile.substring(j + 1)
                                        : outfile;
                                Processor.TX_Text += "~FO5:"
                                        + Processor.mycall
                                        + ":"
                                        + Processor.sm.myserver
                                        + ":"
                                        + filename
                                        + ":s: :"
                                        + Long.toString(pendingfiles[0]
                                        .length()) + "\n";
                            }
                        } else {
                            // no pending message
                            String zippedfile = Processor.HomePath
                                    + Processor.Dirprefix + "tmp.mail";
                            String codedfile = Processor.HomePath
                                    + Processor.Dirprefix + "tmp2.mail";

                            FileInputStream in = new FileInputStream(
                                    Processor.Mailoutfile);
                            GZIPOutputStream gzout = new GZIPOutputStream(
                                    new FileOutputStream(zippedfile));
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                gzout.write(buffer, 0, bytesRead);
                            }
                            in.close();
                            gzout.close();
                            Base64.encodeFileToFile(zippedfile, codedfile);

                            File filelength = new File(codedfile);

                            int length = (int) filelength.length();
                            Session.DataSize = length;
                            Session.DataReceived = 0;
                            String lengthstr = Integer.toString(length);

                            FileReader b64in = new FileReader(codedfile);
                            //
                            BufferedReader br = new BufferedReader(b64in);

                            FileWriter fstream = new FileWriter(
                                    Processor.Pendingdir + filename);
                            BufferedWriter pout = new BufferedWriter(fstream);

                            if (Processor.protocol == 0) {
                                Processor.TX_Text += "~CSEND\n";
                            } else {
                                String callsign = AndPskmail.myconfig
                                        .getPreference("CALL");
                                callsign = callsign.trim();
//										String servercall = AndPskmail.myconfig.getPreference("SERVER");
                                String servercall = AndPskmail.serverToCall;
                                servercall = servercall.trim();
                                Processor.TX_Text += ">FM:" + callsign + ":"
                                        + servercall + ":" + filename + ":s: :"
                                        + lengthstr + "\n";
                            }
                            String record = null;
                            while ((record = br.readLine()) != null) {
                                Processor.TX_Text += record + "\n";
                                if (Processor.protocol > 0) {
                                    pout.write(record + "\n");
                                }
                            }
                            Processor.TX_Text += "-end-\n";
                            if (Processor.protocol > 0) {
                                pout.write("-end-\n");
                                pout.close();
                            }

                            Session.DataSize = Processor.TX_Text.length();

                            boolean success = true;
                            File f = new File(zippedfile);
                            if (f.exists()) {
                                success = f.delete();
                            }
                            if (!success) {
                                loggingclass.writelog(
                                        "Error deleting temporary zip file",
                                        null, true);
                            }

                        }
                    } else {
                        topToastText("No Mail to Send...");
                    }
                } catch (IOException ex) {
                    // JD Fix logging
                    // Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE,
                    // null, ex);
                    loggingclass
                            .writelog("Error processing compressed Mail Send",
                                    null, true);
                }
            } else {
                // not compressed (default)
                try {
                    File dir = new File(Processor.HomePath
                            + Processor.Dirprefix + "Outbox");
                    File[] files = dir.listFiles();

                    if (files.length > 0) {
                        topToastText("Trying to send your email...");
                        Processor.Mailoutfile = files[0].getAbsolutePath();
                        FileReader in = new FileReader(files[0]);
                        BufferedReader br = new BufferedReader(in);
                        String record = null;
                        while ((record = br.readLine()) != null) {
                            Processor.TX_Text += record + "\n";
                        }

                    } else {
                        topToastText("No Mail to Send...");
                    }
                } catch (IOException ex) {
                    // JD Fix logging
                    // Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE,
                    // null, ex);
                    loggingclass.writelog(
                            "Error processing UNcompressed Mail Send", null,
                            true);
                }
            }
        } else {
            bottomToastText("You Must Connect First");
        }
    }

    private void LoadWebpages() {
        try {
            WebArrayAdapter.clear();
            for (int i = 6; i > 0; i--) {
                String url = AndPskmail.myconfig.getPreference(
                        "URL" + Integer.toString(i), "");
                String beginstring = AndPskmail.myconfig.getPreference("URL"
                        + Integer.toString(i) + "B", "");
                if (beginstring.trim().length() > 0) {
                    beginstring = "   Begin: " + beginstring;
                }
                String endstring = AndPskmail.myconfig.getPreference("URL"
                        + Integer.toString(i) + "E", "");
                if (endstring.trim().length() > 0) {
                    endstring = "   End: " + endstring;
                }
                if (url.trim().length() > 0) {
                    // WebArrayAdapter.add(Integer.toString(i) + ".  " + url +
                    // beginstring + endstring);
                    WebArrayAdapter.insert(Integer.toString(i) + ".  " + url
                            + beginstring + endstring, 0);
                } else {
                    // WebArrayAdapter.add(Integer.toString(i) + ".  ");
                    WebArrayAdapter.insert(Integer.toString(i) + ".  ", 0);
                }
            }

        } catch (Exception e) {
            loggingclass.writelog("Error when showing mail headers.", e, true);
        }
    }

    private void LoadHeaders() {
        try {
            // Open the file
            FileReader frs = new FileReader(Processor.HomePath
                    + Processor.Dirprefix + "headers");
            BufferedReader br = new BufferedReader(frs);
            String s;
            MailArrayAdapter.clear();
            while ((s = br.readLine()) != null) {
                //Trim initial spaces
                while (s.startsWith(" ") && s.length() > 1) {
                    s = s.substring(s.indexOf(" ") + 1);
                }
                if (s.length() > 10) {
                    String[] mystrarr = new String[4];
                    Integer myA = s.indexOf(" ");
                    if (myA < 1) //stop gap
                        myA = 1;
                    mystrarr[0] = s.substring(0, myA).trim(); // Number
                    // From
                    Integer myB = s.indexOf("  ");
                    mystrarr[1] = s.substring(myA + 1, myB).trim();
                    // Subject
                    Integer myC = s.lastIndexOf(" ");
                    mystrarr[2] = s.substring(myB + 2, myC).trim();
                    // Size
                    mystrarr[3] = s.substring(myC).trim();
                    MailArrayAdapter.add(mystrarr[0] + "  " + mystrarr[1]
                            + "  " + mystrarr[2] + "  " + mystrarr[3]);
                }
            }
            frs.close();
        } catch (Exception e) {
            loggingclass.writelog("Error when showing mail headers.", e, true);
        }
    }

    // Loads the list of Mail from Inbox file
    private void LoadInbox() {
        try {
            MessageViewHandler inboxHandler = new MessageViewHandler(
                    Processor.HomePath + Processor.Dirprefix + "Inbox");

            if (inboxHandler.Fetchmbox()) {
                emaillist = inboxHandler.getEmaillist();
                MailArrayAdapter.clear();
                for (int i = 0; i < emaillist.size(); i++) {
                    String thiscontent = emaillist.get(i).getContent();
                    MailArrayAdapter.add(emaillist.get(i).getFrom()
                            + "  "
                            + emaillist.get(i).getSubject()
                            + "  "
                            + emaillist.get(i).getDatestr()
                            + "  "
                            + thiscontent.substring(
                            0,
                            thiscontent.length() < 30 ? thiscontent
                                    .length() : 30));
                }
            }
        } catch (Exception e) {
            loggingclass.writelog("Error when showing inbox.", e, true);
        }
    }

    // Loads the list of Mail from Outbox directory (several file)
    private void LoadOutbox() {

        try {
            // Get the list of files in the outbox
            File dir = new File(Processor.HomePath + Processor.Dirprefix
                    + "Outbox");
            File[] files = dir.listFiles(); // This filter only returns files
            FileFilter fileFilter = new FileFilter() {
                public boolean accept(File file) {
                    return file.isFile();
                }
            };

            // We should now have an array of strings containing the file names
            files = dir.listFiles(fileFilter);
            MessageViewHandler outboxHandler = new MessageViewHandler(files);

            if (outboxHandler.FetchOutbox()) {
                emaillist = outboxHandler.getEmaillist();
                MailArrayAdapter.clear();
                for (int i = 0; i < emaillist.size(); i++) {
                    String thiscontent = emaillist.get(i).getContent();
                    MailArrayAdapter.add(emaillist.get(i).getTo()
                            + "  "
                            + emaillist.get(i).getSubject()
                            + "  "
                            + thiscontent.substring(
                            0,
                            thiscontent.length() < 30 ? thiscontent
                                    .length() : 30));
                }
            }
        } catch (Exception e) {
            loggingclass.writelog("Error when showing Outbox.", e, true);
        }
    }


    private void LoadSentMail() {
        try {
            //Check is file is present. If not, create a blank one
            File sentItemsFile = new File(Processor.HomePath
                    + Processor.Dirprefix + "sentmail");
            if (!sentItemsFile.exists()) {
                // File did not exist, create it
                sentItemsFile.createNewFile();
            }

            // Open the file
            FileReader frs = new FileReader(Processor.HomePath
                    + Processor.Dirprefix + "sentmail");
            BufferedReader br = new BufferedReader(frs);
            String s;
            MailArrayAdapter.clear();
            String thisMail = "";
            boolean storing = false;
            while ((s = br.readLine()) != null) {
                if (!storing) {
                    //Located the start of the first email?
                    if (s.startsWith("Sent at:")) {
                        storing = true;
                        thisMail = s;
                    }
                } else {
                    //Found a new sent email?
                    if (s.startsWith("Sent at:")) {
                        //Save it and init the next email string
                        MailArrayAdapter.add(thisMail);
                        thisMail = s;
                    } else {
                        //Just add that line
                        thisMail = thisMail + "\n" + s;
                    }
                }

            }
            //Have we got a last one to add when we reached EOF?
            if (thisMail.length() > 0) {
                MailArrayAdapter.add(thisMail);
            }
            // Advise user which screen we are in
            topToastText("Mail SENT Items Screen");
            frs.close();
        } catch (Exception e) {
            loggingclass.writelog("Error when showing Sent Mail.", e, true);
        }
    }




    // Process the abort Button
    private void AbortButtonAction() {
        // In case the client is TXing, ignore and ask operator to press while
        // RXing
        if (!Processor.TXActive) {

            Processor.Bulletinmode = false;
            Processor.Connecting = false;
            Processor.Scanning = false;
            Processor.sm.FileDownload = false;

            if (Processor.Connected) {
                try {

                    Processor.q.send_abort();
                } catch (InterruptedException ex) {
                    // JD Fix logging
                    // Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE,
                    // null, ex);
                }
                try {
                    if (Processor.sm.pFile != null) {
                        Processor.sm.pFile.close();
                    }
                } catch (IOException e) {
                    Processor.q.Message("Cannot close pending file", 10);
                }
                try {
                    if (Processor.sm.dlFile != null) {
                        Processor.sm.dlFile.close();
                    }
                } catch (IOException e) {
                    Processor.q.Message("Cannot close pending file", 10);
                }

            } else if (Processor.TTYConnected.equals("Connected")) {
                try {
                    Processor.q.send_abort();
                } catch (InterruptedException ex) {
                    // JD Ficx Logger
                    // Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE,
                    // null, ex);
                }
            } else {
                try {
                    Processor.Connected = true;
                    // disableMboxMenu();
                    Processor.q.send_abort();
                } catch (InterruptedException ex) {
                    // JD Fix logging
                    // Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE,
                    // null, ex);
                }

            }

            // bConnect.setText(java.util.ResourceBundle.getBundle("javapskmail/mainpskmailui").getString("Connect"));
            // Conn_connect.setText(java.util.ResourceBundle.getBundle("javapskmail/mainpskmailui").getString("Connect"));
            // FileConnect.setText(java.util.ResourceBundle.getBundle("javapskmail/mainpskmailui").getString("Connect"));

            Processor.Connected = false;
            Processor.TTYConnected = "";
            Processor.sm.FileDownload = false;
            Processor.Status = "Listening";

        } else {
            topToastText("Not Active during TX. Wait for RX and Press Again...");

        }

    }

    // Delete pending downloads (Pending directory)
    private void ClearDownloads() {
        File dir = new File(Processor.Pendingdir);
        if (dir.exists()) {
            String[] info = dir.list();
            for (int i = 0; i < info.length; i++) {
                @SuppressWarnings("static-access")
                File n = new File(Processor.Pendingdir + dir.separator
                        + info[i]);
                if (!n.isFile()) {
                    continue;
                } else {
                    n.delete();
                }

            }
        }
    }

    // Delete pending uploads (Outpending directory)
    private void ClearUploads() {
        FileReader tin = null;
        File uplds = new File(Processor.Transactions);
        if (uplds.exists()) {
            uplds.delete();
        }
        String fileName = Processor.Outpendingdir;
        // A File object to represent the filename
        File dir = new File(fileName);

        // Make sure the file or directory exists and isn't write protected
        if (dir.exists()) {
            String[] info = dir.list();
            for (int i = 0; i < info.length; i++) {
                @SuppressWarnings("static-access")
                File n = new File(Processor.Outpendingdir + dir.separator
                        + info[i]);
                if (!n.isFile()) {
                    continue;
                } else {
                    n.delete();
                }

            }
        }
    }

    // Delete Outbox directory
    private void ClearOutbox() {
        File dir = new File(Processor.HomePath + Processor.Dirprefix + "Outbox");
        if (dir.exists()) {
            String[] info = dir.list();
            for (int i = 0; i < info.length; i++) {
                File n = new File(Processor.HomePath + Processor.Dirprefix
                        + "Outbox" + File.separator + info[i]);
                if (!n.isFile()) // skip ., .., other directories too
                    continue;

                if (!n.delete()) {
                    topToastText("Couldn't remove " + n.getPath());
                }
            }
        }

    }

    // List all pending transcations in terminal
    public static void ListPendingTransactions() {
        File tr = new File(Processor.Transactions);
        FileReader fr1 = null;

        if (tr.exists()) {
            Processor.PostToTerminal("\nPending uploads:\n");
            try {
                fr1 = new FileReader(tr);
            } catch (FileNotFoundException ex) {
                // Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE,
                // null, ex);
            }

            BufferedReader br = new BufferedReader(fr1);
            String brl = "";
            try {
                while ((brl = br.readLine()) != null) {
                    String[] ss = brl.split(":");
                    if (ss[4].equals("u")) {
                        Processor.PostToTerminal(ss[2] + ":" + ss[5] + ":"
                                + ss[6] + "\n");
                    }
                }
            } catch (IOException ex) {
                // Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE,
                // null, ex);
            }
            try {
                br.close();
            } catch (IOException ex) {
                // Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE,
                // null, ex);
            }
        }

        File outb1 = new File(Processor.HomePath + Processor.Dirprefix
                + "Outbox");
        int i1 = outb1.list().length;
        Processor.PostToTerminal("\nOutbox:" + Integer.toString(i1) + "\n");

        File outb = new File(Processor.Pendingdir);
        int i = outb.list().length;

        Processor.PostToTerminal("Incomplete Downloads:" + Integer.toString(i)
                + "\n\n");
    }

}
