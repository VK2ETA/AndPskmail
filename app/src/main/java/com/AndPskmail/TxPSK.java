/*
 * Copyright (C) 2011 John Douyere (VK2ETA)
 * Adapted from JavaModem class from Franz-Josef Maas (DB3CF)
 * and adapted from gMFSK (see notice
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 *    psk31tx.c  --  PSK31 modulator
 *
 *    Copyright (C) 2001, 2002, 2003, 2004
 *      Tomi Manninen (oh2bns@sral.fi)
 *
 *    This file is part of gMFSK.
 *
 *    gMFSK is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    gMFSK is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with gMFSK; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package com.AndPskmail;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;


/**
 *
 * @author Franz-Josef <DB3CF at pskmail.de>
 */
public class TxPSK {

	public static String TXmodename = "     ";
	
	private static boolean prevsymbol = true;
//	private static Complex prevsymbol = new Complex(1.0,0.0);

    private static int sampleRate = 8000;
    private static final double TWOPI = (2 * Math.PI);
    private static int frequency = 1000;
    private static int volumebits = 10;
    private static int xoffset;
    private static int symbollen = 64; //32 = psk250
    private static int[] txshape;
    private static int[] OneMtxshape;
    
    private static int[] cosarray;
    private static int maxcossteps = 0;
    private static double phaseacc;
    private static int phasestep;
    public static short[] outSbuffer;
    
    private boolean tune;
    private int preamble;

    //public AudioTrack at = null;
    private int DCDOFF = 32;
    private double volume = 0.5;
    
    //PSK Robust variables   
    private interleave Txinlv;
    private boolean _pskr = false;
	static final int PSKR_K = 7; 
	static final int PSKR_POLY1 = 0x6d;
	static final int PSKR_POLY2 = 0x4f; 
	private ViterbiEncoder enc;


    /**
     * @param aFrequency the frequency to set between 500 and 2000
     */
    public static void setFrequency(int aFrequency) {
        if (aFrequency < 500) aFrequency = 500;
        if (aFrequency > 2000) aFrequency = 2000;
        frequency = aFrequency;
    }

    /**
     * @return the symbollen //e.g. 32 - PSK250 
     */
    public static int getSymbollen() {
        return symbollen;
    }

    /**
     * @param aSymbollen the symbollen to set
     */
    public static void setSymbollen(int aSymbollen) {
        symbollen = aSymbollen;
    }


    public TxPSK(modemmodeenum mode) {

    	int numinterleavers =0;
    	
    	String frequencySTR = AndPskmail.myconfig.getPreference("AFREQUENCY","1000");
    	frequency = Integer.parseInt(frequencySTR);
		if (frequency < 500) frequency = 500;
		if (frequency > 2500) frequency = 2500;

    	volumebits = Integer.parseInt(AndPskmail.myconfig.getPreference("VOLUME","8"));

    	xoffset = 0;
        tune = false;

        switch(mode){
    	case PSK31: 
    		TXmodename = "PSK31";
    		symbollen = 256;
    		preamble = DCDOFF = 32; 
    		_pskr = false;
    		break;
    	case PSK63:
    		TXmodename = "PSK63";
    		symbollen = 128;
    		preamble = DCDOFF = 64; 
    		_pskr = false;
    		break;
    	case PSK125:
    		TXmodename = "PSK125";
    		symbollen = 64;
    		preamble = DCDOFF = 128; 
    		_pskr = false;
    		break;
    	case PSK250:
    		TXmodename = "PSK250";
    		symbollen = 32;
    		preamble = DCDOFF = 256; 
    		_pskr = false;
    		break;
    	case PSK500:
    		TXmodename = "PSK500";
    		symbollen = 16;
    		preamble = DCDOFF = 512; 
    		_pskr = false;
    		break;
    	case PSK125R:
    		TXmodename = "PSK125R";
    		symbollen = 64;
    		preamble = DCDOFF = 128; 
    		numinterleavers = -240;  // 2x2x40 interleaver
    		_pskr = true;
    		break;
    	case PSK250R:
    		TXmodename = "PSK250R";
    		symbollen = 32;
    		preamble = DCDOFF = 256; 
    		numinterleavers = -280;  // 2x2x80 interleaver
    		_pskr = true;
    		break;
    	case PSK500R:
    		TXmodename = "PSK500R";
    		symbollen = 16;
    		preamble = DCDOFF = 512; 
    		numinterleavers = -2160;  // 2x2x160 interleaver
    		_pskr = true;
    		break;
        }

        //Prepare for PSK Robust
        if (_pskr) {
			// Interleaver. To maintain constant time delay between bits,
			// we double the number of concatenated square iterleavers for
			// each doubling of speed: 2x2x20 for BSK63+FEC, 2x2x40 for
			// BPSK125+FEC, etc..
			//
			// 2x2x(20,40,80,160)
			Txinlv = new interleave (numinterleavers, interleave.INTERLEAVE_FWD); 
			//Viterbi encoder
			enc = new ViterbiEncoder(PSKR_K, PSKR_POLY1, PSKR_POLY2);

        }
        
        txshape = new int[symbollen];
        OneMtxshape = new int[symbollen];
    	maxcossteps = 0;
    	for (int i = 0; i < symbollen; i++) {
    		txshape[i] = (int) (4095.0 * (0.5 * Math.cos(i * Math.PI / symbollen) + 0.5));
    		OneMtxshape[i] = (int) (4095.0 - txshape[i]);
    	}
    	//VK2ETA Optimise speed for Android devices by pre-calculating cosinus values
    	//Calculate the array size first
    	double delta = TWOPI * (frequency + xoffset) / sampleRate;
    	phaseacc = 2 * delta;  //Can�t be less than 2 (4000hz audio!)
    	for (int i = 2; i < 1000; i++) {
    		double cosval = Math.cos(phaseacc);
    		if (cosval > 0.9995){ //5000PPM error
    			//We found an acceptable return to one
    			maxcossteps = i;
    			break;
    		}
	        phaseacc += delta;
                if (phaseacc > Math.PI)
                    phaseacc -= TWOPI;
    	}
    	cosarray = new int[maxcossteps];
    	phaseacc = 0;
    	for (int i = 0; i < maxcossteps; i++) {
    		//Pre-scale at 2048 for conversion to Int (will scale back later)
    		cosarray[i] = (int) (2048 * Math.cos(phaseacc));

            phaseacc += delta;
            if (phaseacc > Math.PI)
                phaseacc -= TWOPI;
    	}
    	//prepare for easy comparison in send symbol class
    	maxcossteps--;
    	
    }

    
    void init() {

    	prevsymbol = true;
//    	prevsymbol = new Complex(1.0,0.0);

    	phaseacc = 0;
        phasestep = 0;
    }

    
    void sendSymbol(boolean sym)
    {
        boolean symbol = sym;
    	int i;
    	
    	//  create output sound buffer 
      	short [] outSbuffer = new short[symbollen];

    	symbol = !(symbol ^ prevsymbol);

    	int ival;
    	for (i = 0; i < symbollen; i++) {

    		if (prevsymbol) {
    			ival = txshape[i];
    		} else {
    			ival = - txshape[i];
    		}

    		if (symbol) {
    			ival += OneMtxshape[i];
    		} else {
    			ival -= OneMtxshape[i];
    		}

    		outSbuffer[i] = (short) ((ival * cosarray[phasestep]) >> volumebits);

    	    if (++phasestep > maxcossteps) 
    			phasestep = 0; 
            
    	}

		//Catch the stopTX flag at this point as well
		if (!Modem.stopTX) Modem.txAudioTrack.write(outSbuffer, 0, symbollen);

    	// save the current symbol 
    	prevsymbol = symbol;
    }

    

    void tx_bit( int sendbit) {
    	int bitshreg;
    	boolean sym;

    	//Viterbi encoder
    	bitshreg = enc.encode(sendbit);
    	// pass through interleaver
    	bitshreg = Txinlv.bits(bitshreg);
    	// Send low bit first
    	sym = (bitshreg & 1) != 0 ? true : false;
    	sendSymbol(sym);
    	sym = (bitshreg & 2) != 0 ? true : false;;
    	sendSymbol(sym);
    }    

    void sendChar( int c) {
    	String code;
    	int sendbit;

    	c &= 255;   //max 255
    	if (_pskr) {
    		code = mfskVaricode.varienc(c);
    		for(int i = 0; i < code.length() ;i++) {
    			sendbit = ((((code.charAt(i) - '0'))== 1)?  1 : 0);
    			tx_bit(sendbit);
    		}
    	} else {
    		code = PSKVariCode.psk_varicode_encode(c);
    		for(int i = 0; i < code.length() ;i++) {
    			sendSymbol( (((code.charAt(i) - '0'))== 1)?  true : false);
    		}
    		sendSymbol(false);
    		sendSymbol(false);
    	}
    }

    void flushTx() {

    	if (_pskr) {
    		for (int i = 0; i < DCDOFF; i++) {
    			tx_bit(0);
    		}
    	} else {

    		/* DCD off sequence (unmodulated carrier) */
    		for (int i = 0; i < DCDOFF; i++)
    			sendSymbol(true);
    	}
    }



    void Txprocess(byte[] str) {
    	int c;
    	int bitshreg;

    	init();
    	if (_pskr) {
    		// Necessary to clear the interleaver before we start sending
    		bitshreg = enc.encode(0);
    		for (int k = 0; k < 160; k++) {
    			Txinlv.bits(bitshreg);
    		}
    	}
    	while (preamble > 0) {
    		if (_pskr) {
    			// FEC prep the encoder with one/zero sequences of bits
    			preamble--;
    			preamble--;
    			tx_bit(1);
    			tx_bit(0);
    			// FEC: Mark start of first character with a double zero 
    			// to ensure sync at end of preamble
    			if (preamble == 0)  tx_bit(0);
    		} else {
    			// Standard BPSK/QPSK preamble
    			preamble--;
    			sendSymbol(false);	/* send phase reversals */
    		}
    	}


    	for(int i = 0; i < str.length; i++) {
    		c = str[i];

    		//Catch the stopTX flag at this point
    		if (!Modem.stopTX) { 
    			switch(c) {
    			case 0xFF:
    				sendChar(0);    /* TX buffer empty */
    				break;
    			case 3:
    				flushTx();
    				break;
    			default:
    				sendChar(c);
    			}
    		}
    	}

    	/* stop if requested to... */
    	//        if (stopflag) {
    	//            flushTx();
    	//        }
    	//        else {
    	if (!_pskr) {
    		sendSymbol(false);	/* send phase reversals */
    		sendSymbol(false);
    	}
    	flushTx();

    }


    void AddBytes(byte[] by) {

    	/* Done at higher level now
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
		*/

        Txprocess(by); //Can be long running since it is in a worker's thread

		/* Done at higher level now
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
	    at.release();
	    */
	    //Reset the stop flag if it was ON
	    Modem.stopTX = false;
    }


    
    /**
     * @return the sampleRate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * @param aSampleRate the sampleRate to set
     */
    public void setSampleRate(int aSampleRate) {
        sampleRate = aSampleRate;
    }

    /**
     * @return the frequency
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * @return the xoffset
     */
    public int getXoffset() {
        return xoffset;
    }

    /**
     * @return the volume
     */
    public double getVolume() {
        return volume;
    }

    /**
     * @param volume the volume to set 0.0 - 1.0
     */
    public void setVolume(double volume) {
        if(volume < 0.0)
            volume = 0.0;
        if(volume > 1.0)
            volume = 1.0;
        this.volume = volume;
    }


}