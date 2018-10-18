import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ForagingWithBeacons extends SimState
{
    public static boolean PRINT_ON_FILE = true;
    public static final int MAX_BEACON_NUMBER = 50;
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
    double range = 10.0;
    int antsNumber = 100;
    double reward = 1.0;
    double pExplore = 0.001;
    double pFollow = 0.90;
    double pDeploy = 0.5;
    double pMove = 0.1;
    int countMax = 5;
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
    //public double getPDeploy(){return pDeploy;}
    //public void setPDeploy(double newConst){if (newConst >=0 && newConst <=1 ) pDeploy = newConst;}
    //public Object domPDeploy () { return new sim.util.Interval(0.0,1.0);}
    public Object domPMove () { return new sim.util.Interval(0.0,1.0);}
    public double getPMove(){return pMove;}
    public void setPMove(double newP) {if (newP >=0 && newP <=1) pMove = newP;}


    public int getBeaconsNumber(){return beaconsPos.size();}


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

        for (int k=0; k < antsNumber; k++){
            Ant ant = new Ant(reward);
            antsPos.setObjectLocation(ant, new Double2D(X_NEST, Y_NEST));
            schedule.scheduleRepeating(schedule.EPOCH, 0, ant);
        }
        Nest nest = new Nest();
        nestPos.setObjectLocation(nest, new Double2D(X_NEST, Y_NEST));
        schedule.scheduleRepeating(schedule.EPOCH, 1, nest, MEAN_TIME);
        foodPos.setObjectLocation(new Food(), new Double2D(X_FOOD, Y_FOOD));
        //New part to create data files that logs performances
        if (PRINT_ON_FILE){
            try(BufferedWriter bw = new BufferedWriter(new FileWriter("data/settings.txt"))){
                bw.write("ants number: "+ antsNumber
                         +"\nrange: "+range
                         +"\nreward: "+ reward
                         +"\ncountMax: " + countMax
                         +"\npExplore: " + pExplore
                         +"\npFollow: " + pFollow
                         +"\npMove: " + pMove
                         +"\nevaporationConstant: " + evaporationConstant
                         +"\nMAX_BEACON_NUMBER: "+ MAX_BEACON_NUMBER
                         +"\nWORLD_SIZE: " + WORLD_SIZE
                         +"\nX_NEST: " + X_NEST
                         +"\nY_NEST: " + Y_NEST
                         +"\nX_FOOD: " + X_FOOD
                         +"\nY_FOOD: " + Y_FOOD
                         +"\nMEAN_TIME: " + MEAN_TIME
                         );
            }
            catch(IOException e){}

            schedule.scheduleRepeating(schedule.EPOCH,2, new Steppable(){
                    public void step (SimState state)
                    {
                        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
                        Nest nest =(Nest)(fwb.nestPos.getAllObjects().get(0));
                        try(BufferedWriter bw = new BufferedWriter(new FileWriter("data/"+seed()+".csv",true))){
                            bw.write(fwb.schedule.time()+","+nest.foodIncomingRate+","+fwb.beaconsPos.size()+","+nest.meanTravelLength+"\n");
                        }
                        catch(IOException e){}
                    }
                },MEAN_TIME);
        }
    }
    public static void main(String[] args)
    {
        doLoop(ForagingWithBeacons.class, args);
        System.exit(0);
    }
}
