package br.com.talmai.tethertester;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;

import android.app.Fragment;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

public class Hotspot {
	private String TAG = "Hotspot";
	private String WIFI_AP_SSID = "TetherTester";
	private String WIFI_AP_PSK = "TetherTester";
	
	private WifiManager wifiManager;
	private Fragment fragment = null;
    
    private static int WIFI_AP_STATE_UNKNOWN = -1;
    private static int WIFI_AP_STATE_DISABLING = 0;
    private static int WIFI_AP_STATE_DISABLED = 1;
    private static int WIFI_AP_STATE_ENABLING = 2;
    private static int WIFI_AP_STATE_ENABLED = 3;
    private static int WIFI_AP_STATE_FAILED = 4;
    private final String[] WIFI_STATE_TEXTSTATE = new String[] {
    		"DISABLING","DISABLED","ENABLING","ENABLED","FAILED"
    };
    private int stateWifiWasIn = WIFI_AP_STATE_UNKNOWN;

    public static int STATUS_NOT_STARTED = -1;
    public static int STATUS_DISABLING_WIFI = 0;
    public static int STATUS_TESTING_AP = 1;
    public static int STATUS_AP_ENABLED = 2;
    public static int STATUS_AP_DISABLED = 3;
    private int currentTestingStatus = STATUS_NOT_STARTED;
    
    public void startProcessConnectionCityEye(WifiManager wifihandler, Fragment frg) {
    	if (wifiManager==null){
    		wifiManager = wifihandler;
    	}
    	this.fragment = frg;

        //remember wireless current state
    	stateWifiWasIn = wifiManager.getWifiState();

    	doNextStep();
    }
    
    public void doNextStep(){
    	new SetWifiAPTask().execute();    	
    }

    public boolean inUsableState() {
    	return (getWifiAPState() == WIFI_AP_STATE_ENABLED ||
    			getWifiAPState() == WIFI_AP_STATE_ENABLING);
    }

    /**
     * Get the wifi AP state
     * @return WifiAP state
     */
    private int getWifiAPState() {
    	try {
	    	Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
		    //method.setAccessible(true); //in the case of visibility change in future APIs
	        Log.w(TAG, "getWifiAPState(): checking isWifiApEnabled " + (Boolean) method.invoke(wifiManager));
        } catch (Exception e) {
        }
    	    
    	int constant = 0;
        int state = WIFI_AP_STATE_UNKNOWN;
        try {
            Method method2 = wifiManager.getClass().getMethod("getWifiApState");
            state = (Integer) method2.invoke(wifiManager);
        } catch (Exception e) {

        }

        if(state>=10){
            //using Android 4.0+ (or maybe 3+, haven't had a 3 device to test it on) so use states that are +10
            constant=10;
        }

        //reset these in case was newer device
        WIFI_AP_STATE_DISABLING = 0+constant;
        WIFI_AP_STATE_DISABLED = 1+constant;
        WIFI_AP_STATE_ENABLING = 2+constant;
        WIFI_AP_STATE_ENABLED = 3+constant;
        WIFI_AP_STATE_FAILED = 4+constant;
        
        //Log.w(TAG, "getWifiAPState.state " + (state==-1?"UNKNOWN":WIFI_STATE_TEXTSTATE[state-constant]));
        return state;
    }
    
    private void changeStateWifi(boolean enable){
        if (wifiManager.getConnectionInfo() != null) {
            Log.w(TAG, "changeStateWifi(" + enable + ")");
            wifiManager.setWifiEnabled(enable);
            // spin until we get confirmation, or fail
            int loopMax = 10;

            while(loopMax > 0 && 
            		(wifiManager.getWifiState() != WIFI_AP_STATE_DISABLED || 
            		 wifiManager.getWifiState() != WIFI_AP_STATE_ENABLING ||
            		 wifiManager.getWifiState() != WIFI_AP_STATE_FAILED)){
//                Log.w(TAG, "changing state wifi: waiting, pass: " + (10-loopMax));
                try {
                    Thread.sleep(500);
                    loopMax--;
                } catch (Exception e) {
                }
            }
            Log.w(TAG, "changing state wifi(" + enable + "): done, pass: " + (10-loopMax));
        }
    }

    private void enableWifiAP(boolean enable){
    	//enable/disable wifi ap
    	int state = WIFI_AP_STATE_UNKNOWN;

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = WIFI_AP_SSID;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.preSharedKey = WIFI_AP_PSK;
        config.hiddenSSID = false;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
    	
    	try {
            Log.w(TAG, "enableWifiAP()");
            Method method1 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method1.invoke(wifiManager, config, enable);
            state = getWifiAPState(); // force update before proceeding..
    	} catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        int loopMax = 10;
        if (!enable) {
            while (loopMax>0 && (state==WIFI_AP_STATE_DISABLING || state==WIFI_AP_STATE_ENABLED || state==WIFI_AP_STATE_FAILED)) {
                Log.w(TAG, "disabling wifi ap: waiting, pass: " + (10-loopMax));
                try {
                    Thread.sleep(500);
                    loopMax--;
                    state = getWifiAPState();
                } catch (Exception e) {
                }
            }
        } 
        else {
            while (loopMax>0 && (state==WIFI_AP_STATE_ENABLING || state==WIFI_AP_STATE_DISABLED || state==WIFI_AP_STATE_FAILED)) {
                Log.w(TAG, "enabling wifi ap: waiting, pass: " + (10-loopMax));
                try {
                    Thread.sleep(500);
                    loopMax--;
                    state = getWifiAPState();
                } catch (Exception e) {
                }
            }
        }
        
        Log.w(TAG, "enabling/disabling wifi ap: done, pass: " + (10-loopMax));
    }

    private void waitConnection(){
    	boolean alive = true;
		int totalTime = (int) fragment.getResources().getInteger(R.dimen.tethering_timeout);

		while (alive) {
			try {
				Thread.sleep(1000);
				totalTime--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

	        Log.w(TAG, "waitConnection(): " + totalTime);
			
			if (numberOfClientsConnected() > 0) {
				// \o/
				currentTestingStatus = STATUS_AP_ENABLED;
				alive = false;
			} else {
				if (totalTime == 0) {
					// not able to connect
					currentTestingStatus = STATUS_AP_DISABLED;
					alive = false;
				}
			}
		}
	}
    
    private int numberOfClientsConnected() {
		int macCount = 0;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			String line;
			while ((line = br.readLine()) != null) {
				String[] splitted = line.split(" +");
				if (splitted != null && splitted.length >= 4) {
					String mac = splitted[3];
					if (mac.matches("..:..:..:..:..:..")) {
						macCount++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return macCount;
	}
    
    private void updateStatus(){
		try {
			((TetherTesterFragment) fragment).updateStatusDisplay(currentTestingStatus);
		} 
		catch (IllegalArgumentException e) {
		}
    }
    
    class SetWifiAPTask extends AsyncTask<Void, Void, Void> {
        public SetWifiAPTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			updateStatus();
		}

        @Override
        protected Void doInBackground(Void... params) {
        	if (currentTestingStatus == STATUS_NOT_STARTED) {
        		currentTestingStatus = STATUS_DISABLING_WIFI;
        		Log.w(TAG, "###########\n STATUS_DISABLING_WIFI\n###########");
        		changeStateWifi(false);
        	}
        	else if (currentTestingStatus == STATUS_DISABLING_WIFI) {
        		currentTestingStatus = STATUS_TESTING_AP;
        		Log.w(TAG, "###########\n STATUS_TESTING_AP\n###########");
        		enableWifiAP(true);
        	}
        	else if (currentTestingStatus == STATUS_TESTING_AP) {
        		Log.w(TAG, "###########\n STATUS_AWAITING_CONNECTION_AP\n###########");
        		waitConnection();
        	}
            return null;
        }
    }    
}