import json
import requests

# Step 1: Read the configuration file
config_file_path = "arx/config.json"
#config_file_path = "arx/config_suratITMS.json"

with open(config_file_path, 'r') as config_file:
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
# Step 3: Send the configuration to the service
url = 'http://localhost:8070/api/arx/process'
headers = {
    'Content-Type':'application/json'
}
response = requests.post(url, headers=headers, data =json.dumps(params))

# Step 4: Handle the response
print("Response status:", response.status_code)
with open('anonymized_output_compare.json', 'w') as f:
    json.dump(json.loads(response.text), f)