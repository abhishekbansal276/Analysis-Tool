import sys
import os
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd
import pymongo
from io import BytesIO
import folium
from folium.plugins import MarkerCluster
import plotly.graph_objs as go
import plotly.io as pio
from plotly.subplots import make_subplots
import plotly.express as px
import json

MONGODB_URI = "mongodb://localhost:27017/"
DB_NAME = "analytics"


def get_state_count(data_list, db, db_name_user):
    user_images_collection = db[db_name_user]
    user_images_collection.delete_many({})

    states = [person['state'] for person in data_list]

    state_counts = {}
    for state in states:
        state_counts[state] = state_counts.get(state, 0) + 1

    return state_counts


def plot_gender_vs_category_stacked(data, user_id, db, db_name_user):
    gender_category_counts = data.groupby(['gender', 'category']).size().unstack()
    colors = ['rgb(255, 127, 14)', 'rgb(44, 160, 44)', 'rgb(214, 39, 40)', 'rgb(148, 103, 189)', 'rgb(140, 86, 75)']
    fig = go.Figure()
    for i, col in enumerate(gender_category_counts.columns):
        fig.add_trace(go.Bar(x=gender_category_counts.index,
                             y=gender_category_counts[col],
                             name=col,
                             marker_color=colors[i % len(colors)]))
    fig.update_layout(
        title='Gender vs Category',
        xaxis=dict(title='Gender', tickfont=dict(size=14, color='black'), titlefont=dict(size=16, color='black')),
        yaxis=dict(title='Count', tickfont=dict(size=14, color='black'), titlefont=dict(size=16, color='black')),
        barmode='stack',
        font=dict(size=15, color='black'),
        plot_bgcolor='white',
        paper_bgcolor='white',
    )
    html_content = pio.to_html(fig, full_html=False)
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one({
        "user_id": user_id,
        "plot_type": "gender_vs_category_stacked",
        "image_data": image_data,
        "html_content": html_content
    })


def plot_occupation_count(data, user_id, db, db_name_user):
    occupation_counts = data['occupation'].value_counts()
    colors = ['rgb(31, 119, 180)', 'rgb(255, 127, 14)', 'rgb(44, 160, 44)', 'rgb(214, 39, 40)', 'rgb(148, 103, 189)',
              'rgb(140, 86, 75)', 'rgb(227, 119, 194)', 'rgb(127, 127, 127)', 'rgb(188, 189, 34)',
              'rgb(23, 190, 207)']
    colors = colors * (len(occupation_counts) // len(colors) + 1)
    fig = go.Figure(go.Bar(
        x=occupation_counts.index,
        y=occupation_counts.values,
        marker_color=colors[:len(occupation_counts)]
    ))
    fig.update_layout(
        title='Occupation Count',
        xaxis=dict(
            title='Occupation',
            tickvals=list(range(len(occupation_counts.index))),
            ticktext=occupation_counts.index,
            tickfont=dict(size=14, color='black')
        ),
        yaxis=dict(title='Count', tickfont=dict(size=14, color='black')),  # Setting y-axis tick font properties
        font=dict(size=14, color='black'),
        plot_bgcolor='rgba(0,0,0,0)',
        paper_bgcolor='rgba(0,0,0,0)'
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = pio.to_html(fig, full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "occupation_count", "image_data": image_data, "html_content": html_content})


def plot_age_distribution_histogram(data, user_id, db, db_name_user):
    fig = go.Figure()
    fig.add_trace(go.Histogram(x=data['age'], marker_color='#008fd5', nbinsx=20))
    fig.update_layout(
        title='Age Distribution',
        xaxis=dict(title='Age'),
        yaxis=dict(title='Frequency'),
        font=dict(size=14, color='black'),
        plot_bgcolor='rgba(0,0,0,0)',
        paper_bgcolor='rgba(0,0,0,0)',
        bargap=0.1,
        margin=dict(l=100)
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = pio.to_html(fig, full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "age_distribution_histogram", "image_data": image_data,
         "html_content": html_content})


def plot_age_distribution_bar(data, user_id, db, db_name_user):
    age_bins = pd.cut(data['age'], bins=[0, 20, 40, 60, 80, 100])
    age_counts = age_bins.value_counts().sort_index()
    fig = go.Figure()
    fig.add_trace(go.Bar(x=age_counts.index.astype(str), y=age_counts.values, marker_color='rgb(31, 119, 180)'))
    fig.update_layout(
        title='Age Distribution',
        xaxis=dict(title='Age Range'),
        yaxis=dict(title='Count'),
        font=dict(size=12, color='black'),
        plot_bgcolor='rgba(0,0,0,0)',
        showlegend=False
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = pio.to_html(fig, full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one({
        "user_id": user_id,
        "plot_type": "age_distribution_bar",
        "image_data": image_data,
        "html_content": html_content
    })


def plot_monthly_income_distribution(data, user_id, db, db_name_user):
    kde_plot = go.Figure()
    kde_plot.add_trace(
        go.Histogram(x=data['monthly_income'], histnorm='probability density', marker=dict(color='#fc4f30'))
    )
    kde_plot.update_layout(
        title='Monthly Income Distribution',
        xaxis_title='Monthly Income',
        yaxis_title='Probability Density',
        font=dict(size=14, color='black'),
        showlegend=False,
        margin=dict(l=40, r=40, t=80, b=40),
        plot_bgcolor='white',
        paper_bgcolor='white',
        bargap=0.2
    )
    image_buffer = BytesIO()
    pio.write_image(kde_plot, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = pio.to_html(kde_plot, full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "monthly_income_distribution_probability", "image_data": image_data,
         "html_content": html_content}
    )


def plot_category_distribution_pie(data, user_id, db, db_name_user):
    category_counts = data['category'].value_counts()
    colors = ['#FF5733', '#FFC300', '#33FF57', '#33FFEC', '#334BFF', '#FF33E9', '#8433FF', '#8A33FF']
    fig = go.Figure(data=[go.Pie(
        labels=category_counts.index,
        values=category_counts,
        textinfo='label+percent',
        textfont=dict(size=16),
        marker=dict(colors=colors),
    )])
    fig.update_layout(
        title='Category Distribution',
        title_font_size=20,
        title_font_color='black'
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "category_distribution_pie", "image_data": image_data,
         "html_content": html_content}
    )


def plot_age_distribution_by_gender_stacked(data, user_id, db, db_name_user):
    fig = go.Figure()
    custom_palette = ["#FF6633", "#FFB399", "#FF33FF", "#FFFF99", "#00B3E6",
                      "#E6B333", "#3366E6", "#999966", "#99FF99", "#B34D4D"]
    for gender, color in zip(data['gender'].unique(), custom_palette):
        age_data = data[data['gender'] == gender]['age']
        fig.add_trace(go.Histogram(x=age_data, name=gender, marker_color=color, opacity=0.7, nbinsx=20))
    fig.update_layout(
        title='Age Distribution by Gender',
        xaxis=dict(title='Age'),
        yaxis=dict(title='Count'),
        font=dict(size=12, color='black'),
        plot_bgcolor='rgba(0,0,0,0)',
        barmode='stack',
        bargap=0.2
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = pio.to_html(fig, full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one({
        "user_id": user_id,
        "plot_type": "age_distribution_by_gender_stacked",
        "image_data": image_data,
        "html_content": html_content
    })


def plot_gender_by_occupation_stacked(data, user_id, db, db_name_user):
    occupation_gender_counts = data.groupby(['occupation', 'gender']).size().unstack(fill_value=0)
    colors = ['#FFC300', '#FF5733', '#33FFEC']
    fig = go.Figure()
    for gender, color in zip(occupation_gender_counts.columns, colors):
        fig.add_trace(go.Bar(x=occupation_gender_counts.index, y=occupation_gender_counts[gender], name=gender, marker_color=color))
    fig.update_layout(
        barmode='stack',
        title='Gender by Occupation',
        title_font_size=20,
        title_font_color='black',
        xaxis_title='Occupation',
        yaxis_title='Count',
        xaxis=dict(tickfont=dict(size=14, color='black')),
        yaxis=dict(tickfont=dict(size=14, color='black')),
        legend=dict(font=dict(size=14)),
        plot_bgcolor='white',
        paper_bgcolor='white',
        template='plotly_white'
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "gender_by_occupation_stacked", "image_data": image_data,
         "html_content": html_content}
    )


def plot_age_distribution_vs_occupation_boxplot(data, user_id, db, db_name_user):
    custom_colors = ['#FF5733', '#FFC300', '#33FFEC', '#33FF57', '#334BFF', '#FF33E9', '#8433FF', '#8A33FF']
    fig = go.Figure()
    unique_occupations = data['occupation'].unique()
    num_occupations = len(unique_occupations)
    for i, occupation in enumerate(unique_occupations):
        occupation_data = data[data['occupation'] == occupation]
        color_index = i % len(custom_colors)
        fig.add_trace(go.Box(y=occupation_data['age'], name=occupation, marker_color=custom_colors[color_index]))
    fig.update_layout(
        title='Age Distribution vs Occupation',
        xaxis_title='Occupation',
        yaxis_title='Age',
        title_font_size=24,
        title_font_color='black',
        xaxis=dict(tickfont=dict(size=14, color='black')),
        yaxis=dict(tickfont=dict(size=14, color='black')),
        plot_bgcolor='white',
        paper_bgcolor='white',
        template='plotly_white'
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "age_distribution_vs_occupation_boxplot", "image_data": image_data,
         "html_content": html_content}
    )


def plot_age_by_category_bar(data, user_id, db, db_name_user):
    fig = go.Figure()
    fig.add_trace(go.Bar(x=data['category'], y=data['age'], marker_color='rgba(31, 119, 180, 0.7)'))
    fig.update_layout(
        title='Age by Category',
        xaxis=dict(title='Category'),
        yaxis=dict(title='Age'),
        font=dict(size=12, color='black'),
        plot_bgcolor='rgba(0,0,0,0)'
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = pio.to_html(fig, full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one({
        "user_id": user_id,
        "plot_type": "age_by_category_bar",
        "image_data": image_data,
        "html_content": html_content
    })


def plot_monthly_income_vs_gender_violin(data, user_id, db, db_name_user):
    fig = go.Figure()
    for gender in data['gender'].unique():
        gender_data = data[data['gender'] == gender]
        fig.add_trace(go.Violin(y=gender_data['monthly_income'], name=gender, box_visible=True, meanline_visible=True))
    fig.update_layout(
        title='Monthly Income vs Gender',
        xaxis_title='Gender',
        yaxis_title='Monthly Income',
        title_font_size=24,
        title_font_color='black',
        xaxis=dict(tickfont=dict(size=14, color='black')),
        yaxis=dict(tickfont=dict(size=14, color='black')),
        plot_bgcolor='white',
        paper_bgcolor='white',
        template='plotly_white'
    )
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "monthly_income_vs_gender_violin", "image_data": image_data,
         "html_content": html_content}
    )


def plot_category_within_occupation(data, user_id, db, db_name_user):
    occupations = data['occupation'].unique()
    category_counts_dict = {occupation: data[data['occupation'] == occupation]['category'].value_counts()
                            for occupation in occupations}
    custom_colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd',
                     '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf']
    fig = go.Figure()
    num_occupations = len(occupations)
    for i, (occupation, category_counts) in enumerate(category_counts_dict.items()):
        color_index = i % len(custom_colors)
        fig.add_trace(go.Bar(x=category_counts.index, y=category_counts, name=occupation,
                             marker_color=custom_colors[color_index]))
    fig.update_layout(title='Category within Each Occupation', title_font_size=20, title_font_color='black',
                      xaxis_title='Category', yaxis_title='Count',
                      font=dict(size=14, color='black'),
                      plot_bgcolor='rgba(0,0,0,0)',
                      paper_bgcolor='rgba(0,0,0,0)')
    image_buffer = BytesIO()
    pio.write_image(fig, image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "category_within_occupation", "image_data": image_data,
         "html_content": html_content}
    )


def plot_occupation_within_age_group(data, user_id, db, db_name_user):
    age_bins = pd.cut(data['age'], bins=[0, 20, 40, 60, 80, 100])
    data['age_group'] = age_bins
    fig = make_subplots(rows=1, cols=5, subplot_titles=['0-20', '21-40', '41-60', '61-80', '81-100'])
    colors = ['#ff5733', '#ffc300', '#3cff33', '#33ffec', '#3367ff']
    for i, age_group in enumerate(data['age_group'].unique(), start=1):
        age_group_data = data[data['age_group'] == age_group]
        occupation_counts = age_group_data['occupation'].value_counts()
        age_group_str = str(age_group)
        fig.add_trace(go.Bar(x=occupation_counts.index, y=occupation_counts, name=age_group_str, marker_color=colors[i-1]), row=1, col=i)
        fig.update_xaxes(title_text="Occupation", row=1, col=i)
        fig.update_yaxes(title_text="Count", row=1, col=i)
    fig.update_layout(title='Occupation within Each Age Group', title_font_size=20, title_font_color='black',
                      font=dict(size=12, color='black'),
                      plot_bgcolor='rgba(0,0,0,0)',
                      paper_bgcolor='rgba(0,0,0,0)')
    image_buffer = BytesIO()
    fig.write_image(image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "occupation_within_age_group", "image_data": image_data,
         "html_content": html_content}
    )


def plot_age_vs_monthly_income_by_gender_scatter(data, user_id, db, db_name_user):
    fig = go.Figure()
    for gender in data['gender'].unique():
        gender_data = data[data['gender'] == gender]
        fig.add_trace(go.Scatter(x=gender_data['age'], y=gender_data['monthly_income'], mode='markers', name=gender))
    fig.update_layout(title='Age vs Monthly Income by Gender',
                      title_font_size=20,
                      title_font_color='black',
                      xaxis=dict(title='Age', tickfont=dict(size=14, color='black')),
                      yaxis=dict(title='Monthly Income', tickfont=dict(size=14, color='black')),
                      template='ggplot2',
                      )
    image_buffer = BytesIO()
    fig.write_image(image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "age_vs_monthly_income_by_gender_scatter", "image_data": image_data,
         "html_content": html_content}
    )


def plot_gender_within_category(data, user_id, db, db_name_user):
    categories = data['category'].unique()
    colors = ['#FF5733', '#FFC300', '#36A2EB', '#33FF99', '#FF99FF']  # Alternative color palette
    fig = make_subplots(rows=1, cols=len(categories), subplot_titles=categories)
    for i, category in enumerate(categories, start=1):
        category_data = data[data['category'] == category]
        gender_counts = category_data['gender'].value_counts()
        fig.add_trace(go.Bar(x=gender_counts.index, y=gender_counts, name=category, marker_color=colors[i-1]), row=1, col=i)
    fig.update_layout(title='Gender within Each Category', title_font_size=20, title_font_color='black')
    image_buffer = BytesIO()
    fig.write_image(image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "gender_within_category", "image_data": image_data,
         "html_content": html_content}
    )


def plot_monthly_income_distribution_by_category_boxplot(data, user_id, db, db_name_user):
    colors = ['#FF5733', '#FFC300', '#36A2EB', '#33FF99', '#FF99FF']
    categories = data['category'].unique()
    fig = make_subplots(rows=1, cols=len(categories), subplot_titles=categories)
    for i, category in enumerate(categories, start=1):
        category_data = data[data['category'] == category]
        fig.add_trace(go.Box(y=category_data['monthly_income'], name=category, marker_color=colors[i-1]), row=1, col=i)
    fig.update_layout(title='Monthly Income Distribution by Category (Boxplot)', title_font_size=20,
                      title_font_color='black')
    image_buffer = BytesIO()
    fig.write_image(image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "monthly_income_distribution_by_category_boxplot", "image_data": image_data,
         "html_content": html_content}
    )


def plot_occupation_within_category(data, user_id, db, db_name_user):
    colors = ['#FF5733', '#FFC300', '#36A2EB', '#33FF99', '#FF99FF']
    categories = data['category'].unique()
    fig = make_subplots(rows=1, cols=len(categories), subplot_titles=categories)
    for i, category in enumerate(categories, start=1):
        category_data = data[data['category'] == category]
        occupation_counts = category_data['occupation'].value_counts()
        fig.add_trace(go.Bar(x=occupation_counts.index, y=occupation_counts, name=category, marker_color=colors[i-1]), row=1, col=i)
    fig.update_layout(title='Occupation within Each Category', title_font_size=20, title_font_color='black')
    image_buffer = BytesIO()
    fig.write_image(image_buffer, format='png')
    image_data = image_buffer.getvalue()
    html_content = fig.to_html(full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "occupation_within_category", "image_data": image_data,
         "html_content": html_content}
    )


def plot_occupation_within_gender_category(data, user_id, db, db_name_user):
    custom_palette = ["#FF5733", "#FFC300", "#36A2EB"]
    plt.figure(figsize=(12, 8))
    categories = data['category'].unique()
    num_categories = len(categories)
    for i, category in enumerate(categories, start=1):
        plt.subplot(1, num_categories, i)
        category_data = data[data['category'] == category]
        sns.countplot(data=category_data, x='occupation', hue='gender', palette=custom_palette)
        plt.title(f'Category {category}', fontsize=16)
        plt.xlabel('Occupation', fontsize=14)
        plt.ylabel('Count', fontsize=14)
        plt.xticks(rotation='vertical')
        plt.tick_params(axis='x', labelsize=8)
        if i == num_categories:
            plt.legend(title='Gender', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()
    image_buffer = BytesIO()
    plt.savefig(image_buffer, format='png')
    image_data = image_buffer.getvalue()
    fig = go.Figure()
    for category in categories:
        category_data = data[data['category'] == category]
        counts = category_data.groupby(['occupation', 'gender']).size().unstack(fill_value=0)
        for gender in counts.columns:
            fig.add_trace(go.Bar(x=counts.index, y=counts[gender], name=gender))
    fig.update_layout(
        title='Occupation within Each Gender and Category',
        xaxis_title='Occupation',
        yaxis_title='Count',
        barmode='group'
    )
    html_content = pio.to_html(fig, full_html=False)
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one({
        "user_id": user_id,
        "plot_type": "occupation_within_gender_category",
        "image_data": image_data,
        "html_content": html_content
    })


def plot_state_counts_on_map(state_counts, user_id, db, db_name_user):
    m = folium.Map(location=[20.5937, 78.9629], zoom_start=5)  # India's coordinates
    marker_cluster = folium.plugins.MarkerCluster().add_to(m)
    for state, count in state_counts.items():
        folium.Marker(location=get_coordinates(state), popup=f"{state}: {count} persons").add_to(marker_cluster)
    html_content = m.get_root().render()
    user_images_collection = db[db_name_user]
    user_images_collection.insert_one(
        {"user_id": user_id, "plot_type": "state_counts_map", "html_content": html_content})


def get_coordinates(state):
    coordinates = {
        "Andhra Pradesh": [15.9129, 79.7400],
        "Arunachal Pradesh": [27.1004, 93.6167],
        "Assam": [26.2006, 92.9376],
        "Bihar": [25.0961, 85.3131],
        "Chhattisgarh": [21.2787, 81.8661],
        "Goa": [15.2993, 74.1240],
        "Gujarat": [22.2587, 71.1924],
        "Haryana": [29.0588, 76.0856],
        "Himachal Pradesh": [31.1048, 77.1734],
        "Jharkhand": [23.6102, 85.2799],
        "Karnataka": [15.3173, 75.7139],
        "Kerala": [10.8505, 76.2711],
        "Madhya Pradesh": [22.9734, 78.6569],
        "Maharashtra": [19.7515, 75.7139],
        "Manipur": [24.6637, 93.9063],
        "Meghalaya": [25.4670, 91.3662],
        "Mizoram": [23.1645, 92.9376],
        "Nagaland": [26.1584, 94.5624],
        "Odisha": [20.9517, 85.0985],
        "Punjab": [31.1471, 75.3412],
        "Rajasthan": [27.0238, 74.2179],
        "Sikkim": [27.5330, 88.5122],
        "Tamil Nadu": [11.1271, 78.6569],
        "Telangana": [18.1124, 79.0193],
        "Tripura": [23.9408, 91.9882],
        "Uttar Pradesh": [26.8467, 80.9462],
        "Uttarakhand": [30.0668, 79.0193],
        "West Bengal": [22.9868, 87.8550],
    }
    return coordinates.get(state, [0, 0])


def main(user_id, folderName):
    client = pymongo.MongoClient(MONGODB_URI)
    db = client[DB_NAME]
    db_name_user = user_id + "images" + folderName

    user_images_collection = db[db_name_user]
    user_images_collection.delete_many({})

    data_str = sys.stdin.read()
    data_list = json.loads(data_str)

    data = pd.DataFrame(data_list)
    state_counts = get_state_count(data_list, db, db_name_user)

    plot_gender_vs_category_stacked(data, user_id, db, db_name_user)
    plot_occupation_count(data, user_id, db, db_name_user)
    plot_age_distribution_histogram(data, user_id, db, db_name_user)
    plot_age_distribution_bar(data, user_id, db, db_name_user)
    plot_monthly_income_distribution(data, user_id, db, db_name_user)
    plot_category_distribution_pie(data, user_id, db, db_name_user)
    plot_age_distribution_by_gender_stacked(data, user_id, db, db_name_user)
    plot_gender_by_occupation_stacked(data, user_id, db, db_name_user)
    plot_age_distribution_vs_occupation_boxplot(data, user_id, db, db_name_user)
    plot_age_by_category_bar(data, user_id, db, db_name_user)
    plot_monthly_income_vs_gender_violin(data, user_id, db, db_name_user)
    plot_category_within_occupation(data, user_id, db, db_name_user)
    plot_occupation_within_age_group(data, user_id, db, db_name_user)
    plot_age_vs_monthly_income_by_gender_scatter(data, user_id, db, db_name_user)
    plot_gender_within_category(data, user_id, db, db_name_user)
    plot_monthly_income_distribution_by_category_boxplot(data, user_id, db, db_name_user)
    plot_occupation_within_category(data, user_id, db, db_name_user)
    plot_occupation_within_gender_category(data, user_id, db, db_name_user)
    plot_state_counts_on_map(state_counts, user_id, db, db_name_user)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python script.py user_id", file=sys.stderr)
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])