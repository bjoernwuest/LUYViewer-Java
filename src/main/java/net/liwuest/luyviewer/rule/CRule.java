package net.liwuest.luyviewer.rule;

import net.liwuest.luyviewer.model.CDatamodel;
import net.liwuest.luyviewer.model.CMetamodel;
import net.liwuest.luyviewer.util.CEventBus;

public final class CRule extends AEvaluatable<CRule> {
  private CMetamodel.Feature m_Feature;
  private Operators.IOperator m_Operator;
  private Object m_Value;

  public CRule() { this(null, null, null); }
  public CRule(Object Value) { this(null, null, Value); }
  public CRule(Operators.IOperator Operator) { this(null, Operator, null); }
  public CRule(CMetamodel.Feature Feature) { this(Feature, null, null); }
  public CRule(Operators.IOperator Operator, Object Value) { this(null, Operator, Value); }
  public CRule(CMetamodel.Feature Feature, Object Value) { this(Feature, null, Value); }
  public CRule(CMetamodel.Feature Feature, Operators.IOperator Operator) { this(Feature, Operator, null); }
  public CRule(CMetamodel.Feature Feature, Operators.IOperator Operator, Object Value) {
    if ((null != Feature) && (null != Operator)) assert Operator.compatibleWith(Feature.featureType);
    m_Feature = Feature;
    m_Operator = Operator;
    m_Value = Value;
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
  public Object getValue() { return m_Value; }
  public CRule setValue(Object Values) { m_Value = Values; CEventBus.publish(new EventEvaluatableChanged()); return this; }

  @Override public boolean evaluate(CDatamodel.Element Element) { if ((null == Element) || (null == m_Operator)) return true; return m_Operator.evaluate(Element.AdditionalData.get(m_Feature.persistentName), m_Feature, m_Value); }
  @Override public boolean isValid() { return (null != m_Feature) && (null != m_Operator) && (!m_Operator.requiresInput() || null != m_Value); }
  @Override public CRule copy() {
    if (null == m_Feature) {
      if (null == m_Operator) return new CRule(m_Value);
      else return new CRule(m_Operator, m_Value);
    } else {
      if (null == m_Operator) return new CRule(m_Feature, m_Value);
      else return new CRule(m_Feature, m_Operator, m_Value);
    }
  }
}
