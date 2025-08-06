package net.liwuest.luyviewer.model;

import net.liwuest.luyviewer.rule.CFilter;
import net.liwuest.luyviewer.util.CEventBus;

import java.util.*;
import java.util.stream.Collectors;

public final class CFilteredAndSortedDatamodel {
  public enum SortOrder {
    ASCENDING,
    DESCENDING,
    UNSORTED;
  }

  public final class DatasortingChanged implements CEventBus.AbstractEvent {}
  public final class FeatureHidden implements CEventBus.AbstractEvent {
    public final CMetamodel.Feature Feature;
    public FeatureHidden(CMetamodel.Feature Feature) { this.Feature = Feature; }
  }
  public final class FeatureUnhidden implements CEventBus.AbstractEvent {
    public final CMetamodel.Feature Feature;
    public FeatureUnhidden(CMetamodel.Feature Feature) { this.Feature = Feature; }
  }

//  public final class DataFiltered implements CEventBus.AbstractEvent {}

  private final CDatamodel m_Data;
  private final Map<CMetamodel.TypeExpression, LinkedHashMap<CMetamodel.Feature, SortOrder>> m_Orderings = new HashMap<>();
  private final Map<CMetamodel.TypeExpression, Set<CMetamodel.Feature>> m_HiddenFeatures = new HashMap<>();
  private final Map<CMetamodel.TypeExpression, LinkedHashSet<? extends CDatamodel.Element>> cachedFilteredAndSortedData = new HashMap<>();
  private Map<CMetamodel.TypeExpression, CFilter> m_Filter = new HashMap<>();

  public CFilteredAndSortedDatamodel(CDatamodel Data) {
    assert null != Data;
    m_Data = Data;
  }

  public synchronized LinkedHashSet<CMetamodel.TypeExpression> getTypes() { return m_Data.Metamodel.SubstantialTypeExpressions.stream().sorted((ste1, ste2) -> ste1.name.compareTo(ste2.name)).collect(Collectors.toCollection(LinkedHashSet::new)); }

  /**
   * Sorts the data model by the specified feature and sort order.
   * First removes any existing sorting for the feature, if present.
   * If the sort order is not UNSORTED, the new sorting is added as the first entry.
   *
   * @param Feature the feature to sort by
   * @param Order   the sort order (ASCENDING, DESCENDING, UNSORTED)
   * @return        the current instance of CFilteredAndSortedDatamodel (for method chaining)
   */
  public synchronized CFilteredAndSortedDatamodel sort(CMetamodel.TypeExpression Type, CMetamodel.Feature Feature, SortOrder Order) {
    if ((null != Type) && (null != Feature) && (null != Order)) {
      m_Orderings.putIfAbsent(Type, new LinkedHashMap<>());
      cachedFilteredAndSortedData.put(Type, null);
      m_Orderings.get(Type).remove(Feature);
      if ((SortOrder.UNSORTED != Order) && Feature.isSortable) {
        m_Orderings.get(Type).putLast(Feature, Order);
        CEventBus.publish(new DatasortingChanged());
      }
    }
    return this;
  }

  public synchronized Set<CMetamodel.Feature> getFeaturesOfRelation(CMetamodel.Feature RelationFeature) {
    if ((null == RelationFeature) || (CMetamodel.FeatureType.SELF_RELATION == RelationFeature.featureType) || !(CMetamodel.FeatureType.RELATION == RelationFeature.featureType)) return null;
    CMetamodel.RelationshipTypeExpression relationshipType = RelationFeature.getRTE();
    if (null != relationshipType) return relationshipType.features;
    return null;
  }

  public synchronized CFilteredAndSortedDatamodel hideFeature(CMetamodel.TypeExpression Type, CMetamodel.Feature Feature) {
    if ((null != Feature) && ("name" != Feature.persistentName) && ("id" != Feature.persistentName)) {
      m_HiddenFeatures.putIfAbsent(Type, new HashSet<>());
      if (m_HiddenFeatures.get(Type).add(Feature)) {
        cachedFilteredAndSortedData.put(Type, null);
        CEventBus.publish(new FeatureHidden(Feature));
      }
    }
    return this;
  }

  public synchronized CFilteredAndSortedDatamodel showFeature(CMetamodel.TypeExpression Type, CMetamodel.Feature Feature) {
    if (null != Feature) {
      if (m_HiddenFeatures.getOrDefault(Type, new HashSet()).remove(Feature)) {
        cachedFilteredAndSortedData.put(Type, null);
        CEventBus.publish(new FeatureUnhidden(Feature));
      }
    }
    return this;
  }

  public synchronized CFilter getFilter(CMetamodel.TypeExpression ForType) { return m_Filter.get(ForType); }
  public synchronized CFilteredAndSortedDatamodel setFilter(CFilter Filter, CMetamodel.TypeExpression ForType) { assert null != Filter; assert null != ForType; m_Filter.put(ForType, Filter); cachedFilteredAndSortedData.put(ForType, null); return this; }

  public synchronized LinkedHashSet<CMetamodel.Feature> getOrderedFeatures(CMetamodel.TypeExpression Type) {
    if (null == Type) return null;
    return Type.features.stream().filter(f -> !m_HiddenFeatures.getOrDefault(Type, new HashSet()).contains(f.persistentName)).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public synchronized LinkedHashSet<? extends CDatamodel.Element> getFilteredAndSortedData(CMetamodel.TypeExpression Type) {
    if (null == Type) return null;

    if (null == cachedFilteredAndSortedData.get(Type)) {
      Map.Entry<? extends CMetamodel.TypeExpression, ? extends Set<? extends CDatamodel.Element>> resultData = m_Data.BuildingBlocks.entrySet().stream().filter(e -> e.getKey().persistentName.equals(Type.persistentName)).findFirst().orElse(null);
      if (null == resultData)
        resultData = m_Data.Relationships.entrySet().stream().filter(e -> e.getKey().persistentName.equals(Type.persistentName)).findFirst().orElse(null);
      if (null == resultData) return null;

      LinkedHashSet<CMetamodel.Feature> filteredOrderingFeatures = getOrderedFeatures(Type);
      LinkedHashSet<? extends CDatamodel.Element> result = new LinkedHashSet<>(resultData.getValue());
      // Apply building block filter
      CFilter filter = m_Filter.get(Type);
      if (null != filter) result = result.stream().filter(e -> filter.evaluate(e)).collect(Collectors.toCollection(LinkedHashSet::new));

      // Apply sorting by feature
      for (Map.Entry<CMetamodel.Feature, SortOrder> e : m_Orderings.getOrDefault(Type, new LinkedHashMap<>()).entrySet()) {
        if (filteredOrderingFeatures.contains(e.getKey())) {
          result = result.stream().sorted((bb1, bb2) -> {
            int comparisonResult = 0;
            // Try a natural comparison of scalars
            Object o1 = bb1.AdditionalData.getOrDefault(e.getKey().persistentName, null);
            Object o2 = bb2.AdditionalData.getOrDefault(e.getKey().persistentName, null);
            if ((o1 instanceof Comparable c1) && (o2 instanceof Comparable c2)) comparisonResult = c1.compareTo(c2);
            else if ((o1 instanceof List l1) && (o2 instanceof List l2)) {
              comparisonResult = Integer.compare(l1.size(), l2.size());
              if ((0 == comparisonResult) && !l1.isEmpty() && (l1.get(0) instanceof Comparable c1) && (l2.get(0) instanceof Comparable c2))
                comparisonResult = c1.compareTo(c2);
            } else {
              // Also, enumerations can be compared
              if ((CMetamodel.FeatureType.ENUMERATION == e.getKey().featureType) && (o1 instanceof List l1) && (o2 instanceof List l2)) {
                // An empty list of enumeration is usually smaller
                comparisonResult = Integer.valueOf(l1.size()).compareTo(l2.size());
                if ((0 == comparisonResult) && (0 < l1.size())) {
                  // If both lists contain exactly one element, compare by this literal
                  if ((l1.get(0) instanceof Comparable c1) && (l2.get(0) instanceof Comparable c2))
                    comparisonResult = c1.compareTo(c2);
                }
              }
            }
            return (SortOrder.ASCENDING == e.getValue()) ? comparisonResult : Math.negateExact(comparisonResult);
          }).collect(Collectors.toCollection(LinkedHashSet::new));
        }
      }
      cachedFilteredAndSortedData.put(Type, result);
    }
    return cachedFilteredAndSortedData.get(Type);
  }
}
