package net.liwuest.luyviewer;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JFXBuildingBlockList extends Pane {
    private final CFilteredAndSortedDatamodel Data;
    private final ComboBox<CMetamodel.TypeExpression> typeComboBox;
    private final TableView<CDatamodel.Element> tableView;

    public JFXBuildingBlockList(CFilteredAndSortedDatamodel Data) {
        assert null != Data;
        this.Data = Data;
        this.typeComboBox = new ComboBox<>();
        this.tableView = new TableView<>();

        // Layout
        VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 10, 10, 10));
        vbox.getChildren().add(typeComboBox);
        // TableView in eine horizontale ScrollPane einbetten, aber vertikales Scrollen der TableView überlassen
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
        scrollPane.setContent(tableView);
        scrollPane.setFitToHeight(true); // TableView soll volle Höhe nutzen
        scrollPane.setFitToWidth(false); // TableView kann breiter als der sichtbare Bereich sein
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED); // Horizontalen Scrollbar anzeigen
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        // Breite der ScrollPane an die Breite des Parent-Pane binden
        scrollPane.prefViewportWidthProperty().bind(this.widthProperty().subtract(20)); // 20 für Padding
        scrollPane.prefViewportHeightProperty().bind(this.heightProperty().subtract(70));
        vbox.getChildren().add(scrollPane);
        vbox.setFillWidth(true);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        this.getChildren().add(vbox);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        // TableView: horizontalen Scrollbar per CSS ausblenden
        tableView.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        // Fill ComboBox with types
        Set<CMetamodel.TypeExpression> types = Data.getTypes();
        typeComboBox.setItems(FXCollections.observableArrayList(types));
        if (!types.isEmpty()) { typeComboBox.getSelectionModel().select(0); }

        // Update table when type changes
        typeComboBox.setOnAction(e -> updateTable());
        updateTable();
    }

    private void updateTable() {
        CMetamodel.TypeExpression selectedType = typeComboBox.getSelectionModel().getSelectedItem();
        tableView.getColumns().clear();
        tableView.getItems().clear();
        if (selectedType == null) return;

        LinkedHashSet<CMetamodel.Feature> features = Data.getOrderedFeatures(selectedType);
        if (features == null) return;

        // Create columns
        int colIdx = 0;
        for (CMetamodel.Feature feature : features) {
            final int columnIndex = colIdx;
            if (feature.isRelationshipFeature && !feature.isSelfrelationFeature) {
                Set<CMetamodel.Feature> relFeatures = Data.getFeaturesOfRelation(feature);
                javafx.scene.control.TableColumn<CDatamodel.Element, Node> parentCol = new TableColumn<>(feature.name);
                int relColIdx = 0;
                for (CMetamodel.Feature relFeature : relFeatures) {
                    final int relColumnIndex = relColIdx;
                    TableColumn<CDatamodel.Element, Node> subCol = new TableColumn<>(relFeature.name);
                    subCol.setCellValueFactory(cellData -> {
                        CDatamodel.Element element = cellData.getValue();
                        int rowIndex = tableView.getItems().indexOf(element);
                        return new javafx.beans.property.SimpleObjectProperty<>(renderRelationshipCell(selectedType, element, feature, relFeature, rowIndex, columnIndex, relColumnIndex));
                    });
                    subCol.setResizable(true);
                    subCol.setMinWidth(120); // Mindestbreite für Subspalten
                    // Disable sorting if not sortable
                    subCol.setSortable(relFeature.isSortable);
                    parentCol.getColumns().add(subCol);
                    relColIdx++;
                }
                parentCol.setResizable(true);
                parentCol.setMinWidth(120 * Math.max(1, relFeatures.size())); // Mindestbreite für Parent
                parentCol.setSortable(false); // Parent columns are not directly sortable
                tableView.getColumns().add(parentCol);
            } else {
                TableColumn<CDatamodel.Element, Node> col = new TableColumn<>(feature.name);
                col.setCellValueFactory(cellData -> {
                    CDatamodel.Element element = cellData.getValue();
                    int rowIndex = tableView.getItems().indexOf(element);
                    return new javafx.beans.property.SimpleObjectProperty<>(renderCell(selectedType, element, feature, rowIndex, columnIndex));
                });
                col.setResizable(true);
                col.setMinWidth(120); // Mindestbreite für normale Spalten
                // Disable sorting if not sortable
                col.setSortable(feature.isSortable);
                tableView.getColumns().add(col);
            }
            colIdx++;
        }

        // Fill rows
        Set<? extends CDatamodel.Element> elements = Data.getFilteredAndSortedData(selectedType);
        if (elements != null) {
            tableView.setItems(FXCollections.observableArrayList(elements));
        }
    }

    Node renderCell(CMetamodel.TypeExpression Type, CDatamodel.Element Element, CMetamodel.Feature Feature, int Row, int Column) {
        switch (Feature.type) {
            case "boolean": {
                Object value = Element.AdditionalData.getOrDefault(Feature.persistentName, null);
                if (value instanceof List valueList) value = valueList.isEmpty() ? null : valueList.get(0);
                CheckBox checkBox = new CheckBox();
                checkBox.setDisable(true);
                if (value instanceof Boolean booleanValue) {
                    checkBox.setSelected(booleanValue);
                    checkBox.setIndeterminate(false);
                    return checkBox;
                } else {
                    checkBox.setIndeterminate(true);
                    checkBox.setStyle("-fx-opacity: 1; -fx-background-color: lightgray;");
                    return checkBox;
                }
            }
            case "date": {
                Object value = Element.AdditionalData.getOrDefault(Feature.persistentName, null);
                if (value instanceof List valueList) value = valueList.isEmpty() ? null : valueList.get(0);
                if ((null != value) && (value instanceof Instant inst)) value = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(inst); else value = "";
                return new Label(value.toString());
            }
            case "date_time": {
                Object value = Element.AdditionalData.getOrDefault(Feature.persistentName, null);
                if (value instanceof List valueList) value = valueList.isEmpty() ? null : valueList.get(0);
                if ((null != value) && (value instanceof Instant inst)) value = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(inst); else value = "";
                return new Label(value.toString());
            }
            case "decimal":
            case "integer":
            case "richtext":
            case "string": {
                Object value = Element.AdditionalData.getOrDefault(Feature.persistentName, "");
                if (value instanceof List valueList) value = valueList.isEmpty() ? "" : valueList.get(0);
                return new Label(value.toString());
            }
            case "io.luy.model.Direction": {
                Object value = Element.AdditionalData.getOrDefault(Feature.persistentName, CMetamodel.DIRECTIONS.NO_DIRECTION);
                if (value instanceof List valueList) value = valueList.isEmpty() ? CMetamodel.DIRECTIONS.NO_DIRECTION : valueList.get(0);
                if (!(value instanceof CMetamodel.DIRECTIONS)) value = CMetamodel.DIRECTIONS.valueOf(value.toString());
                if (value instanceof CMetamodel.DIRECTIONS dirValue) {
                    Label label = switch (dirValue) {
                        case FIRST_TO_SECOND -> new Label("\u2192"); // →
                        case SECOND_TO_FIRST -> new Label("\u2190"); // ←
                        case BOTH_DIRECTIONS -> new Label("\u2194"); // ↔
                        default -> new Label("-");
                    };
                    label.setStyle("-fx-font-size: 16px; -fx-alignment: center;");
                    return label;
                }
                return new Label("-");
            }
            default: {
                if (Feature.isSelfrelationFeature || Feature.referencesBuildingblock()) {
                    java.util.List<CDatamodel.BuildingBlock> blocks = (java.util.List<CDatamodel.BuildingBlock>) Element.AdditionalData.getOrDefault(Feature.persistentName, new java.util.ArrayList<CDatamodel.BuildingBlock>());
                    java.util.List<CDatamodel.BuildingBlock> sortedBlocks = new java.util.ArrayList<>(blocks);
                    sortedBlocks.sort(java.util.Comparator.comparing(block -> block.name != null ? block.name : ""));
                    javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox();
                    vbox.setSpacing(2);
                    vbox.setFillWidth(true);
                    java.util.List<javafx.scene.control.Button> buttons = new java.util.ArrayList<>();
                    for (CDatamodel.BuildingBlock block : sortedBlocks) {
                        String label = block.name + " (" + block.id + ")";
                        javafx.scene.control.Button btn = new javafx.scene.control.Button(label);
                        btn.setMaxWidth(Double.MAX_VALUE);
                        buttons.add(btn);
                    }
                    vbox.getChildren().addAll(buttons);
                    return vbox;
                }
                if (Feature.isEnumerationAttribute) {
                    Object value = Element.AdditionalData.getOrDefault(Feature.persistentName, new java.util.ArrayList<CMetamodel.Literal>());
                    if (value instanceof List valueList) {
                        if (valueList.isEmpty()) return new Label("");
                        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox();
                        vbox.setSpacing(2);
                        vbox.setFillWidth(true);
                        valueList.stream().sorted(Comparator.comparing(l -> ((CMetamodel.Literal) l).name)).forEach(l -> {
                            CMetamodel.Literal literal = (CMetamodel.Literal)l;
                            javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox();
                            hbox.setSpacing(5);
                            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(literal.name);
                            nameLabel.setMaxWidth(Double.MAX_VALUE);
                            javafx.scene.layout.HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);
                            javafx.scene.control.Label colorLabel = new javafx.scene.control.Label();
                            colorLabel.setMinWidth(24);
                            colorLabel.setMaxWidth(24);
                            colorLabel.setPrefWidth(24);
                            colorLabel.setStyle("-fx-background-color: " + (null != literal.color ? String.format("#%06X", literal.color.getRGB() & 0xFFFFFF) : "#cccccc") + "; -fx-border-color: #888; -fx-border-radius: 3; -fx-background-radius: 3;");
                            hbox.getChildren().addAll(nameLabel, colorLabel);
                            vbox.getChildren().add(hbox);
                        });
                        return vbox;
                    }
                }
                System.out.println("Cell " + Row + "/" + Column + " - Feature type: " + Feature.type + " (ref. BB: " + Feature.referencesBuildingblock() + ") - Element: " + Element.elementURI);
                return new Label("todo");
            }
        }
    }

    /**
     * Renders a cell for a relationship feature's sub-feature (nested table cell).
     */
    Node renderRelationshipCell(CMetamodel.TypeExpression Type, CDatamodel.Element Element, CMetamodel.Feature RelationshipFeature, CMetamodel.Feature SubFeature, int Row, int Column, int SubColumn) {
        final double ROW_HEIGHT = 24.0; // Feste Höhe für jede Zeile/Node
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox();
        vbox.setSpacing(2);
        vbox.setFillWidth(true);
        if (Element.AdditionalData.getOrDefault(RelationshipFeature.persistentName, new ArrayList<>()) instanceof List elements) {
            for (Object e : elements) {
                Node node = renderCell(Type, (CDatamodel.Element)e, SubFeature, Row, SubColumn);
                if (node instanceof javafx.scene.control.Label label) {
                    label.setMinHeight(ROW_HEIGHT);
                    label.setPrefHeight(ROW_HEIGHT);
                    label.setMaxHeight(ROW_HEIGHT);
                } else if (node instanceof javafx.scene.layout.Region region) {
                    region.setMinHeight(ROW_HEIGHT);
                    region.setPrefHeight(ROW_HEIGHT);
                    region.setMaxHeight(ROW_HEIGHT);
                }
                vbox.getChildren().add(node);
            }
        }
        // Optional: VBox auf Gesamthöhe setzen
        vbox.setMinHeight((ROW_HEIGHT+2) * Math.max(1, vbox.getChildren().size()));
        vbox.setPrefHeight((ROW_HEIGHT+2) * Math.max(1, vbox.getChildren().size()));
        vbox.setMaxHeight((ROW_HEIGHT+2) * Math.max(1, vbox.getChildren().size()));
        return vbox;
    }
}
