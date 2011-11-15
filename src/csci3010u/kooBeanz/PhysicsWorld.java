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
import android.view.View;

public class PhysicsWorld extends View{

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

	public PhysicsWorld(Context context,int W,int H) {
		super(context);
		World_W=W;
		World_H=H;

		// Step 1: Create Physics World Boundaries
		worldAABB = new AABB();
		Vec2 min = new Vec2(-50, -50);
		Vec2 max = new Vec2(World_W + 50, World_H + 50);
		worldAABB.lowerBound.set(min);
		worldAABB.upperBound.set(max);

		// Step 2: Create Physics World with Gravity
		Vec2 gravity = new Vec2((float) 0.0, (float) -20.0);
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
		//circle.restitution=0.0f;

		// Assign shape to Body
		bodies[count].createShape(circle);
		bodies[count].setMassFromShapes();
		bodies[count].m_linearDamping = 1;
		

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
		
		canvas.drawRect(50, 50, 50, 50, paint);
	}

}