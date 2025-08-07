package net.liwuest.luyviewer.rule;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import net.liwuest.luyviewer.model.CDatamodel;
import net.liwuest.luyviewer.model.CMetamodel;

import java.lang.reflect.Field;
import java.time.*;
import java.util.*;

public final class Operators {
  interface IOperator<T> {
    /**
     * Evaluate the value against the given Against values using this operator.
     *
     * @param Value The value to evaluation.
     * @param OfFeature The feature in which context the evaluation takes place.
     * @param Against The values to evaluate against.
     * @return {@code true} if the value satisfies the values to evaluate against by this operator, {@code false} otherwise.
     */
    public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, T Against);

    /**
     * Checks if this operator is compatible to the given feature type.
     *
     * @param FeatureType The feature type to check.
     * @return {@code true} if this operator can be used with the feature type, {@code false} otherwise.
     */
    public boolean compatibleWith(CMetamodel.FeatureType FeatureType);

    /**
     * Get the input element that provides the values to use as {@code Against} in {@link #evaluate(Object, CMetamodel.Feature, Object)}.
     *
     * @param Rule    The rule where the {@code Against} values are to be stored at.
     * @param Feature The feature the input is shown. Useful for e.g. enumerations.
     * @param ForType The actual TypeExpression this rule would be applied to.
     * @param Data    The current data model. Useful for e.g. self-realtions.
     * @return The input element.
     */
    public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data);
  }

  public final static IOperator EQUALS_BOOLEAN = new IOperator<Boolean>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Boolean Against) {
      Boolean typedValue = null;
      if ((Value instanceof Collection listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Boolean bv)) typedValue = bv;
      if ((null == typedValue) && (Value instanceof Boolean bv)) typedValue = bv;

      final Boolean valueToValidate = typedValue;
      if (null == Against) return (null == valueToValidate);
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.BOOLEAN  == FeatureType; }

    @Override public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      // Tri-state checkbox (true, false, null)
      CheckBox cb = new CheckBox();
      cb.setAllowIndeterminate(true);
      cb.setIndeterminate(true); // default: null
      cb.setTooltip(new Tooltip("Wert: Wahr, Falsch oder nicht gesetzt"));
      cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
        if (cb.isIndeterminate()) Rule.setValue(null);
        else Rule.setValue(cb.isSelected());
      });
      return cb;
    }

    @Override public String toString() { return "equals"; }
  };

  public final static IOperator EQUALS_DATE = new IOperator<Date>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Date Against) {
      Date typedValued = null;
      if ((Value instanceof Collection listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Date bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof Date bv)) typedValued = bv;

      final Date valueToValidate = typedValued;
      if (null == Against) return (null == valueToValidate);
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.DATE == FeatureType; }

    @Override public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      DatePicker dp = new DatePicker();
      Object val = Rule.getValue();
      if (val instanceof java.time.LocalDate ld) dp.setValue(ld);
      dp.valueProperty().addListener((obs, old, v) -> {
        if (v == null) Rule.setValue(null);
        else Rule.setValue(v);
      });
      return dp;
    }

    @Override public String toString() { return "equals"; }
  };

  public final static IOperator EQUALS_DATE_TIME = new IOperator<Instant>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Instant Against) {
      Instant typedValued = null;
      if ((Value instanceof Collection listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Instant bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof Instant bv)) typedValued = bv;

      final Instant valueToValidate = typedValued;
      if (null == Against) return (null == valueToValidate);
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.DATE_TIME == FeatureType; }

    @Override public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      // DatePicker + Uhrzeitfeld
      VBox box = new VBox(2);
      javafx.scene.control.DatePicker dp = new javafx.scene.control.DatePicker();
      TextField tfTime = new TextField();
      tfTime.setPromptText("HH:mm:ss");
      Object val = Rule.getValue();
      if (val instanceof java.time.LocalDateTime ldt) {
        dp.setValue(ldt.toLocalDate());
        tfTime.setText(ldt.toLocalTime().toString());
      }
      dp.valueProperty().addListener((obs, old, v) -> updateDateTimeValue(Rule, dp, tfTime));
      tfTime.textProperty().addListener((obs, old, v) -> updateDateTimeValue(Rule, dp, tfTime));
      box.getChildren().addAll(dp, tfTime);
      return box;
    }

    private void updateDateTimeValue(CRule rule, DatePicker dp, TextField tfTime) {
      LocalDate date = dp.getValue();
      String time = tfTime.getText();
      if (date == null || time == null || time.isBlank()) rule.setValue(null);
      else try { rule.setValue(LocalDateTime.of(date, LocalTime.parse(time)).atZone(ZoneId.systemDefault()).toInstant()); } catch (Exception ex) { rule.setValue(null); }
    }

    @Override public String toString() { return "equals"; }
  };

  public final static IOperator EQUALS_DECIMAL = new IOperator<Double>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Double Against) {
      Double typedValue = null;
      if ((Value instanceof Collection listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Double bv)) typedValue = bv;
      if ((null == typedValue) && (Value instanceof Double bv)) typedValue = bv;

      final Double valueToValidate = typedValue;
      if (null == Against) return (null == valueToValidate);
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.DECIMAL == FeatureType; }

    @Override public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      TextField tf = new TextField(null == Rule.getValue() ? "" : Rule.getValue().toString());
      // Validator: Nur Dezimalzahlen zulassen
      tf.textProperty().addListener((obs, old, v) -> {
        if (v == null || v.isBlank()) {
          Rule.setValue(null);
          tf.setStyle("");
        } else {
          try {
            Double.parseDouble(v.replace(',', '.'));
            Rule.setValue(Double.parseDouble(v.replace(',', '.')));
            tf.setStyle("");
          } catch (NumberFormatException ex) {
            tf.setStyle("-fx-background-color: #ffcccc;");
            Rule.setValue(null);
          }
        }
      });
      tf.setPromptText("Dezimalzahl");
      return tf;
    }

    @Override public String toString() { return "equals"; }
  };

  public final static IOperator EQUALS_INTEGER = new IOperator<Integer>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Integer Against) {
      Integer typedValued = null;
      if ((Value instanceof Collection listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Integer bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof Integer bv)) typedValued = bv;

      final Integer valueToValidate = typedValued;
      if (null == Against) return null == valueToValidate;
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.INTEGER == FeatureType; }

    @Override public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      TextField tf = new TextField(null == Rule.getValue() ? "" : Rule.getValue().toString());
      // Validator: Nur Integer zulassen
      tf.textProperty().addListener((obs, old, v) -> {
        if (v == null || v.isBlank()) {
          Rule.setValue(null);
          tf.setStyle("");
        } else {
          try {
            Integer.parseInt(v);
            Rule.setValue(Integer.parseInt(v));
            tf.setStyle("");
          } catch (NumberFormatException ex) {
            tf.setStyle("-fx-background-color: #ffcccc;");
            Rule.setValue(null);
          }
        }
      });
      tf.setPromptText("Ganzzahl");
      return tf;
    }

    @Override public String toString() { return "equals"; }
  };

  public final static IOperator EQUALS_INTERFACE_DIRECTION = new IOperator<CMetamodel.INTERFACE_DIRECTIONS>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, CMetamodel.INTERFACE_DIRECTIONS Against) {
      CMetamodel.INTERFACE_DIRECTIONS typedValued = null;
      if ((Value instanceof Collection listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof CMetamodel.INTERFACE_DIRECTIONS bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof CMetamodel.INTERFACE_DIRECTIONS bv)) typedValued = bv;

      final CMetamodel.INTERFACE_DIRECTIONS valueToValidate = typedValued;
      if (null == Against) return null == valueToValidate;
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.INTERFACE_DIRECTION == FeatureType; }

    @Override public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      // Einzel-Auswahl DIRECTIONS per ComboBox
      ComboBox<CMetamodel.INTERFACE_DIRECTIONS> combo = new ComboBox<>(FXCollections.observableArrayList(CMetamodel.INTERFACE_DIRECTIONS.values()));
      combo.setPromptText("Bitte wählen");
      // Vorbelegung
      Object vals = Rule.getValue();
      if (vals != null && vals instanceof CMetamodel.INTERFACE_DIRECTIONS dir) combo.setValue(dir);
      combo.valueProperty().addListener((obs, old, val) -> {
        if (val == null) Rule.setValue(null);
        else Rule.setValue(val);
      });
      return combo;
    }

    @Override public String toString() { return "equals"; }
  };

  public final static IOperator EQUALS_STRING = new IOperator<String>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, String Against) {
      String typedValued = null;
      if ((Value instanceof Collection listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof String bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof String bv)) typedValued = bv;

      final String valueToValidate = typedValued;
      if (null == Against) return null == valueToValidate;
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.STRING == FeatureType || FeatureType.RICHTEXT == FeatureType; }

    @Override public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      TextField tf = new TextField(null == Rule.getValue() ? "" : Rule.getValue().toString());
      tf.textProperty().addListener((obs, old, v) -> {
        if (v == null || v.isBlank()) Rule.setValue(null);
        else Rule.setValue(v);
      });
      return tf;
    }

    @Override public String toString() { return "equals"; }
  };

  public final static IOperator EQUALS_ENUM = new IOperator<Set<CMetamodel.Literal>>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Set<CMetamodel.Literal> Against) {
      Set<CMetamodel.Literal> valuesToValidate = new HashSet<>();
      if (Value instanceof Collection listValue) listValue.stream().filter(v -> v instanceof CMetamodel.Literal).forEach(v -> valuesToValidate.add((CMetamodel.Literal) v));
      if (Value instanceof CMetamodel.Literal singleValue) valuesToValidate.add(singleValue);

      if (null == Against || Against.isEmpty()) return valuesToValidate.isEmpty();
      else return Against.containsAll(valuesToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.ENUMERATION == FeatureType; }

    @Override public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      CMetamodel.EnumerationExpression enumExpression = Feature.metamodel.EnumerationExpressions.stream().filter(ee -> ee.name.equals(Feature.name)).findFirst().orElse(null);
      if (enumExpression == null || enumExpression.literals == null || enumExpression.literals.isEmpty()) return new Label("Keine Literale vorhanden");

      VBox box = new VBox(4);
      List<CheckBox> checkBoxes = new ArrayList<>();
      Set<CMetamodel.Literal> selected = new HashSet<>();
      // Vorbelegung
      Object val = Rule.getValue();
      if (val instanceof Set<?> set) set.forEach(o -> { if (o instanceof CMetamodel.Literal lit) selected.add(lit); });
      else if (val instanceof CMetamodel.Literal lit) selected.add(lit);

      for (CMetamodel.Literal literal : enumExpression.literals) {
        CheckBox cb = new CheckBox(literal.name);
        if (selected.contains(literal)) cb.setSelected(true);
        cb.selectedProperty().addListener((obs, old, isSelected) -> {
          if (isSelected) selected.add(literal);
          else selected.remove(literal);
          if (selected.isEmpty()) Rule.setValue(null);
          else Rule.setValue(new HashSet<>(selected));
        });
        checkBoxes.add(cb);
      }
      if (checkBoxes.size() > 4) {
        VBox inner = new VBox(4);
        inner.getChildren().addAll(checkBoxes);
        ScrollPane scroll = new ScrollPane(inner);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(100);
        box.getChildren().add(scroll);
      } else box.getChildren().addAll(checkBoxes);
      return box;
    }

    @Override public String toString() { return "equals"; }
  };

  public final static IOperator EQUALS_SELF_RELATION = new IOperator<Set<CMetamodel.SubstantialTypeExpression>>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Set<CMetamodel.SubstantialTypeExpression> Against) {
      Set<CMetamodel.SubstantialTypeExpression> valuesToValidate = new HashSet<>();
      if (Value instanceof Collection listValue) listValue.stream().filter(v -> v instanceof CMetamodel.SubstantialTypeExpression).forEach(v -> valuesToValidate.add((CMetamodel.SubstantialTypeExpression) v));
      if (Value instanceof CMetamodel.SubstantialTypeExpression singleValue) valuesToValidate.add(singleValue);

      if (null == Against || Against.isEmpty()) return valuesToValidate.isEmpty();
      else return Against.containsAll(valuesToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) { return FeatureType.SELF_RELATION == FeatureType; }

    @Override
    public Node getInput(CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      Set<CDatamodel.BuildingBlock> buildingBlocks = Data.BuildingBlocks.get(ForType);
      Set<CMetamodel.SubstantialTypeExpression> selected = new HashSet<>();
      Object val = Rule.getValue();
      if (val instanceof Collection<?> list) list.forEach(o -> { if (o instanceof CMetamodel.SubstantialTypeExpression ste) selected.add(ste); });
      else if (val instanceof CMetamodel.SubstantialTypeExpression ste) selected.add(ste);
      List<CheckBox> checkBoxes = new ArrayList<>();
      if (buildingBlocks != null) {
        for (CDatamodel.BuildingBlock bb : buildingBlocks) {
          String label = bb.name + " (ID: " + bb.id + ")";
          CheckBox cb = new CheckBox(label);
          if (selected.stream().anyMatch(sel -> sel.equals(bb.metamodelType))) cb.setSelected(true);
          cb.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) selected.add(bb.metamodelType);
            else selected.remove(bb.metamodelType);
            if (selected.isEmpty()) Rule.setValue(null);
            else Rule.setValue(new HashSet<>(selected));
          });
          checkBoxes.add(cb);
        }
      }
      VBox box = new VBox(4);
      if (checkBoxes.size() > 4) {
        VBox inner = new VBox(4);
        inner.getChildren().addAll(checkBoxes);
        ScrollPane scroll = new ScrollPane(inner);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(100);
        box.getChildren().add(scroll);
      } else box.getChildren().addAll(checkBoxes);
      return box;
    }

    @Override public String toString() { return "equals"; }
  };

  public final static Set<IOperator> getSupportedOperators(CMetamodel.FeatureType FeatureType) {
    Set<IOperator> result = new HashSet<>();
    for (Field f : Operators.class.getDeclaredFields()) {
      try {
        Object o = f.get(null);
        if (o instanceof IOperator operator) {
          if (operator.compatibleWith(FeatureType)) result.add(operator);
        }
      } catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }
    return result;
  }
}
