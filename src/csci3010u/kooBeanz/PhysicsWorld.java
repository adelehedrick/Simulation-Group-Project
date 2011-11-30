package csci3010u.kooBeanz;




import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
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
	private HashMap<String,Paint> body_paint = new HashMap<String,Paint>();
	private HashMap<String,Integer> body_paint_id = new HashMap<String,Integer>();
	private float radius=10;
	
	/**
	 * Accelerometer stuff
	 */
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Display mDisplay;
	private float mSensorX;
    private float mSensorY;

    private Vec2 gravity;
    private Vec2 gravity_threshold = new Vec2((float) 0.1, (float) 0.1);
    

    /**
     * Network stuff
     */
    private HttpClient httpclient = new DefaultHttpClient();
	private HttpPost httppost = new HttpPost();
    private String url = "http://leda.science.uoit.ca:15001";
    private int clientID;
    private ArrayList<String> balls_to_send = new ArrayList<String>();

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
		
		switch(clientID%5) {
		case 0:
			paint.setColor(Color.RED);
			break;
		case 1:
			paint.setColor(Color.BLUE);
			break;
		case 2:
			paint.setColor(Color.GREEN);
			break;
		case 3:
			paint.setColor(Color.MAGENTA);
			break;
		case 4:
			paint.setColor(Color.YELLOW);
			break;
		default:
			paint.setColor(Color.RED);
			break;
			
		}
		
		startNetworkReciever();
		

	}

	private void startNetworkReciever() {
		
		
	    
		new Thread(new Runnable() {

		    
			public void run() {
				
				boolean workingFine = true;
				while(workingFine) {
					try {
			    		
						if (balls_to_send.size() > 0)
							while (balls_to_send.size() > 0) {
								
								httppost.setURI(new URI(url+balls_to_send.get(0)));
								
								// Execute HTTP Post Request
						        HttpResponse response = httpclient.execute(httppost);
						        
						        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
						        StringBuilder builder = new StringBuilder();
						        for (String line = null; (line = reader.readLine()) != null;) {
						            builder.append(line).append("\n");
						        }
						        
						        Log.e( TAG, "******SEND BALL RESPONSE: "+builder);
						        balls_to_send.remove(0);
							}
						
						httppost.setURI(new URI(url+"/ping?cid="+clientID));
						
						// Execute HTTP Post Request
				        HttpResponse response = httpclient.execute(httppost);
				        
				        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				     
				        for (String line = null; (line = reader.readLine()) != null;) {
				            
				        	String[] args = line.split(",");
				        	
				        	if (args[3].equalsIgnoreCase("-1")) {
				        		addBall(20, Float.parseFloat(args[0])*World_H,
				        				Float.parseFloat(args[1]),
				        				Float.parseFloat(args[2]),
				        				Integer.parseInt(args[4]));
				        	} else {
				        		addBall(World_W-20, Float.parseFloat(args[0])*World_H,
				        				Float.parseFloat(args[1]),
				        				Float.parseFloat(args[2]),
				        				Integer.parseInt(args[4]));
				        	}
				        	
				            Log.e( TAG, "******PING RESPONSE: "+line);
				        }
				        
				       
				        
				        Thread.sleep(200);
				        
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
		
				
		    		
		    		
		try {
			
			httppost.setURI(new URI(url+"/register?w="+World_W+"&h="+World_H));
			
			// Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);
	        
	       BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));


	        
	        String input = reader.readLine();
	  
	        if (!input.equals(null))
	        	clientID = Integer.parseInt(input);
	        else Log.e( TAG, "invalid reponse from server");
	        
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
	
	public void sendBall(final int from_, final int to_, final float y_pos_, final float v_x_, final float v_y_, final int colour) {
		
		balls_to_send.add("/update?cid="+from_+
				"&side="+to_+
				"&y="+y_pos_+
				"&vx="+v_x_+
				"&vy="+v_y_+
				"&colour="+colour);

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
		
		
		Random rnd = new Random();

		addBall((float) radius*2+rnd.nextInt( (int)(World_W-radius*4) ), 	//x
				(float)2*radius+ rnd.nextInt( (int)(World_H-radius*4) ), 	//y
				0, 															//v_x
				0,															//v_y
				clientID);       											//colour

	}
	
	public void addBall (float x, float y, float v_x, float v_y, int colour) {
		
		if (colour == clientID) {
			body_paint.put("Ball"+count, paint);
			body_paint_id.put("Ball"+count, clientID%5);
		} else {
			body_paint.put("Ball"+count, new Paint());
			body_paint.get("Ball"+count).setStyle(Style.FILL);
			body_paint_id.put("Ball"+count, colour);
			
			switch(colour) {
			case 0:
				body_paint.get("Ball"+count).setColor(Color.RED);
				break;
			case 1:
				body_paint.get("Ball"+count).setColor(Color.BLUE);
				break;
			case 2:
				body_paint.get("Ball"+count).setColor(Color.GREEN);
				break;
			case 3:
				body_paint.get("Ball"+count).setColor(Color.MAGENTA);
				break;
			case 4:
				body_paint.get("Ball"+count).setColor(Color.YELLOW);
				break;
			default:
				body_paint.get("Ball"+count).setColor(Color.RED);
				break;
				
			}
		}
			
		
		// Create Dynamic Body
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		new_bodies.put("Ball"+count, world.createBody(bodyDef));

		// Create Shape with Properties
		CircleDef circle = new CircleDef();
		circle.radius = (float) radius;
		circle.density = (float) 1.0;
		circle.restitution=1.0f;

		// Assign shape to Body
		new_bodies.get("Ball"+count).setUserData("Ball"+count);
		new_bodies.get("Ball"+count).createShape(circle);
		new_bodies.get("Ball"+count).setMassFromShapes();
		new_bodies.get("Ball"+count).m_linearDamping = 0;
		//new_bodies.get("Ball"+count).wakeUp();
		new_bodies.get("Ball"+count).setLinearVelocity(new Vec2(v_x, v_y));
		//new_bodies.get("Ball"+count).applyImpulse(new Vec2(v_x, v_y), new_bodies.get("Ball"+count).getPosition());
		new_bodies.get("Ball"+count).wakeUp();
		//new_bodies.get("Ball"+count).setLinearVelocity(new Vec2(5, -4));
		Log.e( TAG, "******SET VELOCITY: "+v_x+ " "+v_y);
		
		Log.e( TAG, "******GET VELOCITY: "+new_bodies.get("Ball"+count).getLinearVelocity().x+ " "+new_bodies.get("Ball"+count).getLinearVelocity().y);
		
		
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
			canvas.drawCircle(new_bodies.get(key).getPosition().x,World_H- new_bodies.get(key).getPosition().y, radius, body_paint.get(key));
			
		
		
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
						sendBall(clientID, 	//from
								1, 	//to
								new_bodies.get((String)point.shape2.getBody().getUserData()).getPosition().y/World_H, 	//y_pos
								new_bodies.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().x, 	//v_x
								new_bodies.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().y,		//v_y
								body_paint_id.get((String)point.shape2.getBody().getUserData()));						//colour
						
						new_bodies.remove((String)point.shape2.getBody().getUserData());
						
			
					} else if (((String)point.shape1.getBody().getUserData()).contentEquals("Right Portal")) {
						Log.d( TAG, "*******PORTAL HIT");
						
						sendBall(clientID, 	//from
								-1, 	//to
								new_bodies.get((String)point.shape2.getBody().getUserData()).getPosition().y/World_H, 	//y_pos
								new_bodies.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().x, 	//v_x
								new_bodies.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().y,		//v_y
								body_paint_id.get((String)point.shape2.getBody().getUserData()));						//colour
						
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
        
        computePhysics();
		
	}

}