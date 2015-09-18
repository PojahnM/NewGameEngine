package pojahn.game.essentials;

import pojahn.game.core.Entity;
import pojahn.game.events.Event;
import pojahn.game.events.RenderEvent;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Utils {

	public static Entity wrap(RenderEvent renderEvent){
		return wrap(renderEvent, 0);
	}
	
	public static Entity wrap(RenderEvent renderEvent, int zIndex){
		return new Entity(){{
				this.zIndex(zIndex);
				this.id = "WRAPPER";
			}
			
			@Override
			public void render(SpriteBatch batch) {
				super.render(batch);
				renderEvent.eventHandling(batch);
			}
		};
	}
	
	public static Entity wrap(Event event){
		return new EntityBuilder().events(event).build();
	}
	
	public static <T> T getRandomElement(T[] array)
	{
		if(array.length == 0)
			return null;
		
		return array[MathUtils.random(array.length)];
	}
	
	public static Direction invert(Direction dir){
		switch(dir){
			case N:
				return Direction.S;
			case NE:
				return Direction.SW;
			case E:
				return Direction.W;
			case SE:
				return Direction.NW;
			case S:
				return Direction.N;
			case SW:
				return Direction.NE;
			case W:
				return Direction.E;
			case NW:
				return Direction.SE;
			default:
				throw new RuntimeException();
		}
	}
}
