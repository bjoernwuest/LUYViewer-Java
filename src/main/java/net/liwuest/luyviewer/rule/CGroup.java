package net.liwuest.luyviewer.rule;

import net.liwuest.luyviewer.model.CDatamodel;
import net.liwuest.luyviewer.util.CEventBus;

import java.util.LinkedHashSet;

public final class CGroup extends AEvaluatable<CGroup> {
  public enum GroupOperator {
    AND,
    OR,
    NOT
  }

  private GroupOperator m_Operator;
  private LinkedHashSet<AEvaluatable> m_Rules = new LinkedHashSet<>();

  public CGroup(GroupOperator Operator) {
    assert null != Operator;
    m_Operator = Operator;
  }

  public GroupOperator getOperator() { return m_Operator; }
  public CGroup setOperator(GroupOperator Operator) {
    if (null != Operator) {
      m_Operator = Operator;
      if ((GroupOperator.NOT == Operator) && (1 < m_Rules.size())) {
        AEvaluatable firstRule = m_Rules.getFirst();
        m_Rules.clear();
        m_Rules.add(firstRule);
      }
      CEventBus.publish(new EventEvaluatableChanged());
    }
    return this;
  }

  public LinkedHashSet<AEvaluatable> getRules() { return new LinkedHashSet<>(m_Rules); }
  public CGroup addRule(AEvaluatable Rule) { return addRuleLast(Rule); }
  public CGroup addRuleFirst(AEvaluatable Rule) { if (null != Rule) m_Rules.addFirst(Rule); CEventBus.publish(new EventEvaluatableChanged()); return this; }
  public CGroup addRuleLast(AEvaluatable Rule) { if (null != Rule) m_Rules.addLast(Rule); CEventBus.publish(new EventEvaluatableChanged()); return this; }
  public CGroup addRuleAtIndex(AEvaluatable Rule, int Index) {
    if (null != Rule) {
      AEvaluatable[] evals = m_Rules.toArray(new AEvaluatable[0]);
      m_Rules.clear();
      for (int i = 0; i < Math.max(0, Math.min(Index, evals.length)); i++) { m_Rules.add(evals[i]); }
      m_Rules.add(Rule);
      for (int i = Math.max(0, Math.min(Index, evals.length)); i < evals.length; i++) { m_Rules.add(evals[i]); }
    }
    CEventBus.publish(new EventEvaluatableChanged());
    return this;
  }
  public CGroup removeRule(AEvaluatable Rule) { m_Rules.remove(Rule); CEventBus.publish(new EventEvaluatableChanged()); return this; }

  @Override public boolean evaluate(final CDatamodel.Element Element) {
    if (m_Rules.isEmpty()) return true;

    return switch (m_Operator) {
      case GroupOperator.NOT -> !m_Rules.getFirst().evaluate(Element);
      case GroupOperator.AND -> m_Rules.stream().allMatch(r -> r.evaluate(Element));
      case GroupOperator.OR -> m_Rules.stream().anyMatch(r -> r.evaluate(Element));
    };
  }
  @Override public boolean isValid() { return m_Rules.stream().allMatch(AEvaluatable::isValid); }
  @Override public CGroup copy() {
    CGroup result = new CGroup(m_Operator);
    m_Rules.forEach(rule -> result.addRule(rule.copy()));
    return result;
  }
}
