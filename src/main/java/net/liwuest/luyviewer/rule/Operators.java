package net.liwuest.luyviewer.rule;

import net.liwuest.luyviewer.model.CMetamodel;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class Operators {
  interface IOperator {
    /**
     * Evaluate the value against the given Against values using this operator.
     *
     * @param Value The value to evaluation.
     * @param Against The values to evaluate against.
     * @return {@code true} if the value satisfies the values to evaluate against by this operator, {@code false} otherwise.
     */
    public boolean evaluate(Object Value, Set<Object> Against);

    /**
     * Checks if this operator is compatible to the given feature type.
     *
     * @param FeatureType The feature type to check.
     * @return {@code true} if this operator can be used with the feature type, {@code false} otherwise.
     */
    public boolean compatibleWith(CMetamodel.FeatureType FeatureType);
  }

  public final static IOperator EQUALS = new IOperator() {
    @Override public boolean evaluate(Object Value, Set<Object> Against) {
      if (Value instanceof Collection valueList) return valueList.stream().allMatch(v -> {
        if (null != v) return Against.stream().anyMatch(a -> a.equals(v.toString()));
        else return Against.stream().anyMatch(a -> a.equals(v));
      });
      else if (null != Value) return Against.stream().anyMatch(a -> a.equals(Value.toString()));
      else return Against.stream().anyMatch(a -> a.equals(Value));
    }

    @Override public boolean compatibleWith(CMetamodel.FeatureType FeatureType) {
      return switch (FeatureType) {
        case BOOLEAN -> true;
        case INTEGER -> true;
        case RICHTEXT -> true;
        case STRING -> true;
        default -> false;
      };
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
