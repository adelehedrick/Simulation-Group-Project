package csci3010u.kooBeanz;




import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.ContactListener;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.ContactPoint;
import org.jbox2d.dynamics.contacts.ContactResult;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;

public class PhysicsWorld extends View implements SensorEventListener{
	
	private static final String TAG = "ADELE";

	/**
	 * Physics world stuff
	 */
	protected static final int GUIUPDATEIDENTIFIER = 0x231;
	public int targetFPS = 40;
	public float timeStep = 10.0f / targetFPS;
	public int iterations = 5;
	private HashMap<String,Body> new_bodies = new HashMap<String,Body>();
	private int count = 0;
	private AABB worldAABB;
	public World world;
	private PolygonDef groundShapeDef;
	public int World_W,World_H;
	private Paint paint;
	private float radius=10;
	
	/**
	 * Accelerometer stuff
	 */
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Display mDisplay;
	private float mSensorX;
    private float mSensorY;
    private long mSensorTimeStamp;
    private long mCpuTimeStamp;
    private Vec2 gravity;
    private Vec2 gravity_threshold = new Vec2((float) 0.1, (float) 0.1);
    

    /**
     * Network stuff
     */
    private HttpClient httpclient = new DefaultHttpClient();
	private HttpPost httppost = new HttpPost();
    private String url = "http://leda.science.uoit.ca:15000";
    private int clientID;
    private int leftPortal, rightPortal;

	public PhysicsWorld(Context context,int W,int H, SensorManager sm, Display d) {
		super(context);
		World_W=W;
		World_H=H;
		
		mSensorManager = sm;
		
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		mDisplay = d;

		// Step 1: Create Physics World Boundaries
		worldAABB = new AABB();
		Vec2 min = new Vec2(-50, -50);
		Vec2 max = new Vec2(World_W + 50, World_H + 50);
		worldAABB.lowerBound.set(min);
		worldAABB.upperBound.set(max);

		// Step 2: Create Physics World with Gravity
		gravity = new Vec2((float) 0.0, (float) 0.0);
		//Vec2 gravity = new Vec2((float) 0.0, (float) 0.0);
		boolean doSleep = true;
		world = new World(worldAABB, gravity, doSleep);

		setContactListener();
		
		registerOnServer();
     
		// Step 3:
		createGroundBox();


		paint=new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(Color.RED);
		
		startNetworkReciever();
		
		
		

	}

	private void startNetworkReciever() {
		
		
	    
		new Thread(new Runnable() {

		    
			public void run() {
				
				boolean workingFine = true;
				while(workingFine) {
					try {
			    		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				        nameValuePairs.add(new BasicNameValuePair("id", clientID+""));
				        
						httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
						
						httppost.setURI(new URI(url+"/get_mail"));
						
						// Execute HTTP Post Request
				        HttpResponse response = httpclient.execute(httppost);
				        
				        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				        StringBuilder builder = new StringBuilder();
				        for (String line = null; (line = reader.readLine()) != null;) {
				            builder.append(line).append("\n");
				        }
				        
				        Log.e( TAG, "******SERVER RESPONSE: "+builder);
				        
				        Thread.sleep(100);
				        
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
					
					
				}
			}
		}).start();
	}
	
	
	public void registerOnServer() {
		new Thread(new Runnable() {

		    
			public void run() {
				
		    		
		    		
				try {
		    		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			        nameValuePairs.add(new BasicNameValuePair("screen_height", World_H+""));
			       
			        
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					
					httppost.setURI(new URI(url+"/register"));
					
					// Execute HTTP Post Request
			        HttpResponse response = httpclient.execute(httppost);
			        
			        //BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		
			        //String[] line = reader.readLine().trim().split(" ");
			        

			        
			        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			        StringBuilder builder = new StringBuilder();
			        for (String line = null; (line = reader.readLine()) != null;) {
			            builder.append(line).append("\n");
			        }
			        
			        Log.d(TAG, "server said: "+builder);
			        
			        JSONObject obj = new JSONObject(builder.toString());
			        
			        clientID = Integer.parseInt(obj.getString("id"));
			        leftPortal = Integer.parseInt(obj.getString("left screen"));
			        rightPortal = Integer.parseInt(obj.getString("right screen"));
			        
			        Log.d( TAG, "******clientID: "+clientID+" leftPortal: "+leftPortal+" rightPortal: "+rightPortal);

			        
				} catch (UnsupportedEncodingException e) {
					Log.e( TAG, e.getMessage());
					//e.printStackTrace();
				} catch (URISyntaxException e) {
					Log.e( TAG, e.getMessage());
				} catch (ClientProtocolException e) {
					Log.e( TAG, e.getMessage());
				} catch (IOException e) {
					Log.e( TAG, e.getMessage());
				} catch (JSONException e) {
					Log.e( TAG, e.getMessage());
				}
				
			}
		}).start();
	}
	
	public void sendBall(final int from_, final String to_, final float y_pos_, final float v_x_, final float v_y_) {
		
		
		new Thread(new Runnable() {

		    
			public void run() {
				
		    		
		    		
				try {
		    		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			        nameValuePairs.add(new BasicNameValuePair("from", from_+""));
			        nameValuePairs.add(new BasicNameValuePair("to", to_));
			        nameValuePairs.add(new BasicNameValuePair("y_pos", y_pos_+""));
			        nameValuePairs.add(new BasicNameValuePair("v_x", v_x_+""));
			        nameValuePairs.add(new BasicNameValuePair("v_y", v_y_+""));
			        
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					
					httppost.setURI(new URI(url+"/send_mail"));
					
					// Execute HTTP Post Request
			        HttpResponse response = httpclient.execute(httppost);
			        
			        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			        StringBuilder builder = new StringBuilder();
			        for (String line = null; (line = reader.readLine()) != null;) {
			            builder.append(line).append("\n");
			        }
			        
			        Log.e( TAG, "******SERVER RESPONSE: "+builder);
			        
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
		}).start();
		
		
		
    	

    }//end send ball
	
	private void createGroundBox() {
		//Create Ground Box :

		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(new Vec2((float) 0.0, (float) -10.0));
		Body groundBody = world.createBody(bodyDef);
		groundShapeDef = new PolygonDef();
		groundShapeDef.setAsBox((float) World_W, (float) 10);
		groundBody.createShape(groundShapeDef);
		
		groundBody.setUserData("Bottom Wall");

		// up :
		bodyDef = new BodyDef();
		bodyDef.position.set(new Vec2((float) 0.0, (float) (World_H+10.0) ));
		groundBody = world.createBody(bodyDef);
		groundShapeDef = new PolygonDef();
		groundShapeDef.setAsBox((float) World_W, (float) 10);
		groundBody.createShape(groundShapeDef);
		
		groundBody.setUserData("Up Wall");

		// left :
		bodyDef = new BodyDef();
		bodyDef.position.set(new Vec2((float) -10, (float) 0.0 ));
		groundBody = world.createBody(bodyDef);
		groundShapeDef = new PolygonDef();
		groundShapeDef.setAsBox((float)10, (float) World_H);
		groundBody.createShape(groundShapeDef);
		
		groundBody.setUserData("Left Portal");

		// right :
		bodyDef = new BodyDef();
		bodyDef.position.set(new Vec2((float) World_W+10, (float) 0.0 ));
		Body groundBody2 = world.createBody(bodyDef);
		groundShapeDef = new PolygonDef();
		groundShapeDef.setAsBox((float)10, (float) World_H);
		groundBody2.createShape(groundShapeDef);
		
		groundBody2.setUserData("Right Portal");
	}


	public void addBall() {
		
		
		// Create Dynamic Body
		BodyDef bodyDef = new BodyDef();
		Random rnd = new Random();
		bodyDef.position.set((float) radius*2+rnd.nextInt( (int)(World_W-radius*4) ), (float)2*radius+ rnd.nextInt( (int)(World_H-radius*4) ));
		new_bodies.put("Ball"+count, world.createBody(bodyDef));

		// Create Shape with Properties
		CircleDef circle = new CircleDef();
		circle.radius = (float) radius;
		circle.density = (float) 1.0;
		circle.restitution=1.0f;

		// Assign shape to Body
		new_bodies.get("Ball"+count).createShape(circle);
		new_bodies.get("Ball"+count).setMassFromShapes();
		new_bodies.get("Ball"+count).m_linearDamping = 0;
		
		new_bodies.get("Ball"+count).setUserData("Ball"+count);
		
		// Increase Counter
		count += 1;        

	}

  

	public void update() {
		world.step(   timeStep  , iterations);
		postInvalidate();
	}  

	@Override
	protected void onDraw(Canvas canvas) {
		// draw balls
		
		for(String key: new_bodies.keySet()) 
			canvas.drawCircle(new_bodies.get(key).getPosition().x,World_H- new_bodies.get(key).getPosition().y, radius, paint);
			
		
		
		//right portal
		canvas.drawRect(new Rect(World_W-10, 0, World_W, World_H), paint);
		
		//left portal
		canvas.drawRect(new Rect(0, 0, 10, World_H), paint);
	}


	/**
	 * Change gravity only if new gravity is greater than
	 * the threshold 
	 */
	private void computePhysics() {
        
        Vec2 gravity_new = new Vec2 (-mSensorX, -mSensorY);
        if (Math.abs(gravity_new.x - gravity.x) > gravity_threshold.x
        		|| Math.abs(gravity_new.y - gravity.y) > gravity_threshold.y) {
        	gravity = gravity_new;
        	world.setGravity(gravity);
        }
       
    }
	
	private void setContactListener() {
		
		world.setContactListener(new ContactListener() {

			@Override
			public void add(ContactPoint point) {
				//this.add(point);
				if (new_bodies.containsKey((String)point.shape2.getBody().getUserData()))
					if (((String)point.shape1.getBody().getUserData()).contentEquals("Left Portal")) {
						Log.d( TAG, "*******PORTAL HIT");
						sendBall(clientID, 	//to
								"right", 	//from
								new_bodies.get((String)point.shape2.getBody().getUserData()).getPosition().y, 			//y_pos
								new_bodies.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().x, 	//v_x
								new_bodies.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().y);	//v_y
						
						new_bodies.remove((String)point.shape2.getBody().getUserData());
						
			
					} else if (((String)point.shape1.getBody().getUserData()).contentEquals("Right Portal")) {
						Log.d( TAG, "*******PORTAL HIT");
						
						sendBall(clientID, 	//to
								"left", 	//from
								new_bodies.get((String)point.shape2.getBody().getUserData()).getPosition().y, 			//y_pos
								new_bodies.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().x, 	//v_x
								new_bodies.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().y);	//v_y
						
						new_bodies.remove((String)point.shape2.getBody().getUserData());
					}
				
			}

			@Override
			public void persist(ContactPoint point) {
				//this.persist(point);
				
			}

			@Override
			public void remove(ContactPoint point) {
				//this.remove(point);
				
			}

			@Override
			public void result(ContactResult point) {
				//this.result(point);
				
			}
			
		});
	}
	

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}


	public void startSimulation() {
	    /*
	     * It is not necessary to get accelerometer events at a very high
	     * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
	     * automatic low-pass filter, which "extracts" the gravity component
	     * of the acceleration. As an added benefit, we use less power and
	     * CPU resources.
	     */
	    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
	}
	
	public void stopSimulation() {
	    mSensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        /*
         * record the accelerometer data, the event's timestamp as well as
         * the current time. The latter is needed so we can calculate the
         * "present" time during rendering. In this application, we need to
         * take into account how the screen is rotated with respect to the
         * sensors (which always return data in a coordinate space aligned
         * to with the screen in its native orientation).
         */
		

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

        mSensorTimeStamp = event.timestamp;
        mCpuTimeStamp = System.nanoTime();
        
        computePhysics();
		
	}

}