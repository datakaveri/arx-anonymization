package iudx.arx;
import org.deidentifier.arx.*;
import org.deidentifier.arx.metric.Metric;
import org.springframework.stereotype.Service;
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

@Service
public class ARXTestingService {
    public static ARXResponse processProperties(
            String k_value,
            String suppress_columns,
            String pseudonymize_columns,
            String insensitive_columns,
            String allowRecordSuppression
    ) throws IOException, NoSuchAlgorithmException {

        List<Map<String, Object>> jsonResponseList;
        List<String> suppressedColumns = Arrays.asList(suppress_columns.split(","));
        List<String> pseudonymizedColumns = Arrays.asList(pseudonymize_columns.split(","));
        List<String> insensitiveColumns = Arrays.asList(insensitive_columns.split(","));
        //JSONTokener tokener = new JSONTokener(new FileReader("/home/kailash/Desktop/arx_anonymization/arx/src/main/java/iudx/arx/config.json"));
        //JSONObject config = new JSONObject(tokener);
        int k = Integer.parseInt(k_value);
        String datasetPath = "Medical_Data_new.csv";
        Charset charset = Charset.forName("UTF-8");
        char delimiter = ',';
/*
        JSONObject medicalConfig = config.getJSONObject("medical");
        JSONArray suppressArray = medicalConfig.getJSONArray("suppress");
        List<String> suppressedColumns = new ArrayList<>();
        for (int i = 0; i < suppressArray.length(); i++) {
            suppressedColumns.add(suppressArray.getString(i));
        }

        JSONArray pseudonymizeArray = medicalConfig.getJSONArray("pseudonymize");
        List<String> pseudonymizedColumns = new ArrayList<>();
        for (int i = 0; i < pseudonymizeArray.length(); i++) {
            pseudonymizedColumns.add(pseudonymizeArray.getString(i));
        }

        JSONArray insensitiveArray = medicalConfig.getJSONArray("insensitive_columns");
        List<String> insensitiveColumns = new ArrayList<>();
        for (int i = 0; i < insensitiveArray.length(); i++) {
            insensitiveColumns.add(insensitiveArray.getString(i));
        }

        JSONObject kAnonymizeConfig = medicalConfig.getJSONObject("k_anonymize");
        int k = kAnonymizeConfig.getInt("k");
*/
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
        //boolean allowRecordSuppression = medicalConfig.getString("allow_record_suppression").equalsIgnoreCase("true");
        boolean allowRecordSuppression2 = allowRecordSuppression.equalsIgnoreCase("true");
        if(allowRecordSuppression2){
            suppressionlimit = 1.0;
        }

        jsonResponseList = DatasetAnonymizer.setupAndAnonymizeDataset(
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
        return new ARXResponse("Success", jsonResponseList);
    }

    public static class ARXResponse {
        private String status;
        private List<Map<String, Object>> message;

        public ARXResponse(String status, List<Map<String, Object>> message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<Map<String, Object>> getMessage() {
            return message;
        }

        public void setMessage(List<Map<String, Object>> message) {
            this.message = message;
        }
    }
}
