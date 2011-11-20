package csci3010u.kooBeanz;




import java.util.Random;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;
import android.view.View;

public class PhysicsWorld extends View implements SensorEventListener{

	protected static final int GUIUPDATEIDENTIFIER = 0x231;
	public int targetFPS = 40;
	public float timeStep = 10.0f / targetFPS;
	public int iterations = 5;
	private Body[] bodies;
	private int count = 0;
	private AABB worldAABB;
	public World world;
	private PolygonDef groundShapeDef;
	public int World_W,World_H;
	private Paint paint;
	private float radius=10;
	
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Display mDisplay;
	private float mSensorX;
    private float mSensorY;
    private long mSensorTimeStamp;
    private long mCpuTimeStamp;
    
    private Vec2 gravity;
    private Vec2 gravity_threshold = new Vec2((float) 0.1, (float) 0.1);
	

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

     
		// Step 3:

		//Create Ground Box :

		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(new Vec2((float) 0.0, (float) -10.0));
		Body groundBody = world.createBody(bodyDef);
		groundShapeDef = new PolygonDef();
		groundShapeDef.setAsBox((float) World_W, (float) 10);
		groundBody.createShape(groundShapeDef);

		// up :
		bodyDef = new BodyDef();
		bodyDef.position.set(new Vec2((float) 0.0, (float) (World_H+10.0) ));
		groundBody = world.createBody(bodyDef);
		groundShapeDef = new PolygonDef();
		groundShapeDef.setAsBox((float) World_W, (float) 10);
		groundBody.createShape(groundShapeDef);

		// left :
		bodyDef = new BodyDef();
		bodyDef.position.set(new Vec2((float) -10, (float) 0.0 ));
		groundBody = world.createBody(bodyDef);
		groundShapeDef = new PolygonDef();
		groundShapeDef.setAsBox((float)10, (float) World_H);
		groundBody.createShape(groundShapeDef);

		// right :
		bodyDef = new BodyDef();
		bodyDef.position.set(new Vec2((float) World_W+10, (float) 0.0 ));
		groundBody = world.createBody(bodyDef);
		groundShapeDef = new PolygonDef();
		groundShapeDef.setAsBox((float)10, (float) World_H);
		groundBody.createShape(groundShapeDef);

		// step 4: initialize
		bodies=new Body[50];

		paint=new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(Color.RED);

	}



	public void addBall() {

		// Create Dynamic Body
		BodyDef bodyDef = new BodyDef();
		Random rnd = new Random();
		bodyDef.position.set((float) radius*2+rnd.nextInt( (int)(World_W-radius*4) ), (float)2*radius+ rnd.nextInt( (int)(World_H-radius*4) ));
		bodies[count] = world.createBody(bodyDef);

		// Create Shape with Properties
		CircleDef circle = new CircleDef();
		circle.radius = (float) radius;
		circle.density = (float) 1.0;
		circle.restitution=1.0f;

		// Assign shape to Body
		bodies[count].createShape(circle);
		bodies[count].setMassFromShapes();
		bodies[count].m_linearDamping = 0;
		

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


		for(int j = 0;j<count;j++) {
			canvas.drawCircle(bodies[j].getPosition().x,World_H- bodies[j].getPosition().y, radius, paint);
			
		}
		
		//canvas.drawRect(50, 50, 50, 50, paint);
	}


	/**
	 * Change gravity only if new gravity is greater than
	 * the threshold 
	 */
	public void computePhysics() {
        
        Vec2 gravity_new = new Vec2 (-mSensorX, -mSensorY);
        if (Math.abs(gravity_new.x - gravity.x) > gravity_threshold.x
        		|| Math.abs(gravity_new.y - gravity.y) > gravity_threshold.y) {
        	gravity = gravity_new;
        	world.setGravity(gravity);
        }
       
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