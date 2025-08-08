package net.liwuest.luyviewer.rule;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.liwuest.luyviewer.model.CDatamodel;
import net.liwuest.luyviewer.model.CMetamodel;
import net.liwuest.luyviewer.util.CEventBus;

import java.util.*;
import java.util.function.Consumer;

/**
 * Ein visueller Rule-Builder-Dialog für Filter (ähnlich reactscript.com/user-friendly-query-builder-react/).
 * Änderungen werden erst beim Speichern übernommen, Abbrechen verwirft alle Änderungen.
 */
public class JFXRuleBuilderDialog extends Stage {
    private final CFilter originalFilter;
    private CFilter workingCopy;
    private final Consumer<CFilter> onSave;
    private CFilter resultFilter = null;
    private final CDatamodel data;
    private final CMetamodel.TypeExpression forType;

    public JFXRuleBuilderDialog(CDatamodel Data, CMetamodel.TypeExpression ForType, CFilter filter, Consumer<CFilter> onSave) {
        this.data = Data;
        this.forType = ForType;
        this.originalFilter = filter;
        this.workingCopy = filter.copy();
        this.onSave = onSave;
        setTitle("Filter bearbeiten");
        initModality(Modality.APPLICATION_MODAL);
        setMinWidth(600);
        setMinHeight(400);
        // Dynamische Breite: Bildschirmbreite berücksichtigen
        double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double preferredWidth = Math.min(1200, screenWidth * 0.98); // Maximal 98% der Bildschirmbreite, aber nicht mehr als 1200
        setWidth(preferredWidth);
        setScene(new Scene(createContent(), preferredWidth, 500));
    }

    public CFilter getResultFilter() {
        return resultFilter;
    }

    private Pane createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        ScrollPane scrollPane = new ScrollPane();
        VBox filterBox = new VBox(10);
        renderGroup(filterBox, workingCopy.getRootGroup());
        scrollPane.setContent(filterBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(getWidth() - 40); // ScrollPane möglichst breit
        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));
        buttonBar.setSpacing(10);
        buttonBar.setStyle("-fx-alignment: center-right;");
        Button btnSave = new Button("Speichern");
        Button btnCancel = new Button("Abbrechen");
        btnSave.setDisable(!workingCopy.isValid());
        // Aktualisiere Button-Status bei jeder Änderung
        CEventBus.subscribe(event -> btnSave.setDisable(!workingCopy.isValid()), EventEvaluatableChanged.class);
        btnSave.setOnAction(e -> {
            resultFilter = workingCopy;
            if (onSave != null) onSave.accept(workingCopy);
            close();
        });
        btnCancel.setOnAction(e -> {
            resultFilter = originalFilter;
            close();
        });
        buttonBar.getChildren().addAll(btnSave, btnCancel);
        root.getChildren().addAll(scrollPane, buttonBar);
        return root;
    }

    private void renderGroup(VBox parent, CGroup group) {
        VBox groupBox = new VBox(8);
        groupBox.setStyle("-fx-border-color: #bbb; -fx-border-radius: 4; -fx-padding: 8; -fx-background-color: #f9f9f9;");
        // Gruppen-Operator
        ComboBox<CGroup.GroupOperator> opBox = new ComboBox<>(FXCollections.observableArrayList(CGroup.GroupOperator.values()));
        opBox.setValue(group.getOperator());
        opBox.valueProperty().addListener((obs, old, val) -> {
            group.setOperator(val);
            refresh();
        });
        HBox opRow = new HBox(8, new Label("Gruppe:"), opBox);
        groupBox.getChildren().add(opRow);
        // Regeln als Grid
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        grid.setPrefWidth(getWidth() - 60); // Grid möglichst breit
        // Spalten wachsen lassen
        for (int i = 0; i < 5; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            if (i == 0) cc.setMinWidth(60); // Label
            if (i == 1) cc.setMinWidth(140); // Feature
            if (i == 2) cc.setMinWidth(110); // Operator
            if (i == 3) cc.setMinWidth(160); // Value
            if (i == 4) cc.setMinWidth(32); // Remove
            grid.getColumnConstraints().add(cc);
        }
        int rowIdx = 0;
        for (AEvaluatable evaluatable : group.getRules()) {
            if (evaluatable instanceof CRule rule) {
                Node[] controls = renderRuleGrid(rule, group);
                for (int col = 0; col < controls.length; col++) {
                    grid.add(controls[col], col, rowIdx);
                }
                rowIdx++;
            } else if (evaluatable instanceof CGroup sub) {
                VBox subBox = new VBox();
                renderGroup(subBox, sub);
                grid.add(subBox, 0, rowIdx++, 5, 1); // Subgruppe über alle Spalten
            } else System.err.println("Unknown type of evaluatable: " + ((null == evaluatable) ? "<null>" : (evaluatable.getClass().getName() + " (" + evaluatable.toString() + "")));
        }
        groupBox.getChildren().add(grid);
        // Buttons zum Hinzufügen
        HBox addBar = new HBox(8);
        Button btnAddRule = new Button("Regel hinzufügen");
        Button btnAddGroup = new Button("Gruppe hinzufügen");
        boolean isNot = group.getOperator() == CGroup.GroupOperator.NOT;
        btnAddRule.setDisable(isNot && group.getRules().size() >= 1);
        btnAddGroup.setDisable(isNot && group.getRules().size() >= 1);
        btnAddRule.setOnAction(e -> {
            group.addRule(new CRule());
            refresh();
        });
        btnAddGroup.setOnAction(e -> {
            group.addRule(new CGroup(CGroup.GroupOperator.AND));
            refresh();
        });
        addBar.getChildren().addAll(btnAddRule, btnAddGroup);
        groupBox.getChildren().add(addBar);
        parent.getChildren().add(groupBox);
    }

    private Node[] renderRuleGrid(CRule rule, CGroup parentGroup) {
        // Regel-Label
        Label lblRegel = new Label("Regel:");
        lblRegel.setMinWidth(60);
        // Feature-Auswahl
        ComboBox<CMetamodel.Feature> featureBox = new ComboBox<>(FXCollections.observableArrayList(workingCopy.getTypeExpression().features));
        featureBox.setValue(rule.getFeature());
        featureBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(CMetamodel.Feature item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : item.name);
            }
        });
        featureBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CMetamodel.Feature item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : item.name);
            }
        });
        featureBox.setMinWidth(140);
        // Operator-Auswahl
        ComboBox<Operators.IOperator> opBox = new ComboBox<>();
        opBox.setMinWidth(110);
        Pane valueInputWrapper = new Pane();
        valueInputWrapper.setMinWidth(160);
        Node valueInput = createValueInput(rule, featureBox.getValue());
        valueInputWrapper.getChildren().setAll(valueInput);
        featureBox.valueProperty().addListener((obs, old, val) -> {
            rule.setFeature(val);
            if (val != null) {
                var supported = FXCollections.observableArrayList(Operators.getSupportedOperators(val.featureType));
                opBox.setItems(supported);
                Operators.IOperator currentOp = opBox.getValue();
                if (currentOp != null && supported.contains(currentOp)) {}
                else if (!supported.isEmpty()) opBox.setValue(supported.get(0));
                else opBox.setValue(null);
            } else {
                opBox.setItems(FXCollections.observableArrayList());
                opBox.setValue(null);
            }
            valueInputWrapper.getChildren().setAll(createValueInput(rule, val));
        });
        if (rule.getFeature() != null)
            opBox.setItems(FXCollections.observableArrayList(Operators.getSupportedOperators(rule.getFeature().featureType)));
        opBox.setValue(rule.getOperator());
        opBox.valueProperty().addListener((obs, old, val) -> {
            rule.setOperator(val);
            valueInputWrapper.getChildren().setAll(createValueInput(rule, featureBox.getValue()));
        });
        // Entfernen-Button
        Button btnRemove = new Button();
        btnRemove.setTooltip(new Tooltip("Entfernen"));
        btnRemove.setGraphic(new Label("\uD83D\uDDD1"));
        btnRemove.setMinWidth(32);
        btnRemove.setMinHeight(32);
        btnRemove.setMaxHeight(32);
        btnRemove.setStyle("-fx-padding: 2 4 2 4;");
        btnRemove.setOnAction(e -> {
            parentGroup.removeRule(rule);
            refresh();
        });
        return new Node[]{lblRegel, featureBox, opBox, valueInputWrapper, btnRemove};
    }

    private void refresh() { setScene(new Scene(createContent(), getScene().getWidth(), getScene().getHeight())); }

    private Node createValueInput(CRule rule, CMetamodel.Feature feature) {
        if ((null != rule) && (null != rule.getOperator())) return rule.getOperator().getInput(rule, feature, forType, data);
        else return new Label("");
    }
}
