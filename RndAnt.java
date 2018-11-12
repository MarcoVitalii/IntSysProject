import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

public class RndAnt extends Ant
{
    public RndAnt () {
        //dummy ctor as this information isn't needed.
        //this class extends Ant to be compatible with
        //statAgent
        super(1);
    }

    public void step(SimState state)
    {
        travelLength+=1;
        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
        Continuous2D antsPos = fwb.antsPos;
        currPos = antsPos.getObjectLocation(this);
        Bag foodInRange = fwb.foodPos.getNeighborsExactlyWithinDistance(currPos, fwb.range);
        Bag nestInRange = fwb.nestPos.getNeighborsExactlyWithinDistance(currPos, fwb.range);
        boolean hasFoodInRange = foodInRange.size() > 0;
        boolean hasNestInRange = nestInRange.size() > 0;

        if (hasFoodInRange && foraging){
            antsPos.setObjectLocation(this, fwb.foodPos.getObjectLocation(foodInRange.get(0)));
            foraging = false;
            ferrying = true;
            actionTaken = 0;
        }
        else if (hasNestInRange && ferrying){
            antsPos.setObjectLocation(this, fwb.nestPos.getObjectLocation(nestInRange.get(0)));
            foraging = true;
            ferrying = false;
            fwb.stats.updateStatsFromNest(travelLength);
            travelLength = 0;
            ++foodRecovered;
            actionTaken = 1;
        }
        else{
            Double2D rndMove;
            while(true){
                double theta = fwb.random.nextDouble() * 2 * Math.PI;
                rndMove = currPos.add(new Double2D(fwb.range*Math.cos(theta),fwb.range*Math.sin(theta)));
                if ( rndMove.x >= 0 &&
                     rndMove.x <= fwb.WORLD_SIZE &&
                     rndMove.y >= 0 &&
                     rndMove.y <= fwb.WORLD_SIZE ){
                    break;
                }
            }
            antsPos.setObjectLocation(this,rndMove);
            currPos = rndMove;
            actionTaken = 9;
        }
        fwb.stats.actionsTaken[actionTaken] += 1;
    }
}
