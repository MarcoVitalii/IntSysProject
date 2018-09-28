import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;

public class Beacon implements Steppable
{
    public double foragingPheromone = 0.0;
    public double ferryingPheromone = 0.0;
    public double wanderingPheromone = 0.0;

    public void step(SimState state)
    {
        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
        double beta = state.evaporationConstant;
        foragingPheromone *= beta;
        ferryingPheromone *= beta;
    }
}
