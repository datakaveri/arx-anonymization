import subprocess
import requests
import json
import os
import time
import shutil

# Run the Java application in the background
source = 'data/data1.csv'
destination = './Medical_Data_new.csv'
shutil.copy(source, destination)

subprocess.Popen(["java", "-jar", "app.jar"])

print(os.getcwd())

# Path to the config file
config_path = 'config/config.json'

# Ensure the config file exists
if not os.path.exists(config_path):
    raise FileNotFoundError(f"Config file not found: {config_path}")

# Read the configuration from config.json
with open(config_path, 'r') as config_file:
    config = json.load(config_file)

# Prepare the parameters to send to the service
dataset_type = config['data_type']
params = {
    "datasetType": config['data_type'],
    "k": config[dataset_type]['k_anonymize']['k'],
    "suppress_columns": ','.join(config[dataset_type]['suppress']),
    "pseudonymize_columns": ','.join(config[dataset_type]['pseudonymize']),
    "generalized_columns": ','.join(config[dataset_type]['generalize']),
    "insensitive_columns": ','.join(config[dataset_type]['insensitive_columns']),
    "widths":config[dataset_type]['width'],
    "num_levels":config[dataset_type]['levels'],
    "allow_record_suppression": config[dataset_type]['allow_record_suppression']
}
print(params)


time.sleep(5)

url = 'http://localhost:8070/api/arx/process'
headers = {
    'Content-Type':'application/json'
}
response = requests.post(url, headers=headers, data =json.dumps(params))

if response.status_code == 200:
    # Write the JSON data to the file
    with open("output/response.json", "w") as f:
         json.dump(json.loads(response.text), f)
    print(f"Response stored")
    
else:
    print(f"Failed to fetch JSON data. Status code: {response.status_code}")
