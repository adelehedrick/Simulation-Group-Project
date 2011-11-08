package csci3010u.kooBeanz;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.jbox2d.collision.PolygonDef;
import org.jbox2d.collision.PolygonShape;
import org.jbox2d.collision.CircleShape;
import org.jbox2d.collision.Shape;
import org.jbox2d.collision.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ToggleButton;

public class SimulationIceActivity extends Activity
{
    private GLSurfaceView mGLView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mGLView = new ClearGLSurfaceView(this, (SensorManager) getSystemService(SENSOR_SERVICE));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(mGLView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(Menu.NONE, 1, Menu.NONE, "Visit bayninestudios.com");
        menu.add(Menu.NONE, 2, Menu.NONE, "Toggle Edit");
        menu.add(Menu.NONE, 3, Menu.NONE, "Toggle Model");
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        	case 2:
        		((ClearGLSurfaceView) mGLView).toggleEdit();
        		return true;
        	case 3:
        		((ClearGLSurfaceView) mGLView).toggleModel();
        		return true;
        	case 1:
        		Intent myIntent = new Intent(Intent.ACTION_VIEW,
        				android.net.Uri.parse("http://www.bayninestudios.com"));
        		startActivity(myIntent);
        		return true;
        }
        return false;
    }
}


class ClearGLSurfaceView extends GLSurfaceView
{
    ClearRenderer mRenderer;
	
    public ClearGLSurfaceView(Context context, SensorManager sensorMgr) {
        super(context);
        mRenderer = new ClearRenderer(context, sensorMgr);
        setRenderer(mRenderer);
    }

    public void toggleEdit()
    {
    	mRenderer.toggleEdit();
    }

    public void toggleModel()
    {
    	mRenderer.switchModel();
    }
//    public boolean onKeyUp(final int keyCode, KeyEvent msg) {
//    	Log.d("key", "key event");
//    	queueEvent(new Runnable(){
//            public void run() {
//            	if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
//            	{
//            	} else if (keyCode == KeyEvent.KEYCODE_MENU)
//            	{
//            	} 
//            }});
//            return true;
//    }

    public boolean onTouchEvent(final MotionEvent event)
    {
    	// Q: Seriously? on every touch setSize!
    	// A: Yep. Until I find a better way
    	mRenderer.setSize(this.getWidth(),this.getHeight());

    	queueEvent(new Runnable(){
            public void run()
            {
        		mRenderer.touchEvent(event.getX(), event.getY(), event.getAction());
            }});
        	// if this isn't here, multiple events happen
	    	try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            return true;
        }
}

class ClearRenderer implements GLSurfaceView.Renderer
{
	private PhysicsWorld mWorld;
	private DrawModel mBox;
	private DrawModel mLongBox;
	private DrawModel mCircle;
	private int activeModel = 1;
	private boolean editMode = false;

	private Context mContext;
	SensorEventListener mSensorEventListener;
	private List<Sensor> sensors;
	private Sensor accSensor;
	private int width;
	private int height;
	
	private float startX, endX, startY, endY;
	
	public ClearRenderer(Context newContext, SensorManager sensorMgr)
	{
		mContext = newContext;
    	mBox = new DrawModel(new float[]{
    			  -1,-1,0,
				  1,-1,0,
				  1,1,0,
				  -1,1,0
				  },
				  new float[]{
	  			  0f,1f,
				  1f,1f,
				  1f,0f,
				  0f,0f
    			  },
				  new short[]{0,1,2,3,0},
    			  5);
    	mLongBox = new DrawModel(new float[]{
    			-.2f,-2f,0,
				.2f,-2f,0,
				.2f,2f,0,
				-.2f,2f,0
				  },
				  new short[]{0,1,2,3,0},
  			  5);
    	mCircle = new DrawModel(new float[]{
    		  0,0,0,
  			  0,1,0,
  			  -.5f,.866f,0,
  			  -.866f,.5f,0,
  			  -1,0,0,
  			  -.866f,-.5f,0,
  			  -.5f,-.866f,0,
  			  0,-1,0,
  			  .5f,-.866f,0,
  			  .866f,-.5f,0,
  			  1,0,0,
  			  .866f,.5f,0,
  			  .5f,.866f,0,
  			  0f,1f,0
    		},
    		new float[]{
      		  0.5f,0.5f,
			  0.5f,0.0f,
			  .25f,.067f,
			  .067f,.25f,
			  0.0f,0.5f,
			  .067f,.75f,
			  .25f,.933f,
			  0.5f,1.0f,
			  .75f,.933f,
			  .933f,.75f,
			  1.0f,0.5f,
			  .933f,.25f,
			  .75f,.067f,
			  .5f,.0f
      		},
    		new short[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
    		14);
        mWorld = new PhysicsWorld();
        mWorld.createWorld();
        mWorld.addBox(0f, -25f, 50f, 10f, 0f, false);
        mWorld.addBall(-5f, -15f, 5f, false);

        mSensorEventListener = new SensorEventListener()
        {	
			@Override
			public void onSensorChanged(SensorEvent event)
			{
	        	float xAxis = event.values[SensorManager.DATA_X];
	        	float yAxis = event.values[SensorManager.DATA_Y];
	        	mWorld.setGrav(-xAxis,-yAxis);
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		};

		sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(sensors.size() > 0)
        {
          accSensor = sensors.get(0);
        } 
		sensorMgr.registerListener(mSensorEventListener,
				accSensor,
				SensorManager.SENSOR_DELAY_UI);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
		GLU.gluOrtho2D(gl, -12f, 12f, -20f, 20f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_REPEAT);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_REPEAT);
        mBox.loadTexture(gl, mContext, R.drawable.box);
        mCircle.loadTexture(gl, mContext, R.drawable.soccerball);
    }

    public void onSurfaceChanged(GL10 gl, int w, int h)
    {
        gl.glViewport(0, 0, w, h);
    }

    public void drawActiveBody(GL10 gl)
    {
    	float x = 10f;
    	float y = 17f;

    	switch (activeModel) {
    		case 0: mCircle.draw(gl, x, y, 0f); break;
    		case 1: mBox.draw(gl, x, y, 0f); break;
    		case 2: mLongBox.draw(gl, x, y, 0f); break;
    	}
    }

    public void onDrawFrame(GL10 gl)
    {
    	if (editMode) {
        	// draw a red background if edit mode
    		gl.glClearColor(0.5f, 0, 0f, 1.0f);
    	} else {
    		// blue if not edit
			gl.glClearColor(0f, 0, 0.5f, 1.0f);
    	}
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
            GL10.GL_REPLACE);

        if (!editMode)
        {
	    	drawActiveBody(gl);
        }
    	gl.glColor4f(1f, 1f, 1f, 1f);  // white
    	Vec2 vec;

    	
    	if (editMode)
    	{
    		float midX = (startX+endX)/2f;
    		float midY = (startY+endY)/2f;
    		float sizeX = (endX - midX);
    		float sizeY = (endY - midY);
			float rotate = (float)Math.atan((double)(sizeY/sizeX));
    		float size = (float)Math.sqrt((double)((sizeX*sizeX)+(sizeY*sizeY)));
        	mBox.draw(gl, midX, midY, 0f, rotate*57.2957795f, size, .2f);
    	}

    	Body mBody = mWorld.getBodyList();
        do
        {
    		Shape mShape = mBody.getShapeList();
    		if (mShape != null)
    		{
		    	vec = mBody.getPosition();
		    	float rot = mBody.getAngle() * 57f;  // convert radians to degrees
        		if (ShapeType.POLYGON_SHAPE == mShape.getType())
        		{
        			Vec2[] vertexes = ((PolygonShape)mShape).getVertices();
        	    	mBox.draw(gl, vec.x, vec.y, 0f, rot, vertexes[2].x, vertexes[2].y);
        		}
        		else if (ShapeType.CIRCLE_SHAPE == mShape.getType())
        		{
        			float radius = ((CircleShape)mShape).m_radius;
			    	mCircle.draw(gl, vec.x, vec.y, 0f, rot, radius);
        		}
    		}
	        mBody = mBody.getNext();
        }
        while (mBody != null);
        mWorld.update();
    }

    // switches between which model gets dropped 
    public void switchModel()
    {
    	activeModel++;
    	if (activeModel > 1)
    	{
    		activeModel = 0;
    	}
    }

    // switches between which model gets dropped 
    public void toggleEdit()
    {
    	if (editMode == false) {
    		editMode = true;
    	} else {
    		editMode = false;
    	}
    }

    public void addLine()
    {
		float midX = (startX+endX)/2f;
		float midY = (startY+endY)/2f;
		float sizeX = (endX - midX);
		float sizeY = (endY - midY);
		float rotate = (float)Math.atan((double)(sizeY/sizeX));
		float size = (float)Math.sqrt((double)((sizeX*sizeX)+(sizeY*sizeY)));
        mWorld.addBox(midX, midY, size, .2f, rotate, false);
        startX = 0;
        startY = 0;
        endX = 0;
        endY = 0;
    }
    
    // interprets the touch event. Either switches models or
    // adds a model to the world.
    public void touchEvent(float x, float y, int eventCode)
    {
    	// look at all those magic numbers, truly magic!
    	// calculate world X and Y from the touch co-ords
    	float worldX = ((x-(this.width/2))*12f)/(this.width/2);
    	float worldY = ((y-(this.height/2))*-20f)/(this.height/2);

    	// if the user clicks lower than the box at the bottom
    	// switch models
//    	if (worldY < -15f)
//    	{
//    		if (eventCode == MotionEvent.ACTION_UP)
//    		{
//	    		if (worldX > 0f)
//	    		{
//	    			this.switchModel();
//	    		}
//	    		else
//	    		{
//	    			toggleEdit();
//	    		}
//    		}
//    	}
//    	else
//    	{
    		if (editMode == false)
    		{
    			if (eventCode == MotionEvent.ACTION_UP)
    			{
	    	    	switch (activeModel) {
		        		case 0: mWorld.addBall(worldX, worldY, 0.98f, true); break;
		        		case 1: mWorld.addBox(worldX, worldY, .98f, .98f, 0f, true); break;
		        		case 2: mWorld.addBox(worldX, worldY, .2f, 2f, 0f, true); break;
	    	    	}
    			}
    		}
    		else // edit mode
    		{
    			if (eventCode == MotionEvent.ACTION_DOWN)
    			{
    				startX = worldX;
    				startY = worldY;
    				endX = worldX;
    				endY = worldY;
    			}
    			else if (eventCode == MotionEvent.ACTION_MOVE)
    			{
    				endX = worldX;
    				endY = worldY;
    			}
    			else if (eventCode == MotionEvent.ACTION_UP)
    			{
    				endX = worldX;
    				endY = worldY;
    				addLine();
    			}
    		}
//    	}
    }
    
    // the easiest way to communicate to the renderer what the
    // size of the screen is
    public void setSize(int x, int y)
    {
    	this.width = x;
    	this.height = y;
    }
}