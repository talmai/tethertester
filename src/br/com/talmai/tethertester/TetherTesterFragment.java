package br.com.talmai.tethertester;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class TetherTesterFragment extends Fragment {
	final String TAG = "TetherTesterFragment";
	static final String STATE_CURRENT = "currentState";
	
	private Hotspot hotspot = null;
	private WifiManager wifiManager = null;
	
	private ImageButton btnStart = null;
	private ImageView animationView = null;
	private TextView status = null;
	private int lastKnownState = Hotspot.STATUS_NOT_STARTED;
	
	public static final int DEVICE_IS_HOTSPOT = 0;
	public static final int DEVICE_IS_NOT_HOTSPOT = 1;

	public TetherTesterFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		View rootView = inflater.inflate(R.layout.fragment_main_screen, container, false);

		btnStart = (ImageButton) rootView.findViewById(R.id.startProcessButton);
		status = (TextView) rootView.findViewById(R.id.status);

		animationView = (ImageView) rootView.findViewById(R.id.animationView);
		animationView.setVisibility(View.GONE);		

		hotspot = new Hotspot();
		wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);

		   
	    // Check whether we're recreating a previously destroyed instance
	    if (savedInstanceState != null) {
	    	lastKnownState = savedInstanceState.getInt(STATE_CURRENT);
	    }
	    
		btnStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (hotspot != null) {
					btnStart.setVisibility(View.GONE);
					animationView.setVisibility(View.VISIBLE);

					animationView.setBackgroundResource(R.drawable.disable_wifi_animation);
					AnimationDrawable animationDrawable = (AnimationDrawable) animationView.getBackground();
					animationDrawable.start();
					
					status.setText(R.string.disabling_wifi);
					hotspot.startProcessConnectionCityEye(wifiManager, TetherTesterFragment.this, lastKnownState);

//					Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
//					for(Method method: wmMethods){
//						Log.w(TAG, "method: " + method.getName());
//					}
					
				}
			}
		});

		return rootView;
	}
	
	public void updateStatusDisplay(int currentState) {
		if (hotspot == null) return;
		
		lastKnownState = currentState; // save for savedInstanceState
		
		if (currentState == Hotspot.STATUS_NOT_STARTED) {
			//failed
		}
		else if (currentState == Hotspot.STATUS_DISABLING_WIFI){
			status.setText(R.string.tether_testing);
			hotspot.doNextStep();
		}
		else if (currentState == Hotspot.STATUS_TESTING_AP){
			animationView.setBackgroundResource(R.drawable.enable_ap_animation);
			AnimationDrawable animationDrawable = (AnimationDrawable) animationView.getBackground();
			animationDrawable.start();

			hotspot.doNextStep();
		}
		else if (currentState == Hotspot.STATUS_AP_ENABLED) {
			// \o/
			status.setText(R.string.tether_works);
			animationView.setBackgroundResource(R.drawable.green_bt);
			((MainScreen) getActivity()).onConnectionResult(DEVICE_IS_HOTSPOT);
		}
		else  if (currentState == Hotspot.STATUS_AP_DISABLED) {
			// not able to connect
			status.setText(R.string.tether_disabled);
			animationView.setBackgroundResource(R.drawable.red_bt);
			((MainScreen) getActivity()).onConnectionResult(DEVICE_IS_NOT_HOTSPOT);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    savedInstanceState.putInt(STATE_CURRENT, lastKnownState);
	    // save the view hierarchy state
	    super.onSaveInstanceState(savedInstanceState);
	}
	
}