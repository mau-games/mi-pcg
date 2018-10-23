package game;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Map.Entry;

import finder.PatternFinder;
import finder.Populator;
import finder.geometry.Bitmap;
import finder.geometry.Polygon;
import finder.graph.Graph;
import util.algorithms.Node;
import finder.patterns.Pattern;
import finder.patterns.SpacialPattern;
import finder.patterns.micro.Connector;
import finder.patterns.micro.Corridor;
import finder.patterns.micro.Enemy;
import finder.patterns.micro.Chamber;
import finder.patterns.micro.Treasure;
import util.Point;
import util.config.MissingConfigurationException;
import util.eventrouting.EventRouter;
import util.eventrouting.events.AlgorithmDone;
import util.eventrouting.events.MapLoaded;
import util.eventrouting.events.MapUpdate;
import generator.config.GeneratorConfig;

/**
 * This class represents a dungeon room map.
 * 
 * @author Johan Holmberg, Malmö University
 * @author Alexander Baldwin, Malmö University
 * @author Chelsi Nolasco, Malmö University
 * @author Axel Österman, Malmö University
 */
public class Room {
	
////////////////////////////NEW/////////////////////////////////
	
	//Maybe I can copy the finder.graph info into other types, I feel that it will give me a lot of constraints as well.
	public finder.graph.Node<Room> node; //This will hold in the edges the doors
	public RoomConfig localConfig;
	public int maxNumberDoors; //--> HAHA
	public Bitmap borders = new Bitmap();

/////////////////////////OLD///////////////////////////

	private Tile[] tileMap; //This HAVE to be the real tilemap
	public int[][] matrix; // The actual map
	private boolean[][] allocated; // A map keeps track of allocated tiles
	private int width;			// The number of columns in a map
	private int height;			// The number of rows in a map
	private int doorCount;	// The number of doors in a map TOTAL
	private int wallCount;	// The number of wall tiles in a map
	private List<Point> doors = new ArrayList<Point>();
	private List<Point> treasures = new ArrayList<Point>();
	private List<Point> enemies = new ArrayList<Point>();
	//private Graph<SpacialPattern> graph = new Graph<SpacialPattern>();
	private PatternFinder finder;
	private int failedPathsToTreasures;
	private int failedPathsToEnemies;
	private int failedPathsToAnotherDoor;
	private Dictionary<Point, Double> treasureSafety;
	private Point entrance;
	private double entranceSafety;
	private double entranceGreed;
	private GeneratorConfig config = null;
	
	//NEW THINGS
	public ZoneNode root;

	/**
	 * Creates an instance of map.
	 * 
	 * @param rows The number of rows in a map.
	 * @param cols The number of columns in a map.
	 */
	private Room(int rows, int cols) { //THIS IS CALLED WHEN LOADING THE ROOM FROM A STRING
		init(rows, cols);
		finder = new PatternFinder(this);
		
		for (int j = 0; j < height; j++)
		{
			for (int i = 0; i < width; i++) 
			{
				tileMap[j * width + i] = new Tile(i, j, matrix[j][i]);
			}
		}
		
		root = new ZoneNode(null, this, getColCount(), getRowCount());
	}
	
	
	/**
	 * Creates an instance of map.
	 * 
	 * @param types A chromosome transformed into an array of TileTypes.
	 * @param rows The number of rows in a map.
	 * @param cols The number of columns in a map.
	 * @param doorCount The number of doors to be seeded in a map.
	 */
	public Room(GeneratorConfig config, TileTypes[] types, int rows, int cols, List<Point> doorPositions, Point entrance) { //THIS IS CALLED WHEN CREATIMNG THE PHENOTYPE
		init(rows, cols);

		this.config = config;
//		localConfig = new RoomConfig(this, 40); //TODO: NEW ADDITION --> HAVE TO BE ADDED EVERYWHERE

		initMapFromTypes(types);
		copyDoors(doorPositions, entrance);

		finder = new PatternFinder(this);
		root = new ZoneNode(null, this, getColCount(), getRowCount());

	}
	
	public Room(GeneratorConfig config, int rows, int cols, int scaleFactor) //THIS IS CALLED WHEN ADDING ROOMS TO THE DUNGEON!
	{
		init(rows, cols);

		this.config = config;
		localConfig = new RoomConfig(this, scaleFactor); //TODO: NEW ADDITION --> HAVE TO BE ADDED EVERYWHERE
		
		this.doorCount = 0;
		
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {

				tileMap[j * width + i] = new Tile(i , j, TileTypes.toTileType(matrix[j][i]));
			}
		}

		finder = new PatternFinder(this);
		root = new ZoneNode(null, this, getColCount(), getRowCount());
		node = new finder.graph.Node<Room>(this);

	}
	
	public Room(Room copyMap, ZoneNode zones) //THIS IS CALLED WHEN CREATING A ZONE IN THE TREE (TO HAVE A COPY OF THE DOORS)
	{
		init(copyMap.getRowCount(), copyMap.getColCount());
		this.config = copyMap.config;
		//TODO: NEW ADDITION --> HAVE TO BE ADDED EVERYWHERE
//		localConfig = new RoomConfig(this, 40); //PROBABLY something must be done here 
		
		for (int j = 0; j < height; j++)
		{
			for (int i = 0; i < width; i++) 
			{
				matrix[j][i] = copyMap.matrix[j][i];
				tileMap[j * width + i] = new Tile(copyMap.tileMap[j * width + i]);
			}
		}	
		
		for (int j = 0; j < height; j++){
			for (int i = 0; i < width; i++) {
				switch (TileTypes.toTileType(matrix[j][i])) {
				case WALL:
					wallCount++;
					break;
				case ENEMY:
					enemies.add(new Point(i, j));
					break;
				case TREASURE:
					treasures.add(new Point(i, j));
					break;
				default:
					break;
				}
			}
		}

		copyDoors(copyMap.getDoors(), copyMap.getEntrance());
		
		finder = new PatternFinder(this);
		root = zones;	
	}
	
	public Room(GeneratorConfig config, ZoneNode rootCopy, int[] chromosomes, int rows, int cols) //THIS IS CALLED FROM THE PHENOTYPE BUT WHEN WE HAVE THE ZONES
	{
		init(rows, cols);

		this.config = config;
//		this.localConfig = new RoomConfig(this, 40); //TODO: NEW ADDITION --> HAVE TO BE ADDED EVERYWHERE
		
		CloneMap(rootCopy.GetMap(), chromosomes);
	}
	
	private void CloneMap(Room room, int[] chromosomes) //FROM THE PREVIOUS CONSTRUCTOR
	{
		this.tileMap = room.tileMap.clone();
		this.matrix = room.matrix.clone();

		for (int j = 0; j < height; j++)
		{
			for (int i = 0; i < width; i++) 
			{
				if(!room.tileMap[j * width + i].GetImmutable())
				{
					setTile(i, j, chromosomes[j * width + i]);
				}
				else
				{
					tileMap[j * width + i] = new Tile(room.tileMap[j * width + i]);
					matrix[j][i] = chromosomes[j * width + i];
				}
			}
		}	
		
		for (int j = 0; j < height; j++){
			for (int i = 0; i < width; i++) {
				switch (TileTypes.toTileType(matrix[j][i])) {
				case WALL:
					wallCount++;
					break;
				case ENEMY:
					enemies.add(new Point(i, j));
					break;
				case TREASURE:
					treasures.add(new Point(i, j));
					break;
				default:
					break;
				}
			}
		}
		
		copyDoors(room.getDoors(), room.getEntrance());
//		
		finder = new PatternFinder(this);
		root = new ZoneNode(null, this, width, height);
		
	}
	
	/***
	 * Basic init code to initialize all the aspects in the room
	 * @param rows
	 * @param cols
	 */
	private void init(int rows, int cols) 
	{

		treasureSafety = new Hashtable<Point, Double>();
		this.width = cols;
		this.height = rows;
		wallCount = 0;
		this.doorCount = 0;
		
		matrix = new int[height][width];
		allocated = new boolean[height][width];
		tileMap = new Tile[rows * cols];
		
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				if(y == 0 || y == height - 1)
				{
					borders.addPoint(new finder.geometry.Point(x, y));
				}
				else if(x == 0 || x == width - 1 )
				{
					borders.addPoint(new finder.geometry.Point(x, y));
				}	
			}
		}
	}
	
	/**
	 * Copy doors from a list of existing ones (/another room) and override whatever tile was there
	 * This is for the new phenotypes when created
	 * @param doorPositions
	 * @param entrance
	 */
	private void copyDoors(List<Point> doorPositions, Point entrance)
	{
		setEntrance(entrance);
		
		for(int i = 0; i < doorPositions.size(); i++)
        {
           
            if (TileTypes.toTileType(matrix[doorPositions.get(i).getY()][doorPositions.get(i).getX()]).isEnemy())  // Check if door overrides an enemy
            {
            	int ii = i;
            	enemies.removeIf((x)->x.equals(doorPositions.get(ii)));
            }
            else if (TileTypes.toTileType(matrix[doorPositions.get(i).getY()][doorPositions.get(i).getX()]).isTreasure()) // Check if door overrides a treasure
            {
            	int ii = i;
            	treasures.removeIf((x)->x.equals(doorPositions.get(ii)));
            }
            else if (matrix[doorPositions.get(i).getY()][doorPositions.get(i).getX()] == TileTypes.WALL.getValue()) // Check if door overrides a wall
            {
                wallCount--;
            } 
            
            if(doorPositions.get(i).equals(entrance))
            {
            	setTile(doorPositions.get(i).getX(), doorPositions.get(i).getY(), TileTypes.DOORENTER);
            }
            else
            {
            	setTile(doorPositions.get(i).getX(), doorPositions.get(i).getY(), TileTypes.DOOR);
            }
            
            addDoor(doorPositions.get(i));
            borders.removePoint(Point.castToGeometry(doorPositions.get(i))); //remove this point from the "usable" border
            
    	}
	}
	
	/***
	 * Create a door in the specified location (It should be guaranteed to be in the border of the room)
	 * This method is for when you connect two different rooms through doors
	 * @param doorPosition
	 */
	public void createDoor(Point doorPosition)
	{
		//Check what will be overwritten
		// Check if door overrides an enemy
        if (TileTypes.toTileType(matrix[doorPosition.getY()][doorPosition.getX()]).isEnemy())
        {
        	enemies.removeIf((x)->x.equals(doorPosition));
        }
        else if (TileTypes.toTileType(matrix[doorPosition.getY()][doorPosition.getX()]).isTreasure())   // Check if door overrides a treasure
        {
        	treasures.removeIf((x)->x.equals(doorPosition));
        }
        else if (matrix[doorPosition.getY()][doorPosition.getX()] == TileTypes.WALL.getValue()) // Check if door overrides a wall
        {
            wallCount--;
        } 
        
        if(doors.size() == 0) //TODO: PLEASE CHANGE ME!!! 
        {
        	setTile(doorPosition.getX(), doorPosition.getY(), TileTypes.DOORENTER);
        	entrance = doorPosition;

        }
        else
        {
        	setTile(doorPosition.getX(), doorPosition.getY(), TileTypes.DOOR);

        }
        
        addDoor(doorPosition);
        borders.removePoint(Point.castToGeometry(doorPosition)); //remove this point from the "usable" border
}
	
	
	/***
	 * Updates the different tile-matrix-components of the room based on changes in the zones
	 * @param updatedMatrix
	 */
	public void Update(int[] updatedMatrix)
	{
		int tile = 0;
		wallCount = 0;
		enemies.clear();
		treasures.clear();
		
		for (int j = 0; j < height; j++) 
		{
			for (int i = 0; i < width; i++) 
			{
				setTile(i, j, updatedMatrix[tile++]);
				
				switch (TileTypes.toTileType(matrix[j][i])) {
				case WALL:
					wallCount++;
					break;
				case ENEMY:
					enemies.add(new Point(i, j));
					break;
				case TREASURE:
					treasures.add(new Point(i, j));
					break;
				default:
					break;
				}
			}
		}
		
		finder = new PatternFinder(this);
	}

	/**
	 * Invalidates the current calculations and forces a re-evaluation of the
	 * map.
	 * 
	 * TODO: This isn't very efficient, but shouldn't be so frequently used, as
	 * to warrant any particular worry. Maybe replace it with a more granular
	 * approach sometime?
	 * 
	 * TODO: Here the cases of door and doorenter are being disable probably part of the problem with the doors
	 */
	public void forceReevaluation() {
		treasures.clear();
		enemies.clear();
		doors.clear();
		treasureSafety = new Hashtable<Point, Double>();
		wallCount = 0;
		this.doorCount = 0;
		allocated = new boolean[height][width];
		
		for (int j = 0; j < height; j++){
			for (int i = 0; i < width; i++) {
				switch (TileTypes.toTileType(matrix[j][i])) {
				case WALL:
					wallCount++;
					break;
				case ENEMY:
					enemies.add(new Point(i, j));
					break;
				case TREASURE:
					treasures.add(new Point(i, j));
					break;
				case DOOR:
					addDoor(new Point(i, j));
					break;
				case DOORENTER:
					setEntrance(new Point(i, j));
					addDoor(new Point(i, j));
					break;
				default:
					break;
				}
			}
		}
		
		//markDoors();
		finder = new PatternFinder(this);
		
		//Update the zones - It could be more efficient by only updating the affected zone
		//(As we already have separated the map)
		root = new ZoneNode(null, this, getColCount(), getRowCount());
	}

	public GeneratorConfig getConfig(){
		return config;
	}

	public void setConfig(GeneratorConfig config){
		this.config = config;
	}

	public void resetAllocated(){
		allocated = new boolean[height][width];
	}

	/**
	 * Gets a list of positions of tiles adjacent to a given position which are not walls (passable tiles)
	 * 
	 * @param position The position of a tile
	 * @return A list of points 
	 */
	public List<Point> getAvailableCoords(Point position){
		List<Point> availableCoords = new ArrayList<Point>();

		if(position.getX() > 0 && getTile((int)position.getX() - 1, (int)position.getY()).GetType() != TileTypes.WALL)
			availableCoords.add(new Point(position.getX()-1,position.getY()));
		if(position.getX() < width - 1 && getTile((int)position.getX() + 1, (int)position.getY()).GetType() != TileTypes.WALL)
			availableCoords.add(new Point(position.getX()+1,position.getY()));
		if(position.getY() > 0 && getTile((int)position.getX(), (int)position.getY() - 1).GetType() != TileTypes.WALL)
			availableCoords.add(new Point(position.getX(),position.getY() - 1));
		if(position.getY() < height - 1 && getTile((int)position.getX(), (int)position.getY() + 1).GetType() != TileTypes.WALL)
			availableCoords.add(new Point(position.getX(),position.getY() + 1));

		return availableCoords;
	}

	/**
	 * Sets a specific tile to a value.
	 * 
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param tile A tile.
	 */
	public void setTile(int x, int y, TileTypes tile) 
	{
		if(localConfig != null) localConfig.getWorldCanvas().setRendered(false); //THIS IS NEEDED TO FORCE RENDERING IN THE WORLD VIEW
		matrix[y][x] = tile.getValue();
		tileMap[y * width + x].SetType(tile);
	}
	
	/**
	 * Sets a specific tile object.
	 * 
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param tile A tile object.
	 */
	public void setTile(int x, int y, Tile tile) 
	{
		if(localConfig != null) localConfig.getWorldCanvas().setRendered(false); //THIS IS NEEDED TO FORCE RENDERING IN THE WORLD VIEW
		matrix[y][x] = tile.GetType().getValue();
		tileMap[y * width + x] = new Tile(tile);
	}
	
	/**
	 * Sets a tile based on a value
	 * 
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param tileValue value of a tiletype.
	 */
	public void setTile(int x, int y, int tileValue)
	{
		if(localConfig != null) localConfig.getWorldCanvas().setRendered(false); //THIS IS NEEDED TO FORCE RENDERING IN THE WORLD VIEW
		matrix[y][x] = tileValue;
		tileMap[y * width + x].SetType(TileTypes.toTileType(tileValue));
	}

	/**
	 * Gets the type of a specific tile.
	 * 
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @return A tile.
	 */
	public Tile getTile(int x, int y) {
		
		return tileMap[y * width + x];

//		return TileTypes.toTileType(matrix[y][x]);
	}

	/**
	 * Gets the type of a specific tile.
	 * 
	 * @param point The position.
	 * @return A tile.
	 */
	public Tile getTile(Point point){
		if (point == null) {
			return null;
		}

		return tileMap[point.getY() * width + point.getX()];

	}

	/**
	 * Returns the number of columns in a map. (WIDTH)
	 * 
	 * @return The number of columns.
	 */
	public int getColCount() {
		return width;
	}

	/**
	 * Returns the number of rows in a map. (HEIGHT)
	 * 
	 * @return The number of rows.
	 */
	public int getRowCount() {
		return height;
	}

	/**
	 *	Gets the number of traversable tiles. 
	 * 
	 * @return The number of traversable tiles.
	 */
	public int countTraversables() {
		return width * height - wallCount;
	}

	/**
	 * Returns the position of the entry door.
	 * 
	 * @return The entry door's position.
	 */
	public Point getEntrance() {
		return entrance;
	}

	/**
	 * Sets the position of the entry door.
	 * 
	 * @param door The position of the new door.
	 */
	public void setEntrance(Point door) {
		entrance = door;
	}

	/**
	 * Adds a door to the map.
	 * 
	 * @param door The position of a new door.
	 */
	public void addDoor(Point door) {
		doors.add(door);
		doorCount++;
	}

	/**
	 * Returns the positions of all doors.
	 * 
	 * @return The doors.
	 */
	public List<Point> getDoors() {
		return doors;
	}

	/**
	 * Gets the number of enemies on a map.
	 * 
	 * @return The number of enemies.
	 */
	public int getEnemyCount() {
		return enemies.size();
	}

	/**
	 * Calculates the enemy density by comparing the number of traversable
	 * tiles to the number of enemies.
	 * 
	 * @return The enemy density.
	 */
	public double calculateEnemyDensity() {
		return enemies.size() / countTraversables();
	}

	/**
	 * Gets the number of treasures in a map.
	 * 
	 * @return The number of treasures.
	 */
	public int getTreasureCount() {
		return treasures.size();
	}

	/**
	 * Calculates the treasure density by comparing the number of traversable
	 * tiles to the number of treasures.
	 * 
	 * @return The treasure density.
	 */
	public double calculateTreasureDensity() {
		return treasures.size() / countTraversables();
	}

	/***
	 * Returns the number of doors in a map
	 * @param entrance if you want to count or not the entrance
	 * @return
	 */
	public int getDoorCount(boolean entrance) 
	{
		return entrance == true ? doorCount : doorCount - 1;
	}
	

	/**
	 * Returns the number of wall tiles in a map.
	 * 
	 * @return The number of walls.
	 */
	public int getWallCount() {
		return wallCount;
	}

	/**
	 * Returns the number of non-wall tiles in a map.
	 * 
	 * @return The number of non-wall tiles.
	 */
	public int getNonWallTileCount()
    {
        return (width * height) - wallCount;
    }

	/**
	 * Get the ratio of enemy tiles to non-wall tiles.
	 * 
	 * @return The ratio of enemy tiles to non-wall tiles.
	 */
	public double getEnemyPercentage()
	{
		int allMap = getNonWallTileCount();
		return getEnemyCount()/(double)allMap;
	}

	/**
	 * Get the ratio of treasure tiles to non-wall tiles.
	 * 
	 * @return The ratio of treasure tiles to non-wall tiles.
	 */
	public double getTreasurePercentage()
	{
		int allMap = getNonWallTileCount();
		return getTreasureCount()/(double)allMap;
	}

	/**
	 * Returns the number of treasures.
	 * 
	 * @return The number of treasures.
	 */
	public List<Point> getTreasures() {
		return treasures;
	}

	/**
	 * Increases the number of failed path searches for treasures by one.
	 */
	public void addFailedPathToTreasures() {
		failedPathsToTreasures++;
	}

	/**
	 * Increases the number of failed path searches for enemies by one.
	 */
	public void addFailedPathToEnemies() {
		failedPathsToEnemies++;
	}

	/**
	 * Increases the number of failed path searches for doors by one.
	 */
	public void addFailedPathToDoors() {
		failedPathsToAnotherDoor++;
	}

	/**
	 * Gets the number of failed path searches for doors.
	 * 
	 * @return The number of failed path searches.
	 */
	public int getFailedPathsToAnotherDoor() {
		return failedPathsToAnotherDoor;
	}

	/**
	 * Gets the number of failed path searches for treasures.
	 * 
	 * @return The number of failed path searches.
	 */
	public int getFailedPathsToTreasures() {
		return failedPathsToTreasures;
	}

    /**
     * Gets the number of failed path searches for enemies.
     * 
     * @return The number of failed path searches.
     */
    public int getFailedPathsToEnemies() {
        return failedPathsToEnemies;
    }
    
    /**
     * Sets the safety value for a treasure.
     * 
     * @param treasure The position of the treasure.
     * @param safety The safety value.
     */
    public void setTreasureSafety(Point treasure, double safety) {
    	treasureSafety.put(treasure, safety);
    }
    
    /**
     * Gets the safety value for at treasure.
     * 
     * @param treasure The position of the treasure.
     * @return The safety value.
     */
    public double getTreasureSafety(Point treasure) {
    	return (double) treasureSafety.get(treasure);
    }
    
    /**
     * Gets the complete list of treasure safety values.
     * 
     * @return A dictionary containing all treasures and their safety values.
     */
    public Dictionary<Point, Double> getTreasureSafety() {
    	return treasureSafety;
    }
    
    /**
     * Gets the complete array of treasure safety values.
     * 
     * @return An array of Doubles containing all treasures safety values.
     */
    public Double[] getAllTreasureSafeties()
    {
    	return Collections.list(treasureSafety.elements()).stream().toArray(Double[]::new);
    }
    
    
    /**
     * Gets a list of all enemies in a map.
     * 
     * @return A list of enemies.
     */
    public List<Point> getEnemies() {
    	return enemies;
    }
    
    /**
     * Sets the safety value for the map's entry point.
     * 
     * @param safety A safety value.
     */
    public void setEntranceSafety(double safety) {
    	entranceSafety = safety;
    }
    
    /**
     * Sets the map's entrance "greed" (that is, how close is the nearest treasure?).
     * 
     * @param safety A safety value.
     */
    public void setEntranceGreed(double greed) {
    	entranceGreed = greed;
    }
    
    /**
     * Gets the safety value for the map's entry point.
     * 
     * @return The safety value.
     */
    public double getEntranceSafety() {
    	return entranceSafety;
    }
    
    /**
     * Gets the allocation matrix.
     * 
     * @return The allocation matrix.
     */
    public boolean[][] getAllocationMatrix() {
    	return allocated;
    }
    
    /**
     * Gets the pattern finder associated with this map.
     * 
     * @return A pattern finder.
     */
    public PatternFinder getPatternFinder() {
    	return finder;
    }
    
    public Tile[] getTileBasedMap()
    {
    	return tileMap;
    }
    
	/**
	 * Initialises a map.
	 * 
	 * @param tiles A list of tiles.
	 */
	private void initMapFromTypes(TileTypes[] tiles) {
		int tile = 0;
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				switch (tiles[tile]) {
				case WALL:
					wallCount++;
					break;
				case ENEMY:
					enemies.add(new Point(i, j));
					break;
				case TREASURE:
					treasures.add(new Point(i, j));
					break;
				default:
					break;
				}
				tileMap[tile] = new Tile(i , j, tiles[tile]);
				matrix[j][i] = tiles[tile++].getValue();

			}
		}
	}

	public GeneratorConfig getCalculatedConfig(){
		GeneratorConfig newConfig = new GeneratorConfig(config);
		//Make a new pattern finder in case the map has been manually edited since the patterns were found
		//finder = new PatternFinder(this);

		List<Enemy> enemies = new ArrayList<Enemy>();
		List<Treasure> treasures = new ArrayList<Treasure>();
		List<Corridor> corridors = new ArrayList<Corridor>();
		List<Connector> connectors = new ArrayList<Connector>();
		List<Chamber> chambers = new ArrayList<Chamber>();

		for (Pattern p : finder.findMicroPatterns()) {
			if (p instanceof Enemy) {
				enemies.add((Enemy) p);
			} else if (p instanceof Treasure) {
				treasures.add((Treasure) p);
			} else if (p instanceof Corridor) {
				corridors.add((Corridor) p);
			} else if (p instanceof Connector) {
				connectors.add((Connector) p);
			} else if (p instanceof Chamber) {
				chambers.add((Chamber) p);
			}
		}

		//TODO: Also take into account other patterns!!!

		//CORRIDOR LENGTH

		double rawCorridorArea = 0;

		for(Pattern p : corridors){
			rawCorridorArea += ((Polygon)p.getGeometry()).getArea();
		}

		int avgCorridorLength = (int)Math.ceil(rawCorridorArea/corridors.size());
		newConfig.setCorridorTargetLength(Math.max(3,avgCorridorLength));

		//ROOM AND CORRIDOR RATIOS

		double passableTiles = getNonWallTileCount();
		double rawRoomArea = 0.0;
		double totalSquareness = 0.0;

		for(Pattern p : chambers){
			rawRoomArea += ((Polygon)p.getGeometry()).getArea();
			totalSquareness += ((Chamber)p).getSquareness();
		}

		double roomProportion = rawRoomArea / passableTiles;

		newConfig.setRoomProportion(roomProportion);
		newConfig.setCorridorProportion(1.0 - roomProportion);

		//CHAMBER AREA

		if(chambers.size() > 0){
			int avgArea = (int)Math.ceil(rawRoomArea/chambers.size());
			newConfig.setChamberTargetArea(avgArea);
		}
		//CHAMBER SQUARENESS AND SIZE

		//		if(rooms.size() > 0){
		//			double avgSquareness = totalSquareness / rooms.size();
		//			newConfig.setChamberTargetSquareness(avgSquareness);
		//			newConfig.setChamberAreaCorrectness(1.0 - avgSquareness);
		//		}

		//ENEMIES
		double enemyProportion = (double)enemies.size()/passableTiles;
		newConfig.setEnemyQuantityRange(enemyProportion, enemyProportion);

		//TREASURE
		double treasureProportion = (double)treasures.size()/passableTiles;
		newConfig.setTreasureQuantityRange(treasureProportion, treasureProportion);

		return newConfig;
	}


	/**
	 * Exports this map as a 2D matrix of integers
	 * 
	 * @return A matrix of integers.
	 */
	public int[][] toMatrix() {
		return matrix;
	}

	/**
	 * Hacky load map implementation - TODO: should probably not be here
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static void LoadMap(File file) throws IOException{
		FileReader reader = new FileReader(file);
		String mapString = "";
		int lineCounter = 0;
		int charCounter = 0;
		while(reader.ready()){
			char c = (char) reader.read();
			mapString += c;
		}
		Room room = fromString(mapString);
		PatternFinder finder = room.getPatternFinder();
		MapContainer result = new MapContainer();
		result.setMap(room);
		result.setMicroPatterns(finder.findMicroPatterns());
		result.setMesoPatterns(finder.findMesoPatterns());
		result.setMacroPatterns(finder.findMacroPatterns());
		EventRouter.getInstance().postEvent(new MapLoaded(result));
	}

	public boolean isFeasible(){
		List<Node> visited = new ArrayList<Node>();
    	Queue<Node> queue = new LinkedList<Node>();
    	int treasure = 0;
    	int enemies = 0;
    	int doors = 0;
    	
    	Node root = new Node(0.0f, getEntrance(), null);
    	queue.add(root);
    	
    	while(!queue.isEmpty()){
    		Node current = queue.remove();
    		visited.add(current);
    		Tile currentTile = getTile(current.position);
    		
    		if(currentTile.GetType() == TileTypes.DOOR)
    			doors++;
    		else if (currentTile.GetType().isEnemy())
    			enemies++;
    		else if (currentTile.GetType().isTreasure())
    			treasure++;
    		
    		List<Point> children = getAvailableCoords(current.position);
            for(Point child : children)
            {
                if (visited.stream().filter(x->x.equals(child)).findFirst().isPresent() 
                		|| queue.stream().filter(x->x.equals(child)).findFirst().isPresent()) 
                	continue;

                //Create child node
                Node n = new Node(0.0f, child, current);
                queue.add(n);
            }
    	}
    	
    	//TODO: I think there is a problem here of not updating the correct values ----- maybe change back?
    	for(int i = treasure; i < getTreasureCount();i++)
    		addFailedPathToTreasures();
    	for(int i = doors; i < getDoorCount(false);i++)
    		addFailedPathToDoors();
    	for(int i = enemies; i < getEnemyCount();i++)
    		addFailedPathToEnemies();

    	return visited.size() == getNonWallTileCount() 
    			&& (treasure + doors + enemies == getTreasureCount() + getDoorCount(false) + getEnemyCount())
    			&& getTreasureCount() > 0 && getEnemyCount() > 0;
}

	//TODO: Double check maybe it can be useful to know this
	public boolean EveryRoomVisitable(){
		List<Node> visited = new ArrayList<Node>();
    	Queue<Node> queue = new LinkedList<Node>();
    	int treasure = 0;
    	int enemies = 0;
    	int doors = 0;
    	
    	Node root = new Node(0.0f, getEntrance(), null);
    	queue.add(root);
    	
    	while(!queue.isEmpty()){
    		Node current = queue.remove();
    		visited.add(current);
    		Tile currentTile = getTile(current.position);
    		
    		if(currentTile.GetType() == TileTypes.DOOR)
    			doors++;
    		else if (currentTile.GetType().isEnemy())
    			enemies++;
    		else if (currentTile.GetType().isTreasure())
    			treasure++;
    		
    		List<Point> children = getAvailableCoords(current.position);
            for(Point child : children)
            {
                if (visited.stream().filter(x->x.equals(child)).findFirst().isPresent() 
                		|| queue.stream().filter(x->x.equals(child)).findFirst().isPresent()) 
                	continue;

                //Create child node
                Node n = new Node(0.0f, child, current);
                queue.add(n);
            }
    	}

    	
    	return visited.size() == getNonWallTileCount();
	}
	
	public boolean isPointInBorder(Point p)
	{
		return borders.contains(Point.castToGeometry(p));
	}

	/////////////////////////////// LOADING MAPS AND STRING DEBUG //////////////////////////////////////////////

	//TODO: This will need to be REMADE how to load
	/**
	 * Builds a map from a string representing a rectangular room. Each row in
	 * the string, separated by a newline (\n), represents a row in the
	 * resulting map's matrix.
	 * 
	 * @param string A string
	 */
	public static Room fromString(String string) {
		String[] rows = string.split("[\\r\\n]+");
		// Had we just stuck to the specs, this wouldn't have been necessary...
		if (rows.length < 2) {
			rows = string.split("[\\n]+");
		}
		int rowCount = rows.length;
		int colCount = rows[0].length();
		TileTypes type = null;



		Room room = new Room(rowCount, colCount);
		try {
			room.setConfig(new GeneratorConfig());
		} catch (MissingConfigurationException e) {
			e.printStackTrace();
		}
//
//		Game.doors.clear();
//		
////		for (int j = 0; j < colCount; j++){
////			 for (int i = 0; i < rowCount; i++) {
//
//		Point p1 = null;
//		Point p2 = null;
//		Point p3 = null;
//		Point p4 = null;
//		
//		//TODO: CHANGE THIS, Is too hardcoded, why use this values when we have the columns and rows?
//		// South
//		Point south = new Point(11/2, 11-1);
//		// East
//		Point east = new Point(11-1, 11/2);
//		// North
//		Point north = new Point(11/2, 0);
//		// West
//		Point west = new Point(0, 11/2);
//
//		for (int i = 0; i < rowCount; i++) {
//			for (int j = 0; j < colCount; j++) {
//				type = TileTypes.toTileType(Integer.parseInt("" + rows[i].charAt(j), 16));
//				room.setTile(i, j, type);
//				switch (type) {
//				case WALL:
//					room.wallCount++;
//					break;
//				case ENEMY:
//					room.enemies.add(new Point(i, j));
//					break;
//				case TREASURE:
//					room.treasures.add(new Point(i, j));
//					break;
//				case DOOR:
//					room.addDoor(new Point(i, j));
//					Point temp = new Point(i, j);
//
//					if(temp.equals(north)) {
//						p1 = temp;
//					}
//					if(temp.equals(east)) {
//						p2 = temp;
//					}
//					if(temp.equals(south)) {
//						p3 = temp;
//					}
//					if(temp.equals(west)) {
//						p4 = temp;
//					}
//
//					Game.doors.add(new Point(i,j));
//					break;
//				case DOORENTER:
//					room.setEntrance(new Point(i, j));
//					Point temp2 = new Point(i, j);
//
//					if(temp2.equals(north)) {
//						p1 = temp2;
//					}
//					if(temp2.equals(east)) {
//						p2 = temp2;
//					}
//					if(temp2.equals(south)) {
//						p3 = temp2;
//					}
//					if(temp2.equals(west)) {
//						p4 = temp2;
//					}
//					room.addDoor(new Point(i, j));
//					Game.doors.add(0, new Point(i,j));
//					break;
//				default:
//				}
//
//			}
//
//		}
//		if (Game.doors.isEmpty()) {
//			room = new Room(11, 11, 0); //TODO: ??
//		}
//		else {
//			GeneratorConfig gc;
//			try {
//				gc = new GeneratorConfig();
//				Room newMap = new Room (gc, 11, 11, p1, p2, p3, p4);
//
//				for (int i = 0; i < rowCount; i++) {
//					for (int j = 0; j < colCount; j++) {
//						type = TileTypes.toTileType(Integer.parseInt("" + rows[i].charAt(j), 16));
//						room.setTile(i, j, type);
//						switch (type) {
//						case WALL:
//							newMap.wallCount++;
//							Point temp = new Point(i, j);
//							newMap.matrix[temp.getX()][temp.getY()] = 1;
//							break;
//						case ENEMY:
//							newMap.enemies.add(new Point(i, j));
//							Point temp2 = new Point(i, j);
//							newMap.matrix[temp2.getX()][temp2.getY()] = 3;
//							break;
//						case TREASURE:
//							newMap.treasures.add(new Point(i, j));
//							Point temp3 = new Point(i, j);
//							newMap.matrix[temp3.getX()][temp3.getY()] = 2;
//							break;
//						default:
//						}
//
//					}
//
//				}
//
//
//				room = newMap;
//			} catch (MissingConfigurationException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

		return room;
	}

	@Override
	public String toString() {
		StringBuilder map = new StringBuilder();

		for (int j = 0; j < height; j++){
			for (int i = 0; i < width; i++)  {
				map.append(Integer.toHexString(matrix[j][i]));
			}
			map.append("\n");
		}

		return map.toString();
	}

	
}