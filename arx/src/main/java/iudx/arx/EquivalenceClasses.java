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

    /**
     * Reads a CSV file and returns its contents as a list of maps.
     *
     * Description:
     *   This function parses the specified CSV file, treating the first row as headers.
     *   Each subsequent row is converted into a map where keys are column headers and 
     *   values are the corresponding cell values. All rows are collected into a list.
     *
     * @param csvFile The path to the CSV file to be read.
     * @return List<Map<String, String>> A list of maps representing rows of the CSV file.
     * @throws IOException If there is an error reading the CSV file.
     */

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

    /**
     * Computes equivalence classes by grouping data based on specified columns.
     *
     * Description:
     *   This function groups rows of the dataset according to the values in the specified 
     *   columns, forming equivalence classes. For each group, it calculates the size 
     *   (number of rows) and returns a list of maps containing the group values and sizes.
     *
     * @param columns An array of column names to group by when forming equivalence classes.
     * @return List<Map<String, Object>> A list of maps where each map represents an equivalence class 
     *                                    with keys "Group" (list of values) and "Size" (number of rows).
     */

    public List<Map<String, Object>> computeEquivalenceClasses(String[] columns) {
        equivalenceClasses = data.stream()
            .collect(Collectors.groupingBy(row -> Arrays.stream(columns)
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
    

    /**
     * Removes outliers from the equivalence classes using the IQR (Interquartile Range) method.
     *
     * Description:
     *   This function filters out equivalence classes whose sizes are considered outliers.
     *   Outliers are determined based on the IQR: any class with size below Q1 - 1.5*IQR
     *   or above Q3 + 1.5*IQR is removed. The method updates the `classSizes` list.
     *
     * @throws IllegalStateException If equivalence classes have not been computed before calling this method.
     */

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

    /**
     * Generates statistics for the equivalence classes.
     *
     * Description:
     *   This function calculates summary statistics for the equivalence classes, including:
     *     1. The total number of equivalence classes.
     *     2. A distribution of equivalence class sizes (how many classes have each size).
     *
     * @return Map<String, Object> A map containing:
     *         - "total_equivalence_classes": total number of classes.
     *         - "size_distribution": a map of class sizes to their frequency.
     * @throws IllegalStateException If equivalence classes have not been computed before calling this method.
     */

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

    /**
     * Saves the equivalence class statistics to a JSON file.
     *
     * Description:
     *   This function generates statistics for the equivalence classes and writes them 
     *   to the specified JSON file. If the file does not exist, it is created; if it 
     *   exists, the file is updated with the new statistics. The JSON output is formatted 
     *   with pretty printing for readability.
     *
     * @param outputJsonFile The path to the JSON file where statistics will be saved.
     * @throws IOException If there is an error creating or writing to the JSON file.
     */


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
    public static void main(String[] generalizedColumns) {
        try {
            
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
