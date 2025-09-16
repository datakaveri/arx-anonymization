# ARX Anonymisation

This repository implements a data anonymization pipeline using the ARX framework (https://arx.deidentifier.org/anonymization-tool/). It is built as a Spring Boot application that exposes a REST API to anonymize datasets based on configurable privacy models like  k-anonymity, l-diversity, and t-closeness.

## Features

- Supports k-anonymity
- Handles numerical quasi-identifiers
- Analytics and risk metrics logging, including equivalence class sizes and suppression rates.
- Python integration support via automation scripts to trigger end-to-end anonymization from external tools or pipelines.

## Installation
<pre><code>mvn install:install-file \
  -Dfile=lib/libarx-3.9.1.jar \
  -DgroupId=org.deidentifier \
  -DartifactId=arx \
  -Dversion=3.9.1 \
  -Dpackaging=jar</code></pre>

<pre><code>cd arx
mvn clean install </code> </pre>

## Usage
The following command will start the Spring Boot application.
<pre><code>java -jar /full/path/to/maven_arx-1.0-SNAPSHOT.jar</code></pre>

In a separate terminal, run the following command:
<pre><code>python3 /full/path/to/test_arx_service.py</code></pre>

## Configuration file (`config.json`)

```json
{
  "operations": [
    "suppress",
    "pseudonymize",
    "k_anonymize"
  ],
  "dataset_name": "syntheticMedicalDataset.csv",
  "data_type": "medical",
  "medical": {
    "insensitive_columns": [
      "S.No",
      "Gender",
      "Test Result",
      "Blood Pressure"
    ],
    "suppress": [
      "Date of Birth",
      "Address"
    ],
    "pseudonymize": [
      "Name",
      "Patient ID"
    ],
    "generalize": [
      "Height",
      "Weight",
      "PIN Code",
      "Age"
    ],
    "width": {
      "Height": 5,
      "Weight": 4,
      "Age": 6
    },
    "levels": {
      "Height": 3,
      "Weight": 6,
      "Age": 4
    },
    "k_anonymize": {
      "k": 49
    },
    "allow_record_suppression": true
  }
}
```

## Example Workflow

1. Ensure your dataset is in CSV format (e.g., `Medical_Data_new.csv`)
2. Configure Anonymisation Parameters
3. Build and run the Spring Boot application
<pre><code>mvn clean install
java -jar /full/path/to/maven_arx-1.0-SNAPSHOT.jar</code></pre>
4. Send API request by running `test_arx_service.py`
<pre><code>python3 /full/path/to/test_arx_service.py</code></pre>

### Output files

| File Name                | Description                                |
|--------------------------|--------------------------------------------|
| `suppressed.csv`         | Dataset with sensitive columns suppressed  |
| `pseudonymized.csv`      | Identifiers pseudonymized                  |
| `anonymized_output.csv`  | Final anonymized dataset                   |
| `equivalence_stats.json` | Equivalence class analysis                 |
| `anonymized_output.json` | Summary of anonymization run & config used |


## Project Structure
<pre><code>.
├── Dockerfile
├── README.md
├── anonymized_output_compare.json
├── arx
│   ├── Dockerfile
│   ├── Medical_Data_new.csv
│   ├── analytics.json
│   ├── anonymized_output.csv
│   ├── anonymized_output.json
│   ├── config.json
│   ├── equivalence_stats.json
│   ├── metadat_gnerator.py
│   ├── metadata.py
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   ├── pseudonymized.csv
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── iudx
│   │   │   │       └── arx
│   │   │   └── resources
│   │   │       ├── application.properties
│   │   │       └── config.properties
│   │   └── test
│   │       └── java
│   │           └── iudx
│   │               └── arx
│   ├── suppressed.csv
│   ├── target
│   │   ├── classes
│   │   │   ├── application.properties
│   │   │   ├── config.properties
│   │   │   └── iudx
│   │   │       └── arx
│   │   │           ├── ARXRequestBody.class
│   │   │           ├── ARXTestingController.class
│   │   │           ├── ARXTestingService$ARXResponse.class
│   │   │           ├── ARXTestingService.class
│   │   │           ├── AppendAnalytics.class
│   │   │           ├── ArxApplication.class
│   │   │           ├── ColumnHierarchyConfigParser.class
│   │   │           ├── ConfigLoader.class
│   │   │           ├── DataAnalysis.class
│   │   │           ├── DatasetAnonymizer.class
│   │   │           ├── EquivalenceClasses.class
│   │   │           ├── HierarchyBuilderUtil.class
│   │   │           ├── Pseudonymize.class
│   │   │           └── Suppress.class
│   │   ├── generated-sources
│   │   │   └── annotations
│   │   ├── generated-test-sources
│   │   │   └── test-annotations
│   │   ├── maven-archiver
│   │   │   └── pom.properties
│   │   ├── maven-status
│   │   │   └── maven-compiler-plugin
│   │   │       ├── compile
│   │   │       │   └── default-compile
│   │   │       └── testCompile
│   │   │           └── default-testCompile
│   │   ├── maven_arx-1.0-SNAPSHOT.jar
│   │   ├── maven_arx-1.0-SNAPSHOT.jar.original
│   │   └── test-classes
│   └── test_arx_service.py
├── docker-compose.yml
├── entrypoint.py
├── lib
│   └── libarx-3.9.1.jar
├── metadata.json
├── output
├── pom.xml
├── qodana.yaml
└── requirements.txt
</code></pre>

## How It Works

1. API Triggered

  A user sends a POST request to the REST endpoint with a config.json containing the dataset path, attribute roles, and privacy model parameters.

2. Configuration Loaded

  The application parses the JSON configuration to determine the dataset to anonymise, which columns are sensitive, quasi-identifiers, pseudonymized, suppressed, etc, and the values for k, l and t (k-anonymity, l-diversity and t-closeness respectively).

3. Data Preprocessing

  Suppressed columns are removed, pseudonymized columns are replaced with random or hashed values, and generalization hierarchies are applied to quasi-identifiers (if available).

4. Privacy Models Applied

  The ARX library is used to apply the selected privacy models:

    k-anonymity to ensure each record is indistinguishable from at least k-1 others

5. Anonymised Dataset Generated
