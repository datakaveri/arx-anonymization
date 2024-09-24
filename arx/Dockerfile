FROM openjdk:17-jdk-alpine

WORKDIR /app

# Copy the jar file from the target directory of your Maven build into the container
COPY target/maven_arx-1.0-SNAPSHOT.jar app.jar
COPY Medical_Data_new.csv Medical_Data_new.csv
COPY suratITMS_data_csv_small.csv suratITMS_data_csv_small.csv
# Expose the port your application runs on
EXPOSE 8070

# Command to run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
