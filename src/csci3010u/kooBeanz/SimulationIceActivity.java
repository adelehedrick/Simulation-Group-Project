package csci3010u.kooBeanz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jbox2d.common.Vec2;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
 
/**
 * The main (and only) Android activity which will create a physics world 
 * and then handle the touch, accelerometer, and network communication
 * between the device and the physics world.
 */
public class SimulationIceActivity extends Activity implements OnTouchListener, SensorEventListener {
	 
	PhysicsWorld pWorld;
	 
	private Handler mHandler;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private WakeLock mWakeLock;
    
    private static final String TAG = "ADELE.ACTIVITY";
	 
    /**
     * Accelerometer and gravity stuff
     */
    Vec2 gravity = new Vec2 (0, 0);
    Vec2 gravity_threshold = new Vec2 ((float)0.1, (float)0.1);
    private SensorManager mSensorManager;
	private Sensor mAccelerometer;    
    
    /**
     * Network stuff
     */
    private HttpClient httpclient = new DefaultHttpClient();
	private HttpPost httppost = new HttpPost();
    private String url = "http://leda.science.uoit.ca:15001";
    int clientID;
	 
    /**
     * When the activity is created this method
     * is called and will set everything up.
     */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
		// Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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

		//set up the physics world
		pWorld = new PhysicsWorld(this,dm.widthPixels,dm.heightPixels);
		this.setContentView(this.pWorld);
		pWorld.setOnTouchListener(this);
		
		//get a clientID and then generate paint based upon it
		registerOnServer();
		pWorld.setPaint();

		// Add some initial balls
		for (int i=0; i<2; i++) { pWorld.addToCreateList(); }
	 
	 
		// Start Regular Update
		mHandler = new Handler();
		mHandler.post(update);
		
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
	
		networkThread();
		
	}
	 
	/**
	 * This is the main UI thread that calls the update method
	 * in the physics world in which the simulation goes through 
	 * a "step" in the calculations
	 */
	private Runnable update = new Runnable() {
		public void run() {
			pWorld.update();
			mHandler.postDelayed(update, (long) (10/pWorld.timeStep));
		}
	};
	   
	@Override
	protected void onPause() {
		super.onPause();
		mHandler.removeCallbacks(update);
        mWakeLock.release();
	}
	 
	@Override
	protected void onResume() {
		super.onResume();
        mWakeLock.acquire();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
	}
	 
	@Override
	protected void onStop() {
		super.onStop();
		mSensorManager.unregisterListener(this);
	}

	
	/**
	 * 
	 * TOUCH STUFF
	 * 
	 */

	/**
	 * When a person touches the screen, a ball will be created 
	 * at that same location
	 */
	@Override
	public boolean onTouch(View v, MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
		switch(e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				pWorld.addBall(x, pWorld.World_H-y, 0, 0, pWorld.clientID);
				break;
		}		
		return true;
	}

	/**
	 * 
	 * ACCELLEROMETER STUFF
	 * 
	 */

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// 
		
	}


	/**
	 * When the accelerometer notices a change this method
	 * gets triggered and will get the new acceleration
	 * and then apply it to the physics world 
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		float mSensorX = 0, mSensorY = 0;
		
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
		
		switch (mDisplay.getRotation()) {
	        case Surface.ROTATION_0: 
	        	mSensorX = event.values[0];	
	        	mSensorY = event.values[1];
	            break;
	        case Surface.ROTATION_90:
	            mSensorX = -event.values[1];
	            mSensorY = event.values[0];
	            break;
	        case Surface.ROTATION_180:
	            mSensorX = -event.values[0];
	            mSensorY = -event.values[1];
	            break;
	        case Surface.ROTATION_270:
	            mSensorX = event.values[1];
	            mSensorY = -event.values[0];
	            break;
	    }
		
		/*
		 * If the gravity change is greater than the threshold
		 * then we will update the gravity in the physics world
		 */
		Vec2 gravity_new = new Vec2 (-mSensorX, -mSensorY);
        if (Math.abs(gravity_new.x - gravity.x) > gravity_threshold.x
        		|| Math.abs(gravity_new.y - gravity.y) > gravity_threshold.y) {
        	gravity = gravity_new;
        	pWorld.updateGravity(gravity);
        }
		
	}//end onSensorChange
	
	/**
	 * 
	 * 	NETWORK STUFF
	 * 
	 */
	 
	/**
	 * The never ending network thread that will send balls to the 
	 * server if there are any to send from the physics world,
	 * and will ping the server to see if there are any balls waiting
	 * to be received.
	 */
	private void networkThread() {
	    
		new Thread(new Runnable() {

		    
			public void run() {
				
				//stop running the thread when there is a connection
				//error with the server
				boolean workingFine = true;
				while(workingFine) {
					try {
			    		
						//if there are balls to send, then send them
						if (pWorld.balls_to_send.size() > 0)
							while (pWorld.balls_to_send.size() > 0) {
								
								httppost.setURI(new URI(url+pWorld.balls_to_send.get(0)));
						        HttpResponse response = httpclient.execute(httppost);
						        
						        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
						        StringBuilder builder = new StringBuilder();
						        for (String line = null; (line = reader.readLine()) != null;) {
						            builder.append(line).append("\n");
						        }
						        
						        pWorld.balls_to_send.remove(0);
							}
						
						/*
						 * Now ping the server to see if there are any 
						 * balls to receive
						 */
						httppost.setURI(new URI(url+"/ping?cid="+clientID));
				        HttpResponse response = httpclient.execute(httppost);				        
				        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				     
				        for (String line = null; (line = reader.readLine()) != null;) {
				        	String[] args = line.split(",");
				        	pWorld.balls_to_create.add( new BallToAdd(
				        			(args[3].equalsIgnoreCase("-1") ? 20 : pWorld.World_W-20),	//x
				        			Float.parseFloat(args[0])*pWorld.World_H,					//y
			        				Float.parseFloat(args[1]),									//v_x
			        				Float.parseFloat(args[2]),									//v_y
			        				Integer.parseInt(args[4])));								//colour
				        }

				        Thread.sleep(50);
				        
					} catch (UnsupportedEncodingException e) {
						Log.e( TAG, e.getMessage());
						workingFine = false;
						//e.printStackTrace();
					} catch (URISyntaxException e) {
						Log.e( TAG, e.getMessage());
						workingFine = false;
					} catch (ClientProtocolException e) {
						Log.e( TAG, e.getMessage());
						workingFine = false;
					} catch (IOException e) {
						Log.e( TAG, e.getMessage());
						workingFine = false;
					} catch (InterruptedException e) {
						workingFine = false;
					}
					
				}//end while working fine
			} //end run
		}).start();
		
	}//end networkThread


	/**
	 * Register the client on the server to obtain a client id
	 * which is then used to determine the colour of the client's 
	 * world/balls
	 */
	public void registerOnServer() {
	
		try {
			
			httppost.setURI(new URI(url+"/register?w="+pWorld.World_W+"&h="+pWorld.World_H));
	        HttpResponse response = httpclient.execute(httppost);
	        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
	        String input = reader.readLine();
	  
	        if (!input.equals(null)) {
	        	clientID = Integer.parseInt(input);
	        	pWorld.clientID = this.clientID;
	        } else Log.e( TAG, "invalid reponse from server");
	        
	        Log.d( TAG, "******clientID: "+clientID);
	
	        
		} catch (UnsupportedEncodingException e) {
			Log.e( TAG, e.getMessage());
			//e.printStackTrace();
		} catch (URISyntaxException e) {
			Log.e( TAG, e.getMessage());
		} catch (ClientProtocolException e) {
			Log.e( TAG, e.getMessage());
		} catch (IOException e) {
			Log.e( TAG, e.getMessage());
		} 	
		
	}
}