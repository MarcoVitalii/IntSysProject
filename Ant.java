import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

public class Ant implements Steppable
{
    //modes are defined as 0:foraging 1:ferrying
    int mode = 0;
    double reward = 0.0;
    int count = 0;
    Double2D currPos;
    Beacon currBeacon;
    double discount = 0.9;

    public Ant(double start_reward)
    {
        reward = start_reward;
    }

    public void step(SimState state)
    {
        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
        Continuous2D beaconsPos = fwb.beaconsPos;
        Continuous2D antsPos = fwb.antsPos;
        currPos = antsPos.getObjectLocation(this);
        currBeacon = findCurrBeacon(beaconsPos, currPos, fwb.range);
        //neighbors are needed in several functions so they are computed here once
        Bag neighbors = beaconsPos.getNeighborsExactlyWithinDistance(currPos,
                                                                      fwb.range);
        // System.out.println("inizio step: posizione e beacon");
        //System.out.print(currPos);
        //System.out.print(beaconsPos.getObjectLocation(currBeacon));

        if (currBeacon != null){
            updatePheromones(neighbors, currBeacon);
        }
        // this implementation of finding the food or nest within range allows
        // to compute later their position to move the ant
        Bag food = fwb.foodPos.getNeighborsExactlyWithinDistance(currPos, fwb.range);
        Bag nest = fwb.nestPos.getNeighborsExactlyWithinDistance(currPos, fwb.range);
        if (food.size() != 0 && mode == 0){
            antsPos.setObjectLocation(this, fwb.foodPos.getObjectLocation(food.get(0)));
            reward = fwb.reward;
            mode = 1;
            //System.out.println("ho trovato il cibo!");
        }
        else if (nest.size() != 0 && mode == 1){
            antsPos.setObjectLocation(this, fwb.nestPos.getObjectLocation(nest.get(0)));
            mode = 0;
            reward = fwb.reward;
            //System.out.println("ho trovato la tana");
        }
        else if (count > 0 && currBeacon != null && neighbors.size() > 1 ){
            Beacon next;
            while(true){
                next = (Beacon) neighbors.get(fwb.random.nextInt(neighbors.size()));
                if ( next != currBeacon) break;
            }
            currPos = beaconsPos.getObjectLocation(next);
            antsPos.setObjectLocation(this,currPos);
            count -= 1;
            //System.out.println("RandomMove. left "+count );
        }
        else if (fwb.random.nextDouble() < fwb.pExplore){
            count = fwb.countMax;
            //System.out.println("reset count");
        }
        else if (currBeacon != null && canFollow(beaconsPos, fwb.range) &&
                 fwb.random.nextDouble() < fwb.pFollow){
            currPos = follow(fwb, beaconsPos, false, fwb.range);
            antsPos.setObjectLocation(this, currPos);
            //System.out.print("following curr pheromone\n");
        }
        else if (currBeacon != null){
            currPos = follow(fwb, beaconsPos, true, fwb.range);
            antsPos.setObjectLocation(this, currPos);
            //System.out.println("wandering");
        }
        currBeacon = findCurrBeacon(beaconsPos, currPos, fwb.range);
        if (currBeacon != null){
            neighbors = beaconsPos.getNeighborsExactlyWithinDistance(currPos,
                                                                      fwb.range);
            updatePheromones(neighbors, currBeacon);
        }
        reward = 0;
        //System.out.println("fine step");

    }

    public Beacon findCurrBeacon(Continuous2D beaconsPos, Double2D currPos, double range)
    {
        Bag candidates = beaconsPos.getNearestNeighbors(currPos,
                                                       1,
                                                       false,
                                                       false,
                                                       true,
                                                       null);
        for (int el = 0; el < candidates.size(); el++){
            Beacon b = (Beacon) candidates.get(el);
            if (beaconsPos.getObjectLocation(b).distance(currPos) <= range){
                return b;
            }
        }
        //No good candidates ( or the bag is empty )
        return null;
    }
    public void updatePheromones(Bag neighbors, Beacon b)
    {
        double maxForaging = b.foragingPheromone;
        double maxFerrying = b.ferryingPheromone;
        int len = neighbors.size();
        double rewardForaging;
        double rewardFerrying;
        if (mode == 1){
            rewardForaging = reward;
            rewardFerrying = 0;
        }
        else{
            rewardForaging = 0;
            rewardFerrying = reward;
        }
        for (int i = 0; i < len; i++){
            Beacon otherB = (Beacon)neighbors.get(i);
            if (otherB == b) continue;
            double valueForaging = otherB.foragingPheromone * discount +
                rewardForaging;
            maxForaging = Math.max(maxForaging,valueForaging);
            double valueFerrying = otherB.ferryingPheromone * discount +
                rewardFerrying;
            maxFerrying = Math.max(maxFerrying,valueFerrying);
        }
        // System.out.println("aggiornamento beacon:");
        //System.out.print(maxFerrying);
        //System.out.print(maxForaging);
        //System.out.print(b.wanderingPheromone);
        //System.out.println("");
        b.ferryingPheromone = maxFerrying;
        b.foragingPheromone = maxForaging;
        b.wanderingPheromone -= 1;
    }
    public boolean canFollow(Continuous2D beaconsPos, double range)
    {
        Bag candidates = beaconsPos.getNeighborsExactlyWithinDistance(currPos, range );
        int len = candidates.size();
        double max = -1;
        for (int i = 0; i < len; i++){
            Beacon cand = (Beacon) candidates.get(i);
            if (cand == currBeacon ) continue;
            if (mode == 0){
                double val = cand.foragingPheromone;
                if (val > max) max = val;

            }
            else{
                double val = cand.ferryingPheromone;
                if (val > max) max = val;
            }
        }
        if (max != -1)
            return true;
        else
            return false;
    }

    public Double2D follow(ForagingWithBeacons fwb, Continuous2D beaconsPos, boolean wandering, double range)
    {
        Bag candidates = beaconsPos.getNeighborsExactlyWithinDistance(currPos, range);
        double max = - 10000;
        int tieBreak = 1;
        int index = 0;
        int len = candidates.size();
        Beacon cand = null;
        while(index < len ){
            cand = (Beacon) candidates.get(index);
            if (cand != currBeacon) break;
            index += 1;
        }
        Double2D nextPos = beaconsPos.getObjectLocation(cand);
        if (wandering) {
            max = cand.wanderingPheromone;
        }
        else if (mode == 0){
            max = cand.foragingPheromone;
        }
        else if (mode == 1){
            max = cand.ferryingPheromone;
        }
        for (; index < len; index++){
            double val = 0;
            cand = (Beacon) candidates.get(index);
            if (wandering) {
                val = cand.wanderingPheromone;
            }
            else if (mode == 0){
                val = cand.foragingPheromone;
            }
            else if (mode == 1){
                val = cand.ferryingPheromone;
            }
            if ((val > max) ||
                ( val == max && (fwb.random.nextDouble() > (1.0 / ++tieBreak)))){
                max = val;
                nextPos = beaconsPos.getObjectLocation(cand);
            }
        }
        return nextPos;
    }
}
