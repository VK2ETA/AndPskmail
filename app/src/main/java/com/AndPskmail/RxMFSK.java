/*
 * Copyright (C) 2011 Rein Couperus (PA0R)
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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author rein
 */

public class RxMFSK extends Thread {

    private static int snrupdater = 49;
	modemmodeenum mode ;
	int samplerate = 8000;
	int symlen =  1024;
	int symbits =    5;
	int basetone = 128;
	int numtones = 32;
	double tonespacing;
	double basefreq;
	sfft binsfft;
	FirFilter hbfilt;
	FirFilter bpfilt;
	Cmovavg syncfilter;
	private double bandwidth;
	//android    Complex[][] pipe;
	double[][] pipeR;
	double[][] pipeI;
	int MAX_SYMBOLS = 32;
	private ViterbiEncoder enc = null;
	static final int K = 7;
	static final int POLY1 = 0x6d;
	static final int POLY2 = 0x4f;
	private Viterbi dec1;
	//2nd Viterbi decoder and 2 receive de-interleaver for comparison
	private Viterbi dec2;
	private interleave Rxinlv;
	private interleave Rxinlv2;
//	private interleave Txinlv;
//	private int bitshreg;
//	private int bitstate;
	int currsymbol;
	int prev1symbol;
	int prev2symbol;
	double prev2vectorR;
	double prev2vectorI;
	double prev1vectorR;
	double prev1vectorI;

	double maxval;
	double prevmaxval;

	double met1;
	double met2;

	double s2n;
	double sig;
	double noise;
	double afcmetric;
	boolean	staticburst;

	double currfreq;

	int counter;
	int synccounter;
	int AFC_COUNT;

	static private int[] symbolpair;
	boolean symcounter;
	double phaseacc = 0.0;
	int pipeptr = 0;
	double metric = 0.0;
	int datashreg = 0;
	int symbolbit;
	boolean s2n_valid = false;
	double s2n_sum = 0.0;
	double s2n_sum2 = 0.0;
	double s2n_ncount = 0;
	double s2n_metric = 0;
	boolean dcd = false;
	static final double TWOPI = 2 * Math.PI;
	static double freqerr;
	static boolean sigsearch = false;
	//android        private Complex currvector;
	private double currvectorR;
	private double currvectorI;
	//android       private ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
	//    private byte[] receiveBytes;
	public int running = 0;
	final Lock lock = new ReentrantLock();


	public RxMFSK(modemmodeenum mode)
	{

		changemode(mode);

		snrupdater = 49;

	}


	public void changemode(modemmodeenum mode)
	{

		double bw, cf, flo, fhi;
		symbolpair = new int[2];

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
			symbits =    4;
			basetone = 16;
			numtones = 16;
			break;

		default:
			//MFSK16
			samplerate = 8000;
			symlen =  512;
			symbits =   4;
			basetone = 64;
			numtones = 16;
			break;
		}

		tonespacing = (double) samplerate / symlen;
		basefreq = 1.0 * samplerate * basetone / symlen;

		binsfft = new sfft (symlen, basetone, basetone + numtones);       

		hbfilt = new FirFilter();
		hbfilt.init_hilbert(37, 1);

		syncfilter = new Cmovavg(8);
		//	
		//	for (int i = 0; i < SCOPESIZE; i++)
		//		vidfilter[i] = new Cmovavg(16);

		//android	pipe = new Complex[2 * symlen][MAX_SYMBOLS];
		pipeR = new double[2 * symlen][MAX_SYMBOLS];
		pipeI = new double[2 * symlen][MAX_SYMBOLS];
		//android    Complex Czero = new Complex(0.0,0.0);

		for (int i = 0; i < 2 * symlen; i++) {
			for (int j = 0; j < MAX_SYMBOLS; j++){
				pipeR[i][j] = 0.0;
				pipeI[i][j] = 0.0; //android
			}
		}        

		enc	= new ViterbiEncoder(K, POLY1, POLY2);
		dec1	= new Viterbi (K, POLY1, POLY2);
		dec2	= new Viterbi (K, POLY1, POLY2);

		dec1.settraceback (45);
		dec2.settraceback (45);
		dec1.setchunksize (1);
		dec2.setchunksize (1);

//		Txinlv = new interleave (symbits, interleave.INTERLEAVE_FWD);
		Rxinlv = new interleave (symbits, interleave.INTERLEAVE_REV);

		bw = (numtones - 1) * tonespacing;
		cf = basefreq + bw / 2.0;

		flo = (cf - bw/2 - 2 * tonespacing) / samplerate;
		fhi = (cf + bw/2 + 2 * tonespacing) / samplerate;

		bpfilt = new FirFilter();
		bpfilt.init_bandpass (127, 1, flo, fhi);

		//	scopedata.alloc(symlen * 2);

		//	int fragmentsize = symlen;
		bandwidth = (numtones - 1) * tonespacing;

		//	boolean startpic = false;
		//	boolean abortxmt = false;
		//	boolean stopflag = false;

//		bitshreg = 0;
//		bitstate = 0;
		phaseacc = 0;
		pipeptr = 0;
		metric = 0;
		prev1symbol = 0;
		prev2symbol = 0;

		symbolpair[0] = 0;
		symbolpair[1] = 0;

		// picTxWin and picRxWin are created once to support all instances of mfsk
		//	if (!picTxWin) createTxViewer();
		//	if (!picRxWin)
		//		createRxViewer();
		//	activate_mfsk_image_item(true);
		afcmetric = 0.0;
		datashreg = 1;

		//	init();
		rxInit();

	}

	void  rxInit()
	{
		//	rxstate = RX_STATE_DATA;
//JD debug array out of bound error		synccounter = 0;
		synccounter = symlen;
		//JD debug
		symcounter = false;
		met1 = 0.0;
		met2 = 0.0;
		counter = 0;

		reset_afc();
		s2n = 0.0;
		//	memset(picheader, ' ', PICHEADER - 1);
		//	picheader[PICHEADER -1] = 0;
		//	put_MODEstatus(mode);
		syncfilter.reset();
		staticburst = false;

		s2n_valid = false;
	}


	//=====================================================================
	// receive processing
	//=====================================================================


	void s2nreport()
	{
		//	modem::s2nreport();
		s2n_valid = false;
	}

	//bool mfsk::check_picture_header(char c)
	//{
	//	char *p;
	//
	//	if (c >= ' ' && c <= 'z') {
	//		memmove(picheader, picheader + 1, PICHEADER - 1);
	//		picheader[PICHEADER - 2] = c;
	//	}
	//	picW = 0;
	//	picH = 0;
	//	color = false;
	//
	//	p = strstr(picheader, "Pic:");
	//	if (p == NULL)
	//		return false;
	//
	//	p += 4;
	//	
	//	if (*p == 0) return false;
	//
	//	while ( *p && isdigit(*p))
	//		picW = (picW * 10) + (*p++ - '0');
	//
	//	if (*p++ != 'x')
	//		return false;
	//
	//	while ( *p && isdigit(*p))
	//		picH = (picH * 10) + (*p++ - '0');
	//
	//	if (*p == 'C') {
	//		color = true;
	//		p++;
	//	}
	//	if (*p == ';') {
	//		if (picW == 0 || picH == 0 || picW > 4095 || picH > 4095)
	//			return false;
	//		RXspp = 8;
	//		return true;
	//	}
	//	if (*p == 'p')
	//		p++;
	//	else
	//		return false;
	//	if (!*p) 
	//		return false;
	//	RXspp = 8;
	//	if (*p == '4') RXspp = 4;
	//	if (*p == '2') RXspp = 2;
	//	p++;
	//	if (!*p) 
	//		return false;
	//	if (*p != ';')
	//		return false;
	//	if (picW == 0 || picH == 0 || picW > 4095 || picH > 4095)
	//		return false;
	//	return true;
	//}

	//void mfsk::recvpic(complex z)
	//{
	//	int byte;
	//	picf += (prevz % z).arg() * samplerate / TWOPI;
	//	prevz = z;
	//
	//	if (RXspp < 8 && progdefaults.slowcpu == true)
	//		return;
	//		
	//	if ((counter % RXspp) == 0) {
	//		picf = 256 * (picf / RXspp - basefreq) / bandwidth;
	//		byte = (int)CLAMP(picf, 0.0, 255.0);
	//		if (reverse)
	//			byte = 255 - byte;
	//		
	//		if (color) {
	//			pixelnbr = rgb + row + 3*col;
	//			REQ(updateRxPic, byte, pixelnbr);
	//			if (++col == picW) {
	//				col = 0;
	//				if (++rgb == 3) {
	//					rgb = 0;
	//					row += 3 * picW;
	//				}
	//			}
	//		} else {
	//			for (int i = 0; i < 3; i++)
	//				REQ(updateRxPic, byte, pixelnbr++);
	//		}
	//		picf = 0.0;
	//
	//		int n = picW * picH * 3;
	//		if (pixelnbr % (picW * 3) == 0) {
	//			int s = snprintf(mfskmsg, sizeof(mfskmsg),
	//					 "Recv picture: %04.1f%% done",
	//					 (100.0f * pixelnbr) / n);
	//			print_time_left( (n - pixelnbr ) * 0.000125 * RXspp , 
	//					mfskmsg + s,
	//					sizeof(mfskmsg) - s, ", ", " left");
	//			put_status(mfskmsg);
	//		}
	//	}
	//}

	void recvchar(int c)
	{
		if (c == -1 || c == 0)
			return;

		//	if (check_picture_header(c) == true) {
		//// 44 nulls at 8 samples per pixel
		//// 88 nulls at 4 samples per pixel
		//// 176 nulls at 2 samples per pixel
		//		counter = 352; 
		//		if (symbolbit == symbits) counter += symlen;
		//		rxstate = RX_STATE_PICTURE_START;
		//		picturesize = RXspp * picW * picH * (color ? 3 : 1);
		//		pixelnbr = 0;
		//		col = 0;
		//		row = 0;
		//		rgb = 0;
		//		memset(picheader, ' ', PICHEADER - 1);
		//		picheader[PICHEADER -1] = 0;		
		//	}

		Modem.rxblock((char) c);


		/*

//	if (progdefaults.Pskmails2nreport && (mailserver || mailclient)) {
		if ((c == '\001') && !s2n_valid) {
			// starts collecting s2n from first SOH in stream (since start of RX)
			s2n_valid = true;
			s2n_sum = 0.0;
                        s2n_sum2 = 0.0;
                        s2n_ncount = 0.0;
		}
		if (s2n_valid) {
			s2n_sum += s2n_metric;
			s2n_sum2 += (s2n_metric * s2n_metric);
			s2n_ncount++;
			if (c == '\004')
				s2nreport();
		}
//	}

		 */
	}

	void recvbit(int bit)
	{
		int c;

		//VK2ETA debug Fix        datashreg = (datashreg & 0xFFFF) | (bit == 0? 1 : 0);
		datashreg = ((datashreg & 0xFFFF) << 1) | (bit == 0? 0 : 1);
		if ((datashreg & 7) == 1) {
			c = mfskVaricode.varidec(datashreg >> 1);
			recvchar(c);
			datashreg = 1;
		}
	}

	void decodesymbol(int symbol)
	{
		int c = 0; 
		int met = 0;

		symbolpair[0] = symbolpair[1];
		symbolpair[1] = symbol;

		symcounter = symcounter ? false : true;

		// only modes with odd number of symbits need a vote
		if (symbits == 5 || symbits == 3) { // could use symbits % 2 == 0
			if (symcounter) {
				if ((c = dec1.decode(symbolpair, met)) == -1)
					return;
				//JD no pointer in java, programmed global variable instead
				met1 = decayavg(met1, dec1.Vmetric, 32);
				if (met1 < met2)
					return;
				metric = met1 / 1.5;
			} else {
				if ((c = dec2.decode(symbolpair, met)) == -1)
					return;
				//JD no pointer in java, programmed global variable instead
				met2 = decayavg(met2, dec2.Vmetric, 32);
				if (met2 < met1)
					return;
				metric = met2 / 1.5;
			}
		} else {
			if (symcounter) return;
			if ((c = dec2.decode(symbolpair, met)) == -1)
				return;
			//JD no pointer in java, programmed global variable instead		met2 = decayavg(met2, met, 32);
			met2 = decayavg(met2, dec2.Vmetric, 32);
			metric = met2 / 1.5;
		}

		// s2n reporting: re-calibrate
		s2n_metric = metric * 3 - 42;
		if (s2n_metric < 0.0) s2n_metric = 0.0;
		if (s2n_metric > 100.0) s2n_metric = 100.0;

        Processor.avgsnr = decayavg(Processor.avgsnr, s2n_metric, 20);

        //Every so often update the frequency offset display
        if (snrupdater < 0) {
        	snrupdater = 49;
            AndPskmail.mHandler.post(AndPskmail.updatesignalquality);
        } else snrupdater --;

        if ( metric <= Processor.m.squelch ) { 
        	return;
        }
		recvbit(c);

	}


	double decayavg(double average, double input, double weight) {
		if (weight <= 1.0)
			return input;
		return input * (1.0 / weight) + average * (1.0 - (1.0 / weight));
	}

	void softdecode(double[] binsR, double[] binsI)
	{
		double binmag;
		double sum;
		double[] b = new double[symbits];
		int[] symbols = new int[symbits];
		int i, j, k;

		for (i = 0; i < symbits; i++)
			b[i] = 0.0;

		// avoid divide by zero later
		sum = 1e-10;
		// gray decode and form soft decision samples
		for (i = 0; i < numtones; i++) {
			j = misc.graydecode(i);

			//		if (reverse)
			//			k = (numtones - 1) - i;
			//		else
			k = i;

			//		binmag = bins[k].abs();
			binmag = binsR[k] * binsR[k] + binsI[k] * binsI[k];
			binmag = Math.sqrt(binmag);

			for (k = 0; k < symbits; k++) {
				int m = 1 << (symbits - k - 1);
				if ((j & m) == 0){
					//JD debug                       b[k] += binmag;
					b[k] -= binmag;
				} else {
					//JD debug                        b[k] -= binmag;
					b[k] += binmag;
				}

			}
			sum += binmag;
		}

		// shift to range 0...255
		for (i = 0; i < symbits; i++) {
			if (staticburst) {
				//JD debug                    symbols[i] = 0;  // puncturing
				symbols[i] = 128;  // puncturing
			} else {
				int s = (int) (128.0 + (b[i] / sum * 128.0));
				if (s > 255){
					s = 255;
				} else if (s < 0) {
					s = 0;
				}

				symbols[i] = s;
			}
		}

		Rxinlv.symbols(symbols);

		for (i = 0; i < symbits; i++) {
			symbolbit = i + 1;
			decodesymbol(symbols[i]);
		}
	}

	public void mixer(double[] inR, double[] inI, double f)
	{
		//    	Complex z;
		//		double[] z = new double[2];
		double indR = inR[0];
		double indI = inI[0];
		double zR, zI;

		// Basetone is a nominal 1000 Hz 
		f -= tonespacing * basetone + bandwidth / 2;	

		//	Complex m = new Complex();
		//    m.setReal(Math.cos(phaseacc));
		//    m.setImag(Math.sin(phaseacc));
		//    z = in.multi(m);
		zR = Math.cos(phaseacc);
		zI = Math.sin(phaseacc);
		inR[1] = indR * zR - indI * zI;//real*r.real - imag*r.imag
		inI[1] = indR * zI + indI * zR;//real*r.imag + imag*r.real

		phaseacc -= TWOPI * f / samplerate;
		if (phaseacc > TWOPI) phaseacc -= TWOPI;
		if (phaseacc < -TWOPI) phaseacc += TWOPI;

		//	return z; //returned in inR and inI position 1

	}

	// finds the tone bin with the largest signal level
	// assumes that will be the present tone received 
	// with NO CW inteference

	//android    public int harddecode(Complex[] in)
	public int harddecode(double[] inR, double[] inI)
	{
		double x, max = 0.0, avg = 0.0;
		int i, symbol = 0;
		int burstcount = 0;

		for (i = 0; i < numtones; i++){
			//VK2ETA possible optimisation here as we re-calculate this below
			avg += Math.sqrt(inR[i] * inR[i] + inI[i] * inI[i]);
		}
		avg /= numtones;

		if (avg < 1e-20) avg = 1e-20;

		for (i = 0; i < numtones; i++) {
			//    		x = in[i].norm();
			x = Math.sqrt(inR[i] * inR[i] + inI[i] * inI[i]);
			if ( x > max) {
				max = x;
				symbol = i;
			}
			if (x > 2.0 * avg) burstcount++;
		}

		staticburst = (burstcount == numtones);

		if (!staticburst)
			afcmetric = 0.95*afcmetric + 0.05 * (2 * max / avg);
		else
			afcmetric = 0.0;

		return symbol;
	}

	//void mfsk::update_syncscope()
	//{
	//	int j;
	//	int pipelen = 2 * symlen;
	//	memset(scopedata, 0, 2 * symlen * sizeof(double));
	//	if (!progStatus.sqlonoff || metric >= progStatus.sldrSquelchValue)
	//		for (unsigned int i = 0; i < SCOPESIZE; i++) {
	//			j = (pipeptr + i * pipelen / SCOPESIZE + 1) % (pipelen);
	//			scopedata[i] = vidfilter[i]->run(pipe[j].vector[prev1symbol].mag());
	//		}
	//	set_scope(scopedata, SCOPESIZE);
	//
	//	scopedata.next(); // change buffers
	//	snprintf(mfskmsg, sizeof(mfskmsg), "s/n %3.0f dB", 20.0 * log10(s2n));
	//	put_Status1(mfskmsg);
	//}

	public void synchronize()
	{
		int i, j;
		double syn = -1;
		double val, max = 0.0;

		if (currsymbol == prev1symbol)
			return;
		if (prev1symbol == prev2symbol)
			return;

		j = pipeptr;

		for (i = 0; i < 2 * symlen; i++) {

			//JD debug		val = (pipe[j][prev1symbol]).norm();
			//		val = (pipe[j][prev1symbol]).abs();
			double vR = pipeR[j][prev1symbol];
			double vI = pipeI[j][prev1symbol];
			val = Math.sqrt(vR * vR + vI * vI);

			if (val > max) {
				max = val;
				syn = i;
			}

			j = (j + 1) % (2 * symlen);
		}

		syn = syncfilter.run(syn);

		synccounter += (int) Math.floor((syn - symlen) / numtones + 0.5);

		//	update_syncscope();
	}

	void reset_afc() {
		freqerr = 0.0;
		syncfilter.reset();
		return;
	}


void afc()
{
	//	Complex z;
	double zR, zI;
	//	Complex prevvector;
	double afcPrevvectorR, afcPrevvectorI;

	double f, f1;
	double ts = tonespacing / 4;

	if (sigsearch) {
		reset_afc();
		sigsearch = false;
	}

//jd debug FIX this...	if (staticburst || metric < Main.squelch || afcmetric < 3.0)
	if (staticburst || metric < 20 || afcmetric < 3.0)
		return;
	if (currsymbol != prev1symbol)
		return;
	if (prev1symbol != prev2symbol)
		return;

	if (pipeptr == 0) {
		//		prevvector = pipe[2*symlen - 1][currsymbol];
		afcPrevvectorR = pipeR[2*symlen - 1][currsymbol];
		afcPrevvectorI = pipeI[2*symlen - 1][currsymbol];

	}
	else {
		//		prevvector = pipe[pipeptr - 1][currsymbol];
		afcPrevvectorR = pipeR[pipeptr - 1][currsymbol];
		afcPrevvectorI = pipeI[pipeptr - 1][currsymbol];
	}

	//	z = prevvector.dividerest(currvectorR) ; 
	zR = afcPrevvectorR * currvectorR + afcPrevvectorI * currvectorI;//z.re = re * y.re + im * y.im;
	zI = afcPrevvectorR * currvectorI - afcPrevvectorI * currvectorR;//z.im = re * y.im - im * y.re;

//	f = z.arg() * samplerate / TWOPI;
	f = Math.atan2(zI, zR) * samplerate / TWOPI;

	f1 = tonespacing * (basetone + currsymbol);	

	if ( Math.abs(f1 - f) < ts) {
		freqerr = decayavg(freqerr, (f1 - f), 32);
		//		set_freq(frequency - freqerr);
		Processor.m.rxFrequency -= freqerr;
	}

}


	void eval_s2n() {
		//sig = pipe[pipeptr][currsymbol].abs();
		double sigR, sigI;
		sigR = pipeR[pipeptr][currsymbol];
		sigI = pipeI[pipeptr][currsymbol];
		sig = Math.sqrt(sigR * sigR + sigI * sigI);
		//noise = (numtones -1) * pipe[pipeptr][prev2symbol].abs();
		double noiseR, noiseI;
		noiseR = pipeR[pipeptr][prev2symbol];	
		noiseI = pipeI[pipeptr][prev2symbol];	
		noise = (numtones -1) * Math.sqrt(noiseR * noiseR + noiseI * noiseI);
		if (noise > 0) 	s2n = decayavg ( s2n, sig / noise, 64 );
	}

	//android int rx_process( double[] buf, int leng)
	int rxProcess( short[] so, int buflength)
	{
		double[] zzR = new double[2];
		double[] zzI = new double[2];
/*
		//JD debug: refill the buffer with a sine wave of the audio frequency for step by step debugging
		double ph = 0.0;
		for (int jj = 0; jj < buflength; jj++) {
			so[jj] = (short) (300 * Math.cos(ph));
			ph -= TWOPI * frequency / samplerate;
			if (ph > TWOPI) ph -= TWOPI;
			if (ph < -TWOPI) ph += TWOPI;
		}

*/		

		int ii = 0;
		while (buflength-- > 0) {

			// create analytic signal...
			zzR[0] = (double) so[ii++];
			zzI[0] = zzR[0];

			hbfilt.run ( zzR, zzI);
			// shift in frequency to the base freq
			//move across for next processing
			zzR[0] = zzR[1];
			zzI[0] = zzI[1];
			mixer(zzR, zzI, Processor.m.rxFrequency);

			// bandpass filter around the shifted center frequency
			// with required bandwidth 

			//No additional filtering on Android version
			// Move across for next processing
			//		zzR[0] = zzR[1];
			//		zzI[0] = zzI[1];
			//					bpfilt.run (zzR, zzI);


			// Returns pointer to first frequency of interest
			// Use result in position 1 of zz		fbins = binsfft.run (z);
			binsfft.run (zzR[1], zzI[1]); //result is in binsfft.binsR[] and binsfft.binsI[]

			//prt(fbins[0]);
			//                System.arraycopy(fbins, basetone, pipes[pipeptr].vectors, 0, numtones);
			System.arraycopy(binsfft.binsR, 0, pipeR[pipeptr], 0, numtones);
			System.arraycopy(binsfft.binsI, 0, pipeI[pipeptr], 0, numtones);

			if (--synccounter <= 0) {

				synccounter = symlen;

				currsymbol = harddecode(binsfft.binsR, binsfft.binsI); 

				currvectorR = binsfft.binsR[currsymbol];	
				currvectorI = binsfft.binsI[currsymbol];	

				softdecode(binsfft.binsR, binsfft.binsI);

				// symbol sync 
				synchronize();

				// frequency tracking 
//Jd debug				afc();

				eval_s2n();

				prev2symbol = prev1symbol;
				prev2vectorR = prev1vectorR; //JD not used
				prev2vectorI = prev1vectorI; //JD not used
				prev1symbol = currsymbol;
				prev1vectorR = currvectorR; //JD not used
				prev1vectorI = currvectorI; //JD not used
				//			prevmaxval = maxval;
			}
			pipeptr = (pipeptr + 1) % (2 * symlen);
		}

		return 0;
	}

}

