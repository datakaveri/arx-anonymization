package iudx.arx;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXLattice;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.json.JSONArray;
import org.json.JSONObject;

public class DatasetAnonymizer {
    public static List<Map<String, Object>> setupAndAnonymizeDataset(String metricType, String datasetPath, Charset charset, char delimiter, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths, List<Integer> sizes, Properties properties, int k, double suppressionLimit, Metric<?> metric) throws IOException, NoSuchAlgorithmException {
        List<Map<String, Object>> json_response;
        String[] attributesToSuppress = properties.getProperty("suppress").split(",");
        String[] attributesToPseudonymize = properties.getProperty("pseudonymize").split(",");
        Suppress.suppression(datasetPath, attributesToSuppress);
        Pseudonymize.pseudonymization(datasetPath, attributesToPseudonymize);
        Data dataset = Data.create("/home/kailash/Desktop/arx-anonymization/arx/pseudonymized.csv", charset, delimiter);
        setupDataset(dataset, hierarchyLevels, intervalWidths, properties, sizes);
        ARXConfiguration config = createARXConfiguration(k, suppressionLimit, metric);
        json_response = anonymizeAndAnalyze(metricType, dataset, config);
        return json_response;
    }

    private static void setupDataset(Data dataset, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths, Properties properties, List<Integer> sizes) {
        HierarchyBuilderUtil.buildHierarchies(dataset, hierarchyLevels, intervalWidths, sizes);
        setAttributes(dataset, properties.getProperty("insensitive.columns").split(","), AttributeType.INSENSITIVE_ATTRIBUTE);
    }

    private static void setAttributes(Data dataset, String[] columns, AttributeType type) {
        String[] var3 = columns;
        int var4 = columns.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String attribute = var3[var5];
            dataset.getDefinition().setAttributeType(attribute, type);
        }

    }

    private static ARXConfiguration createARXConfiguration(int k, double suppressionLimit, Metric<?> metric) {
        ARXConfiguration config = ARXConfiguration.create();
        config.addPrivacyModel(new KAnonymity(k));
        config.setSuppressionLimit(suppressionLimit);
        config.setQualityModel(metric);
        return config;
    }

    private static List<Map<String, Object>> anonymizeAndAnalyze(String metricType, Data dataset, ARXConfiguration config) throws IOException {
        List<Map<String, Object>> json_response ;
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(dataset, config);
        DataHandle outputhandle = result.getOutput();
        json_response = outputAnonymizedDataset(outputhandle);
        ARXLattice.ARXNode transformation = result.getGlobalOptimum();
        DataAnalysis.analytics(result);
        AppendAnalytics.main(new String[]{});
        return json_response;
    }

    private static List<Map<String, Object>> outputAnonymizedDataset(DataHandle handle) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        CsvParserSettings parserSettings = new CsvParserSettings();
        CsvParser parser2 = new CsvParser(parserSettings);
        List<String[]> allRows2 = parser2.parseAll(new File("/home/kailash/Desktop/arx-anonymization/arx/pseudonymized.csv"));
        String[] headers = (String[])allRows2.get(0);

        for(int rowIndex = 0; rowIndex < handle.getNumRows(); ++rowIndex) {
            Map<String, Object> rowMap = new HashMap<>();

            for(int colIndex = 0; colIndex < headers.length; ++colIndex) {
                String columnName = headers[colIndex];
                String value = handle.getValue(rowIndex, colIndex);
                rowMap.put(columnName, value);
            }

            result.add(rowMap);
        }

        FileWriter fileWriter = new FileWriter("anonymized_output.json");

        try {
            fileWriter.write(convertToJsonString(result));
        } catch (Throwable var12) {
            try {
                fileWriter.close();
            } catch (Throwable var11) {
                var12.addSuppressed(var11);
            }

            throw var12;
        }

        fileWriter.close();
        handle.save(new File("anonymized_datset.csv"), ',');
        System.out.println("Anonymized dataset saved to /home/shivanit/Documents/P3DX/test_arx_api/arx/anonymized_datset.csv");
        return result;
    }
    private static String convertToJsonString(List<Map<String, Object>> list) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            jsonBuilder.append("  {\n");
            int j = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonBuilder.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                if (j < map.size() - 1) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\n");
                j++;
            }
            jsonBuilder.append("  }");
            if (i < list.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }
}