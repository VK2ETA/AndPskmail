/* -----------------------------------------------------------------------
 * Viterbi.java  --  Viterbi decoder
 *
 * Transcoded from Fldigi C++ to Java by John Douyere (VK2ETA)
 *
 * Adapted from code contained in gmfsk source code distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */



package com.AndPskmail;


public class Viterbi {


	static final int PATHMEM = 64;

	//Public variable to counter the absence of pointer (for the decode method)
	public int Vmetric;
	
	private	int _traceback;
	private	int _chunksize;
	private	int nstates;
//	private	int *output;
	private	int[] output;
//	private	int *metrics[PATHMEM];
	private	int[][] metrics; // = new int[PATHMEM];
//	private	int *history[PATHMEM];
	private	int[][] history; // = new int[PATHMEM];
	private	int[] sequence = new int[PATHMEM];
	private	int[][] mettab = new int[2][256];
	private	int ptr;



	public Viterbi(int k, int poly1, int poly2) {
		int outsize = 1 << k;
		_traceback = PATHMEM - 1;
		_chunksize = 8;
		nstates = 1 << (k - 1);

		output = new int[outsize];

		for (int i = 0; i < outsize; i++) {
			// Note: Parity function. Return one if `value' has odd number of ones, zero otherwise.
			output[i] = (Integer.bitCount(poly1 & i) % 2) | ((Integer.bitCount(poly2 & i) % 2) << 1);
		}

		//Code from within the loop (creates the right size arrays)
		metrics = new int[PATHMEM][nstates];
		history = new int[PATHMEM][nstates];

		for (int i = 0; i < PATHMEM; i++) {
//			metrics[i] = new int[nstates];
//			history[i] = new int[nstates];
			sequence[i] = 0;
			//Redundant code since we call reset() below
			//			for (int j = 0; j < nstates; j++)
			//				metrics[i][j] = history[i][j] = 0;
		}
		for (int i = 0; i < 256; i++) {
			mettab[0][i] = 128 - i;
			mettab[1][i] = i - 128;
		}
		reset();
	}


	public void reset()
	{
		//Speed optimisation is not critical since this is done once
		for (int i = 0; i < PATHMEM; i++) {
			for (int j = 0; j < nstates; j++) {
			metrics[i][j] = 0;
			history[i][j] = 0;

			}
		}
		ptr = 0;
	}

	public int settraceback(int trace) {
		if (trace < 0 || trace > PATHMEM - 1)
			return -1;
		_traceback = trace;
		return 0;
	}

	public int setchunksize(int chunk) {
		if (chunk < 1 || chunk > _traceback)
			return -1;
		_chunksize = chunk;
		return 0;
	}



	private int traceback(int metric)
	{
		int bestmetric, beststate;
		int p, c = 0;

		p = (ptr - 1) % PATHMEM;

		//VK2ETA added this due to out of bound index (-1) as Java does not have unsigned int
		if (p < 0) p = PATHMEM - 1;

		
		// Find the state with the best metric
		bestmetric = Integer.MIN_VALUE; // INT_MIN;
		beststate = 0;

		for (int i = 0; i < nstates; i++) {
			if (metrics[p][i] > bestmetric) {
				bestmetric = metrics[p][i];
				beststate = i;
			}
		}

		// Trace back 'traceback' steps, starting from the best state
		sequence[p] = beststate;

		for (int i = 0; i < _traceback; i++) {
			int prev = (p - 1) % PATHMEM;

			//VK2ETA added this due to out of bound index (-1) as Java does not have unsigned int
			if (prev < 0) prev = PATHMEM - 1;
			
			sequence[prev] = history[p][sequence[p]];
			p = prev;
		}

			Vmetric = metrics[p][sequence[p]];

		// Decode 'chunksize' bits
		for (int i = 0; i < _chunksize; i++) {
			// low bit of state is the previous input bit
			c = (c << 1) | (sequence[p] & 1);
			p = (p + 1) % PATHMEM;
		}

		Vmetric = metrics[p][sequence[p]] - Vmetric;

		return c;
	}

	public int decode(int[] sym, int metric)
	{
		int currptr, prevptr;
		int[] met = new int[4];

		currptr = ptr;
		prevptr = (currptr - 1) % PATHMEM;
		//VK2ETA re-instatated this as Java does not have unsigned int
		if (prevptr < 0) prevptr = PATHMEM - 1;

		met[0] = mettab[0][sym[1]] + mettab[0][sym[0]];
		met[1] = mettab[0][sym[1]] + mettab[1][sym[0]];
		met[2] = mettab[1][sym[1]] + mettab[0][sym[0]];
		met[3] = mettab[1][sym[1]] + mettab[1][sym[0]];

		//	met[0] = 256 - sym[1] - sym[0];
		//	met[1] = sym[0] - sym[1];
		//	met[2] = sym[1] - sym[0];
		//	met[3] = sym[0] + sym[1] - 256;

		for (int n = 0; n < nstates; n++) {
			int p0, p1, s0, s1, m0, m1;

			m0 = 0;
			m1 = 0;
			s0 = n;
			s1 = n + nstates;

			p0 = s0 >> 1;
			p1 = s1 >> 1;

			m0 = metrics[prevptr][p0] + met[output[s0]];
			m1 = metrics[prevptr][p1] + met[output[s1]];

			if (m0 > m1) {
				metrics[currptr][n] = m0;
				history[currptr][n] = p0;
			} else {
				metrics[currptr][n] = m1;
				history[currptr][n] = p1;
			}
		}

		ptr = (ptr + 1) % PATHMEM;

		if ((ptr % _chunksize) == 0)
			return traceback(metric);

//		if (metrics[currptr][0] > INT_MAX / 2) {
		if (metrics[currptr][0] > Integer.MAX_VALUE / 2) {
			for (int i = 0; i < PATHMEM; i++)
				for (int j = 0; j < nstates; j++)
//					metrics[i][j] -= INT_MAX / 2;
					metrics[i][j] -= Integer.MAX_VALUE / 2;
		}
//		if (metrics[currptr][0] < INT_MIN / 2) {
		if (metrics[currptr][0] < Integer.MIN_VALUE / 2) {
			for (int i = 0; i < PATHMEM; i++)
				for (int j = 0; j < nstates; j++)
//					metrics[i][j] += INT_MIN / 2;
					metrics[i][j] += Integer.MIN_VALUE / 2;
		}

		return -1;
	}

}