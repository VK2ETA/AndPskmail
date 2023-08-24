/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.AndPskmail;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author rein
 */
public class RxTHOR {

    private static int snrupdater = 49;
    modemmodeenum mode;
    boolean keeprunning = false;
//    private ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
//    private byte[] receiveBytes;
//    private boolean received = false;
    public int running = 0;
    final Lock lock = new ReentrantLock();
//    private SoundInput soundIn = null;
//    private double[] soundDaten = null;
    int symlen = 1024;
    int doublespaced = 1;
    int samplerate = 8000;
    double tonespacing;
    double bandwidth;
    final static int THORNUMTONES = 18;
    final static int THORMAXFFTS = 8;
    final static double THORBASEFREQ = 500.0;
    final static double THORFIRSTIF = 1000.0;
    final static double TWOPI = 2 * Math.PI;
    final static double M_PI = Math.PI;
//    int THORSCOPESIZE = 64;
    //JD debug CPU Load
//    int THORSLOWPATHS = 3;
    final static int THORSLOWPATHS = 1;
    final static int THORFASTPATHS = 3;
//    boolean THOR_FILTER = true;
    boolean THOR_FILTER = false;  // debug later RC
    int extones;
    int paths;
    int lotone;
    int hitone;
    int numbins;
    int twosym;
//    Complex[][] pipe;
    double[][] pipeR;
    double[][] pipeI;
    int pipeptr = 0;
    int symcounter = 0;
    double metric = 0.0;
    int fragmentsize;
    double s2n;
//    Complex currvector;
    double currvectorR;
    double currvectorI;
    int currsymbol;
    int prev1symbol;
    int prev2symbol;
    double currmag;
    double prev1mag;
    double prev2mag;
    private FirFilter hilbert;
//    private int FIRBufferLen = 4096;
//    private Cfft rsfft;
//JD debug: not yet    private fftfilt fft;
    private int basetone;
    private boolean slowcpu;
    private sfft[] binsfft;
    private boolean filter_reset;
    Cmovavg syncfilter;
    ViterbiEncoder Enc;
    final int THOR_K = 7;
    final int THOR_POLY1 = 0x6d;
    final int THOR_POLY2 = 0x4f;
    Viterbi Dec;
    interleave Txinlv;
    interleave Rxinlv;
    int bitstate = 0;
    int[] symbolpair;
    int datashreg = 1;
    int frequency;
    double[] phase = new double[THORMAXFFTS + 1];
    boolean s2n_valid;
    double s2n_sum;
    double s2n_sum2;
    double s2n_ncount;
    double s2n_metric;
    boolean staticburst;
    int synccounter;
    double met1;
    double met2;
    double sig;
    double noise;
//    static public Complex zref = new Complex();

    
 //Constructor   
    RxTHOR(modemmodeenum mode) {
//	cap |= CAP_REV;

		changemode(mode);

		snrupdater = 49;
		
    }
    
    
	public void changemode(modemmodeenum mode) {


        switch (mode) {
// 11.025 kHz modes

            case THOR11:
                symlen = 1024;
                doublespaced = 1;
                samplerate = 11025;
                break;

            case THOR22:
                symlen = 512;
                doublespaced = 1;
                samplerate = 11025;
                break;
// 8kHz modes
  /*          case THOR4:
                symlen = 2048;
                doublespaced = 2;
                samplerate = 8000;
                break;
*/
            case THOR8:
                symlen = 1024;
                doublespaced = 2;
                samplerate = 8000;
                break;
            case THOR16:
                symlen = 512;
                doublespaced = 1;
                samplerate = 8000;
                break;
            default: //THOR22
                symlen = 512;
                doublespaced = 1;
                samplerate = 11025;
                break;
        }

        tonespacing = 1.0 * samplerate * doublespaced / symlen;

        bandwidth = THORNUMTONES * tonespacing;

        hilbert = new FirFilter();
        
        slowcpu = AndPskmail.myconfig.getPreferenceB("SLOWCPU", true);

        //VK2ETA test CPU load
        if (slowcpu) {
            hilbert.init_hilbert(15, 1);
        } else {
        	hilbert.init_hilbert(37, 1);
        }

// fft filter at first if frequency
/* VK2ETA not yet        fft = new fftfilt((THORFIRSTIF - 0.5 * 1.5 * bandwidth) / samplerate,
                (THORFIRSTIF + 0.5 * 1.5 * bandwidth) / samplerate,
                1024);
*/
        basetone = (int) Math.floor(THORBASEFREQ * symlen / samplerate + 0.5);

        binsfft = new sfft[THORMAXFFTS];

        for (int i = 0; i < THORMAXFFTS; i++) {
            binsfft[i] = null;
        }


        reset_filters();

//	for (int i = 0; i < THORSCOPESIZE; i++)
//		vidfilter[i] = new Cmovavg(16);

        syncfilter = new Cmovavg(8);

        twosym = 2 * symlen;

//        Complex Czero = new Complex(0.0, 0.0);

//        pipe = new Complex[twosym][RxTHOR.THORMAXFFTS * RxTHOR.THORNUMTONES * 6];
//JD Debug OUT OF MEMORY ERROR: fixed to largest value so that mode changes are seamless
//        pipeR = new double[twosym][RxTHOR.THORMAXFFTS * RxTHOR.THORNUMTONES * 6];
//        pipeI = new double[twosym][RxTHOR.THORMAXFFTS * RxTHOR.THORNUMTONES * 6];
        pipeR = new double[2048][52 * 3];
        pipeI = new double[2048][52 * 3];
        for (int i = 0; i < twosym; i++) {
//JD Debug OUT OF MEMORY ERROR            for (int j = 0; j < RxTHOR.THORMAXFFTS * RxTHOR.THORNUMTONES * 6; j++) {
                for (int j = 0; j < 52 * 3; j++) {
                pipeR[i][j] = 0.0;
                pipeI[i][j] = 0.0;
            }
        }

//	scopedata.alloc(THORSCOPESIZE);
//	videodata.alloc(THORMAXFFTS * numbins );

        pipeptr = 0;

        symcounter = 0;
        metric = 0.0;

        fragmentsize = symlen;

        s2n = 0.0;

        prev1symbol = 0;
        prev2symbol = 0;


        Dec = new Viterbi(THOR_K, THOR_POLY1, THOR_POLY2);
        Dec.settraceback(45);
        Dec.setchunksize(1);
//        Txinlv = new interleave(4, interleave.INTERLEAVE_FWD); // 4x4x10
        Rxinlv = new interleave(4, interleave.INTERLEAVE_REV); // 4x4x10
        bitstate = 0;
        symbolpair = new int[2];
        symbolpair[0] = 0;
        symbolpair[1] = 0;
        datashreg = 1;


//        init();
    }

    void rxInit() {

        synccounter = 0;
        symcounter = 0;
        met1 = 0;
        met2 = 0;
        //   counter = 0;
        phase[0] = 0;
 //VK2ETA not used       currmag = prev1mag = prev2mag = 0;
        for (int i = 0; i < THORMAXFFTS; i++) {
            phase[i + 1] = 0.0;
        }
        syncfilter.reset();
        datashreg = 1;
        sig = noise = 6;
        s2n_valid = false;
    }

    void restart() {
        filter_reset = true;
    }

    private void reset_filters() {
// fft filter at first IF frequency
/* VK2ETA not yet        fft.create_filter((THORFIRSTIF - 0.5 * 1.5 * bandwidth) / samplerate,
                (THORFIRSTIF + 0.5 * 1.5 * bandwidth) / samplerate);
*/
        if (slowcpu) {
            //JD debug CPU Load
//            extones = 4;
            extones = 0;
            paths = THORSLOWPATHS;
        } else {
            //VK2ETA CPU Load
//            extones = THORNUMTONES / 2;
            extones = 4;
            paths = THORFASTPATHS;
        }

        lotone = basetone - extones * doublespaced;
        hitone = basetone + THORNUMTONES * doublespaced + extones * doublespaced;

        numbins = hitone - lotone;

        for (int i = 0; i < paths; i++) {
            binsfft[i] = new sfft(symlen, lotone, hitone);
        }

        filter_reset = false;
    }

//    private Complex mixer(int n, Complex in) {
	private void mixer(int n, double[] inR, double[] inI) {
		//Complex z = new Complex();
        double zR, zI;
        double f;

        // first IF mixer (n == 0) plus
        // THORMAXFFTS mixers are supported each separated by 1/THORMAXFFTS bin size
        // n == 1, 2, 3, 4 ... THORMAXFFTS

        if (n == 0) {
            f = Processor.m.rxFrequency - THORFIRSTIF;
        } else {
            f = THORFIRSTIF - THORBASEFREQ - bandwidth / 2 + (samplerate / symlen) * (1.0 * n / paths);
        }
      
        zR = Math.cos(phase[n]);
        zI = Math.sin(phase[n]);

        //re-pack Complex result in position 1
        inR[1] = zR * inR[0] - zI * inI[0];
        inI[1] = zR * inI[0] + zI * inR[0];

        phase[n] -= TWOPI * f / samplerate;
        if (phase[n] > M_PI) {
            phase[n] -= TWOPI;
        } else if (phase[n] < M_PI) {
            phase[n] += TWOPI;
        }
    }


    void recvchar(int c) {
        if (c == -1) {
            return;
        //}
        //if ((c & 0x100) == 0x100) {
        	//NO secondary characters for the time being (no use for it in ARQ)
        	//		put_sec_char(c & 0xFF);
        } else {
        	//		put_rx_char(c & 0xFF);
    		Modem.rxblock((char) (c & 0xFF));

        }
    }

//=============================================================================
// Receive
//=============================================================================
    void decodePairs(int symbol) {
        int bit, ch, met;

        symbolpair[0] = symbolpair[1];
        symbolpair[1] = symbol;
        met = 0;

        symcounter = (symcounter != 0) ? 0 : 1;

//VK2ETA DEBUG FIX: 	if (symcounter == 0) return;
        if (symcounter != 0) {
            return;
        }

        bit = Dec.decode(symbolpair, met);

        if (bit == -1) {
            return;
        }

        if (metric <= Processor.m.squelch) {
            return;
        }

//	shreg = (shreg << 1) | !!bit;
//        datashreg = ((datashreg << 1) & 0xFFFF) | (bit == 0? 1 : 0);
        //VK2ETA UTF-8 support
        //datashreg = ((datashreg << 1) & 0xFFF) | (bit == 0 ? 0 : 1);
        datashreg = ((datashreg << 1) & 0xFFFF) | (bit == 0 ? 0 : 1);


        if ((datashreg & 7) == 1) {
            ch = thorvaricode.thorvaridec(datashreg >> 1);
            recvchar(ch);
            datashreg = 1;
        }
    }

    void decodesymbol() {
        int c;
        double fdiff;//, softmag;
        int[] symbols = new int[4];

        boolean outofrange = false;

// Decode the IFK+ sequence, which results in a single nibble

        fdiff = currsymbol - prev1symbol;
//System.out.println(fdiff);	
//	if (reverse) fdiff = -fdiff;
        fdiff /= paths;
        fdiff /= doublespaced;

        if (Math.abs(fdiff) > 17) {
            outofrange = true;
        }

        c = (int) Math.floor(fdiff + .5) - 2;
        if (c < 0) {
            c += THORNUMTONES;
        }
//prt(c);
        if (staticburst == true || outofrange == true) // puncture the code
        {
            symbols[3] = symbols[2] = symbols[1] = symbols[0] = 0;
        } else {
            symbols[3] = (c & 1) == 1 ? 255 : 0;
            c /= 2;
            symbols[2] = (c & 1) == 1 ? 255 : 0;
            c /= 2;
            symbols[1] = (c & 1) == 1 ? 255 : 0;
            c /= 2;
            symbols[0] = (c & 1) == 1 ? 255 : 0;
            c /= 2;
        }

        Rxinlv.symbols(symbols);

        for (int i = 0; i < 4; i++) {
            decodePairs(symbols[i]);
        }

    }

    int harddecode() {
        double xR, xI, x;
        double max = 0.0;
        int symbol = 0;
    	
    	/* VK2ETA debug: CPU usage reduction: no CWI detection, no static burst detection
        double avg = 0.0;
        boolean cwi[] = new boolean[paths * numbins];
        double cwmag;

         for (int i = 0; i < paths * numbins; i++) {

            avg += pipe[pipeptr][i].abs();
        }
        avg /= (paths * numbins);

        if (avg < 1e-10) {
            avg = 1e-10;
        }

        int numtests = 10;
        int count = 0;
        for (int i = 0; i < paths * numbins; i++) {
            cwmag = 0.0;
            count = 0;
            for (int j = 1; j <= numtests; j++) {
                int p = pipeptr - j;
                if (p < 0) {
                    p += twosym;
                }
                cwmag = (pipe[j][i].abs()) / numtests;
                if (cwmag >= 50.0 * (1.0 - 0.1) * avg) {
                    count++;
                }
//                        prt(count);
            }
            cwi[i] = (count == numtests);
        }
        for (int i = 0; i < paths * numbins; i++) {
            if (cwi[i] == false) {
                x = pipe[pipeptr][i].abs();
                if (x > max) {
                    max = x;
                    symbol = i;

                }
            }
        }

        staticburst = (max / avg < 1.2);
*/
        staticburst = false;
        
        for (int i = 0; i < paths * numbins; i++) {
        	xR = pipeR[pipeptr][i];
        	xI = pipeI[pipeptr][i];
        	x = xR * xR + xI * xI;
        	if (x > max) {
        		max = x;
        		symbol = i;

        	}
        }

        return symbol;
    }

//void update_syncscope()
//{
//
//	double max = 0, min = 1e6, range, mag;
//
//	memset(videodata, 0, paths * numbins * sizeof(double));
//
//	if (!progStatus.sqlonoff || metric >= progStatus.sldrSquelchValue) {
//		for (int i = 0; i < paths * numbins; i++ ) {
//			mag = pipe[pipeptr].vector[i].mag();
//			if (max < mag) max = mag;
//			if (min > mag) min = mag;
//		}
//		range = max - min;
//		for (int i = 0; i < paths * numbins; i++ ) {
//			if (range > 2) {
//				mag = (pipe[pipeptr].vector[i].mag() - min) / range + 0.0001;
//				mag = 1 + 2 * log10(mag);
//				if (mag < 0) mag = 0;
//			} else
//				mag = 0;
//			videodata[(i + paths * numbins / 2)/2] = 255*mag;
//		}
//	}
//	set_video(videodata, paths * numbins, false);
//	videodata.next();
//
//	memset(scopedata, 0, THORSCOPESIZE * sizeof(double));
//	if (!progStatus.sqlonoff || metric >= progStatus.sldrSquelchValue) {
//		for (unsigned int i = 0, j = 0; i < THORSCOPESIZE; i++) {
//			j = (pipeptr + i * twosym / THORSCOPESIZE + 1) % (twosym);
//			scopedata[i] = vidfilter[i]->run(pipe[j].vector[prev1symbol].mag());
//		}
//	}
//	set_scope(scopedata, THORSCOPESIZE);
//	scopedata.next();
//}
    void synchronize() {
        double syn = -1;
        double valR, valI, val, max = 0.0;

        if (staticburst == true) {
            return;
        }

        if (currsymbol == prev1symbol) {
            return;
        }
        if (prev1symbol == prev2symbol) {
            return;
        }

        for (int i = 0, j = pipeptr; i < twosym; i++) {
//            val = (pipe[j][prev1symbol]).abs();
            valR = (pipeR[j][prev1symbol]);
            valI = (pipeI[j][prev1symbol]);
            val = valR * valR + valI * valI;
            if (val > max) {
                max = val;
                syn = i;
            }
          //No unsigned ints in Java. Have to use a different logic
          //j = (j + 1) % twosym;
          j = ++j >= twosym ? 0 : j;
        }

        syn = syncfilter.run(syn);

        synccounter += (int) Math.floor(1.0 * (syn - symlen) / THORNUMTONES + 0.5);
//prt(synccounter);
//	update_syncscope();

    }

    void eval_s2n() {
    	double sR, sI, s;
    	double nR, nI, n;
    	sR = pipeR[pipeptr][currsymbol];
    	sI = pipeI[pipeptr][currsymbol];
    	s = Math.sqrt(sR * sR + sI * sI);

    	nR = (THORNUMTONES - 1) * pipeR[(pipeptr + symlen) % twosym][currsymbol];
    	nI = (THORNUMTONES - 1) * pipeI[(pipeptr + symlen) % twosym][currsymbol];
    	n = Math.sqrt(nR * nR + nI * nI);
    	
        sig = decayavg(sig, s, s - sig > 0 ? 4 : 20);
        noise = decayavg(noise, n, 64);

        if (noise != 0.0) {
            s2n = 20 * Math.log10(sig / noise);
        } else {
            s2n = 0;
        }
        // To partially offset the increase of noise by (THORNUMTONES -1)
        // in the noise calculation above, 
        // add 15*log10(THORNUMTONES -1) = 18.4, and multiply by 6
        metric = 6 * (s2n + 18.4);
        // s2n reporting: re-calibrate
        s2n_metric = metric * 2.5 - 70;
        s2n_metric =  s2n_metric < 0 ? 0 : s2n_metric > 100 ? 100 : s2n_metric;

        metric = metric < 0 ? 0 : metric > 100 ? 100 : metric;

        //	display_metric(metric);
        Processor.avgsnr = decayavg(Processor.avgsnr, s2n_metric, 20);

        //Every so often update the frequency offset display
        if (snrupdater < 0) {
        	snrupdater = 49;
            AndPskmail.mHandler.post(AndPskmail.updatesignalquality);
        } else snrupdater --;

        //System.out.println(metric);


    }

    int rxProcess(short[] buf8K, int len8K, double[] buf12k, int len12K) {

//VK2ETA Debug FIX       Complex z = new Complex();
    	double[] zrefR = new double[2];
    	double[] zrefI = new double[2];
    	double zR, zI;

//        Complex z = new Complex();
//        Complex[] zp = new Complex[symlen];
//        Complex[] bins = new Complex[numbins];

//        Complex[] zarray = new Complex[symlen];

        int n = 0;
        int bufptr = 0;

        if (filter_reset) {
            reset_filters();
        }

//VK2ETA: only execute once on change of slowcpu from false to true
//	if (slowcpu != false) {
//		slowcpu = true;
//      reset_filters();
//	}

        /*
        //VK2ETA debug only: refill the buffer with a sine wave of the audio frequency for step by step debugging
        double ph = 0.0;
        for (int jj = 0; jj < len; jj++) {
        buf[jj] = (300.0 * Math.cos(ph));
        ph -= TWOPI * frequency / samplerate;
        if (ph > TWOPI) ph -= TWOPI;
        if (ph < -TWOPI) ph += TWOPI;
        }
         */
        int len = (samplerate == 8000) ? len8K : len12K;

        while (len >= 0) {
// create analytic signal at first IF
        	if (samplerate == 8000) {
                zrefR[0] = (double) buf8K[bufptr];
        	} else {
                zrefR[0] = buf12k[bufptr];
        	}
            zrefI[0] = zrefR[0];

//VK2ETA: We need an array as in Java it is passed by reference, not by value as is a 
//    scalar (in=zref[0], out=zref[1])
            hilbert.run(zrefR, zrefI);
			//move across for next processing
			zrefR[0] = zrefR[1];
			zrefI[0] = zrefI[1];
            mixer(0, zrefR, zrefI);

//VK2ETA: not yet          if (THOR_FILTER) {   // tbd
// filter using fft convolution
//              n = fft.run(zref[1], zp); //<- needs resolving RC
//                zp = fftfilt.fftout;
//            } else {
//                zarray[0] = zref[1];
//                zp = zarray;
                n = 1;
                zR = zrefR[1];
                zI = zrefI[1];
//            }

            if (n > 0) {
                for (int i = 0; i < n; i++) {
// process THORMAXFFTS sets of sliding FFTs spaced at 1/THORMAXFFTS bin intervals each of which
// is a matched filter for the current symbol length
                    for (int k = 0; k < paths; k++) {
// shift in frequency to base band for the sliding DFTs
            			//reset at each pass
            			zrefR[0] = zR;
            			zrefI[0] = zI;
//                      z = mixer(k + 1, zp[i]);
                        mixer(k + 1, zrefR, zrefI);

//                        bins = binsfft[k].run(zref);
                        binsfft[k].run(zrefR[1],zrefI[1]);

// copy current vector to the pipe interleaving the FFT vectors
                        if (paths > 1) {
                        	for (int j = 0; j < numbins; j++) {
                        		//                            pipe[pipeptr][k + paths * j] = bins[j];
                        		pipeR[pipeptr][k + paths * j] = binsfft[k].binsR[j];
                        		pipeI[pipeptr][k + paths * j] = binsfft[k].binsI[j];
                        	}
                        } else { //faster copy for slow cpu option
                        	System.arraycopy(binsfft[k].binsR, 0, pipeR[pipeptr], 0, numbins);
                        	System.arraycopy(binsfft[k].binsI, 0, pipeI[pipeptr], 0, numbins);
                        }
                    }

                    if (--synccounter <= 0) {
                        synccounter = symlen;
                        currsymbol = harddecode();
//VK2ETA not used                        currmag = pipe[pipeptr][currsymbol].abs();
//                        double currmagR = pipeR[pipeptr][currsymbol];
//                        double currmagI = pipeI[pipeptr][currsymbol];
//                        currmag = Math.sqrt(currmagR * currmagR + currmagI * currmagI);

                        eval_s2n();
                        decodesymbol();
                        synchronize();
                        prev2symbol = prev1symbol;
                        prev1symbol = currsymbol;
//VK2ETA not used                         prev2mag = prev1mag;
//VK2ETA not used                         prev1mag = currmag;
                    }
                    pipeptr++;
                    if (pipeptr >= twosym) {
                        pipeptr = 0;
                    }
                }
            }
            --len;
            ++bufptr;
        }

        return 0;
    }

    double decayavg(double average, double input, double weight) {
        if (weight <= 1.0) {
            return input;
        }
        return input * (1.0 / weight) + average * (1.0 - (1.0 / weight));
    }
}
