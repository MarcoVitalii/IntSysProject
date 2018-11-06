import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

public class ForagingWithBeacons extends SimState
{
    public static boolean PRINT_ON_FILE = true;
    public static final double WORLD_SIZE = 100;
    public static final double X_NEST = 10.0;
    public static final double Y_NEST = 10.0;
    public static final double X_FOOD = 90.0;
    public static final double Y_FOOD = 90.0;
    public static final int MEAN_TIME = 50;
    public Continuous2D antsPos = new Continuous2D(1.0, WORLD_SIZE, WORLD_SIZE);
    public Continuous2D beaconsPos = new Continuous2D(1.0, WORLD_SIZE, WORLD_SIZE);
    public Continuous2D foodPos = new Continuous2D(1.0, WORLD_SIZE, WORLD_SIZE);
    public Continuous2D nestPos = new Continuous2D(1.0, WORLD_SIZE, WORLD_SIZE);
    public double range = 10.0;
    public double minRange = 5.0;
    public int antsNumber = 100;
    public double reward = 1.0;
    public double pExplore = 0.1;
    public double pFollow = 0.95;
    public double pMove = 0.1;
    public int countMax = 5;
    public int beaconTimeScale = 150;
    public double beaconShrinkingFactor = Math.pow(minRange / range, 1.0 / beaconTimeScale);
    //tau is a shape factor for pRemove
    public double tau = 40;
    //maxBeaconNumber is a shape factor for pDeploy
    public int maxBeaconNumber = 50;
    public double evaporationConstant = 0.95;
    public final boolean fixedBeacons = false;
    public final boolean ARTICLE_CODE = true;
    //double[] actionsTaken = new double[10];
    //public double[] getActionsTaken () {return actionsTaken;}


    //Setters and getters to manipulate the model through the Model panel in the console
    public int getAntsNumber(){return antsNumber;}
    public void setAntsNumber(int ants){if (ants >0) antsNumber = ants;}

    public double getPExplore(){return pExplore;}
    public void setPExplore(double expl){if (expl >=0 && expl <=1 ) pExplore = expl;}
    public Object domPExplore () { return new sim.util.Interval(0.0,1.0);}

    public double getPFollow(){return pFollow;}
    public void setPFollow(double follow){if (follow >=0 && follow <=1 ) pFollow = follow;}
    public Object domPFollow () { return new sim.util.Interval(0.0,1.0);}

    public double getEvaporationConstant(){return evaporationConstant;}
    public void setEvaporationConstant(double newConst){if (newConst >=0 && newConst <=1 ) evaporationConstant = newConst;}
    public Object domEvaporationConstant () { return new sim.util.Interval(0.0,1.0);}

    public int getBeaconTimeScale () { return beaconTimeScale;}
    public void setBeaconTimeScale(int newFactor)
    {
        if ( newFactor > 0 ){
        beaconTimeScale = newFactor;
        beaconShrinkingFactor = Math.pow(minRange / range, 1.0 / beaconTimeScale);
        }
    }

    public double getPMove(){return pMove;}
    public void setPMove(double newP) {if (newP >=0 && newP <=1) pMove = newP;}
    public Object domPMove () { return new sim.util.Interval(0.0,1.0);}


    /* Features not included in current studies.
    double beaconSigma = 0;
    public double getBeaconSigma () {return beaconSigma;}
    public void setBeaconSigma (double bs) { if (bs >= 0 && bs <= range ) beaconSigma = bs;}
    public Object domBeaconSigma () { return new sim.util.Interval(0.0,range);}

    double foodSigma = 0.0;
    public double getFoodSigma () {return foodSigma;}
    public void setFoodSigma (double fs) { if (fs >= 0 && fs <= range ) foodSigma = fs;}
    public Object domFoodSigma () { return new sim.util.Interval(0.0,range);}
    */
    //agent needed to collect and print stats of the model
    StatAgent stats;


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
                    Beacon b = new Beacon(new Double2D(spacing *x , spacing * y), range);
                    beaconsPos.setObjectLocation(b,
                                                 new Double2D(spacing * x,
                                                              spacing * y));
                    b.stopper = schedule.scheduleRepeating(schedule.EPOCH, 1, b);

                }
            }
        }

        if (ARTICLE_CODE){
            beaconShrinkingFactor = 1.0;
            for (int k=0; k < antsNumber; k++){
                Ant ant = new Ant(reward){
                        public double pRemove(ForagingWithBeacons fwb)
                        {
                            return 1;
                        }
                        public double pDeploy(ForagingWithBeacons fwb)
                        {
                            if (fwb.beaconsPos.size() < fwb.maxBeaconNumber)
                                return 0.9;
                            else{
                                System.out.println("MAX BEACON REACHED");
                                return 0.0;
                            }
                        }
                    };
                antsPos.setObjectLocation(ant, new Double2D(X_NEST, Y_NEST));
                schedule.scheduleRepeating(schedule.EPOCH, 0, ant);
            }
        }
        else{
            for (int k=0; k < antsNumber; k++){
            Ant ant = new Ant(reward);
            antsPos.setObjectLocation(ant, new Double2D(X_NEST, Y_NEST));
            schedule.scheduleRepeating(schedule.EPOCH, 0, ant);
            }
        }
        Nest nest = new Nest();
        nestPos.setObjectLocation(nest, new Double2D(X_NEST, Y_NEST));

        //schedule.scheduleRepeating(schedule.EPOCH, 1, nest, MEAN_TIME);
        Food food = new Food();
        foodPos.setObjectLocation(food, new Double2D(X_FOOD, Y_FOOD));
        //line below is needed if food is allowed do do a random walk.
        //schedule.scheduleRepeating(schedule.EPOCH, 1, food);

        stats = new StatAgent (this, PRINT_ON_FILE);
        if (PRINT_ON_FILE){
            schedule.scheduleRepeating(schedule.EPOCH, 2, stats, stats.MEAN_TIME);
        }
    }
    public static void main(String[] args)
    {
        doLoop(ForagingWithBeacons.class, args);
        System.exit(0);
    }
}
