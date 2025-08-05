package net.liwuest.luyviewer;

import javafx.application.Application;
import javafx.stage.Stage;


public class LUYViewer extends Application {

  public void start(Stage stage) {
    String javaVersion = System.getProperty("java.version");
    String javafxVersion = System.getProperty("javafx.version");

    try {
      new JFXMainWindow(stage);
      stage.show();
    } catch (Exception Ex) { Ex.printStackTrace(); }
  }

  public static void main(String[] args) {
    launch(args);
  }

}