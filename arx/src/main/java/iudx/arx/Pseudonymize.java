package iudx.arx;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pseudonymize {
    public static void pseudonymization(String datasetPath, String[] attributesToPseudonymize) throws IOException, NoSuchAlgorithmException {
        CsvParserSettings parserSettings = new CsvParserSettings();
        CsvParser parser3 = new CsvParser(parserSettings);
        List<String[]> allRows3 = parser3.parseAll(new File("suppressed.csv"));
        if (allRows3.isEmpty()) {
            throw new IOException("The file is empty.");
        } else {
            String[] headers = allRows3.get(0);
            List<String> headersList = new ArrayList<>(Arrays.asList(headers));
            List<Integer> indicesToPseudonymize = new ArrayList<>();

            for (String attribute : attributesToPseudonymize) {
                int index = headersList.indexOf(attribute);
                if (index != -1) {
                    indicesToPseudonymize.add(index);
                } else {
                    System.out.println("Warning: Column '" + attribute + "' not found in the dataset.");
                }
            }

            if (indicesToPseudonymize.isEmpty()) {
                System.out.println("No matching columns found to pseudonymize.");
            } else {
                CsvWriter writer = getCsvWriter(headers, indicesToPseudonymize, allRows3);
                writer.close();
                System.out.println("Pseudonymized dataset created successfully: pseudonymized.csv");
            }
        }
    }

    private static CsvWriter getCsvWriter(String[] headers, List<Integer> indicesToPseudonymize, List<String[]> allRows) throws IOException, NoSuchAlgorithmException {
        List<String> newHeaders = new ArrayList(Arrays.asList(headers));
        CsvWriterSettings writerSettings = new CsvWriterSettings();
        CsvWriter writer = new CsvWriter(new FileWriter("pseudonymized.csv"), writerSettings);
        writer.writeRow(newHeaders);

        for(int rowIndex = 1; rowIndex < allRows.size(); ++rowIndex) {
            String[] row = (String[])allRows.get(rowIndex);
            List<String> newRow = new ArrayList();
            Map<Integer, String> pseudonymizedValues = new HashMap();

            int i;
            for(i = 0; i < row.length; ++i) {
                if (indicesToPseudonymize.contains(i)) {
                    String pseudonymizedValue = pseudonymizeValue(row[i]);
                    pseudonymizedValues.put(i, pseudonymizedValue);
                }
            }

            for(i = 0; i < row.length; ++i) {
                if (indicesToPseudonymize.contains(i)) {
                    newRow.add((String)pseudonymizedValues.get(i));
                } else {
                    newRow.add(row[i]);
                }
            }

            writer.writeRow(newRow);
        }

        return writer;
    }

    private static String pseudonymizeValue(String value) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes());
        StringBuilder sb = new StringBuilder();
        byte[] var4 = hash;
        int var5 = hash.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            byte b = var4[var6];
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}