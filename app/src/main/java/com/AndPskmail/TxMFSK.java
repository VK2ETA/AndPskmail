 /*
 * TxMFSK.java
 *
 * @author Rein Couperus
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


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class TxMFSK {
	
	//VK2ETA wide mfsk mode test
	double doublespacing = 1.0;
    
    public AudioTrack at = null; 
	private double frequency;
	private int volumebits;
	private double bandwidth;
    int numtones = 32;
    static final double TWOPI = 2 * Math.PI;
    static final double M_PI = Math.PI;
    double tonespacing;
    int samplerate = 8000;
    int symlen;
    double[] outbuf;
    double phaseacc;
//    static public SoundOutput soundout;
    ViterbiEncoder Enc;
    int bitshreg;
    int bitstate;
    int symbits;
    static final int K = 7;
    static final int POLY1 = 0x6d;
    static final int POLY2 = 0x4f;
    interleave Txinlv;
    final int TX_STATE_PREAMBLE = 0;
    final int TX_STATE_START = 1;
    final int TX_STATE_DATA = 2;
    final int TX_STATE_FLUSH = 3;
    final int RX_STATE_DATA = 4;
    public boolean stopflag = false;
    int txstate = 0;
    int rxstate = 0;
    int basetone;
    static public String SendText = "";
    
    public TxMFSK(modemmodeenum mode){
        
    	//VK2ETA wide mfsk mode test
    	doublespacing = 1.0;
        
    	String frequencySTR = AndPskmail.myconfig.getPreference("AFREQUENCY","1000");
    	frequency = Integer.parseInt(frequencySTR);
		if (frequency < 500) frequency = 500;
		if (frequency > 2500) frequency = 2500;

    	volumebits = Integer.parseInt(AndPskmail.myconfig.getPreference("VOLUME","8"));
    	
        Enc = new ViterbiEncoder(K, POLY1, POLY2);
        Txinlv = new interleave (symbits, interleave.INTERLEAVE_FWD);
        outbuf = new double[symlen];
        
        switch (mode) {
		
	case MFSK8:
		samplerate = 8000;
		symlen =  1024;
		symbits =    5;
		basetone = 128;
		numtones = 32;
		break;
	case MFSK16:
		samplerate = 8000;
		symlen =  512;
		symbits =   4;
		basetone = 64;
		numtones = 16;
		break;
	case MFSK32:
		samplerate = 8000;
		symlen =  256;
		symbits =   4;
		basetone = 32;
		numtones = 16;
		break;
	case MFSK64:
		samplerate = 8000;
		symlen =  128;
		symbits =   4;
		basetone = 16;
		numtones = 16;
		break;
/*	case MFSK22:
		samplerate = 11025;
		symlen =  512;
		symbits =    4;
		basetone = 46;
		numtones = 16;
		break;
*/
	default:
		//MFSK16
		samplerate = 8000;
		symlen =  512;
		symbits =   4;
		basetone = 64;
		numtones = 16;
	}
        
        Enc = new ViterbiEncoder(K, POLY1, POLY2);
        Txinlv = new interleave (symbits, 0);
        outbuf = new double[symlen];
        tonespacing = (double) samplerate / symlen;
      //VK2ETA test: wide mfsk mode:	 bandwidth = (numtones - 1) * tonespacing;
       bandwidth = (numtones - 1) * tonespacing * doublespacing;

        tx_init();
    }
    
void tx_init()
{
	txstate = TX_STATE_PREAMBLE;
	bitstate = 0;
//        System.out.println("Change to MFSK16"); 
}
    
void sendsymbol(int sym)
{
	double f, phaseincr;
//        double[] outDbuf = new double[symlen];
      	short [] outSbuffer = new short[symlen];
//prt(sym);
//    	f = Main.Freq_offset - bandwidth / 2;
    	f = frequency - bandwidth / 2;
//prt(f);	
	sym = misc.grayencode(sym & (numtones - 1));
//	if (reverse)
//		sym = (numtones - 1) - sym;

//VK2ETA test: wide mfsk mode:	phaseincr = TWOPI * (f + sym * tonespacing) / samplerate;
//shift the frequency by double plus one half to centre between the two adjacent bins on the RX side
	phaseincr = TWOPI * (f + sym * tonespacing * doublespacing) / samplerate;
	
	for (int i = 0; i < symlen; i++) {
//		outbuf[i] = Math.cos(phaseacc);
		outSbuffer[i] = (short) ((int)(8386560.0 * Math.cos(phaseacc)) >> volumebits);
		phaseacc -= phaseincr;
		if (phaseacc > M_PI)
			phaseacc -= TWOPI;
		else if (phaseacc < M_PI)
			phaseacc += TWOPI;
	}

	//Catch the stopTX flag at this point as well
	if (!Modem.stopTX) at.write(outSbuffer, 0, symlen);

}

void sendbit(int bit)
{
	int data = Enc.encode(bit);
	for (int i = 0; i < 2; i++) {
		bitshreg = (bitshreg << 1) | ((data >> i) & 1);
		bitstate++;

		if (bitstate == symbits) {
//VK2ETA debug fix			Txinlv.bits(bitshreg);
			bitshreg = Txinlv.bits(bitshreg);
			sendsymbol(bitshreg);
			bitstate = 0;
			bitshreg = 0;
		}
	}
}

void sendchar(int c)
{	
    String code = mfskVaricode.varienc(c);
	
        while (code.length() > 0){
            if (code.charAt(0) == '1'){            
            	sendbit(1);
            } else {
                sendbit(0);
            }
            code = code.substring(1);
        }
//	put_echo_char(c);
}

void sendidle()
{
	sendchar(0);	// <NUL>
	sendbit(1);

// extended zero bit stream
	for (int i = 0; i < 32; i++)
		sendbit(0);
}

void flushtx()
{
// flush the varicode decoder at the other end
	sendbit(1);

// flush the convolutional encoder and interleaver
	for (int i = 0; i < 107; i++)
		sendbit(0);

	bitstate = 0;
}


//void mfsk::sendpic(unsigned char *data, int len)
//{
//	double *ptr;
//	double f;
//	int i, j;
//
//	ptr = outbuf;
//
//	for (i = 0; i < len; i++) {
//		if (txstate == TX_STATE_PICTURE)
//		    REQ(updateTxPic, data[i]);
//		if (reverse)
//			f = get_txfreq_woffset() - bandwidth * (data[i] - 128) / 256.0;
//		else
//			f = get_txfreq_woffset() + bandwidth * (data[i] - 128) / 256.0;
//			
//		for (j = 0; j < TXspp; j++) {
//			*ptr++ = cos(phaseacc);
//
//			phaseacc += TWOPI * f / samplerate;
//
//			if (phaseacc > M_PI)
//				phaseacc -= 2.0 * M_PI;
//		}
//	}
//
//	ModulateXmtr(outbuf, TXspp * len);
//}


void clearbits()
{
	int data = Enc.encode(0);
	for (int k = 0; k < 100; k++) {
		for (int i = 0; i < 2; i++) {
			bitshreg = (bitshreg << 1) | ((data >> i) & 1);
			bitstate++;

			if (bitstate == symbits) {
//VK2ETA Debug Fix				Txinlv.bits(bitshreg);
				bitshreg = Txinlv.bits(bitshreg);
				bitstate = 0;
				bitshreg = 0;
			}
		}
	}
}



void Txprocess(byte[] str) {
	int c;
	//Preamble
	clearbits();
	for (int i = 0; i < 32; i++)
		sendbit(0);
	//Start
//    sendchar('\r');
//    sendchar(2);		// STX
    sendchar('\r');
    //data
	for(int i = 0; i < str.length; i++) {
		c = str[i];

		//Catch the stopTX flag at this point
		if (!Modem.stopTX) { 
			switch(c) {
			case 0xFF:
				sendchar(0);    /* TX buffer empty */
				break;
			case 3:
				flushtx();
				break;
			default:
                sendchar(c);
			}
		}
	}
	//Postamble
//    sendchar('\r');
//    sendchar(4);		// EOT
//    sendchar('\r');
    flushtx();

}

/*
int tx_process(byte[] by)
{

	int xmtbyte;
        
        SendText = new String(by);
//System.out.println(SendText);
        while (SendText.length() > 0){
            switch (txstate) {
                    case TX_STATE_PREAMBLE:
//System.out.println("PREAMBLE");
                            clearbits();
                            for (int i = 0; i < 32; i++)
                                    sendbit(0);
                            txstate = TX_STATE_START;
                            break;

                    case TX_STATE_START:
//System.out.println("START");                        
                            sendchar('\r');
                            sendchar(2);		// STX
                            sendchar('\r');
                            txstate = TX_STATE_DATA;
                            break;

                    case TX_STATE_DATA:
//System.out.println("DATA");
                                xmtbyte = get_tx_char();
//        prt (xmtbyte);
                                if ( xmtbyte == 0x03 || stopflag)
                                        txstate = TX_STATE_FLUSH;
                                else if (xmtbyte == -1) {
                                        sendidle();
                                } else
                                        sendchar(xmtbyte);
                            break;

                    case TX_STATE_FLUSH:
//System.out.println("FLUSH");                        
                            sendchar('\r');
                            sendchar(4);		// EOT
                            sendchar('\r');
                            flushtx();
                            rxstate = RX_STATE_DATA;
                            txstate = TX_STATE_PREAMBLE;
//System.out.println("PREAMBLE");
                            stopflag = false;
    //			cwid();
                            return -1;
                }

//		case TX_STATE_PICTURE_START:
//// 176 samples
//			memset(picprologue, 0, 44 * 8 / TXspp);
//			sendpic(picprologue, 44 * 8 / TXspp);
//			txstate = TX_STATE_PICTURE;
//			break;
//	
//		case TX_STATE_PICTURE:
//			int i = 0;
//			int blocklen = 128;
//			while (i < xmtbytes) {
//				if (stopflag || abortxmt)
//					break;
//				if (i + blocklen < xmtbytes)
//					sendpic( &xmtpicbuff[i], blocklen);
//				else
//					sendpic( &xmtpicbuff[i], xmtbytes - i);
//				if ( (100 * i / xmtbytes) % 2 == 0) {
//					int n = snprintf(mfskmsg, sizeof(mfskmsg),
//							 "Send picture: %04.1f%% done",
//							 (100.0f * i) / xmtbytes);
//					print_time_left((xmtbytes - i) * 0.000125 * TXspp, mfskmsg + n,
//							sizeof(mfskmsg) - n, ", ", " left");
//					put_status(mfskmsg);
//				}
//				i += blocklen;
//			}
//			REQ_FLUSH(GET_THREAD_ID());
//
//			txstate = TX_STATE_DATA;
//			put_status("Send picture: done");
//			FL_LOCK_E();
//			btnpicTxSendAbort->hide();
//			btnpicTxSPP->show();
//			btnpicTxSendColor->show();
//			btnpicTxSendGrey->show();
//			btnpicTxLoad->show();
//			btnpicTxClose->show();
//			abortxmt = false;
//			rxstate = RX_STATE_DATA;
//			memset(picheader, ' ', PICHEADER - 1);
//			picheader[PICHEADER -1] = 0;
//			FL_UNLOCK_E();
//			break;
	}

	return 0;
}
  int get_tx_char(){
      int c = -1;
      //tbd
      if (SendText.length() > 0) {
          c = SendText.charAt(0);
          SendText = SendText.substring(1);
      }
      return c;
  }
  
  void set_tx_text(String s){
      SendText = s;
  }
  
    public void setSoundout(SoundOutput soundout) {
        if(this.soundout != soundout)
            this.soundout = soundout;
    }
    
*/

    void AddBytes(byte[] by) {
    	
    	//JD check the multiplier value for the buffer size
        int intSize = 10 * android.media.AudioTrack.getMinBufferSize(8000,
        		AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (AndPskmail.toBluetooth) {
        //JD Bluetooth hack test
//        	at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
        	at = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,  intSize , AudioTrack.MODE_STREAM);
        } else {
        	at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
        }

// 	    at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);

        //JD Bluetooth hack test
//        at = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
 	    
 	    
 	    //Launch TX
 	    at.setStereoVolume(1.0f,1.0f);
	    at.play();
        Txprocess(by); //Can be long running since it is in a worker's thread
		//Stop audio track
		at.stop();
		//Wait for end of audio play to avoid 
		//overlaps between end of TX and start of RX
		while (at.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		}
		//Close audio track
	    at.release();
	    //Reset the stop flag if it was ON
	    Modem.stopTX = false;
    }


/* 
    void mfskAdd(byte[] by) {

        Main.PTT = true;
                if (Main.SoftPTT){
                    Rigctl.SetPTT(Main.PTT);
                }
        int len = by.length;
        Main.TXActive = true;
        Main.txbusy = true;
        txstate = TX_STATE_PREAMBLE;
        boolean ready = false;
//        while (!ready){
            tx_process(by);
//        }
    }

void frametimer (int len){
    int TM = 0;
    int FC = 0;
           if (Main.TxModem == modemmodeenum.PSK500R){
            TM = 50;
            FC = 1500;
        } else if (Main.TxModem == modemmodeenum.PSK250R){
            TM = 100;
            FC = 1500;
        } else if (Main.TxModem == modemmodeenum.PSK125R){
            TM = 150;
            FC = 1500;
        } else if (Main.TxModem == modemmodeenum.PSK500){
            TM = 25;
            FC = 1000;
        } else if (Main.TxModem == modemmodeenum.PSK250){
            TM = 50;
            FC = 1000;
        } else if (Main.TxModem == modemmodeenum.PSK125){
            TM = 80;
            FC = 1000;
        } else if (Main.TxModem == modemmodeenum.PSK63){
            TM = 160;
            FC = 1000;
        }
        
        if (Main.txrsid){
            FC = 1920;
        } else {
            FC = 0;
        }

            try {
                Thread.sleep(len * TM + 1920 + FC);
            } catch (InterruptedException ex) {
                Logger.getLogger(SoundOutput.class.getName()).log(Level.SEVERE, null, ex);
            }
}

  int grayencode(int data)
//unsigned char graydecode(unsigned char data)
{
	int bits = data;

	bits ^= data >> 1;
	bits ^= data >> 2;
	bits ^= data >> 3;
	bits ^= data >> 4;
	bits ^= data >> 5;
	bits ^= data >> 6;
	bits ^= data >> 7;

	return bits;
}  
      private void prt(String x){
        System.out.println(x);
    }
    private void prt(Complex x){
        System.out.println(x);
    }  
    private void prt(int x){
        System.out.println(x);
    }  
    private void prt(double x){
        System.out.println(x);
    }
*/    
}
