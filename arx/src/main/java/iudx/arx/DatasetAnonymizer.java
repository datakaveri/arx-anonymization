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
import java.util.Collections;

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
import org.deidentifier.arx.DataDefinition;

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
        Suppress.suppression(datasetPath, attributesToSuppress);
        System.out.println("Suppression completed");
        Pseudonymize.pseudonymization(attributesToPseudonymize);
        System.out.println("Pseudonymization completed");
        datasetPath = "pseudonymized.csv";
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

    /**
         * Sets up the dataset by assigning attribute types (sensitive, insensitive, quasi-identifiers) 
         * and building hierarchies for the specified generalized columns.
         *
         * @param dataset            Dataset to be prepared.
         * @param sensitiveColumn    Column name containing sensitive information.
         * @param insensitiveColumns Array of column names to be treated as insensitive.
         * @param generalizedColumns Array of column names to be generalized (quasi-identifiers).
         * @param hierarchyLevels    Map specifying the number of hierarchy levels for each generalized column.
         * @param intervalWidths     Map specifying interval widths for numeric generalization.
         * @param sizes              List of integers representing size constraints for hierarchy building.
         */

    private static void setupDataset(Data dataset, String sensitiveColumn, String[] insensitiveColumns,String[] generalizedColumns, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths, List<Integer> sizes) {   
        HierarchyBuilderUtil.buildHierarchies(dataset, generalizedColumns,hierarchyLevels, intervalWidths, sizes);
        setAttributes(dataset, insensitiveColumns, AttributeType.INSENSITIVE_ATTRIBUTE);
        dataset.getDefinition().setAttributeType(sensitiveColumn, AttributeType.SENSITIVE_ATTRIBUTE);
        for (String col : generalizedColumns) {
            dataset.getDefinition().setAttributeType(col, AttributeType.QUASI_IDENTIFYING_ATTRIBUTE);
        }

        String[] headers = new String[dataset.getHandle().getNumColumns()];
        for (int i = 0; i < dataset.getHandle().getNumColumns(); i++) {
            headers[i] = dataset.getHandle().getAttributeName(i);
        }
        List<String> headersList = Arrays.asList(headers);
        
        for (String col : headersList) {
            if (!Arrays.asList(generalizedColumns).contains(col)&& !col.equals(sensitiveColumn)&& !Arrays.asList(insensitiveColumns).contains(col)) {
                dataset.getDefinition().setAttributeType(col, AttributeType.INSENSITIVE_ATTRIBUTE);
            }
        }

    }

    /**
         * Sets the specified attribute type for a list of columns in the dataset.
         *
         * @param dataset Dataset whose columns will be updated.
         * @param columns Array of column names to set the attribute type for.
         * @param type    AttributeType to assign to the specified columns (e.g., INSENSITIVE_ATTRIBUTE, 
         *                SENSITIVE_ATTRIBUTE, QUASI_IDENTIFYING_ATTRIBUTE).
         */

    private static void setAttributes(Data dataset, String[] columns, AttributeType type) {
        String[] var3 = columns;
        int var4 = columns.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            String attribute = var3[var5];
            dataset.getDefinition().setAttributeType(attribute, type);
        }

    }

     /**
         * Creates and configures an ARXConfiguration object for k-anonymity and l-diversity.
         *
         * @param k               The k parameter for k-anonymity.
         * @param l               The l parameter for l-diversity.
         * @param sensitiveColumn The column name containing sensitive information.
         * @param suppression_limit Maximum allowed suppression proportion (between 0 and 1).
         * @param metric          The quality metric used to evaluate anonymization results.
         * @return ARXConfiguration A configured ARXConfiguration object ready for anonymization.
         */

    private static ARXConfiguration createARXConfiguration(int k, int l, String sensitiveColumn, double suppression_limit, Metric<?> metric) {
        ARXConfiguration config = ARXConfiguration.create();
        config.addPrivacyModel(new KAnonymity(k));
        config.setSuppressionLimit(suppression_limit);
        config.setQualityModel(metric);
        config.addPrivacyModel(new EntropyLDiversity(sensitiveColumn,l));
        return config;
    }

    /**
         * Performs anonymization on a dataset using ARX and analyzes the results.
         *
         * @param metricType        The type of metric used for evaluating anonymization (e.g., "entropy").
         * @param generalizedColumns The list of columns that are generalized (quasi-identifiers).
         * @param dataset           The dataset to be anonymized.
         * @param config            The ARX configuration specifying privacy models and quality metrics.
         * @return List<Map<String, Object>> A list of maps representing the anonymized dataset in JSON format,
         *                                   with metadata appended for transformation and information loss.
         * @throws IOException If there is an error during anonymization or writing output.
         */

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

            //Reorder transformation to alphabetical column order
            String[] headers = new String[outputhandle.getNumColumns()];
            for (int i = 0; i < outputhandle.getNumColumns(); i++) {
                headers[i] = outputhandle.getAttributeName(i);
            }
            List<String> headersList = Arrays.asList(headers);
            System.out.println("All headers: " + headersList);

            int[] original = transformation.getTransformation();
            System.out.println("Original transformation (dataset order): " + Arrays.toString(original));

            List<String> allQids = new ArrayList<>();
            for (String col : headersList) {
                if (dataset.getDefinition().getAttributeType(col) == AttributeType.QUASI_IDENTIFYING_ATTRIBUTE) {
                    allQids.add(col);
                }
            }
            System.out.println("QIDs in header/config order: " + allQids);

            List<String> sortedQids = new ArrayList<>(allQids);
            Collections.sort(sortedQids);
            System.out.println("QIDs alphabetical order: " + sortedQids);

            int[] reordered = new int[allQids.size()];
            for (int i = 0; i < allQids.size(); i++) {
                String qid = allQids.get(i);          
                int targetIndex = sortedQids.indexOf(qid); 
                reordered[targetIndex] = original[i]; 
            }

            System.out.println("Reordered transformation (alphabetical): " + Arrays.toString(reordered));

            System.out.println("6. Running equivalence classes analysis...");
            EquivalenceClasses.main(generalizedColumns);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("transformation_node", reordered);
            metadata.put("information_loss", transformation.getLowestScore());
            metadata.put("meta", true); // marker to identify metadata
            json_response.add(metadata);
            System.out.println("Metadata added: " + metadata);
            System.out.println("Full JSON response size: " + json_response.size());
            
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

    /**
         * Converts an anonymized DataHandle into a list of maps and saves the output to JSON and CSV files.
         *
         * @param handle The DataHandle representing the anonymized dataset.
         * @return List<Map<String, Object>> A list of maps where each map corresponds to a row of the anonymized dataset.
         * @throws IOException If there is an error writing to the JSON or CSV files.
         */

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

    /**
         * Converts a list of maps into a formatted JSON string representing the anonymized dataset.
         *
         * @param list The list of maps representing rows of the dataset.
         * @return String A JSON-formatted string representing the anonymized dataset.
         */
    
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

    /**
         * Reads a JSON file and converts its contents into a list of maps.
         *
         * @param filePath The path to the JSON file to be read.
         * @return List<Map<String, Object>> A list of maps representing the JSON content. Arrays in JSON are converted to lists of maps.
         * @throws IOException If there is an error reading the file.
         */
    
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