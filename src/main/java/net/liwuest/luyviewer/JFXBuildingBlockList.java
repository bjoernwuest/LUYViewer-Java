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
import java.util.stream.Collectors;

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
        vbox.getChildren().addAll(typeComboBox, tableView);
        this.getChildren().add(vbox);

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
            TableColumn<CDatamodel.Element, Node> col = new TableColumn<>(feature.name);
            col.setCellValueFactory(cellData -> {
                CDatamodel.Element element = cellData.getValue();
                int rowIndex = tableView.getItems().indexOf(element);
                return new javafx.beans.property.SimpleObjectProperty<>(renderCell(selectedType, element, feature, rowIndex, columnIndex));
            });
            // Prevent reordering of the first two columns
            // and prevent moving any other column before them
            tableView.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<CDatamodel.Element, ?>>) change -> {
                while (change.next()) {
                    if (change.wasPermutated() || change.wasReplaced()) {
                        // Ensure first two columns remain at index 0 and 1
                        for (int i = 0; i < 2 && i < tableView.getColumns().size(); i++) {
                            TableColumn<CDatamodel.Element, ?> col2 = tableView.getColumns().get(i);
                            if (col2.getText() == null || !col2.getText().equals(features.toArray(new CMetamodel.Feature[0])[i].name)) {
                                // Move the column back to its original position
                                TableColumn<CDatamodel.Element, ?> correctCol = null;
                                for (TableColumn<CDatamodel.Element, ?> c : tableView.getColumns()) {
                                    if (c.getText() != null && c.getText().equals(features.toArray(new CMetamodel.Feature[0])[i].name)) {
                                        correctCol = c;
                                        break;
                                    }
                                }
                                if (correctCol != null) {
                                    tableView.getColumns().remove(correctCol);
                                    tableView.getColumns().add(i, correctCol);
                                }
                            }
                        }
                    }
                }
            });
            tableView.getColumns().add(col);
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
            case "integer":
            case "richtext":
            case "string": {
                Object value = Element.AdditionalData.getOrDefault(Feature.persistentName, "");
                if (value instanceof List valueList) value = valueList.isEmpty() ? "" : valueList.get(0);
                return new Label(value.toString());
            }
            default: {
                if (Feature.isSelfrelationFeature) {
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
                if (!Feature.isRelationshipFeature) System.out.println("Cell " + Row + "/" + Column + " - Feature type: " + Feature.type + " - Element: " + Element.elementURI);
                return new Label("todo");
            }
        }
    }
}
