/*
 * MessageViewHandler.java
 *
 * Copyright (C) 2010 Per Crusefalk (SM0RWO)
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

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;



/**
 *
 * @author Per Crusefalk
 */
public class MessageViewHandler {

    private File[] files;          // Used to select another target, i.e. outbox, sent, trash etc
    private String mboxfilename;
    private File mboxfile;
    private email mymail;
    private ArrayList<email> emaillist = new ArrayList<email>(); // Used to hold all the emails

    /**
     * Constructs the messageviewhandler class
     * @param filename
     */
    public MessageViewHandler(String filename){
     // Clear the storage and set it up
        mboxfilename = filename;
    }

    public MessageViewHandler(File[] filearr){
     // Clear the storage and set it up
        files = filearr;
    }

    /**
     * Fetch the inbox mbox and process the contents
     */
    public Boolean Fetchmbox(){
        try{
            mboxfile = new File(mboxfilename);
            
            if (mboxfile == null) {
                throw new IllegalArgumentException("File should not be null.");
            }
            if (!mboxfile.exists()) {
                // File did not exist, create it
                mboxfile.createNewFile();
            }
            if (!mboxfile.isFile()) {
                throw new IllegalArgumentException("Should not be a directory: " + mboxfile);
            }

            FileReader fin = new FileReader(mboxfilename);
            Scanner src = new Scanner(fin);
            Pattern myPat = Pattern.compile("\nFrom ");
            src.useDelimiter(myPat);
            String retur="";
            while (src.hasNext()){
                retur = src.next();
                // Create an email object, add that to the list
                mymail = new email(retur);
                emaillist.add(mymail);
            }
            return true;
        }
        catch(Exception e) {
//JD Fix logging            Main.log.writelog("Could not fetch the mbox and its contents.", true);
            return false;
        }
    }

    /**
     * Outbox are a number of files in a directory, list those and create email objects
     * @return
     */
    public Boolean FetchOutbox(){
        DataInputStream in;
        BufferedReader br;
        FileInputStream fstream;
        
        try{
            if (files == null) {
                throw new IllegalArgumentException("File should not be null.");
            }

            // We should now have an array of strings containing the file names
            for (File mys : files) {
                String whole="";
                fstream = new FileInputStream(mys.toString());
                // Get the object of DataInputStream
                in = new DataInputStream(fstream);
                br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                //Read File Line By Line
                while ((strLine = br.readLine()) != null)   {
                    whole += strLine+"\n";
                }
                //Close the input stream
                in.close();
                mymail = new email(whole);
                emaillist.add(mymail);
            }
            return true;
        }
        catch(Exception e) {
            loggingclass.writelog("Could not fetch the outbox and its contents.", e, true);
            return false;
        }
    }

    /**
     * Get the raw list of emails
     * @return
     */
    public ArrayList<email> getEmaillist() {
        return emaillist;
    }

}
