import json, requests

# necessary file reads
# config_file_name = "config/pipelineConfig.json"

# config = utils.read_config(config_file_name) 
url = 'http://localhost:8080/api/arx/process'

response = requests.get(url)

print("response status:", response.status_code)
with open('anonymized_output_compare.json','w') as f:
    json.dump(json.loads(response.text), f) 
