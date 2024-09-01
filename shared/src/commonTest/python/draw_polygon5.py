from polygon_plotter import plot_polygon

# Test case 5 for polygon4 with composedLongitudeToCut
polygon4 = [(-2.0, -2.0), (-2.0, 2.0), (2.0, 2.0), (2.0, -2.0)]

left_expected5 = [[(-2.0, -2.0), (-2.0, -1.0), (1.0, -1.0), (2.0, 0.0), (2.0, -2.0), (-2.0, -2.0)]]

right_expected5 = [[(-2.0, -1.0), (-2.0, 2.0), (2.0, 2.0), (2.0, 0.0), (1.0, -1.0), (-2.0, -1.0)]]

# Composed cut line
composed_cut_line5 = [(-3.0, -1.0), (1.0, -1.0), (3.0, 1.0)]

cut_points5 = [(-2.0, -1.0), (1.0, -1.0), (2.0, 0.0)]

# Plot test case 5
plot_polygon(5, 4, polygon4, left_expected5, right_expected5, composed_cut_line5, cut_points5, composed=True)

