/*
 * Processor.java
 *
 * Copyright (C) 2008 P�r Crusefalk and Rein Couperus
 * Adapted for Android by John Douyere (VK2ETA)
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

import java.util.Calendar;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

/**
 *
 * @author Per jPskmail Main.java 
 * 
 */
public class Processor extends Service{

	static String application ="AndPskmail 1.2.2"; // Used to preset an empty status
	static String version = "Version 1.2.2, 2019-09-24";
	public static int RxFrequencyOffset = 0;
	public static boolean showallcharacters = false; //debugging
	public static boolean justReceivedRSID = false;

	static boolean onWindows = true;
	static String ModemPreamble="";  // String to send before each frame
	static String ModemPostamble=""; // String to send after each frame
//	static modemmodeenum Mymode = modemmodeenum.PSK250;
	static modemmodeenum[] modeprofile ;
//	static modemmodeenum linkmode = modemmodeenum.PSK250;
	static int modemnumber = 0;
	static modemmodeenum defaultmode = modemmodeenum.PSK250;
	static String CurrentModemProfile = "0";
	static int sending_link = 5;
	static int sending_beacon = 0;
	static boolean CBeacon = true;
	static String HomePath = "";
	static String Dirprefix = "/.pskmail/";
	static String Separator = "/";
	static String  Mailoutfile = "";
	static File pending = null;
	static String pendingstr = "";
	static String Pendingdir = "";
	static String Outpendingdir = "";
	static String Transactions = "";
	static boolean compressedmail = false;
	static boolean Bulletinmode = false;
	static boolean IACmode = false;
	static boolean comp = false;
	static boolean debug = false;
	static String Sendline = "";
	static int DCD  = 0;
	static int MAXDCD = 3;
	static int RXBlocksize = 0;
	static int Totalbytes = 0;
	static boolean  TXActive = false;
	static boolean TXID = false;
	/*/   static String[] Modes = {"       ","THOR8","MFSK16","THOR22",
   "MFSK32",  "PSK250R", "PSK500R", "PSK500", "PSK250"};
static String[] AltModes = {"       ","THOR8","MFSK16","THOR22",
   "MFSK32",  "PSK125R", "PSK250R", "PSK250", "PSK250"};
	 */
	static String[] Modes = {"       ","THOR8","MFSK16","THOR22",
		"MFSK32",  "PSK250R", "PSK500R", "PSK500", "PSK250"};
	static String[] AltModes = {"       ","THOR8","MFSK16","THOR22",
		"MFSK32",  "PSK125R", "PSK250R", "PSK250", "PSK250"};
	static boolean UseAlttable = false;
	static String modelist = "789a"; //PSK500 to SPK63 only at this point. Was "7654321";
	static modemmodeenum TxModem = modemmodeenum.PSK250;
	static modemmodeenum RxModem = modemmodeenum.PSK250;
	static String RxModemString = "PSK250";

	//Semaphores to instruct the RxTx Thread to start or stop
	public static Semaphore restartRxModem = new Semaphore(1, false);
	public static Semaphore StopProcessor = new Semaphore(1, false);
	public static boolean tune = false;
	private NotificationManager myNotificationMgr;

	//Thread object for Rx thread
//delete:	private RxThread myRxThread = null;

	// globals to pass info to gui windows
	static String monitor = "";
	static String TXmonitor = "";
	static boolean monmutex = false;
	static String mainwindow = "";
	static String APRSwindow = "";
	static boolean mainmutex = false;
	static String MSGwindow = "";
	static String Mailheaderswindow = "";
	static String FilesTextArea = "";
	public static String Status = "Listening";
	static String Statusline = "";
	static int StatusLineTimer;
	static boolean txbusy = false;
	static boolean rxbusy = false;
	static boolean autolink = true;
	static int protocol = 1;
	static String protocolstr = "1";
	static int cpuload;

	// globals for communication
	static String Icon;
	static int APRSMessageNumber;
	static String APRS_Server = "netherlands.aprs2.net";
	static  String mycall;     // mycall from options
	static String myserver;    // myserver from options
	static String TTYCaller;     // TTY caller
	static String TTYConnected = "";
	static int DCDthrow;
	static String connectsecond;
	static long oldtime = 0;
	static long blockval = 0; //msec 
	static int charval = 0; //msecs
	static int blocktime; // seconds
	static int idlesecs = 0;
	static String LastBlockExchange = "  ";
	static long LastSessionExchangeTime = 0;
	static boolean Connected = false;
	static boolean Connecting = false;
	static boolean Aborting = false;
	static boolean Scanning = false;
	static boolean linked = false; // flag for link ack
	static String linkedserver = "";
	static String session = ""; // present session
	static boolean validblock = true;
	static String myrxstatus = "   "; // last rx status
	static String TX_Text; // output queue
	static int Progress = 0;
	static String DataSize = "";
	static String Servers[] = {"","","","","","","","","",""};
	static double AvgTxMissing = 0;
	static double AvgRxMissing = 0;
	static double hiss2n = 50;
	static double mys2n = 50;
	static double snr = 0.0;
	static double avgsnr = 50; //midrange
	static double SNR[] = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
	static String Lastheard[] = {"","","","","","","","","",""};
	static int packets_received[] = {0,0,0,0,0,0,0,0,0,0};
	static String modes_received[] = {"","","","","","","","","",""};
	static int strength[] = {0,0,0,0,0,0,0,0,0,0};
	static int snr_db = 0;
	static int TimeoutPolls = 0;
	static boolean JustDowngradedRX = false;
	static boolean status_received = false;
	static int NumberOfAcks = 5;
	static int Freq_offset = 1000;
	// Positions
	static String Positions [][] = new String [100][5];
	// gpsd data
	static boolean HaveGPSD = false;
	static boolean WantGpsd = false;
	static boolean NewGPSD = false;
	static boolean WantRigctl = false;
	static boolean wantScanner = false;
	static boolean Scanenabled = true;
	static String CurrentFreq = "0";
	static String ServerFreq = "0";
	static String freqstore = "0";
	static boolean summoning = false;
	static String GPSD_latitude =  "";
	static String GPSD_longitude =  "";
	static Socket gpsdSocket = null;
	static PrintWriter gpsdout = null;
	static BufferedReader gpsdin = null;
	static String gpsd_data[] = {"","","","","","","","","","","","","","","","","","","",""};
	static long t1 = System.currentTimeMillis();
	static boolean wantigate = false;

	//crypto
	static String strkey = "1234";
	static String Passwrd = "password";
	static String hispubkey = "";
	static crypt cr = null;
	static String serverversion = "1.1";
	static double sversion = 1.1;
	static Session sm = null;

	static String aprsbeacontxt = "";
	static boolean Serverbeacon = false;

	// arq object
	static arq q;
	// Config object
	//not used in Android   static config configuration; // Static config object
	// Error handling and logging object
	static loggingclass log;

	// Modem handle
	static public Modem m;

	// File handles
	static FileWriter bulletin = null;
	static FileReader hdr = null;
	// DCD
	static String DCDstr;
	// APRS server socket
	//JD Not now   static aprsmapsocket mapsock;
	//   static boolean aprsserverenabled = true;
	//   static Integer aprsserverport=8063;


	/**
	 * @param args the command line arguments
	 */
	//   public static void main(String[] args) throws InterruptedException {
	public static void processor() {

		//Nothing as this is a service
	}

	@Override
	public void onCreate() {

		// Create error handling class
		log = new loggingclass("AndPskmail");

		// Call the folder handling method
		handlefolderstructure();

		// Create config object
		// done in AndPskmail class        configuration = new config(HomePath + Dirprefix);


		// Make arq object
		q = new arq();

		// Make calendar object
		//           Calendar cal = Calendar.getInstance();

		// Get settings and initialize
		handleinitialization();

		/* not on Android            
        if (WantGpsd & !onWindows){
            handlegpsd();
        }
		 */
		// Handle GPS

		if (!HaveGPSD) 
			handlegps();
		/*
        if (want_igate) {
           try {
                igate.start();
           } catch (IOException ex) {
//                  Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
		 */
		// Make session object
		sm = new Session();  // session, class

		//New Modem
		m = new Modem();
		//Reset frequency and squelch
		m.reset();

		// init modemarray
		// Not used       Modemarray = m.pmodes;


		/* not a separate thread here since we launch a worker's thread for modem and session processing           
        Thread myThread = new Thread(m);
        // Start the modem thread
        myThread.setDaemon(true);
        myThread.start();
		 */

		q.Message(version, 10);


		/* integrated GPS only for now 
         // Start the aprs server socket
        mapsock = new aprsmapsocket();
        mapsock.setPort(aprsserverport);
        mapsock.setPortopen(aprsserverenabled);
        mapsock.start();
		 */

		// init random generator for DCD
		Random generator = new Random();

		//Always have RXid ON so that TTY connects and igates beacons can be heard on any mode
		q.send_rsid_command("ON");

		//Make sure the display strings are blank
		Processor.monitor = "";
		Processor.TXmonitor = "";
		Processor.mainwindow = "";
		Processor.APRSwindow = "";

	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Start the RxThread
		if (m != null) {
			m.startmodem();

			//Make sure Android keeps this running even if resources are limited
			//Display the notification in the system bar at the top at the same time
			startForeground(1, AndPskmail.myNotification);


		}
		// Keep this service running until it is explicitly stopped, so use sticky.
		return START_STICKY;
	}
	
	
	@Override
	public void onDestroy() {
		// Kill the Rx Modem thread
		if (m != null) {
			m.stopRxModem();
		}
	}  



	//Post to main terminal window
	public static void PostToTerminal(String text) {
		Processor.mainwindow += text;
		AndPskmail.mHandler.post(AndPskmail.addtoterminal);
	}


	//Post to main terminal window
	public static void PostToModem(String text) {
		Processor.monitor += text;
		AndPskmail.mHandler.post(AndPskmail.addtomodem);
	}


	//Post to main terminal window
	public static void PostToAPRS(String text) {
		Processor.APRSwindow += text;
		AndPskmail.mHandler.post(AndPskmail.addtoAPRS);
	}




	//Process one block of received data. Called from below when a good 
	//  block is received
	public static void ProcessBlock(String Blockline) {

		//JD Therefore no sending in this method        	  

		try {
			//                    if (m.checkBlock()) {

			//                        Blockline = m.getMessage();

			RXBlock rxb = new RXBlock(Blockline);
			if (!rxb.valid) { 
				validblock =false;
			} else {
				validblock = true;
			}

			if (!Bulletinmode & !IACmode ) {

				if (Connected) {

					// status block from server
					if (rxb.type.equals("s") &
							rxb.valid  & rxb.session.equals(session)) {
						idlesecs = 0;      // reset idle timer
						Processor.TimeoutPolls = 0; // Reset timeout polls count
						// set blocktime for idle time measurement...
						if (Blockline.length() > 8){
							charval = (int)(blockval / (Blockline.length() - 4)); // msec
							blocktime = (charval * 64/ 1000);
						}
						//Move processing of block before decision on mode upgrade
						sm.RXStatus(rxb.payload);   // parse incoming status packet
						// get the tx status
						myrxstatus = sm.getTXStatus();
						if (!LastBlockExchange.equals(sm.getBlockExchanges())) {
							LastSessionExchangeTime = System.currentTimeMillis() / 1000;
							LastBlockExchange = sm.getBlockExchanges();
						}
						// set the modem type for TX if client. For TTY server, adjust TX mode based on received s2n from TTY client.
						//Common data needed for later
						String pbyte = rxb.protocol;
						char pchr = pbyte.charAt(0);
						int pint = (int) pchr;
						//Turn RXid ON as I am a client
						//Not needed with integrated modem as always on:  q.send_rsid_command("ON");
						pint = (int) pchr - 48;
						if (pint <= (modelist.length()) & pint > 0) {
							//The table is read from right to left
							//from the server starting at 1
							TxModem = Modem.getmode(modelist.charAt(modelist.length() - pint));
							//zero = symmetric mode
							if (CurrentModemProfile.equals("0")) {
								TxModem = RxModem;
							}
						} else if (pint == 0) {
							TxModem = RxModem;
						}

						if (Session.tx_missing.length() > 0 | Processor.TX_Text.length() > 0) {
							String outstr = sm.doTXbuffer();
							q.send_data(outstr);
						} else {
							myrxstatus = sm.getTXStatus();
							q.send_status(myrxstatus);  // send our status
						}
						Processor.validblock = true;

					} else if (Connected & (rxb.type.equals("p")) &
							rxb.valid  & rxb.session.equals(session)) {
						sm.RXStatus(rxb.payload);   // parse incoming status packet

						myrxstatus = sm.getTXStatus();
						q.send_status(myrxstatus);  // send our status
						Processor.txbusy = true;
						// disconnect request
					} else if (Connected & rxb.session.equals(session) & rxb.type.equals("d")) {
						Status = "Listening";
						AndPskmail.mHandler.post(AndPskmail.updatetitle);
						Connected = false;
						session = "";
						Totalbytes = 0;
						sm.FileDownload = false;
						try {
							if (sm.pFile != null) {
								sm.pFile.close();
							}
						}
						catch (IOException e) {
							Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, e);
						}
						q.send_rsid_command("OFF");
						// ident block
					} else if (rxb.session.equals(session) & rxb.type.equals("i")) {
						// discard
						// data block
					} else if (rxb.valid & rxb.session.equals(session) ) {
						myrxstatus = sm.doRXBuffer(rxb.payload, rxb.type);
					} else if (rxb.session.equals(session) ) {

						myrxstatus = sm.doRXBuffer("", rxb.type);

					}


					/* OLD    				// PI4TUE 0.9.33-13:28:52-IM46>
    				if (Blockline.contains(q.servercall)) {
    					Pattern ppc = Pattern.compile("\\S+\\s.*-\\d+:\\d+:(\\d+)");
    					Matcher mpc = ppc.matcher(Blockline);
    					connectsecond = "";
    					if (mpc.lookingAt()) {

    						connectsecond = mpc.group(1);
    					}
    				}
					 */
					// PI4TUE 0.9.33-13:28:52-IM46>
					if (Blockline.contains(q.servercall)) {
						//                                   Pattern ppc = Pattern.compile(".*(\\d\\.\\d).*\\-\\d+:\\d+:(\\d+)\\-(.*)M(\\d+)");
						Pattern ppc = Pattern.compile(".*\\S+\\s\\S+\\s(\\S{3}).*\\-\\d+:\\d+:(\\d+)\\-(.*)M(\\d+)");
						//System.out.println(Blockline);
						Matcher mpc = ppc.matcher(Blockline);

						connectsecond = "";
						String localmail = "";
						if (mpc.lookingAt()) {
							serverversion = mpc.group(1);
							sm.serverversion = mpc.group(1);
							sversion = Double.parseDouble(serverversion);
							//System.out.println(sversion);
							connectsecond = mpc.group(2);
							localmail = mpc.group(3);
							if (localmail.contains("L")) {
								//                                mainui.enableMboxMenu();
							}
							if (sversion > 1.1){
								//System.out.println("success");
								sm.hispubkey = mpc.group(4);
								hispubkey = sm.hispubkey;

								cr = new crypt();

								String output = cr.encrypt (sm.hispubkey, Passwrd);

								Processor.TX_Text += "~Mp" + output + "\n";
							}
						}
					}

				}

				if (!Connected & Blockline.contains("QSL") & Blockline.contains(q.callsign)) {  
					//Display in APRS Window, even if not valid block
					Processor.APRSwindow += "\n" + Blockline + "\n";
					AndPskmail.mHandler.post(AndPskmail.addtoAPRS);

					String pCheck = "";
					Pattern psc = Pattern.compile(".*de ([A-Z0-9\\-]+)\\s([0123456789ABCDEF]{4}).*");
					Matcher msc = psc.matcher(Blockline);
					String scall = "";
					if (msc.lookingAt()) {
						scall = msc.group(1);
						pCheck = msc.group(2);
						q.send_txrsid_command("OFF");
						Thread.sleep(500);
						//                                        q.send_mode_command(defaultmode);
					} 
					// fill the servers drop down list
					char soh = 1;
					String sohstr = Character.toString(soh);
					String checkstring = sohstr + "QSL " + q.callsign +  " de " + scall ;
					String check = q.checksum(checkstring);
					if (check.equals(pCheck)){
						rxb.get_serverstat(scall);
						int i = 0;
						boolean knownserver = false;
						for (i = 0; i <10; i++) {
							//   System.out.println(Servers[i] + scall);
							if (scall.equals(Servers[i])) {
								knownserver = true;
								break;
							}
						}
						/* VK2ETA Not yet                                        if (!knownserver) {
                           mainui.addServer(scall); // add to servers drop down list
                                        }
						 */                                
						// switch off txrsid
						q.send_txrsid_command("OFF");
					}
				}

				if (!Connected & Blockline.contains(":71 ")) {
					Pattern psc = Pattern.compile(".*00u(\\S+):71\\s([0123456789ABCDEF]{4}).*");
					Matcher msc = psc.matcher(Blockline);
					String scall = "";
					String pCheck = "";
					if (msc.lookingAt()) {  
						scall = msc.group(1);
						pCheck = msc.group(2);                                 
					} 
					// fill the servers drop down list
					String checkstring = "00u"  + scall + ":71 " ;                            
					String check = q.checksum(checkstring);
					if (check.equals(pCheck)){
						rxb.get_serverstat(scall);

						// switch off txrsid
						q.send_txrsid_command("OFF");


					} else {
						//                                        System.out.println("check not ok.\n");
					}
				}
				// beacons
				if (!Connected & Blockline.contains(":26 ")) {
					Pattern bsc = Pattern.compile(".*00u(\\S+):26\\s(.)(.*)(.)([0123456789ABCDEF]{4}).*");
					Matcher bmsc = bsc.matcher(Blockline);
					String scall = "";
					String binfo = "";
					if (bmsc.lookingAt()) {
						scall = bmsc.group(1);
						String type = bmsc.group(2);
						binfo = bmsc.group(3);
						String nodetype = bmsc.group(4);
						String pCheck = bmsc.group(5);
						binfo += nodetype;
						String checkstring = "00u"  + scall + ":26 " + type + binfo;
						String check = q.checksum(checkstring);
						String outstring = "";
						if (check.equals(pCheck)){

							if (type.equals("!")) {
								outstring = scall + ">PSKAPR,TCPIP*:" + type + binfo;
								//                                                System.out.println(outstring);
								//JD add later                                                igate.write(outstring);
								// Push this to aprs map too
								//JD Add later                                                mapsock.sendmessage(outstring);
								if (nodetype.equals("&")) {
									// is serverbeacon
									Serverbeacon = true;
									int i;
									boolean knownserver = false;
									for (i = 0; i < 10; i++) {
										if (scall.equals(Servers[i])) {
											knownserver = true;
											break;
										}
									}

									if (!knownserver) {
										for (i = 0; i < 10; i++) {
											if (Servers[i].equals("")) {
												Servers[i] = scall;
												//JD add later                                                                  mainui.addServer(scall);
												break;
											}
										}
									}

								}
							} else if (!type.equals(":")) {
								// message PA0R-2:26 PA0R test

								Pattern gm = Pattern.compile(".*00u(\\S+):26\\s(\\S+)>PSKAPR...(\\S+)\\s*:(.*)\\{(\\d\\d).*");
								Matcher gmm = gm.matcher(Blockline);
								if (gmm.lookingAt()) {
									String fromcall = gmm.group(3) + "        ";
									fromcall = fromcall.substring(0,8);

									String toxastir = gmm.group(2) + ">PSKAPR,TCPIP*,qAC," + gmm.group(1) + "::" + fromcall + "  " + ":" + gmm.group(4) +  "\n";
									//JD add later                                                    mapsock.sendmessage(toxastir);

								}


							} else {

								Pattern gc =Pattern.compile("(\\S+)>PSKAPR.::(\\S+)\\s*:(.*)(\\{\\d+)");
								Matcher gmc = gc.matcher(type + binfo);
								if (gmc.lookingAt()){

									String outcall = gmc.group(2);
									binfo = gmc.group(3);
									String mnumber = gmc.group(4);
									outstring = scall + ">PSKAPR,TCPIP*::" + outcall;
									String padder = "        ";
									outstring += padder.substring(0, 8 - outcall.length());
									outstring += ":";
									outstring += binfo;
									outstring += mnumber;
									//JD add later                                                    igate.write(outstring);
									// Push this to aprs map too
									//JD add later                                                    mapsock.sendmessage(outstring);
								}
							}
							outstring = "";
						}



					}
				}
/*VK2ETA ADD LATER				
				// compressed beacons
				if (!Connected & Blockline.contains(":6 ")) {
					Pattern cbsc = Pattern.compile(".*00u(\\S+):6\\s(.*)([0123456789ABCDEF]{4}).*");
					Matcher cbmsc = cbsc.matcher(Blockline);
					String scall = "";
					String binfo = "";
					if (cbmsc.lookingAt()) {
						scall = cbmsc.group(1);
						binfo = cbmsc.group(2);
						String pCheck = cbmsc.group(3);
						String checkstring = "00u"  + scall + ":6 " + binfo;
						String check = q.checksum(checkstring);

						if (check.equals(pCheck)){
							byte[] cmps = binfo.substring(0,11).getBytes();
							int flg = cmps[0] - 32;
							int latdegrees = cmps[1] - 32;
							String s_latdegrees = String.format ("%02d", latdegrees);
							int latminutes = cmps[2] - 32;
							String s_latminutes = String.format ("%02d", latminutes);
							int latrest = cmps[3] - 32;
							String s_latrest = String.format ("%02d", latrest);
							int londegrees = cmps[4] - 32;
							String s_londegrees = String.format ("%03d", londegrees);
							int lonminutes = cmps[5] - 32;
							String s_lonminutes = String.format ("%02d", lonminutes);
							int lonrest = cmps[6] - 32;
							String s_lonrest = String.format ("%02d", lonrest);
							int course = cmps[7] - 32;
							String s_course = String.format ("%03d", course);
							int speed = cmps[8] - 32;
							String s_speed = String.format ("%03d", speed);
							char c = (char) cmps[9];
							String symbol = Character.toString(c);
							//int statusinx = cmps[10] - 32;
							String statusmessage = binfo.substring(11);
							//JD                                           if (statusinx <= igate.maxstatus) {
							//   statusmessage = igate.status[statusinx]  + statusmessage;
							//                                            }
							String latstr = "S";
							String lonstr = "W";

							int x = flg & 32;
							if (x == 32) {
								course += 180;
							}
							x = flg & 16;
							if (x == 16) {
								speed += 90;
							}
							x = flg & 8;
							if (x == 8) {
								latstr = "N";
							}
							x = flg & 4;
							if (x == 4) {
								lonstr = "E";
							}
							x = flg & 2;
							if (x == 2) {
								londegrees += 90;
								s_londegrees = String.format ("%03d", londegrees);
							}
							String linfo = "!";
							linfo += s_latdegrees;
							linfo += s_latminutes;
							linfo += ".";
							linfo += s_latrest;
							linfo += latstr;
							linfo += "/";
							linfo += s_londegrees;
							linfo += s_lonminutes;
							linfo += ".";
							linfo += s_lonrest;
							linfo += lonstr;
							linfo += symbol;
							linfo += s_course;
							linfo += "/";
							linfo += s_speed;
							linfo += "/";
							linfo += statusmessage;
							//VK2ETA add later   
							//String outstring = scall + ">PSKAPR,TCPIP*:" + linfo;

							// System.out.println(outstring);
							// igate.write(outstring);
							// Push this to aprs map too
							//outstring = "";
						}
					}
				}
*/
				//Not used	String s = Blockline + "\n";

				//                           System.out.println(Blockline);

				// unproto packet
				if (rxb.type.equals("u")) {
					if (rxb.port.equals("26") & !Serverbeacon) {
						if (rxb.call.equals(AndPskmail.myconfig.getPreference("CALL")) || rxb.call.equals(AndPskmail.myconfig.getPreference("PSKAPRS"))) {
							q.send_txrsid_command("OFF");
							Thread.sleep(500);                          

							if (rxb.msgtext.indexOf("ack") != 0 & rxb.msgtext.indexOf(":") != 0 ) {
								MSGwindow += rxb.from + ": " + rxb.msgtext + "\n";
								if (!Connected) {
									PostToTerminal(rxb.from + ": " + rxb.msgtext + "\n");
								} else {
									q.Message("You received a message", 10);
								}
							}
						}                                    
					} else if (rxb.port.equals("71") | rxb.port.equals("72")) {
						int i; 
						boolean knownserver = false;
						Calendar cal = Calendar.getInstance();
						int Hour = cal.get(Calendar.HOUR_OF_DAY);
						int Minute = cal.get(Calendar.MINUTE);                                                                                                                                                                                                                                                                                                                                                                                                                   String formathour = "0" + Integer.toString(Hour);
						formathour = formathour.substring(formathour.length() - 2);
						String formatminute = "0" + Integer.toString(Minute);
						formatminute = formatminute.substring(formatminute.length() - 2);
						String lh = formathour  + ":" + formatminute;
						for (i = 0; i <10; i++) {

							if (rxb.server.equals(Servers[i])) {
								knownserver = true;
								SNR[i] = snr;
								Lastheard[i] =  lh;
								packets_received[i]++;
								modes_received[i] = RxModemString;
								strength[i] = snr_db;
								break;
							}
						}
						if (!knownserver) {
							for (i = 0; i <10; i++) {
								if (Servers[i].equals("")) {
									Pattern sw = Pattern.compile("[A-Z0-9]+\\-*\\[0-9]*");
									Matcher ssw = sw.matcher(rxb.server);
									if (ssw.lookingAt() & rxb.server.length() > 3) {
										Servers[i] = rxb.server;
										SNR[i] = snr;
										Lastheard[i] =  lh;
										packets_received[i]++;
										strength[i] = snr_db;
										//JD add later      mainui.addServer(rxb.server);
										break;
									}
								}
							}

						}

					}
					// reject
				} else if (rxb.type.equals("r") & rxb.valid) {  // reject
					String rejectcall = "";
					String rejectreason = "";
					Pattern pr = Pattern.compile("^(\\S+):(.*)");
					Matcher mr = pr.matcher(rxb.payload);
					if (mr.lookingAt()) {
						rejectcall = mr.group(1);
						rejectreason = mr.group(2);
					}

					if (rejectcall.equals(mycall)){
						Status = "Listening";
						AndPskmail.mHandler.post(AndPskmail.updatetitle);
						Connected = false;
						Bulletinmode = false;
						Connecting = false;
						Scanning = false;
						session = "";
						Totalbytes = 0;
						q.send_rsid_command("OFF");
						q.Message("Rejected:" + rejectreason, 10);
					}
					// connect_ack
				} else if (rxb.type.equals("k") & rxb.valid) {  // connect ack

					Pattern pk = Pattern.compile("^(\\S+):\\d+\\s(\\S+):\\d+\\s(\\d)$");
					Matcher mk = pk.matcher(rxb.payload);
					if (mk.lookingAt()) {
						rxb.server = mk.group(1);
						rxb.call = mk.group(2);
						rxb.serverBlocklength = mk.group(3);
					}
					// are we  connected?
					// if (rxb.call.equals(rxb.mycall) & rxb.server.equals(AndPskmail.myconfig.getPreference("SERVER"))){
					if (rxb.call.equals(rxb.mycall) & rxb.server.equals(AndPskmail.serverToCall)){
						//txid OFF, rxid ON
						q.send_txrsid_command("OFF");
						Thread.sleep(500);
						Status = "Connected";
						AndPskmail.mHandler.post(AndPskmail.updatetitle);
						Connected = true;
						Connecting = false;
						Scanning = false;
						summoning = false;
						Processor.linked = true;
						Processor.linkedserver = rxb.server;
						// reset tx queue 
						TX_Text = "";
						Totalbytes = 0;
						sm.initSession();
						session = rxb.session;
						sm.session_id = rxb.session;
						sm.myserver = rxb.server;
						protocolstr = rxb.protocol;
						protocol = protocolstr.charAt(0) - 48;


						/*						Replaced with a call to pending transaction display
     					File outb1 = new File (Processor.HomePath + Processor.Dirprefix + "Outbox");
    					int i1 = outb1.list().length;
    					if (i1 > 0) {
    						Processor.PostToTerminal("\nWaiting in outbox:" + Integer.toString(i1) + "\n");
    					}

    					File outb = new File (Processor.Pendingdir);
    					int i = outb.list().length;
    					if (i > 0){
    						Processor.PostToTerminal("Incomplete Downloads:" + Integer.toString(i) + "\n\n");
    					}
						 */

						AndPskmail.ListPendingTransactions();
					}

				} else if (Processor.Bulletinmode){
					// Bulletin mode
					Blockline = Blockline.substring(5);
					if (Blockline.length()> 9){
						Blockline = Blockline.substring(0, Blockline.length() - 9);
					}
					Pattern pb = Pattern.compile("NNNN");
					Matcher mb= pb.matcher(Blockline);
					if (mb.find()) {
						Blockline = "\n----------\n";
						bulletin.write(Blockline);
						Processor.Bulletinmode = false;
						Processor.Status = "Listening";
					}
					// System.out.println("MAIN:" + Blockline);
					Processor.PostToTerminal(Blockline);

					// write to bulletins file...
					bulletin.write(Blockline);
					bulletin.flush();

				} else if (Processor.IACmode) {
					sm.parseInput(Blockline);                              
				} 


				if (debug) {
					loggingclass.writelog("Rxb.server " + rxb.server , null, true);
					System.out.println(rxb.server);
					loggingclass.writelog("Rxb.test " + rxb.test , null, true);
					System.out.println(rxb.test);
					loggingclass.writelog("Rxb.protocol " + rxb.protocol , null, true);
					System.out.println(rxb.protocol);
					loggingclass.writelog("Rxb.session " + rxb.session , null, true);
					System.out.println(rxb.session);
					loggingclass.writelog("Rxb.type " + rxb.type , null, true);
					System.out.println(rxb.type);
					loggingclass.writelog("Rxb.crc " + rxb.crc , null, true);
					System.out.println(rxb.crc);
					loggingclass.writelog("Rxb.port " + rxb.port , null, true);
					System.out.println(rxb.port);

					if (rxb.valid == true) {
						loggingclass.writelog("Rxb.valid = true" , null, true);
						System.out.println("valid");
					}
				}
				oldtime = System.currentTimeMillis() / 1000;

			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
			Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);

		} finally {
			try {
				if (!(bulletin == null)){
					bulletin.close();
				}
			} catch (IOException ex) {
				loggingclass.writelog("IO Exception when closing bulletins!", ex, true);
			}

		}
	} // end Main

	/**
	 * Add a server to the array of known servers, for instance as written by the user
	 * @param MyServer
	 */
	public static void AddServerToArray(String myServer){           
		try {
			int i;
			boolean knownserver = false;
			for (i = 0; i < 10; i++) {
				if (myServer.equals(Servers[i])) {
					knownserver = true;
					break;
				}
			}

			if (!knownserver) {
				for (i = 0; i < 10; i++) {
					if (Servers[i].equals("")) {
						Servers[i] = myServer;
						//JD later on                          mainui.addServer(myServer);
						break;
					}
				}
			}
		} 
		catch (Exception e) {
			loggingclass.writelog("Had problem adding server to array, full?", e, true);
		}
	}

	/**
	 * Create or check the necessary folder structure (.pskmail)
	 */
	private static void handlefolderstructure(){

		// are we on Linux?
		try {
			//VK2ETA Changed to SD card device           HomePath = System.getProperty("user.home");
			//VK2ETA FIX: add exception here when there is no external storage
			HomePath = Environment.getExternalStorageDirectory().getAbsolutePath();
			if (File.separator.equals("/")) {
				Dirprefix = "/pskmail/";
				onWindows = false;
			} else  {
				Dirprefix = "\\pskmail\\";
				onWindows = true;
			}


			//Check if pskmail directory exists, create if not
			File dir = new File(HomePath + Dirprefix);
			if (!dir.isDirectory()) {
				dir.mkdir();
			}
			//Check if Outbox directory exists, create if not
			if (File.separator.equals("/")) {
				Separator = "/";
			} else {
				Separator = "\\";
			}
			File outbox = new File(HomePath + Dirprefix + "Outbox" + Separator);
			if (!outbox.isDirectory()) {
				outbox.mkdir();
			}
			File pendingfl = new File(HomePath + Dirprefix + "Pending" + Separator);

			if (!pendingfl.isDirectory()) {
				pendingfl.mkdir();
			}
			File outpendingfl = new File(HomePath + Dirprefix + "Outpending" + Separator);

			if (!outpendingfl.isDirectory()) {
				outpendingfl.mkdir();
			}

			//Check if Downloads directory exists, create if not
			if (File.separator.equals("/")) {
				Separator = "/";
			} else {
				Separator = "\\";
			}
			File downloads = new File(HomePath + Dirprefix + "Downloads" + Separator);
			if (!downloads.isDirectory()) {
				downloads.mkdir();
			}


			//Check if Pending directory exists, create if not
			if (File.separator.equals("/")) {
				Separator = "/";
			} else {
				Separator = "\\";
			}
			pendingstr =   HomePath + Dirprefix + "Pending" + Separator;
			pending = new File(pendingstr);
			if (!pending.isDirectory()) {
				pending.mkdir();
			}
			Pendingdir = HomePath + Dirprefix + "Pending" + Separator;
			Outpendingdir = HomePath + Dirprefix + "Outpending" + Separator;
			Transactions = HomePath + Dirprefix + "Transactions";

			// Check if bulletin file  exists, create if not
			File fFile = new File(Processor.HomePath + Processor.Dirprefix + "Downloads" + Separator + "bulletins");
			if (!fFile.exists()) {
				fFile.createNewFile();
			}

			bulletin = new FileWriter(fFile, true);

			// check if headers file exists, and read in contents 
			File fh = new File(HomePath + Dirprefix + "headers");
			if (!fh.exists()) {
				fh.createNewFile();
			}

			hdr = new FileReader(fh);
			BufferedReader br = new BufferedReader(hdr);
			String s;
			while ((s = br.readLine()) != null) {
				String fl = s + "\n";
				Mailheaderswindow += fl;
			}
			br.close();               
		}
		catch(Exception ex){
			loggingclass.writelog("Problem when handling pskmail folder structure.", ex, true);
		}
	}

	private static void handleinitialization(){

		try {
			Passwrd = AndPskmail.myconfig.getPreference("PASSWORD","");
			// try to initialize MAXDCD from Prefs
			DCDstr = AndPskmail.myconfig.getPreference("DCD","0");
			MAXDCD = Integer.parseInt(DCDstr);
			// try to initialize Icon from Prefs
			Icon = AndPskmail.myconfig.getPreference("ICON","y");
			// Initialize APRSMessageNumber
			APRSMessageNumber = 0;
			// Initialize send queue
			TX_Text = "";
			// Modem settings
			//                host = configuration.getPreference("MODEMIP", "localhost");
			//                port = Integer.parseInt(configuration.getPreference("MODEMIPPORT", "7322"));
			ModemPreamble = AndPskmail.myconfig.getPreference("MODEMPREAMBLE");
			ModemPostamble = AndPskmail.myconfig.getPreference("MODEMPOSTAMBLE");
			// Mail settings
			compressedmail = AndPskmail.myconfig.getPreferenceB("COMPRESSED");
			String profile = AndPskmail.myconfig.getPreference("BLOCKLENGTH");
			CurrentModemProfile = profile;
			Character c =  profile.charAt(0);
			int profilenr = c.charValue() - 48;

			if (profilenr > 7 & profilenr > 1) {
				Processor.defaultmode = modemmodeenum.PSK250;
			} else {
				Processor.defaultmode = modemmodeenum.PSK250;
			}
			modeprofile  = new modemmodeenum[10];

			/*
// integrated GPS only at present
               if (AndPskmail.myconfig.getPreference("GPSD").equals("1")){
                   WantGpsd = true;
               }
               if (AndPskmail.myconfig.getPreference("SCANNER").equals("yes")) {
                    wantScanner = true;
               }
               // APRSServerSettings
               //
					aprsserverport=Integer.parseInt(AndPskmail.myconfig.getPreference("APRSSERVERPORT"));
               if (AndPskmail.myconfig.getPreference("APRSSERVER").equals("yes")) {
                   aprsserverenabled = true;
               } else {
                   aprsserverenabled = false;
               }
			 */

		} catch (Exception e) {
			MAXDCD = 3;
			q.backoff = "5";
			Icon = "y";
			loggingclass.writelog("Problems with config parameter.", e, true);
		}
		// Send Link request
		//           q.set_txstatus(txstatus.TXlinkreq);
		//           q.send_link();

//		Servers[0] = AndPskmail.myconfig.getPreference("SERVER");
		Servers[0] = AndPskmail.serverToCall;
		Processor.myserver = Servers[0];
		Processor.mycall = AndPskmail.myconfig.getPreference("CALL");
		//            Freq_offset = Integer.parseInt(Main.AndPskmail.myconfig.getPreference("RIGOFFSET"));

		/* No need here
            String XMLIP = AndPskmail.myconfig.getPreference("MODEMIP");

            if (XMLIP.equals("localhost")) {
                XMLIP = "127.0.0.1"; 
            }

             XmlRpc_URL = "http://" + XMLIP + ":7362/RPC2";
		 */

	}

	/* review all of this for integrated GPS in Android

   private static void handlegpsd() {
        try {
            // Connect to gpsd at port 2947 on localhost
            InetAddress addr = InetAddress.getByName("localhost");
            int target = 2947;
            SocketAddress sockaddr = new InetSocketAddress(addr, target);

            // Block no more than timeoutMs.
            // If the timeout occurs, SocketTimeoutException is thrown.

            int timeoutMs = 2000;   // 2 seconds

            gpsdSocket = new Socket();
            gpsdSocket.connect(sockaddr, timeoutMs);
            gpsdout = new PrintWriter(gpsdSocket.getOutputStream(), true);
            gpsdin = new BufferedReader(new InputStreamReader(
            gpsdSocket.getInputStream()));

            String outgps = "?WATCH={\"enable\":true, \"nmea\":true };";
            gpsdout.println(outgps);

            long t0 = System.currentTimeMillis();
            t1 = t0;

            boolean ready = false;

            while (t1 - t0 < 2000 & !ready){

                t1 = System.currentTimeMillis();

                String myRead = "";

                if (gpsdin.ready()) {
                    myRead = gpsdin.readLine();

                    if (myRead.substring(0,6).equals("$GPRMC")) {
                       HaveGPSD = true;
                       ready = true;
                    }
                }

            }

            if (!HaveGPSD) {
                q.Message ("Problem with GPSD", 10);
            }

        } catch (UnknownHostException e) {
                        q.Message ("Cannot find GPSD", 10);
                        HaveGPSD = false;
        } catch (IOException e) {
                        q.Message ("Cannot find gpsd", 10);
                        HaveGPSD = false;
        }

        if (HaveGPSD) {
            gpsdata = new nmeaparser();     // Parser for nmea data
            q.Message("Connected to GPSD", 10);
        }

   }
	 */

	/* not used. Review if required
   static public void getgpsddata() {
             String myRead = "";
             Boolean ready = false;

             while (HaveGPSD & !ready) {

                   try {
                        myRead = gpsdin.readLine();
                   } catch (IOException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                   }

                   if (myRead.length() > 6 ){
                        if (myRead.substring(0,6).equals("$GPRMC")) {
                            gpsd_data = myRead.split(",");
                            if (gpsd_data[1].length() > 2) {
                                gpsdata.validfix = true;
                                gpsdata.fixat = gpsd_data[1];
                            }

                            gpsdata.latitude = gpsd_data[3];
                            float latdata = Float.valueOf(gpsdata.latitude)/100;
                            int degr = (int)latdata;
                            float mindata = (latdata - degr)/60*100;
                            mindata = degr + mindata;
                            if (gpsd_data[4].equals("S")) {
                                mindata *= -1;
                            }
                            GPSD_latitude = Float.toString(mindata);

                            gpsdata.longitude = gpsd_data[5];
                            float longdata = Float.valueOf(gpsdata.longitude)/100;
                            degr = (int)longdata;
                            mindata = (longdata - degr)/60*100;
                            mindata = degr + mindata;
                            if (gpsd_data[6].equals("S")) {
                                mindata *= -1;
                            }
                            GPSD_longitude = Float.toString(mindata);

                            gpsdata.speed = gpsd_data[7];
                            gpsdata.course = gpsd_data[8];

                            char[] buffer = new char [ 4000 ];
                        try {
                            int cnt = gpsdin.read(buffer);
                         } catch (IOException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                         }
                      }

                            AndPskmail.myconfig.setLatitude(GPSD_latitude);
                            AndPskmail.myconfig.setLongitude(GPSD_longitude);

                            ready = true;
                  }




                 }

   }

static  public void parsenmeadata(String nmeadata){
        gpsdata.newdata(nmeadata);
            if (gpsdata.getFixStatus()){
                AndPskmail.myconfig.setLatitude(gpsdata.getLatitude());
                AndPskmail.myconfig.setLongitude(gpsdata.getLongitude());
                AndPskmail.myconfig.setSpeed(gpsdata.getSpeed());
                AndPskmail.myconfig.setCourse(gpsdata.getCourse());
            }
}
	 */
	/**
	 * Open a GPS connection, if that should be used
	 */
	private static void handlegps(){
		/* review this for Android
       // GPS
       gpsport = new serialport();       // Serial port object
       gpsdata = new nmeaparser();     // Parser for nmea data
       String portforgps = AndPskmail.myconfig.getPreference("GPSPORT");

       // Make sure the selected port still exists!
       if (AndPskmail.myconfig.getPreference("GPSENABLED").equals("yes")){
            if (!gpsport.checkComPort(portforgps)){
                Main.log.writelog("Serial port "+portforgps+" does not exist! Was the GPS removed? Disabling GPS.", true);
                AndPskmail.myconfig.setPreference("GPSENABLED", "no");
            }
       }

       if (AndPskmail.myconfig.getPreference("GPSENABLED").equals("yes")){
       try
       {
            String speedforgps = AndPskmail.myconfig.getPreference("GPSSPEED");
            int speedygps = Integer.parseInt(speedforgps);
            gpsport.connect(portforgps,speedygps);
            // Check if the port is open
            if (!gpsport.curstate){
                // Disconnect and set it off
                gpsport.disconnect();
                AndPskmail.myconfig.setPreference("GPSENABLED", "no");
            }
            // if (portforgps.contains("USB"))
            // Here is the code for getting a gps out of sirf mode
			// gpsdata.writehexsirfmsg("8102010100010101050101010001000100010001000112c0"); //Set 4800 bps nmea
       }
       catch(Exception ex)
       {
            log.writelog("Error when trying to connect to the GPS.", ex, true);
       }
      }   
		 */
	}



	static String getTXModemString (modemmodeenum mode) {
		try {
			String Txmodemstring = "";
			Txmodemstring = Modem.getModemString(mode);
			return Txmodemstring;
		} catch (Exception e) {
			return "";
		}
	}
	static String getAltTXModemString (modemmodeenum mode) {
		String Txmodemstring = "";
		Txmodemstring = Modem.getAltModemString(mode);
		return Txmodemstring;
	}

	/* not used
   static void setFreq (String freq) {
       if (Rigctl.opened) {
           int fr = Integer.parseInt(Main.CurrentFreq) + Rigctl.OFF;
           freqstore = Integer.toString(fr);
           Rigctl.Setfreq(freq);
           summoning = true;
       }
   }
	 */

	static void savePreferences()  {
		try {
			// store the config file if present

			File f1 = new File( "configuration.xml");
			File f2 = new File(Processor.HomePath + Processor.Dirprefix + "configuration.xml");

			if (f1.isFile()) {

				InputStream in = new FileInputStream(f1);

				//Overwrite the file.
				OutputStream out = new FileOutputStream(f2);

				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0){
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
				q.Message("Config File stored.", 10);
			}
		}
		catch (Exception e) {
			q.Message("problem writing the config file", 10);
		}
	}


	static double decayaverage(double oldaverage, double newvalue, double factor)  {

		double newaverage = oldaverage;
		if (factor > 1) {
			newaverage = (oldaverage * (1 - 1 / factor)) + (newvalue / factor);
		}
		return newaverage;
	}

	/* TTY not implemented in Android
//Downgrade RX and TX modes alternatively (until we recover the link)
 static void DowngradeOneMode()  {
    int currentmodeindex = 0;

    if (Main.JustDowngradedRX) {
        JustDowngradedRX = false;
        Main.hiss2n = 50; //Reset to mid-range
        if (Main.UseAlttable) {
            currentmodeindex = m.getAltModemPos(Main.TxModem);
            if (currentmodeindex > 1) {
                Main.TxModem = m.getaltmode(currentmodeindex - 1);
            }
        } else {
            currentmodeindex = m.getAltModemPos(Main.TxModem);
            if (currentmodeindex > 1) {
                Main.TxModem = m.getmode(currentmodeindex - 1);
            }
        }
    } else {
         JustDowngradedRX = true;
         Main.mys2n = 50; //Reset to mid-range
         if (Main.UseAlttable) {
                currentmodeindex = m.getAltModemPos(Main.RxModem);
                if (currentmodeindex > 1) {
                    Main.RxModem = m.getaltmode(currentmodeindex - 1);
                    Main.RxModemString = m.getAltModemString(Main.RxModem);
                    blocktime = m.getBlockTime (Main.RxModem);
                }
          } else {
                currentmodeindex = m.getAltModemPos(Main.RxModem);
                if (currentmodeindex > 1) {
                    Main.RxModem = m.getmode(currentmodeindex - 1);
                    Main.RxModemString = m.getModemString(Main.RxModem);
                    blocktime = m.getBlockTime (Main.RxModem);
                }
          }
   }
 }

 static void ChangeMode ( modemmodeenum Modem) {
         String SendMode = "";
        if (UseAlttable) {
             SendMode ="<cmd><mode>" + getAltTXModemString(Modem) + "</mode></cmd>";
        } else {
            SendMode ="<cmd><mode>" + getTXModemString(Modem) + "</mode></cmd>";
        }
        m.Sendln(SendMode);
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (Sendline.length() > 0) {
            m.Sendln(Sendline);
            Main.TXActive = true;
        }
        Sendline = "";

 }
	 */

	static String myTime () {
		// create a java calendar instance
		Calendar calendar = Calendar.getInstance();

		// get a java.util.Date from the calendar instance.
		// this date will represent the current instant, or "now".
		java.util.Date now = calendar.getTime();

		// a java current time (now) instance
		java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());

		return currentTimestamp.toString().substring(0,16);
	}


	static void log (String logtext) {
		//           File consolelog = new File (HomePath + Dirprefix + "logfile");
		try{
			// Create file
			FileWriter logstream = new FileWriter(HomePath + Dirprefix + "logfile", true);
			BufferedWriter out = new BufferedWriter(logstream);

			out.write(myTime() + " " + logtext + "\n");
			//Close the output stream
			out.close();

		}catch (Exception e){//Catch exception if any
			//              System.err.println("LogError: " + e.getMessage());
			loggingclass.writelog("LogError " + e.getMessage() , null, true);
		}
		Processor.PostToTerminal(myTime() + " " + logtext + "\n");
	}


	@Override
	public IBinder onBind(Intent arg0) {
		// Nothing here, not used
		return null;
	}


} // end Main class



