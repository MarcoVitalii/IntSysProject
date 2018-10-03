import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

public class ForagingWithBeacons extends SimState
{
    public static final int MAX_BEACON_NUMBER = 70;
    public static final double WORLD_SIZE = 100;
    public static final double X_NEST = 50.0;
    public static final double Y_NEST = 50.0;
    public Continuous2D antsPos = new Continuous2D(1.0, WORLD_SIZE, WORLD_SIZE);
    public Continuous2D beaconsPos = new Continuous2D(1.0, WORLD_SIZE, WORLD_SIZE);
    public Continuous2D foodPos = new Continuous2D(1.0, WORLD_SIZE, WORLD_SIZE);
    public Continuous2D nestPos = new Continuous2D(1.0, WORLD_SIZE, WORLD_SIZE);
    double range = 10.0;
    int antsNumber = 10;
    double reward = 1.0;
    double pExplore = 0.005;
    double pFollow = 0.90;
    double pDeploy = 0.9;
    double pMove = 0.3;
    int countMax = 10;
    public double evaporationConstant = 0.95;
    public final boolean fixedBeacons = false;
    public double getRange(){return range;}
    public void setRange(double newRange){if (newRange >0) range = newRange;}
    public int getAntsNumber(){return antsNumber;}
    public void setAntsNumber(int ants){if (ants >0) antsNumber = ants;}
    public double getPExplore(){return pExplore;}
    public void setPExplore(double expl){if (expl >=0 && expl <=1 ) pExplore = expl;}
    public double getPFollow(){return pFollow;}
    public void setPFollow(double follow){if (follow >=0 && follow <=1 ) pFollow = follow;}
    public double getEvaporationConstant(){return evaporationConstant;}
    public void setEvaporationConstant(double newConst){if (newConst >=0 && newConst <=1 ) evaporationConstant = newConst;}
    public Object domEvaporationConstant () { return new sim.util.Interval(0.0,1.0);}
    public Object domPExplore () { return new sim.util.Interval(0.0,1.0);}
    public Object domPFollow () { return new sim.util.Interval(0.0,1.0);}
    public double getPDeploy(){return pDeploy;}
    public void setPDeploy(double newConst){if (newConst >=0 && newConst <=1 ) pDeploy = newConst;}
    public Object domPDeploy () { return new sim.util.Interval(0.0,1.0);}

    public ForagingWithBeacons(long seed)
    {
        super(seed);
    }
    public void start()
    {
        super.start();

        antsPos.clear();
        beaconsPos.clear();
        nestPos.clear();
        foodPos.clear();

        if (fixedBeacons){
            int grid_density = 10;
            double spacing = WORLD_SIZE / (grid_density + 1);
            for (int x = 1; x <= grid_density; x++){
                for (int y = 1; y <= grid_density; y++){
                    Beacon b = new Beacon();
                    beaconsPos.setObjectLocation(b,
                                                 new Double2D(spacing * x,
                                                              spacing * y));
                    b.stopper = schedule.scheduleRepeating(schedule.EPOCH, 1, b);

                }
            }
        }

        for (int k=0; k < antsNumber; k++){
            Ant ant = new Ant(reward);
            antsPos.setObjectLocation(ant, new Double2D(X_NEST, Y_NEST));
            schedule.scheduleRepeating(schedule.EPOCH, 0, ant);
        }
        nestPos.setObjectLocation(new Nest(), new Double2D(X_NEST, Y_NEST));
        foodPos.setObjectLocation(new Food(), new Double2D(90.0, 90.0));
    }
    public static void main(String[] args)
    {
        doLoop(ForagingWithBeacons.class, args);
        System.exit(0);
    }
}
