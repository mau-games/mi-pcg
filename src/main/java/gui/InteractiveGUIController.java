package gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import game.ApplicationConfig;
import game.Map;
import game.MapContainer;
import gui.views.EditViewController;
import gui.views.StartViewController;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import util.config.MissingConfigurationException;
import util.eventrouting.EventRouter;
import util.eventrouting.Listener;
import util.eventrouting.PCGEvent;
import util.eventrouting.events.AlgorithmDone;
import util.eventrouting.events.MapLoaded;
import util.eventrouting.events.RequestRedraw;
import util.eventrouting.events.RequestViewSwitch;
import util.eventrouting.events.Start;
import util.eventrouting.events.StatusMessage;
import util.eventrouting.events.Stop;

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
	
	Stage stage = null;
	
	StartViewController startView = null;
	EditViewController editView = null;
	EventHandler<MouseEvent> mouseEventHandler = null;
	
	final static Logger logger = LoggerFactory.getLogger(InteractiveGUIController.class);
	private static EventRouter router = EventRouter.getInstance();
	private ApplicationConfig config;

	@Override
	public synchronized void ping(PCGEvent e) {
		if (e instanceof RequestViewSwitch) {
			if (e.getPayload() == null) {
				router.postEvent(new Stop());
				initStartView();
			} else {
				MapContainer container = (MapContainer) e.getPayload();
				initEditView(container);
			}
		} else if (e instanceof MapLoaded) {
			MapContainer container = (MapContainer) e.getPayload();
			updateConfigBasedOnMap(container.getMap());
			initEditView(container);
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		try {
			config = ApplicationConfig.getInstance();
		} catch (MissingConfigurationException e) {
			logger.error("Couldn't read config file.");
		}
		
		router.registerListener(this, new StatusMessage(null));
		router.registerListener(this, new AlgorithmDone(null));
		router.registerListener(this, new RequestRedraw());
		router.registerListener(this, new RequestViewSwitch(null));
		router.registerListener(this, new MapLoaded(null));
		
		startView = new StartViewController();
		editView = new EditViewController();
		
		mainPane.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
			if (newScene != null) {
				stage = (Stage) newScene.getWindow();
			}
		});
		
		initStartView();
	}
	
	/*
	 * Event stuff
	 */
	
	public void startNewFlow() {
		router.postEvent(new Start(6));
		initStartView();
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
				Map.LoadMap(selectedFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
	}
	
	public void saveMap() {
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
				Files.write(selectedFile.toPath(), editView.getMap().getMap().toString().getBytes());
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
		if (selectedFile != null && editView.getCurrentMap() != null) {
			logger.debug("Exporting map to " + selectedFile.getPath());
			BufferedImage image = SwingFXUtils.fromFXImage(editView.getRenderedMap(), null);

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
	
	public void openAboutApplication() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About Eddy");
		alert.setHeaderText(null);
		alert.setContentText("Written by:\n"
				+ "Alexander Baldwin <alexander.baldwin@mah.se>\n"
				+ "JohanHolmberg <johan.holmberg@mah.se>\n\n"
				+ "Thanks to José, Steve and Carl Mangus\n"
				+ "for your input!");
		alert.showAndWait();
	}
	
	public void generateNewMap() {
		System.out.println("Generate map");
	}
	
	private void updateConfigBasedOnMap(Map map) {
		config.setDimensionM(map.getColCount());
		config.setDimensionN(map.getRowCount());
	}
	
	/*
	 * Initialisation methods
	 */
	
	/**
	 * Initialises the start view.
	 */
	private void initStartView() {
		mainPane.getChildren().clear();
		
		AnchorPane.setTopAnchor(startView, 0.0);
		AnchorPane.setRightAnchor(startView, 0.0);
		AnchorPane.setBottomAnchor(startView, 0.0);
		AnchorPane.setLeftAnchor(startView, 0.0);
		mainPane.getChildren().add(startView);
		
		saveItem.setDisable(true);
		saveAsItem.setDisable(true);
		exportItem.setDisable(true);

		startView.setActive(true);
		editView.setActive(false);

		startView.initialise();
	}
	
	/**
	 * Initialises the edit view and starts a new generation run.
	 */
	private void initEditView(MapContainer map) {
		mainPane.getChildren().clear();
		AnchorPane.setTopAnchor(editView, 0.0);
		AnchorPane.setRightAnchor(editView, 0.0);
		AnchorPane.setBottomAnchor(editView, 0.0);
		AnchorPane.setLeftAnchor(editView, 0.0);
		mainPane.getChildren().add(editView);
		
		editView.updateMap(map.getMap());
		editView.generateNewMaps();
		
		saveItem.setDisable(false);
		saveAsItem.setDisable(false);
		exportItem.setDisable(false);

		startView.setActive(false);
		editView.setActive(true);
	}
}
