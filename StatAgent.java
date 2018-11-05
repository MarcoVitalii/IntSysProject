import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

class StatAgent implements Steppable {

    public double[] actionsTaken = new double[10];
    static final int MEAN_TIME = 100;
    public int foodRecovered;
    public int prevFoodRecovered;
    public double foodIncomingRate;
    public double meanTravelLength;
    public double skewedAvgLength;
    double normFactor;

    //getters shouldn't be accessible without a proper inspector, not implemented yet.
    public int getFoodRecovered() {return foodRecovered;}
    public double getfoodIncomingRate () {return foodIncomingRate;}
    public double getMeanTravelLength () {return meanTravelLength;}
    public double getSkewedAvgLength () {return skewedAvgLength;}

    public StatAgent (ForagingWithBeacons state)
    {
        foodRecovered = 0;
        prevFoodRecovered = 0;
        foodIncomingRate = 0;
        meanTravelLength = 0;
        normFactor = MEAN_TIME * state.antsNumber;
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("data/settings.txt"))){
            bw.write("ants number: "+ state.antsNumber
                     +"\nrange: "+state.range
                     +"\nreward: "+ state.reward
                     +"\ncountMax: " + state.countMax
                     +"\npExplore: " + state.pExplore
                     +"\npFollow: " + state.pFollow
                     +"\npMove: " + state.pMove
                     +"\nevaporationConstant: " + state.evaporationConstant
                     +"\nmaxBeaconNumber: "+ state.maxBeaconNumber
                     +"\ntau: " + state.tau
                     +"\nbeaconTimeScale: " + state.beaconTimeScale
                     +"\nWORLD_SIZE: " + state.WORLD_SIZE
                     +"\nX_NEST: " + state.X_NEST
                     +"\nY_NEST: " + state.Y_NEST
                     +"\nX_FOOD: " + state.X_FOOD
                     +"\nY_FOOD: " + state.Y_FOOD
                     +"\nMEAN_TIME: " + MEAN_TIME
                     );
        }
        catch(IOException e){}
    }
    public void step (SimState state)
    {
        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
        foodIncomingRate = (double) (foodRecovered - prevFoodRecovered) / MEAN_TIME;
        prevFoodRecovered = foodRecovered;
        Nest nest =(Nest)(fwb.nestPos.getAllObjects().get(0));
        double meanFoodPerAnt = 0;
        double meanSqFoodPerAnt = 0;
        Bag ants = fwb.antsPos.getAllObjects();
        for (int i = 0; i < ants.size(); i++){
            Ant ant = (Ant) ants.get(i);
            meanFoodPerAnt += ant.foodRecovered;
            meanSqFoodPerAnt += Math.pow(ant.foodRecovered, 2);
        }
        meanFoodPerAnt /= ants.size();
        meanSqFoodPerAnt /= ants.size();
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("data/"+state.seed()+".csv",true))){
            bw.write(fwb.beaconTimeScale + "," +
                     fwb.minRange + "," +
                     fwb.schedule.time() + "," +
                     foodIncomingRate + "," +
                     foodRecovered + "," +
                     fwb.beaconsPos.size() + "," +
                     skewedAvgLength + "," +
                     (actionsTaken[0] / normFactor)  + "," +
                     (actionsTaken[1] / normFactor)  + "," +
                     (actionsTaken[2] / normFactor)  + "," +
                     (actionsTaken[3] / normFactor)  + "," +
                     (actionsTaken[4] / normFactor)  + "," +
                     (actionsTaken[5] / normFactor)  + "," +
                     (actionsTaken[6] / normFactor)  + "," +
                     (actionsTaken[7] / normFactor)  + "," +
                     (actionsTaken[8] / normFactor)  + "," +
                     (actionsTaken[9] / normFactor)  + "," +
                     meanFoodPerAnt + "," +
                     meanSqFoodPerAnt + "," +
                     fwb.seed() + "\n");
        }
        catch(IOException e){}
        for (int i = 0; i < 10; i++){
            actionsTaken[i] = 0;
        }
    }

    public void updateStatsFromNest (double Length)
    {

        meanTravelLength = (double)(meanTravelLength * foodRecovered + Length)/
            (++foodRecovered);
        skewedAvgLength = skewedAvgLength + 0.01 * (Length - skewedAvgLength);
    }
}
