/*
 * arq.java
 *
 * Copyright (C) 2008 Per Crusefalk (SM0RWO)
 * Adapted to Android by John Douyere (VK2ETA)
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

import java.text.DecimalFormat;
import java.util.regex.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.location.Location;
import android.location.LocationManager;

import static java.lang.System.currentTimeMillis;

/**
 *
 * @author Per Crusefalk <per@crusefalk.se>
 * 
 */
public class arq {

    // Frame handling variables
    char Unproto = 'u';
    char Connect = 'c';
    char Summon = 'n';
    char Status = 's';
    char Abort = 'a';
    char Acknowledge = 'k';
    String Streamid;
    char SOC = (char) 26; // Start of command
    char EOC = (char) 27; // End of command
    char NUL = (char) 0; // Null character
    char StartHeader = (char) 1;                    // <soh>, start of frame
    String FrameEnd = "" +(char) 4 + (char) 10+ "    ";     // <eot> + lf, end of frame
    char sendstring = (char) 31;                    // <us>, used instead of dcd. Sent first
    int Lastblockinframe = 0;

// Common objects

    String path = Processor.HomePath + Processor.Dirprefix;
//    config cf= new config(path); // Configuration object
    public String callsign = AndPskmail.myconfig.getCallsign();
//    public String callsign=cf.getCallsign();
    public String servercall = AndPskmail.myconfig.getServer();
    public String statustxt = AndPskmail.myconfig.getStatus();
    public String backoff = AndPskmail.myconfig.getBlocklength();
    private String summonsfreq = "0";
    
//    String Modem = "PSK500R";    // Current modem mode
    modemmodeenum mode = modemmodeenum.PSK250;
    public int CurrentTXmode = 0;

    //Counts the number of received RSID for frequency sensitive modes like MFSK16 so that we can decide to send an RSID to re-align the server's RX
    private int rxRsidCounter = 0;

    /**
     *
     * @param incall
     */
    public void setCallsign(String incall){
        callsign = incall;
    }

    /**
     *
     * @param server
     */
    public void setServer(String server) {
         servercall = server;
    }
    
    /**
     * Get the current server to link to
     * @return server callsign
     */
    public String getServer(){
        return servercall;
    }
    /**
      *
      * @param intext
      */
//     public void setTxtStatus(String intext){
//         statustxt = intext;
//     }

    /* Status enums */
    public txstatus txserverstatus;

    /** /
     * Set the tx status, extremly important this gets set the right way
     * @param tx
     */
    public void set_txstatus(txstatus tx){
        this.txserverstatus = tx;
    }

    public void Message(String msg, int time) {
        Processor.Statusline = msg;
        Processor.StatusLineTimer = time;
    }

    
    public arq(){
//     String mypath = Processor.HomePath + Processor.Dirprefix;
//     config ca = new config(mypath);
    	callsign = AndPskmail.myconfig.getCallsign();
	    //preferences
   }

   
   /* JD Fix when required
    
 public String getAPRSMessageNumber(){
        Main.APRSMessageNumber++;
        if (Main.APRSMessageNumber > 99) {
            Main.APRSMessageNumber = 0;
        }
        String outnumber = Integer.toString(Main.APRSMessageNumber);
        if (Main.APRSMessageNumber < 10) {
            outnumber = "0" + outnumber;
        }
        return "{" + outnumber;
    }
    public String  getLastAPRSMessageNumber(){
        String outnumber = Integer.toString(Main.APRSMessageNumber);
        if (Main.APRSMessageNumber < 10) {
            outnumber = "0" + outnumber;
        }
        return "{" + outnumber;
    }
*/

    /**
     *
     * @param outmessage
     */
    public void sendit(String outmessage){
    	String sendtext;

    	try{
            sendtext = outmessage;
            Processor.Sendline = Processor.ModemPreamble + sendtext + Processor.ModemPostamble;
            
            String montext = sendtext.substring(1, sendtext.length() - 6);
            while (Processor.monmutex) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
    				loggingclass.writelog("Exception in class " + Modem.class.getName() , null, true);
                    Logger.getLogger(Modem.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //JD: TO-DO Check if the display need to be is delayed until we 
            //   switch the receive off in Modem class, so that we do not 
            //   intermix with received characters
            //Processor.TXmonitor += "\n\n*TX*  " + "<SOH>" + montext + "<EOT>\n\n";
            Modem.appendToModemBuffer("\n\n*TX*  " + "<SOH>" + montext + "<EOT>\n\n");
            //Force immediate update of screen
            AndPskmail.lastUpdateTime = 0L;
            AndPskmail.mHandler.post(AndPskmail.updateModemScreen);

            //Display on APRS too if unproto TX
            if (montext.substring(4, 4).equals("u")) {
        		Processor.APRSwindow += "\n\n*TX*  " + "<SOH>" + montext + "<EOT>\n\n";
        		AndPskmail.mHandler.post(AndPskmail.addtoAPRS);
            }

            Processor.m.sendln(Processor.Sendline);
            
       }
       catch (Exception ex){
        Logger.getLogger(arq.class.getName()).log(Level.SEVERE, null, ex);
       }

    
    }

    
    /** /
     * Create UI message, used for pskaprs email among things
     * @param intext
     * @return
     */
    private String ui_messageblock(String intext){
        String returnframe="";
        returnframe = "00" +  Unproto+callsign+":25 "+intext+"\n";
        return returnframe;
    }

    private String ui_aprsblock(String intext){
	String returnframe="";
        returnframe = "0" +"0"+ Unproto + callsign + ":26 "+ intext;
        return returnframe;
    }

    private String pingblock(){
        String returnframe="";
        // Fix stream id, this is wrong
        callsign = AndPskmail.myconfig.getPreference("CALL");
        returnframe = "00"+Unproto+callsign+":7 ";
        return returnframe;
    }

    private String inquireblock(){
        String returnframe="";
        //Uses the UI dialog to set the server call
        callsign = AndPskmail.myconfig.getPreference("CALL");
//        returnframe = "00"+Unproto+callsign+":8 " + Processor.sm.myserver + " ";
        returnframe = "00"+Unproto+callsign+":8 " + AndPskmail.serverToCall.trim() + " ";
        return returnframe;
    }

    private String cqblock(){
        String returnframe="";
        // Fix stream id, this is wrong
        callsign = AndPskmail.myconfig.getPreference("CALL");
        returnframe = "00"+Unproto+callsign+":27 CQ CQ CQ PSKmail ";
        return returnframe;
    }

     private String ui_linkblock(){
        String returnframe="";
        // Fix stream id, this is wrong
        callsign = AndPskmail.myconfig.getCallsign();
        //servercall = AndPskmail.myconfig.getServer();
        servercall = AndPskmail.serverToCall;
        returnframe = "00"+Unproto+callsign+"><"+servercall+" ";
        return returnframe;
    }

     private String ui_beaconblock(String payload){
    	 String statustxt = payload;
    	 String Icon = AndPskmail.myconfig.getPreference("ICON","."); //Default is "X" marker
         String returnframe="";
         try
         {
            String latstring = "0000.00";
            String lonstring = "00000.00";
            Float latnum; // = floatValue(0);
            Float lonnum; // = (float) 0;
            String latsign = "N";
            String lonsign = "E";
            String course="0";
            String speed="0";

            callsign = AndPskmail.myconfig.getPreference("CALL");
            boolean CBeacon = AndPskmail.myconfig.getPreferenceB("CBEACON",false);
            if (statustxt.length() == 0) {
                statustxt = AndPskmail.myconfig.getPreference("STATUS");
            }

            
            // Get the GPS position data 
            Location location = null;
            if (AndPskmail.locationManager != null) {
                try {
                    location = AndPskmail.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } catch (SecurityException e) {
                    //noting for now, reports 0,0 location
                }
            }

//JD FIX: add this here            if (location != null) {

        	latnum = lonnum = (float) 0.0;

            if (location != null) {
            	latnum = (float) location.getLatitude();
            	lonnum = (float) location.getLongitude();
                //VK2ETA debug
                Long locationAge = ((System.currentTimeMillis() - location.getTime()) / 1000); //In seconds
                //AndPskmail.myInstance.topToastText("Using GPS Location that is : " + latnum + ", " + lonnum + " and is " + locationAge + " Seconds Old");
                AndPskmail.myInstance.topToastText("Using GPS Location that is" + locationAge + " Seconds Old");
            } else {
                //VK2ETA debug
                AndPskmail.myInstance.topToastText("Last Known GPS Location is not valid");
            	//JD: TO-DO prompt for location or use preferences' location
            }


            if (latnum < 0){
                latnum = Math.abs(latnum);
                latsign = "S";
            }
            if (lonnum < 0){
                lonnum = Math.abs(lonnum);
                lonsign = "W";
            }

            if (CBeacon){
                int latdegrees = (int) latnum.intValue();
                float latminutes = Round(((latnum - latdegrees) * 60), 2);
                int latminuteint = (int) latminutes;
                int latrest = (int) ((latminutes - latminuteint) * 100);
                int londegrees = (int) lonnum.intValue();
                float lonminutes = Round(((lonnum - londegrees) * 60), 2);
                int lonminuteint = (int) lonminutes;
                int lonrest = (int) ((lonminutes - lonminuteint) * 100);

                int flg = 0;
                int courseint = Integer.parseInt(course);
                int speedint = Integer.parseInt(speed);

                if (latsign.equals("N")) {
                    flg += 8;
                }
                if (lonsign.equals("E")){
                    flg += 4;
                }
                if (courseint > 179) {
                    courseint -= 180;
                    flg += 32;
                }
                courseint /= 2;
                if (speedint > 89){
                    speedint -= 90;
                    flg += 16;
                }
                if (londegrees > 89){
                    londegrees -= 90;
                    flg += 2;
                }
//JD Fix that section                if (Main.gpsdata.getFixStatus()){
//                    flg += 1;
//                }
                flg += 32;
                latdegrees += 32;
                latminuteint += 32;
                latrest += 32;
                londegrees += 32;
                lonminuteint += 32;
                lonrest += 32;
                courseint += 32;
                speedint += 32;
                int stdmsg = 0;

                Pattern pw = Pattern.compile("^\\s*(\\d+)(.*)");
                Matcher mw = pw.matcher(statustxt);
                if (mw.lookingAt()) {
                   stdmsg = Integer.parseInt(mw.group(1));
                   statustxt = mw.group(2);
                }
                stdmsg += 32;
                
                return "00"+Unproto+callsign+":6 "+ (char) flg + (char)latdegrees
                        + (char)latminuteint + (char)latrest + (char)londegrees
                        + (char)lonminuteint + (char)lonrest + (char)courseint + (char)speedint
//JD fix icon + ststustxt
                        + Icon + (char)stdmsg + statustxt;
                
            } else {
                DecimalFormat twoPlaces = new DecimalFormat("##0.00");
                int latint = (int)latnum.intValue();
                int lonint = (int)lonnum.intValue();

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

            // Fix stream id, this is wrong

                returnframe = "00"+Unproto+callsign+":26 "+"!" + latstring + latsign  + "/" +lonstring + lonsign + Icon + statustxt;

                return returnframe;
            }
         }
         catch(Exception ex)
         {
        	 loggingclass.writelog(
        			 "Error when creating beaconblock" + ex.getMessage(), null, true);
         }

         return returnframe;
     }

     public static String getmodelist() {
    	 String thatlist = "";
//    	 String strmode = "";
    	 for (int ii = 0; ii < modemmodeenum.values().length; ii++) {
 //   		 strmode = modemmodeenum.values()[ii].toString();
    		 boolean thatmode = AndPskmail.myconfig.getPreferenceB("USE"+modemmodeenum.values()[ii].toString(), false);
    		 if (thatmode) {
    			 //Add new mode at beginning since we send a list from least to most robust
    			 thatlist = Character.toString(Modem.modemmodecode[ii]) + thatlist;
    		 }
    	 }
    	 return thatlist;
     }
     
     private String connectblock(){
         String returnframe="";
        // Fix stream id, this is wrong
         callsign = AndPskmail.myconfig.getPreference("CALL");
         callsign = callsign.trim();
         //servercall = AndPskmail.myconfig.getPreference("SERVER");
         servercall = AndPskmail.serverToCall.trim();
         //Change from default mode to currently selected mode (could 
         //  have been changed by the modeUP/Down button in the Modem screen
         //         backoff = AndPskmail.myconfig.getPreference("RXMODE");
         backoff = Character.toString(Modem.modemmodecode[Processor.RxModem.ordinal()]);
         Processor.CurrentModemProfile = backoff;
         Processor.RxModemString = Modem.getModemString(Modem.getmode(AndPskmail.myconfig.getRxMode()));
         //choose connection mode: custom list or symmetric connect (no RSID receive)
         boolean modelistconnect = AndPskmail.myconfig.getPreferenceB("CONNECTWITHMODELIST",false);
    	 //Build the mode list first, falling on the default one if no mode is selected
         if (modelistconnect) {
             //List of modes (server 1.4.0 onwards) PSK63 to 500 and PSK robust for now
        	 Processor.modelist = getmodelist(); //updates Processor.modelist
        	 //Nothing selected in Preferences, use a default table
        	 if (Processor.modelist.length() == 0) Processor.modelist = "8543d1"; //PSK250, PSK250R, MFSK32, THOR22, THOR11, THOR8
        	 //Stop-Gap measure for the time being in case the RX OR TX modes
        	 //   are not in the selected list of modes
        	 //If the select RX mode is not in the list, select the slowest one in the list
        	 if (!Processor.modelist.contains(backoff)){
        		 //Advise user first
      			 loggingclass.writelog("Current RX Mode (" + Modem.getmode(backoff.charAt(0)).toString() + ") not in Mode List, using the slowest mode instead! \n", null, true);
      			 char rxmodemcode = Processor.modelist.charAt(Processor.modelist.length()-1);
      			 backoff = Character.toString(rxmodemcode);
      			 Processor.RxModem = Modem.getmode(rxmodemcode);
        	 }
        	 //If the select TX mode is not in the list, select the slowest one in the list
        	 //String txmode  = AndPskmail.myconfig.getPreference("TXMODE");
             String txmode = Character.toString(Modem.modemmodecode[Processor.TxModem.ordinal()]);
        	 if (!Processor.modelist.contains(txmode)){
        		 //Advise user first
      			 loggingclass.writelog("Current TX Mode (" + Modem.getmode(txmode.charAt(0)).toString() + ") not in Mode List, using the slowest mode instead! \n", null, true);
        		 txmode = Processor.modelist.substring(1, 1);
        		 Processor.TxModem = Modem.getmode(Processor.modelist.charAt(Processor.modelist.length()-1));
        	 }
        	 //Send connect frame
        	 returnframe = "10"+Connect+callsign+":1024 "+servercall+":24 " + backoff + Processor.modelist;
         } else {
        	 //Use symmetric mode connect (RX = TX mode and stay on that mode)
             returnframe = "10"+Connect+callsign+":1024 "+servercall+":24 " + "0";
         }
        return returnframe;
    }

     
     private String summonblock(){
         String returnframe="";
         String curfreq = "0";
        // Fix stream id, this is wrong
         callsign = AndPskmail.myconfig.getPreference("CALL");
         callsign = callsign.trim();
//         servercall = AndPskmail.myconfig.getPreference("SERVER");
         servercall = AndPskmail.serverToCall;
         servercall = servercall.trim();
         backoff = AndPskmail.myconfig.getPreference("BLOCKLENGTH");
         Processor.CurrentModemProfile = backoff;
/* not yet
         if (Rigctl.opened) {
            int fr = Integer.parseInt(Main.CurrentFreq) + Rigctl.OFF;
            curfreq = Integer.toString(fr);
         } else {
            int fr = Integer.parseInt(mainpskmailui.ClientFreqTxtfield.getText());
            curfreq = Integer.toString(fr);

         }
 */
         Processor.RxModemString = Processor.Modes[Integer.parseInt(backoff)];
         returnframe = "00"+Summon+callsign+":1024 "+servercall+":24 " + curfreq + " "+ backoff;
         return returnframe;
    }

     
     private String connect_ack(String server) {
       // <US><SOH>1/kPI4TUE:24 PA0R:1024 56663<SOH>0/s   60D3<EOT>
         Random generator = new Random();
         int randomIndex = generator.nextInt( 64 );
         // get random number between 1...63
         while (randomIndex == 0) { 
             randomIndex = generator.nextInt( 64 ) + 32;
         }
 
         char c = (char)(randomIndex + '0');
         Streamid = Character.toString(c);
         Processor.session = Streamid;
         callsign = AndPskmail.myconfig.getPreference("CALL");
         Processor.TTYConnected = "Connecting";
         return(  "1" + Streamid + Acknowledge + callsign + ":24 " + server + ":1024 5");
     }
     
    public boolean send_ack(String server) {
//        try {
            String info = "";
            String outstring = "";

            send_txrsid_command("ON");
//            Thread.sleep(1000);
            info = connect_ack(server);
            outstring = make_block(info);
/*no TTY for now
            if (Main.TTYConnected.equals("Connecting")) { //I am a TTY server
                char sprobyte = ' ';
                if (Main.UseAlttable) {
//VK2ETA debug                    sprobyte = (char) (48 + Main.MgetAltModemPos(Main.RxModem));
                    sprobyte = (char) (48 + Main.m.getAltModemPos(Main.RxModem));
                } else {
//VK2ETA debug                   sprobyte = (char) (48 + Main.MgetModemPos(Main.RxModem));
                   sprobyte = (char) (48 + Main.m.getModemPos(Main.RxModem));
                }
               String protobyte = Character.toString(sprobyte);
               info =  protobyte + Main.session + Character.toString(Status) + Main.myrxstatus;
            } else {
               info =  "0" + Main.session + Character.toString(Status) + Main.myrxstatus;
            }
*/
            info =  "0" + Processor.session + Character.toString(Status) + Processor.myrxstatus;
            outstring += make_block(info) + FrameEnd;
            Processor.TXActive = false; // force transmit
            sendit(outstring);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(arq.class.getName()).log(Level.SEVERE, null, ex);
//        }
        return true;
     }
 
     public boolean send_disconnect() {
//        try {
            String info = "";
            String outstring = "";

            send_txrsid_command("ON");
//            Thread.sleep(1000);
            info =  "0" + Streamid + "d" ;       
            outstring += make_block(info) + FrameEnd;   
            sendit(outstring);
//        } catch (InterruptedException ex) {
 //           Logger.getLogger(arq.class.getName()).log(Level.SEVERE, null, ex);
//        }
        return true;
     }
  
          private String statusblock(String status){
              String returnframe = "";
       // calculate quality byte
              int degradeFactor = 3;
              int quality = (int) (Processor.snr * 90.5 / 100) + 32;
    // make the protobyte
              char sprobyte = (char)quality;
/* no TTY for now
              if (Main.TTYConnected.equals("Connected")) { //I am a TTY server - send client's TX mode
                  if (Main.UseAlttable) {
                     sprobyte = (char) (48 + Main.m.getAltModemPos(Main.RxModem));
                  } else {
                     sprobyte = (char) (48 + Main.m.getModemPos(Main.RxModem));
                  }
              }
*/
              String protobyte = Character.toString(sprobyte);
              returnframe = protobyte + Processor.session + Character.toString(Status) + status;
              return returnframe;
          }

          private String abortblock(){
              String returnframe = "";
              if (!Processor.session.equals("")){
                returnframe = "0" + Processor.session + Character.toString(Abort) ;
              }
              return returnframe;
          }
     
     /**
      *
      * @param payload
      */
//JD FIX what is the reson for exception here?     public void send_frame(String payload) throws InterruptedException{
     public void send_frame(String payload) {
       String info="";
       String outstring="";      
       
       switch (this.txserverstatus){
  
           case TXUImessage:
                 send_txrsid_command("ON");
  //              Thread.sleep(500);
                info = ui_messageblock(payload);
                Lastblockinframe = 1;
                outstring = make_block(info) + FrameEnd;
                break;
           case TXPing:
                send_txrsid_command("ON");
               	info = pingblock();
               	Lastblockinframe = 1;
               	outstring = make_block(info) + FrameEnd;
               break;
          case TXInq:
//    System.out.println("INQ");
                send_txrsid_command("ON");
                info = inquireblock();
              	Lastblockinframe = 1;
              	outstring = make_block(info) + FrameEnd;
               break;
          case TXCQ:
               	info = cqblock();
               	Lastblockinframe = 1;
               	outstring = make_block(info) + FrameEnd;
               break;

           case TXaprsmessage:
                 send_txrsid_command("ON");
 //               Thread.sleep(1000);
                 info = ui_aprsblock(payload);
                 Lastblockinframe = 1;
                 outstring = make_block(info) + FrameEnd;
                break;
           case TXlinkreq:
                send_txrsid_command("ON");
 //               Thread.sleep(500);
//JD ??               send_mode_command(Processor.linkmode);
 //               Thread.sleep(500);
              	info = ui_linkblock();
                outstring = make_block(info) + FrameEnd;
               break;
           case TXBeacon:
               send_txrsid_command("ON");
//               Thread.sleep(500);                          
               info = ui_beaconblock(payload);
               outstring = make_block(info) + FrameEnd;
              break;
           case TXConnect:
                send_txrsid_command("ON");
//                Thread.sleep(1000);
               info = connectblock();
               outstring = make_blockWithPassword(info) + FrameEnd;
               break;
           case TXSummon:
               send_txrsid_command("ON");
        	   //JD FIX                int fr = Integer.parseInt(Main.ServerFreq) - Rigctl.OFF;
//JD FIX                Main.setFreq(Integer.toString(fr));
                send_txrsid_command("ON");
//                Thread.sleep(1000);
              //JD FIX when required                info = summonblock();
               outstring = make_block(info) + FrameEnd;
               break;
           case TXAbort:
        	  Processor.Aborting = true;
              send_txrsid_command("ON");
              send_rsid_command("ON");
              info = abortblock();
              if (!info.equals("")) {
                outstring = make_block(info) + FrameEnd;
              }
               break;           
           case TXStat:
        	   if (Processor.justReceivedRSID) rxRsidCounter++;
        	   if (rxRsidCounter > 1 && (Processor.TxModem == modemmodeenum.MFSK32 ||
        			   Processor.TxModem == modemmodeenum.MFSK64 ||
        			   Processor.TxModem == modemmodeenum.PSK125 ||
        			   Processor.TxModem == modemmodeenum.PSK125R   )) {
        		   send_txrsid_command("ON");
        		   rxRsidCounter = 0;
        	   } else if (rxRsidCounter > 0 && (
        	           Processor.TxModem == modemmodeenum.MFSK8  ||
        			   Processor.TxModem == modemmodeenum.MFSK16 ||
                       Processor.TxModem == modemmodeenum.PSK63  ||
        			   Processor.TxModem == modemmodeenum.PSK31   )) {
        		   send_txrsid_command("ON");
        		   rxRsidCounter = 0;
        	   };
        	   //In any case send RSID until we have full connect exchange so that the server can gauge it's tx delay
               if (Processor.connectingPhase) {
                   send_txrsid_command("ON");
               }
        	   info = statusblock(Processor.myrxstatus);
        	   outstring = make_block(info) + FrameEnd;
        	   break;
           case TXTraffic:
               outstring = "";
               info = payload;
               outstring = StartHeader + info;

                info = statusblock(Processor.myrxstatus);
                outstring += make_block(info) + FrameEnd;
                break;
       }
       if (!outstring.equals("")) {
           outstring = "\n" + outstring;  // add eol before each frame
            sendit(outstring);
       }
    }

    /** /
     * Send UI, unnumbered information, message
     * @param msg
     */
    public void send_uimessage(String msg){
        this.txserverstatus = txstatus.TXUImessage;
		send_frame(msg);
    }

    /** /
     * Send a simple ping, using port 7
     */
    public void send_ping() throws InterruptedException{
        this.txserverstatus = txstatus.TXPing;
        send_frame("");
    }

    /** /
     * Send an Inquire block, using port 7
     */
    public void send_inquire() throws InterruptedException{
        this.txserverstatus = txstatus.TXInq;
        send_frame("");
    }

    /**
     * Send a mode command to the modem
     */
    public void send_rsid_command(String s){

    	//VK2ETA: do nothing for now but keep refernce in case we decide to make 
    	//   RSID receive conditional in the future

    	//        String rsidstart="<cmd><rsid>";
    	//        String rsidend="</rsid></cmd>";
		//        if (!s.equals("")){
		//                Main.SendCommand += (rsidstart+s+rsidend);
		//        }       
    }    
       public void send_txrsid_command(String s){
    	   if (s.equals("ON")) {
    		   Processor.TXID = true; //let the modem sent it when required
    	   } else {
    		   Processor.TXID = false; 
    	   }
    		   
       }    
    
    /**
     *
     */
    public void send_link() throws InterruptedException{
        this.txserverstatus = txstatus.TXlinkreq;
        send_frame("");
    }

    /**
     *
     */
    public void send_beacon(String status) throws InterruptedException{
        if (!Processor.Connected) {
            this.txserverstatus = txstatus.TXBeacon;
            send_frame(status);
        }
    }
    /**
     *
     * @param msg
     */
    public void send_aprsmessage(String msg) throws InterruptedException{
        this.txserverstatus = txstatus.TXaprsmessage;
        send_frame(msg);
    }

    public void send_status(String txt) throws InterruptedException {
          this.txserverstatus = txstatus.TXStat;
        send_frame(txt);
    }
    
    public void send_abort() throws InterruptedException {
          this.txserverstatus = txstatus.TXAbort;
        send_frame("");
    }
    
    public  void send_data(String outstr) throws InterruptedException{
        this.txserverstatus = txstatus.TXTraffic;
        send_frame(outstr);
        int datalength = outstr.length();
        Session.DataReceived += datalength;
        if (Session.DataSize > 0) {
             Processor.Progress =  100 * Session.DataReceived / Session.DataSize ;
        }
    }

    public String TX_addblock(int nr){
        if (Session.txbuffer[nr].length() > 0) {
            char c = (char) nr;
            String accum = "0";
            accum += Processor.session ;
            accum += Character.toString(c);
            accum += Session.txbuffer[nr];
            String blcheck = checksum(accum);
            accum += blcheck;
          
            return make_block(accum);
        } else {
            return "";
        }
    }


    /** /
     * Adds SOH and checksum
     * e.g.: '<SOH>00jThis is data for'akj0
     * @param info
     * @return
     */
    public String make_block(String info) {
        String check="";
        if (info.length()>0) {
            check = checksum(info);
        }
        return StartHeader + info + check;
    }


    /** /
     * Adds SOH and checksum
     * e.g.: '<SOH>00jThis is data for'akj0
     * @param info
     * @return
     */
    public String make_blockWithPassword(String info) {
        String check="";
        if (info.length()>0) {
            check = checksum(info + AndPskmail.serverAccessPassword);
        }
        return StartHeader + info + check;
    }


    /*
    ############################################################
    # Checksum of header + block
    # Time + password + header + block
    ############################################################
    */
    /**
     *
     * @param intext
     * @return
     */
    public String checksum(String intext) {
    String Encrypted = "0000";

    int[] table = {
            0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
            0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
            0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
            0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
            0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
            0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
            0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
            0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
            0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
            0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
            0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
            0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
            0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
            0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
            0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
            0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
            0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
            0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
            0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
            0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
            0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
            0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
            0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
            0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
            0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
            0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
            0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
            0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
            0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
            0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
            0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
            0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040,
        };


        byte[] bytes = intext.getBytes();
        int crc = 0x0000;
        for (byte b : bytes) {
            crc = (crc >>> 8) ^ table[(crc ^ b) & 0xff];
        }

  	Encrypted += Integer.toHexString(crc).toUpperCase();
        return Encrypted.substring(Encrypted.length()-4);
    }

        public static float Round(float Rval, int Rpl) {
            float p = (float)Math.pow(10,Rpl);
            Rval = Rval * p;
            float tmp = Math.round(Rval);
            return tmp/p;
    }

 }




