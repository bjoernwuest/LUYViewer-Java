package net.liwuest.luyviewer.rule;

import net.liwuest.luyviewer.model.CDatamodel;
import net.liwuest.luyviewer.model.CMetamodel;

public final class CFilter extends AEvaluatable<CFilter> {
  private String m_FilterName;
  private CMetamodel.TypeExpression m_TypeExpression;
  private CGroup m_Group;

  public CFilter(String Filtername, CMetamodel.TypeExpression TypeExpression) { this(Filtername, TypeExpression, new CGroup(CGroup.GroupOperator.AND)); }
  public CFilter(String Filtername, CMetamodel.TypeExpression TypeExpression, CGroup Group) {
    assert null != TypeExpression;
    assert null != Group;
    m_FilterName = Filtername;
    m_TypeExpression = TypeExpression;
    m_Group = Group;
  }

  public String getFilterName() { return m_FilterName; }
  public CFilter setFilterName(String FilterName) { assert null != FilterName; m_FilterName = FilterName; return this; }

  public CMetamodel.TypeExpression getTypeExpression() { return m_TypeExpression; }
  public CGroup getRootGroup() { return m_Group; }

  @Override public boolean evaluate(CDatamodel.Element Element) {
    if (null == m_Group) return true;
    else return m_Group.evaluate(Element);
  }
  @Override public boolean isValid() { return (m_TypeExpression != null) && m_Group.isValid(); }
  @Override public CFilter copy() {
    CFilter result = new CFilter(m_FilterName, m_TypeExpression, m_Group);
    return result;
  }
}
