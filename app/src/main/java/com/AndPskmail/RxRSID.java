/*
 * RxRSID.java  
 *   
 * Copyright (C) 2011 John Douyere (VK2ETA)  
 * Translated and adapted into Java class from Fldigi
 * as per Fldigi code from Dave Freese, W1HKJ and Stelios Bounanos, M0GLD
 * 
 * This program is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  
 * GNU General Public License for more details.  
 *   
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 */

	// ----------------------------------------------------------------------------
	// Tone separation: 10.766Hz
	// Integer tone separator (x 16): 172
	// Error on 16 tones: 0.25Hz

	// Tone duration: 0.093 sec
	// Tone duration, #samples at 8ksps: 743
	// Error on 15 tones: negligible

	// 1024 samples -> 512 tones
	// 2048 samples, second half zeros

	// each 512 samples new FFT
	// ----------------------------------------------------------------------------


package com.AndPskmail;


public class RxRSID {

	
	static final int RSID_SAMPLE_RATE = 11025;

	static final int RSID_FFT_SAMPLES = 512;
	//Made public for waterfall test
	public static final int RSID_FFT_SIZE = 1024;
	static final int RSID_ARRAY_SIZE = (RSID_FFT_SIZE * 2);

	static final int RSID_NSYMBOLS = 15;
	static final int RSID_RESOL = 2;
	static final int RSID_NTIMES = (RSID_NSYMBOLS * RSID_RESOL);
	static final int RSID_HASH_LEN = 256;
	static final double RSID_PRECISION = 2.7; // detected frequency precision in Hz

//	static final int RSID_SYMLEN = (1024 / RSID_SAMPLE_RATE); // 0.09288 // duration of each rsid symbol
//	static final int RSID_NONE = -1;

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


// Span of FFT bins, in which the RSID will be searched for
	int		nBinLow;
	int		nBinHigh;
//    private float[] aInputSamples = new float[RSID_ARRAY_SIZE];
    private double[] aInputSamples = new double[RSID_ARRAY_SIZE];
	private double[] fftwindow = new double[RSID_ARRAY_SIZE];
	private double[] aFFTReal = new double[RSID_ARRAY_SIZE];
	//JD made public static for waterfall test
	public static double[] aFFTAmpl = new double[RSID_FFT_SIZE];
	Cfft	rsfft;
	private double[] blankFFT_SizeDoubles = new double[RSID_FFT_SIZE]; 

	// Hashing tables
	short[] aHashTable1 = new short[RSID_HASH_LEN];
	short[] aHashTable2 = new short[RSID_HASH_LEN];

	boolean		bPrevTimeSliceValid;
	int		iPrevDistance;
	int		iPrevBin;
	int		iPrevSymbol;
	int		iTime; // modulo RSID_NTIMES
	int[][]	aBuckets = new int[RSID_NTIMES][RSID_FFT_SIZE];

	int		DistanceOut;
	int		MetricsOut;

	private short[] pCodes;

	//Resampling
	static int inptr;
	
	//Moved to Modem method
	//Waterfall interaction
	//public static double[] WaterfallAmpl = new double[RSID_FFT_SIZE];
	//public static boolean newAmplReady = false;
	public static boolean processingamp = false;
	
	private static int fftCounter = 0;

    //Escaped RSIDs for new modes
    static final int escapeRsid = 6;
    long lastEscapeTime = 0L;
    double lastEscapeFreq;
    boolean foundRsidEscape = false;


    //	static const RSIDs  rsid_ids[];
    private final static int[][] rsid_ids = {

            { escapeRsid, 1,   2,   4,  57,  60, 126, 137, 138, 143, 145, 147, 173, 183, 186, 187, 204, 553,  0},
            {          0,'e', 'a', '9', '2', 'f', '8', '1', 'g', 'd', '3', '4', '7', 'b', '5', '6', 'c', 'e', 0}

            // NONE must be the last element              \
            //ELEM_(0, NONE, NUM_MODES)

        /*


        ELEM_(1, BPSK31, MODE_PSK31)					\
        ELEM_(2, BPSK63, MODE_PSK63)					\
        ELEM_(4, BPSK125, MODE_PSK125)                  \
        ELEM_(126, BPSK250, MODE_PSK250)                \
        ELEM_(173, BPSK500, MODE_PSK500)                \
                                                        \
        ELEM_(183, PSK125R, MODE_PSK125R)               \
        ELEM_(186, PSK250R, MODE_PSK250R)               \
        ELEM_(187, PSK500R, MODE_PSK500R)               \
                                                        \
        ELEM_(136, THOR_4, MODE_THOR4)                  \
        ELEM_(137, THOR_8, MODE_THOR8)                  \
        ELEM_(138, THOR_16, MODE_THOR16)                \
        ELEM_(139, THOR_5, MODE_THOR5)                  \
        ELEM_(143, THOR_11, MODE_THOR11)                \
        ELEM_(145, THOR_22, MODE_THOR22)                \

        ELEM_(60, MFSK8, MODE_MFSK8)                    \
        ELEM_(57, MFSK16, MODE_MFSK16)                  \
        ELEM_(147, MFSK32, MODE_MFSK32)                 \
        ELEM_(148, MFSK11, MODE_MFSK11)                 \
        ELEM_(152, MFSK22, MODE_MFSK22)                 \

		//Re-use contestia codes since these are useless for ARQ due to their limited character set
        ELEM_(204, CONTESTIA_4_125, MODE_CONTESTIA)     \ //Re-allocated to MFSK64
        ELEM_(55,  CONTESTIA_4_250, MODE_CONTESTIA)     \
        ELEM_(54,  CONTESTIA_4_500, MODE_CONTESTIA)     \
        ELEM_(255, CONTESTIA_4_1000, MODE_CONTESTIA)    \
        ELEM_(254, CONTESTIA_4_2000, MODE_CONTESTIA)    \
                                                        \

 */
    };


    //	const int cRsId::rsid_ids_size = sizeof(rsid_ids)/sizeof(*rsid_ids) - 1;
    static final int rsid_ids_size = rsid_ids[0].length - 1;


    private static void Encode(int code, short[] temprsid)
	{
		temprsid[0] = (short) (code >> 8);
		temprsid[1] = (short) ((code >> 4) & 0x0f);
		temprsid[2] = (short) (code & 0x0f);
		for (int i = 3; i < RSID_NSYMBOLS; i++)
			temprsid[i] = 0;
		for (int i = 0; i < 12; i++) {
			for (int j = RSID_NSYMBOLS - 1; j > 0; j--)
				temprsid[j] = (short) (temprsid[j - 1] ^ Squares[(temprsid[j] << 4) + indices[i]]);
			temprsid[0] = (short) Squares[(temprsid[0] << 4) + indices[i]];
		}
	}
	
	
	//Constructor
	public RxRSID() {

/* JD no need as we use simple resampling in the receive class
 		src_state = src_new(progdefaults.sample_converter, 1, &error);
		if (error) {
			LOG_ERROR("src_new error %d: %s", error, src_strerror(error));
			abort();
		}
		src_data.end_of_input = 0;
*/
		//used for fast reset of arrays by copying that blank one later on
		misc.memset(blankFFT_SizeDoubles, 0.0);
		
		reset();

		rsfft = new Cfft(RSID_FFT_SIZE);

		misc.memset(aHashTable1, (short) 255);
		misc.memset(aHashTable2, (short) 255);
		misc.memset(fftwindow, 0.0);
//		BlackmanWindow(fftwindow, RSID_FFT_SIZE);
//		HammingWindow(fftwindow, RSID_FFT_SIZE);
//		HanningWindow(fftwindow, RSID_FFT_SIZE);
		misc.RectWindow(fftwindow, RSID_FFT_SIZE);
		
//		pCodes = new unsigned char[rsid_ids_size * RSID_NSYMBOLS];
		pCodes = new short[rsid_ids_size * RSID_NSYMBOLS];
//		memset(pCodes, 0, rsid_ids_size * RSID_NSYMBOLS);
		misc.memset(pCodes, (short) 0);

		// Initialization  of assigned mode/submode IDs.
		// HashTable is used for finding a code with lowest Hamming distance.

		short[] temprsid = new short[RSID_NSYMBOLS];
	    //		unsigned char* c;
		int c;
		int hash1, hash2;
		for (int i = 0; i < rsid_ids_size; i++) {
//			c = pCodes + i * RSID_NSYMBOLS;
			c = i * RSID_NSYMBOLS;
//			Encode(rsid_ids[i].rs, c);
			Encode(rsid_ids[0][i], temprsid);
			System.arraycopy(temprsid, 0, pCodes, c, RSID_NSYMBOLS);
//			hash1 = c[11] | (c[12] << 4);
			hash1 = pCodes[c + 11] | (pCodes[c + 12] << 4);
//			hash2 = c[13] | (c[14] << 4);
			hash2 = pCodes[c + 13] | (pCodes[c + 14] << 4);
//			aHashTable1[hash1] = i;
			aHashTable1[hash1] = (short) i;
//			aHashTable2[hash2] = i;
			aHashTable2[hash2] = (short) i;
		}

		nBinLow = RSID_RESOL + 1;
		nBinHigh = RSID_FFT_SIZE - 32;

	}

	void reset() {
		iPrevDistance = 99;
		bPrevTimeSliceValid = false;
		iTime = 0;
//		memset(aInputSamples, 0, sizeof(aInputSamples));
		misc.memset(aInputSamples, 0);
//		memset(aFFTReal, 0, sizeof(aFFTReal));
		misc.memset(aFFTReal, 0);
//		memset(aFFTAmpl, 0, sizeof(aFFTAmpl));
		misc.memset(aFFTAmpl, 0);
//		memset(aBuckets, 0, sizeof(aBuckets));
//VK2ETA: since done only once at initialisation, it can be brute force
// note: int[RSID_NTIMES][RSID_FFT_SIZE];
		for (int ii = 0; ii < RSID_NTIMES; ii++) {
			for (int jj = 0; jj < RSID_NTIMES; jj++) {
				aBuckets[ii][jj] = 0;	
			}
		}

/* JD not used		int error = src_reset(src_state);
		if (error)
			LOG_ERROR("src_reset error %d: %s", error, src_strerror(error));
		src_data.src_ratio = 0.0;
*/
//		inptr = aInputSamples + RSID_FFT_SAMPLES;
		inptr = RSID_FFT_SAMPLES;
	}


//	void CalculateBuckets(const double *pSpectrum, int iBegin, int iEnd)
	void CalculateBuckets(double[] pSpectrum, int iBegin, int iEnd)
	{
		double Amp = 0.0, AmpMax = 0.0;
		int iBucketMax = iBegin - RSID_RESOL;
		int j;

		for (int i = iBegin; i < iEnd; i += RSID_RESOL) {
			if (iBucketMax == i - RSID_RESOL) {
				AmpMax = pSpectrum[i];
				iBucketMax = i;
				for (j = i + RSID_RESOL; j < i + RSID_NTIMES + RSID_RESOL; j += RSID_RESOL) {
					Amp = pSpectrum[j];
					if (Amp > AmpMax) {
						AmpMax = Amp;
						iBucketMax = j;
					}
				}
			}
			else {
				j = i + RSID_NTIMES;
				Amp = pSpectrum[j];
				if (Amp > AmpMax) {
					AmpMax    = Amp;
					iBucketMax = j;
				}
			}
			aBuckets[iTime][i] = (iBucketMax - i) >> 1;
		}
	}



//	void receive(const float* buf, size_t len)
	void receive(double[] buf, int len) {
		int bufOffset = 0;
//VK2ETA test only	with fixed sample rate
		//double src_ratio = RSID_SAMPLE_RATE / active_modem->get_samplerate();
//		double src_ratio = (double) RSID_SAMPLE_RATE / 8000.0;
//		double srUnderRatio = 1 / src_ratio;
//		boolean resample = (Math.abs(src_ratio - 1.0) >= 0.1);
		int ns;

		while (len > 0) {
//			ns = inptr - aInputSamples;
			ns = inptr;
			if (ns >= RSID_FFT_SAMPLES) // inptr points to second half of aInputSamples
				ns -= RSID_FFT_SAMPLES;
			ns = RSID_FFT_SAMPLES - ns; // number of additional samples we need to call search()

//			if (resample) {
/* For the time being since we use only a small windows of frequencies
 * in the Android application (i.e. the window is much smaller than 
 * the fundamental frequency of the centre audio frequency), 
 * then harmonics created at multiples of the fundamental are of no importance.
 * Therefore we use a very simple resampling technique.
 * Note: this would NOT be applicable for a search of the ENTIRE 
 * audio passband (typically 2.5Khz) since the upper frequencies
 * are at multiples of the lowest ones.
 * 				
				if (src_data.src_ratio != src_ratio)
					src_set_ratio(src_state, src_data.src_ratio = src_ratio);
				src_data.data_in = const_cast<float*>(buf);
				src_data.input_frames = len;
				src_data.data_out = inptr;
				src_data.output_frames = ns;
				src_data.input_frames_used = 0;
				int error = src_process(src_state, &src_data);
				if (unlikely(error)) {
					LOG_ERROR("src_process error %d: %s", error, src_strerror(error));
					return;
				}
				inptr += src_data.output_frames_gen;
				buf += src_data.input_frames_used;
				len -= src_data.input_frames_used;
 */
			
/* NO RESAMPLING - done in Modem method

				int posin = 0;
				int nscount = 0;
				while ((posin < len) && (nscount < ns)) {
					 aInputSamples[inptr + nscount] = buf[bufOffset + posin];
					 nscount++;
 					 posin = (int) (nscount * srUnderRatio);
				}
				inptr += nscount;
				bufOffset += posin;
				len -= posin;
*/

				ns = Math.min(ns, len);
//				memcpy(inptr, buf, ns * sizeof(*inptr));
				System.arraycopy(buf, bufOffset, aInputSamples, inptr, ns);
				inptr += ns;
				bufOffset += ns;
				len -= ns;
				
//			ns = inptr - aInputSamples;
				
			if (inptr == RSID_FFT_SAMPLES || inptr == RSID_FFT_SIZE)
				search(); // will reset inptr if at end of input
		}
	}

	void search()	{
		int[] SymbolOut = new int[1];
		int[] BinOut = new int[1];
		int[] iDistance = new int[1];//to pass as paramter to search method
		
/* VK2ETA not used here		if (progdefaults.rsidWideSearch) {
			nBinLow = RSID_RESOL + 1;
			nBinHigh = RSID_FFT_SIZE - 32;
		}
		else {
*/
		String frequencySTR = AndPskmail.myconfig.getPreference("AFREQUENCY","1000");
		double centerfreq = Integer.parseInt(frequencySTR);
		if (centerfreq < 500) centerfreq = 500.0;
		if (centerfreq > 2500) centerfreq = 2500.0;
		nBinLow = (int)((centerfreq  - 100.0 * RSID_RESOL) * 2048.0 / RSID_SAMPLE_RATE);
		nBinHigh = (int)((centerfreq  + 100.0 * RSID_RESOL) * 2048.0 / RSID_SAMPLE_RATE);
//		}

//			bool bReverse = !(wf->Reverse() ^ wf->USB());
//JD assume USB and not reverse mode in Fldigi
		boolean bReverse = false;
		if (bReverse) {
			nBinLow  = RSID_FFT_SIZE - nBinHigh;
			nBinHigh = RSID_FFT_SIZE - nBinLow;
		}

//		if (inptr == aInputSamples + RSID_FFT_SIZE) {
		if (inptr == RSID_FFT_SIZE) {
//			for (int i = 0; i < RSID_FFT_SIZE; i++)
//				aFFTReal[i] = aInputSamples[i];
			System.arraycopy(aInputSamples, 0, aFFTReal, 0, RSID_FFT_SIZE);
//			inptr = aInputSamples;
			inptr = 0;
		} else { // second half of aInputSamples is older
//			for (int i = RSID_FFT_SAMPLES; i < RSID_FFT_SIZE; i++)
//				aFFTReal[i - RSID_FFT_SAMPLES] = aInputSamples[i];
			System.arraycopy(aInputSamples, RSID_FFT_SAMPLES, aFFTReal, 0, RSID_FFT_SAMPLES);
//			for (int i = 0; i < RSID_FFT_SAMPLES; i++)
//				aFFTReal[i + RSID_FFT_SAMPLES] = aInputSamples[i];
			System.arraycopy(aInputSamples, 0, aFFTReal, RSID_FFT_SAMPLES, RSID_FFT_SAMPLES);
		}

		for (int i = 0; i < RSID_FFT_SIZE; i++) aFFTReal[i] *= fftwindow[i];
//		memset(aFFTReal + RSID_FFT_SIZE, 0, RSID_FFT_SIZE * sizeof(double));
//		Use the pre zero-filled array for a quick memset
		System.arraycopy(blankFFT_SizeDoubles, 0, aFFTReal, RSID_FFT_SIZE, RSID_FFT_SIZE);

		rsfft.rdft(aFFTReal);
		
//		memset(aFFTAmpl, 0, RSID_FFT_SIZE * sizeof(double));
//		Use the pre zero-filled array for a quick memset
		System.arraycopy(blankFFT_SizeDoubles, 0, aFFTAmpl, 0, RSID_FFT_SIZE);
		double Real, Imag;
		for (int i = 0; i < RSID_FFT_SIZE; i++) {
			Real = aFFTReal[2*i];
			Imag = aFFTReal[2*i + 1];
			if (bReverse) {
				aFFTAmpl[RSID_FFT_SIZE - 1 - i] = Real * Real + Imag * Imag;
			} else {
				aFFTAmpl[i] = Real * Real + Imag * Imag;
			}
		}
		
		//Copy to Waterfall array only if the previous data set has been used
		if (!Modem.newAmplReady) {
			fftCounter++;
			if (fftCounter > 1) { //Once every two FFTs since we have a new FFT for every half period
				fftCounter = 0;
				System.arraycopy(aFFTAmpl, 0, Modem.WaterfallAmpl, 0, RSID_FFT_SIZE);
				Modem.newAmplReady = true;
				AndPskmail.mHandler.post(AndPskmail.updatewaterfall);
			}
		}
		//		int SymbolOut = -1, BinOut = -1;
//		Use arrays of one element since they are passed by reference in Java
		SymbolOut[0] = -1;
		BinOut[0] = -1;
		if (search_amp(SymbolOut, BinOut, iDistance)) {
			if (bReverse)
				BinOut[0] = 1024 - BinOut[0] - 31;
			apply(SymbolOut[0], BinOut[0], iDistance[0]);
		}
	}



	void apply(int iSymbol, int iBin, int iDistance)
	{

		double freq = (iBin + (RSID_NSYMBOLS - 1) * RSID_RESOL / 2) * RSID_SAMPLE_RATE / 2048.0;

        //Is it an escape code?
		if (iSymbol == escapeRsid) {
            //Debug
            //Processor.TXmonitor += "\n Escape RSID (" + Integer.toString(iDistance) + ") freq: " + Integer.toString((int) freq) + "\n";
            lastEscapeTime = System.currentTimeMillis();
            lastEscapeFreq = freq;
            foundRsidEscape = true;
            return; //No more to do for now
        }

        if (foundRsidEscape) {
            Long sinceLastRsid = System.currentTimeMillis() - lastEscapeTime;
            //We had a previous escape code, expect new code within a time and frequency window
            if  (( sinceLastRsid > 2030) &&                 //200 ms time leeway approx
                    (sinceLastRsid < 2550) &&
                    (Math.abs(lastEscapeFreq - freq) < 11.0f) //11 Hz leeway
                    ){
                //Debug
                // Processor.TXmonitor += "\n 2nd RSID, time diff = " + Long.toString(sinceLastRsid) + ", freq diff = " + Double.toString(lastEscapeFreq - freq) + "\n";
            } else {
                //Reset as we have the wrong timing or frequency
                foundRsidEscape = false;
                //Debug
                //Processor.TXmonitor += "\n Reset Escape RSID, time diff = " + Long.toString(sinceLastRsid) + ", freq diff = " + Double.toString(lastEscapeFreq - freq) + "\n";
                return;
            }
        }

        //Apply the new mode
        for (int ii = 0; ii < rsid_ids[0].length; ii++) {

			if (rsid_ids[0][ii] == (short) iSymbol) {
				Processor.RxModem = Modem.getmode((char) rsid_ids[1][ii]);
				//Reject wild frequency excursions (> 100Hz) compared to Modem's default
				String frequencySTR = AndPskmail.myconfig.getPreference("AFREQUENCY","1000");
				double defaultFrequency = Double.parseDouble(frequencySTR);
				if (Math.abs(defaultFrequency - freq) < 100) {
					Processor.m.setFrequency(freq);
					Processor.m.changemode(Processor.RxModem);
					Processor.justReceivedRSID = true;
					//Alert the operator via Modem screen
					Processor.TXmonitor += "\n RSID (" + Integer.toString(iDistance) + ") " + Processor.RxModem.name() + ", freq: " + Integer.toString((int) freq) + "\n";
					//Update title bar with modem names
					AndPskmail.mHandler.post(AndPskmail.updatetitle);   
				}
			}
		}


/* VK2ETA: keep for review
 		int n, mbin = NUM_MODES;
 		for (n = 0; n < rsid_ids_size; n++) {
			if (rsid_ids[n].rs == iSymbol) {
				mbin = rsid_ids[n].mode;
				break;
			}
		}
		if (mbin == NUM_MODES) {
			char msg[50];
			if (n < rsid_ids_size) // RSID known but unimplemented
				snprintf(msg, sizeof(msg), "RSID: %s unimplemented", rsid_ids[n].name);
			else // RSID unknown; shouldn't  happen
				snprintf(msg, sizeof(msg), "RSID: code %d unknown", iSymbol);
			put_status(msg, 4.0);
			LOG_VERBOSE("%s", msg);
			return;
		}
		else
			LOG_INFO("RSID: %s @ %0.0f Hz", rsid_ids[n].name, freq);
*/
	}

	//=============================================================================
	// search_amp routine #1
	//=============================================================================

//	int HammingDistance(int iBucket, unsigned char *p2)
	int HammingDistance(int iBucket, short[] p2, int offset)
	{
		int dist = 0;
		int j = iTime - RSID_NTIMES + 1; // first value
		if (j < 0)
			j += RSID_NTIMES;
		for (int i = 0; i < RSID_NSYMBOLS; i++) {
			if (aBuckets[j][iBucket] != p2[i + offset])//*p2++)
				//VK2ETA increase minimum Hamming Distance from 1 to 5 to decode at lower s/n levels
				if (++dist == 6)
					return dist;
			j += RSID_RESOL;//2;
			if (j >= RSID_NTIMES)
				j -= RSID_NTIMES;
		}
		return dist;
	}

//	boolean search_amp( int &SymbolOut,	int &BinOut)
	boolean search_amp( int[] SymbolOut, int[] BinOut, int minIDistance[])
	{
		int i, j;
		int iDistanceMin = 99;  // infinity
		int iDistance;
		int iBin		 = -1;
		int iSymbol		 = -1;
		int iEnd		 = nBinHigh - RSID_NTIMES;//30;
		int i1, i2, i3;
		
		if (++iTime == RSID_NTIMES)
			iTime = 0;

		i1 = iTime - 3 * RSID_RESOL;//6;
		i2 = i1 + RSID_RESOL;//2;
		i3 = i2 + RSID_RESOL;//2;

		if (i1 < 0) {
			i1 += RSID_NTIMES;
			if (i2 < 0) {
				i2 += RSID_NTIMES;
				if (i3 < 0)
					i3 += RSID_NTIMES;
			}
		}

		CalculateBuckets ( aFFTAmpl, nBinLow,     iEnd);//nBinHigh - 30);
		CalculateBuckets ( aFFTAmpl, nBinLow + 1, iEnd);//nBinHigh - 30);
		
		for (i = nBinLow; i < iEnd; ++ i) {
			j = aHashTable1[aBuckets[i1][i] | (aBuckets[i2][i] << 4)];
			if (j < rsid_ids_size)  { //!= 255) {
//				iDistance = HammingDistance(i, pCodes + j * RSID_NSYMBOLS);
				iDistance = HammingDistance(i, pCodes, j * RSID_NSYMBOLS);
				//VK2ETA increase minimum Hamming Distance from 1 to 5 to decode a lower s/n levels
				if (iDistance < 6 && iDistance < iDistanceMin) {
					iDistanceMin = iDistance;
//					iSymbol  	 = rsid_ids[j].rs;
					iSymbol  	 = rsid_ids[0][j];
					iBin		 = i;
				}
			}
			j = aHashTable2[aBuckets[i3][i] | (aBuckets[iTime][i] << 4)];
			if (j < rsid_ids_size)  { //!= 255) {
//				iDistance = HammingDistance (i, pCodes + j * RSID_NSYMBOLS);
				iDistance = HammingDistance (i, pCodes, j * RSID_NSYMBOLS);
				//VK2ETA increase minimum Hamming Distance from 1 to 5 to decode a lower s/n levels
				if (iDistance < 6 && iDistance < iDistanceMin) {
					iDistanceMin = iDistance;
//					iSymbol		 = rsid_ids[j].rs;
					iSymbol  	 = rsid_ids[0][j];
					iBin		 = i;
				}
			}
		}
		
		if (iSymbol == -1) {
			// No RSID found in this time slice.
			// If there is a code stored from the previous time slice, return it.
			if (bPrevTimeSliceValid) {
				SymbolOut[0]			= iPrevSymbol;
				BinOut[0]				= iPrevBin;
				DistanceOut	    	= iPrevDistance;
				MetricsOut			= 0;
				bPrevTimeSliceValid = false;
				minIDistance[0] = DistanceOut; //return value for analysis
				return true;
			}
			return false;
		}

		if (! bPrevTimeSliceValid ||
				iDistanceMin <= iPrevDistance) {
			iPrevSymbol		= iSymbol;
			iPrevBin		= iBin;
			iPrevDistance	= iDistanceMin;
		}
		bPrevTimeSliceValid = true;
		return false;
	}
	
}