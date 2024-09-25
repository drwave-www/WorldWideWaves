import matplotlib.pyplot as plt

# Function to plot a polygon test case with all required elements
def plot_polygon(test_case_number, polygon_number, polygon, left_expected, right_expected, cut_line, cut_points, composed=False):
    plt.figure(figsize=(8, 8))

    # Plot the original polygon
    latitudes = [pos[0] for pos in polygon]
    longitudes = [pos[1] for pos in polygon]
    plt.plot(longitudes, latitudes, marker='o', label=f'Polygon {polygon_number}')
    plt.fill(longitudes, latitudes, 'lightblue', alpha=0.4)

    # Plot the left expected polygon
    for left_polygon in left_expected:
        latitudes_left = [pos[0] for pos in left_polygon]
        longitudes_left = [pos[1] for pos in left_polygon]
        plt.plot(longitudes_left, latitudes_left, marker='o', label=f'Left Expected {polygon_number}', color='green')
        plt.fill(longitudes_left, latitudes_left, 'green', alpha=0.3)

    # Plot the right expected polygon
    for right_polygon in right_expected:
        latitudes_right = [pos[0] for pos in right_polygon]
        longitudes_right = [pos[1] for pos in right_polygon]
        plt.plot(longitudes_right, latitudes_right, marker='o', label=f'Right Expected {polygon_number}', color='orange')
        plt.fill(longitudes_right, latitudes_right, 'orange', alpha=0.3)

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


# Test case 1 for polygon1
polygon1 = [(-12.0, -6.0), (-13.0, -3.0), (-11.0, -3.0), (-9.0, -3.0), (-8.0, -6.0), (-7.0, -3.0), (-6.0, -6.0),
            (-5.0, -3.0), (-3.0, 0.0), (-2.0, -3.0), (-1.0, 2.0), (1.0, 2.0), (3.0, -8.0), (5.0, -8.0), (7.0, -7.0),
            (8.0, -5.0), (9.0, -1.0), (9.0, 2.0), (14.0, 2.0), (14.0, -5.0), (12.0, -5.0), (12.0, -1.0), (10.0, 1.0),
            (10.0, -7.0), (10.0, -9.0), (-11.0, -9.0)]

left_expected1 = [[(-12.0, -6.0), (-13.0, -3.0), (-9.0, -3.0), (-8.0, -6.0), (-7.0, -3.0), (-6.0, -6.0),
                   (-5.0, -3.0), (2.0, -3.0), (3.0, -8.0), (5.0, -8.0), (7.0, -7.0), (8.0, -5.0), (8.5, -3.0),
                   (10.0, -3.0), (10.0, -7.0), (10.0, -9.0), (-11.0, -9.0), (-12.0, -6.0)]]

right_expected1 = [[(-5.0, -3.0), (-3.0, 0.0), (-2.0, -3.0), (-1.0, 2.0), (1.0, 2.0), (2.0, -3.0), (-5.0, -3.0)],
                   [(8.5, -3.0), (9.0, -1.0), (9.0, 2.0), (14.0, 2.0), (14.0, -3.0), (12.0, -3.0), (12.0, -1.0),
                    (10.0, 1.0), (10.0, -3.0), (8.5, -3.0)]]

cut_line1 = -3.0
cut_points1 = [(-13.0, -3.0), (-9.0, -3.0), (-7.0, -3.0), (-5.0, -3.0), (2.0, -3.0), (8.5, -3.0), (10.0, -3.0)]

# Plot test case 1
plot_polygon(1, 1, polygon1, left_expected1, right_expected1, cut_line1, cut_points1, composed=False)

