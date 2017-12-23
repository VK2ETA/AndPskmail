/*
 * email.java
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

import java.util.Date;
import java.util.Scanner;

/**
 * This class is used to hold a single email object.
 * Feed it the raw data using the loaddata method and it will try to parse
 * the message and fill in the local variables. Use the getter methods to get
 * the message fields.
 * @author Per Crusefalk <per at crusefalk.se>
 */
public class email {
    private String from;
    private String to;
    private String cc;
    private String replyto;
    private String subject;
    private String datestr;
    private Date txdate;
    private Date rxdate;
    private String content="";
    private String rawmessage;
    private String[] attachments;
    @SuppressWarnings("unused")
    private String size;

    /**
     * This is the constructor for the email
     * @param payload This is the raw text you get after splitting an mbox file into
     * messages. Feed the whole message here.
     */
    public email(String payload){
        this.rawmessage = payload;
        loaddata();
    }

    /**
     * Internal parser for raw message data, sets the locals
     */
    private void loaddata(){
        try{
            if (this.rawmessage.length()>10){
                Scanner myscan = new Scanner(rawmessage);
                String currentline="";
                content="";
                Boolean Contentstarted = false; // True if there is an empty line, add the rest to content then
                Boolean isanoutboxfile = false; //To distinguis between inbox and outbox
                Boolean hassubjectfield = false; //To mark that we have seen the subject field
                
                while (myscan.hasNextLine()){
                    currentline = myscan.nextLine();
                    // use the currentline to set the local datafields
                    if (currentline.startsWith("~SEND")) {
                    	isanoutboxfile = true;
                    	Contentstarted = false; //reset content started as we are in an outbox file
                    }
                    if (currentline.startsWith("From:")) this.from = currentline.substring(5);
                    if (currentline.startsWith("To:")) this.to = currentline.substring(3);
                    if (currentline.startsWith("Reply-To:")) this.replyto = currentline.substring(9);
                    if (currentline.startsWith("Cc:")) this.cc = currentline.substring(3);
                    if (currentline.startsWith("Date:")) this.datestr = currentline.substring(5);
                    if (currentline.startsWith("Subject:")) {
                    	this.subject = currentline.substring(8);
                    	if (isanoutboxfile) hassubjectfield = true; //passed subject is start of content
                    }
                    if (Contentstarted) this.content += currentline +"\n"; // Add content if that has started
                    if ( (currentline.length()<1) || (isanoutboxfile && hassubjectfield)) Contentstarted = true;
                }
           }
        }
        catch(Exception ex)
        {
            // TBD: fixxa
        }
    }


    public String getRawmessage() {
        return rawmessage;
    }

    public void setRawmessage(String rawmessage) {
        this.rawmessage = rawmessage;
    }

    public String[] getAttachments() {
        return attachments;
    }

    public String getCc() {
        return cc;
    }

    public String getContent() {
        return content;
    }

    public String getDatestr() {
        return datestr;
    }

    public String getFrom() {
        return from;
    }

    public Date getRxdate() {
        return rxdate;
    }

    public String getSubject() {
        return subject;
    }

    public String getTo() {
        return to;
    }

    public String getReplyto(){
        return this.replyto;
    }

    public Date getTxdate() {
        return txdate;
    }

    public String getSize() {
        Integer storlek = rawmessage.length();
        return storlek.toString();
    }
    
}
