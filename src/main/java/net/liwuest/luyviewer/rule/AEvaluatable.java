package net.liwuest.luyviewer.rule;

import net.liwuest.luyviewer.model.CDatamodel;

abstract class AEvaluatable<T extends AEvaluatable> {
  public abstract boolean evaluate(CDatamodel.Element Element);
  public abstract boolean isValid();
  public abstract T copy();
}
