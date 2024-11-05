import pandas as pd
from faker import Faker
import random

# Initialize Faker with 'en_IN' for Indian names
fake = Faker('en_IN')
Faker.seed(0)

# Define the number of records to generate
n = 100000

# Define a list of transaction categories
transaction_categories = ['Dining', 'Groceries', 'Electronics', 'Apparel', 'Travel', 'Health', 'Entertainment']

# Generate synthetic data
data = {
    "CustomerName": [fake.name() for _ in range(n)],
    "Address": [fake.address() for _ in range(n)],
    "Age": [random.randint(18, 70) for _ in range(n)],  # Random age between 18 and 70
    "Gender": [random.choice(["Male", "Female"]) for _ in range(n)],
    "TransactionCategory": [random.choice(transaction_categories) for _ in range(n)],
    "TransactionAmount": [round(random.uniform(100, 50000), 2) for _ in range(n)]  # Transaction amount between ₹100 and ₹50000
}

# Convert to DataFrame
df = pd.DataFrame(data)

# Display the first few rows
print(df.head())

# Save the synthetic dataset to CSV
df.to_csv("synthetic_financial_transactions_raw.csv", index=False)
