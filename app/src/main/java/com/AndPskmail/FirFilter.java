/*
 * Copyright (C) 2009 Franz-Josef Maas (DB3CF)
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


/**
 *
 * @author Franz-Josef <DB3CF at pskmail.de>
 */
public class FirFilter {

	private int length = 0;
	private int decimateratio = 0;
	//    private Complex[] filter = null;
	private double[] filterR = null;
	private double[] filterI = null;
	final private int FIRBufferLen = 1024;
	//    private Complex[] buffer = new Complex[FIRBufferLen];
	private double[] bufferR = new double[FIRBufferLen];
	private double[] bufferI = new double[FIRBufferLen];
	private int pointer = 0;
	private int counter = 0;
	//    private double ffreq = 0.0;

	double sinc(double x) {
		if (Math.abs(x) < 1e-10)
			return 1.0;
		else
			return Math.sin(Math.PI * x) / (Math.PI * x);
	}

	double cosc(double x) {
		if (Math.abs(x) < 1e-10)
			return 0.0;
		else
			return (1.0 - Math.cos(Math.PI * x)) / (Math.PI * x);
	}

	double hamming(double x) {
		return 0.54 - 0.46 * Math.cos(2 * Math.PI * x);
	}


	void init(int len, int dec, double [] taps) {
		length = len;
		decimateratio = dec;

		for (int i = 0; i < FIRBufferLen; i++) {
			//            buffer[i] = new Complex();
			bufferR[i] = 0.0;
			bufferI[i] = 0.0;
		}
		//        filter = new Complex[len];
		filterR = new double[len];
		filterI = new double[len];
		for(int i = 0; i < len; i++) {
			//            filter[i] = new Complex(taps[i],taps[i]);
			filterR[i] = taps[i];
			filterI[i] = taps[i];
		}
		pointer = len;
		counter = 0;
	}

	//=====================================================================
	// Run
	// passes a mycomplex value (in) and receives the mycomplex value (out)
	// function returns 0 if the filter is not yet stable
	// returns 1 when stable and decimated mycomplex output value is valid
	//=====================================================================

	//JD Optimised as this is the most CPU intensive task:
	// 1. inline the MAC method
	// 2. Eliminate use of complex objects as getters and setters are very inneficient in Android java
	// 3. Change double to int
	// 4. Check the real-life effect of halfing the length of the filter (32 vs 64)

	boolean run (double[] ioR, double[] ioI) {
		//    	  buffer[pointer].gleich(io[0]);
		//		  //code for above
		//        public Complex gleich(Complex x) {
		//            real = x.getReal();
		//            imag = x.getImag();
		//            return this;
		//        }
		bufferR[pointer] = ioR[0];
		bufferI[pointer] = ioI[0];
		counter++;

		if (counter == decimateratio) {
			//            Complex sum = mac(buffer, pointer-length, filter, length);
			double sumR = 0.0;
			double sumI = 0.0;
			//    	    Complex mac(Complex[] a, int pos, Complex[] b, int size) {
			//	    		Complex sum = new Complex();
			//	    		for (int i = 0; i < size; i++) {
			//  	  			sum.setReal( sum.getReal() + (a[i+pos].getReal() * b[i].getReal()));
			//    				sum.setImag( sum.getImag() + (a[i+pos].getImag() * b[i].getImag()));
			//	    		}
			//    		)

			int pos = pointer-length;
			for (int i = 0; i < length; i++) {
				sumR += (bufferR[i+pos] * filterR[i]);
				sumI += (bufferI[i+pos] * filterI[i]);
			}
			//            io[1] = sum; 
			ioR[1] = sumR; 
			ioI[1] = sumI; 
		}
		pointer++;
		if (pointer == FIRBufferLen) {
			//    		System.arraycopy(buffer, FIRBufferLen - length, buffer, 0, length );
			System.arraycopy(bufferR, FIRBufferLen - length, bufferR, 0, length );
			System.arraycopy(bufferI, FIRBufferLen - length, bufferI, 0, length );
			pointer = length;
		}
		if (counter == decimateratio) {
			counter = 0;
			return true;
		}
		return false;
	}



	/* original code 
    boolean run (Complex[] io) {
	buffer[pointer].gleich(io[0]);
	counter++;
	if (counter == decimateratio) {
            Complex sum = mac(buffer, pointer-length, filter, length);
            io[1] = sum; //new Complex(mac(buffer, pointer - length, filter, length));
        }
	pointer++;
	if (pointer == FIRBufferLen) {
		System.arraycopy(buffer, FIRBufferLen - length, buffer, 0, length );
		pointer = length;
	}
	if (counter == decimateratio) {
		counter = 0;
		return true;
	}
	return false;
    }

    Complex mac(Complex[] a, int pos, Complex[] b, int size) {
		Complex sum = new Complex();
		for (int i = 0; i < size; i++) {
			sum.setReal( sum.getReal() + (a[i+pos].getReal() * b[i].getReal()));
			sum.setImag( sum.getImag() + (a[i+pos].getImag() * b[i].getImag()));
                }
		return sum;
	}
	 */


	void init_hilbert (int len, int dec) {
		double[] fi = bp_FIR(len, 0, 0.05, 0.45);
		double[] fq = bp_FIR(len, 1, 0.05, 0.45);

		length = len;
		decimateratio = dec;

		for (int i = 0; i < FIRBufferLen; i++) {
			//            buffer[i] = new Complex();
			bufferR[i] = 0.0;
			bufferI[i] = 0.0;
		}
		filterR = new double[len];
		filterI = new double[len];
		for(int i = 0; i < len; i++) {
			filterR[i] = fi[i];
			filterI[i] = fq[i];
		}
		pointer = len;
		counter = 0;
	}



	void init_bandpass (int len, int dec, double f1, double f2) {
		double[] fi = bp_FIR (len, 0, f1, f2);
		length = len;
		decimateratio = dec;

		for (int i = 0; i < FIRBufferLen; i++) {
			//            buffer[i] = new Complex();
			bufferR[i] = 0.0;
			bufferI[i] = 0.0;
		}
		//        filter = new Complex[len];
		filterR = new double[len];
		filterI = new double[len];
		for(int i = 0; i < len; i++) {
			//            filter[i] = new Complex(fi[i],fi[i]);
			filterR[i] = fi[i];
			filterI[i] = fi[i];
		}
		pointer = len;
		counter = 0;
		//	delete [] fi;
	}



	double[] bp_FIR(int len, int hilbert, double f1, double f2)
	{
		double[] fir;
		double t, h, x;

		fir = new double[len];

		for (int i = 0; i < len; i++) {
			t = i - (len - 1.0) / 2.0;
			h = i * (1.0 / (len - 1.0));

			//VK2ETA Debug fix			if (hilbert != 0) {
			if (hilbert == 0) {
				x = (2 * f2 * sinc(2 * f2 * t) -
						2 * f1 * sinc(2 * f1 * t)) * hamming(h);
			} else {
				x = (2 * f2 * cosc(2 * f2 * t) -
						2 * f1 * cosc(2 * f1 * t)) * hamming(h);
				// The actual filter code assumes the impulse response
				// is in time reversed order. This will be anti-
				// symmetric so the minus sign handles that for us.
				x = -x;
			}

			fir[i] = x;
		}

		return fir;
	}



}
