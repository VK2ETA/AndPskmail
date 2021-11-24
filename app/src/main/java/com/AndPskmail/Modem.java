/*
 * Modem.java
 *
 * Copyright (C) 2011 John Douyere (VK2ETA) - for Android platforms
 * Copyright (C) 2008 Per Crusefalk and Rein Couperus
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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


public class Modem {


    public static final int RXMODEMIDLE = 0;
    public static final int RXMODEMSTARTING = 1;
    public static final int RXMODEMRUNNING = 2;
    public static final int RXMODEMPAUSED = 3;
    public static final int RXMODEMSTOPPING = 4;

    public int modemState = RXMODEMIDLE;

    public boolean modemThreadOn = true;

    //	public static boolean rxHasStopped = true;
    public static int NumberOfOverruns = 0;
    private static AudioRecord rxAudioRecorder = null;
    private boolean RxON = false;
    private static int rxBufferSize = 0;
    private static int txBufferSize = 0;
    private final static float sampleRate = 8000.0f;
    public static AudioTrack txAudioTrack = null;

    public static double[] WaterfallAmpl = new double[RxRSID.RSID_FFT_SIZE];
    public static boolean newAmplReady = false;

    private static boolean BlockActive = false;
    //public static String MonitorBuffer = "";
    //private static StringBuilder MonitorBuffer = new StringBuilder(11000);
    //private static String BlockBuffer = "";
    private static StringBuilder BlockBuffer = new StringBuilder(500);
    private static int RxBlockSize = 0;
    public static long lastCharacterTime = 0;

    public static TxPSK TXMpsk;
    public static TxMFSK TXMmfsk;
    public static TxTHOR TXMthor;
    public static RxPSK RXMpsk;
    public static RxMFSK RXMmfsk;
    public static RxTHOR RXMthor;
    public static RxRSID myRxRSID;

    private static SampleRateConversion myResampler;

    public double rxFrequency = 1500.0;
    double squelch = 20.0;


    public static boolean stopTX = false;


    public Modem () {

        //Prepare RSID Receive
        myRxRSID =  new RxRSID();
        //Initialise modems
        RXMpsk = new RxPSK(Processor.RxModem);
        RXMmfsk = new RxMFSK(Processor.RxModem);
        RXMthor = new RxTHOR(Processor.RxModem);
        //Initialize Re-sampling to 11025Hz for RSID, THOR and MFSK modems
        myResampler = new SampleRateConversion(11025.0 / 8000.0);

    }



    public static String getModemString (modemmodeenum mode) {
        return mode.toString();
    }



    public static String getAltModemString (modemmodeenum mode) {
        return mode.toString();
    }


    //THE ORDER MUST MATCH the list in modemmodeenum.java
    static public final char modemmodecode[] = { 'f','1','2','d','g','e','3','4','b','a','5','9','c','6','8','7' };
    static public final String modemmodecodeSTR = "786c95ab43d21";



    //Returns the enum corresponding to the String value of the modem
    public static modemmodeenum getModeFromName(String modeStr) {
        modemmodeenum mode = modemmodeenum.MFSK32;
        for (int i=0; i<modemmodecode.length; i++)
            if (modemmodeenum.values()[i].toString().equals(modeStr)) {
                mode = modemmodeenum.values()[i];
            }
        return mode;
    }


/*
From ARQ.pm of server version 2.45
my %modelist = ("0" => "default",
				"1" => "THOR8",
				"2" => "MFSK16",
				"3" => "THOR22",
				"4" => "MFSK32",
				"5" => "PSK250R",
				"6" => "PSK500R",
				"7" => "PSK500",
				"8" => "PSK250",
				"9" => "PSK125",
				"a" => "PSk63",
				"b" => "PSK125R",
				"c" => "MFSK64",
				"d" => "THOR11",
				"e" => "THOR4",
				"f" => "Contestia",
				"g" => "PSK1000",
				"h" => "PSK63RC5",
				"i" => "PSK63RC10",
				"j" => "PSK250RC3",
				"k" => "PSK125RC4",
				"l" => "DOMINOEX22",
				"m" => "DOMINOEX11");
 */
    //Returns the ennum from the character in the connect list of modes
    public static modemmodeenum getmode (char modemcode) {
        modemmodeenum outmode = modemmodeenum.PSK250R;
        switch (modemcode){
            case 'f':
                outmode = modemmodeenum.MFSK8;
                break;
            case '1':
                outmode = modemmodeenum.THOR8;
                break;
            case '2':
                outmode = modemmodeenum.MFSK16;
                break;
            case 'd':
                outmode = modemmodeenum.THOR11;
                break;
            case 'g':
                outmode = modemmodeenum.THOR16;
                break;
            case 'e':
                outmode = modemmodeenum.PSK31;
                break;
            case '3':
                outmode = modemmodeenum.THOR22;
                break;
            case '4':
                outmode = modemmodeenum.MFSK32;
                break;
            case 'b':
                outmode = modemmodeenum.PSK125R;
                break;
            case 'a':
                outmode = modemmodeenum.PSK63;
                break;
            case '5':
                outmode = modemmodeenum.PSK250R;
                break;
            case '9':
                outmode = modemmodeenum.PSK125;
                break;
            case 'c':
                outmode = modemmodeenum.MFSK64;
                break;
            case '6':
                outmode = modemmodeenum.PSK500R;
                break;
            case '8':
                outmode = modemmodeenum.PSK250;
                break;
            case '7':
                outmode = modemmodeenum.PSK500;
                break;
            default:
                outmode = modemmodeenum.PSK250R;
        }
        return outmode;
    }


    //return the array of selected modes
    public static String[] getSelectedModesArray() {
        String[] thatlist = new String[modemmodeenum.values().length];

        int j = 0;
        for (int i = 0; i < modemmodeenum.values().length; i++) {
            boolean thatmode = AndPskmail.myconfig.getPreferenceB("USE"+modemmodeenum.values()[i].toString(), false);
            if (thatmode) {
                thatlist[j++] = modemmodeenum.values()[i].toString();
            }
        }
        String[] returnArray = new String[j];
        System.arraycopy(thatlist, 0, returnArray, 0, j);

        return returnArray;
    }



    private void soundInInit() {

        rxBufferSize = (int) sampleRate; // 1 second of Audio max
        if (rxBufferSize < AudioRecord.getMinBufferSize((int) sampleRate, AudioFormat.CHANNEL_IN_MONO , AudioFormat.ENCODING_PCM_16BIT)) {
            // Check to make sure buffer size is not smaller than the smallest allowed one
            rxBufferSize = AudioRecord.getMinBufferSize((int) sampleRate, AudioFormat.CHANNEL_IN_MONO , AudioFormat.ENCODING_PCM_16BIT);
        }
        int ii = 20; //number of 1/4 seconds wait
        while (--ii > 0) {
            if (AndPskmail.toBluetooth) {
                //Bluetooth hack (use voice call)
                rxAudioRecorder = new AudioRecord(AudioSource.MIC, 8000, android.media.AudioFormat.CHANNEL_IN_MONO,
                        android.media.AudioFormat.ENCODING_PCM_16BIT, rxBufferSize);
            } else {
                rxAudioRecorder = new AudioRecord(AudioSource.MIC, 8000, android.media.AudioFormat.CHANNEL_IN_MONO,
                        android.media.AudioFormat.ENCODING_PCM_16BIT, rxBufferSize);
            }
            if (rxAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                ii = 0;//ok done
            } else {
                if (ii < 10) { //Only if have to wait more than 1 seconds
                    loggingclass.writelog("\nWaiting for Audio MIC availability..." , null, true);
                }
                try {
                    Thread.sleep(100);//1/2 second
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (rxAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            //Check the permission for audio recording
            if (ContextCompat.checkSelfPermission(AndPskmail.myContext, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                //Request permission from the user
                final int REQUEST_PERMISSIONS = 15559;
                String[] permissionList = {Manifest.permission.RECORD_AUDIO};
                ActivityCompat.requestPermissions(AndPskmail.myInstance, permissionList, REQUEST_PERMISSIONS);
            }
            loggingclass.writelog("\n\nCan't open Audio Input" , null, true);
        }
    }

/* initialise ALL modems
 	private void rxInit() {
		if (Processor.RxModem.toString().startsWith("PSK")) {
			//Initialise PSK modems
			if (RXMpsk != null) RXMpsk.rxInit();
		} else if (Processor.RxModem.toString().startsWith("MFSK")) {
			//Initialise PSK modems
			if (RXMmfsk != null) RXMmfsk.rxInit();
		} else if (Processor.RxModem.toString().startsWith("THOR")) {
			//Initialise PSK modems
			if (RXMthor != null) RXMthor.rxInit();
		} else {
			loggingclass.writelog("Wrong RX Mode called: " + Processor.RxModem.toString() , null, true);
		}
	}
 */

    private void rxInit() {
        //Initialise PSK modems
        if (RXMpsk != null) RXMpsk.rxInit();
        //Initialise PSK modems
        if (RXMmfsk != null) RXMmfsk.rxInit();
        //Initialise PSK modems
        if (RXMthor != null) RXMthor.rxInit();
    }


    public void startmodem() {

        modemThreadOn = true;

        new Thread(new Runnable() {
            public void run() {


                while (modemThreadOn) {


                    modemState = RXMODEMSTARTING;
                    double startproctime = 0;
                    double endproctime = 0;
                    int numSamples8K = 0;
                    soundInInit();
                    rxInit();
                    NumberOfOverruns = 0;
                    try {
                        rxAudioRecorder.startRecording();
                        RxON = true;
                    } catch (IllegalStateException e) {
                        //e.printStackTrace();
                        loggingclass.writelog("\n\nCan't access the Audio Input. Either it is in used by another application" +
                                " OR the Permission to record Audio has been denied.\n" +
                                "Modem reception has been disabled until the above is rectified.\n\n" , null, true);
                        AndPskmail.myInstance.topToastText("Can't access the Audio Input. Either it is in used by another application" +
                                "OR the Permission to record Audio has been denied.\n" +
                                "Modem reception has been disabled until the above is rectified.\n\n");
                        if (Processor.m.modemState == Modem.RXMODEMRUNNING) {
                            Processor.m.stopRxModem();
                            AndPskmail.myInstance.stopService(new Intent(AndPskmail.myInstance, Processor.class));
                            AndPskmail.ProcessorON = false;
                        }
                    }
                    Processor.restartRxModem.drainPermits();
                    //Since the callback is not working, implement a while loop.
                    short[] so8K = new short[rxBufferSize];
                    int size12Kbuf =  (int) ((rxBufferSize + 1) * 11025.0 / 8000.0);
                    double[] so12K = new double[size12Kbuf];
                    if (Processor.RxModem.toString().startsWith("PSK")) {
                        //Change PSK modem
                        Modem.RXMpsk.changemode(Processor.RxModem);
                    } else if (Processor.RxModem.toString().startsWith("THOR")) {
                        //Change MFSK modem
                        Modem.RXMthor.changemode(Processor.RxModem);
                    } else if (Processor.RxModem.toString().startsWith("MFSK")) {
                        //Change MFSK modem
                        Modem.RXMmfsk.changemode(Processor.RxModem);
                    } else {
                        loggingclass.writelog("Wrong RX Mode called in RxThread: " + Processor.TxModem.toString() +"\n" , null, true);
                    }
                    while (RxON) {
                        endproctime = System.currentTimeMillis();
                        double buffertime = (double) numSamples8K / 8000.0 * 1000.0; //in milliseconds
                        if (numSamples8K > 0) Processor.cpuload = (int)(((double)(endproctime - startproctime)) / buffertime * 100);
                        if (Processor.cpuload > 100) Processor.cpuload = 100;
                        AndPskmail.mHandler.post(AndPskmail.updatecpuload);
                        numSamples8K = rxAudioRecorder.read(so8K, 0, 8000/4); //process only part of the buffer to avoid lumpy processing
                        if (numSamples8K > 0) {
                            modemState = RXMODEMRUNNING;
                            startproctime = System.currentTimeMillis();
                            //Process only if Rx is ON, otherwise discard (we have already decided to TX)
                            if (RxON) {
                                //Re-sample to 11025Hz for RSID, THOR and MFSK modems
                                int numSamples12K = myResampler.Process(so8K, numSamples8K, so12K, size12Kbuf);
                                //RxRSID always ON
                                myRxRSID.receive(so12K, numSamples12K);
                                //Then the selected RX modem
                                if (Processor.RxModem.toString().startsWith("PSK")) {
                                    RXMpsk.rxProcess(so8K, numSamples8K);
                                } else if (Processor.RxModem.toString().startsWith("MFSK")) {
                                    RXMmfsk.rxProcess(so8K, numSamples8K);
                                } else if (Processor.RxModem.toString().startsWith("THOR")) {
                                    RXMthor.rxProcess(so8K, numSamples8K, so12K, numSamples12K);
                                } else {
                                    loggingclass.writelog("Wrong RX Mode called: " + Processor.RxModem.toString() , null, true);
                                }
                            }
                            //Post to monitor (Modem) window after each buffer processing
                            //Add TX frame too if present
                            //if (MonitorBuffer.length() > 0 || Processor.TXmonitor.length() > 0) {
                            //if (Processor.TXmonitor.length() > 0) {
                                //Processor.monitor += Modem.MonitorBuffer + Processor.TXmonitor;
                            //    appendToModemBuffer(Processor.TXmonitor);
                            //    Processor.TXmonitor = "";
                                //Modem.MonitorBuffer = "";
                            //    AndPskmail.mHandler.post(AndPskmail.updateModemScreen);
                            //}
                        }
                    }//while (RxON)
                    //We dropped here on pause flag
                    if (rxAudioRecorder != null) {
                        //Avoid some crashes on wrong state
                        if (rxAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                            if (rxAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                                rxAudioRecorder.stop();
                            }
                            rxAudioRecorder.release();
                        }
                    }
                    modemState = RXMODEMPAUSED;
                    //Marker for end of thread (Stop modem thread flag)
                    if (!modemThreadOn) {
                        modemState = RXMODEMIDLE;
                        return;
                    }
                    //Now waits for a restart (or having this thread killed)
                    Processor.restartRxModem.acquireUninterruptibly(1);
                    //Make sure we don's have spare permits
                    Processor.restartRxModem.drainPermits();
                }//while (modemThreadOn)
                //We dropped here on thread stop request
                modemState = RXMODEMIDLE;
            } //run
        }).start(); //	new Thread(new Runnable() {

    }


    public void stopRxModem() {
        modemThreadOn = false;
        RxON = false;
    }

    public void pauseRxModem() {
        RxON = false;
    }

    public void unPauseRxModem() {
        Processor.restartRxModem.release(1);
    }

    void changemode(modemmodeenum newmode) {
        //Stop the modem receiving side to prevent using the wrong values
        pauseRxModem();
        //Restart modem reception
        unPauseRxModem();

    }

    void setFrequency(double rxfreq) {
        rxFrequency = rxfreq;
    }

    void reset() {
        String frequencySTR = AndPskmail.myconfig.getPreference("AFREQUENCY","1000");
        rxFrequency = Integer.parseInt(frequencySTR);
        if (rxFrequency < 500) rxFrequency = 500;
        if (rxFrequency > 2500) rxFrequency = 2500;
        squelch = AndPskmail.mysp.getFloat("SQUELCHVALUE", (float) 20.0);
    }


    /**
     * @param squelchdiff the delta to add to squelch
     */
    public void AddtoSquelch(double squelchdiff) {
        squelch += (squelch > 10) ? squelchdiff : squelchdiff / 2;
        if (squelch < 0) squelch =0;
        if (squelch >100) squelch =100;
        //store value into preferences
        SharedPreferences.Editor editor = AndPskmail.mysp.edit();
        editor.putFloat("SQUELCHVALUE", (float) squelch);
        // Commit the edits!
        editor.commit();

    }

    //Appends received string to modem buffer
    public static void appendToModemBuffer(String rxedCharacters) {
        synchronized(AndPskmail.modemBufferlock) {
            AndPskmail.ModemBuffer.append(rxedCharacters);
        }
    }


    //Same for single character
    public static void appendToModemBuffer(char rxedCharacter) {
        synchronized(AndPskmail.modemBufferlock) {
            AndPskmail.ModemBuffer.append(rxedCharacter);
        }
    }


    public static void rxblock (char inChar) {

        if (!Processor.Connected & !Processor.Connecting) {
            Processor.DCD = Processor.MAXDCD;
        }
        //Save the time of the last character received
        lastCharacterTime = System.currentTimeMillis();
        if (inChar > 127) {
            // todo: unicode encoding
            inChar = 0;
        }
        switch (inChar) {
            case 0:
                break; // do nothing
            case 1:
                appendToModemBuffer("<SOH>");
                AndPskmail.mHandler.post(AndPskmail.updateModemScreen);
                if (!BlockActive) {
                    BlockActive = true;
                    BlockBuffer.delete(0,BlockBuffer.length());
                    BlockBuffer.append("<SOH>");
                } else {
                    //Process block if of right size
                    BlockBuffer.append("<SOH>");
                    RxBlockSize = BlockBuffer.length() - 17;
                    Processor.Totalbytes += Processor.RXBlocksize;
                    if (RxBlockSize > 0 ) {
                        Processor.ProcessBlock(BlockBuffer.toString());
                    }
                    //Reset for next block
                    BlockBuffer = new StringBuilder(500);
                    BlockBuffer.append("<SOH>");
                }
                //	            Main.DCD = 0;
                break;
            case 4:
                //Store the snr value for ARQ usage
                Processor.snr = Processor.avgsnr;
                Processor.avgsnr = 50; //reset to midrange
                appendToModemBuffer("<EOT>");
                //Force immediate update of screen
                AndPskmail.lastUpdateTime = 0L;
                AndPskmail.mHandler.post(AndPskmail.updateModemScreen);
                if (BlockActive == true) {
                    BlockBuffer.append("<EOT>");
                    Processor.ProcessBlock(BlockBuffer.toString());
                    BlockBuffer.delete(0,BlockBuffer.length());
                }
                if (BlockActive) {
                    BlockActive = false;
                }
                break;
            case 31:
                appendToModemBuffer("<US>");
                break;
            case 10:
            case 13:
                appendToModemBuffer("\n");
                if (BlockActive == true) {
                    BlockBuffer.append(inChar);
                }
                break;
            default:
                if (BlockActive) {
                    BlockBuffer.append(inChar);
                }
                if (inChar > 31) {
                    //MonitorBuffer.append(inChar);
                    appendToModemBuffer(inChar);
                    AndPskmail.mHandler.post(AndPskmail.updateModemScreen);
                }
                break;
        }
        // end switch
    }


    //Init sound systems for TX
    private static void txSoundInInit() {

        //Open and initialise the Output towards the Radio
        txBufferSize = 4 * android.media.AudioTrack.getMinBufferSize(8000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT); //Android check the multiplier value for the buffer size
        if (AndPskmail.toBluetooth) {
            txAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, txBufferSize, AudioTrack.MODE_STREAM);
        } else {
            txAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, txBufferSize, AudioTrack.MODE_STREAM);
        }

        //Start TX audio track
        txAudioTrack.setStereoVolume(1.0f, 1.0f);
        txAudioTrack.play();

        //Set requested volume AFTER we open the audio track as some devices (e.g. Oppo have two different volumes for when in or out of audio track
        AudioManager audioManager = (AudioManager) AndPskmail.myContext.getSystemService(Context.AUDIO_SERVICE);
        try {
            int maxVolume;
            int stream = AndPskmail.toBluetooth ? AndPskmail.STREAM_BLUETOOTH_SCO : AudioManager.STREAM_MUSIC;
            maxVolume = audioManager.getStreamMaxVolume(stream);
            int mediaVolume = config.getPreferenceI("MEDIAVOLUME", 100);
            if (mediaVolume < 5) mediaVolume = 5;
            if (mediaVolume > 100) mediaVolume = 100;
            maxVolume = maxVolume * mediaVolume / 100;
            audioManager.setStreamVolume(stream,
                    maxVolume, 0);  // 0 can also be changed to AudioManager.FLAG_PLAY_SOUND
        } catch (Exception e) {
            //AndPskmail.myContext.middleToastText("Error Adjusting Volume");
        }
    }


    //Release sound systems
    private static void txSoundRelease() {
        if (txAudioTrack != null) {

            //Wait for end of buffer to be emptied
            //try {
            //    Thread.sleep(1000 * txBufferSize / 8000);//wait buffer length time (milli sec).
            //} catch (InterruptedException e) {
            //Do nothing
            //}
            //Stop audio track
            txAudioTrack.stop();
            //debugging only
            //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'txAudioTrack.stop'");
            //Wait for end of audio play to avoid
            //overlaps between end of TX and start of RX
            while (txAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }
            }
            //debugging only
            //RMsgUtil.addEntryToLog(RMsgUtil.dateTimeStamp() + "Done 'waiting for end of playing state'");
            //Android debug add a fixed delay to avoid cutting off the tail end of the modulation
            //try {
            //    Thread.sleep(500);
            //} catch (InterruptedException e) {
            //e.printStackTrace();
            //}

            txAudioTrack.release();
        }
    }

    //In a separate thread so that the UI thread is not blocked during TX
    public void sendln(String Sendline) {
        Runnable TxRun = new TxThread(Sendline);
        Sendline = "";
        new Thread(TxRun).start();
    }

    public class TxThread implements Runnable {
        private String TxSendline = "";

        public TxThread(String Sendline) {
            TxSendline = Sendline;
        }

        public void run() {
            // see if tx active and DCD is off
            //JD Debug - FIX this: review DCD logic        if (Sendline.length() > 0 & !Processor.TXActive & Processor.DCD == 0) {
            if (TxSendline.length() > 0 & !Processor.TXActive) {
                //        	DCDthrow = generator.nextInt(Persistence);
                if (Processor.Connected | Processor.Connecting | Processor.Aborting){
                    if (Processor.Aborting) {
                        Processor.Aborting = false;
                    }
                    Processor.DCDthrow = 0;
                }
                if (Processor.DCDthrow == 0){
                    try {
                        Processor.TXActive = true;
                        AndPskmail.mHandler.post(AndPskmail.updatetitle);
                        //Stop the modem receiving side
                        pauseRxModem();
                        //TX delay here from config popup (allows on-the-fly changes)
                        int TxDelay = 0;
                        TxDelay = AndPskmail.myconfig.getTxDelay();

                        if (TxDelay > 0) {
                            Thread.sleep(TxDelay);
                        }

						/* not yet                    //  Add a 2 seconds delay when mode is MFSK16 (1 sec for MFSK32) to prevent overlaps as
                        //  the trail of MFSK is very long
                        if (Main.RxModem.equals(modemmodeenum.MFSK16)) {
                            Thread.sleep(2000);
                        } else if (Main.RxModem.equals(modemmodeenum.MFSK32)) {
                            Thread.sleep(1000);
                        }
						 */

                        //Reset receive marker of RSID for next RX
                        Processor.justReceivedRSID = false;

                        if (Processor.TxModem.toString().startsWith("PSK")) {
                            //Initialise PSK modems
                            TXMpsk = new TxPSK(Processor.TxModem);
                        } else if (Processor.TxModem.toString().startsWith("MFSK")) {
                            //Initialise PSK modems
                            TXMmfsk = new TxMFSK(Processor.TxModem);
                        } else if (Processor.TxModem.toString().startsWith("THOR")) {
                            //Initialise PSK modems
                            TXMthor = new TxTHOR(Processor.TxModem);
                        } else {
                            loggingclass.writelog("Wrong TX Mode called: " + Processor.TxModem.toString() , null, true);
                        }

                        //Init sound system
                        txSoundInInit();

                        //Send TX RSID if required
                        if (Processor.TXID) {
                            //Reset the send RSID flag
                            Processor.TXID = false;
                            TxRSID.send();
                        }
                        if (Processor.TxModem.toString().startsWith("PSK")) {
                            TXMpsk.AddBytes(TxSendline.getBytes());
                        } else if (Processor.TxModem.toString().startsWith("MFSK")) {
                            TXMmfsk.AddBytes(TxSendline.getBytes());
                        } else if (Processor.TxModem.toString().startsWith("THOR")) {
                            TXMthor.AddBytes(TxSendline.getBytes());
                        } else {
                            loggingclass.writelog("Wrong TX Mode called: " + Processor.TxModem.toString() , null, true);
                        }
                        //Reset TxRSID as it is OFF by default and needs to be enabled when required
                        Processor.q.send_txrsid_command("OFF");
                        //Release sound systems
                        txSoundRelease();
                        //Restart modem reception
                        unPauseRxModem();
                        Processor.TXActive = false;
                        AndPskmail.mHandler.post(AndPskmail.updatetitle);
                    }
                    catch (Exception e) {
                        loggingclass.writelog("Can't output sound. Is Sound device busy?", null, true);
                    }

                }

            }
        }
    };




    //Send Tune in a separate thread so that the UI thread is not blocked
    //  during TX
    public void TxTune() {
        Runnable txTuneThread = new TxTuneThread();
        new Thread(txTuneThread).start();
    }

    private class TxTuneThread implements Runnable {


        public TxTuneThread() {
        }

        public void run() {
            // Check that we are not TXing

            //Stop the modem receiving side
            pauseRxModem();

            //Wait 1/2 second so that if there is potential RF feedback
            //  on the touchscreen we do not start TXing while the
            //  finger is still on the screen
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            Processor.TXActive = true;
            String frequencySTR = AndPskmail.myconfig.getPreference("AFREQUENCY","1000");
            int frequency = Integer.parseInt(frequencySTR);

            String tuneLengthSTR = AndPskmail.myconfig.getPreference("TUNELENGTH","3");
            int tuneLength = Integer.parseInt(tuneLengthSTR);

            int volumebits = Integer.parseInt(AndPskmail.myconfig.getPreference("VOLUME","8"));

            /* New method
            //Note the multiplier value for the buffer size
            int intSize = 4 * android.media.AudioTrack.getMinBufferSize(8000,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

            if (AndPskmail.toBluetooth) {
                //JD Bluetooth hack test
                //		        	txAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
                txAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
            } else {
                txAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
            }

            //Open audiotrack
            txAudioTrack.setStereoVolume(1.0f,1.0f);
            txAudioTrack.play();
            */

            //Init sound system
            txSoundInInit();

            int sr = 8000; // should be active_modem->get_samplerate();
            int symlen = (int) (1 *  sr); //1 second buffer
            short[] outbuf = new short[symlen];

            double phaseincr;
            double phase = 0.0;
            phaseincr = 2.0 * Math.PI * frequency / sr;

            if (tuneLength == 0) {
                for (int i = 0; i < 60; i++) { //60 seconds max as safeguard
                    for (int j = 0; j < symlen; j++) {
                        phase += phaseincr;
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI;
                        outbuf[j] = (short) ((int) (Math.sin(phase) * 8386560) >> volumebits);
                    }
                    if (!Processor.tune) {
                        i = 60; //exit tune
                    } else {
                        txAudioTrack.write(outbuf, 0, symlen);
                    }
                }
            } else {
                for (int i = 0; i < tuneLength; i++) {
                    for (int j = 0; j < symlen; j++) {
                        phase += phaseincr;
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI;
                        outbuf[j] = (short) ((int) (Math.sin(phase) * 8386560) >> volumebits);
                    }
                    txAudioTrack.write(outbuf, 0, symlen);
                }
            }

            /* New method
            //Stop audio track
            txAudioTrack.stop();
            //Wait for end of audio play to avoid
            //overlaps between end of TX and start of RX
            while (txAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    //				e.printStackTrace();
                }
            }
            //Close audio track
            txAudioTrack.release();
            */

            //Release sound systems
            txSoundRelease();

            Processor.TXActive = false;

            //Restart the modem receiving side
            unPauseRxModem();

        }
    };

}
