package gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import finder.PatternFinder;
import game.ApplicationConfig;
import game.Dungeon;
import game.Game;
import game.Room;
import game.MapContainer;
import game.TileTypes;
import generator.config.GeneratorConfig;
import gui.utils.MapRenderer;
import gui.views.LaunchViewController;
import gui.views.RoomViewController;
import gui.views.SuggestionsViewController;
import gui.views.WorldViewController;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
//import javafx.scene.control.Alert;
//import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import util.Point;
import util.config.MissingConfigurationException;
import util.eventrouting.EventRouter;
import util.eventrouting.Listener;
import util.eventrouting.PCGEvent;
import util.eventrouting.events.AlgorithmDone;
import util.eventrouting.events.ApplySuggestion;
import util.eventrouting.events.MapLoaded;
import util.eventrouting.events.RequestAppliedMap;
import util.eventrouting.events.RequestConnection;
import util.eventrouting.events.RequestEmptyRoom;
import util.eventrouting.events.RequestNewRoom;
import util.eventrouting.events.RequestNullRoom;
import util.eventrouting.events.RequestRedraw;
import util.eventrouting.events.RequestRoomView;
import util.eventrouting.events.RequestSuggestionsView;
import util.eventrouting.events.RequestWorldView;
import util.eventrouting.events.Start;
import util.eventrouting.events.StartWorld;
import util.eventrouting.events.StatusMessage;
import util.eventrouting.events.Stop;
import util.eventrouting.events.SuggestedMapsDone;
import util.eventrouting.events.SuggestedMapsLoading;
import util.eventrouting.events.UpdateMiniMap;

/*
 * @author Chelsi Nolasco, Malmö University
 * @author Axel Österman, Malmö University
 */

//Definetely I agree that this class can be the one "controlling" all the views and have in any moment the most updated version of
//the dungeon. But it is simply doing too much at the moment, It should "create" the dungeon but if another room wants to be incorporated
//It should be the dungeon adding such a room, Basically this should be an intermid, knowing which dungeon, which view, etc.
public class InteractiveGUIController implements Initializable, Listener {

	@FXML private AnchorPane mainPane;
	@FXML private MenuItem newItem;
	@FXML private MenuItem openItem;
	@FXML private MenuItem saveItem;
	@FXML private MenuItem saveAsItem;
	@FXML private MenuItem exportItem;
	@FXML private MenuItem prefsItem;
	@FXML private MenuItem exitItem;
	@FXML private MenuItem aboutItem;
	public boolean firstIsClicked = false;
	public boolean secondIsClicked = false;
	public boolean thirdIsClicked = false;
	public boolean fourthIsClicked = false;
	Stage stage = null;

	SuggestionsViewController suggestionsView = null;
	RoomViewController roomView = null;
	WorldViewController worldView = null;
	LaunchViewController launchView = null;
	EventHandler<MouseEvent> mouseEventHandler = null;

	final static Logger logger = LoggerFactory.getLogger(InteractiveGUIController.class);
	private static EventRouter router = EventRouter.getInstance();
	private ApplicationConfig config;

	private MapContainer currentQuadMap = new MapContainer();
	private MapContainer quadMap1 = new MapContainer();
	private MapContainer quadMap2 = new MapContainer();
	private MapContainer quadMap3 = new MapContainer();
	private MapContainer quadMap4 = new MapContainer();
	private MapContainer tempLargeContainer = new MapContainer();

	// VARIABLE FOR PICKING THE SIZE OF THE WORLD MAP (3 = 3x3 map)
	private int size = 3;
	private MapContainer[][] worldMapMatrix = new MapContainer[size][size];
	private int row = 0;
	private int col = 0;

	private Node oldNode;
	
	//NEW
	private Dungeon dungeonMap = new Dungeon();
	

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		try {
			config = ApplicationConfig.getInstance();
		} catch (MissingConfigurationException e) {
			logger.error("Couldn't read config file.");
		}

		router.registerListener(this, new RequestConnection(null, -1, null, null, null, null));
		router.registerListener(this, new RequestNewRoom(null, -1, -1, -1));
		router.registerListener(this, new StatusMessage(null));
		router.registerListener(this, new AlgorithmDone(null));
		router.registerListener(this, new RequestRedraw());
		router.registerListener(this, new RequestRoomView(null, 0, 0, null));
		router.registerListener(this, new MapLoaded(null));
		router.registerListener(this, new RequestWorldView());
		router.registerListener(this, new RequestEmptyRoom(null, 0, 0, null));
		router.registerListener(this, new RequestSuggestionsView(null, 0, 0, null, 0));
		router.registerListener(this, new Stop());
		router.registerListener(this, new SuggestedMapsDone());
		router.registerListener(this, new SuggestedMapsLoading());
		router.registerListener(this, new RequestNullRoom(null, 0, 0, null));
		router.registerListener(this, new StartWorld(0));
		router.registerListener(this, new RequestAppliedMap(null, 0, 0));

		suggestionsView = new SuggestionsViewController();
		roomView = new RoomViewController();
		worldView = new WorldViewController();
		launchView = new LaunchViewController();

		mainPane.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
			if (newScene != null) {
				stage = (Stage) newScene.getWindow();

			}

		});

		initLaunchView();


	}


	@Override
	public synchronized void ping(PCGEvent e) 
	{
		if(e instanceof RequestConnection)
		{
			RequestConnection rC = (RequestConnection)e;
			//TODO: Here you should check for which dungeon
			dungeonMap.addConnection(rC.getFromRoom(), rC.getToRoom(), rC.getFromPos(), rC.getToPos());
			worldView.initWorldMap(dungeonMap);
		}
		else if(e instanceof RequestNewRoom)
		{
			RequestNewRoom rNR = (RequestNewRoom)e;
			//TODO: Here you should check for which dungeon
			dungeonMap.addRoom(rNR.getHeight(), rNR.getWidth());
			worldView.initWorldMap(dungeonMap);
		}
		else if (e instanceof RequestRoomView) {
			
			//Yeah dont care about matrix
			initRoomView((MapContainer) e.getPayload());
			
//			if (((RequestRoomView) e).getMatrix() != null) {
//				worldMapMatrix = ((RequestRoomView) e).getMatrix();
//				row = ((RequestRoomView) e).getRow();
//				col = ((RequestRoomView) e).getCol();
//				MapContainer container = (MapContainer) e.getPayload();
//				initRoomView(container);
//			}
//			else if (!worldMapMatrix[((RequestRoomView) e).getRow()][((RequestRoomView) e).getCol()].getMap().getNull()) { 
//				row = ((RequestRoomView) e).getRow();
//				col = ((RequestRoomView) e).getCol();
//				MapContainer container = worldMapMatrix[row][col];
//				initRoomView(container);
//
//			}

		} else if (e instanceof RequestAppliedMap) {
			Room room = (Room) ((RequestAppliedMap) e).getPayload();
			MapContainer mapCont = new MapContainer();
			row = ((RequestAppliedMap) e).getRow();
			col = ((RequestAppliedMap) e).getCol(); 
			mapCont.setMap(room);
			worldMapMatrix[row][col] = mapCont;
			initRoomView(mapCont);
		} else if (e instanceof RequestSuggestionsView) {
			worldMapMatrix = ((RequestSuggestionsView) e).getMatrix();
			row = ((RequestSuggestionsView) e).getRow();
			col = ((RequestSuggestionsView) e).getCol();
			MapContainer container = (MapContainer) e.getPayload();
			initSuggestionsView();
		} else if (e instanceof RequestWorldView) {

			backToWorldView();
//			worldView.getStartEmptyBtn().setDisable(true);
//			worldView.getRoomNullBtn().setDisable(true); //TODO: HERE
//			worldView.getSuggestionsBtn().setDisable(true);


		} else if (e instanceof RequestEmptyRoom) {
			worldMapMatrix = ((RequestEmptyRoom) e).getMatrix();
			row = ((RequestEmptyRoom) e).getRow();
			col = ((RequestEmptyRoom) e).getCol();
			MapContainer container = (MapContainer) e.getPayload();
			initRoomView(container);

		} else if (e instanceof StartWorld) {
			size = ((StartWorld) e).getSize();
			if (size != 0) {
				initWorldView();
			}
//			worldView.getStartEmptyBtn().setDisable(true);
//			worldView.getRoomNullBtn().setDisable(true);
//			worldView.getSuggestionsBtn().setDisable(true);
		}
		else if (e instanceof SuggestedMapsDone) {
			roomView.getUpdateMiniMapBtn().setDisable(false);
			roomView.getWorldGridBtn().setDisable(false);
			roomView.getGenSuggestionsBtn().setDisable(false);			
		} else if (e instanceof SuggestedMapsLoading) {

			firstIsClicked = false;
			secondIsClicked = false;
			thirdIsClicked = false;
			fourthIsClicked = false;

			roomView.getMap(0).setStyle("-fx-background-color:#2c2f33;");
			roomView.getMap(1).setStyle("-fx-background-color:#2c2f33;");
			roomView.getMap(2).setStyle("-fx-background-color:#2c2f33;");
			roomView.getMap(3).setStyle("-fx-background-color:#2c2f33;");
			roomView.clearStats();
			roomView.getUpdateMiniMapBtn().setDisable(true);
			roomView.getWorldGridBtn().setDisable(true);
			roomView.getGenSuggestionsBtn().setDisable(true);
			roomView.getAppSuggestionsBtn().setDisable(true);

			roomView.getAppSuggestionsBtn().setDisable(true);

		}
		 else if (e instanceof RequestNullRoom) {
			worldMapMatrix = ((RequestNullRoom) e).getMatrix();
			row = ((RequestNullRoom) e).getRow();
			col = ((RequestNullRoom) e).getCol();
			MapContainer container = (MapContainer) e.getPayload();

			if (!worldMapMatrix[row][col].getMap().getNull()) {
				Room nullMap = new Room(Game.sizeWidth, Game.sizeHeight, 0);
				MapContainer nullCont = new MapContainer();
				nullCont.setMap(nullMap);
				worldMapMatrix[row][col] = nullCont;
			}
			else {
				
				//TODO: This really needs to change
				
				// South
				Point south = new Point(11/2, 11-1);
				// East
				Point east = new Point(11-1, 11/2);
				// North
				Point north = new Point(11/2, 0);
				// West
				Point west = new Point(0, 11/2);
				GeneratorConfig gc = null;
				try {
					gc = new GeneratorConfig();

				} catch (MissingConfigurationException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
				Room tempMap = null;
				// 1
				if (row == 0 && col == 0) {
					tempMap = new Room(gc, 11, 11, null, east, south, null);
				}
				// 3
				if (row == 0 && col == (size - 1)) {
					tempMap = new Room(gc, 11, 11, null, null, south, west);
				}
				// 7
				if (row == (size - 1) && col == 0) {
					tempMap = new Room(gc, 11, 11, north, east, null, null);
				}
				// 9
				if (row == (size - 1) && col == (size - 1)) {
					tempMap = new Room(gc, 11, 11, north, null, null, west);
				}
				// top
				if (row == 0 && col != (size - 1) && col != 0) {
					tempMap = new Room(gc, 11, 11, null, east, south, west);
				}
				// left
				if (row != 0 && col == 0 && row != (size - 1)) {
					tempMap = new Room(gc, 11, 11, north, east, south, null);
				}
				// right
				if (row != 0 && row != (size - 1) && col == (size - 1)) {
					tempMap = new Room(gc, 11, 11, north, null, south, west);
				}
				// bottom
				if (col != 0 && col != (size - 1) && row == (size - 1)) {
					tempMap = new Room(gc, 11, 11, north, east, null, west);
				}
				// other
				else if (col != 0 && col != (size - 1) && row != 0 && row != (size - 1)) {
					tempMap = new Room(gc, 11, 11, north, east, south, west);
				}
				MapContainer revertCont = new MapContainer();
				revertCont.setMap(tempMap);
				worldMapMatrix[row][col] = revertCont;
			}
			evaluateNullChange();
			backToWorldView();
		}

	}

	/*
	 * Event stuff
	 */

	public void startNewFlow() {
		initLaunchView();
	}

	public void exitApplication() {
		// TODO: Maybe be a bit more graceful than this...

		Platform.exit();
		System.exit(0);
	}

	public void openMap() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Map");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Map Files", "*.map"),
				new ExtensionFilter("All Files", "*.*"));
		File selectedFile = fileChooser.showOpenDialog(stage);
		if (selectedFile != null) {
			try {

				FileReader reader = new FileReader(selectedFile);
				String mapString = "";
				while(reader.ready()){
					char c = (char) reader.read();

					mapString += c;
				}
				worldMapMatrix = updateLargeMap(mapString);

				router.postEvent(new RequestWorldView());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void saveMap() {
		roomView.updateLargeMap(tempLargeContainer.getMap());
		DateTimeFormatter format =
				DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-s-n");
		String name = "map_" +
				LocalDateTime.now().format(format) + ".map";

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Map");
		fileChooser.setInitialFileName(name);
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Map Files", "*.map"),
				new ExtensionFilter("All Files", "*.*"));
		File selectedFile = fileChooser.showSaveDialog(stage);
		if (selectedFile != null) {
			logger.debug("Writing map to " + selectedFile.getPath());
			try {
				Files.write(selectedFile.toPath(), matrixToString().getBytes());
			} catch (IOException e) {
				logger.error("Couldn't write map to " + selectedFile +
						":\n" + e.getMessage());
			}
		}
	}

	public void exportImage() {
		DateTimeFormatter format =
				DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-s-n");
		String name = "renderedmap_" +
				LocalDateTime.now().format(format) + ".png";

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Map");
		fileChooser.setInitialFileName(name);
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("PNG Files", "*.png"),
				new ExtensionFilter("All Files", "*.*"));
		File selectedFile = fileChooser.showSaveDialog(stage);
		if (selectedFile != null && roomView.getCurrentMap() != null) {
			logger.debug("Exporting map to " + selectedFile.getPath());
			BufferedImage image = SwingFXUtils.fromFXImage(roomView.getRenderedMap(), null);

			try {
				ImageIO.write(image, "png", selectedFile);
			} catch (IOException e1) {
				logger.error("Couldn't export map to " + selectedFile +
						":\n" + e1.getMessage());
			}
		}
	}

	public void openPreferences() {
		System.out.println("Preferences...");
	}

	public void generateNewMap() {
		System.out.println("Generate map");
	}

	private void updateConfigBasedOnMap(Room room) {
		config.setDimensionM(room.getColCount());
		config.setDimensionN(room.getRowCount());
	}

	/*
	 * Initialisation methods
	 */

	/**
	 * Initialises the suggestions view.
	 */
	private void initSuggestionsView() {
		mainPane.getChildren().clear();

		AnchorPane.setTopAnchor(suggestionsView, 0.0);
		AnchorPane.setRightAnchor(suggestionsView, 0.0);
		AnchorPane.setBottomAnchor(suggestionsView, 0.0);
		AnchorPane.setLeftAnchor(suggestionsView, 0.0);
		mainPane.getChildren().add(suggestionsView);

		saveItem.setDisable(true);
		saveAsItem.setDisable(true);
		exportItem.setDisable(true);


		suggestionsView.setActive(true);
		roomView.setActive(false);
		worldView.setActive(false);
		launchView.setActive(false);


		suggestionsView.initialise();
	}

	/**
	 * Initialises the world view.
	 */

	private void initWorldView() {
		mainPane.getChildren().clear();
		AnchorPane.setTopAnchor(worldView, 0.0);
		AnchorPane.setRightAnchor(worldView, 0.0);
		AnchorPane.setBottomAnchor(worldView, 0.0);
		AnchorPane.setLeftAnchor(worldView, 0.0);
		mainPane.getChildren().add(worldView);

		worldView.initWorldMap(initMatrix());

		saveItem.setDisable(false);
		saveAsItem.setDisable(false);
		exportItem.setDisable(false);

		suggestionsView.setActive(false);
		roomView.setActive(false);
		worldView.setActive(true);
		launchView.setActive(false);

	}

	private void initLaunchView() {
		mainPane.getChildren().clear();
		AnchorPane.setTopAnchor(launchView, 0.0);
		AnchorPane.setRightAnchor(launchView, 0.0);
		AnchorPane.setBottomAnchor(launchView, 0.0);
		AnchorPane.setLeftAnchor(launchView, 0.0);
		mainPane.getChildren().add(launchView);

		launchView.initGui();
		suggestionsView.setActive(false);
		roomView.setActive(false);
		worldView.setActive(false);
		launchView.setActive(true);

	}


	private void backToWorldView() {
		mainPane.getChildren().clear();
		AnchorPane.setTopAnchor(worldView, 0.0);
		AnchorPane.setRightAnchor(worldView, 0.0);
		AnchorPane.setBottomAnchor(worldView, 0.0);
		AnchorPane.setLeftAnchor(worldView, 0.0);
		mainPane.getChildren().add(worldView);

		worldView.initWorldMap(dungeonMap);

		saveItem.setDisable(false);
		saveAsItem.setDisable(false);
		exportItem.setDisable(false);

		suggestionsView.setActive(false);
		roomView.setActive(false);
		worldView.setActive(true);
		launchView.setActive(false);

	}

	/**
	 * Initialises the edit view and starts a new generation run.
	 */
	private void initRoomView(MapContainer map) {
		mainPane.getChildren().clear();
		AnchorPane.setTopAnchor(roomView, 0.0);
		AnchorPane.setRightAnchor(roomView, 0.0);
		AnchorPane.setBottomAnchor(roomView, 0.0);
		AnchorPane.setLeftAnchor(roomView, 0.0);
		mainPane.getChildren().add(roomView);
		roomView.updateLargeMap(map.getMap());
		roomView.updateMap(map.getMap());	
		setCurrentQuadMap(map);

		
		
		roomView.initializeView(map.getMap());
		roomView.roomMouseEvents();
		
		//TODO: Crazyness to create mini map based on the dungeon...
		//It would need to have different dimensions for the room view and for the world view
		
//		
//		roomView.minimap.getChildren().clear();
//		roomView.minimap.getChildren().add(dungeonMap.dPane);
//		dungeonMap.dPane.setPrefSize(roomView.minimap.getPrefWidth(), roomView.minimap.getPrefHeight());
		
		//TODO: HERE IS WHAT YOU NEED TO FIX!! PLEASE :/
//		roomView.updateMiniMap(worldMapMatrix);
//		roomView.updatePosition(row, col);

//		roomView.generateNewMaps();

		saveItem.setDisable(false);
		saveAsItem.setDisable(false);
		exportItem.setDisable(false);

		worldView.setActive(false);
		roomView.setActive(true);		
		launchView.setActive(false);
		suggestionsView.setActive(false);


	}

	/*
	 * Mouse methods for controllers
	 */

	

	//TODO: this method...
	private void evaluateNullChange() {
		// South
		Point south = new Point(11/2, 11-1);
		// East
		Point east = new Point(11-1, 11/2);
		// North
		Point north = new Point(11/2, 0);
		// West
		Point west = new Point(0, 11/2);
		for (int rows = 0; rows < size; rows++) {
			for (int cols = 0; cols < size; cols++) {
				if (!worldMapMatrix[rows][cols].getMap().getNull()) {
					if (rows != 0) {
						//north
						if (worldMapMatrix[rows - 1][cols].getMap().getNull() && (worldMapMatrix[rows][cols].getMap().matrix[north.getY()][north.getX()] == 5 || 
								worldMapMatrix[rows][cols].getMap().matrix[north.getY()][north.getX()] == 4)) {
							worldMapMatrix[rows][cols].getMap().setTile(north.getX(), north.getY(), 0);
//							worldMapMatrix[rows][cols].getMap().matrix[north.getY()][north.getX()] = 0;
							worldMapMatrix[rows][cols].getMap().setNumberOfDoors(worldMapMatrix[rows][cols].getMap().getNumberOfDoors() - 1);
							worldMapMatrix[rows][cols].getMap().setNorth(false);
						}
						else if (!worldMapMatrix[rows - 1][cols].getMap().getNull() && !(worldMapMatrix[rows][cols].getMap().matrix[north.getY()][north.getX()] == 5 || 
								worldMapMatrix[rows][cols].getMap().matrix[north.getY()][north.getX()] == 4)) {
							worldMapMatrix[rows][cols].getMap().setTile(north.getX(), north.getY(), 4);
//							worldMapMatrix[rows][cols].getMap().matrix[north.getY()][north.getX()] = 4;
							worldMapMatrix[rows][cols].getMap().setNumberOfDoors(worldMapMatrix[rows][cols].getMap().getNumberOfDoors() + 1);
							worldMapMatrix[rows][cols].getMap().setNorth(true);
						}
					}
					if (cols != (size - 1)) {
						//east
						if (worldMapMatrix[rows][cols + 1].getMap().getNull() && (worldMapMatrix[rows][cols].getMap().matrix[east.getY()][east.getX()] == 5 || 
								worldMapMatrix[rows][cols].getMap().matrix[east.getY()][east.getX()] == 4)) {
							worldMapMatrix[rows][cols].getMap().setTile(east.getX(), east.getY(), 0);
//							worldMapMatrix[rows][cols].getMap().matrix[east.getY()][east.getX()] = 0;
							worldMapMatrix[rows][cols].getMap().setNumberOfDoors(worldMapMatrix[rows][cols].getMap().getNumberOfDoors() - 1);
							worldMapMatrix[rows][cols].getMap().setEast(false);
						}
						else if (!worldMapMatrix[rows][cols + 1].getMap().getNull() && !(worldMapMatrix[rows][cols].getMap().matrix[east.getY()][east.getX()] == 5 || 
								worldMapMatrix[rows][cols].getMap().matrix[east.getY()][east.getX()] == 4)) {
							worldMapMatrix[rows][cols].getMap().setTile(east.getX(), east.getY(), 4);
//							worldMapMatrix[rows][cols].getMap().matrix[east.getY()][east.getX()] = 4;
							worldMapMatrix[rows][cols].getMap().setNumberOfDoors(worldMapMatrix[rows][cols].getMap().getNumberOfDoors() + 1);
							worldMapMatrix[rows][cols].getMap().setEast(true);
						}

					}
					if (rows != (size - 1)) {
						//south
						if (worldMapMatrix[rows + 1][cols].getMap().getNull() && (worldMapMatrix[rows][cols].getMap().matrix[south.getY()][south.getX()] == 5 || 
								worldMapMatrix[rows][cols].getMap().matrix[south.getY()][south.getX()] == 4)) {
							worldMapMatrix[rows][cols].getMap().setTile(south.getX(), south.getY(), 0);
//							worldMapMatrix[rows][cols].getMap().matrix[south.getY()][south.getX()] = 0;
							worldMapMatrix[rows][cols].getMap().setNumberOfDoors(worldMapMatrix[rows][cols].getMap().getNumberOfDoors() - 1);
							worldMapMatrix[rows][cols].getMap().setSouth(false);
						}
						else if (!worldMapMatrix[rows + 1][cols].getMap().getNull() && !(worldMapMatrix[rows][cols].getMap().matrix[south.getY()][south.getX()] == 5 || 
								worldMapMatrix[rows][cols].getMap().matrix[south.getY()][south.getX()] == 4)) {
							worldMapMatrix[rows][cols].getMap().setTile(south.getX(), south.getY(), 4);
//							worldMapMatrix[rows][cols].getMap().matrix[south.getY()][south.getX()] = 4;
							worldMapMatrix[rows][cols].getMap().setNumberOfDoors(worldMapMatrix[rows][cols].getMap().getNumberOfDoors() + 1);
							worldMapMatrix[rows][cols].getMap().setSouth(true);
						}

					}
					if (cols != 0) {
						//west
						if (worldMapMatrix[rows][cols - 1].getMap().getNull() && (worldMapMatrix[rows][cols].getMap().matrix[west.getY()][west.getX()] == 5 || 
								worldMapMatrix[rows][cols].getMap().matrix[west.getY()][west.getX()] == 4)) {
							worldMapMatrix[rows][cols].getMap().setTile(west.getX(), west.getY(), 0);
//							worldMapMatrix[rows][cols].getMap().matrix[west.getY()][west.getX()] = 0;
							worldMapMatrix[rows][cols].getMap().setNumberOfDoors(worldMapMatrix[rows][cols].getMap().getNumberOfDoors() - 1);
							worldMapMatrix[rows][cols].getMap().setWest(false);
						}
						else if (!worldMapMatrix[rows][cols - 1].getMap().getNull() && !(worldMapMatrix[rows][cols].getMap().matrix[west.getY()][west.getX()] == 5 || 
								worldMapMatrix[rows][cols].getMap().matrix[west.getY()][west.getX()] == 4)) {
							worldMapMatrix[rows][cols].getMap().setTile(west.getX(), west.getY(), 4);
//							worldMapMatrix[rows][cols].getMap().matrix[west.getY()][west.getX()] = 4;
							worldMapMatrix[rows][cols].getMap().setNumberOfDoors(worldMapMatrix[rows][cols].getMap().getNumberOfDoors() + 1);
							worldMapMatrix[rows][cols].getMap().setWest(true);
						}

					}
					if (worldMapMatrix[rows][cols].getMap().getNumberOfDoors() == 0) {
						Room nullMap = new Room(11, 11, 0);
						MapContainer nullCont = new MapContainer();
						nullCont.setMap(nullMap);
						worldMapMatrix[rows][cols] = nullCont;
					}
					else
					{
						worldMapMatrix[rows][cols].getMap().RecalculateEntrance();
					}
				}
			}
		}

	}

	//TODO: This part has a few issues, like set numbers (11) and how the map is created
	private Dungeon initMatrix() 
	{
		int width = Game.sizeWidth;
		int height = Game.sizeHeight;

		GeneratorConfig gc = null;
		try {
			gc = new GeneratorConfig();

		} catch (MissingConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		dungeonMap = new Dungeon(gc, 1, width, height);
		
		return dungeonMap;
	}

	private void createWorldMatrix() {
		//START OF MATRIX STUFF		
		//fill matrix
		for (MapContainer[] outer : worldMapMatrix) {
			for (int i = 0; i < outer.length; i++) {
				outer[i] = quadMap1;
			}
		}								
	}


	private String matrixToString() {
		//create large string
		String largeString = "";
		int j = 1;

		for (MapContainer[] outer : worldMapMatrix) {

			for (int k = 0; k < outer[0].getMap().toString().length(); k++) {

				if (outer[0].getMap().toString().charAt(k) != '\n') {
					largeString += outer[0].getMap().toString().charAt(k);

				}
				if (outer[0].getMap().toString().charAt(k) == '\n') {
					while (j < size) {

						for (int i = (k - 11); i < k; i++) {
							largeString += outer[j].getMap().toString().charAt(i);

						}
						j++;
					}
					j = 1;
					largeString += outer[0].getMap().toString().charAt(k);
				}

			}

		}
		return largeString;
	}


	private MapContainer[][] updateLargeMap(String loadedMap) {


		String largeString = loadedMap;
		//fill matrix from string
		int charNbr = 0;
		while (largeString.charAt(charNbr) != '\n') {
			charNbr++;
		}
		int actualCharNbr = charNbr / 11;
		MapContainer[][] worldMapMatrix2 = new MapContainer[actualCharNbr][actualCharNbr];
		String[] stringArray = new String[actualCharNbr];

		for (int s = 0; s < stringArray.length; s++) {
			stringArray[s] = "";
		}

		int p = 0;
		int charAmount = 0;
		int newLineCount = 0;
		int q = 0;

		while (q < actualCharNbr) {

			for (int i = 0; i < largeString.length(); i++) {

				if (largeString.charAt(i) == '\n') {
					newLineCount++;
					for (int s = 0; s < stringArray.length; s++) {
						stringArray[s] += largeString.charAt(i);
					}

					if ((newLineCount%11) == 0) {

						for (int s = 0; s < stringArray.length; s++) {
							MapContainer helpContainer = new MapContainer();
							helpContainer.setMap(Room.fromString(stringArray[s]));
							
							
							int counter = 0;
							for (int j = 0; j < stringArray[s].length(); j++) {
								if (stringArray[s].charAt(j) == '4' || stringArray[s].charAt(j) == '5') {
									counter++;
								}
								
							}
							helpContainer.getMap().setNumberOfDoors(counter);

							worldMapMatrix2[q][s] = helpContainer;
							stringArray[s] = "";

						}
						q++;

					}

					p = 0;
					charAmount = 0;
				}

				if ((charAmount%11) == 0 && charAmount != 0) {
					p++;
				}
				if (largeString.charAt(i) != '\n') {
					charAmount++;

					stringArray[p] += largeString.charAt(i);
				}

			}
		}
		
		
		
		for (MapContainer[] mc : worldMapMatrix2) {
			for (MapContainer mc2 : mc) {
				if (mc2.getMap().getNumberOfDoors() == 0) {
					mc2.getMap().setNull();
				}
			}
		}
		
		
		size = worldMapMatrix2.length;

		return worldMapMatrix2;
	}

	private MapContainer getCurrentQuadMap() {
		return currentQuadMap;
	}

	private void setCurrentQuadMap(MapContainer currentQuadMap) {
		this.currentQuadMap = currentQuadMap;
	}



}
