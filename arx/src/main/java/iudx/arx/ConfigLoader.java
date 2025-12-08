package iudx.arx;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    /**
     * Loads configuration properties from a specified config file.
     *
     * Description:
     *   This function reads a configuration file
     *   from the given file path and loads its key-value pairs into a `Properties` object. 
     *   The input stream is safely closed even if an exception occurs during loading.
     *
     * @param filePath The path to the configuration file to load.
     * @return Properties A Properties object containing the configuration key-value pairs.
     * @throws IOException If there is an error reading the configuration file.
     */

    public static Properties loadProperties(String filePath) throws IOException {
        Properties properties = new Properties();
        FileInputStream inputStream = new FileInputStream(filePath);

        try {
            properties.load(inputStream);
        } catch (Throwable var6) {
            try {
                inputStream.close();
            } catch (Throwable var5) {
                var6.addSuppressed(var5);
            }

            throw var6;
        }

        inputStream.close();
        return properties;
    }
}
