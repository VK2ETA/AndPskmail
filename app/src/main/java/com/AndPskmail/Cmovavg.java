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
public class Cmovavg {

//=====================================================================
// Moving average filter
//
// Simple in concept, sublime in implementation ... the fastest filter
// in the west.  Also optimal for the processing of time domain signals
// characterized by a transition edge.  The is the perfect signal filter
// for CW, RTTY and other signals of that type.  For a given filter size
// it provides the greatest s/n improvement while retaining the sharpest
// leading edge on the filtered signal.
//=====================================================================
    double[]	in = null;
    double	out;
    int		len, pint;
    boolean	empty;

    Cmovavg (int filtlen) {
        len = filtlen;
        in = new double[len];
        empty = true;
    }

    double run(double a) {
        if (in == null) {
            return a;
        }
	if (empty) {
            empty = false;
            for (int i = 0; i < len; i++) {
                in[i] = a;
            }
            out = a * len;
            pint = 0;
            return a;
        }
        out = out - in[pint] + a;
        in[pint] = a;
        if (++pint >= len) pint = 0;
        return out / len;
    }

    void setLength(int filtlen) {
	if (filtlen > len) {
            in = new double[filtlen];
        }
        len = filtlen;
        empty = true;
    }

    void reset() {
        empty = true;
    }

}
