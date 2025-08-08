package net.liwuest.luyviewer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import net.liwuest.luyviewer.model.CDatamodel;
import net.liwuest.luyviewer.model.CFilteredAndSortedDatamodel;
import net.liwuest.luyviewer.model.CLuyFileService;
import net.liwuest.luyviewer.util.CTranslations;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Main window of the LUYViewer application.
 */
public class JFXMainWindow {
    private final static record FileListEntry(String timestamp) {
        @Override
        public String toString() {
            try {
                long timestampLong = Long.parseLong(timestamp);
                // If timestamp appears to be in seconds (less than a reasonable millisecond value) convert to milliseconds
                if (timestampLong < 10000000000L) timestampLong *= 1000;
                Instant instant = Instant.ofEpochMilli(timestampLong);
                LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (NumberFormatException | java.time.DateTimeException e) { return "Invalid timestamp: " + timestamp; }
        }
    }

    private final ComboBox<FileListEntry> fileComboBox = new ComboBox<>();
//    private final ViewerPanel viewerPanel = new ViewerPanel();
//    private final SwingNode swingNode = new SwingNode();
    private final Label statusBar = new Label(CTranslations.INSTANCE.Label_Ready);
    private final Map<String, SoftReference<CDatamodel>> loadedModels = new TreeMap<>();
    private final Stage stage;

    public JFXMainWindow(Stage stage) throws IOException {
        this.stage = stage;

        // Configure combo box for LUY file selection
        fileComboBox.setOnAction(e -> {
            FileListEntry selectedItem = fileComboBox.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                loadDataModel(selectedItem);
            }
        });

        // Load existing files
        CLuyFileService.listFiles().stream().map(FileListEntry::new).forEach(fileComboBox.getItems()::add);

        // Configure download button to get new LUY data file
        Button downloadButton = new Button(CTranslations.INSTANCE.Button_Download);
        downloadButton.setOnAction(e -> {
            JFXDownloadDialog dialog = new JFXDownloadDialog(stage);
            dialog.setOnFileDownloaded(timestamp -> {
                Platform.runLater(() -> {
                    fileComboBox.getItems().add(new FileListEntry(timestamp));
                    // If the fileComboBox was empty, select (and thus load) the first entry
                    if (1 == fileComboBox.getItems().size()) {
                        fileComboBox.getSelectionModel().selectFirst();
                        loadDataModel(fileComboBox.getSelectionModel().getSelectedItem());
                    }
                });
            });
            dialog.show();
        });

        setupUI(downloadButton);
    }

    private void loadDataModel(FileListEntry selectedItem) {
        statusBar.setText(CTranslations.INSTANCE.Label_Downloading);

        Task<CDatamodel> loadTask = new Task<CDatamodel>() {
            @Override
            protected CDatamodel call() throws Exception {
                SoftReference<CDatamodel> model = loadedModels.get(selectedItem.timestamp);
                if (model == null || model.get() == null) {
                    CDatamodel dataModel = CDatamodel.load("data/" + selectedItem.timestamp);
                    loadedModels.put(selectedItem.timestamp, new SoftReference<>(dataModel));
                    return dataModel;
                } else {
                    return model.get();
                }
            }
        };

        loadTask.setOnSucceeded(event -> {
            CDatamodel loadedModel = loadTask.getValue();
            if (loadedModel != null) {
                // Wrap the loaded model in a filtered/sorted datamodel
                CFilteredAndSortedDatamodel filteredModel = new CFilteredAndSortedDatamodel(loadedModel);
                JFXBuildingBlockList buildingBlockList = new JFXBuildingBlockList(filteredModel);
                // Set the JFXBuildingBlockList as the center of the main BorderPane
                Scene scene = stage.getScene();
                if (scene != null && scene.getRoot() instanceof BorderPane root) root.setCenter(buildingBlockList);
            }
            statusBar.setText(CTranslations.INSTANCE.Label_Ready);
        });

        loadTask.setOnFailed(event -> {
            Throwable exception = loadTask.getException();
            exception.printStackTrace();
            statusBar.setText(String.format(CTranslations.INSTANCE.Label_DownloadError, exception.getMessage()));
        });

        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void setupUI(Button downloadButton) {
        // Top panel with file selection and download button
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));

        Label fileLabel = new Label(CTranslations.INSTANCE.Label_SelectDatafile);
        fileComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fileComboBox, Priority.ALWAYS);

        // Add spacer to push download button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.NEVER);

        topPanel.getChildren().addAll(fileLabel, fileComboBox, downloadButton);

        // Status bar styling
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");

        // Main layout
        BorderPane root = new BorderPane();
        root.setTop(topPanel);
//        root.setCenter(swingNode); // Use SwingNode instead of ViewerPanel directly
        root.setBottom(statusBar);

        // Configure stage
        stage.setTitle("LUYViewer - JavaFX Edition");
        stage.setScene(new Scene(root, 1200, 800));
        stage.centerOnScreen();

        // Set close operation
        stage.setOnCloseRequest(e -> Platform.exit());
    }

    public void show() {
        stage.show();
    }
}