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

/*    Label label = new Label("LUYViewer, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
    ImageView imageView = new ImageView(new Image(LUYViewer.class.getResourceAsStream("/hellofx/openduke.png")));
    imageView.setFitHeight(200);
    imageView.setPreserveRatio(true);

    VBox root = new VBox(30, imageView, label);
    root.setAlignment(Pos.CENTER);
    Scene scene = new Scene(root, 640, 480);
    scene.getStylesheets().add(LUYViewer.class.getResource("style.css").toExternalForm());
    stage.setScene(scene);
    stage.show();*/
  }

  public static void main(String[] args) {
    launch(args);
  }

}