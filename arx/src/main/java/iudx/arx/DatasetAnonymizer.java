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
import org.deidentifier.arx.criteria.LDiversity;
import org.deidentifier.arx.criteria.EntropyLDiversity;
import org.deidentifier.arx.criteria.DistinctLDiversity;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DatasetAnonymizer {
    public static List<Map<String, Object>> setupAndAnonymizeDataset(
            String metricType,
            String datasetPath,
            Charset charset,
            char delimiter,
            String[] attributesToSuppress,
            String[] attributesToPseudonymize,
            String[] insensitiveColumns,
            String[] generalizedColumns,
            String sensitiveColumn,
            Map<String, Integer> hierarchyLevels,
            Map<String, Double> intervalWidths,
            List<Integer> sizes,
            int k,
            int l,
            double suppression_limit,
            Metric<?> metric
    ) throws IOException, NoSuchAlgorithmException {
        List<Map<String, Object>> json_response;
        System.out.println("Starting suppression...");
        System.out.flush();
        //Suppress.suppression(datasetPath, attributesToSuppress);
        //System.out.println("Suppression completed");
        //Pseudonymize.pseudonymization(attributesToPseudonymize);
        try {
        System.out.println("Loading dataset from: " + datasetPath);
        System.out.flush();

        Data dataset = Data.create(datasetPath, charset, delimiter);
        System.out.println("Dataset loaded");
        System.out.flush();

        setupDataset(dataset, sensitiveColumn, insensitiveColumns, generalizedColumns,hierarchyLevels, intervalWidths, sizes);
        ARXConfiguration config = createARXConfiguration(k, l, sensitiveColumn, suppression_limit, metric);
        json_response = anonymizeAndAnalyze(metricType,generalizedColumns ,dataset, config);
        return json_response;
    } catch(Exception e) {
        System.err.println("ERROR DURING ANONYMISATION:");
        e.printStackTrace();
        throw e;
    }
}

    private static void setupDataset(Data dataset, String sensitiveColumn, String[] insensitiveColumns,String[] generalizedColumns, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths, List<Integer> sizes) {
        HierarchyBuilderUtil.buildHierarchies(dataset, generalizedColumns,hierarchyLevels, intervalWidths, sizes);
        setAttributes(dataset, insensitiveColumns, AttributeType.INSENSITIVE_ATTRIBUTE);
        dataset.getDefinition().setAttributeType(sensitiveColumn, AttributeType.SENSITIVE_ATTRIBUTE);
    }


    private static void setAttributes(Data dataset, String[] columns, AttributeType type) {
        String[] var3 = columns;
        int var4 = columns.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            String attribute = var3[var5];
            dataset.getDefinition().setAttributeType(attribute, type);
        }

    }

    private static ARXConfiguration createARXConfiguration(int k, int l, String sensitiveColumn, double suppression_limit, Metric<?> metric) {
        ARXConfiguration config = ARXConfiguration.create();
        config.addPrivacyModel(new KAnonymity(k));
        config.setSuppressionLimit(suppression_limit);
        config.setQualityModel(metric);
        config.addPrivacyModel(new EntropyLDiversity(sensitiveColumn,l));
        return config;
    }

    private static List<Map<String, Object>> anonymizeAndAnalyze(String metricType, String[] generalizedColumns, Data dataset, ARXConfiguration config) throws IOException {
        System.out.println("\n=== Starting Anonymization Process ===");
        List<Map<String, Object>> json_response;
        
        try {
            System.out.println("1. Creating ARX anonymizer...");
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            
            System.out.println("2. Running anonymization...");
            System.out.println("- Dataset rows: " + dataset.getHandle().getNumRows());
            System.out.println("- Dataset columns: " + dataset.getHandle().getNumColumns());
            System.out.println("- Privacy model: " + config.getPrivacyModels());
            ARXResult result = anonymizer.anonymize(dataset, config);
            
            if (result == null) {
                throw new RuntimeException("Anonymization failed - null result");
            }
            
            System.out.println("3. Getting output handle...");
            DataHandle outputhandle = result.getOutput();
            if (outputhandle == null) {
                throw new RuntimeException("Output handle is null");
            }
            
            System.out.println("4. Writing to JSON...");
            json_response = outputAnonymizedDataset(outputhandle);
            
            System.out.println("5. Getting global optimum...");
            ARXLattice.ARXNode transformation = result.getGlobalOptimum();

            if (transformation == null) {
                System.out.println("Warning: No global optimum found");
            } else {
                System.out.println("Transformation level: " + Arrays.toString(transformation.getTransformation()));
                System.out.println("Information loss: " + result.getGlobalOptimum().getLowestScore());
            }
            
            System.out.println("6. Running equivalence classes analysis...");
            EquivalenceClasses.main(generalizedColumns);
            
            //System.out.println("7. Appending analytics...");
            //AppendAnalytics.main(new String[]{});
            
            //System.out.println("8. Reading final JSON...");
            //json_response = readJsonAsListOfMaps("anonymized_output.json");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("transformation_node", transformation.getTransformation());
            metadata.put("information_loss", transformation.getLowestScore());
            metadata.put("meta", true); // marker to identify metadata
            json_response.add(metadata);
            System.out.println("Metadata added: " + metadata);
            System.out.println("Full JSON response size: " + json_response.size());

            //System.out.println("TEST DEBUG STATEMENT FOR TRANSFORMATION NODE;");
            
            System.out.println("=== Anonymization Process Complete ===");
            
            return json_response;
            
        } catch (Exception e) {
            System.err.println("\nERROR in anonymization process:");
            System.err.println("Message: " + e.getMessage());
            System.err.println("Location: " + e.getStackTrace()[0]);
            e.printStackTrace();
            throw e;
        }
    }

    private static List<Map<String, Object>> outputAnonymizedDataset(DataHandle handle) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();

        // Get headers directly from handle
        String[] headers = new String[handle.getNumColumns()];
        for (int i = 0; i < handle.getNumColumns(); i++) {
            headers[i] = handle.getAttributeName(i);
        }
        System.out.println("Header length: " + headers.length + ", Headers: " + Arrays.toString(headers));

        // Process anonymized dataset
        for (int rowIndex = 0; rowIndex < handle.getNumRows(); rowIndex++) {
            Map<String, Object> rowMap = new HashMap<>();

            for (int colIndex = 0; colIndex < headers.length; colIndex++) {
                String columnName = headers[colIndex];

                // Ensure column index is within bounds
                if (colIndex >= handle.getNumColumns()) {
                    System.err.println("Column index out of bounds: " + colIndex);
                    continue;
                }

                String value = handle.getValue(rowIndex, colIndex);

                // Sanitize value
                if (value == null) value = "";
                value = value.replace("\n", " ").replace("\r", " ");

                rowMap.put(columnName, value);
            }

            result.add(rowMap);
        }

        System.out.println("Processing completed. Rows processed: " + result.size());

        // Write output to JSON
        try (FileWriter fileWriter = new FileWriter("anonymized_output.json")) {
            fileWriter.write(convertToJsonString(result));
        } catch (Throwable var12) {
            System.err.println("Error writing JSON: " + var12.getMessage());
            throw var12;
        }

        // Save CSV output
        handle.save(new File("anonymized_output.csv"), ',');
        System.out.println("Anonymized dataset saved to anonymized_output.csv");

        return result;
    }
    
    private static String convertToJsonString(List<Map<String, Object>> list) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        jsonBuilder.append("  \"anonymized_output\": [\n");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            jsonBuilder.append("    {\n");
            int j = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonBuilder.append("      \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                if (j < map.size() - 1) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\n");
                j++;
            }
            jsonBuilder.append("    }");
            if (i < list.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
        jsonBuilder.append("  ]\n");
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }
    
    public static List<Map<String, Object>> readJsonAsListOfMaps(String filePath) throws IOException {
        // Read the entire JSON file as a String
        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
    
        // Parse the JSON content
        JSONObject jsonObject = new JSONObject(jsonContent);
    
        // Create a list to store the JSON content as Maps
        List<Map<String, Object>> jsonResponse = new ArrayList<>();
    
        // Convert the JSONObject to a Map
        Map<String, Object> jsonMap = jsonObject.toMap();
    
        // Iterate through the entries in the map
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            // Check if the entry value is a JSONArray
            if (entry.getValue() instanceof List) {
                List<Map<String, Object>> list = new ArrayList<>();
                JSONArray jsonArray = jsonObject.getJSONArray(entry.getKey());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    list.add(item.toMap());
                }
                jsonResponse.add(Map.of(entry.getKey(), list));
            } else {
                // If it's not an array, directly put the entry into the list as a map
                jsonResponse.add(Map.of(entry.getKey(), entry.getValue()));
            }
        }
    
        return jsonResponse;
    }
}