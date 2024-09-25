import matplotlib.pyplot as plt

def plot_polygon(test_case_number, polygon_number, polygon, left_expected, right_expected, cut_line, cut_points, composed=False):
    plt.figure(figsize=(8, 8))

    # Plot the original polygon
    latitudes = [pos[0] for pos in polygon]
    longitudes = [pos[1] for pos in polygon]
    plt.plot(longitudes, latitudes, marker='o', label=f'Polygon {polygon_number}')
    plt.fill(longitudes, latitudes, 'lightgray', alpha=0.4)

    # Define more distinct color maps for left and right polygons
    left_colors = ['#FF4500', '#FFA500', '#FFD700']  # Bright red, orange, and gold for left polygons
    right_colors = ['#0000FF', '#00008B', '#00FFFF']  # Strong blue, dark blue, and cyan for right polygons

    # Plot the left expected polygons with distinct warm colors
    for i, left_polygon in enumerate(left_expected):
        latitudes_left = [pos[0] for pos in left_polygon]
        longitudes_left = [pos[1] for pos in left_polygon]
        color = left_colors[i % len(left_colors)]  # Cycle through warm shades
        plt.plot(longitudes_left, latitudes_left, marker='o', label=f'Left Expected {polygon_number} - {i+1}', color=color)
        plt.fill(longitudes_left, latitudes_left, color, alpha=0.3)

    # Plot the right expected polygons with distinct cool colors
    for i, right_polygon in enumerate(right_expected):
        latitudes_right = [pos[0] for pos in right_polygon]
        longitudes_right = [pos[1] for pos in right_polygon]
        color = right_colors[i % len(right_colors)]  # Cycle through cool shades
        plt.plot(longitudes_right, latitudes_right, marker='o', label=f'Right Expected {polygon_number} - {i+1}', color=color)
        plt.fill(longitudes_right, latitudes_right, color, alpha=0.3)

    # Plot the cut line
    if composed:
        plt.plot([pos[1] for pos in cut_line], [pos[0] for pos in cut_line], linestyle='--', color='blue', label='Composed Cut Line', linewidth=2)
    else:
        plt.axvline(x=cut_line, color='red', linestyle='--', label='Cut Line', linewidth=2)

    # Plot the cut points as empty square markers
    cut_latitudes = [pos[0] for pos in cut_points]
    cut_longitudes = [pos[1] for pos in cut_points]
    plt.scatter(cut_longitudes, cut_latitudes, facecolors='none', edgecolors='black', s=100, label='Cut Points', marker='s')

    # Add titles and labels
    plt.title(f'Test Case {test_case_number} - Polygon {polygon_number}')
    plt.xlabel('Longitude')
    plt.ylabel('Latitude')
    plt.xticks(range(-10, 11))
    plt.yticks(range(-10, 11))

    # Highlight the 0 axis in bold
    plt.axhline(y=0, color='black', linewidth=2)  # Horizontal axis
    plt.axvline(x=0, color='black', linewidth=2)  # Vertical axis

    # Display legend
    plt.legend()
    plt.grid(True)

    # Show the plot
    plt.show()

