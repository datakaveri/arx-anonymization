import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXLattice;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;
import org.json.JSONArray;
import org.json.JSONObject;

public class DatasetAnonymizer {
    public DatasetAnonymizer() {
    }

    public static ARXResult setupAndAnonymizeDataset(String metricType, String datasetPath, Charset charset, char delimiter, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths, List<Integer> sizes, Properties properties, int k, double suppressionLimit, Metric<?> metric) throws IOException, NoSuchAlgorithmException {
        String[] attributesToSuppress = properties.getProperty("suppress").split(",");
        String[] attributesToPseudonymize = properties.getProperty("pseudonymize").split(",");
        Suppress.suppression(datasetPath, attributesToSuppress);
        Pseudonymize.pseudonymization(datasetPath, attributesToPseudonymize);
        Data dataset = Data.create("/home/kailash/Desktop/arx_anonymization/pseudonymized.csv", charset, delimiter);
        setupDataset(dataset, hierarchyLevels, intervalWidths, properties, sizes);
        ARXConfiguration config = createARXConfiguration(k, suppressionLimit, metric);
        return anonymizeAndAnalyze(metricType, dataset, config);
    }

    private static void setupDataset(Data dataset, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths, Properties properties, List<Integer> sizes) {
        HierarchyBuilderUtil.buildHierarchies(dataset, hierarchyLevels, intervalWidths, sizes);
        setAttributes(dataset, properties.getProperty("insensitive.columns").split(","), AttributeType.INSENSITIVE_ATTRIBUTE);
    }

    private static void setAttributes(Data dataset, String[] columns, AttributeType type) {
        String[] var3 = columns;
        int var4 = columns.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String attribute = var3[var5];
            dataset.getDefinition().setAttributeType(attribute, type);
        }

    }

    private static ARXConfiguration createARXConfiguration(int k, double suppressionLimit, Metric<?> metric) {
        ARXConfiguration config = ARXConfiguration.create();
        config.addPrivacyModel(new KAnonymity(k));
        config.setSuppressionLimit(suppressionLimit);
        config.setQualityModel(metric);
        return config;
    }

    private static ARXResult anonymizeAndAnalyze(String metricType, Data dataset, ARXConfiguration config) throws IOException {
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(dataset, config);
        DataHandle outputhandle = result.getOutput();
        outputAnonymizedDataset(outputhandle);
        ARXLattice.ARXNode transformation = result.getGlobalOptimum();
        System.out.println("Node used for anonymizing using " + metricType + ": " + Arrays.toString(transformation.getTransformation()));
        return result;
    }

    private static void outputAnonymizedDataset(DataHandle handle) throws IOException {
        JSONArray jsonArray = new JSONArray();
        CsvParserSettings parserSettings = new CsvParserSettings();
        CsvParser parser2 = new CsvParser(parserSettings);
        List<String[]> allRows2 = parser2.parseAll(new File("/home/kailash/Desktop/arx_anonymization/pseudonymized.csv"));
        String[] headers = (String[])allRows2.get(0);

        for(int rowIndex = 0; rowIndex < handle.getNumRows(); ++rowIndex) {
            JSONObject jsonObject = new JSONObject();

            for(int colIndex = 0; colIndex < headers.length; ++colIndex) {
                String columnName = headers[colIndex];
                String value = handle.getValue(rowIndex, colIndex);
                jsonObject.put(columnName, value);
            }

            jsonArray.put(jsonObject);
        }

        FileWriter fileWriter = new FileWriter("anonymized_output.json");

        try {
            fileWriter.write(jsonArray.toString(4));
        } catch (Throwable var12) {
            try {
                fileWriter.close();
            } catch (Throwable var11) {
                var12.addSuppressed(var11);
            }

            throw var12;
        }

        fileWriter.close();
        handle.save(new File("/home/kailash/Desktop/arx_anonymization/anonymized_datset.csv"), ',');
        System.out.println("Anonymized dataset saved to /home/kailash/Desktop/arx_anonymization/anonymized_datset.csv");
    }
}