import sys
import json
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import traceback
import os


def generate_graphs(data1, data2):
    try:
        df1 = pd.DataFrame(data1)
        df2 = pd.DataFrame(data2)

        # 1) Correlation heatmap
        plt.figure(figsize=(15, 6))
        plt.subplot(1, 2, 1)
        sns.heatmap(df1.corr(), annot=True, cmap='coolwarm')
        plt.title('Correlation Heatmap (df1)')

        plt.savefig(os.path.join('D:\\scala_play_projects\\analytics\\public\\images', 'correlation_heatmap_df1.png'))

        plt.subplot(1, 2, 2)
        sns.heatmap(df2.corr(), annot=True, cmap='coolwarm')
        plt.title('Correlation Heatmap (df2)')

        plt.tight_layout()
        plt.savefig(os.path.join('D:\\scala_play_projects\\analytics\\public\\images', 'correlation_heatmap_df2.png'))

        # 2) Pair plot
        plt.figure(figsize=(15, 6))
        plt.subplot(1, 2, 1)
        sns.pairplot(df1)
        plt.title('Pairplot (df1)')

        plt.subplot(1, 2, 2)
        sns.pairplot(df2)
        plt.title('Pairplot (df2)')

        plt.tight_layout()
        plt.show()

        # 3) Count-plot for gender distribution
        plt.figure(figsize=(15, 6))
        plt.subplot(1, 2, 1)
        sns.countplot(data=df1, x='gender')
        plt.title('Gender Distribution (df1)')

        plt.subplot(1, 2, 2)
        sns.countplot(data=df2, x='gender')
        plt.title('Gender Distribution (df2)')

        plt.tight_layout()
        plt.show()

        # 4) Scatter plot for monthly income
        plt.figure(figsize=(15, 6))
        plt.subplot(1, 2, 1)
        sns.scatterplot(data=df1, x='monthly_income', y='age', hue='gender')
        plt.title('Scatter plot for Monthly Income (df1)')

        plt.subplot(1, 2, 2)
        sns.scatterplot(data=df2, x='monthly_income', y='age', hue='gender')
        plt.title('Scatter plot for Monthly Income (df2)')

        plt.tight_layout()
        plt.show()

        # 5) Histogram for age distribution
        plt.figure(figsize=(15, 6))
        plt.subplot(1, 2, 1)
        sns.histplot(data=df1, x='age', kde=True)
        plt.title('Histogram for Age Distribution (df1)')

        plt.subplot(1, 2, 2)
        sns.histplot(data=df2, x='age', kde=True)
        plt.title('Histogram for Age Distribution (df2)')

        plt.tight_layout()
        plt.show()

        # 6) Box plot for age distribution by gender
        plt.figure(figsize=(15, 6))
        plt.subplot(1, 2, 1)
        sns.boxplot(data=df1, x='gender', y='age')
        plt.title('Box plot for Age Distribution by Gender (df1)')

        plt.subplot(1, 2, 2)
        sns.boxplot(data=df2, x='gender', y='age')
        plt.title('Box plot for Age Distribution by Gender (df2)')

        plt.tight_layout()
        plt.show()

    except Exception as ex:
        print("Error occurred:", ex)
        traceback.print_exc()


if __name__ == "__main__":
    try:
        data1_str = sys.stdin.readline().strip()
        data2_str = sys.stdin.readline().strip()

        data1 = json.loads(data1_str)
        data2 = json.loads(data2_str)

        generate_graphs(data1, data2)

    except Exception as e:
        print("Error occurred:", e)
        traceback.print_exc()
