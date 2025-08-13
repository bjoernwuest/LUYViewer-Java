package net.liwuest.luyviewer.rule;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import net.liwuest.luyviewer.model.CDatamodel;
import net.liwuest.luyviewer.model.CMetamodel;
import net.liwuest.luyviewer.util.CTranslations;

import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public final class Operators {
  public interface IOperator<T> {
    /**
     * Evaluate the value against the given Against values using this operator.
     *
     * @param Value The value to evaluation.
     * @param OfFeature The feature in which context the evaluation takes place.
     * @param Against The values to evaluate against.
     * @return {@code true} if the value satisfies the values to evaluate against by this operator, {@code false} otherwise.
     */
    boolean evaluate(Object Value, CMetamodel.Feature OfFeature, T Against);

    /**
     * Checks if this operator is compatible to the given feature type.
     *
     * @param Feature The feature type to check.
     * @return {@code true} if this operator can be used with the feature type, {@code false} otherwise.
     */
    boolean compatibleWith(CMetamodel.Feature Feature);

    /**
     * Get the input element that provides the values to use as {@code Against} in {@link #evaluate(Object, CMetamodel.Feature, Object)}.
     *
     * @param Filter
     * @param Rule    The rule where the {@code Against} values are to be stored at.
     * @param Feature The feature the input is shown. Useful for e.g. enumerations.
     * @param ForType The actual TypeExpression this rule would be applied to.
     * @param Data    The current data model. Useful for e.g. self-relations.
     * @return The input element.
     */
    Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data);

    default boolean requiresInput() { return true; }
  }

  private static Node getBooleanInput(CRule Rule) {
    CheckBox cb = new CheckBox();
    cb.setSelected(true);
    if (Rule.getValue() instanceof Boolean b) cb.setSelected(b);
    cb.setTooltip(new Tooltip("Wert: Wahr oder Falsch"));
    cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
      if (cb.isIndeterminate()) Rule.setValue(null);
      else Rule.setValue(cb.isSelected());
    });
    return cb;
  }
  @SuppressWarnings("unused") public final static IOperator<Boolean> BOOLEAN_EQUALS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Boolean Against) {
      Boolean typedValue = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Boolean bv)) typedValue = bv;
      if ((null == typedValue) && (Value instanceof Boolean bv)) typedValue = bv;

      final Boolean valueToValidate = typedValue;
      if (null == Against) return (null == valueToValidate);
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.BOOLEAN  == Feature.featureType; }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getBooleanInput(Rule); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_Equals; }
  };
  @SuppressWarnings("unused") public final static IOperator<Boolean> BOOLEAN_ANY = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Boolean Against) {
      Boolean typedValue = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Boolean bv)) typedValue = bv;
      if ((null == typedValue) && (Value instanceof Boolean bv)) typedValue = bv;
      return (null != typedValue) ? true : false;
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return (CMetamodel.FeatureType.BOOLEAN == Feature.featureType); }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return new Label(""); }
    @Override public boolean requiresInput() { return false; }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_Any; }
  };


  @SuppressWarnings("unused") public final static IOperator<Date> DATE_EQUALS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Date Against) {
      Date typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Date bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof Date bv)) typedValued = bv;

      final Date valueToValidate = typedValued;
      if (null == Against) return (null == valueToValidate);
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.DATE == Feature.featureType; }

    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      DatePicker dp = new DatePicker();
      Object val = Rule.getValue();
      if (val instanceof java.time.LocalDate ld) dp.setValue(ld);
      dp.valueProperty().addListener((obs, old, v) -> Rule.setValue(v));
      return dp;
    }

    @Override public String toString() { return CTranslations.INSTANCE.Operation_Equals; }
  };


  @SuppressWarnings("unused") public final static IOperator<Instant> DATE_TIME_EQUALS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Instant Against) {
      Instant typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Instant bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof Instant bv)) typedValued = bv;

      final Instant valueToValidate = typedValued;
      if (null == Against) return (null == valueToValidate);
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.DATE_TIME == Feature.featureType; }

    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
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

    @Override public String toString() { return CTranslations.INSTANCE.Operation_Equals; }
  };


  @SuppressWarnings("unused") public final static IOperator<Double> DECIMAL_EQUALS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Double Against) {
      Double typedValue = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Double bv)) typedValue = bv;
      if ((null == typedValue) && (Value instanceof Double bv)) typedValue = bv;

      final Double valueToValidate = typedValue;
      if (null == Against) return (null == valueToValidate);
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.DECIMAL == Feature.featureType; }

    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
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

    @Override public String toString() { return CTranslations.INSTANCE.Operation_Equals; }
  };


  @SuppressWarnings("unused") public final static IOperator<Integer> INTEGER_EQUALS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Integer Against) {
      Integer typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof Integer bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof Integer bv)) typedValued = bv;

      final Integer valueToValidate = typedValued;
      if (null == Against) return null == valueToValidate;
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.INTEGER == Feature.featureType; }

    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
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

    @Override public String toString() { return CTranslations.INSTANCE.Operation_Equals; }
  };


  private static Node getInterfaceDirectionInput(CRule Rule) {
    VBox box = new VBox(4);
    Set<CMetamodel.INTERFACE_DIRECTIONS> selected = (null != Rule.getValue()) ? (Set)Rule.getValue() : new HashSet<>();
    for (CMetamodel.INTERFACE_DIRECTIONS dir : CMetamodel.INTERFACE_DIRECTIONS.values()) {
      CheckBox cb = new CheckBox(dir.name());
      box.getChildren().add(cb);
      Object vals = Rule.getValue();
      if (vals instanceof Set<?> set && set.contains(dir)) cb.setSelected(true);
      else if (vals instanceof CMetamodel.INTERFACE_DIRECTIONS single && single == dir) cb.setSelected(true);
      cb.selectedProperty().addListener((obs, old, isSelected) -> {
        if (isSelected) selected.add(dir);
        else selected.remove(dir);
        if (selected.isEmpty()) Rule.setValue(null);
        else Rule.setValue(selected);
      });
    }
    return box;
  }
  @SuppressWarnings("unused") public final static IOperator<Set<CMetamodel.INTERFACE_DIRECTIONS>> INTERFACE_DIRECTION_CONTAINS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Set<CMetamodel.INTERFACE_DIRECTIONS> Against) {
      CMetamodel.INTERFACE_DIRECTIONS typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof CMetamodel.INTERFACE_DIRECTIONS bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof CMetamodel.INTERFACE_DIRECTIONS bv)) typedValued = bv;

      final CMetamodel.INTERFACE_DIRECTIONS valueToValidate = typedValued;
      if (null == Against) return null == valueToValidate;
      else return Against.contains(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.INTERFACE_DIRECTION == Feature.featureType; }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getInterfaceDirectionInput(Rule); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_Contains; }
  };


  private static Node getStringInput(CRule Rule) {
    TextField tf = new TextField(null == Rule.getValue() ? "" : Rule.getValue().toString());
    tf.textProperty().addListener((obs, old, v) -> {
      if (v == null || v.isBlank()) Rule.setValue(null);
      else Rule.setValue(v);
    });
    return tf;

  }
  @SuppressWarnings("unused") public final static IOperator<String> STRING_ANY = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, String Against) {
      String typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof String bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof String bv)) typedValued = bv;
      return (null != typedValued) ? true : false;
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.STRING == Feature.featureType || CMetamodel.FeatureType.RICHTEXT == Feature.featureType; }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return new Label(""); }
    @Override public boolean requiresInput() { return false; }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_Any; }
  };
  @SuppressWarnings("unused") public final static IOperator<String> STRING_EQUALS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, String Against) {
      String typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof String bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof String bv)) typedValued = bv;

      final String valueToValidate = typedValued;
      if (null == Against) return null == valueToValidate;
      else return Against.equals(valueToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.STRING == Feature.featureType || CMetamodel.FeatureType.RICHTEXT == Feature.featureType; }

    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getStringInput(Rule); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_Equals; }
  };
  @SuppressWarnings("unused") public final static IOperator<String> STRING_CONTAINS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, String Against) {
      String typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof String bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof String bv)) typedValued = bv;

      if (null == Against) return true;
      else if (null == typedValued) return false;
      else return typedValued.contains(Against);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.STRING == Feature.featureType || CMetamodel.FeatureType.RICHTEXT == Feature.featureType; }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getStringInput(Rule); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_Contains; }
  };
  @SuppressWarnings("unused") public final static IOperator<String> STRING_STARTS_WITH = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, String Against) {
      String typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof String bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof String bv)) typedValued = bv;

      if (null == Against) return true;
      else if (null == typedValued) return false;
      else return typedValued.startsWith(Against);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.STRING == Feature.featureType || CMetamodel.FeatureType.RICHTEXT == Feature.featureType; }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_StartsWith; }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getStringInput(Rule); }
  };
  @SuppressWarnings("unused") public final static IOperator<String> STRING_ENDS_WITH = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, String Against) {
      String typedValued = null;
      if ((Value instanceof Collection<?> listValue) && !listValue.isEmpty() && (listValue.iterator().next() instanceof String bv)) typedValued = bv;
      if ((null == typedValued) && (Value instanceof String bv)) typedValued = bv;

      if (null == Against) return true;
      else if (null == typedValued) return false;
      else return typedValued.endsWith(Against);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.STRING == Feature.featureType || CMetamodel.FeatureType.RICHTEXT == Feature.featureType; }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_EndsWith; }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getStringInput(Rule); }
  };


  private static Node getEnumInput(CRule Rule, CMetamodel.Feature Feature) {
    // SORT LITERALS!
    CMetamodel.EnumerationExpression enumExpression = Feature.metamodel.EnumerationExpressions.stream().filter(ee -> ee.name.equals(Feature.name)).findFirst().orElse(null);
    if (enumExpression == null || enumExpression.literals == null || enumExpression.literals.isEmpty()) return new Label("Keine Literale vorhanden");

    VBox box = new VBox(4);
    List<CheckBox> checkBoxes = new ArrayList<>();
    Set<CMetamodel.Literal> selected = new HashSet<>();
    // Vorbelegung
    Object val = Rule.getValue();
    if (val instanceof Set<?> set) set.forEach(o -> { if (o instanceof CMetamodel.Literal lit) selected.add(lit); });
    else if (val instanceof CMetamodel.Literal lit) selected.add(lit);

    VBox inner = new VBox(4);
    for (CMetamodel.Literal literal : enumExpression.literals.stream().sorted((l1, l2) -> l1.name.compareTo(l2.name)).collect(Collectors.toCollection(ArrayList::new))) {
      CheckBox cb = new CheckBox(literal.name);
      inner.getChildren().add(cb);
      if (selected.contains(literal)) cb.setSelected(true);
      cb.selectedProperty().addListener((obs, old, isSelected) -> {
        if (isSelected) selected.add(literal);
        else selected.remove(literal);
        if (selected.isEmpty()) Rule.setValue(null);
        else Rule.setValue(new HashSet<>(selected));
      });
      checkBoxes.add(cb);
    }
    ScrollPane scroll = new ScrollPane(inner);
    scroll.setFitToWidth(true);
    scroll.setPrefViewportHeight(80);
    box.getChildren().add(scroll);
    return box;
  }
  @SuppressWarnings("unused") public final static IOperator<Set<CMetamodel.Literal>> ENUM_EQUALS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Set<CMetamodel.Literal> Against) {
      Set<CMetamodel.Literal> valuesToValidate = new HashSet<>();
      if (Value instanceof Collection<?> listValue) listValue.stream().filter(v -> v instanceof CMetamodel.Literal).forEach(v -> valuesToValidate.add((CMetamodel.Literal) v));
      if (Value instanceof CMetamodel.Literal singleValue) valuesToValidate.add(singleValue);

      if (null == Against || Against.isEmpty()) return valuesToValidate.isEmpty();
      else return Against.containsAll(valuesToValidate) && valuesToValidate.containsAll(Against);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.ENUMERATION == Feature.featureType; }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getEnumInput(Rule, Feature); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_Equals; }
  };
  @SuppressWarnings("unused") public final static IOperator<Set<CMetamodel.Literal>> ENUM_CONTAINS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Set<CMetamodel.Literal> Against) {
      Set<CMetamodel.Literal> valuesToValidate = new HashSet<>();
      if (Value instanceof Collection<?> listValue) listValue.stream().filter(v -> v instanceof CMetamodel.Literal).forEach(v -> valuesToValidate.add((CMetamodel.Literal) v));
      if (Value instanceof CMetamodel.Literal singleValue) valuesToValidate.add(singleValue);

      if (null == Against || Against.isEmpty()) return valuesToValidate.isEmpty();
      return Against.stream().anyMatch(v -> valuesToValidate.contains(v));
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.ENUMERATION == Feature.featureType; }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getEnumInput(Rule, Feature); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_Contains; }
  };


  @SuppressWarnings("unused") public final static IOperator<Set<CMetamodel.SubstantialTypeExpression>> SELF_RELATION_EQUALS = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, Set<CMetamodel.SubstantialTypeExpression> Against) {
      Set<CMetamodel.SubstantialTypeExpression> valuesToValidate = new HashSet<>();
      if (Value instanceof Collection<?> listValue) listValue.stream().filter(v -> v instanceof CMetamodel.SubstantialTypeExpression).forEach(v -> valuesToValidate.add((CMetamodel.SubstantialTypeExpression) v));
      if (Value instanceof CMetamodel.SubstantialTypeExpression singleValue) valuesToValidate.add(singleValue);

      if (null == Against || Against.isEmpty()) return valuesToValidate.isEmpty();
      else return Against.containsAll(valuesToValidate);
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.SELF_RELATION == Feature.featureType; }

    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) {
      Set<CDatamodel.BuildingBlock> buildingBlocks = Data.BuildingBlocks.get(ForType).stream().sorted((bb1, bb2) -> bb1.name.compareTo(bb2.name)).collect(Collectors.toCollection(LinkedHashSet::new));
      Set<CDatamodel.BuildingBlock> selected = new HashSet<>();
      Object val = Rule.getValue();
      if (val instanceof Collection<?> list) list.forEach(o -> { if (o instanceof CDatamodel.BuildingBlock ste) selected.add(ste); });
      List<CheckBox> checkBoxes = new ArrayList<>();
      VBox box = new VBox(4);
      VBox inner = new VBox(4);
      if (buildingBlocks != null) {
        buildingBlocks.forEach(bb -> {
          String label = bb.name + " (ID: " + bb.id + ")";
          CheckBox cb = new CheckBox(label);
          inner.getChildren().add(cb);
          if (selected.stream().anyMatch(sel -> sel.equals(bb))) cb.setSelected(true);
          cb.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) selected.add(bb);
            else selected.remove(bb);
            if (selected.isEmpty()) Rule.setValue(null);
            else Rule.setValue(new HashSet<>(selected));
          });
          checkBoxes.add(cb);
        });
      }
      ScrollPane scroll = new ScrollPane(inner);
      scroll.setFitToWidth(true);
      scroll.setPrefViewportHeight(80);
      box.getChildren().add(scroll);
      return box;
    }

    @Override public String toString() { return CTranslations.INSTANCE.Operation_Equals; }
  };


  private static Node getSTEInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CDatamodel Data) {
    CMetamodel.TypeExpression te = Feature.metamodel.SubstantialTypeExpressions.stream().filter(rte -> rte.persistentName.equals(Feature.type)).findFirst().orElse(null);
    if (null != te) {
      if (null == Rule.getValue()) Rule.setValue(new CFilter(Filter.getFilterName() + " \u2192 " + Feature.name, te, new CGroup(CGroup.GroupOperator.AND)));
      CFilter filter = (CFilter)Rule.getValue();
      Button btnEdit = new Button(CTranslations.INSTANCE.Operation_Button_Complex);
      btnEdit.setOnAction(e -> {
        JFXRuleBuilderDialog dialog = new JFXRuleBuilderDialog(Data, te, filter, updated -> Rule.setValue(updated));
        dialog.showAndWait();
      });
      btnEdit.setMaxWidth(Double.MAX_VALUE);
      VBox vbox = new VBox(8, btnEdit);
      vbox.setFillWidth(true);
      return vbox;
    } else return new Label("<UNKNOWN>");
  }
  @SuppressWarnings("unused") public final static IOperator<CFilter> SUBSTANTIALTYPE_COMPLEX_ANY = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, CFilter Against) {
      if ((null != Against) && (null != Value) && (Value instanceof Collection valueList) && !valueList.isEmpty()) return valueList.stream().anyMatch(e -> Against.evaluate((CDatamodel.Element)e));
      else return false;
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.SELF_RELATION == Feature.featureType || (CMetamodel.FeatureType.RELATION == Feature.featureType && Feature.referencesBuildingblock()); }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getSTEInput(Filter, Rule, Feature, Data); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_ComplexAny; }
  };
  @SuppressWarnings("unused") public final static IOperator<CFilter> SUBSTANTIALTYPE_COMPLEX_ALL = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, CFilter Against) {
      if ((null != Against) && (null != Value) && (Value instanceof Collection valueList) && !valueList.isEmpty()) return valueList.stream().allMatch(e -> Against.evaluate((CDatamodel.Element)e));
      else return false;
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.SELF_RELATION == Feature.featureType || (CMetamodel.FeatureType.RELATION == Feature.featureType && Feature.referencesBuildingblock()); }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getSTEInput(Filter, Rule, Feature, Data); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_ComplexAll; }
  };


  private static Node getRTEInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CDatamodel Data) {
    CMetamodel.TypeExpression te = Feature.metamodel.RelationshipTypeExpressions.stream().filter(rte -> rte.persistentName.equals(Feature.type)).findFirst().orElse(null);
    if (null != te) {
      if (null == Rule.getValue()) Rule.setValue(new CFilter(Filter.getFilterName() + " \u2192 " + Feature.name, te, new CGroup(CGroup.GroupOperator.AND)));
      CFilter filter = (CFilter)Rule.getValue();
      Button btnEdit = new Button(CTranslations.INSTANCE.Operation_Button_Complex);
      btnEdit.setOnAction(e -> {
        JFXRuleBuilderDialog dialog = new JFXRuleBuilderDialog(Data, te, filter, updated -> Rule.setValue(updated));
        dialog.showAndWait();
      });
      btnEdit.setMaxWidth(Double.MAX_VALUE);
      VBox vbox = new VBox(8, btnEdit);
      vbox.setFillWidth(true);
      return vbox;
    } else return new Label("<UNKNOWN>");
  }
  @SuppressWarnings("unused") public final static IOperator<CFilter> RELATION_COMPLEX_ANY = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, CFilter Against) {
      if ((null != Against) && (null != Value) && (Value instanceof Collection valueList) && !valueList.isEmpty()) return valueList.stream().anyMatch(e -> Against.evaluate((CDatamodel.Element)e));
      else return false;
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.RELATION == Feature.featureType && !Feature.referencesBuildingblock(); }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getRTEInput(Filter, Rule, Feature, Data); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_ComplexAny; }
  };
  @SuppressWarnings("unused") public final static IOperator<CFilter> RELATION_COMPLEX_ALL = new IOperator<>() {
    @Override public boolean evaluate(Object Value, CMetamodel.Feature OfFeature, CFilter Against) {
      if ((null != Against) && (null != Value) && (Value instanceof Collection valueList) && !valueList.isEmpty()) return valueList.stream().allMatch(e -> Against.evaluate((CDatamodel.Element)e));
      else return false;
    }

    @Override public boolean compatibleWith(CMetamodel.Feature Feature) { return CMetamodel.FeatureType.RELATION == Feature.featureType && !Feature.referencesBuildingblock(); }
    @Override public Node getInput(CFilter Filter, CRule Rule, CMetamodel.Feature Feature, CMetamodel.TypeExpression ForType, CDatamodel Data) { return getRTEInput(Filter, Rule, Feature, Data); }
    @Override public String toString() { return CTranslations.INSTANCE.Operation_ComplexAll; }
  };





  @SuppressWarnings("rawtypes") public static Set<IOperator> getSupportedOperators(CMetamodel.Feature Feature) {
    Set<IOperator> result = new HashSet<>();
    for (Field f : Operators.class.getDeclaredFields()) {
      try {
        Object o = f.get(null);
        if (o instanceof IOperator operator) {
          if (operator.compatibleWith(Feature)) result.add(operator);
        }
      } catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }
    return result;
  }
}
