package net.liwuest.luyviewer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.liwuest.luyviewer.model.CLuyFileService;
import net.liwuest.luyviewer.util.CConfig;
import net.liwuest.luyviewer.util.CConfigService;
import net.liwuest.luyviewer.util.CTranslations;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Dialog zur Auswahl und zum Download von S3-Dateipaaren.
 */
public class JFXS3DownloadDialog {
    private final Stage dialogStage;
    private Consumer<String> onFileDownloaded;

    public JFXS3DownloadDialog(Stage owner, javafx.scene.control.ComboBox<?> fileComboBox) {
        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(CTranslations.INSTANCE.Button_DownloadFromS3);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        Label infoLabel = new Label(CTranslations.INSTANCE.Label_SelectDatasetToDownload);
        root.setTop(infoLabel);

        ListView<String> listView = new ListView<>();
        root.setCenter(listView);

        Button downloadBtn = new Button(CTranslations.INSTANCE.Button_Download);
        downloadBtn.setDisable(true);
        HBox buttonBox = new HBox(10, downloadBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(buttonBox);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> downloadBtn.setDisable(sel == null));

        downloadBtn.setOnAction(e -> {
            String baseName = listView.getSelectionModel().getSelectedItem();
            if (baseName != null) {
                downloadBtn.setDisable(true);
                infoLabel.setText(CTranslations.INSTANCE.Label_Downloading);
                new Thread(() -> {
                    try {
                        CConfig config = CConfigService.getConfig();
                        CLuyFileService.downloadS3FilePair(config, baseName);
                        Platform.runLater(() -> {
                            infoLabel.setText(CTranslations.INSTANCE.Label_DownloadSuccess);
                            if (onFileDownloaded != null) onFileDownloaded.accept(baseName);
                            dialogStage.close();
                        });
                    } catch (Exception ex) {
                      LUYViewer.LOGGER.log(java.util.logging.Level.SEVERE, "Failed to download S3 file pair", ex);
                        Platform.runLater(() -> infoLabel.setText(String.format(CTranslations.INSTANCE.Label_DownloadError, ex.getMessage())));
                        downloadBtn.setDisable(false);
                    }
                }).start();
            }
        });

        dialogStage.setScene(new Scene(root, 400, 300));

        // Lade S3-Paare, die lokal fehlen
        new Thread(() -> {
            try {
                CConfig config = CConfigService.getConfig();
                Set<String> missingPairs = CLuyFileService.getMissingS3Pairs(config);
                Platform.runLater(() -> listView.setItems(FXCollections.observableArrayList(missingPairs)));
            } catch (Exception ex) {
              LUYViewer.LOGGER.log(java.util.logging.Level.SEVERE, "Failed to load S3-List", ex);
                Platform.runLater(() -> infoLabel.setText(String.format(CTranslations.INSTANCE.Label_S3ListingFailed, ex.getMessage())));
            }
        }).start();
    }

    public void setOnFileDownloaded(Consumer<String> handler) {
        this.onFileDownloaded = handler;
    }

    public void show() {
        dialogStage.showAndWait();
    }
}

