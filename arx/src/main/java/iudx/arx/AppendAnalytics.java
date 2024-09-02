package iudx.arx;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileWriter;

public class AppendAnalytics {

    public static void main(String[] args) {
        String anonymizedOutputPath = "anonymized_output.json";
        String analyticsPath = "analytics.json";

        try {
            // Read the content of anonymized_output.json
            String anonymizedOutputContent = new String(Files.readAllBytes(Paths.get(anonymizedOutputPath)));
            JSONObject anonymizedOutputObject = new JSONObject(anonymizedOutputContent);

            // Extract the "anonymized_output" array from the JSON
            JSONArray anonymizedOutputArray = anonymizedOutputObject.getJSONArray("anonymized_output");

            // Read the content of analytics.json
            String analyticsContent = new String(Files.readAllBytes(Paths.get(analyticsPath)));
            JSONObject analyticsObject = new JSONObject(analyticsContent);

            // Create a new JSONObject for analytics and add it to the array
            JSONObject analyticsEntry = new JSONObject();
            analyticsEntry.put("analytics", analyticsObject.getJSONObject("analytics"));

            anonymizedOutputArray.put(analyticsEntry);

            // Write the modified JSON back to anonymized_output.json
            try (FileWriter fileWriter = new FileWriter(anonymizedOutputPath)) {
                fileWriter.write(anonymizedOutputObject.toString(4)); // Pretty print with an indent of 4 spaces
            }

            System.out.println("Analytics data has been successfully added to anonymized_output.json.");

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
