package iudx.arx;

import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataType;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;
import org.deidentifier.arx.aggregates.HierarchyBuilderRedactionBased;

import java.io.IOException;
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
     */
    public static void buildHierarchies(Data dataset, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths,
                                        List<Integer> sizes){
        Map<String, Integer> anonymizationLevels = new HashMap<>();
        System.out.println("\n");
        for (String columnName : hierarchyLevels.keySet()) {
            int colIndex = getColumnIndex(dataset, columnName);
            if (colIndex == -1) {
                System.out.println("Column " + columnName + " not found in dataset.");
                continue;  
            }

            System.out.println("Building hierarchy for column: " + columnName);

      
            String hierarchyType = "interval";
            if (Objects.equals(columnName, "PIN Code")){
                hierarchyType = "redaction";
            }

            if (hierarchyType.equals("interval")) {
   
                buildIntervalBasedHierarchy(dataset, columnName, hierarchyLevels.get(columnName), intervalWidths.get(columnName), sizes);
            }
            else {
                buildRedactionBasedHierarchy(dataset, columnName, hierarchyLevels.get(columnName));
            }

            anonymizationLevels.put(columnName, hierarchyLevels.get(columnName));
        }
        System.out.println("\n");
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

    private static void buildRedactionBasedHierarchy(Data dataset, String columnName, int hierarchyLevel) {
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
