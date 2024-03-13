import sys
import os
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd


def load_data(data_list):
    # Convert list of dictionaries to DataFrame
    df = pd.DataFrame(data_list)
    return df


def plot_gender_distribution(data, output_folder):
    gender_counts = data['gender'].value_counts()
    plt.figure(figsize=(8, 6))
    sns.barplot(x=gender_counts.index, y=gender_counts.values, color='#023e8a')
    plt.title('Gender Distribution', fontsize=20, color='black')
    plt.xlabel('Gender', fontsize=16, color='black')
    plt.ylabel('Count', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    sns.despine(top=True, right=True)
    plt.savefig(os.path.join(output_folder, 'gender_distribution.png'))
    plt.close()


def plot_age_distribution(data, output_folder):
    plt.figure(figsize=(8, 6))
    sns.histplot(data['age'], bins=20, kde=True, color='#fdc500')
    sns.despine(top=True, right=True)
    plt.title('Age Distribution', fontsize=20, color='black')
    plt.xlabel('Age', fontsize=16, color='black')
    plt.ylabel('Frequency', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'age_distribution.png'))
    plt.close()


def plot_occupation_distribution(data, output_folder):
    occupation_counts = data['occupation'].value_counts()
    colors = ['lightblue', 'lightgreen', 'lightcoral', 'lightskyblue', 'lightpink']
    plt.figure(figsize=(10, 8))
    plt.pie(occupation_counts, labels=occupation_counts.index, autopct='%1.1f%%', startangle=140, colors=colors, textprops={'fontsize': 18})
    plt.savefig(os.path.join(output_folder, 'occupation_distribution.png'))
    plt.close()


def plot_monthly_income_distribution(data, output_folder):
    plt.figure(figsize=(8, 6))
    sns.histplot(data['monthly_income'], bins=20, kde=True, color='#007f5f')
    sns.despine(top=True, right=True)
    plt.title('Monthly Income Distribution', fontsize=20, color='black')
    plt.xlabel('Monthly Income', fontsize=16, color='black')
    plt.ylabel('Frequency', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'monthly_income_distribution.png'))
    plt.close()


def plot_category_distribution(data, output_folder):
    category_counts = data['category'].value_counts()
    plt.figure(figsize=(10, 8))
    sns.barplot(x=category_counts.index, y=category_counts.values, color='#9d4edd')
    sns.despine(top=True, right=True)
    plt.title('Category Distribution', fontsize=20, color='black')
    plt.xlabel('Category', fontsize=16, color='black')
    plt.ylabel('Count', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'category_distribution.png'))
    plt.close()


def plot_age_vs_occupation(data, output_folder):
    colors = ['#ffcdb2', '#a2d2ff', '#b7e4c7', '#e0aaff', '#ffccd5']
    mean_age_by_occupation = data.groupby('occupation')['age'].mean()
    plt.figure(figsize=(10, 8))
    mean_age_by_occupation.plot(kind='pie', autopct='%1.1f%%', colors=colors, startangle=140, textprops={'fontsize': 18})
    plt.title('Mean Age by Occupation', fontsize=25, color='black')
    plt.ylabel('')
    plt.savefig(os.path.join(output_folder, 'age_vs_occupation.png'))
    plt.close()


def plot_monthly_income_vs_occupation(data, output_folder):
    plt.figure(figsize=(10, 8))
    sns.boxplot(x='occupation', y='monthly_income', data=data, color='#a4133c')
    sns.despine(top=True, right=True)
    plt.title('Monthly Income vs Occupation', fontsize=20, color='black')
    plt.xlabel('Occupation', fontsize=16, color='black')
    plt.ylabel('Monthly Income', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'monthly_income_vs_occupation_boxplot.png'))
    plt.close()


def plot_monthly_income_vs_occupation_violin(data, output_folder):
    plt.figure(figsize=(10, 8))
    sns.violinplot(x='occupation', y='monthly_income', data=data, color='#ff7b00')
    sns.despine(top=True, right=True)
    plt.title('Monthly Income vs Occupation', fontsize=20, color='black')
    plt.xlabel('Occupation', fontsize=16, color='black')
    plt.ylabel('Monthly Income', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'monthly_income_vs_occupation_violinplot.png'))
    plt.close()


def plot_age_vs_gender_stacked(data, output_folder):
    gender_age_counts = data.groupby(['gender', 'age']).size().unstack()
    gender_age_counts.plot(kind='bar', stacked=True, figsize=(10, 8))
    sns.despine(top=True, right=True)
    plt.title('Age vs Gender', fontsize=20, color='black')
    plt.xlabel('Age', fontsize=16, color='black')
    plt.ylabel('Count', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black', rotation=0)
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'age_vs_gender_stacked.png'))
    plt.close()


def plot_age_vs_category_stacked(data, output_folder):
    category_age_counts = data.groupby(['category', 'age']).size().unstack()
    category_age_counts.plot(kind='bar', stacked=True, figsize=(10, 8))
    sns.despine(top=True, right=True)
    plt.title('Age vs Category', fontsize=20, color='black')
    plt.xlabel('Age', fontsize=16, color='black')
    plt.ylabel('Count', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black', rotation=0)
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'age_vs_category_stacked.png'))
    plt.close()


def plot_occupation_vs_category_stacked(data, output_folder):
    category_occupation_counts = data.groupby(['occupation', 'category']).size().unstack()
    category_occupation_counts.plot(kind='bar', stacked=True, figsize=(10, 8))
    sns.despine(top=True, right=True)
    plt.title('Occupation vs Category', fontsize=20, color='black')
    plt.xlabel('Occupation', fontsize=16, color='black')
    plt.ylabel('Count', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black', rotation=0)
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'occupation_vs_category_stacked.png'))
    plt.close()


def plot_top_10_monthly_incomes(data, output_folder):
    data['aadhaar_last_4'] = data['aadhaar_id'].astype(str).str[-4:]
    top_10_incomes = data.nlargest(10, 'monthly_income')
    plt.figure(figsize=(10, 8))
    sns.barplot(x='monthly_income', y='aadhaar_last_4', data=top_10_incomes, color='#036666')
    sns.despine(top=True, right=True)
    plt.title('Top 10 Monthly Incomes', fontsize=20, color='black')
    plt.xlabel('Monthly Income', fontsize=16, color='black')
    plt.ylabel('Aadhaar ID', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'top_10_monthly_incomes.png'))
    plt.close()


def plot_total_monthly_income_by_occupation(data, output_folder):
    total_income_by_occupation = data.groupby('occupation')['monthly_income'].sum().reset_index()
    plt.figure(figsize=(10, 8))
    sns.barplot(x='occupation', y='monthly_income', data=total_income_by_occupation, color='#ef6351')
    sns.despine(top=True, right=True)
    plt.title('Total Monthly Income by Occupation', fontsize=20, color='black')
    plt.xlabel('Occupation', fontsize=16, color='black')
    plt.ylabel('Total Monthly Income', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'total_monthly_income_by_occupation.png'))
    plt.close()


def plot_entries_per_state(data, output_folder):
    state_counts = data['state'].value_counts()
    plt.figure(figsize=(12, 8))
    sns.barplot(x=state_counts.index, y=state_counts.values, color='#3a86ff')
    sns.despine(top=True, right=True)
    plt.title('Number of Entries per State', fontsize=20, color='black')
    plt.xlabel('State', fontsize=16, color='black')
    plt.ylabel('Count', fontsize=16, color='black')
    plt.xticks(fontsize=14, color='black')
    plt.yticks(fontsize=14, color='black')
    plt.savefig(os.path.join(output_folder, 'entries_per_state.png'))
    plt.close()


def main(output_folder):
    # Get data passed from Scala script
    data_str = sys.stdin.read()
    data_list = eval(data_str)

    # Load data
    data = load_data(data_list)

    # Perform analysis and generate plots
    plot_gender_distribution(data, output_folder)
    plot_age_distribution(data, output_folder)
    plot_occupation_distribution(data, output_folder)
    plot_monthly_income_distribution(data, output_folder)
    plot_category_distribution(data, output_folder)
    plot_age_vs_occupation(data, output_folder)
    plot_monthly_income_vs_occupation(data, output_folder)
    plot_monthly_income_vs_occupation_violin(data, output_folder)
    plot_age_vs_gender_stacked(data, output_folder)
    plot_age_vs_category_stacked(data, output_folder)
    plot_occupation_vs_category_stacked(data, output_folder)
    plot_top_10_monthly_incomes(data, output_folder)
    plot_total_monthly_income_by_occupation(data, output_folder)
    plot_entries_per_state(data, output_folder)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python analysis_script.py <output_folder>")
        sys.exit(1)

    output_folder = sys.argv[1]
    main(output_folder)
