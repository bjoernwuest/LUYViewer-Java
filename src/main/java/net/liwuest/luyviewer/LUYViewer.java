package net.liwuest.luyviewer;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.*;


public class LUYViewer extends Application {
    public static final Logger LOGGER = Logger.getLogger("LUYViewerLogger");
    static {
        try {
            FileHandler fh = new FileHandler("LUYViewer.log", true);
            fh.setFormatter(new SimpleFormatter());
            fh.setLevel(Level.ALL);
            LOGGER.addHandler(fh);
            LOGGER.setLevel(Level.ALL);
            LOGGER.setUseParentHandlers(false);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");

        try {
            new JFXMainWindow(stage);
            stage.show();
        } catch (Exception Ex) { LOGGER.log(Level.SEVERE, "Exception in start", Ex); }
    }

    public static void main(String[] args) {
        LOGGER.info("Starting LUYViewer - JavaFX version");
        launch(args);
    }

}