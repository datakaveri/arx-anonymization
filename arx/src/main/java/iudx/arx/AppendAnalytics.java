package iudx.arx;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileWriter;

/**
 * Appends analytics data to the anonymized output JSON file.
 *
 * One-line brief:
 *   Merges the "size_distribution" analytics data into the anonymized output JSON file.
 *
 * Description:
 *   This function reads both the anonymized output file and the analytics file, parses them as JSON,
 *   and integrates the "size_distribution" object from the analytics JSON into the root level
 *   of the anonymized output JSON. The updated anonymized output file is then saved with
 *   pretty-printed formatting for readability.
 *
 * Responsibilities:
 *   - Load the anonymized output JSON from the given file path.
 *   - Load the analytics JSON from its file path.
 *   - Extract the "size_distribution" section from analytics.
 *   - Append the extracted data to the anonymized output JSON object.
 *   - Write the modified JSON back to the anonymized output file.
 *
 * Exceptions:
 *   IOException   - If reading or writing to the files fails.
 *   JSONException - If parsing the JSON content fails.
 */


public class AppendAnalytics {

    public static void main(String[] args) {
        String anonymizedOutputPath = "anonymized_output.json";
        String analyticsPath = "equivalence_stats.json";

        try {
            // Read the anonymized output JSON file into a string
            String anonymizedOutputContent = new String(Files.readAllBytes(Paths.get(anonymizedOutputPath)));

            // Parse the anonymized output JSON content
            JSONObject anonymizedOutputObject = new JSONObject(anonymizedOutputContent);

            // Extract the "anonymized_output" array from the JSON object
            JSONArray anonymizedOutputArray = anonymizedOutputObject.getJSONArray("anonymized_output");

            // Read the analytics JSON file into a string
            String analyticsContent = new String(Files.readAllBytes(Paths.get(analyticsPath)));

            // Parse the analytics JSON content
            JSONObject analyticsObject = new JSONObject(analyticsContent);

            // Append the "analytics" object to the root level of the anonymized output JSON object
            anonymizedOutputObject.put("size_distribution", analyticsObject.getJSONObject("size_distribution"));

            // Write the modified JSON object back to the anonymized output file
            try (FileWriter fileWriter = new FileWriter(anonymizedOutputPath)) {
                fileWriter.write(anonymizedOutputObject.toString(4)); // Pretty print with an indent of 4 spaces
            }

            System.out.println("Analytics data has been successfully added to anonymized_output.json.");

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
