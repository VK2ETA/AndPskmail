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

//===========================================================================
//Real Discrete Fourier Transform
//  dimension   :one
//  data length :power of 2, must be larger than 4
//  decimation  :frequency
//  radix       :4, 2
//  data        :inplace
//classes:
//	 Cfft: real discrete fourier transform class
//functions:
//	 Cfft::rdft  : compute the forward real discrete fourier transform
//	 Cfft::cdft  : compute the forward double discrete fourier transform
//	 Cfft::icdft : compute the reverse double discrete fourier transform 
//	 Cfft::fft   : compute the forward real dft on a set of integer values
//	
//	 This class is derived from the work of Takuya Ooura, who has kindly put his
//	 fft algorithims in the public domain.  Thank you Takuya Ooura!
//===========================================================================
//n = size of fourier transform in complex pairs
//fftsiz = size of fourier transform in real (double) values


package com.AndPskmail;

public class Cfft {

	public enum fftPrefilter {FFT_NONE, FFT_HAMMING, FFT_HANNING, FFT_BLACKMAN, FFT_TRIANGULAR};

	double xi;
//	double *w;
//	int  *ip;
//	double *fftwin;
	double[] w;
	int[]  ip;
	double[] fftwin;
	fftPrefilter wintype;
	int  fftlen;
	int  fftsiz;
	
	
	
	Cfft(int n)
	{
		int tablesize = (int)(Math.sqrt(n*1.0)+0.5) + 2;
		fftlen = n;
		fftsiz = 2 * n;
		ip = new int[tablesize];
		w = new double[fftlen];
		fftwin = new double[fftlen*2];
		makewt();
		makect();
		wintype = fftPrefilter.FFT_NONE;
		misc.RectWindow(fftwin, fftlen*2);
	}


	void resize(int n)
	{
		int tablesize = (int)(Math.sqrt(n*1.0)+0.5) + 2;
		fftlen = n;
		fftsiz = 2 * n;
//		if (ip) delete [] ip;
		ip	= new int[tablesize];
//		if (w) delete [] w;
		w 	= new double[fftlen];
//		if (fftwin) delete [] fftwin;
		fftwin = new double[fftlen*2];
		makewt();
		makect();
		wintype = fftPrefilter.FFT_NONE;
		misc.RectWindow(fftwin, fftlen*2);
	}

	void cdft(double[] aCmpx)
	{
		if (wintype != fftPrefilter.FFT_NONE)
			for (int i = 0; i < fftlen; i++) {
				aCmpx[2*i] *= fftwin[2*i];
				aCmpx[2*i+1] *= fftwin[2*i];
			}
//		bitrv2(fftsiz, ip + 2, aCmpx);
		bitrv2(fftsiz, ip, aCmpx); //offset done in bitrv2 class
		cftfsub(fftsiz, aCmpx);
		double scale = 1.0 / fftlen;
		for (int i = 0; i < fftsiz; i++) aCmpx[i] = aCmpx[i] * scale;
	}

	void icdft(double[] aCmpx)
	{
		bitrv2conj(fftsiz, ip, aCmpx);
		cftbsub(fftsiz, aCmpx);
		for (int i = 0; i < fftsiz; i++) aCmpx[i] = aCmpx[i] / 2.0;
	}

	//FFT of an array of short integers
	//siData = array (size n) of unsigned integers such as the output of a soundcard
	//operating in 16 bit mode
	//out = array (size n) of double pairs

	void sifft(short[] siData, double[] out)
	{
		for (int i = 0; i < fftlen; i++) {
			out[2*i] = siData[i];
			out[2*i+1] = 0.0;
		}
		cdft(out);
		return;
	}


	void rdft(double[] RealData) // RealData is 2N long
	{
		if (wintype != fftPrefilter.FFT_NONE)
			for (int i = 0; i < fftlen*2; i++) {
				RealData[i] *= fftwin[i];
			}

		if (fftsiz > 4) {
//			bitrv2(fftsiz, ip + 2, RealData); //offset done in bitrv2 class
			bitrv2(fftsiz, ip, RealData);
			cftfsub(fftsiz, RealData);
			rftfsub(fftsiz, RealData);
		} else if (fftsiz == 4) {
			cftfsub(fftsiz, RealData);
		}
		double xi = RealData[0] - RealData[1];
		RealData[0] += RealData[1];
		RealData[1] = xi;
		double scale = 1.0 / fftlen;
		for (int i = 0; i < fftsiz; i++) RealData[i] *= scale;

	}

	void irdft(double[] RealData)
	{
		/*
 int nw, nc;
 double xi;

 nw = ip[0];
 if (n > (nw << 2)) {
     nw = n >> 2;
     makewt(nw, ip, w);
 }
 nc = ip[1];
 if (n > (nc << 2)) {
     nc = n >> 2;
     makect(nc, ip, w + nw);
 }
 if (isgn >= 0) {
     if (n > 4) {
         bitrv2(n, ip + 2, a);
         cftfsub(n, a, w);
         rftfsub(n, a, nc, w + nw);
     } else if (n == 4) {
         cftfsub(n, a, w);
     }
     xi = a[0] - a[1];
     a[0] += a[1];
     a[1] = xi;
 } else {
     a[1] = 0.5 * (a[0] - a[1]);
     a[0] -= a[1];
     if (n > 4) {
         rftbsub(n, a, nc, w + nw);
         bitrv2(n, ip + 2, a);
         cftbsub(n, a, w);
     } else if (n == 4) {
         cftfsub(n, a, w);
     }
 }
		 */
	}

	void setWindow(fftPrefilter pf)
	{
		wintype = pf;

		if (wintype == fftPrefilter.FFT_TRIANGULAR)
			misc.TriangularWindow(fftwin, fftlen*2);
		else if (wintype == fftPrefilter.FFT_HAMMING)
			misc.HammingWindow(fftwin, fftlen*2);
		else if (wintype == fftPrefilter.FFT_HANNING)
			misc.HanningWindow(fftwin, fftlen*2);
		else if (wintype == fftPrefilter.FFT_BLACKMAN)
			misc.BlackmanWindow(fftwin, fftlen*2);
		else
		misc.RectWindow(fftwin, fftlen*2);
	}

	/* -------- initializing routines -------- */


	void makewt()
	{
		int j, 
		nwh, nw = fftsiz / 4;
		double delta, x, y;

		ip[0] = nw;
		ip[1] = 1;
		if (nw > 2) {
			nwh = nw >> 1;
		delta = Math.atan(1.0) / nwh;
		w[0] = 1;
		w[1] = 0;
		w[nwh] = Math.cos(delta * nwh);
		w[nwh + 1] = w[nwh];
		if (nwh > 2) {
			for (j = 2; j < nwh; j += 2) {
				x = Math.cos(delta * j);
				y = Math.sin(delta * j);
				w[j] = x;
				w[j + 1] = y;
				w[nw - j] = y;
				w[nw - j + 1] = x;
			}
//			bitrv2(nw, ip + 2, w);
			bitrv2(nw, ip, w);  //offset done in bitrv2 class
		}
		}
	}

	void makect()
	{
		int j, nch, nc = fftsiz / 4;
		double delta;
//		double *c = w + fftsiz / 4;
//		c = w + fftsiz / 4;
		int offset_c = fftsiz / 4;
		offset_c = fftsiz / 4;
		ip[1] = nc;
		if (nc > 1) {
			nch = nc >> 1;
			delta = Math.atan(1.0) / nch;
			w[0 + offset_c] = Math.cos(delta * nch);
			w[nch + offset_c] = 0.5 * w[0 + offset_c];
			for (j = 1; j < nch; j++) {
				w[j + offset_c] = 0.5 * Math.cos(delta * j);
				w[nc - j + offset_c] = 0.5 * Math.sin(delta * j);
			}
		}
	}


	/* -------- child routines -------- */


	void bitrv2(int n, int[] ip, double[] a)
	{
//Java transcoding Warning: 
//	Since all calls were made with ip + 2, the offset is HARD included here
		
		int j, j1, k, k1, l, m, m2;
		double xr, xi, yr, yi;

//		ip[0] = 0; //Example of hard coded offset
		ip[2] = 0;
		l = n;
		m = 1;
		while ((m << 3) < l) {
			l >>= 1;
		for (j = 0; j < m; j++) {
			ip[m + j + 2] = ip[j + 2] + l;
		}
		m <<= 1;
		}
		m2 = 2 * m;
		if ((m << 3) == l) {
			for (k = 0; k < m; k++) {
				for (j = 0; j < k; j++) {
					j1 = 2 * j + ip[k + 2];
					k1 = 2 * k + ip[j +2];
					xr = a[j1];
					xi = a[j1 + 1];
					yr = a[k1];
					yi = a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
					j1 += m2;
					k1 += 2 * m2;
					xr = a[j1];
					xi = a[j1 + 1];
					yr = a[k1];
					yi = a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
					j1 += m2;
					k1 -= m2;
					xr = a[j1];
					xi = a[j1 + 1];
					yr = a[k1];
					yi = a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
					j1 += m2;
					k1 += 2 * m2;
					xr = a[j1];
					xi = a[j1 + 1];
					yr = a[k1];
					yi = a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
				}
				j1 = 2 * k + m2 + ip[k + 2];
				k1 = j1 + m2;
				xr = a[j1];
				xi = a[j1 + 1];
				yr = a[k1];
				yi = a[k1 + 1];
				a[j1] = yr;
				a[j1 + 1] = yi;
				a[k1] = xr;
				a[k1 + 1] = xi;
			}
		} else {
			for (k = 1; k < m; k++) {
				for (j = 0; j < k; j++) {
					j1 = 2 * j + ip[k + 2];
					k1 = 2 * k + ip[j + 2];
					xr = a[j1];
					xi = a[j1 + 1];
					yr = a[k1];
					yi = a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
					j1 += m2;
					k1 += m2;
					xr = a[j1];
					xi = a[j1 + 1];
					yr = a[k1];
					yi = a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
				}
			}
		}
	}


	void cftfsub(int n, double[] a)
	{
		int j, j1, j2, j3, l;
		double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;

		l = 2;
		if (n > 8) {
			cft1st(n, a);
			l = 8;
			while ((l << 2) < n) {
				cftmdl(n, l, a);
				l <<= 2;
			}
		}
		if ((l << 2) == n) {
			for (j = 0; j < l; j += 2) {
				j1 = j + l;
				j2 = j1 + l;
				j3 = j2 + l;
				x0r = a[j] + a[j1];
				x0i = a[j + 1] + a[j1 + 1];
				x1r = a[j] - a[j1];
				x1i = a[j + 1] - a[j1 + 1];
				x2r = a[j2] + a[j3];
				x2i = a[j2 + 1] + a[j3 + 1];
				x3r = a[j2] - a[j3];
				x3i = a[j2 + 1] - a[j3 + 1];
				a[j] = x0r + x2r;
				a[j + 1] = x0i + x2i;
				a[j2] = x0r - x2r;
				a[j2 + 1] = x0i - x2i;
				a[j1] = x1r - x3i;
				a[j1 + 1] = x1i + x3r;
				a[j3] = x1r + x3i;
				a[j3 + 1] = x1i - x3r;
			}
		} else {
			for (j = 0; j < l; j += 2) {
				j1 = j + l;
				x0r = a[j] - a[j1];
				x0i = a[j + 1] - a[j1 + 1];
				a[j] += a[j1];
				a[j + 1] += a[j1 + 1];
				a[j1] = x0r;
				a[j1 + 1] = x0i;
			}
		}
	}


	void cft1st(int n, double[] a)
	{
		int j, k1, k2;
		double wk1r, wk1i, wk2r, wk2i, wk3r, wk3i;
		double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;

		x0r = a[0] + a[2];
		x0i = a[1] + a[3];
		x1r = a[0] - a[2];
		x1i = a[1] - a[3];
		x2r = a[4] + a[6];
		x2i = a[5] + a[7];
		x3r = a[4] - a[6];
		x3i = a[5] - a[7];
		a[0] = x0r + x2r;
		a[1] = x0i + x2i;
		a[4] = x0r - x2r;
		a[5] = x0i - x2i;
		a[2] = x1r - x3i;
		a[3] = x1i + x3r;
		a[6] = x1r + x3i;
		a[7] = x1i - x3r;
		wk1r = w[2];
		x0r = a[8] + a[10];
		x0i = a[9] + a[11];
		x1r = a[8] - a[10];
		x1i = a[9] - a[11];
		x2r = a[12] + a[14];
		x2i = a[13] + a[15];
		x3r = a[12] - a[14];
		x3i = a[13] - a[15];
		a[8] = x0r + x2r;
		a[9] = x0i + x2i;
		a[12] = x2i - x0i;
		a[13] = x0r - x2r;
		x0r = x1r - x3i;
		x0i = x1i + x3r;
		a[10] = wk1r * (x0r - x0i);
		a[11] = wk1r * (x0r + x0i);
		x0r = x3i + x1r;
		x0i = x3r - x1i;
		a[14] = wk1r * (x0i - x0r);
		a[15] = wk1r * (x0i + x0r);
		k1 = 0;
		for (j = 16; j < n; j += 16) {
			k1 += 2;
			k2 = 2 * k1;
			wk2r = w[k1];
			wk2i = w[k1 + 1];
			wk1r = w[k2];
			wk1i = w[k2 + 1];
			wk3r = wk1r - 2 * wk2i * wk1i;
			wk3i = 2 * wk2i * wk1r - wk1i;
			x0r = a[j] + a[j + 2];
			x0i = a[j + 1] + a[j + 3];
			x1r = a[j] - a[j + 2];
			x1i = a[j + 1] - a[j + 3];
			x2r = a[j + 4] + a[j + 6];
			x2i = a[j + 5] + a[j + 7];
			x3r = a[j + 4] - a[j + 6];
			x3i = a[j + 5] - a[j + 7];
			a[j] = x0r + x2r;
			a[j + 1] = x0i + x2i;
			x0r -= x2r;
			x0i -= x2i;
			a[j + 4] = wk2r * x0r - wk2i * x0i;
			a[j + 5] = wk2r * x0i + wk2i * x0r;
			x0r = x1r - x3i;
			x0i = x1i + x3r;
			a[j + 2] = wk1r * x0r - wk1i * x0i;
			a[j + 3] = wk1r * x0i + wk1i * x0r;
			x0r = x1r + x3i;
			x0i = x1i - x3r;
			a[j + 6] = wk3r * x0r - wk3i * x0i;
			a[j + 7] = wk3r * x0i + wk3i * x0r;
			wk1r = w[k2 + 2];
			wk1i = w[k2 + 3];
			wk3r = wk1r - 2 * wk2r * wk1i;
			wk3i = 2 * wk2r * wk1r - wk1i;
			x0r = a[j + 8] + a[j + 10];
			x0i = a[j + 9] + a[j + 11];
			x1r = a[j + 8] - a[j + 10];
			x1i = a[j + 9] - a[j + 11];
			x2r = a[j + 12] + a[j + 14];
			x2i = a[j + 13] + a[j + 15];
			x3r = a[j + 12] - a[j + 14];
			x3i = a[j + 13] - a[j + 15];
			a[j + 8] = x0r + x2r;
			a[j + 9] = x0i + x2i;
			x0r -= x2r;
			x0i -= x2i;
			a[j + 12] = -wk2i * x0r - wk2r * x0i;
			a[j + 13] = -wk2i * x0i + wk2r * x0r;
			x0r = x1r - x3i;
			x0i = x1i + x3r;
			a[j + 10] = wk1r * x0r - wk1i * x0i;
			a[j + 11] = wk1r * x0i + wk1i * x0r;
			x0r = x1r + x3i;
			x0i = x1i - x3r;
			a[j + 14] = wk3r * x0r - wk3i * x0i;
			a[j + 15] = wk3r * x0i + wk3i * x0r;
		}
	}


	void cftmdl(int n, int l, double[] a)
	{
		int j, j1, j2, j3, k, k1, k2, m, m2;
		double wk1r, wk1i, wk2r, wk2i, wk3r, wk3i;
		double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;

		m = l << 2;
		for (j = 0; j < l; j += 2) {
			j1 = j + l;
			j2 = j1 + l;
			j3 = j2 + l;
			x0r = a[j] + a[j1];
			x0i = a[j + 1] + a[j1 + 1];
			x1r = a[j] - a[j1];
			x1i = a[j + 1] - a[j1 + 1];
			x2r = a[j2] + a[j3];
			x2i = a[j2 + 1] + a[j3 + 1];
			x3r = a[j2] - a[j3];
			x3i = a[j2 + 1] - a[j3 + 1];
			a[j] = x0r + x2r;
			a[j + 1] = x0i + x2i;
			a[j2] = x0r - x2r;
			a[j2 + 1] = x0i - x2i;
			a[j1] = x1r - x3i;
			a[j1 + 1] = x1i + x3r;
			a[j3] = x1r + x3i;
			a[j3 + 1] = x1i - x3r;
		}
		wk1r = w[2];
		for (j = m; j < l + m; j += 2) {
			j1 = j + l;
			j2 = j1 + l;
			j3 = j2 + l;
			x0r = a[j] + a[j1];
			x0i = a[j + 1] + a[j1 + 1];
			x1r = a[j] - a[j1];
			x1i = a[j + 1] - a[j1 + 1];
			x2r = a[j2] + a[j3];
			x2i = a[j2 + 1] + a[j3 + 1];
			x3r = a[j2] - a[j3];
			x3i = a[j2 + 1] - a[j3 + 1];
			a[j] = x0r + x2r;
			a[j + 1] = x0i + x2i;
			a[j2] = x2i - x0i;
			a[j2 + 1] = x0r - x2r;
			x0r = x1r - x3i;
			x0i = x1i + x3r;
			a[j1] = wk1r * (x0r - x0i);
			a[j1 + 1] = wk1r * (x0r + x0i);
			x0r = x3i + x1r;
			x0i = x3r - x1i;
			a[j3] = wk1r * (x0i - x0r);
			a[j3 + 1] = wk1r * (x0i + x0r);
		}
		k1 = 0;
		m2 = 2 * m;
		for (k = m2; k < n; k += m2) {
			k1 += 2;
			k2 = 2 * k1;
			wk2r = w[k1];
			wk2i = w[k1 + 1];
			wk1r = w[k2];
			wk1i = w[k2 + 1];
			wk3r = wk1r - 2 * wk2i * wk1i;
			wk3i = 2 * wk2i * wk1r - wk1i;
			for (j = k; j < l + k; j += 2) {
				j1 = j + l;
				j2 = j1 + l;
				j3 = j2 + l;
				x0r = a[j] + a[j1];
				x0i = a[j + 1] + a[j1 + 1];
				x1r = a[j] - a[j1];
				x1i = a[j + 1] - a[j1 + 1];
				x2r = a[j2] + a[j3];
				x2i = a[j2 + 1] + a[j3 + 1];
				x3r = a[j2] - a[j3];
				x3i = a[j2 + 1] - a[j3 + 1];
				a[j] = x0r + x2r;
				a[j + 1] = x0i + x2i;
				x0r -= x2r;
				x0i -= x2i;
				a[j2] = wk2r * x0r - wk2i * x0i;
				a[j2 + 1] = wk2r * x0i + wk2i * x0r;
				x0r = x1r - x3i;
				x0i = x1i + x3r;
				a[j1] = wk1r * x0r - wk1i * x0i;
				a[j1 + 1] = wk1r * x0i + wk1i * x0r;
				x0r = x1r + x3i;
				x0i = x1i - x3r;
				a[j3] = wk3r * x0r - wk3i * x0i;
				a[j3 + 1] = wk3r * x0i + wk3i * x0r;
			}
			wk1r = w[k2 + 2];
			wk1i = w[k2 + 3];
			wk3r = wk1r - 2 * wk2r * wk1i;
			wk3i = 2 * wk2r * wk1r - wk1i;
			for (j = k + m; j < l + (k + m); j += 2) {
				j1 = j + l;
				j2 = j1 + l;
				j3 = j2 + l;
				x0r = a[j] + a[j1];
				x0i = a[j + 1] + a[j1 + 1];
				x1r = a[j] - a[j1];
				x1i = a[j + 1] - a[j1 + 1];
				x2r = a[j2] + a[j3];
				x2i = a[j2 + 1] + a[j3 + 1];
				x3r = a[j2] - a[j3];
				x3i = a[j2 + 1] - a[j3 + 1];
				a[j] = x0r + x2r;
				a[j + 1] = x0i + x2i;
				x0r -= x2r;
				x0i -= x2i;
				a[j2] = -wk2i * x0r - wk2r * x0i;
				a[j2 + 1] = -wk2i * x0i + wk2r * x0r;
				x0r = x1r - x3i;
				x0i = x1i + x3r;
				a[j1] = wk1r * x0r - wk1i * x0i;
				a[j1 + 1] = wk1r * x0i + wk1i * x0r;
				x0r = x1r + x3i;
				x0i = x1i - x3r;
				a[j3] = wk3r * x0r - wk3i * x0i;
				a[j3 + 1] = wk3r * x0i + wk3i * x0r;
			}
		}
	}


	void cftbsub(int n, double[] a)
	{
		int j, j1, j2, j3, l;
		double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;

		l = 2;
		if (n > 8) {
			cft1st(n, a);
			l = 8;
			while ((l << 2) < n) {
				cftmdl(n, l, a);
				l <<= 2;
			}
		}
		if ((l << 2) == n) {
			for (j = 0; j < l; j += 2) {
				j1 = j + l;
				j2 = j1 + l;
				j3 = j2 + l;
				x0r = a[j] + a[j1];
				x0i = -a[j + 1] - a[j1 + 1];
				x1r = a[j] - a[j1];
				x1i = -a[j + 1] + a[j1 + 1];
				x2r = a[j2] + a[j3];
				x2i = a[j2 + 1] + a[j3 + 1];
				x3r = a[j2] - a[j3];
				x3i = a[j2 + 1] - a[j3 + 1];
				a[j] = x0r + x2r;
				a[j + 1] = x0i - x2i;
				a[j2] = x0r - x2r;
				a[j2 + 1] = x0i + x2i;
				a[j1] = x1r - x3i;
				a[j1 + 1] = x1i - x3r;
				a[j3] = x1r + x3i;
				a[j3 + 1] = x1i + x3r;
			}
		} else {
			for (j = 0; j < l; j += 2) {
				j1 = j + l;
				x0r = a[j] - a[j1];
				x0i = -a[j + 1] + a[j1 + 1];
				a[j] += a[j1];
				a[j + 1] = -a[j + 1] - a[j1 + 1];
				a[j1] = x0r;
				a[j1 + 1] = x0i;
			}
		}
	}

	void bitrv2conj(int n, int[] ip, double[] a)
	{
		int j, j1, k, k1, l, m, m2;
		double xr, xi, yr, yi;

		ip[0] = 0;
		l = n;
		m = 1;
		while ((m << 3) < l) {
			l >>= 1;
		for (j = 0; j < m; j++) {
			ip[m + j] = ip[j] + l;
		}
		m <<= 1;
		}
		m2 = 2 * m;
		if ((m << 3) == l) {
			for (k = 0; k < m; k++) {
				for (j = 0; j < k; j++) {
					j1 = 2 * j + ip[k];
					k1 = 2 * k + ip[j];
					xr = a[j1];
					xi = -a[j1 + 1];
					yr = a[k1];
					yi = -a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
					j1 += m2;
					k1 += 2 * m2;
					xr = a[j1];
					xi = -a[j1 + 1];
					yr = a[k1];
					yi = -a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
					j1 += m2;
					k1 -= m2;
					xr = a[j1];
					xi = -a[j1 + 1];
					yr = a[k1];
					yi = -a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
					j1 += m2;
					k1 += 2 * m2;
					xr = a[j1];
					xi = -a[j1 + 1];
					yr = a[k1];
					yi = -a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
				}
				k1 = 2 * k + ip[k];
				a[k1 + 1] = -a[k1 + 1];
				j1 = k1 + m2;
				k1 = j1 + m2;
				xr = a[j1];
				xi = -a[j1 + 1];
				yr = a[k1];
				yi = -a[k1 + 1];
				a[j1] = yr;
				a[j1 + 1] = yi;
				a[k1] = xr;
				a[k1 + 1] = xi;
				k1 += m2;
				a[k1 + 1] = -a[k1 + 1];
			}
		} else {
			a[1] = -a[1];
			a[m2 + 1] = -a[m2 + 1];
			for (k = 1; k < m; k++) {
				for (j = 0; j < k; j++) {
					j1 = 2 * j + ip[k];
					k1 = 2 * k + ip[j];
					xr = a[j1];
					xi = -a[j1 + 1];
					yr = a[k1];
					yi = -a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
					j1 += m2;
					k1 += m2;
					xr = a[j1];
					xi = -a[j1 + 1];
					yr = a[k1];
					yi = -a[k1 + 1];
					a[j1] = yr;
					a[j1 + 1] = yi;
					a[k1] = xr;
					a[k1 + 1] = xi;
				}
				k1 = 2 * k + ip[k];
				a[k1 + 1] = -a[k1 + 1];
				a[k1 + m2 + 1] = -a[k1 + m2 + 1];
			}
		}
	}

	void rftfsub(int n, double[] a)
	{
		int j, k, kk, ks, m;
		double wkr, wki, xr, xi, yr, yi;
//		double *c = w + fftsiz / 4;
//		c = w + fftsiz / 4;
		int offset_c = fftsiz / 4;
		offset_c = fftsiz / 4;
		int nc = n >> 2;

				m = n >> 1;
				ks = 2 * nc / m;
				kk = 0;
				for (j = 2; j < m; j += 2) {
					k = n - j;
					kk += ks;
					wkr = 0.5 - w[nc - kk + offset_c];
					wki = w[kk + offset_c];
					xr = a[j] - a[k];
					xi = a[j + 1] + a[k + 1];
					yr = wkr * xr - wki * xi;
					yi = wkr * xi + wki * xr;
					a[j] -= yr;
					a[j + 1] -= yi;
					a[k] += yr;
					a[k + 1] -= yi;
				}
	}

/* Not used
	void rftbsub(int n, double[] a)
	{
	}
 */

}