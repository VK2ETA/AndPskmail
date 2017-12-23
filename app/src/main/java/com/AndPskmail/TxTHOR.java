/*
 * TxTHOR.java
 *
 * @author John Douyere (VK2ETA). Translated from Fldigi's C++.
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


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class TxTHOR {

	double doublespacing = 1.0;

	public AudioTrack at = null; 
	private double frequency;
	private int volumebits;
	private double bandwidth;
	static final double TWOPI = 2 * Math.PI;
	static final double M_PI = Math.PI;
	double tonespacing;
	int samplerate = 8000;
	int symlen;
	int txprevtone;
	double[] outbuf;
	double txphase;
	//    static public SoundOutput soundout;
	ViterbiEncoder Enc;
	int bitshreg;
	int bitstate;
	int symbits;
	static final int THORNUMTONES = 18;
	static final int K = 7;
	static final int POLY1 = 0x6d;
	static final int POLY2 = 0x4f;
	interleave Txinlv;
	final int TX_STATE_PREAMBLE = 0;
	final int TX_STATE_START = 1;
	final int TX_STATE_DATA = 2;
	final int TX_STATE_FLUSH = 3;
	final int RX_STATE_DATA = 4;
	public boolean stopflag = false;
	int txstate = 0;
	int rxstate = 0;
	static public String SendText = "";
	int doublespaced = 1;



	public TxTHOR(modemmodeenum mode){


		String frequencySTR = AndPskmail.myconfig.getPreference("AFREQUENCY","1000");
		frequency = Integer.parseInt(frequencySTR);
		if (frequency < 500) frequency = 500;
		if (frequency > 2500) frequency = 2500;

		volumebits = Integer.parseInt(AndPskmail.myconfig.getPreference("VOLUME","8"));

		Enc = new ViterbiEncoder(K, POLY1, POLY2);
//		Txinlv = new interleave (symbits, interleave.INTERLEAVE_FWD);
		outbuf = new double[symlen];

		switch (mode) {
		// 11.025 kHz modes

		case THOR11:
			symlen = 1024;
			doublespaced = 1;
			samplerate = 11025;
			break;

		case THOR22:
			symlen = 512;
			doublespaced = 1;
			samplerate = 11025;
			break;
			// 8kHz modes
			/*          case THOR4:
                symlen = 2048;
                doublespaced = 2;
                samplerate = 8000;
                break;
			 */
		case THOR8:
			symlen = 1024;
			doublespaced = 2;
			samplerate = 8000;
			break;
		case THOR16:
		default:
			symlen = 512;
			doublespaced = 1;
			samplerate = 8000;
		}

		tonespacing = 1.0 * samplerate * doublespaced / symlen;
		bandwidth = THORNUMTONES * tonespacing;
		Txinlv = new interleave(4, interleave.INTERLEAVE_FWD); // 4x4x10

	}

	void tx_init()
	{
		txstate = TX_STATE_PREAMBLE;
		bitstate = 0;
		txprevtone = 0;
		txphase = 0;
	}

	void sendtone(int tone, int duration)
	{
		//adjust for 8Khz sampling		
		int adjSymlen = (int) (((double) symlen) * (8000.0 / (double) samplerate) + 0.5);
		double f, phaseincr;
		//		short [] outSbuffer = new short[symlen];
		short [] outSbuffer = new short[adjSymlen];

		f = (tone + 0.5) * tonespacing + frequency - bandwidth / 2;
		//		phaseincr = TWOPI * f / samplerate;
		phaseincr = TWOPI * f / 8000.0;
		for (int j = 0; j < duration; j++) {
			for (int i = 0; i < adjSymlen; i++) {
				//			outSbuffer[i] = (short) Math.cos(txphase);
				outSbuffer[i] = (short) ((int)(8386560.0 * Math.cos(txphase)) >> volumebits);
				txphase -= phaseincr;
				if (txphase > M_PI)
					txphase -= TWOPI;
				else if (txphase < M_PI)
					txphase += TWOPI;
			}
			//Catch the stopTX flag at this point as well
			if (!Modem.stopTX) at.write(outSbuffer, 0, adjSymlen);

		}
	}

	void sendsymbol(int sym)
	{
		//	complex z;
		int tone;

		tone = (txprevtone + 2 + sym) % THORNUMTONES;
		txprevtone = tone;
		//	if (reverse)
		//		tone = (THORNUMTONES - 1) - tone;
		sendtone(tone, 1);
	}

	// Send THOR FEC varicode

	void sendchar(int c, int secondary)
	{

		String code = thorvaricode.thorvarienc(c, secondary);

		while (code.length() > 0){
			int data = Enc.encode(code.charAt(0) - '0');
			for (int i = 0; i < 2; i++) {
				bitshreg = (bitshreg << 1) | ((data >> i) & 1);
				bitstate++;
				if (bitstate == 4) {
					bitshreg = Txinlv.bits(bitshreg);
					sendsymbol(bitshreg);
					bitstate = 0;
					bitshreg = 0;
				}
			}
			code = code.substring(1);
		}
		//	if (!secondary)
		//		put_echo_char(c);
	}

	void sendidle()
	{
		sendchar(0, 0);	// <NUL>
	}

	/*
void sendsecondary() {
	int c = get_secondary_char();
	sendchar(c & 0xFF, 1);
}
	 */

	void Clearbits()
	{
		int data = Enc.encode(0);
		for (int k = 0; k < 100; k++) {
			for (int i = 0; i < 2; i++) {
				bitshreg = (bitshreg << 1) | ((data >> i) & 1);
				bitstate++;
				if (bitstate == 4) {
					bitshreg = Txinlv.bits(bitshreg);
					bitstate = 0;
					bitshreg = 0;
				}
			}
		}
	}

	void flushtx()
	{
		// flush the varicode decoder at the other end
		// flush the convolutional encoder and interleaver
		//VK2ETA: 3 * 11 bits is not enough to completely flush the 160 bits interleaver
		//We need at least 80 bits into the Viterbi encoder (= 160 bits out)            
		// Fldigi seems to only get by due to the additional characters sent after the TX sequence
        // But this reduces the decoding capability of the end of sequence as
        //only one bit of the Viterbi encoder is correct
		//            for (int i = 0; i < 4; i++)
		for (int i = 0; i < 8; i++) 
			sendidle();		// <NULL> is 11 bits, so 7 * 11 = 77 bits
		sendchar(' ', 0);	// <SPACE> is 3 bits, so 80 bits in total
		bitstate = 0;
	}

	void Txprocess(byte[] str) {
		int c;

		Clearbits();
		for (int j = 0; j < 16; j++) sendsymbol(0);
		sendidle();
		sendchar('\r', 0);
		for(int i = 0; i < str.length; i++) {
			c = str[i];

			//Catch the stopTX flag at this point
			if (!Modem.stopTX) 	sendchar(c, 0);
		}
		flushtx();
	}




	void AddBytes(byte[] by) {

		//JD check the multiplier value for the buffer size
		int intSize = 10 * android.media.AudioTrack.getMinBufferSize(8000,
				AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

		if (AndPskmail.toBluetooth) {
			//JD Bluetooth hack test
			//        	at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
			at = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,  intSize , AudioTrack.MODE_STREAM);
		} else {
			at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize , AudioTrack.MODE_STREAM);
		}

		//Launch TX
		at.setStereoVolume(1.0f,1.0f);
		at.play();
		Txprocess(by); //Can be long running since it is in a worker's thread
		//Stop audio track
		at.stop();
		//Wait for end of audio play to avoid 
		//overlaps between end of TX and start of RX
		while (at.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		}
		//Close audio track
		at.release();
		//Reset the stop flag if it was ON
		Modem.stopTX = false;
	}

}
