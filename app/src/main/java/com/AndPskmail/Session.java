/*
 * Session.java
 *
 * Copyright (C) 2008 Per Crusefalk and Rein Couperus
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileInputStream;
import java.text.DecimalFormat;

/**
 *
 * @author rein
 */
public class Session {

	public String mycall;
	public String myserver;
	private static String blocklength;
	private static int Blocklength;
	public String session_id;
	private String Lineoutstring;

	// service flags
	public boolean Headers = false;
	public boolean  FileDownload = false;
	public boolean WWWDownload = false;
	public boolean CwwwDownload = false;
	public boolean FileList = false;
	public String ThisFile = "";
	public File Trfile = null;
	public String Transaction = "";
	public String ThisFileLength = "";
	public boolean  MsgDownload = false;
	public boolean CMsgDownload = false;
	public boolean base64attachment = false;
	public String attachmentFilename = null;

	//rx is my machine, tx is the other machine
	public  static String tx_lastsent;    //Last block sent to me
	public  static String tx_missing; //List of repeats  I need to resend.
	public static String  tx_ok;  //last block received conseq ok at other end.
	public static String tx_lastreceived;  // at other end

	private static   String rx_lastsent;    // Last block I sent
	private static   String rx_ok;   //  Text o.k  until this one
	private static  String rx_lastreceived;   // Last block I received
	public static   String rx_missing; //List of repeat requests I need to send to other side
	private static   boolean rx_lastBlock; // Flag for end of frame

	private static String[] rxbuffer = new String[64];
	private static int goodblock;
	private static int thisblock;
	private static int lastblock;
	public static int lastdisplayed = 0;
	private int lastgoodblock_received;
	public static boolean gooddata;
	public String serverversion;
	public String hispubkey = "";

	public static String[] txbuffer = new String[64] ;
	private static int lastqueued = 0;

	// progress bar values
	public static int DataReceived = 0;
	public static int DataSize = 0;

	private FileWriter headers = null;
	public FileWriter dlFile = null;
	public FileWriter pFile = null;
	private FileWriter tmpmessage = null;
	private FileWriter inbox = null;
	private arq a;
	private BufferedWriter iacout;
	//VK2ETA Not now (bulletins) private fastfec f;




	//  private  String lastqueued;  //Last block in my send queue

	public Session() {
		//              String path = Processor.HomePath + Processor.Dirprefix;
		//             config cf = new config(path);
		a = new arq();
		//              f = new fastfec();
		myserver = AndPskmail.myconfig.getServer();
		blocklength = "5";
		try {
			blocklength = AndPskmail.myconfig.getBlocklength();
		}
		catch(Exception e){
			blocklength = "5";
		}
		//             Blocklength = Integer.parseInt(blocklength);
		Blocklength = 5;
		Processor.TX_Text = "";

		initSession();
	}
	
	public void resetRecord() {
		Processor.TX_Text += "~RESETRECORD\n";
	}
	
	public boolean setPassword() {
		String mailpass = AndPskmail.myconfig.getPreference("POPPASS");
		if (mailpass.length() > 0 & Processor.Passwrd.length() > 0) {
			String intext = Processor.cr.encrypt(
				Processor.sm.hispubkey, mailpass + ","
				+ Processor.Passwrd);
			Processor.TX_Text += ("~Msp" + intext + "\n");
			//Processor.PostToTerminal("\n=>>" + intext + "\n");
			return true;
		} else {
			Processor.PostToTerminal("\n=>>" + "POP password or Session password NOT set?\n\n Check the Preferences\n");
			return false;
		}
	}

	
	public void sendUpdate(){

		String output;
		String record;
		String pophost = AndPskmail.myconfig.getPreference("POPHOST");
		String popuser = AndPskmail.myconfig.getPreference("POPUSER");
		String poppass = AndPskmail.myconfig.getPreference("POPPASS");
		String returnaddr = AndPskmail.myconfig.getPreference("RETURNADDRESS");
		String recinfo = pophost + "," + popuser + "," + poppass
				+ "," + returnaddr;

		if (Processor.sversion > 1.5){
			String rec64 = base_64.base64Encode(recinfo);
			output = Processor.cr.encrypt (Processor.sm.hispubkey, rec64);
			record = "~RECx" + output + "\n";
			while (record.length() > 30){
				Processor.TX_Text += record.substring(0, 30) + "\n";
				record = record.substring(30);
			}
			Processor.TX_Text += record + "Q\n";            
		} else {
			record = "~RECx" + base_64.base64Encode(recinfo);
			int eol_loc = -1;
			String frst = null;
			String secnd = null;
			eol_loc = record.indexOf(10);
			if ( eol_loc != -1) {
				frst = record.substring(0, eol_loc-1);
				secnd = record.substring(eol_loc+1, record.length());
				record = frst + secnd;
			}             
			Processor.TX_Text += record + "\n";
		}

		/* Removed older version - replaced above (VK2ETA)
 		if (Processor.sversion > 1.1) {
			recinfo += ",none\n";
		}

		if (Processor.sversion > 1.1) {
			String rec64 = base_64.base64Encode(recinfo);
			output = Processor.cr.encrypt (Processor.sm.hispubkey, rec64);
			//           output = Main.cr.encrypt (Main.sm.hispubkey, recinfo);
			record = "~RECx" + output;
		} else {
			record = "~RECx" + base_64.base64Encode(recinfo);
		}

		int eol_loc = -1;
		String frst = null;
		String secnd = null;
		eol_loc = record.indexOf(10);
		if ( eol_loc != -1) {
			frst = record.substring(0, eol_loc-1);
			secnd = record.substring(eol_loc+1, record.length());
			record = frst + secnd;
		}   
		Processor.TX_Text += record + "\n";
*/

	}


	public void RXStatus(String text){
		if (text.length() > 2) {
			tx_lastsent = text.substring(0,1);
			tx_lastreceived = text.substring(1,2);
			tx_ok = text.substring(2,3);
			tx_missing = text.substring(3);
			if (tx_missing.length() > 0 & Processor.Connected) {
				Processor.txbusy = true;                     
			} else {
				Processor.txbusy = false;
			}
		}
	}


	public String getTXStatus( ){
		rx_missing = "";
		int endblock;
		lastblock = (tx_lastsent.charAt(0) - 32);

		endblock = (lastgoodblock_received + 64) % 64;               ;

		int i = 0;
		int index = 0;

		int runvar = 0;
		if (lastblock  < lastdisplayed ) {
			runvar = lastblock +  64;
		} else {
			runvar = lastblock;
		}

		for (i =lastdisplayed + 1; i <= runvar; i++){
			index = i % 64;

			if (rxbuffer[index].equals("")){              ;

			char m = (char) (index + 32);
			rx_missing += Character.toString(m);
			} 
		}
		// generate the status block      

		if (rx_missing.length() == 0) {
			Processor.rxbusy = false;
		} else {
			Processor.rxbusy = true;
		}

		goodblock = lastdisplayed;
		char last =(char)(lastgoodblock_received + 32);
		rx_lastreceived = Character.toString(last);
		char ok =(char)(goodblock + 32);
		rx_ok= Character.toString(ok);

		if (rx_missing.length() > 8) {
			rx_missing = rx_missing.substring(0,8);
		}
		String outstr = rx_lastsent + rx_ok + rx_lastreceived + rx_missing;
		return outstr;
	}

	public String getRXmissing(){
		rx_missing = "";
		int i = 0;
		int end = thisblock;
		if (thisblock <= lastdisplayed  & lastdisplayed - thisblock > 49) {
			end = (thisblock + 64) % 64;
		}

		for (i = lastdisplayed + 1; i < end; i++){
			char m = (char) ((i % 64) + 32);
			rx_missing += Character.toString(m);
		}

		rx_missing = rx_missing.substring(0,8);

		return rx_missing;
	}

	//Used in Main loop for detecting idle TTY sessions by the TTY server
	public String getBlockExchanges() {
		//rx is my machine, tx is the other machine
		return(tx_lastsent + rx_lastsent);    //Last block sent to me  + Last block I sent
	}

	//Used in Main loop for measuring the link quality (from a data perspective)
	//public double RXGoodBlocksRatio() {
	//rx is my machine, tx is the other machine


	//  return(1);
	// }


	public void initSession(){
		tx_lastsent = " ";
		tx_lastreceived = " ";
		tx_ok = " ";
		tx_missing = "";
		rx_lastsent = " ";
		rx_ok = " ";
		rx_lastreceived = " ";
		rx_missing = "";
		rx_lastBlock = false;
		goodblock = 0;
		thisblock = 0;
		lastblock = 0;
		lastqueued = 1; // to make sure the first command is transmitted...
		lastdisplayed =0;
		gooddata = true;
		lastgoodblock_received = 0;  
		serverversion = "1.0";
		hispubkey = "";

		for (int i = 0; i < 64; i++){
			rxbuffer[i] = "";
			txbuffer[i] = "";
		}
		Lineoutstring = " ";
		Processor.rxbusy = false;
		Processor.txbusy = false;  
	}

	// handles the rx buffer and calculates the new TXStatus after every Block
	public String doRXBuffer(String block, String index) throws FileNotFoundException, IOException, Exception {

		if (block.length() > 0) { //valid block
			thisblock = index.charAt(0) - 32;
			if (thisblock < 64) {
				rxbuffer[thisblock] = block;
			}

			if (lastdisplayed > 63){
				lastdisplayed -= 64;
			}

			if (thisblock  > lastgoodblock_received | (lastgoodblock_received - thisblock) > (64 - 24) ) {
				lastgoodblock_received = thisblock;
			} 

			while(!rxbuffer[(lastdisplayed+1) % 64].equals("")  ){
				// display this block

				lastdisplayed++;
				lastdisplayed %= 64;

				// set goodblock

				goodblock = lastdisplayed;



				if ((lastdisplayed  > lastgoodblock_received) | (lastgoodblock_received - lastdisplayed) > (64 - 17)
						| (lastdisplayed == 0 & thisblock == 0)) {

					lastgoodblock_received = thisblock;
				} 
				// output to main window
				int maxwait = 0;
				while (Processor.mainmutex) {
					try {
						Thread.sleep(5);
						maxwait++;
						if (maxwait > 100) {
							break;
						}
					} catch (InterruptedException ex) {
						Logger.getLogger(Processor.class.getName()).log(Level.SEVERE, null, ex);
					}
				}

				if (! rxbuffer[lastdisplayed].startsWith("~FA:")) {
					Processor.PostToTerminal(rxbuffer[lastdisplayed]);
				}



				// parse commands
				Lineoutstring += rxbuffer[lastdisplayed];

				int Linebreak = -1;
				while (Lineoutstring.indexOf("\n") >= 0) {
					Linebreak = Lineoutstring.indexOf("\n");
					if (Linebreak  >= 0) {
						String fullLine = Lineoutstring.substring(0,Linebreak);
						Lineoutstring = Lineoutstring.substring(Linebreak + 1);

						parseInput(fullLine);

					}

				}

			}

			// make room for more data
			if (lastdisplayed == lastgoodblock_received) {
				int i = 0;
				for (i = lastdisplayed + 8 ; i < lastdisplayed + 32; i++){
					rxbuffer[i % 64] = "";
				}
			}



		}

		return "";
	}

	public void parseInput(String str) throws NoClassDefFoundError, FileNotFoundException, IOException, Exception{
		boolean Firstline = false;
		/* NO TTY Server function for now      
       // ~QUIT for TTY session...
                                Pattern TTYm = Pattern.compile("^\\s*~QUIT");
                                     Matcher tm = TTYm.matcher(str);
                                    if (Main.TTYConnected.equals("Connected") & tm.lookingAt()) {
                                        Main.disconnect = true;
                                        Main.log ("Disconnect request from " + Main.TTYCaller);

                                    } else if (tm.lookingAt()) {
                                        Main.TX_Text = "~QUIT\n";
                                    }
       // ~LISTFILES for TTY session...
                                Pattern LFm = Pattern.compile("^\\s*~LISTFILES");
                                     Matcher lf = LFm.matcher(str);
       //Open both ways                        if (Main.TTYConnected.equals("Connected") & lf.lookingAt()) {
                                    if (lf.lookingAt()) {
                                        String downloaddir = Main.HomePath + Main.Dirprefix + "Downloads" + Main.Separator;
                                        File dd = new File (downloaddir);
                                        String[] filelist = dd.list();
                                        Main.TX_Text += ("Your_files: " + Integer.toString(filelist.length) + "\n");
                                        for (int i = 0; i < filelist.length; i++) {
                                            Main.TX_Text += (filelist[i] + "\n");
                                        }
                                        Main.TX_Text += "-end-\n";
                                    }
       // ~GETBIN for TTY session...
                                Pattern GBm = Pattern.compile("^\\s*~GETBIN\\s(\\S+)");
                                     Matcher gb = GBm.matcher(str);
//Open both ways                                    if (Main.TTYConnected.equals("Connected") & gb.lookingAt()) {
                                    if (gb.lookingAt()) {
                                        String downloaddir = Main.HomePath + Main.Dirprefix + "Downloads" + Main.Separator;

                                         String codedFile = "";
                                         String token = "";
                                         String myfile = gb.group(1);
                                         String mypath = downloaddir + gb.group(1);

                                        if (mypath.length() > 0){

                                            String Destination = myserver;

                                            FileInputStream in = null;

                                            File incp = new File(mypath);
                                            File outcp = new File (Main.HomePath + Main.Dirprefix + "tmpfile");

                                            FileInputStream fis = null;
                                            try {
                                                fis = new FileInputStream(incp);
                                            } catch (FileNotFoundException ex) {
                                                Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(outcp);
                                            } catch (FileNotFoundException ex) {
                                                Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                            }

                                            try {
                                                byte[] buf = new byte[1024];
                                                int i = 0;
                                                while ((i = fis.read(buf)) != -1) {
                                                    fos.write(buf, 0, i);
                                                }
                                            }
                                            catch (Exception e) {
                                                Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, e);
                                            }
                                            finally {
                                                if (fis != null) try {
                                                    fis.close();
                                                    } catch (IOException ex) {
                                                         Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                                if (fos != null) try {
                                                    fos.close();
                                                } catch (IOException ex) {
                                                 Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }

                                           String mysourcefile = Main.HomePath + Main.Dirprefix + "tmpfile";

                                            try {
                                                in = new FileInputStream(mysourcefile);
                                            } catch (FileNotFoundException ex) {
                                                Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                            }

                                            GZIPOutputStream myzippedfile = null;

                                            String tmpfile = Main.HomePath + Main.Dirprefix + "tmpfile.gz";

                                            try {
                                                 myzippedfile = new GZIPOutputStream(new FileOutputStream(tmpfile));
                                            } catch (FileNotFoundException ex) {
                                                Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                            catch (IOException ioe) {
                                                Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ioe);
                                            }

                                            byte[] buffer = new byte[4096];
                                            int bytesRead;

                                            try {
                                                while ((bytesRead = in.read(buffer)) != -1) {
                                                    myzippedfile.write(buffer, 0, bytesRead);
                                                }
                                            } catch (IOException ex) {
                                                Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                            }

                                            try {
                                                in.close();
                                                myzippedfile.close();
                                            } catch (IOException ex) {
                                                Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                            }

                                            Random r = new Random();
                                            token = Long.toString(Math.abs(r.nextLong()), 12);
                                            token = "tmp" + token;

                                            codedFile = Main.HomePath + Main.Dirprefix + "Outpending" + Main.Separator + token;

                                            Base64.encodeFileToFile(tmpfile, codedFile);

                                            File dlfile = new File(tmpfile);
                                            if (dlfile.exists()){
                                                dlfile.delete();
                                            }

                                            String TrString = "";
                                            File mycodedFile = new File (codedFile);
                                             if (mycodedFile.isFile()) {
                                                TrString = ">FM:" + a.callsign + ":" + Destination + ":"
                                                + token + ":u:" + myfile
                                                + ":" + Long.toString(mycodedFile.length()) + "\n";
                                             }


                                            if (Main.Connected) {
                                                    if (mycodedFile.isFile()) {
                                                        Main.TX_Text += "~FO5:" + a.callsign + ":" + Destination + ":"
                                                        + token + ":u:" + myfile
                                                        + ":" + Long.toString(mycodedFile.length()) + "\n";
                                                }
                                            }

                                        File Transactions = new File (Main.Transactions);
                                        FileWriter tr;
                                        try {
                                            tr = new FileWriter(Transactions, true);
                                            tr.write(TrString);
                                            tr.close();
                                        } catch (IOException ex) {
                                            Logger.getLogger(mainpskmailui.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                      }
                                    }
		 */

		// APRS positions
		//P14,PE1FTV,51.35300,5.38517
		Pattern APRSm = Pattern.compile("(\\w\\d+),(\\S+),(\\-*\\d+\\.\\d+),(\\-*\\d+\\.\\d+)");
		Matcher am = APRSm.matcher(str);
		if (am.lookingAt()){
			String wptype = am.group(1);
			String Tag = am.group(2);
			String Lat = am.group(3);
			String Lon = am.group(4);
			long epoch = System.currentTimeMillis()/1000;

			//String outline ="SK0QO-B>SK0QO-BS:!5914.24ND01814.61E&RNG0050 70cm Voice 434.575 -2.00 MHz";

			String aprsString = "";

			aprsString =  convert_to_aprsformat(Tag, Lat, Lon, "", wptype);

			//JD Do nothing for now                                        Main.mapsock.sendmessage(aprsString);

			int i = 0;
			for (i=0; i<20;i++) {

				if (Processor.Positions[i][0] == null ) {
					Processor.Positions[i][0] = wptype;
					Processor.Positions[i][1] = Tag;
					Processor.Positions[i][2] = Lat;
					Processor.Positions[i][3] = Lon;
					Processor.Positions[i][4] =Long.toString( epoch);
					break;
				} else if (Processor.Positions[i][1].equals(Tag)){
					Processor.Positions[i][2] = Lat;
					Processor.Positions[i][3] = Lon;
					Processor.Positions[i][4] =Long.toString( epoch);
					break;
				}
				if ((epoch - Long.parseLong(Processor.Positions[i][4]))/1000 > 180) {
					Processor.Positions[i][0] = null;
					Processor.Positions[i][1] = null;
					Processor.Positions[i][2] = null;
					Processor.Positions[i][3] = null;
					Processor.Positions[i][4] = null;
				}
				if (Processor.Positions[i][0] != null){

				}
			}
		}

		// Record updated
		//Modified as per JPskmail V1.7b
		//Pattern SPC = Pattern.compile("^Updated database for");
        Pattern SPC = Pattern.compile("^Updated data");
        Matcher spc = SPC.matcher(str);
        if (spc.lookingAt()){
            if (Processor.sm.hispubkey.length() > 0){
                String mailpass = AndPskmail.myconfig.getPreference("POPPASS", "none");
                if (mailpass.length() > 0 & Processor.Passwrd.length() > 0) {
                    String intext = Processor.cr.encrypt (Processor.sm.hispubkey, mailpass + "," + Processor.Passwrd);
                    Processor.TX_Text += ("~Msp" + intext + "\n");
                    Processor.PostToTerminal("\nSending Link Password...\n");
                } else {
                	Processor.PostToTerminal("\n=>>" + "No POP password or Link password set?\n");
                }
            } else {
            	Processor.PostToTerminal("\n=>>" + "No server public key... reconnect....\n");
            }
        }                                    

		// mail headers 
		Pattern pm = Pattern.compile("^\\s*(Your\\smail:)\\s(\\d*)");
		Matcher mm = pm.matcher(str);
		if (mm.lookingAt()) {
			if (mm.group(1).equals("Your mail:")){   
				Headers = true;
				Firstline = true;
				DataSize = Integer.parseInt(mm.group(2));
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
				try{
					this.headers = new FileWriter(Processor.HomePath + Processor.Dirprefix + "headers", true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the headers file.", e, true);
				}
			}
		}



		// IAC fleetcodes
		if (!Processor.Connected){
			Pattern pc = Pattern.compile(".*<SOH>(ZFZF)");
			Matcher mc = pc.matcher(str);
			if (mc.lookingAt()) {
				if (mc.group(1).equals("ZFZF")){   
					Processor.IACmode = true;
					Firstline = true;
					try {                                            
						iacout = new BufferedWriter(new FileWriter(Processor.HomePath + Processor.Dirprefix + "iactemp", true));
						a.Message("Receiving IAC fleetcode file", 15);
					} 
					catch (Exception e){
						loggingclass.writelog("Error when trying to open the iac file.", e, true);
						a.Message("Error opening file...", 15);
					}
				}
			}                                
		}                                    

		// file list
		Pattern pl = Pattern.compile("^\\s*(Your_files:)\\s(\\d+)");
		Matcher ml = pl.matcher(str);
		if (ml.lookingAt()) {
			if (ml.group(1).equals("Your_files:")){
				FileList = true;
				Firstline = true;
				// set progress indicator here...
				ThisFileLength = ml.group(2);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
			}
		}

		// file download
		Pattern pf = Pattern.compile("^\\s*(Your\\sfile:)(.*)\\s(\\d+)");
		Matcher mf = pf.matcher(str);
		if (mf.lookingAt()) {
			if (mf.group(1).equals("Your file:")){   
				FileDownload = true;
				Processor.comp = true;
				Firstline = true;
				ThisFile = mf.group(2);
				// set progress indicator here...
				ThisFileLength = mf.group(3);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
				try{
					File tmp = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
					tmp.delete();                                              
					this.dlFile = new FileWriter(new File (Processor.HomePath + Processor.Dirprefix + "TempFile"), true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the download file.", e, true);
				}
			}
		}
		// >FM:PI4TUE:PA0R:Jynhgf:f:test.txt:496
		Pattern fm = Pattern.compile(">FM:(\\S+):(\\S+):(\\S+):(\\w):(.*):(\\d+)");
		Matcher fmm = fm.matcher(str);
		if (fmm.lookingAt()) {
			if (fmm.group(4).equals("f")){
				FileDownload = true;
				Processor.comp = true;
				Firstline = true;
				ThisFile = fmm.group(5);
				Transaction = fmm.group(3);
				// set progress indicator here...
				ThisFileLength = fmm.group(6);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
				try{
					File tmp = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
					tmp.delete();
					// open filewriter for TempFile
					this.dlFile = new FileWriter(new File (Processor.HomePath + Processor.Dirprefix +"TempFile"), true);
					// copy pending file into temp
					File pending = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);

					if (pending.exists()) {
						DataReceived = (int) pending.length();
						FileReader in = new FileReader(pending);
						int c;
						// copy the pending file
						while ((c = in.read()) != -1)
							dlFile.write(c);
						// close pending file
						in.close();
					}

				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the download file.", e, true);
				}
				try {
					Trfile = new File(Processor.pendingstr + Transaction);
					// open pending file (now called pFile) for write
					this.pFile = new FileWriter (Trfile, true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the pending file.", e, true);
				}
			} else if (fmm.group(4).equals("u")){
				FileDownload = true;
				Processor.comp = true;
				Firstline = true;
				ThisFile = fmm.group(5) + ".gz";
				Transaction = fmm.group(3);
				// set progress indicator here...
				ThisFileLength = fmm.group(6);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
				Processor.log("Receiving file " + ThisFile + " from " + Processor.TTYCaller);
				try{
					File tmp = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
					tmp.delete();
					// open filewriter for TempFile
					this.dlFile = new FileWriter(new File (Processor.HomePath + Processor.Dirprefix +"TempFile"), true);
					// copy pending file into temp
					File pending = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);

					if (pending.exists()) {
						DataReceived = (int) pending.length();
						FileReader in = new FileReader(pending);
						int c;
						// copy the pending file
						while ((c = in.read()) != -1)
							dlFile.write(c);
						// close pending file
						in.close();
					}

				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the download file.", e, true);
				}
				try {
					Trfile = new File(Processor.pendingstr + Transaction);
					// open pending file (now called pFile) for write
					this.pFile = new FileWriter (Trfile, true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the pending file.", e, true);
				}
				// compressed web page open...
			} else if (fmm.group(4).equals("w")){
				CwwwDownload = true;
				Processor.comp = true;
				Firstline = true;
				ThisFile = fmm.group(5);
				Transaction = fmm.group(3);
				// set progress indicator here...
				ThisFileLength = fmm.group(6);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
				try{
					File tmp = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
					tmp.delete();
					// open filewriter for TempFile
					this.dlFile = new FileWriter(new File (Processor.HomePath + Processor.Dirprefix +"TempFile"), true);
					// open File for pending
					File pending = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);

					if (pending.exists()) {
						DataReceived = (int) pending.length();
						FileReader in = new FileReader(pending);
						int c;
						// copy the pending file
						while ((c = in.read()) != -1)
							dlFile.write(c);
						// close pending file
						in.close();
						//                                                    pending.delete();
					}

				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the download file.", e, true);
				}
				try {
					Trfile = new File(Processor.pendingstr + Transaction);
					this.pFile = new FileWriter (Trfile, true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the pending file.", e, true);
				}

				// compressed mail download
			} else if (fmm.group(4).equals("m")){
				CMsgDownload = true;
				Transaction = fmm.group(3);
				Processor.comp = true;
				Firstline = true;
				try{
					File F1 = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage");  // make sure it is empty
					if (F1.exists()){
						F1.delete();
					}
					File F2 = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage.gz");  // make sure it is empty
					if (F2.exists()){
						F2.delete();
					}
					tmpmessage = new FileWriter(Processor.HomePath + Processor.Dirprefix + "tmpmessage", true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the temporary files.", e, true);
				}
				// set progress indicator here...
				ThisFileLength = fmm.group(6);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);

				File pending = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);

				if (pending.exists()) {
					DataReceived = (int) pending.length();
					FileReader in = new FileReader(pending);
					int c;
					// copy the pending file
					while ((c = in.read()) != -1)
						tmpmessage.write(c);
					// close pending file
					in.close();
				}


				try {
					Trfile = new File(Processor.pendingstr + Transaction);
					pFile = new FileWriter (Trfile, true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the pending file.", e, true);
				}

			}

		}


		// >FO5:PI4TUE:PA0R:JhyJkk:f:test.txt:496
		Pattern ofr = Pattern.compile(">FO(\\d):([A-Z0-9\\-]+):([A-Z0-9\\-]+):([A-Za-z0-9_-]+):(\\w)");
		Matcher ofrm = ofr.matcher(str);
		if (ofrm.lookingAt()) {
			if (ofrm.group(5).equals("f") | ofrm.group(5).equals("w") | ofrm.group(5).equals("m")) {
				// get the file ?
				File pending = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + ofrm.group(4));
				long x = 0;
				if (pending.exists()) {
					x = pending.length();
				}
				String myresume = AndPskmail.myconfig.getPreference("RESUME", "Accept");
				if (myresume.equals("Accept")) {
					Processor.TX_Text += "~FY:" + ofrm.group(4) + ":" + Long.toString(x) + "\n";
				} else if (myresume.equals("Reject")){
					Processor.TX_Text += "~FN:" + ofrm.group(4) +  "\n";
				} else {
					Processor.TX_Text += "~FA:" + ofrm.group(4) + "\n";
					if (pending.exists()) {
						pending.delete();
					}

				}
			}                                        
		}

		//  ~FO5:PI4TUE:PA0R:tmpasdkkdfj:u:test.txt:36
		Pattern ofr2 = Pattern.compile(".*~FO(\\d):([A-Z0-9\\-]+):([A-Z0-9\\-]+):([A-Za-z0-9_-]+):(\\w).*");
		Matcher ofrm2 = ofr2.matcher(str);
		if (ofrm2.lookingAt()) {

			if (ofrm2.group(5).equals("u")) {  
				// get the file ?
				File pending = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + ofrm2.group(4));
				long x = 0;
				if (pending.exists()) {
					x = pending.length();
				}


				String myresume = AndPskmail.myconfig.getPreference("RESUME", "Accept");

				if (myresume.equals("Accept")) {
					Processor.TX_Text += "~FY:" + ofrm2.group(4) + ":" + Long.toString(x) + "\n";
				} else if (myresume.equals("Reject")){
					Processor.TX_Text += "~FN:" + ofrm2.group(4) +  "\n";
				} else {
					Processor.TX_Text += "~FA:" + ofrm2.group(4) + "\n";
					if (pending.exists()) {
						pending.delete();
					}

				}

			}
		}



		// ~FY:tmpjGUytg:request partial email upload 
		Pattern yfr = Pattern.compile(".*~FY:([A-Za-z0-9]+):(\\d+)");
		Matcher yfrm = yfr.matcher(str);
		if (yfrm.lookingAt()) {
			String partialfile = yfrm.group(1);
			String startingbyte = yfrm.group(2);
			//                     System.out.println(partialfile);
			int start = Integer.parseInt(startingbyte);
			File penf = new File(Processor.Pendingdir + partialfile);
			File foutpending = new File(Processor.Outpendingdir + partialfile);
			String filename = "";

			if (penf.exists()) {

				int i = 0;
				try {
					FileInputStream fis = new FileInputStream(penf);
					char current;
					String callsign = AndPskmail.myconfig.getPreference("CALL");
					callsign = callsign.trim();
//					String servercall = AndPskmail.myconfig.getPreference("SERVER");
					String servercall = AndPskmail.serverToCall;
					servercall = servercall.trim();
					long flen = 0;
					flen = penf.length() - start;

					Processor.TX_Text += ">FM:" + callsign + ":" + servercall + ":" + partialfile + ":s: :" + Long.toString(flen) +  "\n";

					while (fis.available() > 0) {
						current = (char) fis.read();
						i++;

						if (i > start) {
							Processor.TX_Text += current;
						}
					}

					Session.DataSize = Processor.TX_Text.length();

					fis.close();

				} catch (IOException e) {
					//                                                System.out.println("IO error on pending file");
				}
			} else if (foutpending.exists()){

				int i = 0;
				try {
					FileInputStream fis = new FileInputStream(foutpending);
					char current;
					String callsign = AndPskmail.myconfig.getPreference("CALL","NOCAL");
					callsign = callsign.trim();
					String servercall = AndPskmail.myconfig.getPreference("SERVER","NOCAL");
					servercall = servercall.trim();


					File ft = new File(Processor.Transactions);
					String[] ss = null;
					if (ft.exists()){
						//                                    System.out.println("Transactons exists");
						FileReader fr = new FileReader(Processor.Transactions);

						BufferedReader br = new BufferedReader(fr);
						String s;

						while((s = br.readLine()) != null) {
							//                                   System.out.println("s=:" + s);

							ss = s.split(":") ;
							//                                  System.out.println(ss[5]);
							if (s.contains(partialfile)) {
								//                                    System.out.println(s);
								filename = ss[5];
							}
						}
						fr.close();
					}

					long flen = 0;
					flen = foutpending.length() - start;


					Processor.TX_Text += ">FM:" + callsign + ":" + servercall + ":" + partialfile + ":u:" + filename + ":" + Long.toString(flen) +  "\n";

					while (fis.available() > 0) {
						current = (char) fis.read();
						i++;

						if (i > start) {
							Processor.TX_Text += current;
						}
					}

					fis.close();

					Processor.TX_Text += "\n-end-\n";

					Session.DataSize = Integer.parseInt(ss[6]);

				} catch (IOException e) {
					//                                                System.out.println("IO error on pending file");
				}

			}

			Processor.log("Sending file: " + filename);
		}

		// ~FA:tmpjGUytg  delete output file in Outbox
		Pattern afr = Pattern.compile("~FA:([A-Za-z0-9]+)");
		Matcher afrm = afr.matcher(str);
		if (afrm.lookingAt()) {

			String deletefl = afrm.group(1);
			str = "";

			try {
				File df = new File(Processor.HomePath + Processor.Dirprefix + "Outbox" + Processor.Separator + deletefl);
				if (df.exists()) {
					df.delete();
					Processor.log ("Mail sent to Server...");
				}
				File outpenf = new File(Processor.Outpendingdir + deletefl);
				if (outpenf.exists()) {
					outpenf.delete();
					Processor.PostToTerminal(Processor.myTime() + " File stored...\n");
					Processor.FilesTextArea += " File stored...\n";
				}
				File penf = new File(Processor.Pendingdir + deletefl);
				if (penf.exists()) {
					penf.delete();
					//                                                Processor.PostToTerminal("Mail sent on server...\n");
					//                                                Processor.FilesTextArea += "Mail sent on server...\n";
				}

				if (TransactionsExists()) {
					FileReader trf = new FileReader (Processor.Transactions);
					BufferedReader tr = new BufferedReader(trf);

					String sta[] = new String[20];
					String st;
					int st1 = 0;
					while((st = tr.readLine()) != null & st1 < 20) {
						if (!st.contains(deletefl)){
							sta[st1] = st;
							st1++;
						}

					}
					tr.close();


					File trw = new File(Processor.Transactions);
					if (trw.exists()){
						trw.delete();
					}
					try {
						if (sta[0] != null) {
							FileWriter trwo = new FileWriter(Processor.Transactions, true);
							int k = 0;

							while (k <= st1) {
								if (!sta[k].contains(deletefl)){
									trwo.write(sta[k]);
									k++;
								}
							}
							trwo.close();
						}
					}
					catch (NullPointerException npe) {
						//                                 System.out.println("nullpointerproblem:" + npe);
					}

				}

				// reset progress bar
				Session.DataSize = 0;
				Session.DataReceived = 0;
				Processor.Progress = 0;
				a.Message("Upload complete...", 10);
				str = "";
				//                                           System.out.println("Deleting temp file " + deletefl);
			}
			catch (IOException i){
				//                                            System.out.println("IO problem:" + i);
			}

		}


		// message receive
		Pattern pmsg = Pattern.compile("^\\s*(Your\\smsg:)\\s(\\d+)");
		Matcher mmsg = pmsg.matcher(str);
		if (mmsg.lookingAt()) {
			if (mmsg.group(1).equals("Your msg:")){   
				MsgDownload = true;
				Firstline = true;
				try{
					this. tmpmessage = new FileWriter(Processor.HomePath + Processor.Dirprefix + "tmpmessage", true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the headers file.", e, true);
				}
				// set progress indicator here...
				ThisFileLength = mmsg.group(2);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
			}
		}                                    
		// compresssed message receive
		Pattern cpmsg = Pattern.compile("^\\s*(~ZIPPED64)\\s(\\d+)");
		Matcher cmmsg = cpmsg.matcher(str);
		if (cmmsg.lookingAt()) {
			if (cmmsg.group(1).equals("~ZIPPED64")){   
				CMsgDownload = true;
				Processor.comp = true;
				Firstline = true;
				try{
					File F1 = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage");  // make sure it is empty
					F1.delete();
					File F2 = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage.gz");  // make sure it is empty
					F2.delete();
					this. tmpmessage = new FileWriter(Processor.HomePath + Processor.Dirprefix + "tmpmessage", true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the headers file.", e, true);
				}
				// set progress indicator here...
				ThisFileLength = cmmsg.group(2);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
			}
		}                                    



		// web page receive
		Pattern pw = Pattern.compile("^\\s*(Your\\swwwpage:)\\s(\\d+)");
		Matcher mw = pw.matcher(str);
		if (mw.lookingAt()) {
			if (mw.group(1).equals("Your wwwpage:")){   
				WWWDownload = true;
				Firstline = true;
				// set progress indicator here...
				ThisFileLength = mw.group(2);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
			}
		}     

		//compressed  website receive
		Pattern tgmsg = Pattern.compile("^\\s*(~TGET64)\\s(\\d+)");
		Matcher tgmmsg = tgmsg.matcher(str);
		if (tgmmsg.lookingAt()) {
			if (tgmmsg.group(1).equals("~TGET64")){   
				CwwwDownload = true;
				Processor.comp = true;
				Firstline = true;
				try{
					File F1 = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage");  // make sure it is empty
					if (F1.exists()){
						F1.delete();
					}
					File F2 = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage.gz");  // make sure it is empty
					if (F2.exists()){
						F2.delete();
					}
					this. tmpmessage = new FileWriter(Processor.HomePath + Processor.Dirprefix + "tmpmessage", true);
				}
				catch (Exception e){
					loggingclass.writelog("Error when trying to open the temp file.", e, true);
				}
				// set progress indicator here...
				ThisFileLength = tgmmsg.group(2);
				DataSize = Integer.parseInt(ThisFileLength);
				DataReceived = 0;
				Processor.DataSize = Integer.toString(DataSize);
			}
		}                                    


		// Message sent...
		Pattern ps = Pattern.compile("^\\s*(Message sent\\.\\.\\.)");
		Matcher ms = ps.matcher(str);
		if (ms.lookingAt()) {
			if (ms.group(1).equals("Message sent...")){
				try{
					File fd = new File(Processor.Mailoutfile);
					fd.delete();
				}
				catch (Exception e){
					loggingclass.writelog("Error deleting mail file.", e, true);
				}
			}
		}                                    
		// -end- command
		Pattern pe = Pattern.compile("^\\s*(\\S+)");
		Matcher me = pe.matcher(str);
		if (me.lookingAt()) {
			if (me.group(1).equals("-end-")){
				if (Headers) {
					Headers = false;
					DataReceived = 0;
					Processor.DataSize = Integer.toString(0);

					try {
						this.headers.close();
					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the headers file.", ex, true);
					}
				}

				if (FileList) {
					FileList = false;
				}

				if (FileDownload){
					FileDownload = false;
					Processor.comp = false;

					try {
						this.dlFile.close();
					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the download file.", ex, true);
					}

					try {
						Base64.decodeFileToFile(Processor.HomePath + Processor.Dirprefix + "TempFile", Processor.HomePath + Processor.Dirprefix + "Downloads" + Processor.Separator + ThisFile);

						Unzip.Unzip(Processor.HomePath + Processor.Dirprefix + "Downloads" + Processor.Separator + ThisFile);

						File tmp = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);
						if (tmp.exists()){
							tmp.delete();
						}

						Processor.TX_Text += "~FA:" + Transaction + "\n";


						Processor.Progress = 0;

					}
					catch (Exception exc){
						loggingclass.writelog("Error when trying to decode the downoad file.", exc, true);
					}
					catch (NoClassDefFoundError exp) {
						a.Message("problem decoding B64 file", 10);
					}
					File tmp = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
					boolean success = tmp.delete();

					try {
						if (pFile != null) {
							Processor.sm.pFile.close();
							Processor.sm.Trfile.delete();
						}
					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the pending file.", ex, true);
					}
					Processor.Progress = 0;

					Processor.log(ThisFile + " received");

				}
				// messages  download      - append tmpmessage to Inbox in mbox format                          
				if (MsgDownload) {
					MsgDownload = false;
					this.tmpmessage.close();
					// append to Inbox file
					FileReader fr = new FileReader(Processor.HomePath + Processor.Dirprefix + "tmpmessage");
					BufferedReader br = new BufferedReader(fr);
					// all local stuff
					String s;
					String From = null;
					String Date = null;
					String Sub = null;
					String outstr = "";
					String attachment = "";
					base64attachment = false;
					// read tmpmessage line by line
					while((s = br.readLine()) != null) {
						// compile some patterns and set up the matchers
						Pattern pfrm = Pattern.compile("^\\s*(From:)\\s(.*)");
						Matcher mfrm = pfrm.matcher(s);
						Pattern pdate = Pattern.compile("^\\s*(Date:)\\s(\\w{3})\\,*\\s+(\\d+)\\s(\\w{3})\\s(\\d{4})\\s(\\d\\d:\\d\\d:\\d\\d)");
						Matcher mdate = pdate.matcher(s);
						Pattern pdate2 = Pattern.compile("^\\s*(Date:)\\s(\\d+)\\s+(\\w{3})\\s+(\\d{4}\\s\\d\\d:\\d\\d:\\d\\d)");
						Matcher mdate2 = pdate2.matcher(s);                                                      
						Pattern psub = Pattern.compile("^\\s*(Subject:)\\s(.*)");
						Matcher msub = psub.matcher(s);
						Pattern p64 = Pattern.compile("^\\s*(content-transfer-encoding: base64)");
						Matcher m64 = p64.matcher(s.toLowerCase());
						Pattern pnm = Pattern.compile(".*(filename=)(.*)");
						Matcher mnm = pnm.matcher(s);                                                       
						if (mfrm.lookingAt()) {
							if (mfrm.group(1).equals("From:")){   
								From = mfrm.group(2);
							} 
						} else if (mdate.lookingAt()){
							if (mdate.group(1).equals("Date:")) {
								Date = mdate.group(2) + " " + mdate.group(3) + " " + 
										mdate.group(4) + " " + mdate.group(5) + " " +
										mdate.group(6);
							}

						} else if (mdate2.lookingAt()){
							if (mdate2.group(1).equals("Date:")) {
								Date = mdate2.group(2) + " " + mdate2.group(3) + " " + 
										mdate2.group(4);
							}

						} else if  (msub.lookingAt()){
							if (msub.group(1).equals("Subject:")) {
								Sub = msub.group(2);
							}

						} else if  (m64.lookingAt()){
							if (m64.group(1).equals("content-transfer-encoding: base64")) {
								// there is an attachment...
								base64attachment = true;
								//                                                                debug ("Attachment");
							}
						} else if  (mnm.lookingAt()){
							if (mnm.group(1).equals("filename=")) {
								// get the file name, remove double quotes if any
								attachmentFilename = mnm.group(2).replace("\"", "");
							}
						} else {
							if (base64attachment) {                                                      
								if (!s.equals("")) {
									if (!s.startsWith("--")){
										attachment += s + "\n";
									} else {
										base64attachment = false;  
										if (attachment.length() > 10 & attachmentFilename != null){
											// is it a grib file?
											if (attachmentFilename.endsWith(".grb")) {
												// are we on linux?
												if (Processor.Separator.equals("/")) {
													try {
														File f1 = new File("/opt/zyGrib/grib/");
														// is zygrib installed?
														if (f1.isDirectory()) {
															attachmentFilename ="/opt/zyGrib/grib/" + attachmentFilename ;
														} else {
															// no, put it in the Files directory
															File myfiles = new File ("Files");
															if (!myfiles.isDirectory()) {
																myfiles.mkdir();
															}

															attachmentFilename = "Files/" + attachmentFilename;
															attachmentFilename = Processor.HomePath + Processor.Dirprefix + attachmentFilename;

														}   
													}
													catch (Exception e) {
														a.Message("IO problem", 10);
													}
												} 
											} else {
												// no grib file
												File myfiles = new File ("Files");
												if (!myfiles.isDirectory()) {
													myfiles.mkdir();
												}

												attachmentFilename = "Files/" + attachmentFilename;
												attachmentFilename = Processor.HomePath + Processor.Dirprefix + attachmentFilename;
											}
											try {
												File myfiles = new File (Processor.HomePath + Processor.Dirprefix + "Files");
												if (!myfiles.isDirectory()) {
													myfiles.mkdir();
												}
												// remove first line if "X-Attachment..."
												String[] attlines = attachment.split("\n");
												if (attlines[0].startsWith("X-Attachment")) {
													attachment = "";
													for (int i = 1; i < attlines.length; i++) {
														attachment += attlines[i] + "\n";
													}
												}
												//loggingclass.writelog("Un-compressed: >"+attachmentFilename+"< length() = " + Integer.toString(attachmentFilename.length()), null, true);
												Base64.decodeToFile(attachment, attachmentFilename);
												a.Message ("File stored", 10);
												Processor.PostToTerminal("\n File stored in " + attachmentFilename + "\n");
											}
											catch (Exception e){
												a.Message("Problem with decoding", 10);
											}

										}
									}
								}
							} else {                    // no attachment, body text for Inbox
								outstr += s + "\n";
								//                                                                debug ("Out=" + s);
							}
						}

					} // end while

					fr.close();
					this.inbox = new FileWriter(Processor.HomePath + Processor.Dirprefix + "Inbox", true);
					inbox.write("From " + From + " " + Date + "\n");
					inbox.write("From: " + From + "\n");
					if (Date != null)   {
						inbox.write("Date: " +Date + "\n");
					}
					inbox.write("Subject: " + Sub + "\n");
					// write message body
					if  (outstr != null) {
						inbox.write(outstr + "\n");
					}
					inbox.flush();
					inbox.close();

					File fl = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage");
					if (fl.exists()) {
						fl.delete();
					}

					a.Message("Added to mbox queue", 10);
				}

				// compressed messages  download      - append tmpmessage to Inbox in mbox format                          
				if (CMsgDownload) {
					CMsgDownload = false;
					Processor.comp = false;
					tmpmessage.close();
					// decode base 64 and unzip...
					String cin = Processor.HomePath + Processor.Dirprefix + "tmpmessage";
					String cout  = Processor.HomePath + Processor.Dirprefix + "tmpmessage.gz";
					String cmid = "";
					try {
						Base64.decodeFileToFile (cin, cout);
						cmid = Unzip.Unzip (Processor.HomePath + Processor.Dirprefix + "tmpmessage.gz");
					}
					catch (Exception e) {
						a.Message ("Decoding error!", 10);
					}

					// append to Inbox file
					FileReader fr = new FileReader(Processor.HomePath + Processor.Dirprefix + "tmpmessage");
					BufferedReader br = new BufferedReader(fr);
					// all local stuff
					String s;
					String From = null;
					String Date = null;
					String Sub = null;
					String outstr = "";
					String attachment = "";
					base64attachment = false;

					// make some room on the screen...
					Processor.PostToTerminal("\n\n");
					// read tmpmessage line by line
					while((s = br.readLine()) != null) {
						// show what we've got...
						int maxwait = 0;
						while (Processor.mainmutex) {
							try {
								Thread.sleep(5);
								maxwait++;
								if (maxwait > 100) {
									break;
								}
							} catch (InterruptedException ex) {
								Logger.getLogger(Modem.class.getName()).log(Level.SEVERE, null, ex);
							}
						}

						Processor.PostToTerminal(s + "\n");
						// compile some patterns and set up the matchers
						Pattern pfrm = Pattern.compile("^\\s*(From:)\\s(.*)");
						Matcher mfrm = pfrm.matcher(s);
						Pattern pdate = Pattern.compile("^\\s*(Date:)\\s(\\w{3})\\,*\\s+(\\d+)\\s(\\w{3})\\s(\\d{4})\\s(\\d\\d:\\d\\d:\\d\\d)");
						Matcher mdate = pdate.matcher(s);
						Pattern pdate2 = Pattern.compile("^\\s*(Date:)\\s(\\d+)\\s+(\\w{3})\\s+(\\d{4}\\s\\d\\d:\\d\\d:\\d\\d)");
						Matcher mdate2 = pdate2.matcher(s);                                                      
						Pattern psub = Pattern.compile("^\\s*(Subject:)\\s(.*)");
						Matcher msub = psub.matcher(s);
						Pattern p64 = Pattern.compile("^\\s*(content-transfer-encoding: base64)");
						Matcher m64 = p64.matcher(s.toLowerCase());
						Pattern c64 = Pattern.compile("^\\s*(content-type)");
						Matcher cc64 = c64.matcher(s.toLowerCase());
						Pattern pnm = Pattern.compile(".*(filename=)(.*)");
						Matcher mnm = pnm.matcher(s);
						Pattern xui = Pattern.compile("X-UI-ATTACHMENT");
						Matcher mxui = xui.matcher(s);
						Pattern nmx = Pattern.compile("name=");
						Matcher mnmx = nmx.matcher(s);

						if (mfrm.lookingAt()) {
							if (mfrm.group(1).equals("From:")){   
								From = mfrm.group(2);
							} 
						} else if (mdate.lookingAt()){
							if (mdate.group(1).equals("Date:")) {
								Date = mdate.group(2) + " " + mdate.group(3) + " " + 
										mdate.group(4) + " " + mdate.group(5) + " " +
										mdate.group(6);
							}

						} else if (mdate2.lookingAt()){
							if (mdate2.group(1).equals("Date:")) {
								Date = mdate2.group(2) + " " + mdate2.group(3) + " " + 
										mdate2.group(4);
							}

						} else if  (msub.lookingAt()){
							if (msub.group(1).equals("Subject:")) {
								Sub = msub.group(2);
							}

						} else if  (m64.lookingAt()){
							if (m64.group(1).equals("content-transfer-encoding: base64")) {
								// there is an attachment...
								base64attachment = true;
								//                                                                debug ("Attachment");
								outstr += s + "\n";  // write to Inbox

							}
						} else if  (cc64.lookingAt()){
							outstr += s + "\n";  // write to Inbox

						} else if  (mxui.lookingAt()){
							outstr += s + "\n";  // write to Inbox

						} else if  (mnm.lookingAt()){
							if (mnm.group(1).equals("filename=")) {
								// get the file name, remove double quotes if any
								attachmentFilename = mnm.group(2).replace("\"", "");

								outstr += s + "\n";  // write to Inbox

							}
						} else if (mnmx.lookingAt()) {
							outstr += s + "\n";  // write to Inbox
						} else {


							if (base64attachment) {
								outstr += s + "\n";  // write to Inbox

								if (!s.equals("")) {
									if (!s.startsWith("--")){
										attachment += s + "\n";
										//     System.out.println(s);
									} else {
										base64attachment = false;  
										if (attachment.length() > 10 & attachmentFilename != null){
											// is it a grib file?
											if (attachmentFilename.endsWith(".grb")) {
												// are we on linux?
												if (Processor.Separator.equals("/")) {
													try {
														File f1 = new File("/opt/zyGrib/grib/");
														// is zygrib installed?
														if (f1.isDirectory()) {
															attachmentFilename ="/opt/zyGrib/grib/" + attachmentFilename ;
														} else {
															// no, put it in the Files directory
															File myfiles = new File ("Files");
															if (!myfiles.isDirectory()) {
																myfiles.mkdir();
															}

															attachmentFilename = "Files/" + attachmentFilename;
															attachmentFilename = Processor.HomePath + Processor.Dirprefix + attachmentFilename;

														}   
													}
													catch (Exception e) {
														a.Message("IO problem", 10);
													}
												} 
											} else {
												// no grib file
												File myfiles = new File ("Files");
												if (!myfiles.isDirectory()) {
													myfiles.mkdir();
												}

												attachmentFilename = "Files/" + attachmentFilename;
												attachmentFilename = Processor.HomePath + Processor.Dirprefix + attachmentFilename;
											}
											try {
												File myfiles = new File (Processor.HomePath + Processor.Dirprefix + "Files");
												if (!myfiles.isDirectory()) {
													myfiles.mkdir();
												}
												// remove first line if "X-Attachment..."
												String[] attlines = attachment.split("\n");
												if (attlines[0].startsWith("X-Attachment")) {
													attachment = "";
													for (int i = 1; i < attlines.length; i++) {
														attachment += attlines[i] + "\n";
													}
												}
												//loggingclass.writelog("Compressed: >"+attachmentFilename+"< length() = " + Integer.toString(attachmentFilename.length()), null, true);

												boolean success = Base64.decodeToFile(attachment, attachmentFilename);
												a.Message ("File stored in " + attachmentFilename, 10);
												Processor.PostToTerminal("\n File stored in " + attachmentFilename + "\n");

											}
											catch (Exception e){
												a.Message("Problem with decoding, "+ e, 10);
											}

										}
									}
								}
							} else {
								outstr += s + "\n";  // write to Inbox
							}
						}

					} // end while

					fr.close();
					this.inbox = new FileWriter(Processor.HomePath + Processor.Dirprefix + "Inbox", true);
					inbox.write("From " + From + " " + Date + "\n");
					inbox.write("From: " + From + "\n");
					if (Date != null)   {
						inbox.write("Date: " +Date + "\n");
					}
					inbox.write("Subject: " + Sub + "\n");
					// write message body
					if  (outstr != null) {
						inbox.write(outstr + "\n");
					}
					inbox.flush();
					inbox.close();

					File fl = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage");
					if (fl.exists()) {
						fl.delete();
					}



					File pending = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);
					if (pending.exists()) {
						pending.delete();
					}

					if (Processor.protocol > 0) {
						Processor.TX_Text += "~FA:" + Transaction + "\n";
					}
					Processor.Progress = 0;

					a.Message("Added to mbox queue", 10);
				}
				// compressed web pages download
				if (CwwwDownload) {
					CwwwDownload = false;
					Processor.comp = false;

					try {
						this.dlFile.close();
					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the download file.", ex, true);
					}



					try {
						try {
							Base64.decodeFileToFile(Processor.HomePath + Processor.Dirprefix + "TempFile", Processor.HomePath + Processor.Dirprefix + "TMP.gz");
						}
						catch (Exception ex) {
							loggingclass.writelog("Error when trying to B64-decode the download file.", ex, true);
						}
						try {
							Unzip.Unzip(Processor.HomePath + Processor.Dirprefix + "TMP.gz");
						}
						catch (Exception exz) {
							loggingclass.writelog("Error when trying to unzip the download file.", exz, true);
						}

						try {
							BufferedReader in = new BufferedReader(new FileReader(Processor.HomePath + Processor.Dirprefix + "TMP"));
							String str2;
							while ((str2 = in.readLine()) != null) {
								Processor.PostToTerminal(str2 + "\n");
							}
							in.close();
						} catch (IOException e) {
							a.Message("problem decoding B64 file", 10);
						}
						File tmp1 = new File (Processor.HomePath + Processor.Dirprefix + "TMP");

						if (tmp1.exists()){
							tmp1.delete();
						}

						File tmp = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);
						if (tmp.exists()){
							tmp.delete();
						}
						if (Processor.protocol > 0){
							Processor.TX_Text += "~FA:" + Transaction + "\n";

						}
						Processor.Progress = 0;

					}
					catch (Exception exc){
						loggingclass.writelog("Error handling the download file.", exc, true);
					}
					catch (NoClassDefFoundError exp) {
						a.Message("problem decoding B64 file", 10);
					}
					File tmp = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
					boolean success = tmp.delete();

					try {
						if (pFile != null) {
							Processor.sm.pFile.close();
							Processor.sm.Trfile.delete();
						}
					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the pending file.", ex, true);
					}
					Processor.Progress = 0;

				}

				// web pages   download                                      
				if (WWWDownload) {
					WWWDownload = false;
				}
				a.Message("done...", 10);
				Processor.Progress = 0;
				Transaction = "";

			} else if (me.group(1).equals("-abort-")) {
				if (Headers) {
					Headers = false;
					DataReceived = 0;
					Processor.DataSize = Integer.toString(0);

					/*                                                try {
                                                        this.headers.close();
                                                        Main.mainui.refreshHeaders();
                                                 } catch (IOException ex) {
                                                      Main.log.writelog("Error when trying to close the headers file.", ex, true);
                                                 }
					 */
				}

				if (FileList) {
					FileList = false;
				}

				if (FileDownload){
					FileDownload = false;
					Processor.comp = false;

					try {
						this.dlFile.close();
						File df = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
						if (df.exists()) {
							boolean scs = df.delete();
						}

					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the download file.", ex, true);
					}

					try {
						File tmp = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);
						if (tmp.exists()){
							tmp.delete();
						}

						Processor.TX_Text += "~FA:" + Transaction + "\n";

						Processor.Progress = 0;

					}
					catch (Exception exc){
						loggingclass.writelog("Error when trying to decode the downoad file.", exc, true);
					}
					catch (NoClassDefFoundError exp) {
						a.Message("problem decoding B64 file", 10);
					}
					File tmp = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
					boolean success = tmp.delete();

					try {
						if (pFile != null) {
							Processor.sm.pFile.close();
							Processor.sm.Trfile.delete();
						}
					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the pending file.", ex, true);
					}
					Processor.Progress = 0;

					//                                                    Main.log(ThisFile + " received");

				}
				// messages  download      - append tmpmessage to Inbox in mbox format
				if (MsgDownload) {
					MsgDownload = false;
					this.tmpmessage.close();
					// append to Inbox file
					FileReader fr = new FileReader("tmpmessage");

					File fl = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage");
					if (fl.exists()) {
						fl.delete();
					}

				}
				// compressed messages  download      - append tmpmessage to Inbox in mbox format
				if (CMsgDownload) {
					CMsgDownload = false;
					Processor.comp = false;
					tmpmessage.close();
					// append to Inbox file
					File fl = new File(Processor.HomePath + Processor.Dirprefix + "tmpmessage");
					if (fl.exists()) {
						fl.delete();
					}

					File pending = new File (Processor.HomePath + Processor.Dirprefix + "Pending" + Processor.Separator + Transaction);
					if (pending.exists()) {
						pending.delete();
					}
				}
				// compressed web pages download
				if (CwwwDownload) {
					CwwwDownload = false;
					Processor.comp = false;

					try {
						this.dlFile.close();
					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the download file.", ex, true);
					}



					File tmp = new File(Processor.HomePath + Processor.Dirprefix + "TempFile");
					boolean success = tmp.delete();

					try {
						if (pFile != null) {
							Processor.sm.pFile.close();
							Processor.sm.Trfile.delete();
						}
					} catch (IOException ex) {
						loggingclass.writelog("Error when trying to close the pending file.", ex, true);
					}
					Processor.Progress = 0;
				}

				// web pages   download
				if (WWWDownload) {
					WWWDownload = false;
				}
				a.Message("done...", 10);
				Processor.Progress = 0;
				Transaction = "";
			}
		}    
		// NNNN 
		// fleetcodes
		/*JD not yet
                                    if (!Main.Connected & Main.IACmode)    {          
                                        Pattern pn = Pattern.compile("<SOH>(NNNN)");
                                        Matcher mn= pn.matcher(str);
                                        if (mn.lookingAt()) {
                                            if (mn.group(1).equals("NNNN")){   
                                                Main.IACmode = false;                                   
                                                a.Message("End of code...", 10);
                                                Main.Status = "Listening";
                                                try {
                                                     iacout.close();
                                                }
                                                catch (Exception e) {
                                                    Main.log.writelog("Error closing the iac file.", e, true);
                                                }
                                                f.fastfec2(Main.HomePath + Main.Dirprefix + "iactemp", "");
                                                deleteFile("iactemp");
                                            }  
                                         } 
                                    }
		 */
		// bulletin                               
		if (!Processor.Connected & Processor.Bulletinmode)    {          
			Pattern pn = Pattern.compile("<SOH>(NNNN)");
			Matcher mn= pn.matcher(str);
			if (mn.lookingAt()) {
				if (mn.group(1).equals("NNNN")){   
					Processor.Bulletinmode = false;
					a.Message("End of bulletin...", 2);
				}
			}
		}


		// write headers
		if (Headers & !Firstline) {
			Pattern phd = Pattern.compile("^(\\s*\\d+.*)");
			Matcher mhd = phd.matcher(str);
			if (mhd.lookingAt()) {
				String outToWindow = mhd.group(1) + "\n";
				Processor.Mailheaderswindow += outToWindow;
				DataReceived += outToWindow.length();
				if (DataSize > 0) {
					Processor.Progress =  100 * DataReceived / DataSize ;
				}
				try {
					this.headers.write(str + "\n");                                                 
				} catch (IOException ex) {
					loggingclass.writelog("Error when trying to write to headers file.", ex, true);
				}
			}
		}
		// files list
		if ( FileList & !Firstline) {
			Processor.FilesTextArea += str + "\n";
			DataReceived += str.length();
			Processor.Progress = 100 * DataReceived / DataSize ;
		}
		// write file
		if (FileDownload & !Firstline) {
			Processor.FilesTextArea += str + "\n";
			DataReceived += str.length();

			if (DataSize > 0) {
				Processor.Progress =  100 * DataReceived / DataSize ;
			}

			try {
				if (pFile != null) {
					pFile.write(str + "\n");
					pFile.flush();
				}
			} catch (IOException ex) {
				//                                                Main.log.writelog("Error when trying to write to pending file.", ex, true);
			}
			try {
				this.dlFile.write(str + "\n");                                                 
			} catch (IOException ex) {
				//                                                Main.log.writelog("Error when trying to write to download file.", ex, true);
			}
		}
		// messages                                    
		if (MsgDownload & !Firstline) {
			DataReceived += str.length();
			Processor.Progress = 100 * DataReceived / DataSize ;
			this.tmpmessage.write(str + "\n");
		}

		// compressed messages                                    
		if (CMsgDownload & !Firstline) {
			DataReceived += str.length();
			Processor.Progress = 100 * DataReceived / DataSize ;

			try {
				if (pFile != null) {
					pFile.write(str + "\n");
					pFile.flush();
				}
			} catch (IOException ex) {
				//                                             Main.log.writelog("Error when trying to write to pending file.", ex, true);
				a.Message("Error writing pending file.",1);
			}
			try {
				tmpmessage.write(str + "\n");
				tmpmessage.flush();
			} catch (IOException ex) {
				loggingclass.writelog("Error when trying to write to tmpmessage file.", ex, true);
			}
		}
		// compressed www pages
		if (CwwwDownload & !Firstline) {
			DataReceived += str.length();
			Processor.Progress = 100 * DataReceived / DataSize ;
			try {
				if (pFile != null & str.length() > 0) {
					pFile.write(str + "\n");
					pFile.flush();
				}
			} catch (IOException ex) {
				//                                               Main.log.writelog("Error when trying to write to pending file.", ex, true);
			}
			try {
				if (str.length() > 0){
					dlFile.write(str + "\n");
					dlFile.flush();
				}
			} catch (IOException ex) {
				loggingclass.writelog("Error when trying to write to tmpmessage file.", ex, true);
			}
		}

		// www pages       WWWDownload     
		if (WWWDownload & !Firstline) {
			DataReceived += str.length();
			//                                        double ProGress = 100 * DataReceived / DataSize;
			if (DataSize > 0) {
				Processor.Progress = 100 * DataReceived / DataSize ;
			}
		}
		// iac fleetcode file     
		//                                    debug (Integer.toString(str.length()));

		if (!Processor.Connected & Processor.IACmode & str.length() > 0) {
			try {
				iacout.write(str + "\n");
				iacout.flush();
				Processor.PostToTerminal(str + "\n");
			}
			catch (IOException exc){
				loggingclass.writelog("Error when trying to write to download file.", exc, true);
			}
		}                                    
	}
	public boolean TransactionsExists() {
		boolean result = false;
		String Tr = Processor.HomePath + Processor.Dirprefix + "Transactions";
		File Transactions = new File (Tr);
		if (Transactions.exists()) {
			result = true;
		}
		return result;
	}

	//Reset private variable Blocklength to middle value of 5 (32 bytes block size). Used when changing modes up and down.
	public void SetBlocklength(int NewBlocklength) {
		Blocklength = NewBlocklength;
	}

	public String doTXbuffer(){

		String Outbuffer = "";
		int nr_missing = tx_missing.length();
		int b[] = new int[nr_missing];  // array of missing blocks
		int i;
		for (i = 0; i < nr_missing; i++) {
			b[i] = (int)tx_missing.substring(i, i+1).charAt(0) - 32;
		}  
		if (tx_missing.length() > 2) {
			//With adaptive modes, blocksize of 8 is not effective. Minimum of 16 (2 ** 4)
			//  if (Blocklength > 3) {
			if (Blocklength > 4) {
				Blocklength--;
			}
		} else if (tx_missing.length() < 1) {
			if (Blocklength < 6) {
				Blocklength++;
			}
		} else  {
			Blocklength = 5;
		}


		// add missing blocks
		if (nr_missing > 0){
			for (i = 0; i < nr_missing; i++){
				String block = TX_addblock(b[i]) ;
				Outbuffer += block;
			}
		}

		i = 0;
		//    Blocklength = Integer.parseInt(blocklength);
		while (i < (8 - nr_missing) & Processor.TX_Text.length() > 0){
			String newstring = "";
			if (Blocklength < 4) {
				Blocklength = 4;
			} else if (Blocklength > 6) {
				Blocklength = 6;
			}
			double bl  = Math.pow(2, Blocklength);
			int queuelen = Processor.TX_Text.length();

			if (queuelen > 0){
				if (queuelen <= (int) bl) {
					newstring = Processor.TX_Text;
					Processor.TX_Text = "";
				} else {
					newstring = Processor.TX_Text.substring(0,(int)bl );
					Processor.TX_Text = Processor.TX_Text.substring((int)bl);
				}

				//            lastqueued += 1;
				//            if (lastqueued > 63) {
				//                lastqueued = 0;
				//            }

				txbuffer[lastqueued] = newstring;

				for (int j = lastqueued + 17; j < lastqueued + 25; j++) {
					txbuffer[j % 64] = "";
				}
			}

			String block = TX_addblock(lastqueued);


			char lasttxchar = (char) (lastqueued + 32);
			rx_lastsent = Character.toString(lasttxchar);
			Processor.myrxstatus = getTXStatus();
			Outbuffer += block;
			i++;
			lastqueued += 1;
			if (lastqueued > 63) {
				lastqueued = 0;
			}       
		}

		return Outbuffer;
	}

	public String TX_addblock(int nr){      
		if (txbuffer[nr].length() > 0) {
			char c = (char) (nr + 32);
			String accum =  "0";
			accum += Processor.session ;
			accum += Character.toString(c );      
			accum += Session.txbuffer[nr];     
			String blcheck = a.checksum(accum);
			accum += blcheck;
			return accum + (char) 1 ;
		} else {
			return "";
		}
	}

	void sendQTC(String mailnr) {
		Processor.TX_Text += "~QTC " + mailnr + "+\n";   
	}
	void sendDelete(String numbers){
		Processor.TX_Text += "~DELETE " + numbers + "\n";
	}

	void sendRead(String mailnr) {
		if (Processor.compressedmail)  {
			Processor.TX_Text += "~READZIP " + mailnr + "\n"; 
		} else {
			Processor.TX_Text += "~READ " + mailnr + "\n";   
		}
	}

	public void makeFile(String filename) {
		String fileName = Processor.HomePath + Processor.Dirprefix + filename;
		File fl = new File(fileName);
		try {
			boolean success = fl.createNewFile();
		}
		catch (IOException e) {
			loggingclass.writelog("Error creating headers file:", e, true);
		} 
	}

	public void deleteFile(String filename){ 
		String fileName = Processor.HomePath + Processor.Dirprefix + filename;
		// A File object to represent the filename
		File fl = new File(fileName);

		// Make sure the file or directory exists and isn't write protected
		try {
			if (!fl.exists()){
				throw new IllegalArgumentException("Delete: Does not exist: "
						+ fileName);
			}


			if (!fl.canWrite())
				throw new IllegalArgumentException("Delete: write protected: "
						+ fileName);

			// If it is a directory, make sure it is empty
			if (fl.isDirectory()) {
				String[] files = fl.list();
				if (files.length > 0)
					throw new IllegalArgumentException(
							"Delete: directory not empty: " + fileName);
			}

			// Attempt to delete it
			boolean success = fl.delete();

			if (!success)
				throw new IllegalArgumentException("Delete: deletion failed");       
		}

		catch (IllegalArgumentException e) {
			loggingclass.writelog("Error deleting headers file:", e, true);
		}

	}
	// read last header number from headers file    
	public String getHeaderCount(String filename) {
		FileReader hdr = null;
		String Countstr = "0";
		File fh = new File(filename);

		if (!fh.exists()){
			return "1";
		}
		try{  
			hdr = new FileReader(fh);
			BufferedReader br = new BufferedReader(hdr);
			String s;
			while((s = br.readLine()) != null) {
				//===================================
				Pattern ph = Pattern.compile("^\\s*(\\d+)");
				Matcher mh = ph.matcher(s);
				if (mh.lookingAt()) {
					Countstr = mh.group(1);
					int Count = Integer.parseInt(Countstr);
					Count++;
					Countstr = Integer.toString(Count);

				}                   
				//=====================================                        
			}
			br.close(); 
		}
		catch (IOException e){
			loggingclass.writelog("Error when trying to read the headers file.", e, true);
		}     
		return Countstr;
	}

	private String convert_to_aprsformat(String Tag, String latstring, String lonstring, String statustxt, String Icon ){
		String returnframe="";
		try
		{
			float latnum = 0;
			float lonnum = 0;
			String latsign = "N";
			String lonsign = "E";
			String course="0";
			String speed="0";

			String callsign = AndPskmail.myconfig.getPreference("CALL");
			statustxt = AndPskmail.myconfig.getPreference("STATUS");

			latnum = Float.parseFloat(latstring);
			lonnum = Float.parseFloat(lonstring);
			if (latnum < 0){
				latnum = Math.abs(latnum);
				latsign = "S";
			}
			if (lonnum < 0){
				lonnum = Math.abs(lonnum);
				lonsign = "W";
			}

			DecimalFormat twoPlaces = new DecimalFormat("##0.00");
			int latint = (int)latnum;
			int lonint = (int)lonnum;

			latnum = ((latnum - latint) * 60 ) + latint * 100;
			latstring = twoPlaces.format(latnum);
			latstring = "0000" + latstring;
			int len = latstring.length();
			if (len > 6) {
				latstring = latstring.substring(len -7 , len);
			}

			// Make sure there is a period in there
			latstring = latstring.replace(",", ".");

			lonnum = ((lonnum -  lonint) * 60) + lonint * 100;
			lonstring = twoPlaces.format(lonnum);
			lonstring = "00000" + lonstring;

			len = lonstring.length();
			if (len > 7){
				lonstring = lonstring.substring(len - 8, len);
			}

			//make sure we have a period there
			lonstring = lonstring.replace(",", ".");
			//       System.out.println("|" + Icon + "|") ;
			if (Icon.equals("P90")) {
				Icon = "y";
			} else if (Icon.equals("P14")){
				Icon = "-";
			} else if (Icon.equals("S07")){
				Icon = "&";
			} else if (Icon.equals("P31")){
				Icon = ">";
			} else if (Icon.equals("P42")){
				Icon = "I";
			} else if (Icon.equals("P87")){
				Icon = "v";
			} else if (Icon.equals("P83")){
				Icon = "r";
			} else if (Icon.equals("P82")){
				Icon = "s";
			} else if (Icon.equals("P56")){
				Icon = "Y";
			} else if (Icon.equals("P52")){
				Icon = "U";
			} else if (Icon.equals("P26")){
				Icon = ";";
			} else if (Icon.equals("P60")){
				Icon = "[";
			} else if (Icon.equals("P78")){
				Icon = "o";
			} else if (Icon.equals("P02")){
				Icon = "#";
			} else if (Icon.equals("S04")){
				Icon = "#";
			} else if (Icon.equals("P86")){
				Icon = "k";
			} else if (Icon.equals("P87")){
				Icon = "k";
			} else if (Icon.equals("P64")){
				Icon = "_";
			} else if (Icon.equals("S87")){
				Icon = "n";
			} else if (Icon.equals("P13")){
				Icon = ",";
			} else {
				Icon = "D";
			}







			// Fix stream id, this is wrong

			returnframe = Tag + ">PSKAPR,qAs," + callsign + "*:!" + latstring + latsign + "/" + lonstring + lonsign + Icon;

			return returnframe;
			//           }
		}
		catch(Exception ex)
		{
			loggingclass.writelog("Error when creating beaconblock", ex, true);
		}

		return returnframe;
	}

	public static float Round(float Rval, int Rpl) {
		float p = (float)Math.pow(10,Rpl);
		Rval = Rval * p;
		float tmp = Math.round(Rval);
		return tmp/p;
	}



	public void debug(String message){
		loggingclass.writelog("Debug: " + message , null, true);
		System.out.println("Debug:" + message);
	}

} // end of class


