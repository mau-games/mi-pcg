package game;

import java.util.List;

import generator.algorithm.Algorithm;
import generator.algorithm.Ranges;
import generator.config.Config;
import generator.config.Config.TLevel;
import util.Point;
import util.eventrouting.EventRouter;
import util.eventrouting.Listener;
import util.eventrouting.PCGEvent;
import util.eventrouting.events.Start;

public class Game implements Listener{

    public static int sizeN; //Horizontal room size in tiles
    public static int sizeM; //Vertical room size in tiles
    public static int sizeDoors; //TODO: ???
	public static String randomRangesFileName = "rangesSupervised"; //File containing probability ranges for generating particular tile types
    public static String gameConfigFileName;
    public static Ranges ranges; 
    public static List<Point> doorsPositions = null; //For fixed door positions - set them here. TODO: Rethink this?
    public static Config.TLevel level; //Difficulty level
    private Algorithm geneticAlgorithm;
    
    public static Config.TLevel getLevel()
    {
        return level;
    }

    public static Ranges getRanges()
    {
        return ranges;
    }

    public Game(int n, int m, int doors, Config.TLevel level, String profile)
    {
        ranges = new Ranges();
        sizeN = n;
        sizeM = m;
        sizeDoors = doors;
        Game.level = level;
        gameConfigFileName = profile;
        
        EventRouter.getInstance().registerListener(this, new Start());
    }

	/**
	 *  Kicks the algorithm into action.
	 */
    private void startAll()
    {
    	reinit();
    	geneticAlgorithm = new Algorithm(Algorithm.POPULATION_SIZE, new Config(gameConfigFileName));
    	//Start the algorithm on a new thread.
    	geneticAlgorithm.start();
    }
    
    /**
     * Set everything back to its initial state before running the genetic algorithm
     */
    private void reinit(){
    	doorsPositions = null;
    }
    
    public void stop(){
    	if(geneticAlgorithm != null && geneticAlgorithm.isAlive()){
    		geneticAlgorithm.terminate();
    	}
    }

	@Override
	public synchronized void ping(PCGEvent e) {
		if(e instanceof Start){
			startAll();		
		}
	}

	// TODO: Bad code smell. This feels really out of place.
	public static TLevel parseDifficulty(String difficulty) {
		TLevel level;
		
		if (difficulty.equals("medium")) {
			level = TLevel.MEDIUM;
		} else if (difficulty.equals("hard")) {
			level = TLevel.HARD;
		} else {
			level = TLevel.EASY;
		}
			
		return level;
	}

	
}
