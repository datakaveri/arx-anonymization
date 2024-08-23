import org.deidentifier.arx.*;
import org.deidentifier.arx.metric.Metric;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Properties properties = ConfigLoader.loadProperties("/home/kailash/Desktop/arx_anonymization/maven_arx/src/main/java/config.properties");

        String datasetPath = properties.getProperty("dataset.path");
        Charset charset = Charset.forName(properties.getProperty("dataset.charset"));
        char delimiter = properties.getProperty("delimiter").charAt(0);

        // Parse column hierarchy configuration
        Map<String, Integer> hierarchyLevels = new HashMap<>();
        Map<String, Double> intervalWidths = new HashMap<>();
        List<Integer> sizes = new ArrayList<>();
        ColumnHierarchyConfigParser.parse(properties.getProperty("columns.hierarchy").split(","), hierarchyLevels, intervalWidths, sizes);

        int k = Integer.parseInt(properties.getProperty("k.anonymity"));
        double suppressionLimit = Double.parseDouble(properties.getProperty("suppression.limit"));

        // Set up and anonymize the dataset using Loss Metric (LM)
        ARXResult result = DatasetAnonymizer.setupAndAnonymizeDataset("Loss Metric (LM)", datasetPath, charset, delimiter, hierarchyLevels, intervalWidths, sizes, properties, k, suppressionLimit, Metric.createLossMetric());
    }
}
