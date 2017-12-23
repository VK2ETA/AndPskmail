
/*
 * txstatus.java  
 *   
 * Copyright (C) 2008 Per Crusefalk (SM0RWO)
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
 * @author Per Crusefalk <per@crusefalk.se>
 */
public enum txstatus {

    /**
     * 
     */
    TXAbortreq,
    /**
     * 
     */
    TXPing,
    /**
     * 
     */
    TXInq,
    /**
     * 
     */
    TXBeacon,
    /**
     * 
     */
    TXUImessage,
    /**
     * 
     */
    TXaprsmessage,
    /**
     * 
     */
    TXlinkreq,
    /**
     * 
     */
    TXlinkack,
    /**
     * 
     */
    TXConnect_ack,
    /**
     * 
     */
    TTYConnect_ack,
    /**
     * 
     */
    TXConnect,
    /**
     * 
     */
    TXSummon,
    /**
     *
     */
    TTYConnect,
    /**
     * 
     */
    TXDisconnect,
     /**
     * 
     */
    TXAbort,   /**
     * 
     */
    TXPoll,
    /**
     * 
     */
    TXStat,
    /**
     * 
     */
    TXTraffic,
//    
    TXCQ
}
