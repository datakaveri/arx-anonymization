import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataType;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HierarchyBuilderUtil {

    /**
     * Builds hierarchies for specified columns in the dataset using interval-based grouping.
     *
     * @param dataset Dataset for which hierarchies need to be built.
     * @param hierarchyLevels Map containing hierarchy levels for each column.
     * @param intervalWidths Map containing interval widths for each column.
     * @return Map containing the number of hierarchy levels for each column.
     */
    public static Map<String, Integer> buildHierarchies(Data dataset, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths) {
        Map<String, Integer> anonymizationLevels = new HashMap<>();

        for (String columnName : hierarchyLevels.keySet()) {
            int colIndex = getColumnIndex(dataset, columnName);
            if (colIndex == -1) {
                System.out.println("Column " + columnName + " not found in dataset.");
                continue;  // Skip if column is not found
            }

            System.out.println("Building hierarchy for column: " + columnName);

            HierarchyBuilderIntervalBased<Double> builder = HierarchyBuilderIntervalBased.create(DataType.DECIMAL);

            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            for (int row = 0; row < dataset.getHandle().getNumRows(); row++) {
                double value = Double.parseDouble(dataset.getHandle().getValue(row, colIndex));
                if (value < minValue) minValue = value;
                if (value > maxValue) maxValue = value;
            }

            double startValue = Math.floor(minValue);
            double endValue = Math.ceil(maxValue);
            double level1Width = intervalWidths.get(columnName);

            for (double i = startValue; i < endValue; i += level1Width) {
                builder.addInterval(i, i + level1Width);
            }
            builder.getLevel(0).addGroup(2);

            for (int level = 1; level < hierarchyLevels.get(columnName); level++) {
                int groupSize = getUserInputForGroupSize(columnName, level);
                builder.getLevel(level).addGroup(groupSize);
            }

            dataset.getDefinition().setAttributeType(columnName, builder);
            anonymizationLevels.put(columnName, hierarchyLevels.get(columnName));
        }

        return anonymizationLevels;
    }

    /**
     * Retrieves the index of a column in the dataset.
     *
     * @param dataset Dataset to search.
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

    /**
     * Prompts the user to enter the group size for a specific level of hierarchy for a column.
     *
     * @param columnName Name of the column.
     * @param level Hierarchy level for which the group size is needed.
     * @return Group size entered by the user.
     */
    private static int getUserInputForGroupSize(String columnName, int level) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the group size for level " + level + " for " + columnName + ": ");
        return scanner.nextInt();
    }
}
