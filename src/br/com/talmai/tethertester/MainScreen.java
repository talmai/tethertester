package br.com.talmai.tethertester;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class MainScreen extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.background));
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_screen);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new TetherTesterFragment()).commit();
		}
	}

	public void onConnectionResult(int result){
		if (result == TetherTesterFragment.DEVICE_IS_NOT_HOTSPOT){
			// nope! :(
		}
		else if (result == TetherTesterFragment.DEVICE_IS_HOTSPOT){
			// device is hotspot!
		}
	}
}
