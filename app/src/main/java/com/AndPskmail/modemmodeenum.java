/*
 * modemmodeenum.java
 *
 * Copyright (C) 2008 P�r Crusefalk (SM0RWO)
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
 * @author per
 */
public enum modemmodeenum {
//ordered in descending order of robustness
//THE ORDER MUST MATCH the list in MODEM.JAVA
	MFSK8, THOR8, MFSK16, THOR11, THOR16, PSK31, THOR22, MFSK32, PSK125R, PSK63, PSK250R,

	PSK125, MFSK64, PSK500R, PSK250, PSK500
//parked the other modes here for the time being
//    MFSK22,
//    MFSK31,
//    THOR4,
//    THOR5,
//    PSK63F
     }
