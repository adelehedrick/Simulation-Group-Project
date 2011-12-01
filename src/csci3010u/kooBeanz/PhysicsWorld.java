package csci3010u.kooBeanz;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

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
import android.view.View;

public class PhysicsWorld extends View {
	
	private static final String TAG = "ADELE.PhysicsWorld";

	/*
	 * Physics engine stuff
	 */
	protected static final int GUIUPDATEIDENTIFIER = 0x231;
	public int targetFPS = 60;
	public float timeStep = 10.0f / targetFPS;
	public int iterations = 5 ;
	
	/*
	 * Ball stuff
	 */
	private HashMap<String,Body> balls = new HashMap<String,Body>();
	private float radius=10;			//size of the balls
	private int count = 0;				//unique identifier for the balls
	
	/*
	 * World definition stuff
	 */
	private AABB worldAABB;
	public World world;
	private PolygonDef groundShapeDef;
	public int World_W,World_H;
	
	/*
	 * Colour stuff
	 */
	private Paint paint;
	private HashMap<String,Paint> ball_paint = new HashMap<String,Paint>();
	private HashMap<String,Integer> ball_paint_id = new HashMap<String,Integer>();
	
	
	/* Balls that either need to be created in the physics
	 * world or removed from the physics world
	 */
	private Vector<Body> balls_to_destroy = new Vector<Body>();
	public Vector<BallToAdd> balls_to_create = new Vector<BallToAdd>();
	

	/* This gets updated by the network thread in the activity */
    private Vec2 gravity = new Vec2((float) 0.0, (float) 0.0);
    

    /**
     * Network stuff
     */
    int clientID;
    public ArrayList<String> balls_to_send = new ArrayList<String>();

	public PhysicsWorld(Context context,int W,int H) {
		super(context);
		World_W=W;
		World_H=H;

		worldAABB = new AABB();
		Vec2 min = new Vec2(-50, -50);
		Vec2 max = new Vec2(World_W + 50, World_H + 50);
		worldAABB.lowerBound.set(min);
		worldAABB.upperBound.set(max);

		boolean doSleep = true;
		world = new World(worldAABB, gravity, doSleep);

		setContactListener();
		
		createGroundBox();
	}
	
	public void setPaint() {
		paint=new Paint();
		paint.setStyle(Style.FILL);
		
		switch(clientID%5) {
			case 0: paint.setColor(Color.RED);		break;
			case 1: paint.setColor(Color.BLUE);		break;
			case 2: paint.setColor(Color.GREEN);	break;
			case 3: paint.setColor(Color.MAGENTA);	break;
			case 4: paint.setColor(Color.YELLOW);	break;
			default: paint.setColor(Color.RED);		break;
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


	public synchronized void addToCreateList() {
		
		
		Random rnd = new Random();
		
		balls_to_create.add( new BallToAdd(
				(float) radius*2+rnd.nextInt( (int)(World_W-radius*4) ),//x
				(float) radius*2+rnd.nextInt( (int)(World_W-radius*4) ),//y
				0,														//v_x
				0,														//v_y
				clientID));												//colour
	}
	
	public synchronized void addBall (float x, float y, float v_x, float v_y, int colour) {
		
		String id = "Ball"+count;
		// Increase Counter
		count += 1;
		
		if (colour == clientID) {
			ball_paint.put(id, paint);
			ball_paint_id.put(id, clientID%5);
		} else {
			ball_paint.put(id, new Paint());
			ball_paint.get(id).setStyle(Style.FILL);
			ball_paint_id.put(id, colour);
			
			switch(colour) {
				case 0: ball_paint.get(id).setColor(Color.RED); 	break;
				case 1: ball_paint.get(id).setColor(Color.BLUE); 	break;
				case 2: ball_paint.get(id).setColor(Color.GREEN); 	break;
				case 3: ball_paint.get(id).setColor(Color.MAGENTA);	break;
				case 4: ball_paint.get(id).setColor(Color.YELLOW);	break;
				default: ball_paint.get(id).setColor(Color.RED);	break;	
			}
		}
			
		
		// Create Dynamic Body
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		balls.put(id, world.createBody(bodyDef));

		// Create Shape with Properties
		CircleDef circle = new CircleDef();
		circle.radius = (float) radius;
		circle.density = (float) 1.0;
		circle.restitution=1.0f;

		// Assign shape to Body
		balls.get(id).createShape(circle);
		balls.get(id).setMassFromShapes();
		balls.get(id).m_linearDamping = 0;
		balls.get(id).setLinearVelocity(new Vec2(v_x, v_y));
		balls.get(id).wakeUp();
		balls.get(id).setUserData(id);
						
	} //end addBall

	public void update() {
		
		//destroy all the balls in the "to destroy" list
		while (balls_to_destroy.size() > 0) {
			world.destroyBody(balls_to_destroy.firstElement());
			balls_to_destroy.removeElementAt(0);
		}
		
		//create all the balls in the "to create" list
		while (balls_to_create.size() > 0) {
			addBall(	balls_to_create.get(0).x,
						balls_to_create.get(0).y,
						balls_to_create.get(0).v_x,
						balls_to_create.get(0).v_y,
						balls_to_create.get(0).colour
					);
			balls_to_create.removeElementAt(0);
		}
		
		//now it is safe to do an iteration
		world.step(   timeStep  , iterations);
		postInvalidate();
	}  

	@Override
	protected void onDraw(Canvas canvas) {
		
		// draw balls
		for(String key: balls.keySet()) 
			canvas.drawCircle(balls.get(key).getPosition().x,	
					World_H- balls.get(key).getPosition().y, 
					radius, ball_paint.get(key));
			
		//right portal
		canvas.drawRect(new Rect(World_W-10, 0, World_W, World_H), paint);
		
		//left portal
		canvas.drawRect(new Rect(0, 0, 10, World_H), paint);
	}

	public void updateGravity (Vec2 gravity) {
		world.setGravity(gravity);
	}
	
	private void setContactListener() {
		
		world.setContactListener(new ContactListener() {

			@Override
			public void add(ContactPoint point) {
				int to = 0;
				
				if (balls.containsKey((String)point.shape2.getBody().getUserData()))
					if (((String)point.shape1.getBody().getUserData()).contentEquals("Left Portal")) 
						to = 1;
					else if (((String)point.shape1.getBody().getUserData()).contentEquals("Right Portal"))
						to = -1;
				
				if (to != 0) {
					sendBall(clientID, 	//from
							to, 	//to
							balls.get((String)point.shape2.getBody().getUserData()).getPosition().y/World_H, 	//y_pos
							balls.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().x, 	//v_x
							balls.get((String)point.shape2.getBody().getUserData()).getLinearVelocity().y,		//v_y
							ball_paint_id.get((String)point.shape2.getBody().getUserData()));						//colour
					
					balls.remove((String)point.shape2.getBody().getUserData());
					balls_to_destroy.add(point.shape2.getBody());
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
	} //end setContactListener
	

}