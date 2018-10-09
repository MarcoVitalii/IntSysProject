import sim.engine.*;
import sim.field.continuous.*; 
import sim.util.*;

public class Nest implements Steppable
{
    //it will have something inside at some point
    public int foodRecovered;
    public int prevFoodRecovered;
    public double meanFoodRecovered;
    public double foodIncomingRate;
    public double meanTravelLength;
    public int getFoodRecovered() {return foodRecovered;}
    public double getMeanFoodRecovered () {return meanFoodRecovered;}
    public double getfoodIncomingRate () {return foodIncomingRate;}
    public double getMeanTravelLength () {return meanTravelLength;}
    

    public Nest()
    {
        foodRecovered = 0;
        prevFoodRecovered = 0;
        meanFoodRecovered = 0;
        foodIncomingRate = 0;
        meanTravelLength = 0;
    }
    public void step(SimState state)
    {
        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
        foodIncomingRate = (double) (foodRecovered - prevFoodRecovered) / fwb.MEAN_TIME;
        meanFoodRecovered = (double)(foodRecovered) / fwb.schedule.getTime();
        prevFoodRecovered = foodRecovered;
    }
}
