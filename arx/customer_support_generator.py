import pandas as pd
from faker import Faker
from faker.providers.address import Provider
import random
# Initialize Faker with Indian locale
fake = Faker("en_IN")
fake.add_provider(Provider)
Faker.seed(0)

# Predefined lists for controlled columns
support_agents = [fake.name() for _ in range(20)]  # Generate 20 random names for support agents
support_channels = ["Email", "Phone", "Chat"]
issue_types = ["Billing", "Refund", "Offer", "Coupon Code", "Technical"]

# Generate synthetic dataset
n = 1000  # Number of records
data = {
    "TicketID": [f"T{str(i).zfill(5)}" for i in range(1, n + 1)],
    "CustomerName": [fake.name() for _ in range(n)],
    "CustomerSupportName": [random.choice(support_agents) for _ in range(n)],
    "ResponseTime (Minutes)": [random.randint(1, 120) for _ in range(n)],
    "PIN Code": [fake.zipcode_in_state() for _ in range(n)],  # PIN codes in major states
    "Refund Amount": [random.choice([0, random.uniform(100, 1000)]) for _ in range(n)],
    "Support Channel": [random.choice(support_channels) for _ in range(n)],
    "Issue Type": [random.choice(issue_types) for _ in range(n)],
    "Phone Number": [fake.phone_number() for _ in range(n)],  # Sensitive column
}

# Convert to DataFrame
df = pd.DataFrame(data)

# Round Refund Amount to 2 decimal places
df["Refund Amount"] = df["Refund Amount"].apply(lambda x: round(x, 2))

# Save to CSV
df.to_csv("synthetic_customer_support_data.csv", index=False)

# Display the first few rows
print(df.head())
