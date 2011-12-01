package csci3010u.kooBeanz;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
 
public class SimulationIceActivity extends Activity implements OnTouchListener {
	 
	PhysicsWorld mWorld;
	 
	private Handler mHandler;
	
	private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private WakeLock mWakeLock;
    
    private static final String TAG = "ADELE.ACTIVITY";
	 
	       
	 
	@Override
	 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
		// Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get an instance of the PowerManager
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Get an instance of the WindowManager
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();

        // Create a bright wake lock
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass()
                .getName());
        
	 
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		mWorld = new PhysicsWorld(this,dm.widthPixels,dm.heightPixels, mSensorManager, mDisplay);
		this.setContentView(this.mWorld);
		
		mWorld.setOnTouchListener(this);

		// Add 10 Balls
		for (int i=0; i<2; i++) {
			mWorld.addBall();
		}
	 
	 
		// Start Regular Update
		mHandler = new Handler();
		mHandler.post(update);
	}
	 
	   
	@Override
	protected void onPause() {
		super.onPause();
		mHandler.removeCallbacks(update);
		
		/*
         * When the activity is paused, we make sure to stop the simulation,
         * release our sensor resources and wake locks
         */

        // Stop the simulation
        mWorld.stopSimulation();

        // and release our wake-lock
        mWakeLock.release();
	}
	 
	private Runnable update = new Runnable() {
		public void run() {
			mWorld.update();
			mHandler.postDelayed(update, (long) (10/mWorld.timeStep));
		}
	};
	 
	@Override
	protected void onResume() {
		super.onResume();
		
		/*
         * when the activity is resumed, we acquire a wake-lock so that the
         * screen stays on, since the user will likely not be fiddling with the
         * screen or buttons.
         */
        mWakeLock.acquire();

        // Start the simulation
        mWorld.startSimulation();
	}
	 
	@Override
	protected void onStop() {
		super.onStop();
	}


	@Override
	public boolean onTouch(View v, MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
		switch(e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mWorld.addBall(x, mWorld.World_H-y, 0, 0, mWorld.clientID);
				break;
		}
		
		Log.e( TAG, "*******TOUCH EVENT");
				
		return true;
	}
	 
}