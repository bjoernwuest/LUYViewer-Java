
package net.liwuest.luyviewer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.liwuest.luyviewer.LUYViewer;
import net.liwuest.luyviewer.util.CTranslations;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Represents a metamodel structure that can be loaded from JSON using Jackson.
 */
public class CMetamodel {
    public enum FeatureType {
        BOOLEAN,
        DATE,
        DATE_TIME,
        DECIMAL,
        INTEGER,
        RICHTEXT,
        STRING,
        INTERFACE_DIRECTION,
        ENUMERATION,
        RELATION,
        SELF_RELATION,
        BUILDING_BLOCK_TYPE
    }
    public final static List<String> NonRelationshipFeatureNames = Arrays.asList("boolean", "date", "date_time", "decimal", "integer", "io.luy.model.Direction", "richtext", "string");
    public final static List<String> SelfRelationFeatureNames = Arrays.asList("children", "parent", "baseComponents", "parentComponents", "predecessors", "successors", "generalisation", "specialisations");

    public enum INTERFACE_DIRECTIONS {
        NO_DIRECTION,
        FIRST_TO_SECOND,
        SECOND_TO_FIRST,
        BOTH_DIRECTIONS
    }

    abstract static class BasicExpression implements Comparable<BasicExpression> {
        public final CMetamodel metamodel;
        public final String type;
        public final String persistentName;
        public final String name;
        public final String pluralName;
        public final String description;

        BasicExpression(CMetamodel Metamodel, Map<String, Object> Data) {
            metamodel = Metamodel;
            type = Data.getOrDefault("type", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            persistentName = Data.getOrDefault("persistentName", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            name = Data.getOrDefault("name", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            pluralName = Data.getOrDefault("pluralName", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            description = Data.getOrDefault("description", CTranslations.INSTANCE.Unknown_Placeholder).toString();
        }

        @Override public int compareTo(BasicExpression Other) { return this.name.compareTo(Other.name); }
        @Override public String toString() { return name; }
    }

    public final static class Feature implements Comparable<Feature> {
        public final CMetamodel metamodel;
        public final FeatureType featureType;
        public final String type;
        public final String persistentName;
        public final String name;
        public final String pluralName;
        public final String description;
        public final boolean mandatory;
        public final boolean multiple;
        public final boolean isSortable;

        Feature(CMetamodel Metamodel, Map<String, Object> Data) {
            metamodel = Metamodel;
            type = Data.getOrDefault("type", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            persistentName = Data.getOrDefault("persistentName", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            name = Data.getOrDefault("name", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            pluralName = Data.getOrDefault("pluralName", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            description = Data.getOrDefault("description", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            mandatory = Boolean.parseBoolean(Data.getOrDefault("mandatory", Boolean.FALSE).toString());
            multiple = Boolean.parseBoolean(Data.getOrDefault("multiple", Boolean.FALSE).toString());

            if (SelfRelationFeatureNames.contains(persistentName)) featureType = FeatureType.SELF_RELATION;
            else if ("io.luy.model.Direction".equals(type)) featureType = FeatureType.INTERFACE_DIRECTION;
            else if (type.startsWith("io.luy.model.attribute.EnumAT.")) featureType = FeatureType.ENUMERATION;
            else if (!NonRelationshipFeatureNames.contains(type)) featureType = FeatureType.RELATION;
            else featureType = FeatureType.valueOf(type.toUpperCase());

            isSortable = !multiple && ((FeatureType.ENUMERATION == featureType) || NonRelationshipFeatureNames.contains(type));
        }

        @Override public int compareTo(Feature Other) {
            if ("id".equals(this.persistentName)) return -1;
            if ("id".equals(Other.persistentName)) return 1;
            if ("name".equals(this.persistentName)) return -1;
            if ("name".equals(Other.persistentName)) return 1;
            return this.name.compareTo(Other.name);
        }

        public boolean referencesBuildingblock() { return metamodel.SubstantialTypeExpressions.stream().anyMatch(r -> r.persistentName.equals(type)); }
        public RelationshipTypeExpression getRTE() { return metamodel.RelationshipTypeExpressions.stream().filter(rte -> rte.persistentName.equals(type)).findFirst().orElse(null); }
    }

    public abstract static class TypeExpression extends BasicExpression {
        public final String abbreviation;
        public final Set<Feature> features;

        TypeExpression(CMetamodel Metamodel, Map<String, Object> Data) {
            super(Metamodel, Data);
            abbreviation = Data.getOrDefault("abbreviation", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            if (Data.getOrDefault("features", new ArrayList<Map<String, Object>>()) instanceof List featureData) {
                features = (Set<Feature>)featureData.stream().filter(fd -> fd instanceof Map).map(fd -> new Feature(Metamodel, (Map)fd)).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));
            } else features = new TreeSet<>();
        }
    }

    public final static class RelationshipTypeExpression extends TypeExpression {
        RelationshipTypeExpression(CMetamodel Metamodel, Map<String, Object> Data) { super(Metamodel, Data); }
    }

    public final static class SubstantialTypeExpression extends TypeExpression {
        SubstantialTypeExpression(CMetamodel Metamodel, Map<String, Object> Data) { super(Metamodel, Data); }
    }

    public final static class Literal implements Comparable<Literal> {
        public final String persistentName;
        public final String name;
        public final String pluralName;
        public final String description;
        public final Color color;
        public final int index;

        Literal(Map<String, Object> Data) {
            persistentName = Data.getOrDefault("persistentName", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            name = Data.getOrDefault("name", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            pluralName = Data.getOrDefault("pluralName", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            description = Data.getOrDefault("description", CTranslations.INSTANCE.Unknown_Placeholder).toString();
            color = deserialize(Data.getOrDefault("color", "rgb(0,0,0)").toString());
            index = Integer.parseInt(Data.getOrDefault("index", -1).toString());
        }

        public Color deserialize(String value) {
            String[] rgb = value.substring(4, value.length() - 1).split(",");
            return new Color(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()));
        }

        @Override public int compareTo(Literal Other) { return this.name.compareTo(Other.name); }
        @Override public String toString() { return this.name; }
    }

    public final static class EnumerationExpression extends BasicExpression {
        public final Set<Literal> literals;

        EnumerationExpression(CMetamodel Metamodel, Map<String, Object> Data) {
            super(Metamodel, Data);
            if (Data.getOrDefault("literals", new ArrayList<Map<String, Literal>>()) instanceof List literalData) {
                literals = (Set<Literal>)literalData.stream().filter(ld -> ld instanceof Map).map(ld -> new Literal((Map)ld)).collect(Collectors.toCollection(TreeSet::new));
            } else literals = new TreeSet<>();
        }
    }

    public final String LUYDataVersion;
    public final Set<EnumerationExpression> EnumerationExpressions = new TreeSet<>();
    public final Set<SubstantialTypeExpression> SubstantialTypeExpressions = new TreeSet<>();
    public final Set<RelationshipTypeExpression> RelationshipTypeExpressions = new TreeSet<>();

    private CMetamodel(String LUYDataVersion) { this.LUYDataVersion = LUYDataVersion.split("/")[1]; }

    /**
     * Loads a {@code Metamodel} from the specified JSON file and parses its content
     * to populate the metamodel with corresponding entities such as EnumerationExpression,
     * SubstantialTypeExpression, or RelationshipTypeExpression.
     *
     * @param filename the path to the JSON file containing the metamodel data
     * @return a {@code Metamodel} instance populated with the parsed data
     * @throws IOException if an error occurs during reading or parsing the JSON file
     */
    public static CMetamodel load(String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        CMetamodel metamodel = new CMetamodel(filename);

        // Parse the JSON file as an array of objects
        Map<String, Object>[] items = objectMapper.readValue(new File(filename + "_metamodel.json"), Map[].class);

        for (Map<String, Object> item : items) {
            switch (item.getOrDefault("type", "No type present").toString()) {
                case "EnumerationExpression": metamodel.EnumerationExpressions.add(new EnumerationExpression(metamodel, item)); break;
                case "SubstantialTypeExpression": metamodel.SubstantialTypeExpressions.add(new SubstantialTypeExpression(metamodel, item)); break;
                case "RelationshipTypeExpression": metamodel.RelationshipTypeExpressions.add(new RelationshipTypeExpression(metamodel, item)); break;
                default:
                    System.err.println("Unknown metamodel type: " + item.get("type"));
                    break;
            }
        }

        StringBuilder loadOutput = new StringBuilder("Loaded LUY metamodel from file '" + filename + "'");
        loadOutput.append("\n\tSubstantial types:");
        metamodel.SubstantialTypeExpressions.forEach(e -> loadOutput.append("\n\t\t" + e.persistentName + " with " + e.features.size() + " features"));
        loadOutput.append("\n\tRelationship types:");
        metamodel.RelationshipTypeExpressions.forEach(e -> loadOutput.append("\n\t\t" + e.persistentName + " with " + e.features.size() + " features"));
        loadOutput.append("\n\tEnumeration types:");
        metamodel.EnumerationExpressions.forEach(e -> loadOutput.append("\n\t\t" + e.persistentName + " with " + e.literals.size() + " literals"));
        LUYViewer.LOGGER.log(Level.INFO, loadOutput.toString());


        return metamodel;
    }

    @Override public String toString() {
        try {
            long timestampLong = Long.parseLong(LUYDataVersion);
            // If timestamp appears to be in seconds (less than a reasonable millisecond value) convert to milliseconds
            if (timestampLong < 10000000000L) timestampLong *= 1000;
            Instant instant = Instant.ofEpochMilli(timestampLong);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (NumberFormatException | java.time.DateTimeException e) { return "Invalid timestamp: " + LUYDataVersion; }
    }
}
