import json
import requests
import time
import math
import csv
import pandas as pd
import numpy as np
import os
from collections import Counter

# Step 1: Read the configuration file
config_file_path = "/mnt/d/PS2/ARX/arx-anonymization/arx/config_medical.json"
#config_file_path = "arx/config_suratITMS.json"

with open(config_file_path, 'r') as config_file:
    config = json.load(config_file)

# Prepare the parameters to send to the service
dataset_type = config['data_type']
params = {
    "dataset_name": "",
    "datasetType": config['data_type'],
    "num_chunks": config["num_chunks"],
    "k": config[dataset_type]['k_anonymize']['k'],
    "l":config[dataset_type]['k_anonymize']['l'],
    "suppress_columns": ','.join(config[dataset_type]['suppress']),
    "pseudonymize_columns": ','.join(config[dataset_type]['pseudonymize']),
    "generalized_columns": ','.join(config[dataset_type]['generalize']),
    "insensitive_columns": ','.join(config[dataset_type]['insensitive_columns']),
    "sensitive_column": config[dataset_type]['sensitive_column'][0],
    "widths":config[dataset_type]['width'],

    "num_levels":config[dataset_type]['levels'],
    #"allow_record_suppression": config[dataset_type]['allow_record_suppression'],
    "suppression_limit": config[dataset_type]['suppression_limit']  # Add this
    # Remove allow_record_suppression
}

# Step 3: Send the configuration to the service
url = 'http://localhost:8070/api/arx/process'
headers = {
    'Content-Type':'application/json'
}

# Categorical Hierarchies
blood_group_hierarchy = {
    1: lambda x: x,
    2: lambda x: {
    "A+": "A", "A-": "A", "B+": "B", "B-": "B",
    "AB+": "AB", "AB-": "AB", "O+": "O", "O-": "O"
    }.get(x, x),
    3: lambda x: "*"
}

profession_hierarchy = {
    0: lambda x: x,
    1: lambda x: {
        "Medical Specialists": "Healthcare", "Allied Health": "Healthcare", "Nursing": "Healthcare", "Healthcare Support": "Healthcare",
        "K-12 Education Teacher": "Education", "Higher Education Teacher": "Education", "Supplemental Education Teacher": "Education", "University Professor": "Education",
        "Performing Arts": "Creative", "Visual & Media Arts": "Creative", "Design": "Creative", "Mixed Media Artist": "Creative",
        "Traditional Engineering": "Engineering", "Software Engineering": "Engineering", "Data & Analytics": "Engineering", "AI & Machine Learning": "Engineering"
    }.get(x, x),
    2: lambda x: {
        "Medical Specialists": "Service Sector", "Allied Health": "Service Sector", "Nursing": "Service Sector", "Healthcare Support": "Service Sector",
        "K-12 Education Teacher": "Service Sector", "Higher Education Teacher": "Service Sector", "Supplemental Education Teacher": "Service Sector", "University Professor": "Service Sector",
        "Performing Arts": "Non-Service", "Visual & Media Arts": "Non-Service", "Design": "Non-Service", "Mixed Media Artist": "Non-Service",
        "Traditional Engineering": "Non-Service", "Software Engineering": "Non-Service", "Data & Analytics": "Non-Service", "AI & Machine Learning": "Non-Service"
    }.get(x, x),
    3: lambda x: "*"
}

gender_hierarchy = {
                1: {  # Level 0: No generalization
                    "Male": "Male", "Female" :"Female"
                },
                2: {  # Level 1: Fully generalized
                    "Male": "*", "Female" :"*"
                }
}

# Generalization function
def generalize_column(series, column_name, level, min_val=None, max_val=None, bin_width=None):
    if column_name == "Blood Group":
        return series.map(blood_group_hierarchy[level])
    elif column_name == "Gender":
        return series.map(gender_hierarchy[level])
    else:
        bin_edges = np.arange(min_val, max_val + bin_width + 1, bin_width)
        labels = [f"[{int(bin_edges[i])}-{int(bin_edges[i+1]-1)}]" for i in range(len(bin_edges)-1)]
        return pd.cut(series, bins=bin_edges, labels=labels, include_lowest=True)

# EXTRACTING BEST NODE AVERAGE

qi_order        = config[dataset_type]['generalize']
num_chunks      = config['num_chunks']
widths          = config[dataset_type]['width']      # numeric QIs
gen_level_sums  = {qi: 0 for qi in qi_order}

# 1‑based shift only for categoricals
categorical_qis = [qi for qi in qi_order if qi not in widths]

csv_path = "best_nodes.csv"

with open(csv_path, "w", newline="") as csvfile:
    writer = csv.DictWriter(csvfile, fieldnames=["chunk"] + qi_order)
    writer.writeheader()

    for i in range(1, num_chunks + 1):
        params["dataset_name"] = f"/mnt/d/PS2/ARX/arx-anonymization/arx/small_data/small_chunk{i}.csv"
        print(f"\nChunk {i} request: {params['dataset_name']}")
        r = requests.post(url, headers=headers, data=json.dumps(params))

        try:
            data = r.json()
            #print("Server JSON response:", json.dumps(data, indent=2))
        except Exception:
            print("Response is not valid JSON:", r.text)
            continue

        # Extract the list of rows
        if isinstance(data, list):
            rows = data
        elif isinstance(data, dict):
            lists = [v for v in data.values() if isinstance(v, list)]
            if not lists:
                print("No list found in response; can't extract rows.")
                continue
            rows = lists[0]
        else:
            print("Unexpected response structure")
            continue

        # Find the transformation_node entry
        transformation_node = None
        for row in rows:
            if isinstance(row, dict) and row.get("meta") is True:
                transformation_node = row.get("transformation_node")
                break

        if transformation_node is None:
            print(f"No transformation node found in chunk {i}")
            continue

        # Parse string list if necessary
        if isinstance(transformation_node, str):
            transformation_node = json.loads(transformation_node)
        if not isinstance(transformation_node, list):
            print("transformation node is not a list:", transformation_node)
            continue

        print("Best node for chunk", i, "→", transformation_node)

        # 1‑based shift for categoricals
        row = {"chunk": i}
        for idx, qi in enumerate(qi_order):
            lvl = transformation_node[idx]
            if qi in categorical_qis:
                lvl += 1
            row[qi] = lvl
            gen_level_sums[qi] += lvl

        print("Writing row:", row)
        writer.writerow(row)
        csvfile.flush()

        time.sleep(1)

# Computing & flooring the averages
print(f"\nAverage generalization levels across {num_chunks} chunks (floored):")
average_row = {"chunk": "average"}
for qi in qi_order:
    avg = gen_level_sums[qi] / num_chunks
    floored = math.floor(avg)
    print(f"   • {qi}: {floored}")
    average_row[qi] = floored

node = [average_row[qi] for qi in qi_order]
print("Node set to average:", node)

with open(csv_path, "a", newline="") as csvfile:
    writer = csv.DictWriter(csvfile, fieldnames=["chunk"] + qi_order)
    writer.writerow(average_row)
    print(f"Appended averages row to {csv_path}")

# COMPUTING DM*

#node = [2, 2, 7, 4]  
#data_folder = "small_data"
full_dataset_path = config['dataset_name']
#data_folder = os.path.basename(os.path.dirname(full_dataset_path))
data_folder = os.path.dirname(full_dataset_path)
k = config[dataset_type]['k_anonymize']['k']

qis = [
    {'column_name': 'Age', 'is_categorical': False, 'min_value': 0},
    {'column_name': 'Blood Group', 'is_categorical': True},
    {'column_name': 'PIN Code_encoded', 'is_categorical': False, 'min_value': 0},
    {'column_name': 'BMI_encoded', 'is_categorical': False, 'min_value': 0},
    #{'column_name': 'Profession', 'is_categorical': True},
]

n = num_chunks

# Step 1: First pass to find min/max for numeric columns
global_minmax = {}
for q in qis:
    if not q['is_categorical']:
        global_minmax[q['column_name']] = [np.inf, -np.inf]

# First lightweight scan
for i in range(1, 101):
    if n <= 0:
        break
    file_path = os.path.join(data_folder, f'small_chunk{i}.csv')
    if os.path.exists(file_path):
        df = pd.read_csv(file_path, usecols=[q['column_name'] for q in qis], dtype={
            'Age': np.float32,
            'PIN Code_encoded': np.int32
        })
        for col in global_minmax:
            global_minmax[col][0] = min(global_minmax[col][0], df[col].min())
            global_minmax[col][1] = max(global_minmax[col][1], df[col].max())
        n -= 1

# Step 2: Now second pass for DM* computation
histogram = Counter()
n = num_chunks

for i in range(1, 101):
    if n <= 0:
        break
    file_path = os.path.join(data_folder, f'small_chunk{i}.csv')
    if os.path.exists(file_path):
        print(f"\n📂 Processing chunk: {file_path}")
        df = pd.read_csv(file_path, dtype={
            'Age': np.float32,
            'PIN Code_encoded': np.int32
        })
        df["Blood Group"] = df["Blood Group"].astype('category')
        df["Gender"] = df["Gender"].astype('category')

        generalized_cols = []
        for (col_info, bw) in zip(qis, node):
            col_name = col_info['column_name']
            is_cat = col_info['is_categorical']
            if is_cat:
                min_val, max_val = None, None
            else:
                min_val, max_val = global_minmax[col_name]

            generalized = generalize_column(
                df[col_name],
                col_name,
                bw if is_cat else 0,
                min_val, max_val,
                bw if not is_cat else None
            )
            generalized_cols.append(generalized)

        df['__eq_class__'] = list(zip(*generalized_cols))
        chunk_histogram = df['__eq_class__'].value_counts().to_dict()
        histogram.update(chunk_histogram)

        n -= 1

# Step 3: Calculate DM*
print("\n🔍 Sorted Equivalence Classes (Top 20 shown):")
i = 2
for eq_class, count in sorted(histogram.items(), key=lambda item: item[1], reverse=True)[:20]:
    if count >= k:
        print(i, f"{eq_class} -> {count}")
        i += 1

low_count_sum = sum(count for count in histogram.values() if count < k)
dm_star = sum(count * count for count in histogram.values() if count >= k)
dm_star += low_count_sum * low_count_sum

print("\n⚠️ Low count total:", low_count_sum)
print(f"\n⭐ Global DM* value: {dm_star}")