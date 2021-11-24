/*
 * TxRSID.java  
 *   
 * Copyright (C) 2011 John Douyere (VK2ETA)  
 * Translated and adapted into Java class from Fldigi
 * as per code from Dave Freese, W1HKJ and Stelios Bounanos, M0GLD
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


public class TxRSID {


    //private static AudioTrack at = null;
    
	static final int RSID_NONE = -1;
	static final double RSID_SAMPLE_RATE = 11025.0;
	static final  int RSID_NSYMBOLS = 15;
	static final int RSID_RESOL = 2;
	static final int RSID_NTIMES  = (RSID_NSYMBOLS * RSID_RESOL);
	private static final double RSID_SYMLEN = (1024.0 / RSID_SAMPLE_RATE);
	static final double TWOPI = 2 * Math.PI;
	static final double M_PI = Math.PI;

	
	
	public TxRSID() {
		//Nothing yet
	}


	public static int getRSIDcode(modemmodeenum mode) {
		int code = RSID_NONE;

		switch (mode){
		case PSK31:
			code = 1;
			break;
		case PSK63:
			code = 2;
			break;
		case PSK125:
			code = 4;
			break;
		case PSK250:
			code = 126;
			break;
		case PSK500:
			code = 173;
			break;
		case PSK125R:
			code = 183;
			break;
		case PSK250R:
			code = 186;
			break;
		case PSK500R:
			code = 187;
			break;
		case MFSK8:
			code = 60;
			break;
		case MFSK16:
			code = 57;
			break;
		case MFSK32:
			code = 147;
			break;
		case MFSK64:
			code = 204;//re-use CONTESTIA_4_125 RSID since we can't use that mode in ARQ (limited character set)
			break;
		case THOR8:
			code = 137;
			break;
		case THOR11:
			code = 143;
			break;
		case THOR16:
			code = 138;
			break;
		case THOR22:
			code = 145;
			break;
		default:
			code = RSID_NONE;                                 
		}
		return code;

	}

	private static final int Squares[] = {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,
		0, 2, 4, 6, 8,10,12,14, 9,11,13,15, 1, 3, 5, 7,
		0, 3, 6, 5,12,15,10, 9, 1, 2, 7, 4,13,14,11, 8,
		0, 4, 8,12, 9,13, 1, 5,11,15, 3, 7, 2, 6,10,14,
		0, 5,10,15,13, 8, 7, 2, 3, 6, 9,12,14,11, 4, 1,
		0, 6,12,10, 1, 7,13,11, 2, 4,14, 8, 3, 5,15, 9,
		0, 7,14, 9, 5, 2,11,12,10,13, 4, 3,15, 8, 1, 6,
		0, 8, 9, 1,11, 3, 2,10,15, 7, 6,14, 4,12,13, 5,
		0, 9,11, 2,15, 6, 4,13, 7,14,12, 5, 8, 1, 3,10,
		0,10,13, 7, 3, 9,14, 4, 6,12,11, 1, 5,15, 8, 2,
		0,11,15, 4, 7,12, 8, 3,14, 5, 1,10, 9, 2, 6,13,
		0,12, 1,13, 2,14, 3,15, 4, 8, 5, 9, 6,10, 7,11,
		0,13, 3,14, 6,11, 5, 8,12, 1,15, 2,10, 7, 9, 4,
		0,14, 5,11,10, 4,15, 1,13, 3, 8, 6, 7, 9, 2,12,
		0,15, 7, 8,14, 1, 9, 6, 5,10, 2,13,11, 4,12, 3
	};

	private final static int indices[] = {
			2, 4, 8, 9, 11, 15, 7, 14, 5, 10, 13, 3
	};


	private static void Encode(int code, int[] rsid)
	{
		rsid[0] = code >> 8;
		rsid[1] = (code >> 4) & 0x0f;
		rsid[2] = code & 0x0f;
		for (int i = 3; i < RSID_NSYMBOLS; i++)
			rsid[i] = 0;
		for (int i = 0; i < 12; i++) {
			for (int j = RSID_NSYMBOLS - 1; j > 0; j--)
				rsid[j] = rsid[j - 1] ^ Squares[(rsid[j] << 4) + indices[i]];
			rsid[0] = Squares[(rsid[0] << 4) + indices[i]];
		}
	}

	public static void send() {
		int rmode = RSID_NONE;

		rmode = getRSIDcode(Processor.TxModem);

		if (rmode == RSID_NONE)
			return;

		//We have a valid code, send
    	String frequencySTR = AndPskmail.myconfig.getPreference("AFREQUENCY","1000");
    	int frequency = Integer.parseInt(frequencySTR);

    	int volumebits = Integer.parseInt(AndPskmail.myconfig.getPreference("VOLUME","8"));

    	/* Done at higher level now
    	//JD check the multiplier value for the buffer size
        int intSize = 4 * android.media.AudioTrack.getMinBufferSize(8000,
        		AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (AndPskmail.toBluetooth) {
        //JD Bluetooth hack test
//        	at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
        	at = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
        } else {
        	at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
        }

 	    //Open audiotrack
 	    at.setStereoVolume(1.0f,1.0f);
	    at.play();
	    */

	    int[] rsid = new int[RSID_NSYMBOLS];

		Encode(rmode, rsid);

		int sr = 8000; // should be active_modem->get_samplerate();
		int symlen = (int) (RSID_SYMLEN *  sr);
		short[] outbuf = new short[symlen];

		// transmit sequence of 15 symbols (tones)
		int iTone;
		double freq, phaseincr;
		double fr = 1.0 * frequency - (RSID_SAMPLE_RATE * 7 / 1024);
		double phase = 0.0;


		for (int i = 0; i < 15; i++) {
			iTone = rsid[i];
			freq = fr + iTone * RSID_SAMPLE_RATE / 1024;
			phaseincr = TWOPI * freq / sr;

			for (int j = 0; j < symlen; j++) {
				phase += phaseincr;
				if (phase > TWOPI) phase -= TWOPI;
				outbuf[j] = (short) ((int) (Math.sin(phase) * 8386560) >> volumebits);
			}
			//		active_modem->ModulateXmtr(outbuf, symlen);
			//Catch the stopTX flag at this point 
			if (!Modem.stopTX)  Modem.txAudioTrack.write(outbuf, 0, symlen);

		}

		// one symbol period of silence
		for (int ii = 0; ii < symlen; ii++) {
			outbuf[ii] = 0;
		}
		// transmit 6 symbol periods of silence at beginning of transmission
		for (int i = 0; i < 6; i++) {
			//Catch the stopTX flag at this point
			if (!Modem.stopTX)  Modem.txAudioTrack.write(outbuf, 0, symlen);
			//		active_modem->ModulateXmtr(outbuf, symlen);
		}

		/* Done at higher level now
		//Close audio track
		at.stop();
	    at.release();
		*/
	}	
}

