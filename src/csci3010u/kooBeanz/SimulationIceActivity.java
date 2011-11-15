package csci3010u.kooBeanz;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
 
public class SimulationIceActivity extends Activity {
	 
	PhysicsWorld mWorld;
	 
	private Handler mHandler;
	 
	       
	 
	@Override
	 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	 
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		mWorld = new PhysicsWorld(this,dm.widthPixels,dm.heightPixels);
		this.setContentView(this.mWorld);

		// Add 10 Balls
		for (int i=0; i<10; i++) {
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
	}
	 
	@Override
	protected void onStop() {
		super.onStop();
	}
	 
}