package iudx.arx;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Suppress {
    public static void suppression(String datasetPath, String[] attributesToSuppress) throws IOException {
        CsvParserSettings parserSettings = new CsvParserSettings();
        CsvParser parser = new CsvParser(parserSettings);
        List<String[]> allRows = parser.parseAll(new File(datasetPath));
        String[] headers = allRows.get(0);
        List<String> headersList = new ArrayList<>(Arrays.asList(headers));
        System.out.println(headersList);
        List<Integer> indicesToSuppress = new ArrayList<>();

        for (String attribute : attributesToSuppress) {
            int index = headersList.indexOf(attribute);
            if (index != -1) {
                indicesToSuppress.add(index);
            } else {
                System.out.println("Warning: Column '" + attribute + "' not found in the dataset.");
            }
        }

        if (indicesToSuppress.isEmpty()) {
            System.out.println("No matching columns found to suppress.");
        } else {
            CsvWriter writer = getCsvWriter(headers, indicesToSuppress, allRows);
            writer.close();
            System.out.println("Suppressed dataset created successfully: suppressed.csv");
        }
    }

    private static CsvWriter getCsvWriter(String[] headers, List<Integer> indicesToSuppress, List<String[]> allRows) throws IOException {
        List<String> newHeaders = new ArrayList();

        for(int i = 0; i < headers.length; ++i) {
            if (!indicesToSuppress.contains(i)) {
                newHeaders.add(headers[i]);
            }
        }

        CsvWriterSettings writerSettings = new CsvWriterSettings();
        CsvWriter writer = new CsvWriter(new FileWriter("suppressed.csv"), writerSettings);
        writer.writeRow(newHeaders);

        for(int rowIndex = 1; rowIndex < allRows.size(); ++rowIndex) {
            String[] row = (String[])allRows.get(rowIndex);
            List<String> newRow = new ArrayList();

            for(int i = 0; i < row.length; ++i) {
                if (!indicesToSuppress.contains(i)) {
                    newRow.add(row[i]);
                }
            }

            writer.writeRow(newRow);
        }

        return writer;
    }
}
