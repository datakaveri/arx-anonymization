package iudx.arx;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.ARXLattice;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.aggregates.StatisticsEquivalenceClasses;
import org.deidentifier.arx.metric.InformationLoss;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DataAnalysis {

    public static JSONObject analytics(ARXResult result) throws IOException {
        DataHandle outputhandle = result.getOutput();
        StatisticsEquivalenceClasses stats = outputhandle.getStatistics().getEquivalenceClassStatistics();
        ARXLattice.ARXNode transformation = result.getGlobalOptimum();
        InformationLoss<?> loss = transformation.getLowestScore();
        int num_suppressed = stats.getNumberOfSuppressedRecords();
        int total_records = stats.getNumberOfRecordsIncludingSuppressedRecords();
        int equivalence_class_size = stats.getMinimalEquivalenceClassSize();
        int num_equivalence_classes = stats.getNumberOfEquivalenceClasses();

        // Prepare analytics data
        JSONObject analyticsData = new JSONObject();
        analyticsData.put("information_loss", loss.toString());
        analyticsData.put("transformation_node", Arrays.toString(transformation.getTransformation()));
        analyticsData.put("num_suppressed", num_suppressed);
        analyticsData.put("total_records", total_records);
        analyticsData.put("smallest_equivalence_class_size", equivalence_class_size);
        analyticsData.put("largest_equivalence_class_size", stats.getMaximalEquivalenceClassSize());
        analyticsData.put("average_equivalence_class_size", stats.getAverageEquivalenceClassSize());
        analyticsData.put("num_equivalence_classes", num_equivalence_classes);

        String filePath = "anonymized_output.csv";
        String jsonFilePath = "analytics.json";

        // Create or update the JSON object
        JSONObject jsonObject;
        try {
            // Read the existing JSON file
            String jsonString = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

            // If the file is empty or malformed, initialize a new JSON object
            if (jsonString.trim().isEmpty()) {
                jsonObject = new JSONObject();
            } else {
                jsonObject = new JSONObject(jsonString);
            }
        } catch (JSONException e) {
            // If the content is not valid JSON, initialize a new JSONObject
            System.err.println("Warning: Malformed JSON file. Creating a new JSON object.");
            jsonObject = new JSONObject();
        } catch (IOException e) {
            // If the file does not exist or cannot be read, create a new JSON object
            System.out.println("Info: JSON file not found. Creating a new one.");
            jsonObject = new JSONObject();
        }

        // Add analytics data to the JSON object
        jsonObject.put("analytics", analyticsData);

        // Write the updated JSON back to the file
        try (FileWriter fileWriter = new FileWriter(jsonFilePath)) {
            fileWriter.write(jsonObject.toString(4)); // Pretty print with an indent of 4 spaces
        }

        System.out.println("Analytics data has been successfully added to analytics.json.");

        // Return the analytics JSON object
        return jsonObject;
    }
}
