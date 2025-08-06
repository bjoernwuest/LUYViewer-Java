package net.liwuest.luyviewer.rule;

import net.liwuest.luyviewer.model.CDatamodel;
import net.liwuest.luyviewer.model.CMetamodel;
import net.liwuest.luyviewer.util.CEventBus;

import java.util.HashSet;
import java.util.Set;

public final class CRule extends AEvaluatable<CRule> {
  private CMetamodel.Feature m_Feature;
  private Operators.IOperator m_Operator;
  private Set<Object> m_Values;

  public CRule() { this(null, null, null); }
  public CRule(Set<Object> Values) { this(null, null, Values); }
  public CRule(Operators.IOperator Operator) { this(null, Operator, null); }
  public CRule(CMetamodel.Feature Feature) { this(Feature, null, null); }
  public CRule(Operators.IOperator Operator, Set<Object> Values) { this(null, Operator, Values); }
  public CRule(CMetamodel.Feature Feature, Set<Object> Values) { this(Feature, null, Values); }
  public CRule(CMetamodel.Feature Feature, Operators.IOperator Operator) { this(Feature, Operator, null); }
  public CRule(CMetamodel.Feature Feature, Operators.IOperator Operator, Set<Object> Values) {
    if ((null != Feature) && (null != Operator)) assert Operator.compatibleWith(Feature.featureType);
    m_Feature = Feature;
    m_Operator = Operator;
    m_Values = Values;
  }

  public CMetamodel.Feature getFeature() { return m_Feature; }
  public CRule setFeature(CMetamodel.Feature Feature) {
    if (null != Feature) {
      if (null != m_Operator) assert m_Operator.compatibleWith(Feature.featureType);
      m_Feature = Feature;
      CEventBus.publish(new EventEvaluatableChanged());
    }
    return this;
  }
  public Operators.IOperator getOperator() { return m_Operator; }
  public CRule setOperator(Operators.IOperator Operator) {
    if (null != Operator) {
      if (null != m_Feature) assert Operator.compatibleWith(m_Feature.featureType);
      m_Operator = Operator;
      CEventBus.publish(new EventEvaluatableChanged());
    }
    return this;
  }
  public Set<Object> getValues() { return new HashSet<>(m_Values); }
  public CRule addValue(Object Value) { assert null != Value; m_Values.add(Value); return this; }
  public CRule setValues(Set<Object> Values) { assert null != Values; m_Values = Values; return this; }

  @Override public boolean evaluate(CDatamodel.Element Element) { if ((null == Element) || (null == m_Operator)) return true; return m_Operator.evaluate(Element.AdditionalData.get(m_Feature.persistentName), m_Values); }
  @Override public boolean isValid() { return (null != m_Feature) && (null != m_Operator) && (null != m_Values); }
  @Override public CRule copy() {
    if (null == m_Feature) {
      if (null == m_Operator) return new CRule((Set<Object>)m_Values);
      else return new CRule(m_Operator, m_Values);
    } else {
      if (null == m_Operator) return new CRule(m_Feature, (Set<Object>)m_Values);
      else return new CRule(m_Feature, m_Operator, m_Values);
    }
  }
}
