import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

public class Food implements Steppable
{
	public void step(SimState state) 
	{
	    ForagingWithBeacons fwb = (ForagingWithBeacons) state;
	    Double2D newPos;
	    Double2D pos = fwb.foodPos.getObjectLocation(this);
	    while (true){
		newPos = pos.add(new Double2D(fwb.random.nextGaussian()*fwb.foodSigma,
					      fwb.random.nextGaussian() * fwb.foodSigma));
		if (newPos.x >= 0 && newPos.x < fwb.WORLD_SIZE &&
		    newPos.y >= 0 && newPos.y < fwb.WORLD_SIZE) break;
	    }
	    fwb.foodPos.setObjectLocation(this, newPos);
	}
}
