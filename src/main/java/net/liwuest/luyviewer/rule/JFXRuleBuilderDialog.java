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
        setScene(new Scene(createContent(), 700, 500));
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
            refresh(); // Buttons für Neue Gruppe/Regel werden so wieder aktiviert/deaktiviert
        });
        HBox opRow = new HBox(8, new Label("Gruppe:"), opBox);
        groupBox.getChildren().add(opRow);
        // Regeln
        for (AEvaluatable evaluatable : group.getRules()) {
            if (evaluatable instanceof CRule rule) groupBox.getChildren().add(renderRule(rule, group));
            else if (evaluatable instanceof CGroup sub) renderGroup(groupBox, sub);
            else System.err.println("Unknown type of evaluatable: " + ((null == evaluatable) ? "<null>" : (evaluatable.getClass().getName() + " (" + evaluatable.toString() + "")));
        }
        // Buttons zum Hinzufügen
        HBox addBar = new HBox(8);
        Button btnAddRule = new Button("Regel hinzufügen");
        Button btnAddGroup = new Button("Gruppe hinzufügen");
        // NOT-Gruppen dürfen nur ein Kind haben
        boolean isNot = group.getOperator() == CGroup.GroupOperator.NOT;
        boolean canAdd = group.getRules().size() < 1 || !isNot;
        btnAddRule.setDisable(isNot && group.getRules().size() >= 1);
        btnAddGroup.setDisable(isNot && group.getRules().size() >= 1);
        btnAddRule.setOnAction(e -> {
            group.addRule(new CRule(null, null, new HashSet<>()));
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

    private HBox renderRule(CRule rule, CGroup parentGroup) {
        HBox row = new HBox(8);
        row.setPadding(new Insets(2, 0, 2, 0));
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
        // Operator-Auswahl
        ComboBox<Operators.IOperator> opBox = new ComboBox<>();
        // Value-Eingabe-Node als Platzhalter
        Pane valueInputWrapper = new Pane();
        Node valueInput = createValueInput(rule, featureBox.getValue());
        valueInputWrapper.getChildren().setAll(valueInput);
        featureBox.valueProperty().addListener((obs, old, val) -> {
            rule.setFeature(val);
            // Operatoren-Liste aktualisieren
            if (val != null) {
                var supported = FXCollections.observableArrayList(Operators.getSupportedOperators(val.featureType));
                opBox.setItems(supported);
                // Prüfe, ob aktueller Operator noch gültig ist
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
        Button btnRemove = new Button("Entfernen");
        btnRemove.setOnAction(e -> {
            parentGroup.removeRule(rule);
            refresh();
        });
        row.getChildren().addAll(new Label("Regel:"), featureBox, opBox, valueInputWrapper, btnRemove);
        return row;
    }

    private void refresh() { setScene(new Scene(createContent(), getScene().getWidth(), getScene().getHeight())); }

    private Node createValueInput(CRule rule, CMetamodel.Feature feature) {
        if ((null != rule) && (null != rule.getOperator())) return rule.getOperator().getInput(rule, feature, forType, data);
        else return new Label("");
    }
}
