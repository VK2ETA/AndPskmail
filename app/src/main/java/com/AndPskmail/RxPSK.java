/*
 * Copyright (C) 2011 John Douyere (VK2ETA)
 * Based on Franz-Josef Maas (DB3CF) PSKMODEM PSK Java implementation
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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Franz-Josef <DB3CF at pskmail.de>
 */


public class RxPSK {

	public String RXmodename;
    
    private static int snrupdater = 49;

//    private Complex prevsymbol;
    private double prevsymbolR;
    private double prevsymbolI;
//    private Complex quality;
    private double qualityR;
    private double qualityI;
    private double phase;
    private int bits;
//    private double SQLCOEFF = 0.02;
    private double metric;
    private int dcdshreg;
    private boolean dcd;
    private boolean afcon = false;
    private boolean signalOK = false;
    private final float sampleRate = 8000.0f;
    private final double TWOPI = (2 * Math.PI);
    private double phaseacc;
    private FirFilter fir1, fir2;
    private double bitclk;
    private double[]	syncbuf = new double[16];
    private double freqerr;
    private double[] fir1c = new double[64];
    private double[] fir2c = new double[64];
    private double symbollen = 32.0; //32 = PSK250 
    private double bandwidth;
//    private double AFCDECAY = 8;
    private double snratio;
    private double imdratio;
//    private SoundInput soundIn = null;
//JD not now    private CirclePlotPanel cpp = null;
    private double[] soundDaten = null;
    private final int NUM_FILTERS = 3;
    private final int GOERTZEL = 288;	//96 x 2 must be an integer value
    private double[] I1 = new double[NUM_FILTERS];
    private double[] I2 = new double[NUM_FILTERS];
    private double[] Q1 = new double[NUM_FILTERS];
    private double[] Q2 = new double[NUM_FILTERS];
    private double[] COEF = new double[NUM_FILTERS];
    private double[] m_Energy = new double[NUM_FILTERS];
    private int m_NCount;
    private boolean imdValid;
    private Cmovavg snfilt;
    private Cmovavg imdfilt;
    private int SQLDECAY = 100; //JD too fast 50;
    private int AFCDECAYSLOW = 8;
    private boolean received = false;
    final Lock lock = new ReentrantLock();
    private boolean slowcpu = true;
    
    //PSK Robust variables
	private Viterbi dec;
	//PSKR modes - 2nd Viterbi decoder and 2 receive de-interleaver for comparison
	private Viterbi dec2;
	private interleave Rxinlv;
	private interleave Rxinlv2;
	private int 	rxbitstate;
	//PSKR modes - Soft decoding
//	unsigned char		symbolpair[2];
	int[] symbolpair = new int[2];
	private double	fecmet;
	private double	fecmet2;
	private int numinterleavers =0;
    private boolean _pskr = false;
    private int shreg = 0;
    private int shreg2 = 0;

	static final int PSKR_K = 7; 
	static final int PSKR_POLY1 = 0x6d;
	static final int PSKR_POLY2 = 0x4f; 

	
    public RxPSK(modemmodeenum mode) {

		changemode(mode);

        snrupdater = 49;
    }

    
    
    public void changemode(modemmodeenum newmode) {

    	switch(newmode){
    	case PSK31: 
    		RXmodename = "PSK31";
    		symbollen = 256;
    		_pskr = false;
    		break;
    	case PSK63: 
    		RXmodename = "PSK63";
    		symbollen = 128;
    		_pskr = false;
    		break;
    	case PSK125: 
    		RXmodename = "PSK125";
    		symbollen = 64;
    		_pskr = false;
    		break;
    	case PSK250: 
    		RXmodename = "PSK250";
    		symbollen = 32;
    		_pskr = false;
    		break;
    	case PSK500: 
    		RXmodename = "PSK500";
    		symbollen = 16;
    		_pskr = false;
    		break;
    	case PSK125R: 
    		RXmodename = "PSK125R";
    		symbollen = 64;
    		_pskr = true;
    		numinterleavers = -240;  // 2x2x40 interleaver
    		break;
    	case PSK250R: 
    		RXmodename = "PSK250R";
    		symbollen = 32;
    		_pskr = true;
    		numinterleavers = -280;  // 2x2x80 interleaver
    		break;
    	case PSK500R: 
    		RXmodename = "PSK500R";
    		symbollen = 16;
    		_pskr = true;
    		numinterleavers = -2160; // 2x2x160 interleaver
    		break;
    	default:
    		//We should not have called the PSK modem class
    		//Set to PSK250 (at least it does work)
    		RXmodename = "PSK250";
    		symbollen = 32;
    		_pskr = false;
    		break;
        }
    	rxInit();
    }


    public void rxInit() {
    	phaseacc = 0;
    	//        prevsymbol	= new Complex ( 1.0, 0.0 );
    	prevsymbolR	= 0.0;
    	prevsymbolI	= 0.0;
    	//        quality		= new Complex ( 0.0, 0.0 );
    	qualityR		= 0.0;
    	qualityI		= 0.0;
//    	shreg = 0;
    	if (_pskr) {
    		// MFSK based varicode instead of psk
    		shreg = 1;
    		shreg2 = 1;
    	} else {
    		shreg = 0;
    		shreg2 = 0;
    	}
    	dcdshreg = 0;
    	dcd = false;
    	bitclk = 0;
    	freqerr = 0.0;
    	bandwidth = sampleRate / symbollen;
    	initSN_IMD();
    	snratio = 1.0;
    	imdratio = 0.001;
    	resetSN_IMD();
    	imdValid = false;

    	//VK2ETA performance optimisation (for SLOW CPUS ONLY) 
    	//	- Reduce filters to 32 taps as this only marginally effects the
    	//  passband width (measured at about 40Hz extra for PSK500) but is the
    	//  single largest CPU intensive process of the PSK modes. 
    	//  - This does not impacts the in-signal-band QRM (although the 
    	//  passband is increased slightly as mentioned above).
    	//  - Gaussian noise sensitivity is same as with 64 taps version as expected.
    	//  - CPU load reduction is around 30% on an HTC Desire (Android version 2.1 - no JIT compiler).
    	//  - Integer rather than Double floating point calculations does not
    	//  improve CPU load at all on an HTC Desire (Android version 2.1 - no JIT compiler)
    	//

        slowcpu = AndPskmail.myconfig.getPreferenceB("SLOWCPU", true);

    	// creates fir1c matched sin(x)/x filter w blackman
    	// and fir2c matched sin(x)/x filter w blackman
        if (slowcpu) {
        	Wsincfilt.wsincfilt32 ( fir1c, 1.0 / symbollen, true );
        	Wsincfilt.wsincfilt32 ( fir2c, 1.0 / 16.0, true );
        } else {
        	Wsincfilt.wsincfilt ( fir1c, 1.0 / symbollen, true );
        	Wsincfilt.wsincfilt ( fir2c, 1.0 / 16.0, true );
        }

        fir1 = new FirFilter();
        fir2 = new FirFilter();
        if (slowcpu) {
        	//	Reduce filters to 32 taps
        	fir1.init ( 32, (int) symbollen / 16, fir1c);
        	fir2.init ( 32, 1, fir2c );
        } else {
        	fir1.init ( 64, (int) symbollen / 16, fir1c);
        	fir2.init ( 64, 1, fir2c );
        }
        
    	snfilt = new Cmovavg(16);
    	imdfilt = new Cmovavg(16);
    	
    	if (_pskr) {
    		// FEC for BPSK. Use a 2nd Viterbi decoder for comparison.
    		// Set decode size to 4 since some characters can be as small
    		// as 3 bits long. This minimises intercharacters decoding
    		// interactions.
    		dec = new Viterbi(PSKR_K, PSKR_POLY1, PSKR_POLY2);
    		dec.setchunksize(4);
    		dec2 = new Viterbi(PSKR_K, PSKR_POLY1, PSKR_POLY2);
    		dec2.setchunksize(4);

    		// Interleaver. To maintain constant time delay between bits,
    		// we double the number of concatenated square iterleavers for
    		// each doubling of speed: 2x2x20 for BSK63+FEC, 2x2x40 for
    		// BPSK125+FEC, etc..
    		//
    		// 2x2x(20,40,80,160)
   			Rxinlv = new interleave (numinterleavers, interleave.INTERLEAVE_REV); 
    		// 2x2x(20,40,80,160)
   			Rxinlv2 = new interleave (numinterleavers, interleave.INTERLEAVE_REV);
    		rxbitstate = 0;
    	}
    }

    
    //JD not now
/*
    public RxPSK(CirclePlotPanel cp) {
        cpp = cp;
//        soundInInit();
        rxInit();
    }
    
*/    
     
    void phaseafc() {
        double error;

        error = (phase - bits * Math.PI / 2.0);
        if (error < -Math.PI/2.0)
            error += TWOPI;
        if (error > Math.PI/2.0)
            error -= TWOPI;
        error *= ((sampleRate / TWOPI) / (16.0 * symbollen));
        if (Math.abs(error) < bandwidth) {
            freqerr = decayavg( freqerr, error, AFCDECAYSLOW);
            //UPdate only if afc is set to ON
            if (afcon ) Processor.m.rxFrequency -= freqerr;
        }
    }

    void afc() {
	if ( dcd == true )
            phaseafc();
    }

    

    void rx_bit ( int bit ) {
	int c;

		shreg = ((shreg << 1) & 0xFFFF) | (bit == 0? 1 : 0);
        //        if(shreg != -1)
        //         System.out.println(shreg + " " + bit);
    	if (_pskr) {
    		// MFSK varicode instead of PSK Varicode
    		if ((shreg & 7) == 1) {
    			c = mfskVaricode.varidec(shreg >> 1);
    			// Voting at the character level
    			if (fecmet >= fecmet2) {
    				if ((c != -1) && (c != 0) && (dcd == true)) {
    	                // Modem.rxblock((char) '<');
	                    Modem.rxblock((char) c);
    	                //Modem.rxblock((char) '>');
	                    received = true;
    				}
    			}
    			shreg = 1;
    		}
    	} else {
	        if ( ( shreg & 3 ) == 0 ) {
	        	c = PSKVariCode.psk_varicode_decode ( shreg >> 2 );
	        	if ( c != -1 )  //-1 kein Varicode
	        	{
	                    c &= 0x7F;
	//                    System.out.print(PSKVariCode.getAscii(c));
	//                    if(c == 4)
	//                        System.out.println();
	                    //Call back Modem class for processing
	                    Modem.rxblock((char) c);
	                    received = true;
	        	}
	        	shreg = 0;
	        }
    	}
    }

    


    void rx_bit2 ( int bit ) {
    	int c;

    	shreg2 = ((shreg2 << 1) & 0xFFFF) | (bit == 0? 1 : 0);
    	// MFSK varicode instead of PSK Varicode
    	if ((shreg2 & 7) == 1) {
    		c = mfskVaricode.varidec(shreg2 >> 1);
    		// Voting at the character level
    		if (fecmet < fecmet2) {
    			if ((c != -1) && (c != 0) && (dcd == true)) {
    				//Modem.rxblock((char) '[');
    				Modem.rxblock((char) c);
    				//Modem.rxblock((char) ']');
    				received = true;
    			}
    		}
    		shreg2 = 1;
    	}
    }

    
    
    
    void rx_pskr(int symbol)
    {
    	int met;
    	int[] twosym = new int[2];
    	int tempc;
    	int c;

    	// Accumulate the soft bits for the interleaver THEN submit to Viterbi
    	// decoder in alternance so that each one is processed one bit later.
    	// Only two possibilities for sync: current bit or previous one since
    	// we encode with R = 1/2 and send encoded bits one after the other
    	// through the interleaver.

    	symbolpair[1] = symbolpair[0];
    	symbolpair[0] = symbol;

    	if (rxbitstate == 0) {
    		rxbitstate++;
    		// copy to avoid scrambling symbolpair for the next bit
    		twosym[0] = symbolpair[0];
    		twosym[1] = symbolpair[1];
    		// De-interleave for Robust modes only
    		Rxinlv2.symbols(twosym);
    		// pass de-interleaved bits pair to the decoder, reversed
    		tempc = twosym[1];
    		twosym[1] = twosym[0];
    		twosym[0] = tempc;
    		// Then viterbi decoder
    		met = 0;
    		c = dec2.decode(twosym, met);
    		met = dec2.Vmetric;
    		if (c != -1) {
    			// FEC only take metric measurement after backtrace
    			// Will be used for voting between the two decoded streams
    			fecmet2 = decayavg(fecmet2, met, 20);
    			rx_bit2(c & 0x08);
    			rx_bit2(c & 0x04);
    			rx_bit2(c & 0x02);
    			rx_bit2(c & 0x01);
    		}
    	} else {
    		// Again for the same stream shifted by one bit
    		rxbitstate = 0;

        	twosym[0] = symbolpair[0];
    		twosym[1] = symbolpair[1];
    		// De-interleave
    		Rxinlv.symbols(twosym);
    		tempc = twosym[1];
    		twosym[1] = twosym[0];
    		twosym[0] = tempc;
    		// Then viterbi decoder
    		met = 0;
    		c = dec.decode(twosym, met);
    		met = dec.Vmetric;
    		if (c != -1) {
    			fecmet = decayavg(fecmet, met, 20);
    			rx_bit(c & 0x08);
    			rx_bit(c & 0x04);
    			rx_bit(c & 0x02);
    			rx_bit(c & 0x01);
    		}
    	}
    	
    }
  
    
    void rx_Symbol ( double symbolR, double symbolI ) {
    	int n;
    	int softbit = 0;
		double cxR = 0.0;
		double cxI = 0.0;
		
        cxR = (prevsymbolR * symbolR) + (prevsymbolI * symbolI);
        cxI = (prevsymbolR * symbolI) - (prevsymbolI * symbolR);
        
        phase = Math.atan2(cxI, cxR);
 
        prevsymbolR = symbolR;
        prevsymbolI = symbolI;

        //        System.out.printf("phase: %3.3f %3.3f ",phase, Math.cos(phase));
        if ( phase < 0.0 )
        	phase += 2 * Math.PI;
        bits = ( ( ( int ) ( phase / Math.PI + 0.5 ) ) & 1 ) << 1;
        //        System.out.printf("phase: %3.3f bits: %d   %3.3f\n",phase, bits, Math.cos(phase));
		// hard decode only for the time being. Note: no reversed bits in that case
        softbit = (bits & 2) != 0 ? 255 : 0;  
        n = 2;

        // simple low pass filter for quality of signal
        double mCos = Math.cos(n*phase);
        double mSin = Math.sin(n*phase);
        //VK2ETA: Make metric measure go up faster than down
        qualityR = decayavg(qualityR, mCos, mCos > qualityR ? SQLDECAY  : SQLDECAY * 3);
        qualityI = decayavg(qualityI, mSin,  mSin > qualityI ? SQLDECAY  : SQLDECAY * 3);

        metric = -15 + 115.0 * ((qualityR * qualityR) + (qualityI * qualityI));
		if (_pskr)
			metric = metric * 1.5;

        //update average for feedback to server
        if (metric > 100) metric = 100;
        if (metric < 0) metric = 0;
        Processor.avgsnr = decayavg(Processor.avgsnr, metric, 20);
        //Every so often update the frequency offset display
        if (snrupdater < 0) {
        	snrupdater = 49;
            AndPskmail.mHandler.post(AndPskmail.updatesignalquality);
        } else snrupdater --;

        //        System.out.printf("metric: %3.3f\n", metric);

        dcdshreg = ( dcdshreg << 2 ) | bits;

        switch ( dcdshreg ) {
            case 0xAAAAAAAA:	/* DCD on by preamble for PSK modes */
            	if (!_pskr) {
            		dcd = true;
            		qualityR = 1.0; 
            		qualityI = 0.0;
            		imdValid = true;
            	}
        		break;
        	case 0xA0A0A0A0:	// DCD on by preamble for PSKR modes ("11001100" sequence sent as preamble)
        		if (_pskr) {
        			dcd = true;
            		qualityR = 1.0; 
            		qualityI = 0.0;
            		imdValid = true;
//JD check this       				s2n_sum = s2n_sum2 = s2n_ncount = 0.0;
        		}
        		break;
            case 0:			/* DCD off by postamble for PSK modes only!!! */
            	if (!_pskr) {
	                dcd = false;
	                qualityR = 0.0;
	                qualityI = 0.0;
	                imdValid = false;
            	}
                break;

            default:
                if ( metric > Processor.m.squelch ) { 
                    dcd = true;
                    this.signalOK = true;
                }
                else {
                    dcd = false;
                    this.signalOK = false;
                }
                imdValid = false;
                break;
        }

    	if (!_pskr) {
//    		set_phase(phase, quality.norm(), dcd);
    		if (dcd == true) rx_bit(bits);
    	} else { // pskr processing
    		// FEC: moved below the rx_bit to use proper value for dcd
    		rx_pskr(softbit);
//    		set_phase(phase, quality.norm(), dcd);
    	}

//      	if(cpp != null)
//          cpp.setPhase(phase);

    }

    
    //JD optimised as this is CPU intensif
    // 1. Done: Eliminate use of complex objects as getters and setters are very inefficient in Android java
    // 2. TO-DO (if necessary): Pre-calculate the sin and cos (check value of such change)
    // 3. Doubles to INTs: Reverted to doubles as no improvement visible by using integers
    // 4. Done: Check the real-life effect of halfing the length of the filter (32 vs 64)
    void rxProcess ( short[] so, int buflength ) {

    	double delta = TWOPI * Processor.m.rxFrequency / sampleRate;
//    	Complex[] z;
        double[] zR = new double[2];
        double[] zI = new double[2];

//        signalQuality();

    	for ( int j = 0; j < buflength; j++ ) {
    		// Mix with the internal NCO

    		//No value since we receive short integers
    		//            if(Math.abs(so[j]) < 0.1)
    		//                so[j] = 0;

            zR[0] = so[j] * Math.cos ( phaseacc );
            zI[0] = so[j] * Math.sin ( phaseacc );
            phaseacc += delta;
            if ( phaseacc > Math.PI )
              phaseacc -= TWOPI;
// Filter and downsample
// by 16 (psk31)
// by  8 (psk63)
// by  4 (psk125)
// by  2 (psk250)
// first filter
            if ( fir1.run ( zR, zI ) ) {  // fir1 returns true every Nth sample

            	//VK2ETA: Bug Fix (PSK31 to 125 would not work)
            	// Move values across for next filter run
                zR[0] = zR[1];
                zI[0] = zI[1];

                // final filter
                fir2.run ( zR, zI ); // fir2 returns value on every sample

                //Not required right now                
                //calcSN_IMD(zR[0], zI[0]);

                int idx = ( int ) bitclk;
                double sum = 0.0;
                double ampsum = 0.0;
//                syncbuf[idx] = 0.8 * syncbuf[idx] + 0.2 * z[1].abs(); //Math.sqrt(norm())
                syncbuf[idx] = 0.8 * syncbuf[idx] + 0.2 * Math.sqrt(zR[1] * zR[1] + zI[1] * zI[1]);

                for ( int i = 0; i < 8; i++ ) {
                    sum += ( syncbuf[i] - syncbuf[i+8] );
                    ampsum += ( syncbuf[i] + syncbuf[i+8] );
                }
                // added correction as per PocketDigi
                // vastly improved performance with synchronous interference !!
                sum = ( ampsum == 0 ? 0 : sum / ampsum );
                bitclk -= sum / 5.0;
                bitclk += 1;

                if ( bitclk < 0 ) bitclk += 16.0;
                if ( bitclk >= 16.0 ) {
                    bitclk -= 16.0;
                    rx_Symbol ( zR[1], zI[1] );
                    //Not now	update_syncscope();
                    //Not Now afc();
                }
            }
        }
    }

//============================================================================
// psk signal evaluation
// using Goertzel IIR filter
// derived from pskcore by Moe Wheatley, AE4JY
//============================================================================

    void initSN_IMD() {
	for(int i = 0; i < NUM_FILTERS; i++)
	{
		I1[i] = I2[i] = Q1[i] = Q2[i] = 0.0;
		m_Energy[i] = 0.0;
	}
	m_NCount = 0;

	COEF[0] = 2.0 * Math.cos(TWOPI * 9 / GOERTZEL);
	COEF[1] = 2.0 * Math.cos(TWOPI * 18 / GOERTZEL);
	COEF[2] = 2.0 * Math.cos(TWOPI  * 27 / GOERTZEL);
    }

    void resetSN_IMD() {
	for(int i = 0; i < NUM_FILTERS; i++) {
		I1[i] = I2[i] = Q1[i] = Q2[i] = 0.0;
	}
	m_NCount = 0;
    }

//============================================================================
//  This routine calculates the energy in the frequency bands of
//   carrier=F0(15.625), noise=F1(31.25), and
//   3rd order product=F2(46.875)
//  It is called with complex data samples at 500 Hz.
//============================================================================

    void calcSN_IMD(double zR, double zI) {
	int i;
//	Complex temp;
	double tempR;
	double tempI;

	for(i = 0; i < NUM_FILTERS; i++) {
//		temp = new Complex(I1[i], Q1[i]);
		tempR = I1[i]; tempI = Q1[i];
//		I1[i] = I1[i] * COEF[i]- I2[i] + z.getReal();
		I1[i] = I1[i] * COEF[i]- I2[i] + zR;
//		Q1[i] = Q1[i] * COEF[i]- Q2[i] + z.getImag();
		Q1[i] = Q1[i] * COEF[i]- Q2[i] + zI;
//		I2[i] = temp.getReal(); Q2[i] = temp.getImag();
		I2[i] = tempR; Q2[i] = tempI;
	}

	if( ++m_NCount >= GOERTZEL ) {
		m_NCount = 0;
		for(i = 0; i < NUM_FILTERS; i++) {
			m_Energy[i] =   I1[i]*I1[i] + Q1[i]*Q1[i]
			              + I2[i]*I2[i] + Q2[i]*Q2[i]
						  - I1[i]*I2[i]*COEF[i]
						  - Q1[i]*Q2[i]*COEF[i];
			I1[i] = I2[i] = Q1[i] = Q2[i] = 0.0;
		}
		signalquality();
	}
    }

    void signalquality() {

	if (m_Energy[1] != 0.0)
		snratio = snfilt.run(m_Energy[0]/m_Energy[1]);
	else
		snratio = snfilt.run(1.0);

	if ((m_Energy[0] != 0.0) && imdValid)
		imdratio = imdfilt.run(m_Energy[2]/m_Energy[0]);
	else
		imdratio = imdfilt.run(0.001);

    }

    double decayavg(double average, double input, double weight) {
        if (weight <= 1.0)
            return input;
//JD Optimize this
        return input * (1.0 / weight) + average * (1.0 - (1.0 / weight));
    }

    
}