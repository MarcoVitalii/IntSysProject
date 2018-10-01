import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

public class Ant implements Steppable
{
    //Constants needed to deploy beacons:
    final static double DEPLOY_RANGE = 0.9;
    final static double DEPLOY_CROWD = 0.6;
    final static int DEPLOY_TRIES = 10;
    final static int MIN_WANDER = -200;
    //modes are defined as 0:foraging 1:ferrying
    int mode = 0;
    double reward = 0.0;
    int count = 0;
    Double2D currPos;
    Beacon currBeacon;
    double discount = 0.9;

    //values needed to compute values inside methods that are not returned by
    //boolean functions
    //eventualMerge is needed to remove Beacons and pass a value to remove
    Beacon eventualMerge = null;
    //whereToDeploy is needed to pass a value to deploy()
    Double2D whereToDeploy = new Double2D(0,0);

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
        currBeacon = findCurrBeacon(fwb, currPos, fwb.range);
        //neighbors are needed in several functions so they are computed here once
        Bag neighbors = beaconsPos.getNeighborsExactlyWithinDistance(currPos,
                                                                      fwb.range);
        // System.out.println("inizio step: posizione e beacon");
        //System.out.print(currPos);
        //System.out.print(beaconsPos.getObjectLocation(currBeacon));

        if (currBeacon != null){
            updatePheromones(neighbors, currBeacon);
            //     System.out.println("first pheromone update");
        }
        // this implementation of finding the food or nest within range allows
        // to compute later their position to move the ant
        Bag food = fwb.foodPos.getNeighborsExactlyWithinDistance(currPos, fwb.range);
        Bag nest = fwb.nestPos.getNeighborsExactlyWithinDistance(currPos, fwb.range);
        if (food.size() != 0 && mode == 0){
            antsPos.setObjectLocation(this, fwb.foodPos.getObjectLocation(food.get(0)));
            reward = fwb.reward;
            mode = 1;
            // System.out.println("ho trovato il cibo!");
        }
        else if (nest.size() != 0 && mode == 1){
            antsPos.setObjectLocation(this, fwb.nestPos.getObjectLocation(nest.get(0)));
            mode = 0;
            reward = fwb.reward;
            //System.out.println("ho trovato la tana");
        }
        else if (currBeacon != null && canRemove(fwb,neighbors,
                                                 (food.numObjs >= 1),
                                                 (nest.numObjs >=1))){
            // System.out.println("ucciso un beacon");
            remove(beaconsPos);
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
        else if (currBeacon != null &&
                 canMove() && //TODO FINISH TO WRITE THIS IMPLEMENTATION
                 fwb.random.nextDouble() < fwb.pMove){
            move();
        }
        else if (currBeacon != null && canFollow(beaconsPos, fwb.range) &&
                 fwb.random.nextDouble() < fwb.pFollow){
            currPos = follow(fwb, beaconsPos, false, fwb.range);
            antsPos.setObjectLocation(this, currPos);
            // System.out.print("following curr pheromone\n");
        }
        else if (canDeploy(fwb) &&
                 fwb.random.nextDouble() < fwb.pDeploy){
            deploy(fwb);
            // System.out.println("deployed a new beacon");
        }
        else if (currBeacon != null){
            currPos = follow(fwb, beaconsPos, true, fwb.range);
            antsPos.setObjectLocation(this, currPos);
            //System.out.println("wandering");
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
            // System.out.println("beaconless random move");
        }
        currBeacon = findCurrBeacon(fwb, currPos, fwb.range);
        if (currBeacon != null){
            neighbors = beaconsPos.getNeighborsExactlyWithinDistance(currPos,
                                                                      fwb.range);
            updatePheromones(neighbors, currBeacon);
        }
        reward = 0;

    }

    public Beacon findCurrBeacon(ForagingWithBeacons state, Double2D currPos, double range)
    {
        Bag candidates = state.beaconsPos.getNeighborsExactlyWithinDistance(currPos,range);
        if (candidates.size() == 0) return null;
        Beacon closest = (Beacon) candidates.objs[0];
        double distance = state.beaconsPos.getObjectLocation(closest).distance(currPos);
        int breakties = 1;
        for (int el = 1; el < candidates.size(); el++){
            Beacon b = (Beacon) candidates.get(el);
            double newDistance = state.beaconsPos.getObjectLocation(b).distance(currPos);
            if ( newDistance > distance) continue;
            if (newDistance == distance )
                if (state.random.nextDouble() > 1. / (++breakties)) continue;
            if (distance != newDistance) breakties = 1;
            closest = b;
            distance = newDistance;
        }
        return closest;
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
        if (len == 1){ //there's only himself. this condition adds the rewards
            //self interaction needed to start as the first beacon needs it.
            //Has to be discussed with the professor.
            maxFerrying +=rewardFerrying;
            maxForaging +=rewardForaging;
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
            else {
                double val = cand.ferryingPheromone;
                if (val > max) max = val;
            }
        }
        if (max != -1){
            return true;
        }
        else
            return false;
    }

    public Double2D follow(ForagingWithBeacons fwb, Continuous2D beaconsPos, boolean wandering, double range)
    {
        Bag candidates = beaconsPos.getNeighborsExactlyWithinDistance(currPos, range);
        //max is unreasonably high to be discarded even if wandering as negative numbers are used
        double max = - 100000;
        int tieBreak = 1;
        int len = candidates.size();
        Beacon cand = null;
        Double2D nextPos = currPos;
        for (int index = 0; index < len; index++){
            double val = 0;
            cand = (Beacon) candidates.get(index);
            if (cand == currBeacon) continue;
            if (wandering) {
                val = cand.wanderingPheromone;
            }
            else if (mode == 0){
                val = cand.foragingPheromone;
            }
            else if (mode == 1){
                val = cand.ferryingPheromone;
            }
            if (val < max) continue;
            if ( val == max )
                if (fwb.random.nextDouble() > (1.0 / ++tieBreak)) continue;
            if (max != val) tieBreak = 1;
            max = val;
            nextPos = beaconsPos.getObjectLocation(cand);
        }
        return nextPos;
    }

    public boolean canDeploy(ForagingWithBeacons state)
    {
        Continuous2D beaconsPos = state.beaconsPos;
        if (beaconsPos.size() > state.MAX_BEACON_NUMBER) {
            System.out.println("Max beacons reached.");
            return false;
        }

        for (int i = 0; i < DEPLOY_TRIES; i++){
            while (true){
                double r = (state.random.nextDouble() *
                            (DEPLOY_RANGE - DEPLOY_CROWD) + DEPLOY_CROWD)
                           * state.range;
                double theta = state.random.nextDouble() * Math.PI *2;
                double x = r * Math.cos(theta);
                double y = r * Math.sin(theta);
                this.whereToDeploy = new Double2D(x,y);

                //PROBLEM TO DISCUSS: this leads to deploy a beacon out of reach.
                //if (currBeacon != null){
                //    whereToDeploy = whereToDeploy.add(beaconsPos.getObjectLocation(currBeacon));
                //}
                //else {
                //    whereToDeploy = whereToDeploy.add(currPos);
                //}
                whereToDeploy = whereToDeploy.add(currPos);

                if ( whereToDeploy.x >= 0 &&
                     whereToDeploy.x <= state.WORLD_SIZE &&
                     whereToDeploy.y >= 0 &&
                     whereToDeploy.y <= state.WORLD_SIZE ){
                    break;
                }
            }
            if (beaconsPos.size() ==0) return true;
            Bag closestBeacon = beaconsPos.getNeighborsExactlyWithinDistance(whereToDeploy, state.range);
            boolean goodSpot = true;
            for ( int j = 0; j < closestBeacon.size(); j++){
                if (beaconsPos.getObjectLocation(closestBeacon.objs[j]).distance(whereToDeploy) < DEPLOY_CROWD * state.range){
                    goodSpot = false;
                    break;
                }
            }
            if (goodSpot) return true;
        }
        return false;
    }

    public void deploy(ForagingWithBeacons state)
    {
        Beacon b = new Beacon();
        //System.out.println("where: "+whereToDeploy);
        state.beaconsPos.setObjectLocation(b,whereToDeploy);
        state.antsPos.setObjectLocation(this, whereToDeploy);
        b.stopper = state.schedule.scheduleRepeating(b,1);
        currBeacon = b;
        currPos = whereToDeploy;
    }

    public boolean canMove()
    {
        return true;
    }
    public void move()
    {

    }
    public boolean canRemove (ForagingWithBeacons state,Bag neighbors,
                              boolean foodWithinRange, boolean nestWithinRange)
    {
        eventualMerge = null;
        //neighbor >3 because the current beacon is contained in neighbors
        if (neighbors.size() > 3 &&
            foodWithinRange == false &&
            nestWithinRange == false &&
            currBeacon.wanderingPheromone <= MIN_WANDER)
            {
                System.out.println("killed by neighborhood");
                return true;
            }
        // System.out.println("butcher at "+currPos+" with beaocon "+ state.beaconsPos.getObjectLocation(currBeacon));
        for (int i = 0; i< neighbors.numObjs; i++){
            if (neighbors.objs[i] == currBeacon) continue;
            Double2D pos = state.beaconsPos.getObjectLocation(neighbors.objs[i]);
            Beacon other = (Beacon) neighbors.objs[i];
            // System.out.println("Candidate for merger at "+pos);
            if (foodWithinRange == true &&
                state.foodPos.getNeighborsExactlyWithinDistance(pos,state.range).size() == 0 ) continue;
            if (nestWithinRange == true &&
                state.nestPos.getNeighborsExactlyWithinDistance(pos,state.range).size() == 0 ) continue;
            if (currBeacon.ferryingPheromone > other.ferryingPheromone) continue;
            if (currBeacon.foragingPheromone > other.foragingPheromone) continue;
            boolean goodBeacon = true;
            for (int j = 0; j< neighbors.numObjs;j++){
                if (i==j) continue;
                if (state.beaconsPos.getObjectLocation(neighbors.objs[j]).distance(pos)>= state.range) {
                    goodBeacon = false;
                    break;
                }
            }
            if (goodBeacon){
                eventualMerge = other;
                return true;
            }
        }
        return false;
    }
    public void remove (Continuous2D beaconsPos)
    {
        if (eventualMerge != null){
            eventualMerge.wanderingPheromone = Math.min(currBeacon.wanderingPheromone,
                                                        eventualMerge.wanderingPheromone);

        }
        beaconsPos.remove(currBeacon);
        currBeacon.stopper.stop();
        currBeacon = null;
    }
}
