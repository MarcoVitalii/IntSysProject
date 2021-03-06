import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import java.awt.*;
import sim.util.*;
import sim.util.distribution.Normal;
public class Beacon extends OvalPortrayal2D implements Steppable
{
    public double foragingPheromone = 0.0;
    public double ferryingPheromone = 0.0;
    public int wanderingPheromone = 0;
    public Stoppable stopper = null;
    public Double2D pos = new Double2D(0.0,0.0);
    public double range;

    Beacon(Double2D newPos, Double newRange)
    {
        pos = newPos;
        range = newRange;
    }
    //public int neighborsCount = 0;

    //getters are needed to inspect the elements inside the visualization
    public double getForagingPheromone() {return foragingPheromone;}
    public double getFerryingPheromone() {return ferryingPheromone;}
    public int getWanderingPheromone() {return wanderingPheromone;}
    public double getRange() {return range;}
    //public int getNeighborsCount() {return neighborsCount;}
    //public void setFerryingPheromone(double ferry) {if(ferry >0) ferryingPheromone = ferry;}
    //public void setForagingPheromone(double forag) { if (forag >0) foragingPheromone = 0.0;}

    public void step(SimState state)
    {
        //beta is not inside Beacon class to allow changes from the console
        //during the simulation.
        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
        //System.out.println("Step at time "+fwb.schedule.getTime()+" "+this);
        double beta = fwb.evaporationConstant;
        foragingPheromone *= beta;
        ferryingPheromone *= beta;
        range *= fwb.beaconShrinkingFactor;
        if (range < fwb.minRange){
            stopper.stop();
            fwb.beaconsPos.remove(this);
        }
        /* feature excluded from current studies
        else{
            Double2D newPos;
            while (true){
                newPos  =  pos.add(new Double2D(fwb.random.nextGaussian() * fwb.beaconSigma, fwb.random.nextGaussian() * fwb.beaconSigma));
                if (newPos.x >= 0 && newPos.x < fwb.WORLD_SIZE &&
                    newPos.y >= 0 && newPos.y < fwb.WORLD_SIZE)
                    break;
            }
            pos = newPos;
            fwb.beaconsPos.setObjectLocation(this, pos);
        }
        */
    }

    /* Nicer option than what's actually happening with anonymus classes inside
       ForagingWithBeaconsUI.java but the outer circle used as a wrapper didn't
       accepted this extention of OvalPortrayal2D.

    public final void draw( Object object, Graphics2D graphics,
                            DrawInfo2D info)
    {
        int scale = Math.min((int)(foragingPheromone/2*255),255);
        paint = (new Color(20,scale,20));
        //System.out.println("cambiocolore"+(int)(foragingPheromone/2*255));
        super.draw(object,graphics,info);
    }
    */
}
