package game;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import generator.algorithm.Algorithm;
import generator.config.GeneratorConfig;
import util.Point;
import util.Util;
import util.config.ConfigurationUtility;
import util.config.MissingConfigurationException;
import util.eventrouting.EventRouter;
import util.eventrouting.Listener;
import util.eventrouting.PCGEvent;
import util.eventrouting.events.AlgorithmDone;
import util.eventrouting.events.AlgorithmStarted;
import util.eventrouting.events.BatchDone;
import util.eventrouting.events.RenderingDone;
import util.eventrouting.events.Start;
import util.eventrouting.events.StartBatch;
import util.eventrouting.events.StartMapMutate;
import util.eventrouting.events.Stop;

public class Game implements Listener{
	private final Logger logger = LoggerFactory.getLogger(Game.class);

	private ApplicationConfig config;
	private List<Algorithm> runs = new ArrayList<Algorithm>();
	private int batchRunsLeft = 0;
	private int batchRunsStillToFinish = 0;
	private boolean batch = false;
	private String batchConfig = "";
	private static final int batchThreads = 8;
	
	//TODO: There must be a better way to handle these public static variables
	public static int sizeM; //Number of columns
    public static int sizeN; //Number of rows
    public static int doorCount;
    public static List<Point> doors = new ArrayList<Point>();

   

    public Game() {

		try {
			config = ApplicationConfig.getInstance();
		} catch (MissingConfigurationException e) {
			logger.error("Couldn't read configuration file:\n" + e.getMessage());
		}

        readConfiguration();
        chooseDoorPositions();

        EventRouter.getInstance().registerListener(this, new Start());
        EventRouter.getInstance().registerListener(this, new StartMapMutate(null));
        EventRouter.getInstance().registerListener(this, new Stop());
        //EventRouter.getInstance().registerListener(this, new AlgorithmDone(null));
        EventRouter.getInstance().registerListener(this, new RenderingDone());
        EventRouter.getInstance().registerListener(this, new StartBatch());
    }
    
    /**
     * Selects positions for between 1 and 4 doors. 
     * The first door is the main entrance. 
     * Doors can't be in corners.
     */
    private void chooseDoorPositions(){
    	doors.clear();
    	List<Integer> walls = new ArrayList<Integer>();
    	walls.add(0);
    	walls.add(1);
    	walls.add(2);
    	walls.add(3);
    	for(int i = 0; i < doorCount; i++){
    		switch(walls.remove(Util.getNextInt(0, walls.size()))){
    		case 0: //North
    			//doors.add(new Point(Util.getNextInt(1, sizeM - 1), sizeN - 1));
    			doors.add(new Point(sizeM / 2, sizeN - 1));
    			break;
    		case 1: //East
    			//doors.add(new Point(sizeM - 1, Util.getNextInt(1, sizeN - 1)));
    			doors.add(new Point(sizeM - 1, sizeN / 2));
    			break;
    		case 2: //South
    			//doors.add(new Point(Util.getNextInt(1, sizeM - 1), 0));
    			doors.add(new Point(sizeM / 2, 0));
    			break;
    		case 3: //West
    			//doors.add(new Point(0, Util.getNextInt(1, sizeN - 1)));
    			doors.add(new Point(0, sizeN / 2));
    			break;
    		}
    	}
    }

    public enum MapMutationType {
    	Preserving,
    	OriginalConfig,
    	ComputedConfig
    }
    
    private void mutateFromMap(Map map, int mutations, MapMutationType mutationType, boolean randomise){
    	sizeM = map.getColCount();
    	sizeN = map.getRowCount();

    	for(int i = 0; i < mutations; i++){
    		switch(mutationType){
			case ComputedConfig:
			{
				GeneratorConfig gc = map.getCalculatedConfig();
				if(randomise)
					gc.mutate();
				Algorithm ga = new Algorithm(gc);
				runs.add(ga);
				ga.start();
				break;
			}
			case OriginalConfig:
			{
				GeneratorConfig gc = new GeneratorConfig(map.getConfig());
				if(randomise)
					gc.mutate();
				Algorithm ga = new Algorithm(gc);
				runs.add(ga);
				ga.start();
				break;
			}
			case Preserving:
			{
				Algorithm ga = new Algorithm(map);
				runs.add(ga);
				ga.start();
				break;
			}
			default:
				break;
        	}
		}
    	
    }
    
	/**
	 *  Kicks the algorithm into action.
	 */
    private void startAll(int runCount)
    {
    	reinit();
    	Algorithm geneticAlgorithm = null;
    	
    	List<String> configs = new ArrayList<String>();
    	if(Math.random() < 0.5)
    		configs.add("config/bendycorridors.json");
    	else
    		configs.add("config/bendycorridors_nodeadends.json");
    	if(Math.random() < 0.5)
    		configs.add("config/straightcorridors.json");
    	else
    		configs.add("config/straightcorridors_nodeadends.json");
    	if(Math.random() < 0.5)
    		configs.add("config/smallrooms.json");
    	else
    		configs.add("config/smallrooms_nodeadends.json");
    	configs.add("config/mediumrooms.json");
    	configs.add("config/bigrooms.json");
    	configs.add("config/roomsandcorridorssquare.json");
//    	
    	for(int i = 0; i < runCount; i++){
    		String c = "config/generator_config.json";
    		if(!configs.isEmpty())
    			c = configs.remove(Util.getNextInt(0, configs.size()));
    		
    		try {
    			geneticAlgorithm = new Algorithm(new GeneratorConfig(c));
    			runs.add(geneticAlgorithm);
    			geneticAlgorithm.start();
    		} catch (MissingConfigurationException e) {
    			logger.error("Couldn't read generator configuration file:\n" + e.getMessage());
    		}
    	}
    	
    }
    
    private void startBatch(String config, int size){
    	batch = true;
    	batchRunsLeft = size;
    	batchRunsStillToFinish = size;
    	batchConfig = config;
    	
    	runs.clear();
    	
    	for(int i = 0; i < batchThreads; i++){
    		startBatchRun();
    	}
    	
    }
    
    private void startBatchRun(){
		try {
			Algorithm geneticAlgorithm = new Algorithm(new GeneratorConfig(batchConfig));
			geneticAlgorithm.start();
			runs.add(geneticAlgorithm);
			batchRunsLeft--;
		} catch (MissingConfigurationException e) {
			e.printStackTrace();
		}
    }
    
  
    
    
    
//    public void batchRun(){
//    	readConfiguration();
//    	chooseDoorPositions();
//    	batch = true;
//    	runCount = 0;
//    	batchStep();
//    	
//    }
    
//    private void batchStep(){
//    	
//		EventRouter.getInstance().postEvent(new AlgorithmStarted("" + runCount));
//		try {
//			geneticAlgorithm = new Algorithm(new GeneratorConfig());
//		} catch (MissingConfigurationException e) {
//			logger.error("Couldn't read generator configuration file:\n" + e.getMessage());
//		}
//    	//Start the algorithm on a new thread.
//    	geneticAlgorithm.start();
//    	runCount++;
//    }
    
    /**
     * Set everything back to its initial state before running the genetic algorithm
     */
    private void reinit(){
    	doors.clear();
    	chooseDoorPositions();
    }
    
    /**
     * Stop the algorithm. Used in the case that the application window is closed.
     */
    public void stop(){
    	for(Algorithm a : runs){
    		if(a.isAlive()) a.terminate();
    	}
//    	if(geneticAlgorithm != null && geneticAlgorithm.isAlive()){
//    		geneticAlgorithm.terminate();
//    	}
    }

	@Override
	public synchronized void ping(PCGEvent e) {
		if(e instanceof Start){
			readConfiguration();
			startAll(((Start) e).getNbrOfThreads());
		} else if (e instanceof StartMapMutate) {
			StartMapMutate smm = (StartMapMutate)e;
			mutateFromMap((Map)e.getPayload(),smm.getMutations(),smm.getMutationType(),smm.getRandomiseConfig());
		} else if (e instanceof StartBatch) {
			startBatch(((StartBatch)e).getConfig(), ((StartBatch)e).getSize());
		} else if (e instanceof Stop) {
			stop();
		} else if (e instanceof RenderingDone){
			if(batch){
				batchRunsStillToFinish--;
				if(batchRunsLeft > 0) {
					startBatchRun();
				}
				if(batchRunsStillToFinish == 0){
					EventRouter.getInstance().postEvent(new BatchDone());
				}
			}
		}
	}

	/**
	 * Reads and applies the current configuration.
	 */
	private void readConfiguration() {  
        sizeM = config.getDimensionM();
        sizeN = config.getDimensionN();
        doorCount = config.getDoors();
	}
	
}
