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
        // Configure the CSV parser
        CsvParserSettings parserSettings = new CsvParserSettings();
        //parserSettings.setHeaderExtractionEnabled(true); // Extract header row
        CsvParser parser = new CsvParser(parserSettings);

        // Parse all rows from the input file
        List<String[]> allRows = parser.parseAll(new File(datasetPath));

        // Extract the header row
        String[] headers = allRows.get(0);
        List<String> headersList = new ArrayList<>(Arrays.asList(headers));
        System.out.println(headersList);
        // Determine the indices of the columns to suppress
        List<Integer> indicesToSuppress = new ArrayList<>();
        for (String attribute : attributesToSuppress) {
            int index = headersList.indexOf(attribute);
            if (index != -1) {
                indicesToSuppress.add(index);
            } else {
                System.out.println("Warning: Column '" + attribute + "' not found in the dataset.");
            }
        }

        // If no valid columns were found to suppress, exit
        if (indicesToSuppress.isEmpty()) {
            System.out.println("No matching columns found to suppress.");
            return;
        }

        // Create a new list of headers excluding the suppressed ones
        CsvWriter writer = getCsvWriter(headers, indicesToSuppress, allRows);

        writer.close();
        System.out.println("Suppressed dataset created successfully: suppressed.csv");
    }

    private static CsvWriter getCsvWriter(String[] headers, List<Integer> indicesToSuppress, List<String[]> allRows) throws IOException {
        List<String> newHeaders = new ArrayList<>();
        for (int i = 0; i < headers.length; i++) {
            if (!indicesToSuppress.contains(i)) {
                newHeaders.add(headers[i]);
            }
        }

        // Configure the CSV writer
        CsvWriterSettings writerSettings = new CsvWriterSettings();
        CsvWriter writer = new CsvWriter(new FileWriter("suppressed.csv"), writerSettings);

        // Write the new header line
        writer.writeRow(newHeaders);

        // Iterate over each row and process it
        for (int rowIndex = 1; rowIndex < allRows.size(); rowIndex++) {
            String[] row = allRows.get(rowIndex);
            List<String> newRow = new ArrayList<>();
            for (int i = 0; i < row.length; i++) {
                if (!indicesToSuppress.contains(i)) {
                    newRow.add(row[i]);
                }
            }
            writer.writeRow(newRow);
        }
        return writer;
    }
}
