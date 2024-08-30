package iudx.arx;

import java.util.List;
import java.util.Map;

public class ColumnHierarchyConfigParser {
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