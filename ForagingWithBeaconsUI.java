import sim.portrayal.continuous.*;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;

public class ForagingWithBeaconsUI extends GUIState
{
    public Display2D display;
    public JFrame displayFrame;
    ContinuousPortrayal2D beaconsPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D nestPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D foodPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D antsPortrayal = new ContinuousPortrayal2D();

    public static void main(String[]args)
    {
        ForagingWithBeaconsUI vid = new ForagingWithBeaconsUI();
        Console c = new Console(vid);
        c.setVisible(true);
    }
    public ForagingWithBeaconsUI()
    {
        super(new ForagingWithBeacons(System.currentTimeMillis()));

        //added to remove the printed on file data when using the GUI
        ForagingWithBeacons sim = (ForagingWithBeacons)state;
        sim.PRINT_ON_FILE = false;
    }
    public ForagingWithBeaconsUI(SimState state){super(state);}

    public Object getSimulationInspectedObject() {return state;}
    public Inspector getInspector()
    {
        Inspector i = super.getInspector();
        i.setVolatile(true);
        return i;
    }

    public static String getName() { return "Foraging with beacons project";}
    public void start()
    {

        super.start();
        setupPortrayals();
    }
    public void load(SimState state)
    {
        super.load(state);
        setupPortrayals();
    }

    public void setupPortrayals()
    {
        ForagingWithBeacons fwb = (ForagingWithBeacons) state;
        beaconsPortrayal.setField(fwb.beaconsPos);
        beaconsPortrayal.setPortrayalForAll(new CircledPortrayal2D(new HexagonalPortrayal2D(false)
            {
                public void draw( Object object,Graphics2D graphics,DrawInfo2D info)
                {
                    Beacon beacon = (Beacon) object;
                    if (beacon.foragingPheromone == 0.0 ) paint = Color.red;
                    else
                        paint = new Color(20,Math.min(255,(int)(beacon.foragingPheromone/2*255)),20);
                    super.draw(object, graphics, info);
                }
            },0,fwb.range*2, Color.black, false)
            {
                public void draw( Object object,Graphics2D graphics,DrawInfo2D info)
                {
                    Beacon beacon = (Beacon) object;
                    if (beacon.ferryingPheromone == 0.0 ) paint = Color.red;
                    else
                        paint = new Color(20,20,Math.min(255,(int)(beacon.ferryingPheromone/2*255)));
                    super.draw(object, graphics, info);
                }
            });
        antsPortrayal.setField(fwb.antsPos);
        antsPortrayal.setPortrayalForAll(new OvalPortrayal2D()
            {
                public void draw( Object object, Graphics2D graphics, DrawInfo2D info){
                    Ant ant = (Ant) object;
                    if ( ant.ferrying) paint = Color.red;
                    else paint = Color.yellow;
                    super.draw(object, graphics, info);
                }
            });
        foodPortrayal.setField(fwb.foodPos);
        foodPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.cyan));
        nestPortrayal.setField(fwb.nestPos);
        nestPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.magenta));
        display.reset();
        display.setBackdrop(Color.white);
        display.repaint();
    }

    public void init(Controller c)
    {
        super.init(c);
        display = new Display2D(600,600,this);
        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("foraging display");
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);
        display.attach(beaconsPortrayal,"beacons");
        display.attach(antsPortrayal,"ants");
        display.attach(foodPortrayal,"food");
        display.attach(nestPortrayal,"nest");
    }
    public void quit()
    {
        super.quit();
        if(displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
    }
}
