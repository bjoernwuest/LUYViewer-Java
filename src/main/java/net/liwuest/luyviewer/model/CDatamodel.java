package net.liwuest.luyviewer.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class CDatamodel {
  /**
   * Parses a date and time string into a {@link java.time.Instant} object.
   * This function attempts to parse the input string using several common date-time formats.
   * If no format matches, an IllegalArgumentException is thrown.
   *
   * @param dateTimeString The date and time string to parse.
   * @return An {@link Instant} representing the parsed date and time.
   * @throws IllegalArgumentException If the input string cannot be parsed by any of the predefined formats.
   */
  static Instant parseToInstant(String dateTimeString) {
    if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
      throw new IllegalArgumentException("Input date-time string cannot be null or empty.");
    }

    // Define a list of common date-time formatters to try
    // Note: ZoneOffset.UTC is used as a default if no timezone information is present
    // in the string, to convert LocalDateTime to Instant.
    DateTimeFormatter[] formatters = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC), // 2023-01-15 14:30:00
        DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy").withZone(ZoneOffset.UTC), // 14:30:00 15-01-2023
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneOffset.UTC), // 15.01.2023 14:30:00
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneOffset.UTC), // 2023/01/15 14:30:00
        DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss").withZone(ZoneOffset.UTC), // 01-15-2023 14:30:00
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC), // ISO-like without Z or offset
        DateTimeFormatter.ISO_INSTANT, // Handles "2023-01-15T14:30:00Z" or "2023-01-15T14:30:00+01:00"
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC) // Handles "2023-01-15T14:30:00"
    };

    for (DateTimeFormatter formatter : formatters) {
      try {
        // Attempt to parse directly to Instant if the formatter supports it (e.g., ISO_INSTANT)
        return Instant.from(formatter.parse(dateTimeString));
      } catch (DateTimeParseException e) {
        // If parsing to Instant fails, try parsing to LocalDateTime and then convert
        try {
          LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
          return localDateTime.toInstant(ZoneOffset.UTC); // Assuming UTC if no timezone is specified
        } catch (DateTimeParseException ignored) {
          // Continue to the next formatter if this one fails
        }
      }
    }

    throw new IllegalArgumentException("Could not parse date-time string: \"" + dateTimeString + "\" with any known format.");
  }

  public abstract static class Element implements Comparable<Element> {
    public final int id;
    public final String elementURI;
    public final Instant lastModificationTime;
    public final String lastModificationUser;
    public final Map<String, Object> AdditionalData;
    public final Map<CMetamodel.Feature, List<CMetamodel.Literal>> Enumerations = new HashMap<>();
    public final Map<CMetamodel.Feature, List<Element>> Relationships = new HashMap<>();

    Element(Map<String, Object> Data, CMetamodel.TypeExpression Type, CMetamodel Metamodel) {
      List<Object> idData = (List)Data.getOrDefault("id", new ArrayList<>());
      id = idData.isEmpty() ? -1 : Integer.parseInt(idData.get(0).toString()); Data.put("id", id);
      elementURI = Data.getOrDefault("elementURI", "<UNKNOWN>").toString(); Data.put("elementURI", elementURI);
      List<Object> lmtData = (List)Data.getOrDefault("lastModificationTime", new ArrayList<>());
      lastModificationTime = parseToInstant(lmtData.isEmpty() ? "1970-01-01 00:00:00" : lmtData.get(0).toString()); Data.put("lastModificationTime", lastModificationTime);
      List<Object> lmuData = (List)Data.getOrDefault("lastModificationUser", new ArrayList<>());
      lastModificationUser = lmuData.isEmpty() ? "<UNKNOWN>>" : lmuData.get(0).toString(); Data.put("lastModificationUser", lastModificationUser);

      // Process enumeration attributes
      Iterator<Map.Entry<String, Object>> iter = Data.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<String, Object> dataEntry = iter.next();
        Type.features.stream().filter(f -> (CMetamodel.FeatureType.ENUMERATION == f.featureType) && f.persistentName.equals(dataEntry.getKey())).findFirst().ifPresent(f -> {
          Enumerations.putIfAbsent(f, new ArrayList<>());
          if ((dataEntry.getValue() instanceof List valueList) && !valueList.isEmpty()) {
            Metamodel.EnumerationExpressions.stream().filter(ee -> ee.persistentName.equals(f.type)).findFirst().ifPresent(ee -> {
              valueList.forEach(value -> {
                ee.literals.stream().filter(l -> l.persistentName.equals(value.toString())).findFirst().ifPresent(literal -> Enumerations.get(f).add(literal));
              });
            });
          }
          dataEntry.setValue(Enumerations.get(f));
        });
      }

      AdditionalData = Data;
      // Transform types of additional data
      AdditionalData.forEach((k, v) -> {
        Type.features.stream().filter(f -> f.persistentName.equals(k)).findFirst().ifPresent(feature -> {
          if (v instanceof List valueList) AdditionalData.put(k, valueList.stream().map(value -> {
            switch (feature.featureType) {
              case BOOLEAN: return Boolean.parseBoolean(value.toString());
              case DATE: try { return CDatamodel.parseToInstant(value.toString()); } catch (Exception e) { return null; }
              case DATE_TIME: try { return CDatamodel.parseToInstant(value.toString()); } catch (Exception e) { return null; }
              case DECIMAL: return Double.parseDouble(value.toString());
              case INTEGER: return Integer.parseInt(value.toString());
              case RICHTEXT: return value;
              case STRING: return value;
              case INTERFACE_DIRECTION: return CMetamodel.INTERFACE_DIRECTIONS.valueOf(value.toString());
              case ENUMERATION: return value;
              case RELATION: return value;
              case SELF_RELATION: return value;
              default: {
                System.out.println("CDatamodel -> Element -> parse additional data, unhandled feature type: " + feature.type + " with value: " + value);
                return value;
              }
            }
          }).toList());
        });
      });
    }

    abstract Set<CMetamodel.Feature> getFeatures();
    final void parseRelationships(CDatamodel datamodel) {
      for (CMetamodel.Feature feature : getFeatures()) {
        if ((CMetamodel.FeatureType.RELATION == feature.featureType) || (CMetamodel.FeatureType.SELF_RELATION == feature.featureType)) {
          Relationships.putIfAbsent(feature, new ArrayList<Element>());
          if (AdditionalData.get(feature.persistentName) instanceof List data) {
            data.forEach(e -> {
              if (e instanceof Map element) {
                Element luyElement = datamodel.lookupById(Integer.parseInt(element.getOrDefault("id", "-1").toString()));
                if (null != luyElement) Relationships.get(feature).add(luyElement);
                else System.out.println(feature.persistentName);
              }
            });
            AdditionalData.put(feature.persistentName, Relationships.get(feature));
          }
        }
      }
    }

    /**
     * Returns the maximum number of values for a single feature.
     * @return Maximum number of values for a single feature.
     */
    public final int getMaxValues() {
      return AdditionalData.values().stream().map(v -> {
        if (v instanceof Collection c) return Math.max(1, c.size());
        else return 1;
      }).max((x, y) -> Integer.compare(x, y)).orElse(1);
    }

    @Override public int compareTo(Element o) { return Integer.compare(this.id, o.id); }
  }

  public final static class BuildingBlock extends Element {
    public final CMetamodel.SubstantialTypeExpression metamodelType;
    public final int hierarchy_level;
    public final String name;
    public final String description;
    public final int position;
    BuildingBlock(Map<String, Object> Data, CMetamodel.SubstantialTypeExpression Type, CMetamodel Metamodel) {
      super(Data, Type, Metamodel);
      List<Object> hierarchy_levelData = (List)Data.getOrDefault("$$hierarchy_level$$", new ArrayList<>());
      hierarchy_level = hierarchy_levelData.isEmpty() ? -1 : Integer.parseInt(hierarchy_levelData.get(0).toString()); Data.put("$$hierarchy_level$$", hierarchy_level);
      List<Object> nameData = (List)Data.getOrDefault("name", new ArrayList<>());
      name = nameData.isEmpty() ? "<UNKNOWN>" : nameData.get(0).toString(); Data.put("name", name);
      List<Object> descriptionData = (List)Data.getOrDefault("description", new ArrayList<>());
      description = descriptionData.isEmpty() ? "" : descriptionData.get(0).toString(); Data.put("description", description);
      List<Object> positionData = (List)Data.getOrDefault("position", new ArrayList<>());
      position = positionData.isEmpty() ? -1 : Integer.parseInt(positionData.get(0).toString()); Data.put("position", position);
      metamodelType = Type;
    }

    @Override Set<CMetamodel.Feature> getFeatures() { return metamodelType.features; }
  }

  public final static class Relationship extends Element {
    public final CMetamodel.RelationshipTypeExpression metamodelType;

    Relationship(Map<String, Object> Data, CMetamodel.RelationshipTypeExpression Type, CMetamodel Metamodel) {
      super(Data, Type, Metamodel);
      metamodelType = Type;
    }

    @Override Set<CMetamodel.Feature> getFeatures() { return metamodelType.features; }
  }

  Element lookupById(int Id) {
    List<? extends Element> r = BuildingBlocks.values().stream().map(l -> l.stream().filter(e -> e.id == Id).toList()).filter(l -> !l.isEmpty()).findFirst().orElse(null);
    if (null == r) r = Relationships.values().stream().map(l -> l.stream().filter(e -> e.id == Id).toList()).filter(l -> !l.isEmpty()).findFirst().orElse(null);
    if (null != r) return r.get(0); else return null;
  }

  public final Map<CMetamodel.SubstantialTypeExpression, Set<BuildingBlock>> BuildingBlocks = new TreeMap<>();
  public final Map<CMetamodel.RelationshipTypeExpression, Set<Relationship>> Relationships = new TreeMap<>();
  public final CMetamodel Metamodel;

  private CDatamodel(CMetamodel Metamodel) { this.Metamodel = Metamodel; }

  public static CDatamodel load(String filename) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    CDatamodel datamodel = new CDatamodel(CMetamodel.load(filename));

    // Parse the JSON file as an array of objects
    Map<String, Object>[] items = objectMapper.readValue(new File(filename + "_data.json"), Map[].class);

    // Create elements
    for (Map<String, Object> item : items) {
      String typeExpressionName = item.getOrDefault("query", "<UNKNOWN>").toString();
      CMetamodel.TypeExpression typeExpression = datamodel.Metamodel.SubstantialTypeExpressions.stream().filter(ste -> ste.persistentName.equals(typeExpressionName)).map(ste -> (CMetamodel.TypeExpression)ste).findFirst().orElseGet(() -> datamodel.Metamodel.RelationshipTypeExpressions.stream().filter(rte -> rte.persistentName.equals(typeExpressionName)).findFirst().get());
      if (item.getOrDefault("result", new ArrayList<>()) instanceof List elements) {
        for (Object element : elements) {
          if (element instanceof Map rawElementData) {
            // Make copy to be able to delete "processed" data
            Map<String, Object> elementData = new TreeMap<>(rawElementData);
            Element e = (typeExpression instanceof CMetamodel.SubstantialTypeExpression) ? new BuildingBlock(elementData, (CMetamodel.SubstantialTypeExpression)typeExpression, datamodel.Metamodel) : new Relationship(elementData, (CMetamodel.RelationshipTypeExpression)typeExpression, datamodel.Metamodel);

            if ((e instanceof BuildingBlock bb) && (typeExpression instanceof CMetamodel.SubstantialTypeExpression ste)) {
              datamodel.BuildingBlocks.putIfAbsent(ste, new TreeSet<>());
              datamodel.BuildingBlocks.get(ste).add(bb);
            }
            if ((e instanceof Relationship rel) && (typeExpression instanceof CMetamodel.RelationshipTypeExpression rte)) {
              datamodel.Relationships.putIfAbsent(rte, new TreeSet<>());
              datamodel.Relationships.get(rte).add(rel);
            }
          }
        }
      };
    }

    // Resolve all kind of relations
    datamodel.BuildingBlocks.values().forEach(bbList -> bbList.forEach(bb -> bb.parseRelationships(datamodel)));
    datamodel.Relationships.values().forEach(relList -> relList.forEach(rel -> rel.parseRelationships(datamodel)));

    return datamodel;
  }
}
