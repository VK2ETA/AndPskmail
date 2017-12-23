/*
 * config.java  
 *   
 * Copyright (C) 2008 Per Crusefalk (SM0RWO)
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author per
 */
public class config {
    // Private variables
    // options dialogue
    private String callsign;
    private String linktoserver;
    private String blocklength;
    private String latitude;
    private String longitude;
    private String course;
    private String speed;
    private String beaconqrg;
    private boolean beacon;
    private boolean autolink;

    private int txdelay;

    //JD add modem volume and audio frequency + initial TX and RX modes
    //Compiler error???
    @SuppressWarnings("unused")
    private int volume;
    @SuppressWarnings("unused")
    private int afrequency;
    private char TxMode;
    private char RxMode;

    private String Webpage1 = "none";
    private String Webpage1b = "";
    private String Webpage1e = "";
    private String Webpage2 = "none";
    private String Webpage2b = "";
    private String Webpage2e = "";
    private String Webpage3 = "none";
    private String Webpage3b = "";
    private String Webpage3e = "";
    private String Webpage4 = "none";
    private String Webpage4b = "";
    private String Webpage4e = "";
    private String Webpage5 = "none";
    private String Webpage5b = "";
    private String Webpage5e = "";
    private String Webpage6 = "none";
    private String Webpage6b = "";
    private String Webpage6e = "";

    // APRS Server
    //VK2ETA not now
//    private String APRSServer = "yes";
//    private String APRSServerPort = "8063";

    // various values
    private String statustxt;

    @SuppressWarnings("unused")
    private String filepath;

    // Common properties file object
    Properties configFile;

    //enums for the config file, use these instead of hardcoded strings, less confusion for all then!
    public enum user {CALL, SERVER, BLOCKLENGTH, LATITUDE, LONGITUDE, BEACON, BEACONQRG, AUTOLINK, ICON, STATUS}
    public enum email {POPHOST, POPUSER, POPPASS, RETURNADDRESS, COMPRESSED}
    public enum configuration {LOGFILE, DCD, RETRIES, IDLETIME, TXDELAY, OFFSETMINUTE, OFFSETSECONDS, VOLUME, AFREQUENCY, TXMODE, RXMODE}
    //    public enum devices {GPSD, GPSPORT, GPSSPEED, GPSENABLED, APRSSERVER, APRSSERVERPORT}
//    public enum rigctrl {RIGCTL, SCANNER, RIGOFFSET, QRG0, QRG1, QRG2, QRG3, QRG4}
//    public enum state {yes, no}
    public enum web {URL1, URL1B, URL1E, URL2, URL2B, URL2E, URL3, URL3B, URL3E, URL4, URL4B, URL4E, URL5, URL5B, URL5E, URL6, URL6B, URL6E}
//    public enum modem {MODEMIP, MODEMIPPORT, MODEMPOSTAMBLE, MODEMPREAMBLE}

    // constructor
    /**
     *
     */
    public config(String path){

        filepath = path;
        configFile = new Properties();
        // Check the config file, create and fill if necessary
//JD no need for that with Android Preferences        initialcheckconfigfile();
        try {
            statustxt = getPreference(user.STATUS.toString());
            callsign = getPreference(user.CALL.toString(), "NOCALL");
            linktoserver =getPreference(user.SERVER.toString(), "NOCALL");
            latitude = getPreference(user.LATITUDE.toString());
            longitude = getPreference(user.LONGITUDE.toString());
            blocklength = getPreference(user.BLOCKLENGTH.toString(), "5");
            Processor.CurrentModemProfile = blocklength;
//            String strvolume = getPreference(configuration.VOLUME.toString(),"8");
            volume = Integer.parseInt(getPreference(configuration.VOLUME.toString(), "8"));
            afrequency = Integer.parseInt(getPreference(configuration.AFREQUENCY.toString(), "1000"));
            TxMode = (getPreference(configuration.TXMODE.toString(),"3")).charAt(0); //PSK250
            RxMode = (getPreference(configuration.RXMODE.toString(),"3")).charAt(0); //PSK250
            txdelay = Integer.parseInt(getPreference(configuration.TXDELAY.toString(),"0"));

            beaconqrg = getPreference(user.BEACONQRG.toString(),"0");
            beacon = getPreferenceB(user.BEACON.toString(),false);
            autolink = getPreferenceB(user.AUTOLINK.toString(),false);
            Webpage1 = getPreference(web.URL1.toString());
            Webpage1b = getPreference(web.URL1B.toString());
            Webpage1e = getPreference(web.URL1E.toString());
            Webpage2 = getPreference(web.URL2.toString());
            Webpage2b = getPreference(web.URL2B.toString());
            Webpage2e = getPreference(web.URL2E.toString());
            Webpage3 = getPreference(web.URL3.toString());
            Webpage3b = getPreference(web.URL3B.toString());
            Webpage3e = getPreference(web.URL3E.toString());
            Webpage4 = getPreference(web.URL4.toString());
            Webpage4b = getPreference(web.URL4B.toString());
            Webpage4e = getPreference(web.URL4E.toString());
            Webpage5 = getPreference(web.URL5.toString());
            Webpage5b = getPreference(web.URL5B.toString());
            Webpage5e = getPreference(web.URL5E.toString());
            Webpage6 = getPreference(web.URL6.toString());
            Webpage6b = getPreference(web.URL6B.toString());
            Webpage6e = getPreference(web.URL6E.toString());
//            APRSServer = getPreference(devices.APRSSERVER.toString(),state.yes.toString());
//            APRSServerPort = getPreference(devices.APRSSERVERPORT.toString(),"8063");
        }
        catch (Exception e) {
            callsign = "N0CALL";
            linktoserver = "N0CALL";
            blocklength = "6";
            latitude = "0.0";
            longitude = "0.0";
            beaconqrg = "0";
            beacon = false;
            autolink = false;
            statustxt = " ";
            Webpage1 = "none";
            Webpage2 = "none";
            Webpage3 = "none";
            Webpage4 = "none";
            Webpage5 = "none";
            Webpage6 = "none";
//            APRSServer = state.yes.toString();
//            APRSServerPort = "8063";
        }
    }

    /**
     * If there is no config file then one should be created,
     * it should have good defaults.
     private void initialcheckconfigfile(){

     try {
     // Check if there is a configuration file
     boolean exists = (new File(filepath + "configuration.xml")).exists();
     //      System.out.println(filepath + "configuration.xml");
     // There is no file, we must create one
     if (!exists) {
     OutputStream fo = new FileOutputStream(filepath + "configuration.xml");
     configFile.setProperty(user.CALL.toString(),"N0CAL");
     configFile.setProperty(user.SERVER.toString() , "N0CAL");
     configFile.setProperty(user.BLOCKLENGTH.toString() , "7");
     configFile.setProperty(user.LATITUDE.toString() , "0.0");
     configFile.setProperty(user.LONGITUDE.toString() , "0.0");
     configFile.setProperty(user.BEACONQRG.toString() , "0");
     configFile.setProperty(user.BEACON.toString() , "1");
     configFile.setProperty(user.AUTOLINK.toString() , "1");
     configFile.setProperty(user.STATUS.toString() , Processor.application);
     // Gps defaults
     configFile.setProperty(devices.GPSPORT.toString(), "/dev/ttyS0");
     configFile.setProperty(devices.GPSSPEED.toString(), "4800");
     configFile.setProperty(devices.GPSENABLED.toString(), state.no.toString());
     // ICON and DCD
     configFile.setProperty(configuration.DCD.toString(), "3");
     configFile.setProperty(user.ICON.toString(), "y");
     // Mail options
     configFile.setProperty(email.POPHOST.toString(), "none");
     configFile.setProperty(email.POPUSER.toString(), "none");
     configFile.setProperty(email.POPPASS.toString(), "none");
     configFile.setProperty(email.RETURNADDRESS.toString(), "myself@myemail.com");

     configFile.setProperty(web.URL1.toString(), "none");
     configFile.setProperty(web.URL1B.toString(), "");
     configFile.setProperty(web.URL1E.toString(), "");
     configFile.setProperty(web.URL2.toString(), "none");
     configFile.setProperty(web.URL2B.toString(), "");
     configFile.setProperty(web.URL2E.toString(), "");
     configFile.setProperty(web.URL3.toString(), "none");
     configFile.setProperty(web.URL3B.toString(), "");
     configFile.setProperty(web.URL3E.toString(), "");
     configFile.setProperty(web.URL4.toString(), "none");
     configFile.setProperty(web.URL4B.toString(), "");
     configFile.setProperty(web.URL4E.toString(), "");
     configFile.setProperty(web.URL5.toString(), "none");
     configFile.setProperty(web.URL5B.toString(), "");
     configFile.setProperty(web.URL5E.toString(), "");
     configFile.setProperty(web.URL6.toString(), "none");
     configFile.setProperty(web.URL6B.toString(), "");
     configFile.setProperty(web.URL6E.toString(), "");

     configFile.setProperty(modem.MODEMIP.toString(), "localhost");
     configFile.setProperty(modem.MODEMIPPORT.toString(), "7322");
     configFile.setProperty(modem.MODEMPOSTAMBLE.toString(), "");
     configFile.setProperty(modem.MODEMPREAMBLE.toString(), "");

     configFile.setProperty(devices.APRSSERVER.toString(), "yes");
     configFile.setProperty(devices.APRSSERVERPORT.toString(), "8063");
     configFile.storeToXML(fo,"Configuration file for JPSKmail client");
     fo.close();
     }
     }
     catch(Exception e) {
     Processor.log.writelog("Could not create settings file, directory permission trouble?", null, true);
     }
     }
     */

    /**
     *
     * @return
     */
    public String getCallsign( )    {
        return callsign;
    }

    /**
     *
     * @param newcall
     */
    public void setCallsign(String newcall)    {
        callsign = newcall;
    }

    /**
     *
     * @return
     */
    public String getServer( )    {
        return linktoserver;
    }

    /**
     *
     * @param newcall
     */
    public void setServer(String newcall)    {
        linktoserver = newcall;
        setPreference(user.SERVER.toString() , newcall);
    }

    /**
     *
     * @param newlat
     */
    public void setLatitude(String newlat)    {
        latitude = newlat;
    }

    /**
     *
     * @return
     */
    public String getLatitude( )    {
        return latitude;
    }

    /**
     *
     * @param newlon
     */
    public void setLongitude(String newlon)    {
        longitude = newlon;
    }
    /**
     *
     * @return
     */
    public String getLongitude( )    {
        return longitude;
    }

    public void setSpeed(String newspeed){
        speed = newspeed;
    }

    public String getSpeed(){
        return speed;
    }

    public void setCourse(String newcourse){
        course = newcourse;
    }

    public String getCourse(){
        return course;
    }

    //Initial TX mode 
    //JD Fix this
    public void setTxMode(String newcourse){
        //       course = newcourse;
    }

    public char getTxMode(){
        return TxMode;
    }

    //Initial RX mode 
    //JD Fix this
    public void setRxMode(String newcourse){
//        course = newcourse;
    }

    public char getRxMode(){
        return RxMode;
    }

    //Tx Delay 
    //JD Fix this
    public void setTxDelay(int newdelay){
//        course = newcourse;
    }

    public int getTxDelay(){
        return txdelay;
    }

    /**
     *
     * @param newlength
     */
    public void setBlocklength(String newlength)    {
        blocklength = newlength;
    }
    /**
     *
     * @return
     */
    public String getBlocklength( )    {
        return blocklength;
    }

    /**
     *
     * @param newqrg
     */
    public void setBeaconqrg(String newqrg)    {
        beaconqrg = newqrg;
        setPreference(user.BEACONQRG.toString(), newqrg);
    }
    /**
     *
     * @return
     */
    public String getBeaconqrg( )    {
        return beaconqrg;
    }

    public void SetBeacon(boolean newbeacon){
        beacon = newbeacon;
        setPreferenceB(user.BEACON.toString(), beacon);
    }

    public boolean getBeacon(){
        return beacon;
    }

    public void setAutolink(boolean newautolink){
        autolink = newautolink;
        setPreferenceB(user.AUTOLINK.toString(), autolink);
    }

    public boolean getAutolink(){
        return autolink;
    }

    /**
     *
     * @return
     */
    public String getStatus()    {
        return statustxt;
    }

    public void SetWebPages(String url1,String url2,String url3,String url4,String url5,String url6){
        Webpage1 = url1;
        Webpage2 = url2;
        Webpage3 = url3;
        Webpage4 = url4;
        Webpage5 = url5;
        Webpage6 = url6;
    }
    public void SetWebPagesB(String url1,String url2,String url3,String url4,String url5,String url6){
        Webpage1b = url1;
        Webpage2b = url2;
        Webpage3b = url3;
        Webpage4b = url4;
        Webpage5b = url5;
        Webpage6b = url6;
    }

    public void SetWebPagesE(String url1,String url2,String url3,String url4,String url5,String url6){
        Webpage1e = url1;
        Webpage2e = url2;
        Webpage3e = url3;
        Webpage4e = url4;
        Webpage5e = url5;
        Webpage6e = url6;
    }



    /**
     *
     * @param newstatus
     */
    public void setStatus(String newstatus) {
        statustxt = newstatus;
    }

    public void saveURLs() {
        if (Webpage1.length() > 0) {
            setPreference("URL1", Webpage1);
        }
        if (Webpage2.length() > 0) {
            setPreference("URL2", Webpage2);
        }
        if (Webpage3.length() > 0) {
            setPreference("URL3", Webpage3);
        }
        if (Webpage4.length() > 0) {
            setPreference("URL4", Webpage4);
        }
        if (Webpage5.length() > 0) {
            setPreference("URL5", Webpage5);
        }
        if (Webpage6.length() > 0) {
            setPreference("URL6", Webpage6);
        }
        if (Webpage1b.length() > 0) {
            setPreference("URL1B", Webpage1b);
        }
        if (Webpage2b.length() > 0) {
            setPreference("URL2B", Webpage2b);
        }
        if (Webpage3b.length() > 0) {
            setPreference("URL3B", Webpage3b);
        }
        if (Webpage4b.length() > 0) {
            setPreference("URL4B", Webpage4b);
        }
        if (Webpage5b.length() > 0) {
            setPreference("URL5B", Webpage5b);
        }
        if (Webpage6b.length() > 0) {
            setPreference("URL6B", Webpage6b);
        }
        if (Webpage1e.length() > 0) {
            setPreference("URL1E", Webpage1e);
        }
        if (Webpage2e.length() > 0) {
            setPreference("URL2E", Webpage2e);
        }
        if (Webpage3e.length() > 0) {
            setPreference("URL3E", Webpage3e);
        }
        if (Webpage4e.length() > 0) {
            setPreference("URL4E", Webpage4e);
        }
        if (Webpage5e.length() > 0) {
            setPreference("URL5E", Webpage5e);
        }
        if (Webpage6e.length() > 0) {
            setPreference("URL6E", Webpage6e);
        }
    }

    /** /
     * Load properties and set config object
     * @param Key
     * @param Value
     * @return
     */

/*
    public String getAPRSServer() {
        return APRSServer;
    }

    public void setAPRSServer(String APRSServer) {
        this.APRSServer = APRSServer;
    }

    public String getAPRSServerPort() {
        return APRSServerPort;
    }

    public void setAPRSServerPort(String APRSServerPort) {
        this.APRSServerPort = APRSServerPort;
    }

*/
    //Store preferences for string type
    public void setPreference(String Key, String Value) {
        try {
        	/*            InputStream f = new FileInputStream(Processor.HomePath + Processor.Dirprefix + "configuration.xml");
        	            configFile.loadFromXML(f);
        	            f.close();
        	            configFile.setProperty(Key, Value);
        	            OutputStream fo = new FileOutputStream(Processor.HomePath + Processor.Dirprefix + "configuration.xml");
        	            configFile.storeToXML(fo,"Configuration file for JPSKmail client");
        	//  System.out.println(">>Key=" + Key + " value=" + configFile.getProperty(Key) + "\n");  // debug
        	*/
        }
        catch(Exception e) {
            loggingclass.writelog("Could not store setting: "+Key, null, true);
        }
    }
    //Same for boolean type
    public void setPreferenceB(String Key, boolean Value) {
        try {
        	/*            InputStream f = new FileInputStream(Processor.HomePath + Processor.Dirprefix + "configuration.xml");
        	            configFile.loadFromXML(f);
        	            f.close();
        	            configFile.setProperty(Key, Value);
        	            OutputStream fo = new FileOutputStream(Processor.HomePath + Processor.Dirprefix + "configuration.xml");
        	            configFile.storeToXML(fo,"Configuration file for JPSKmail client");
        	//  System.out.println(">>Key=" + Key + " value=" + configFile.getProperty(Key) + "\n");  // debug
        	*/
        }
        catch(Exception e) {
            loggingclass.writelog("Could not store setting: "+Key, null, true);
        }
    }

    /**
     *
     * @param Key
     * @return
     */
    public String getPreference(String Key) {
        String myReturn="";

        try {
            myReturn = AndPskmail.mysp.getString(Key,"");
        }
        catch (Exception e) {
            myReturn = "";
        }
        return myReturn;
    }

    /**
     * Get the saved value, if its not there then use the default value
     * @param Key
     * @param Default
     * @return
     */
    public String getPreference(String Key, String Default) {
        String myReturn="";

        myReturn = AndPskmail.mysp.getString(Key,"");
        if (myReturn.equals(""))
            myReturn=Default;
        return myReturn;
    }


    /**
     *
     * @param Key
     * @return
     */
    public boolean getPreferenceB(String Key) {
        boolean myReturn = false;

        try {
            myReturn = AndPskmail.mysp.getBoolean(Key,false);
        }
        catch (Exception e) {
            myReturn = false;
        }
        return myReturn;
    }

    /**
     * Get the saved value, if its not there then use the default value
     * @param Key
     * @param Default
     * @return
     */
    public boolean getPreferenceB(String Key, boolean Default) {
        boolean myReturn=false;

        myReturn = AndPskmail.mysp.getBoolean(Key,Default);

        return myReturn;
    }



    //Backup all preferences to file
    public static boolean saveSharedPreferencesToFile(String fileName) {

        String fullFileName = Processor.HomePath + Processor.Dirprefix + fileName;
        File dst = new File(fullFileName);
        boolean res = false;
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            output.writeObject(AndPskmail.mysp.getAll());
            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }



    //Read backup preference file and restore values
    public static void loadSharedPreferencesFromFile(String fileName) {
        String fullFileName = Processor.HomePath + Processor.Dirprefix + fileName;
        final File src = new File(fullFileName);


        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
                AndPskmail.myInstance);
        myAlertDialog.setMessage("Are you sure you want to overwrite the current preferences with the ones in the backup?");
        myAlertDialog.setCancelable(false);
        myAlertDialog.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ObjectInputStream input = null;
                        SharedPreferences.Editor editor = AndPskmail.mysp.edit();
                        try {
                            editor.clear();
                            input = new ObjectInputStream(new FileInputStream(src));
                            Map<String, ?> entries = (Map<String, ?>) input.readObject();
                            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                                Object v = entry.getValue();
                                String key = entry.getKey();
                                if (v instanceof Boolean)
                                    editor.putBoolean(key, ((Boolean) v).booleanValue());
                                else if (v instanceof Float)
                                    editor.putFloat(key, ((Float) v).floatValue());
                                else if (v instanceof Integer)
                                    editor.putInt(key, ((Integer) v).intValue());
                                else if (v instanceof Long)
                                    editor.putLong(key, ((Long) v).longValue());
                                else if (v instanceof String)
                                    editor.putString(key, ((String) v));
                            }
                            editor.commit();
                        } catch (FileNotFoundException e) {
                            //e.printStackTrace();
                            AndPskmail.myInstance.topToastText("No backup file found.\n\n Use \"Save Preferences\" option first");
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }finally {
                            try {
                                if (input != null) {
                                    input.close();
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
        myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        myAlertDialog.show();
    }




    public static void restoreSettingsToDefault() {

        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
                AndPskmail.myInstance);
        myAlertDialog.setMessage("Are you sure you want to Restore the KEY settings to default? \n\n " +
                "Personal Data and non-communication critical data will be preserved");
        myAlertDialog.setCancelable(false);
        myAlertDialog.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences.Editor editor = AndPskmail.mysp.edit();


                        //General and GUI
                        editor.putString("TXMODE", "5");
                        editor.putString("RXMODE", "5");
                        editor.putBoolean("CONNECTWITHMODELIST", true);
                        editor.putString("VOLUME", "10");
                        editor.putString("AFREQUENCY", "1000");
                        editor.putBoolean("SLOWCPU", false);
                        editor.putString("TXDELAY","1800");
                        editor.putString("DCD", "0");
                        editor.putString("ICON",".");
                        editor.putBoolean("CBEACON",true);
                        editor.putBoolean("COMPRESSED",true);
                        //All modes to false first
                        for (int ii = 0; ii < modemmodeenum.values().length; ii++) {
                            editor.putBoolean("USE"+modemmodeenum.values()[ii].toString(), false);
                        }
                        //Then reset the default list to true
                        editor.putBoolean("USEPSK250",true);
                        editor.putBoolean("USEPSK250R",true);
                        editor.putBoolean("USEMFSK32",true);
                        editor.putBoolean("USETHOR22",true);
                        editor.putBoolean("USETHOR11",true);
                        editor.putBoolean("USEGPSTIME",true);

                        editor.commit();

                    }
                });
        myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        myAlertDialog.show();

    }

}
