package iudx.arx;

import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataType;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;
import org.deidentifier.arx.aggregates.HierarchyBuilderRedactionBased;
import org.deidentifier.arx.aggregates.HierarchyBuilderOrderBased;
import org.deidentifier.arx.aggregates.HierarchyBuilderGroupingBased;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HierarchyBuilderUtil {

    /**
     * Builds hierarchies for specified columns in the dataset using the hierarchy types specified by the user.
     *
     * @param dataset         Dataset for which hierarchies need to be built.
     * @param hierarchyLevels Map containing hierarchy levels for each column.
     * @param intervalWidths  Map containing interval widths for each column (used for interval-based hierarchies).
     * @param sizes           List containing sizes for redaction-based hierarchy.
     * @return List of messages generated during hierarchy building.
     */
    public static void buildHierarchies(Data dataset, String[] generalizedColumns, Map<String, Integer> hierarchyLevels,
                                                Map<String, Double> intervalWidths, List<Integer> sizes) {
        System.out.println("\n=== Starting Hierarchy Building ===");
        System.out.println("Columns to process: " + Arrays.toString(generalizedColumns));
        
        for (String columnName : generalizedColumns) {
            try {
                System.out.println("\nProcessing column: " + columnName);
                System.out.println("Hierarchy level: " + hierarchyLevels.get(columnName));
                System.out.println("Interval width: " + intervalWidths.get(columnName));

                switch (columnName) {
                    case "Blood Group":
                        System.out.println("Building blood group hierarchy...");
                        buildBloodGroupHierarchy(dataset, columnName);
                        break;
                    case "Profession":
                        System.out.println("Building profession hierarchy...");
                        buildProfessionHierarchy(dataset, columnName);
                        break;
                    case "PIN Code":
                        System.out.println("Building PIN code hierarchy...");
                        buildRedactionBasedHierarchy(dataset, columnName);
                        break;
                    case "Gender":
                        System.out.println("Building Gender hierarchy...");
                        buildGenderHierarchy(dataset, columnName);
                        break;
                    default:
                        System.out.println("Building interval-based hierarchy...");
                        System.out.println("Parameters:");
                        System.out.println("- Level: " + hierarchyLevels.get(columnName));
                        System.out.println("- Width: " + intervalWidths.get(columnName));
                        System.out.println("- Sizes: " + sizes);
                        
                        buildIntervalBasedHierarchy(dataset, columnName,
                                hierarchyLevels.get(columnName),
                                intervalWidths.get(columnName), sizes);
                }
                System.out.println("Successfully built hierarchy for: " + columnName);
                
            } catch (Exception e) {
                System.err.println("Error building hierarchy for " + columnName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void buildBloodGroupHierarchy(Data dataset, String columnName) {
        // Create a string array for the hierarchy
        String[][] hierarchy = {
            {"A+", "A", "*"},
            {"A-", "A", "*"},
            {"B+", "B", "*"},
            {"B-", "B", "*"},
            {"AB+", "AB", "*"},
            {"AB-", "AB", "*"},
            {"O+", "O", "*"},
            {"O-", "O", "*"},
        };
        
        // Create hierarchy directly
        AttributeType.Hierarchy bloodGroupHierarchy = AttributeType.Hierarchy.create(hierarchy);
        dataset.getDefinition().setAttributeType(columnName, bloodGroupHierarchy);
    }

    private static void buildProfessionHierarchy(Data dataset, String columnName) {
        // Create a string array for the hierarchy
        String[][] hierarchy = {
    {"Medical Specialists", "Healthcare", "Service Sector", "Workforce Sector"},
    {"Allied Health", "Healthcare", "Service Sector", "Workforce Sector"},
    {"Nursing", "Healthcare", "Service Sector", "Workforce Sector"},
    {"Healthcare Support", "Healthcare", "Service Sector", "Workforce Sector"},
    {"K-12 Education Teacher", "Education", "Service Sector", "Workforce Sector"},
    {"Higher Education Teacher", "Education", "Service Sector", "Workforce Sector"},
    {"Supplemental Education Teacher", "Education", "Service Sector", "Workforce Sector"},
    {"University Professor", "Education", "Service Sector", "Workforce Sector"},
    {"Performing Arts", "Creative", "Non-Service", "Workforce Sector"},
    {"Visual & Media Arts", "Creative", "Non-Service", "Workforce Sector"},
    {"Design", "Creative", "Non-Service", "Workforce Sector"},
    {"Mixed Media Artist", "Creative", "Non-Service", "Workforce Sector"},
    {"Traditional Engineering", "Engineering", "Non-Service", "Workforce Sector"},
    {"Software Engineering", "Engineering", "Non-Service", "Workforce Sector"},
    {"Data & Analytics", "Engineering", "Non-Service", "Workforce Sector"},
    {"AI & Machine Learning", "Engineering", "Non-Service", "Workforce Sector"},
        };

        // Create hierarchy directly
        AttributeType.Hierarchy professionHierarchy = AttributeType.Hierarchy.create(hierarchy);
        dataset.getDefinition().setAttributeType(columnName, professionHierarchy);
    }

    private static void buildGenderHierarchy(Data dataset, String columnName) {
        String[][] hierarchy = {
            {"Male", "*"},
            {"Female", "*"},
        };

        AttributeType.Hierarchy genderHierarchy = AttributeType.Hierarchy.create(hierarchy);
        dataset.getDefinition().setAttributeType(columnName, genderHierarchy);
    }

    private static void buildIntervalBasedHierarchy(Data dataset, String columnName, int hierarchyLevel, double intervalWidth, List<Integer> sizes) {
        HierarchyBuilderIntervalBased<Double> builder = HierarchyBuilderIntervalBased.create(DataType.DECIMAL);

        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;

        int colIndex = getColumnIndex(dataset, columnName);

        for (int row = 0; row < dataset.getHandle().getNumRows(); row++) {
            double value = Double.parseDouble(dataset.getHandle().getValue(row, colIndex));
            if (value < minValue) minValue = value;
            if (value > maxValue) maxValue = value;
        }

        double startValue = Math.floor(minValue);
        double endValue = Math.ceil(maxValue) + 1;

        for (double i = startValue; i < endValue; i += intervalWidth) {
            builder.addInterval(i, i + intervalWidth);
        }
        builder.getLevel(0).addGroup(2);

        for (int level = 1; level < hierarchyLevel; level++) {
            int groupSize = sizes.get(level - 1);
            builder.getLevel(level).addGroup(groupSize);
        }

        dataset.getDefinition().setAttributeType(columnName, builder);
    }

    private static void buildRedactionBasedHierarchy(Data dataset, String columnName) {
        HierarchyBuilderRedactionBased<Object> builder = HierarchyBuilderRedactionBased.create('*');
        int colIndex = getColumnIndex(dataset, columnName);
        dataset.getDefinition().setAttributeType(columnName, builder);
    }

    /**
     * Retrieves the index of a column in the dataset.
     *
     * @param dataset   Dataset to search.
     * @param columnName Name of the column to find.
     * @return Index of the column, or -1 if not found.
     */
    private static int getColumnIndex(Data dataset, String columnName) {
        for (int i = 0; i < dataset.getHandle().getNumColumns(); i++) {
            if (dataset.getHandle().getAttributeName(i).equals(columnName)) {
                return i;
            }
        }
        return -1;
    }
}
