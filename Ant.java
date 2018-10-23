import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

public class Ant implements Steppable
{
    //variables for the model
    final static double DEPLOY_RANGE = 0.9;
    final static double DEPLOY_CROWD = 0.6;
    final static int DEPLOY_TRIES = 10;
    final static int MIN_WANDER = -200;
    final static double WANDER_FRACTION = 0.7;
    final static double discount = 0.9;
    final static boolean wandering = true;
    boolean foraging = true;
    boolean ferrying = false;
    double reward = 0.0;
    int count = 0;
    Double2D currPos;
    Beacon currBeacon;

    boolean printStatus = false;
    public boolean getPrintStatus () {return printStatus;}
    public void setPrintStatus (boolean newStatus) {printStatus = newStatus;}
    //variables for statistic purposes
    int travelLength = 0;
    //values needed to compute values inside methods that are not returned by
    //boolean functions
    //eventualMerge is needed to remove Beacons and pass a value to remove
    Beacon eventualMerge = null;
    Beacon localBeacon = null;
    int localityCount = 0;
    public int getLocalityCount() {return localityCount;}
    public Beacon getCurrentBeacon() {return currBeacon;}
    double tau = 40;
    //whereToDeploy is needed to pass a value to deploy()
    Double2D whereToDeploy = new Double2D(0,0);

    public Ant(double start_reward)
    {
        reward = start_reward;
    }

    public void step(SimState state)
    {
        travelLength += 1;
        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
        //System.out.print("Step at time "+fwb.schedule.getTime());
        Continuous2D beaconsPos = fwb.beaconsPos;
        Continuous2D antsPos = fwb.antsPos;
        currPos = antsPos.getObjectLocation(this);
        currBeacon = findCurrBeacon(fwb, currPos, fwb.range);
        //neighbors are needed in several functions so they are computed here once
        Bag neighbors = getBeaconsInRange(currPos,fwb);

        // this implementation of finding the food or nest within range allows
        // to compute later their position to move the ant
        Bag foodInRange = fwb.foodPos.getNeighborsExactlyWithinDistance(currPos, fwb.range);
        Bag nestInRange = fwb.nestPos.getNeighborsExactlyWithinDistance(currPos, fwb.range);
        boolean hasBeacon = currBeacon != null;
        boolean hasFoodInRange = foodInRange.size() > 0;
        boolean hasNestInRange = nestInRange.size() > 0;



        //System.out.print("ant "+this+ ": ");
        if (hasBeacon){
            updatePheromones(neighbors);
            //     System.out.println("first pheromone update");
        }
        if (hasFoodInRange && foraging){
            antsPos.setObjectLocation(this, fwb.foodPos.getObjectLocation(foodInRange.get(0)));
            reward = fwb.reward;
            foraging = false;
            ferrying = true;
            if(printStatus) System.out.println("Food Reached.");
        }
        else if (hasNestInRange && ferrying){
            antsPos.setObjectLocation(this, fwb.nestPos.getObjectLocation(nestInRange.get(0)));
            reward = fwb.reward;
            foraging = true;
            ferrying = false;
            Nest nest = (Nest) nestInRange.objs[0];
            nest.meanTravelLength = (double)(nest.meanTravelLength * nest.foodRecovered + travelLength)/ (++nest.foodRecovered); 
            travelLength = 0;
            if(printStatus) System.out.println("nest Reached.");
        }
        else if (hasBeacon && canRemove(fwb, neighbors, hasFoodInRange, hasNestInRange)
                 && fwb.random.nextDouble() <= Math.pow((-localityCount+tau)/tau,0.5)){
            if(printStatus) System.out.println("removed beacon "+currBeacon);
            remove(beaconsPos);
        }
        else if (count > 0 && hasBeacon && (neighbors.size() > 1) ){
            Beacon next;
            while(true){
                next = (Beacon) neighbors.get(fwb.random.nextInt(neighbors.size()));
                if (next != currBeacon) break;
            }
            currPos = next.pos;
            antsPos.setObjectLocation(this, currPos);
            count -= 1;
            if(printStatus) System.out.println("Exploring. "+count+" turns left.");
        }
        else if (fwb.random.nextDouble() < fwb.pExplore){
            count = fwb.countMax;
            if(printStatus) System.out.println("Started exploration");
        }
        else if (hasBeacon && canMove(fwb, hasFoodInRange, hasNestInRange) &&
                 fwb.random.nextDouble() < fwb.pMove){
            move(fwb);
            if(printStatus) System.out.println("Moved beacon "+currBeacon);
        }
        else if (hasBeacon && canFollow(fwb) &&
                 fwb.random.nextDouble() < fwb.pFollow){
            currPos = follow(fwb, beaconsPos, !wandering, fwb.range);
            antsPos.setObjectLocation(this, currPos);
            if(printStatus) System.out.println("followed pheromone");
        }
        //else if (canDeploy(fwb) && fwb.random.nextDouble() < fwb.pDeploy){
        else if (canDeploy(fwb) && fwb.random.nextDouble() < Math.exp(-fwb.beaconsPos.size()/fwb.MAX_BEACON_NUMBER)){
            deploy(fwb);
            if(printStatus) System.out.println("deployed the beacon " + currBeacon);
        }
        else if (hasBeacon){
            currPos = follow(fwb, beaconsPos, wandering, fwb.range);
            antsPos.setObjectLocation(this, currPos);
            if(printStatus) System.out.println("wandering. Num beacons "+fwb.beaconsPos.size()+"/"+fwb.MAX_BEACON_NUMBER);
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
            if(printStatus) System.out.println("beaconless random move");
        }
        currBeacon = findCurrBeacon(fwb, currPos, fwb.range);
        hasBeacon = currBeacon != null;
        if (hasBeacon){
            neighbors = getBeaconsInRange(currPos, fwb);
            updatePheromones(neighbors);
        }
        reward = 0;

    }

    public Beacon findCurrBeacon(ForagingWithBeacons state, Double2D currPos, double range)
    {
        Bag candidates = getBeaconsInRange(currPos, state);
        if (candidates.size() == 0) return null;
        Beacon closest = (Beacon) candidates.objs[0];
        double distance = closest.pos.distance(currPos);
        int breakties = 1;
        for (int el = 1; el < candidates.size(); el++){
            Beacon b = (Beacon) candidates.get(el);
            double newDistance = b.pos.distance(currPos);
            if ( newDistance > distance) continue;
            if (newDistance == distance )
                if (state.random.nextDouble() > 1. / (++breakties)) continue;
            if (distance != newDistance) breakties = 1;
            closest = b;
            distance = newDistance;
        }
        return closest;
    }

    public void updatePheromones(Bag neighbors)
    {
        double maxForaging = currBeacon.foragingPheromone;
        double maxFerrying = currBeacon.ferryingPheromone;
        int len = neighbors.size();
        double rewardForaging;
        double rewardFerrying;
        if (ferrying){
            rewardForaging = reward;
            rewardFerrying = 0;
        }
        else{
            rewardForaging = 0;
            rewardFerrying = reward;
        }
        for (int i = 0; i < len; i++){
            Beacon otherB = (Beacon)neighbors.get(i);
            if (otherB == currBeacon) continue;
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
        currBeacon.ferryingPheromone = maxFerrying;
        currBeacon.foragingPheromone = maxForaging;
        currBeacon.wanderingPheromone -= 1;
    }


    public boolean canFollow(ForagingWithBeacons fwb)
    {
        Bag candidates = getBeaconsInRange(currBeacon.pos, fwb);
        int len = candidates.size();
        double max = -1;
        for (int i = 0; i < len; i++){
            Beacon cand = (Beacon) candidates.get(i);
            if (cand == currBeacon ) continue;
            if (foraging){
                double val = cand.foragingPheromone;
                if (val > max) max = val;

            }
            else {
                double val = cand.ferryingPheromone;
                if (val > max) max = val;
            }
        }

        //MODIFIED: returns true if there is a beacon with higher pheromone than currBeacon
        if (foraging && max > currBeacon.foragingPheromone ||
            ferrying && max > currBeacon.ferryingPheromone){
            return true;
        }
        else
            return false;
        //if (max != -1) return true;
        //return false;
    }

    public Double2D follow(ForagingWithBeacons fwb, Continuous2D beaconsPos, boolean wandering, double range)
    {
        Bag candidates = getBeaconsInRange(currBeacon.pos, fwb);
        //max is unreasonably high to be discarded even if wandering as negative numbers are used
        double max = -1e40;
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
            else if (foraging){
                val = cand.foragingPheromone;
            }
            else if (ferrying){
                val = cand.ferryingPheromone;
            }
            if (val < max) continue;
            if ( val == max )
                if (fwb.random.nextDouble() > (1.0 / ++tieBreak)) continue;
            if (max != val) tieBreak = 1;
            max = val;
            nextPos = cand.pos;
        }
        return nextPos;
    }

    public boolean canDeploy(ForagingWithBeacons state)
    {
        Continuous2D beaconsPos = state.beaconsPos;
        /* CHANGED BY USING PDEPLOY(number of beacons)
        if (beaconsPos.size() >= state.MAX_BEACON_NUMBER) {
            //System.out.println("Max beacons reached.");
            return false;
        }
        */
        for (int i = 0; i < DEPLOY_TRIES; i++){
            while (true){
                //PROBLEM TO DISCUSS: this leads to deploy a beacon out of reach.
                double correctRange = state.range;
                if (currBeacon != null){
                    whereToDeploy = currBeacon.pos;
                    //This is added to propagate pheromones, if state.range is used
                    //the current beacon might get out of range once the ants move.
                    correctRange = currBeacon.range;
                }
                else {
                    whereToDeploy = currPos;
                }
                //whereToDeploy = whereToDeploy.add(currPos);
                double r = (state.random.nextDouble() *
                            (DEPLOY_RANGE - DEPLOY_CROWD) + DEPLOY_CROWD)
                           * correctRange;
                double theta = state.random.nextDouble() * Math.PI *2;
                double x = r * Math.cos(theta);
                double y = r * Math.sin(theta);
                whereToDeploy = whereToDeploy.add(new Double2D(x,y));


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
            //Sloppy density definition required to be able to deploy beacons in correctRange
            for ( int j = 0; j < closestBeacon.size(); j++){
                Beacon other = (Beacon) closestBeacon.objs[j];
                if (other.pos.distance(whereToDeploy) < DEPLOY_CROWD * other.range){
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
        Beacon b = new Beacon(whereToDeploy, state.range);
        state.beaconsPos.setObjectLocation(b,whereToDeploy);
        state.antsPos.setObjectLocation(this, whereToDeploy);
        b.stopper = state.schedule.scheduleRepeating(state.schedule.getTime()+0.5,b);
        currBeacon = b;
        currPos = whereToDeploy;
    }

    public boolean canMove(ForagingWithBeacons state, boolean closeToFood, boolean closeToNest)
    {
        Bag neighbors = getBeaconsInRange(currBeacon.pos, state);
        Double2D ferryPos = currPos;
        Double2D foragePos = currPos;
        Double ferrymax = -1.0;
        Double foragemax = -1.0;
        int tiebreak1 = 1;
        int tiebreak2 = 1;
        int wander1 = 0;
        int wander2 = 0;
        for (int i = 0; i < neighbors.numObjs; i++){
            Beacon obj = (Beacon) neighbors.objs[i];
            if (obj.foragingPheromone > foragemax) {
                foragemax = obj.foragingPheromone;
                foragePos = obj.pos;
                tiebreak1 = 1;
                wander1 = obj.wanderingPheromone;
            }
            else if (obj.foragingPheromone == foragemax) {
                if (state.random.nextDouble() < 1.0 / (++tiebreak1)){
                    foragePos = obj.pos;
                    wander1 = obj.wanderingPheromone;
                }
            }
            if (obj.ferryingPheromone > ferrymax) {
                ferrymax = obj.ferryingPheromone;
                ferryPos = obj.pos;
                wander2 = obj.wanderingPheromone;
                tiebreak2 = 1;
            }
            else if (obj.ferryingPheromone == ferrymax) {
                if (state.random.nextDouble() < 1.0 / (++tiebreak2)){
                    ferryPos = obj.pos;
                    wander2 = obj.wanderingPheromone;
                }
            }
        }

        int W = Math.min(wander1,wander2);
        if (closeToFood){
            foragePos = state.foodPos.getObjectLocation(state.foodPos.getNeighborsExactlyWithinDistance(currPos,state.range).get(0));
            foragemax = state.reward;
        }
        if (closeToNest){
            ferryPos = state.nestPos.getObjectLocation(state.nestPos.getNeighborsExactlyWithinDistance(currPos,state.range).get(0));
            ferrymax = state.reward;
        }
        if (ferryPos == currPos || foragePos == currPos || ferryPos == foragePos ||
            ferrymax == 0 || foragemax == 0) return false;
        whereToDeploy = ferryPos.add(foragePos).multiply(0.5);

        //checking the second condition in the article
        if (currBeacon.wanderingPheromone >= MIN_WANDER) return true;
        for(int i = 0 ; i < neighbors.numObjs ; i++){
            Beacon b = (Beacon) neighbors.objs[i];
            if (b == currBeacon) continue;
            if (b.wanderingPheromone < WANDER_FRACTION * W){
                if (b.pos.distance(whereToDeploy) > state.range) return false;
            }
        }
        //Without obstacles the checks are finished.
        return true;
    }
    public void move(ForagingWithBeacons state)
    {
        state.beaconsPos.setObjectLocation(currBeacon, whereToDeploy);
        currBeacon.pos = whereToDeploy;
        state.antsPos.setObjectLocation(this,whereToDeploy);
        currPos = whereToDeploy;
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
                return true;
            }
        for (int i = 0; i< neighbors.numObjs; i++){
            if (neighbors.objs[i] == currBeacon) continue;
            Beacon other = (Beacon) neighbors.objs[i];
            if (foodWithinRange == true &&
                state.foodPos.getNeighborsExactlyWithinDistance(other.pos,state.range).size() == 0 ) continue;
            if (nestWithinRange == true &&
                state.nestPos.getNeighborsExactlyWithinDistance(other.pos,state.range).size() == 0 ) continue;
            if (currBeacon.ferryingPheromone > other.ferryingPheromone) continue;
            if (currBeacon.foragingPheromone > other.foragingPheromone) continue;
            boolean goodBeacon = true;
            for (int j = 0; j< neighbors.numObjs;j++){
                if (i==j) continue;
                if (((Beacon)(neighbors.objs[j])).pos.distance(other.pos)>= ((Beacon)neighbors.objs[j]).range) {
                    goodBeacon = false;
                    break;
                }
            }
            if (goodBeacon){
                eventualMerge = other;
                if (localBeacon == eventualMerge) localityCount++;
                else localityCount = 0;
                localBeacon = eventualMerge;
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

    public Bag getBeaconsInRange(Double2D pos, ForagingWithBeacons state)
    {
        Bag candidates = state.beaconsPos.getNeighborsExactlyWithinDistance(pos, state.range);
        for (int i = 0 ; i < candidates.size(); i++){
            Beacon el = (Beacon)candidates.get(i);
            if ( el.pos.distance(pos) > el.range){
                candidates.remove(i);
                --i;
            }
        }

        return candidates;
    }
}
