package iudx.arx;

import org.springframework.stereotype.Service;

import org.deidentifier.arx.*;
import org.deidentifier.arx.aggregates.StatisticsFrequencyDistribution;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.json.JSONArray;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import java.security.NoSuchAlgorithmException;


@Service
public class ARXTestingService {

    public ARXResponse processProperties() throws IOException, NoSuchAlgorithmException {
        List<Map<String, Object>> jsonResponseList;
        Properties properties = ConfigLoader.loadProperties("src/main/java/iudx/arx/config.properties");
        String datasetPath = properties.getProperty("dataset.path");
        Charset charset = Charset.forName(properties.getProperty("dataset.charset"));
        char delimiter = properties.getProperty("delimiter").charAt(0);
        Map<String, Integer> hierarchyLevels = new HashMap<>();
        Map<String, Double> intervalWidths = new HashMap<>();
        List<Integer> sizes = new ArrayList<>();
        ColumnHierarchyConfigParser.parse(properties.getProperty("columns.hierarchy").split(","), hierarchyLevels, intervalWidths, sizes);
        int k = Integer.parseInt(properties.getProperty("k.anonymity"));
        double suppressionLimit = Double.parseDouble(properties.getProperty("suppression.limit"));
        jsonResponseList = DatasetAnonymizer.setupAndAnonymizeDataset("Loss Metric (LM)", datasetPath, charset, delimiter, hierarchyLevels, intervalWidths, sizes, properties, k, suppressionLimit, Metric.createLossMetric());
        return new ARXResponse("Success", jsonResponseList);
    }
    // Response class to encapsulate the JSON response data
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
