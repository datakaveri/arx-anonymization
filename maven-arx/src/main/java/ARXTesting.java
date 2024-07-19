import org.deidentifier.arx.*;
import org.deidentifier.arx.aggregates.StatisticsFrequencyDistribution;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class ARXTesting {

    public static void main(String[] args) throws IOException {
        Properties properties = loadProperties("/home/kailash/Desktop/arx-anonymization/maven-arx/src/main/java/config.properties");

        String datasetPath = properties.getProperty("dataset.path");
        Charset charset = Charset.forName(properties.getProperty("dataset.charset"));
        char delimiter = properties.getProperty("delimiter").charAt(0);

        // Print columns in the dataset
        printDatasetColumns(datasetPath, charset, delimiter);

        // Parse column hierarchy configuration
        Map<String, Integer> hierarchyLevels = new HashMap<>();
        Map<String, Double> intervalWidths = new HashMap<>();
        parseColumnHierarchyConfig(properties.getProperty("columns.hierarchy").split(","), hierarchyLevels, intervalWidths);

        int k = Integer.parseInt(properties.getProperty("k.anonymity"));
        double suppressionLimit = Double.parseDouble(properties.getProperty("suppression.limit"));

        // Set up and anonymize the dataset using Loss Metric (LM)
        setupAndAnonymizeDataset("Loss Metric (LM)", datasetPath, charset, delimiter, hierarchyLevels, intervalWidths, properties, k, suppressionLimit, Metric.createLossMetric());
        // Set up and anonymize the dataset using Discernability Metric (DM*)
        setupAndAnonymizeDataset("Discernability Metric (DM*)", datasetPath, charset, delimiter, hierarchyLevels, intervalWidths, properties, k, suppressionLimit, Metric.createDiscernabilityMetric());
    }

    /**
     * Loads properties from a configuration file.
     *
     * @param filePath Path to the configuration file.
     * @return Properties object containing configuration properties.
     * @throws IOException If an I/O error occurs.
     */
    private static Properties loadProperties(String filePath) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            properties.load(inputStream);
        }
        return properties;
    }

    /**
     * Prints the columns of a dataset.
     *
     * @param datasetPath Path to the dataset.
     * @param charset Charset of the dataset.
     * @param delimiter Delimiter used in the dataset.
     * @throws IOException If an I/O error occurs.
     */
    private static void printDatasetColumns(String datasetPath, Charset charset, char delimiter) throws IOException {
        Data dataset = Data.create(datasetPath, charset, delimiter);
        System.out.println("Columns in the dataset:");
        for (int i = 0; i < dataset.getHandle().getNumColumns(); i++) {
            System.out.println(i + ": " + dataset.getHandle().getAttributeName(i));
        }
    }

    /**
     * Parses the column hierarchy configuration and populates the hierarchy levels and interval widths.
     *
     * @param columnHierarchyConfig Array of column hierarchy configurations.
     * @param hierarchyLevels Map to store hierarchy levels.
     * @param intervalWidths Map to store interval widths.
     */
    private static void parseColumnHierarchyConfig(String[] columnHierarchyConfig, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths) {
        for (String config : columnHierarchyConfig) {
            String[] parts = config.split(":");
            String columnName = parts[0];
            int numLevels = Integer.parseInt(parts[1]);
            double intervalWidth = Double.parseDouble(parts[2]);
            hierarchyLevels.put(columnName, numLevels);
            intervalWidths.put(columnName, intervalWidth);
        }
    }

    /**
     * Sets up and anonymizes the dataset with the given configuration and metric type.
     *
     * @param metricType Type of metric used for anonymization.
     * @param datasetPath Path to the dataset.
     * @param charset Charset of the dataset.
     * @param delimiter Delimiter used in the dataset.
     * @param hierarchyLevels Map of hierarchy levels.
     * @param intervalWidths Map of interval widths.
     * @param properties Properties object containing configuration properties.
     * @param k Value of k for k-anonymity.
     * @param suppressionLimit Suppression limit for anonymization.
     * @param metric Metric used for anonymization.
     * @throws IOException If an I/O error occurs.
     */
    private static void setupAndAnonymizeDataset(String metricType, String datasetPath, Charset charset, char delimiter,
                                                 Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths,
                                                 Properties properties, int k, double suppressionLimit, Metric<?> metric) throws IOException {
        Data dataset = Data.create(datasetPath, charset, delimiter);
        setupDataset(dataset, hierarchyLevels, intervalWidths, properties);

        ARXConfiguration config = createARXConfiguration(k, suppressionLimit, metric);
        anonymizeAndAnalyze(metricType, dataset, config);
    }

    /**
     * Sets up the dataset by building hierarchies and setting attribute types.
     *
     * @param dataset Dataset to set up.
     * @param hierarchyLevels Map of hierarchy levels.
     * @param intervalWidths Map of interval widths.
     * @param properties Properties object containing configuration properties.
     * @throws IOException If an I/O error occurs.
     */
    private static void setupDataset(Data dataset, Map<String, Integer> hierarchyLevels, Map<String, Double> intervalWidths, Properties properties) throws IOException {
        HierarchyBuilderUtil.buildHierarchies(dataset, hierarchyLevels, intervalWidths);
        setAttributes(dataset, properties.getProperty("identifying.columns").split(","), AttributeType.IDENTIFYING_ATTRIBUTE);
        setAttributes(dataset, properties.getProperty("insensitive.columns").split(","), AttributeType.INSENSITIVE_ATTRIBUTE);
    }

    /**
     * Sets attribute types for the specified columns in the dataset.
     *
     * @param dataset Dataset in which to set attribute types.
     * @param columns Array of column names.
     * @param type Attribute type to set.
     */
    private static void setAttributes(Data dataset, String[] columns, AttributeType type) {
        for (String attribute : columns) {
            dataset.getDefinition().setAttributeType(attribute, type);
        }
    }

    /**
     * Creates an ARX configuration with the specified k-anonymity, suppression limit, and metric.
     *
     * @param k Value of k for k-anonymity.
     * @param suppressionLimit Suppression limit for anonymization.
     * @param metric Metric used for anonymization.
     * @return ARXConfiguration object.
     */
    private static ARXConfiguration createARXConfiguration(int k, double suppressionLimit, Metric<?> metric) {
        ARXConfiguration config = ARXConfiguration.create();
        config.addPrivacyModel(new KAnonymity(k));
        config.setSuppressionLimit(suppressionLimit);
        config.setQualityModel(metric);
        return config;
    }

    /**
     * Anonymizes and analyzes the dataset using the specified metric type and configuration.
     *
     * @param metricType Type of metric used for anonymization.
     * @param dataset Dataset to anonymize.
     * @param config ARX configuration for anonymization.
     * @throws IOException If an I/O error occurs.
     */
    private static void anonymizeAndAnalyze(String metricType, Data dataset, ARXConfiguration config) throws IOException {
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(dataset, config);
        ARXLattice.ARXNode transformation = result.getGlobalOptimum();

        System.out.println("Optimum Node used for anonymizing using " + metricType + ": " + Arrays.toString(transformation.getTransformation()));

        Map<String, Integer> anonymizationLevels = getAnonymizationLevels(dataset, transformation);
        analyzeColumns(result.getOutput(), anonymizationLevels, transformation);
    }

    /**
     * Retrieves the anonymization levels for each column in the dataset.
     *
     * @param dataset Dataset for which to retrieve anonymization levels.
     * @param transformation ARX transformation node.
     * @return Map of anonymization levels for each column.
     */
    private static Map<String, Integer> getAnonymizationLevels(Data dataset, ARXLattice.ARXNode transformation) {
        Map<String, Integer> anonymizationLevels = new HashMap<>();
        for (int i = 0; i < dataset.getHandle().getNumColumns(); i++) {
            String columnName = dataset.getHandle().getAttributeName(i);
            anonymizationLevels.put(columnName, transformation.getGeneralization(columnName));
        }
        return anonymizationLevels;
    }

    /**
     * Analyzes and prints the frequency distribution for a specified column in the dataset.
     *
     * @param handle Data handle for the anonymized dataset.
     * @param anonymizationLevels Map of anonymization levels for each column.
     * @param transformation ARX transformation node.
     */
    private static void analyzeColumns(DataHandle handle, Map<String, Integer> anonymizationLevels, ARXLattice.ARXNode transformation) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter the index of the column to analyze (or type 'exit' to quit): ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            try {
                int columnIndex = Integer.parseInt(input);
                StatisticsFrequencyDistribution freqDist = handle.getStatistics().getFrequencyDistribution(columnIndex);
                String[] values = freqDist.values;
                double[] frequencies = freqDist.frequency;

                List<Pair<String, Double>> pairList = new ArrayList<>();
                for (int i = 0; i < values.length; i++) {
                    pairList.add(new Pair<>(values[i], frequencies[i]));
                }

                pairList.sort(Comparator.comparing(Pair::getValue));

                System.out.println("Frequency distribution for column: " + handle.getAttributeName(columnIndex));
                for (Pair<String, Double> pair : pairList) {
                    System.out.println(pair.getKey() + ": " + pair.getValue() * freqDist.count);
                }

                String columnName = handle.getAttributeName(columnIndex);
                if (anonymizationLevels.containsKey(columnName)) {
                    int actualLevel = transformation.getGeneralization(columnName);
                    System.out.println("Anonymization level for column " + columnName + ": " + actualLevel);
                } else {
                    System.out.println("No anonymization level set for column " + columnName);
                }

            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid column index or 'exit' to quit.");
            }
        }
    }

    /**
     * A simple Pair class to hold key-value pairs.
     *
     * @param <K> Type of the key.
     * @param <V> Type of the value.
     */
    static class Pair<K, V> {
        private final K key;
        private final V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}
