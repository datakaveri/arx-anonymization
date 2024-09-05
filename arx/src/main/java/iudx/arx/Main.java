package iudx.arx;

import org.deidentifier.arx.*;
import org.deidentifier.arx.metric.Metric;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;



import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Main {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // Load config.json using JSONTokener
        JSONTokener tokener = new JSONTokener(new FileReader("src/main/java/iudx/arx/config.json"));
        JSONObject config = new JSONObject(tokener);

        // Extract basic information
        String datasetPath = "/home/kailash/Desktop/arx_anonymization/arx/" + config.getString("dataset_name");
        Charset charset = Charset.forName("UTF-8");
        char delimiter = ',';

        // Extract suppressed columns
        JSONObject medicalConfig = config.getJSONObject("medical");
        JSONArray suppressArray = medicalConfig.getJSONArray("suppress");
        List<String> suppressedColumns = new ArrayList<>();
        for (int i = 0; i < suppressArray.length(); i++) {
            suppressedColumns.add(suppressArray.getString(i));
        }

        // Extract pseudonymized columns
        JSONArray pseudonymizeArray = medicalConfig.getJSONArray("pseudonymize");
        List<String> pseudonymizedColumns = new ArrayList<>();
        for (int i = 0; i < pseudonymizeArray.length(); i++) {
            pseudonymizedColumns.add(pseudonymizeArray.getString(i));
        }

        // Extract insensitive columns
        JSONArray insensitiveArray = medicalConfig.getJSONArray("insensitive_columns");
        List<String> insensitiveColumns = new ArrayList<>();
        for (int i = 0; i < insensitiveArray.length(); i++) {
            insensitiveColumns.add(insensitiveArray.getString(i));
        }

        // Extract k value for k-anonymity
        JSONObject kAnonymizeConfig = medicalConfig.getJSONObject("k_anonymize");
        int k = kAnonymizeConfig.getInt("k");

        // Hardcoded hierarchy levels and interval widths (based on config.json)
        Map<String, Integer> hierarchyLevels = new HashMap<>();
        Map<String, Double> intervalWidths = new HashMap<>();

        hierarchyLevels.put("Age", 10);
        intervalWidths.put("Age", 2.0);

        hierarchyLevels.put("PIN Code", 1);
        intervalWidths.put("PIN Code", 200.0);

        hierarchyLevels.put("Height", 4);
        intervalWidths.put("Height", 15.0);

        hierarchyLevels.put("Weight", 4);
        intervalWidths.put("Weight", 5.0);

        List<Integer> sizes = new ArrayList<>(Arrays.asList(2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2));

        double suppressionlimit  = 0;
        boolean allowRecordSuppression = medicalConfig.getString("allow_record_suppression").equalsIgnoreCase("true");
        if(allowRecordSuppression){
            suppressionlimit = 1.0;
        }
        // Call the anonymization method
        DatasetAnonymizer.setupAndAnonymizeDataset(
                "Loss Metric (LM)",
                datasetPath,
                charset,
                delimiter,
                suppressedColumns.toArray(new String[0]),
                pseudonymizedColumns.toArray(new String[0]),
                insensitiveColumns.toArray(new String[0]),
                hierarchyLevels,
                intervalWidths,
                sizes,
                k,
                1.0,  // Suppression limit, hardcoded for now
                Metric.createLossMetric()
        );
    }
}
