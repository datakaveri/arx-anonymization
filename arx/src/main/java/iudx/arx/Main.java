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
        JSONTokener tokener = new JSONTokener(new FileReader("config.json"));
        JSONObject config = new JSONObject(tokener);

        // Extract basic information
        String datasetPath = config.getString("dataset_name");
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
        
        JSONArray generalizedArray = medicalConfig.getJSONArray("generalize");
        List<String> generalizedColumns = new ArrayList<>();
        for (int i = 0; i < generalizedArray.length(); i++) {
            generalizedColumns.add(generalizedArray.getString(i));
        }
        
        // Extract insensitive columns
        JSONArray insensitiveArray = medicalConfig.getJSONArray("insensitive_columns");
        List<String> insensitiveColumns = new ArrayList<>();
        for (int i = 0; i < insensitiveArray.length(); i++) {
            insensitiveColumns.add(insensitiveArray.getString(i));
        }

        insensitiveColumns.addAll(pseudonymizedColumns);
        System.out.println(insensitiveColumns);
        // Extract k value for k-anonymity
        JSONObject kAnonymizeConfig = medicalConfig.getJSONObject("k_anonymize");
        int k = kAnonymizeConfig.getInt("k");

        Map<String, Integer> hierarchyLevels = new HashMap<>();
        Map<String, Double> intervalWidths = new HashMap<>();

        List<Integer> sizes = new ArrayList<>();

        for (String column : generalizedColumns) {
            if (column.equalsIgnoreCase("Age")) {
                hierarchyLevels.put(column, 10);  
                intervalWidths.put(column, 2.0);  

                for (int i = 0; i < 10; i++) {
                    sizes.add(2);
                }
            } else if (column.equalsIgnoreCase("Height")) {
                hierarchyLevels.put(column, 4);
                intervalWidths.put(column, 15.0);
                for (int i = 0; i < 4; i++) {
                    sizes.add(2);
                }
            } else if (column.equalsIgnoreCase("Weight")) {
                hierarchyLevels.put(column, 4);
                intervalWidths.put(column, 5.0);
                for (int i = 0; i < 4; i++) {
                    sizes.add(2);
                }
            } else if (column.equalsIgnoreCase("PIN Code")) {
                hierarchyLevels.put("PIN Code", 1);
                intervalWidths.put("PIN Code", 200.0);
            }
        }

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
                generalizedColumns,
                hierarchyLevels,
                intervalWidths,
                sizes,
                k,
                1.0,  // Suppression limit, hardcoded for now
                Metric.createLossMetric()
        );
    }
}
