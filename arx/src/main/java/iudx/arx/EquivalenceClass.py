import pandas as pd
import json
import matplotlib.pyplot as plt
from collections import Counter

class EquivalenceClasses:
    def __init__(self, csv_file):
        self.data = pd.read_csv(csv_file)
        self.equivalence_classes = None
        self.class_sizes = None

    def compute_equivalence_classes(self, columns):
        # Group data based on specified columns to form equivalence classes
        self.equivalence_classes = self.data.groupby(columns)
        
        # Calculate size of each equivalence class
        self.class_sizes = self.equivalence_classes.size().reset_index(name='Size')
        return self.class_sizes

    def remove_outliers(self):
        # Remove outliers using the IQR method
        if self.class_sizes is None:
            raise ValueError("Equivalence classes not computed. Call compute_equivalence_classes() first.")
        
        Q1 = self.class_sizes['Size'].quantile(0.25)
        Q3 = self.class_sizes['Size'].quantile(0.75)
        IQR = Q3 - Q1

        # Define the bounds for non-outliers
        lower_bound = Q1 - 1.5 * IQR
        upper_bound = Q3 + 1.5 * IQR

        # Filter out the outliers
        filtered_class_sizes = self.class_sizes[(self.class_sizes['Size'] >= lower_bound) & (self.class_sizes['Size'] <= upper_bound)]
        self.class_sizes = filtered_class_sizes.reset_index(drop=True)

    def generate_stats(self):
        if self.class_sizes is None:
            raise ValueError("Equivalence classes not computed. Call compute_equivalence_classes() first.")
        
        # Count how many equivalence classes there are of each size
        size_counts = Counter(self.class_sizes['Size'])
        
        # Prepare stats
        stats = {
            "total_equivalence_classes": len(self.class_sizes),
            "size_distribution": dict(size_counts)
        }
        return stats

    def save_stats_to_json(self, output_json_file):
        stats = self.generate_stats()
        
        # Save the stats as a JSON file
        with open(output_json_file, 'w') as json_file:
            json.dump(stats, json_file, indent=4)

    def plot_equivalence_classes(self, output_image_file='equivalence_classes.png', k=500):
        if self.class_sizes is None:
            raise ValueError("Equivalence classes not computed. Call compute_equivalence_classes() first.")

        # Plot a bar graph for equivalence class sizes
        size_counts = self.class_sizes['Size'].value_counts().sort_index()

        plt.figure(figsize=(12, 8))  # Increase the figure size
        plt.axvline(x=k, color='red', linestyle='--', linewidth=1.1, label=f'k = {k}')
        plt.bar(size_counts.index, size_counts.values, color='skyblue', width=0.8)  # Adjust the bar width
        
        plt.xlabel('Size of Equivalence Class')
        plt.ylabel('Number of Equivalence Classes')
        plt.title('Equivalence Class Size Distribution')
        
        # Set the x-axis ticks and format them
        max_x = max(size_counts.index)
        tick_values = list(range(0, int(max_x),k))  # Set x-ticks at intervals of 1000
        
        # Set xticks and format them to show '0, 1k, 2k, etc.'
        plt.xticks(tick_values)
        
        plt.grid(axis='y')
        plt.xlim(left=0, right=max_x)  # Auto-adjust x-axis range to start from 0
        plt.legend()

        # Save the plot to a file
        plt.savefig(output_image_file)
        plt.show()

# Example usage:
if __name__ == "__main__":
    eq_classes = EquivalenceClasses('/home/kailash/Desktop/arx_anonymization/arx/anonymized_output.csv')
    eq_classes.compute_equivalence_classes(columns=["Age","PIN Code","Height","Weight"])  # Specify the columns for equivalence classes
    
    # Remove outliers
    eq_classes.remove_outliers()
    
    # Save stats to JSON
    eq_classes.save_stats_to_json('equivalence_stats.json')
    
    # Plot the distribution of equivalence class sizes
    eq_classes.plot_equivalence_classes('equivalence_classes_distribution.png', k=10)
