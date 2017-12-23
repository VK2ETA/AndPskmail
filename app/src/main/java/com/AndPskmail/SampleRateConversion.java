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

// ----------------------------------------------------------------------------
// Rate converter - real input/output, quadratic interpolation
// similar limits like for RateConv1



package com.AndPskmail;


public class SampleRateConversion {

	private double OutStep = 1.0;
	private double OutdspPhase = 0;
	private int TapPtr = 0;
	private double[] Tap = new double[4];



	public SampleRateConversion(double OutVsInp)
	{
		OutStep = 1.0 / OutVsInp;
		for (int i = 0; i < 4; i++)
			Tap[i] = 0;
		OutdspPhase = 0;
		TapPtr = 0;
	}


	public int Process(
			short[] so8k, int InpLen,
			double[] so12k, int MaxOutLen)
	{
		int inPtr = 0;
		int outPtr = 0;
		int i, o, t;
		double Ref0, Ref1, Diff0, Diff1;
		for (o = i = 0; (i < InpLen) && (o < MaxOutLen); ) {
			if (OutdspPhase >= 1.0) {
				Tap[TapPtr] = so8k[inPtr++];
				i++;
				TapPtr = (TapPtr + 1) & 3;
				OutdspPhase -= 1.0;
			} else {
				t = TapPtr;
				Diff0 = (Tap[t^2] - Tap[t]) / 2;
				Ref1 = Tap[t^2];
				t = (t + 1) & 3;
				Diff1 = (Tap[t^2] - Tap[t]) / 2;
				Ref0 = Tap[t];
				so12k[outPtr++] = (Ref0 * (1.0 - OutdspPhase) + Ref1*OutdspPhase // linear piece
						-(Diff1-Diff0)*OutdspPhase*(1.0-OutdspPhase)/2); // quadr. piece
				o++;
				OutdspPhase += OutStep;
			}
		}

		return outPtr;
	}


}
