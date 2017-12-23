/*
 * misc.java  
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

package com.AndPskmail;

public class misc {

	@SuppressWarnings("unused")
	private double rect(double x)
	{
		return 1.0;
	}

	private static double blackman(double x)
	{
		return (0.42 - 0.50 * Math.cos(2 * Math.PI * x) + 0.08 * Math.cos(4 * Math.PI * x));
	}

	private static double hamming(double x)
	{
		return 0.54 - 0.46 * Math.cos(2 * Math.PI * x);
	}

	private static double hanning(double x)
	{
		return 0.5 - 0.5 * Math.cos(2 * Math.PI * x);
	}


	// Rectangular - no pre filtering of data array
	public static void RectWindow(double[] array, int n) {
		for (int i = 0; i < n; i++)
			array[i] = 1.0;
	}


	// Hamming - used by gmfsk
	public static void HammingWindow(double[] array, int n) {
		double pwr = 0.0;
		for (int i = 0; i < n; i++) {
			array[i] = hamming((double)i/(double)n);
			pwr += array[i] * array[i];
		}
		pwr = Math.sqrt((double)n/pwr);
		for (int i = 0; i < n; i++)
			array[i] *= pwr;
	}

	// Hanning - used by winpsk
	public static void HanningWindow(double[] array, int n) {
		double pwr = 0.0;
		for (int i = 0; i < n; i++) {
			array[i] = hanning((double)i/(double)n);
			pwr += array[i] * array[i];
		}
		pwr = Math.sqrt((double)n/pwr);
		for (int i = 0; i < n; i++)
			array[i] *= pwr;
	}

	// Best lob suppression - least in band ripple
	public static void BlackmanWindow(double[] array, int n) {
		double pwr = 0.0;
		for (int i = 0; i < n; i++) {
			array[i] = blackman((double)i/(double)n);
			pwr += array[i] * array[i];
		}
		pwr = Math.sqrt((double)n/pwr);
		for (int i = 0; i < n; i++)
			array[i] *= pwr;
	}

	// Simple about effective as Hamming or Hanning
	public static void TriangularWindow(double[] array, int n) {
		double pwr = 0.0;
		for (int i = 0; i < n; i++) array[i] = 1.0;
		for (int i = 0; i < n / 4; i++) {
				array[i] = 4.0 * (double)i / (double)n;
				array[n-i] = array[i];
		}
		for (int i = 0; i < n; i++)	pwr += array[i] * array[i];
		pwr = Math.sqrt((double)n/pwr);
		for (int i = 0; i < n; i++)
			array[i] *= pwr;
	}

	//efficient java memset for short[]
	public static void memset(short[] myarray, short j) {
		int len = myarray.length;
		if (len > 0)
			myarray[0] = j;
		for (int i = 1; i < len; i += i) {
			System.arraycopy( myarray, 0, myarray, i, ((len - i) < i) ? (len - i) : i);
		}
	}
	
	//memset equivalent for int[]
	public static void memset(int[] myarray, int j) {
		int len = myarray.length;
		if (len > 0)
			myarray[0] = j;
		for (int i = 1; i < len; i += i) {
			System.arraycopy( myarray, 0, myarray, i, ((len - i) < i) ? (len - i) : i);
		}
	}
	
	//memset equivalent for float[]
	public static void memset(float[] myarray, float j) {
		int len = myarray.length;
		if (len > 0)
			myarray[0] = j;
		for (int i = 1; i < len; i += i) {
			System.arraycopy( myarray, 0, myarray, i, ((len - i) < i) ? (len - i) : i);
		}
	}
	
	//memset equivalent for double[]
	public static void memset(double[] myarray, double j) {
		int len = myarray.length;
		if (len > 0)
			myarray[0] = j;
		for (int i = 1; i < len; i += i) {
			System.arraycopy( myarray, 0, myarray, i, ((len - i) < i) ? (len - i) : i);
		}
	}

    public static int grayencode(int data)
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

    public static int graydecode(int data)
    {
            return data ^ (data >> 1);
    }

	
	
}