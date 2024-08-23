import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
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
