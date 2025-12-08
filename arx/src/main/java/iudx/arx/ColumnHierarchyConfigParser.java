package iudx.arx;

import java.util.List;
import java.util.Map;

public class ColumnHierarchyConfigParser {
    /**
     * Parses column hierarchy configuration and populates hierarchy parameters.
     *
     * Description:
     *   This function reads an array of configuration strings for columns, where each 
     *   string specifies the column name, number of hierarchy levels, interval width, 
     *   and optional group sizes. It extracts these values and populates the provided 
     *   maps and list for use in hierarchy building.
     *
     * @param columnHierarchyConfig An array of strings, each containing configuration for a column in the format:
     *                              "columnName:numLevels:intervalWidth:size1:size2:...".
     * @param hierarchyLevels       A map to store the number of hierarchy levels for each column.
     * @param intervalWidths        A map to store the interval width for each column.
     * @param sizes                 A list to store group sizes for higher hierarchy levels.
     */

    public static void parse(String[] columnHierarchyConfig, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths, List<Integer> sizes) {
        String[] var4 = columnHierarchyConfig;
        int var5 = columnHierarchyConfig.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            String config = var4[var6];
            String[] parts = config.split(":");
            String columnName = parts[0];
            int numLevels = Integer.parseInt(parts[1]);
            double intervalWidth = Double.parseDouble(parts[2]);

            for(int i = 0; i < numLevels - 1; ++i) {
                sizes.add(Integer.parseInt(parts[i + 3]));
            }

            hierarchyLevels.put(columnName, numLevels);
            intervalWidths.put(columnName, intervalWidth);
        }

    }
}