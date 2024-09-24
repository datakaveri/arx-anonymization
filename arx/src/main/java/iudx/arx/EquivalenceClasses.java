package iudx.arx;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
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


public class EquivalenceClasses {

    private List<Map<String, String>> data;
    private List<Map<String, Object>> equivalenceClasses;
    private List<Map<String, Object>> classSizes;

    // Constructor to load CSV data
    public EquivalenceClasses(String csvFile) throws IOException {
        data = readCsv(csvFile);
    }

    // Read CSV file and return data as List of Maps
    private List<Map<String, String>> readCsv(String csvFile) throws IOException {
        List<Map<String, String>> records = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(Paths.get(csvFile));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> row = new HashMap<>();
                csvParser.getHeaderMap().forEach((key, value) -> row.put(key, csvRecord.get(key)));
                records.add(row);
            }
        }
        return records;
    }

    // Group data based on specified columns to form equivalence classes
    public List<Map<String, Object>> computeEquivalenceClasses(List<String> columns) {
        equivalenceClasses = data.stream()
            .collect(Collectors.groupingBy(row -> columns.stream()
                .map(row::get)
                .collect(Collectors.toList())))
            .entrySet().stream()
            .map(entry -> {
                Map<String, Object> map = new HashMap<>();
                map.put("Group", entry.getKey());
                map.put("Size", entry.getValue().size());
                return map;
            }).collect(Collectors.toList());

        // Calculate size of each equivalence class
        classSizes = equivalenceClasses.stream()
            .map(group -> {
                Map<String, Object> sizeMap = new HashMap<>(group);
                sizeMap.put("Size", group.get("Size"));
                return sizeMap;
            }).collect(Collectors.toList());

        return classSizes;
    }

    // Remove outliers using the IQR method
    public void removeOutliers() {
        if (classSizes == null) {
            throw new IllegalStateException("Equivalence classes not computed. Call computeEquivalenceClasses() first.");
        }

        List<Double> sizes = classSizes.stream()
            .map(map -> Double.valueOf(map.get("Size").toString()))
            .sorted()
            .collect(Collectors.toList());

        // Compute Q1 and Q3
        double Q1 = sizes.get((int) (sizes.size() * 0.25));
        double Q3 = sizes.get((int) (sizes.size() * 0.75));
        double IQR = Q3 - Q1;

        // Define bounds for non-outliers
        double lowerBound = Q1 - 1.5 * IQR;
        double upperBound = Q3 + 1.5 * IQR;

        // Filter out the outliers
        classSizes = classSizes.stream()
            .filter(map -> {
                double size = Double.valueOf(map.get("Size").toString());
                return size >= lowerBound && size <= upperBound;
            })
            .collect(Collectors.toList());
    }

    // Generate statistics for equivalence classes
    public Map<String, Object> generateStats() {
        if (classSizes == null) {
            throw new IllegalStateException("Equivalence classes not computed. Call computeEquivalenceClasses() first.");
        }

        // Count how many equivalence classes there are of each size
        Map<Integer, Long> sizeCounts = classSizes.stream()
            .collect(Collectors.groupingBy(map -> Integer.valueOf(map.get("Size").toString()), Collectors.counting()));

        // Prepare stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_equivalence_classes", classSizes.size());
        stats.put("size_distribution", sizeCounts);
        return stats;
    }

    public void saveStatsToJson(String outputJsonFile) throws IOException {
        Map<String, Object> stats = generateStats();
        ObjectMapper objectMapper = new ObjectMapper();

        File jsonFile = new File(outputJsonFile);
        if (!jsonFile.exists()) {
            System.out.println("Info: JSON file not found. Creating a new one.");
            jsonFile.createNewFile();
        } else {
            System.out.println("Info: JSON file found. Updating the existing file.");
        }


        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, stats);
        System.out.println("Equivalence stats have been saved to " + outputJsonFile);
    }

    // Main method to demonstrate the functionality
    public static void main(List<String> generalizedColumns) {
        try {
            System.out.println(generalizedColumns);
            
            EquivalenceClasses eqClasses = new EquivalenceClasses("anonymized_output.csv");

            // Specify the columns for equivalence classes
            eqClasses.computeEquivalenceClasses(generalizedColumns);

            // Remove outliers
            eqClasses.removeOutliers();

            // Save stats to JSON
            eqClasses.saveStatsToJson("equivalence_stats.json");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
