import pandas as pd
import json

def generate_metadata(file_path, output_file):
    # Load dataset
    df = pd.read_csv(file_path)
    
    # Initialize metadata dictionary
    metadata = {}
    
    for column in df.columns:
        # Initialize column metadata dictionary
        column_metadata = {}
        
        # Get data type
        column_metadata['data_type'] = str(df[column].dtype)
        
        if pd.api.types.is_numeric_dtype(df[column]):
            # If numeric, get min and max
            column_metadata['min'] = df[column].min()
            column_metadata['max'] = df[column].max()
        else:
            # If categorical, get number of unique values
            column_metadata['unique_values_count'] = df[column].nunique()
        
        # Add column metadata to main metadata dictionary
        metadata[column] = column_metadata
    
    # Save metadata to JSON file
    with open(output_file, 'w') as file:
        json.dump(metadata, file, indent=4)
    
    print(f"Metadata has been saved to {output_file}")

# Example usage
file_path = '/home/kailash/Desktop/arx-anonymization/arx/Medical_Data_new.csv'  # Replace with your dataset path
output_file = 'metadata.json'
generate_metadata(file_path, output_file)
