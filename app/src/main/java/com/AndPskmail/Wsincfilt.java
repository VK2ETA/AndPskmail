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
public class Wsincfilt {

    static public void wsincfilt(double firc[], double fc, boolean blackman)
    {
        double normalize = 0;
// Math.sin(x-tau)/(x-tau)
        for (int i = 0; i < 64; i++)
            if (i == 32)
                firc[i] = 2.0 * Math.PI * fc;
            else
                firc[i] = (Math.sin(2*Math.PI*fc*(i - 32)))/(i-32);
// blackman window
        if (blackman)
            for (int i = 0; i < 64; i++)
                firc[i] = firc[i] * (0.42 - 0.5 * Math.cos(2*Math.PI*i/64) + 0.08 * Math.cos(4*Math.PI*i/64));
// hamming window
        else
            for (int i = 0; i < 64; i++)
                firc[i] = firc[i] * (0.54 - 0.46 * Math.cos(2*Math.PI*i/64));
// normalization factor
        for (int i = 0; i < 64; i++)
            normalize += firc[i];
// normalize the filter
        for (int i = 0; i < 64; i++)
            firc[i] /= normalize;
    }

    //32 taps version of the filter instead of the 64
    static public void wsincfilt32(double firc[], double fc, boolean blackman)
    {
        double normalize = 0;
// Math.sin(x-tau)/(x-tau)
        for (int i = 0; i < 32; i++)
            if (i == 16)
                firc[i] = 2.0 * Math.PI * fc;
            else
                firc[i] = (Math.sin(2*Math.PI*fc*(i - 16)))/(i-16);
// blackman window
        if (blackman)
            for (int i = 0; i < 32; i++)
                firc[i] = firc[i] * (0.42 - 0.5 * Math.cos(2*Math.PI*i/32) + 0.08 * Math.cos(4*Math.PI*i/32));
// hamming window
        else
            for (int i = 0; i < 32; i++)
                firc[i] = firc[i] * (0.54 - 0.46 * Math.cos(2*Math.PI*i/32));
// normalization factor
        for (int i = 0; i < 32; i++)
            normalize += firc[i];
// normalize the filter
        for (int i = 0; i < 32; i++)
            firc[i] /= normalize;
    }


}
