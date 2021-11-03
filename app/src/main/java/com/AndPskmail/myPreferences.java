
/*
 * Preferences.java  
 *   
 * Copyright (C) 2011 John Douyere (VK2ETA)  
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
 * @author John Douyere <vk2eta@gmail.com>
 */



import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class myPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		addPreferencesFromResource(R.xml.preferences);

	}

	@Override
	protected void onResume() {
		super.onResume();

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced


		AndPskmail.splistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				// Implementation
				if (key.equals("RXMODE") || key.equals("AFREQUENCY") || key.equals("TXMODE") || key.equals("SLOWCPU")
					|| key.equals("CALL") || key.equals("SERVER")) {
					AndPskmail.RXParamsChanged = true;
				}

				/*
				if (key.equals("CALL")) {
					//remove un-allowed characters
					String cleanCall = AndPskmail.myconfig.getPreference("CALL", "N0CALL");
					cleanCall = cleanCall.replaceAll("[^a-zA-Z0-9\\/\\-]", "");
					if (cleanCall.trim().length() == 0) cleanCall = "N0CALL";
					SharedPreferences.Editor editor = AndPskmail.mysp
							.edit();
					editor.putString("CALL",cleanCall);
					// Commit the edits!
					editor.commit();
				}

				if (key.equals("SERVER")) {
					//remove un-allowed characters
					String cleanCall = AndPskmail.myconfig.getPreference("SERVER", "N0CALL");
					cleanCall = cleanCall.replaceAll("[^a-zA-Z0-9\\/\\-]", "");
					if (cleanCall.trim().length() == 0) cleanCall = "N0CALL";
					SharedPreferences.Editor editor = AndPskmail.mysp
							.edit();
					editor.putString("SERVER",cleanCall);
					// Commit the edits!
					editor.commit();
				}

				// Automatic Beacons
				if (key.equals("BEACON")) {
					boolean autobeacon = AndPskmail.myconfig.getPreferenceB("BEACON", false);
					if (autobeacon) {
						//Remove one minute for the GPS fix
						AndPskmail.myHandler.postDelayed(AndPskmail.prepareGPSforBeacon, AndPskmail.nextBeaconOrLinkDelay(true, false) - 60000);
					}

				}

				// Automatic Link Requests
				if (key.equals("AUTOLINK")) {
					boolean autolink = AndPskmail.myconfig.getPreferenceB("AUTOLINK", false);
					if (autolink) {
						AndPskmail.myHandler.postDelayed(AndPskmail.sendAndRequeueLink, AndPskmail.nextBeaconOrLinkDelay(false, false));
					}

				}
 */
			}
		};
		AndPskmail.mysp.registerOnSharedPreferenceChangeListener(AndPskmail.splistener);	

	}


}




