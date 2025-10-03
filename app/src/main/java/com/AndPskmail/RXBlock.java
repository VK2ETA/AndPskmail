/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.AndPskmail;


import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;


/**
 *
 * @author rein
 */
public class RXBlock{
    private String store = "";
    public arq myarq;
//    public config myconfig;
    String protocol ;
    String session;
    String type;
    String payload;
    String port;
    String crc;
    boolean valid;
    boolean endblock;
//    boolean isBulletin;
    boolean lastblock;
    String inString;
    public String server;
    public String call;
    private String linkserver;
    private String linkcall;

    String mycall;
    String myserver;
    String mysession;
    
//    static config myconfig;

    String from;
    String msgtext;
    boolean direct_message;
    String serverBlocklength;
    String ack;
    public String test;    // test variable
    private static Pattern pc = Pattern.compile("ZCZC");
    private static Pattern pf = Pattern.compile("ZFZF");
    private static Pattern pn = Pattern.compile("NNNN");
    private static Pattern b72 = Pattern.compile(".*00u(.*):72 ");
    private static Pattern b71 = Pattern.compile(".*00u(.*):71 ");
    private static Pattern b26 = Pattern.compile(".*00u(.*):26\\s!.*[NS]P.*[EW]&.*");
    private static Pattern pla = Pattern.compile("(\\S+)<>(\\S+)");
    private static Pattern p = Pattern.compile("^(.*):(\\d+)\\s(\\S+)>.*::(\\S+)\\s+:(.*)$");
    private static Pattern pa = Pattern.compile("^(.*)\\{(.{2})$");
    private static Pattern pd = Pattern.compile("^(.*):(\\d+)\\s(\\S+)\\s(.*)$");
    private static Pattern pl = Pattern.compile("<SOH>(.*)");
    private static Pattern plb = Pattern.compile("(.*)\\{linebreak\\}(.*)");

    RXBlock(String inBlock) {
             protocol ="";
             session = "";
             type ="";
             serverBlocklength = "5";
             payload = "";
            port = "0";
            crc = "";
            valid = false;
            endblock = false;
            lastblock = false;
            test = "";
            server= "";
            call = "";
            from = "";
            msgtext = "";
            direct_message = false;

            myarq = new arq();
//JD use same "config file" across the board
//            String path = Processor.HomePath + Processor.Dirprefix;
//            myconfig = new config(path);
            mycall = AndPskmail.myconfig.getCallsign();
            myserver = AndPskmail.myconfig.getServer();
            try {
                test = analyze(inBlock);
            } catch (IOException ex) {
                Logger.getLogger(RXBlock.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    private String analyze(String inString) throws IOException  {

        String checkstring = "";
        String BlockCRC = "";

        if (inString.length() <= 4) {
        //    System.out.println("Nullstring in analyze");
            return "";
        }

        // check if bulletin
             Matcher mc = pc.matcher(inString);
              if (mc.find() ) {
                       Processor.Bulletinmode = true;
                       Processor.Status = "Bulletin";
//       System.out.println("RXBl:Bulletin ON");
                        return "";
              }
        // check if IAC fleetcode file
        if (!Processor.Connected) {
             Matcher mf = pf.matcher(inString);
              if (mf.find()) {
                       Processor.IACmode = true;
                       return "";
              }
        }

        //check if NNNN
             Matcher mn= pn.matcher(inString);
              if (mn.find() & Processor.Bulletinmode) {
                       Processor.Bulletinmode = false;
                       Processor.Status = "Listening";
                       Processor.PostToTerminal("\n----------\n");
                       Processor.DCD = 0;
//           System.out.println("RXBl:Bulletin OFF");
                        return "\n----------\n";
              }

        // bulletin, just print it.
        if (Processor.Bulletinmode ) {
            Matcher mpl= pl.matcher(inString);
            if (mpl.lookingAt()) {
                String OString = mpl.group(1);
                Matcher mplb = plb.matcher(OString);
                if (mplb.lookingAt()) {
                    OString = mplb.group(1) + mplb.group(2);
                }

                OString += "\n";
                Processor.PostToTerminal(OString);
                Processor.bulletin.write(OString);
                Processor.bulletin.flush();
            }

            return inString;
        }

        if (Processor.IACmode) {
            return inString;
        }

       if (inString.length() > 7) {
        store = inString.substring (5);
        protocol = store.substring(0,1);
        session = store.substring(1,2);
        type = store.substring(2,3);
       }

       // check if <EOT>
        if (store.matches("^.*<EOT>*$")){
            lastblock = true;
        } else {
           lastblock = false;
        }

       // get  CRC and payload from RX
       int storelength = 0;
       storelength = store.length();
       if (storelength > 11) {
            crc = store.substring(storelength - 9, storelength - 5);
            payload = store.substring(3,storelength-9);
       }
       // check CRC
       checkstring = protocol + session + type + payload;
       BlockCRC = checksum(checkstring);

        if (BlockCRC.equals(crc)){
           valid = true;
        }

       // unproto Blocks
       if ( type.equals( "u")) {
    	   //Display in APRS screen even if crc is bad
    	   //Processor.APRSwindow += payload + "\n";
    	   //AndPskmail.mHandler.post(AndPskmail.addtoAPRS);

           // server beacon? 00uIS0GRB-3:72
           if (inString.contains("71") | inString.contains("72") | inString.contains("26")) {
 //     System.out.println(inString) ;
               Matcher bcm = b72.matcher(inString);
               if (bcm.lookingAt()) {
                   server = bcm.group(1);
                    get_serverstat(server);
                    port = "72";
                }
               Matcher bcm2 = b71.matcher(inString);
               if(bcm2.lookingAt()){
                   server = bcm2.group(1);
                    get_serverstat(server);
                    port  = "71";
               }
                Matcher bcm3 = b26.matcher(inString);
               if(bcm3.lookingAt()){
//      System.out.println(inString) ;
                   server = bcm3.group(1);
                    get_serverstat(server);
                    port  = "72";
               }

           }
           // look if we have a link acknowledgement and set some globals if true
           if (Processor.autolink) {
               Matcher mla = pla.matcher(payload);
               if (mla.lookingAt()) {
                    linkserver = mla.group(1);
                    linkcall = mla.group(2);
                 if (linkcall.toUpperCase(Locale.US).equals(AndPskmail.myconfig.getPreference("CALL").toUpperCase(Locale.US))) {
//                        try {
                            Processor.linked = true;
                            Processor.linkedserver = linkserver;
                            Processor.sending_link = 0;
                            //Check logic here                            myarq.send_mode_command(Main.defaultmode);
                            myarq.Message("Linked to " + linkserver, 10);
                            // switch off rsid
                            //myarq.send_rsid_command("OFF");
                            //                           Thread.sleep(500);
                           //myarq.send_txrsid_command("OFF");
                            //                           Thread.sleep(500);
                            //                      } catch (InterruptedException ex) {
                            //                           Logger.getLogger(RXBlock.class.getName()).log(Level.SEVERE, null, ex);
                            //                        }
                   }
                    get_serverstat(linkserver);
                }
           }
           
          // APRS messages...
           try {             
                Matcher ma = p.matcher(payload);
              if (ma.lookingAt()) {
                  server = ma.group(1);
                  port = ma.group(2);
                  from = ma.group(3);
                  call = ma.group(4);
                  msgtext = ma.group(5);
//00uPI4TUE:26 PA0R>PSKAPR*::PA0R     :ack06
                  if (!from.equals(mycall) & port.equals("26") & msgtext.indexOf("ack") != 0 & call.equals(mycall)){
                    Matcher mb = pa.matcher(msgtext);
                    if (mb.lookingAt()) {
                            msgtext = mb.group(1);
                            ack = mb.group(2);
                            String Ackmessage = from + " ack" + ack;
                            myarq.set_txstatus(txstatus.TXaprsmessage);
                            myarq.send_aprsmessage(Ackmessage);
                        }
                  }
             } else {
                Matcher md = pd.matcher(payload);
              if (md.lookingAt()) {
                from = md.group(1);
                port = md.group(2);
                call = md.group(3);
                msgtext = md.group(4);

                if (call.equals(mycall) & valid == true & port.equals("26")) {
//                    direct_message = true;
 //                   System.out.println(from + ":" + call + ":" + msgtext);
                }
              }
             }
           }
           catch (Exception e){
               myarq.Message ("Problem in arq...", 10);
           }
      }

       return payload;
    }

    void get_serverstat(String server) {
        // get the time for MH.
        // Make calendar object
        Calendar cal = Calendar.getInstance();
        int Hour = cal.get(Calendar.HOUR_OF_DAY);
        int Minute = cal.get(Calendar.MINUTE);
        String formathour = "0" + Integer.toString(Hour);
        formathour = formathour.substring(formathour.length() - 2);
        String formatminute = "0" + Integer.toString(Minute);
        formatminute = formatminute.substring(formatminute.length() - 2);
        String lh = formathour + ":" + formatminute;
        int i;
        boolean knownserver = false;
        for (i = 0; i < 10; i++) {
            if (server.equals(Processor.Servers[i])) {
                knownserver = true;
                Processor.SNR[i] = Processor.snr;
                Processor.Lastheard[i] = lh;
                Processor.packets_received[i]++;
                break;
            }
        }
        if (!knownserver) {
            for (i = 0; i < 10; i++) {
                if (Processor.Servers[i].equals("")) {
                    Processor.Servers[i] = server;
                    Processor.SNR[i] = Processor.snr;
                    Processor.Lastheard[i] = lh;
                    Processor.packets_received[i]++;
                    //JD Not yet                                                    Main.mainui.addServer(server);
                    break;
                }
            }
        }

    }

   /*
    ############################################################
    # Checksum of header + block
    # Time + password + header + block
    ############################################################
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


        //byte[] bytes = intext.getBytes();
        byte[] bytes;
        try {
            bytes = intext.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            bytes = intext.getBytes();
        }
        int crc1 = 0x0000;
        for (byte b : bytes) {
            crc1 = (crc1 >>> 8) ^ table[(crc1 ^ b) & 0xff];
        }

  	Encrypted += Integer.toHexString(crc1).toUpperCase();
        return Encrypted.substring(Encrypted.length()-4);
}

}
