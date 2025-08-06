
package net.liwuest.luyviewer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.liwuest.luyviewer.model.CLuyFileService;

import java.util.function.Consumer;

/**
 * Dialog for downloading LUY files from remote instances with authentication.
 */
public class JFXDownloadDialog {
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button downloadButton = new Button("Download");
    private final Button cancelButton = new Button("Cancel");
    private final Label statusLabel = new Label("Enter your credentials to download from LUY");
    private Consumer<String> onFileDownloaded;
    private final Stage dialog;

    public JFXDownloadDialog(Stage parent) {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);
        dialog.setTitle("Download from LUY");
        dialog.setResizable(false);

        initializeComponents();
        setupEventHandlers();
    }

    private void initializeComponents() {
        // Main panel with credentials
        GridPane mainPanel = new GridPane();
        mainPanel.setHgap(10);
        mainPanel.setVgap(10);
        mainPanel.setPadding(new Insets(20, 20, 10, 20));

        // Username field
        mainPanel.add(new Label("Username:"), 0, 0);
        usernameField.setPrefWidth(200);
        mainPanel.add(usernameField, 1, 0);

        // Password field
        mainPanel.add(new Label("Password:"), 0, 1);
        passwordField.setPrefWidth(200);
        mainPanel.add(passwordField, 1, 1);

        // Column constraints to make text fields expand
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        mainPanel.getColumnConstraints().addAll(col1, col2);

        // Bottom panel with buttons and status
        VBox bottomPanel = new VBox(10);

        // Button panel
        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        buttonPanel.setPadding(new Insets(10, 20, 10, 20));

        downloadButton.setDisable(true);
        downloadButton.setOnAction(e -> downloadFile());

        cancelButton.setOnAction(e -> dialog.close());

        buttonPanel.getChildren().addAll(cancelButton, downloadButton);

        // Status panel
        statusLabel.setPadding(new Insets(5, 10, 5, 10));
        statusLabel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");
        statusLabel.setPrefHeight(25);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        bottomPanel.getChildren().addAll(buttonPanel, statusLabel);

        // Main layout
        BorderPane root = new BorderPane();
        root.setCenter(mainPanel);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 400, 250);
        dialog.setScene(scene);
    }

    private void setupEventHandlers() {
        // Text listeners to enable/disable download button
        Runnable updateDownloadButtonState = () -> {
            boolean hasUsername = !usernameField.getText().trim().isEmpty();
            boolean hasPassword = !passwordField.getText().isEmpty();
            downloadButton.setDisable(!(hasUsername && hasPassword));
        };

        usernameField.textProperty().addListener((obs, oldText, newText) -> updateDownloadButtonState.run());
        passwordField.textProperty().addListener((obs, oldText, newText) -> updateDownloadButtonState.run());

        // Enter key handling
        passwordField.setOnAction(e -> {
            if (!downloadButton.isDisabled()) downloadFile();
        });

        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    private void downloadFile() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Input Required");
            alert.setHeaderText(null);
            alert.setContentText("Please enter both username and password");
            alert.showAndWait();
            return;
        }

        statusLabel.setText("Downloading from LUY...");
        downloadButton.setDisable(true);
        cancelButton.setDisable(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);

        Task<String> downloadTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return CLuyFileService.downloadFile(username, password);
            }
        };

        downloadTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("Download completed successfully");
                if (onFileDownloaded != null) onFileDownloaded.accept(downloadTask.getValue());
                dialog.close();
            });
        });

        downloadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                Throwable exception = downloadTask.getException();
                statusLabel.setText("Download failed: " + exception.getMessage());

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Download Error");
                alert.setHeaderText(null);
                alert.setContentText("Download failed:\n" + exception.getMessage());
                alert.showAndWait();

                // Reset UI state
                passwordField.clear();
                usernameField.setDisable(false);
                passwordField.setDisable(false);
                cancelButton.setDisable(false);
                updateDownloadButtonState();
            });
        });

        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    private void updateDownloadButtonState() {
        boolean hasUsername = !usernameField.getText().trim().isEmpty();
        boolean hasPassword = !passwordField.getText().isEmpty();
        downloadButton.setDisable(!(hasUsername && hasPassword));
    }

    public void setOnFileDownloaded(Consumer<String> onFileDownloaded) {
        this.onFileDownloaded = onFileDownloaded;
    }

    public void show() {
        dialog.show();
    }
}
