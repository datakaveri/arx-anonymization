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
            String datasetType,
            int k_value,
            int l_value,
            double t_value,
            String suppress_columns,
            String pseudonymize_columns,
            String generalized_columns,
            String insensitive_columns,
            String sensitive_column,
            Map<String, Double> intervalWidths,
            Map<String, Integer> hierarchyLevels,
            String allowRecordSuppression
    ) throws IOException, NoSuchAlgorithmException {

        List<Map<String, Object>> jsonResponseList;
        List<String> suppressedColumns = Arrays.asList(suppress_columns.split(","));
        List<String> pseudonymizedColumns = Arrays.asList(pseudonymize_columns.split(","));
        List<String> insensitiveColumns = new ArrayList<>(Arrays.asList(insensitive_columns.split(",")));
        List<String> generalizedColumns = Arrays.asList(generalized_columns.split(","));
        String sensitiveColumn = sensitive_column;

        insensitiveColumns.addAll(pseudonymizedColumns);
        int k = k_value;
        int l = l_value;
        double t = t_value;
        String datasetPath = "Medical_Data_new.csv";
        if (datasetType.equals("spatioTemporal")){
            datasetPath = "suratITMS_data_csv_50K.csv";
        }
        if (datasetType.equals("soil_data")){
            datasetPath = "/home/kailash/Desktop/arx-anonymization/data/Telangana_Soil_data.csv";
        }
        System.out.println(datasetPath);
        Charset charset = Charset.forName("UTF-8");
        char delimiter = ',';
        List<Integer> sizes = new ArrayList<>();

        System.out.println(l);
        System.out.println(sensitiveColumn);
        for (String column : generalizedColumns) {
            if (hierarchyLevels.containsKey(column)) {
                int n = hierarchyLevels.get(column);
                for (int i = 0; i < n; i++) {
                    sizes.add(2);
                }
            } else {
                System.out.println("Warning: No hierarchy level found for column '" + column + "'.");
            }
        }

        double suppressionlimit = 0;
        boolean allowRecordSuppression2 = allowRecordSuppression.equalsIgnoreCase("true");
        if (allowRecordSuppression2) {
            suppressionlimit = 1.0; // Suppression limit is hardcoded to 1
        }

        jsonResponseList = DatasetAnonymizer.setupAndAnonymizeDataset(
                "Loss Metric (LM)",
                datasetPath,
                charset,
                delimiter,
                suppressedColumns.toArray(new String[0]),
                pseudonymizedColumns.toArray(new String[0]),
                insensitiveColumns.toArray(new String[0]),
                generalizedColumns.toArray(new String[0]),
                sensitiveColumn,
                hierarchyLevels,
                intervalWidths,
                sizes,
                k,
                l,
                t,
                suppressionlimit,
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
