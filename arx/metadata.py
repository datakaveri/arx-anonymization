import pandas as pd
import json
import io

csv_file = '/home/kailash/Desktop/arx_anonymization/arx/Medical_Data_new.csv' 
df = pd.read_csv(csv_file)
buffer = io.StringIO()
df.info(buf=buffer)
info_str = buffer.getvalue()

min_max_values = {}
numeric_columns = df.select_dtypes(include=['int64', 'float64']).columns

for col in numeric_columns:
    min_max_values[col] = {
        "min": int(df[col].min()) if pd.api.types.is_integer_dtype(df[col]) else float(df[col].min()),
        "max": int(df[col].max()) if pd.api.types.is_integer_dtype(df[col]) else float(df[col].max())
    }
allowed_quasi_identifiers = list(numeric_columns)


info_lines = info_str.splitlines() 

metadata = {
    "info": info_lines,  # Store each line as an element in the list
    "numeric_columns_min_max": min_max_values,  # Add min and max for numeric columns
    "allowed_quasi_identifiers": allowed_quasi_identifiers  # List of numeric columns as quasi-identifiers
}

with open('metadata.json', 'w') as json_file:
    json.dump(metadata, json_file, indent=4)

print("Metadata with min/max and quasi-identifiers saved to metadata.json")
