from polygon_plotter import plot_polygon

# Test case 3 for polygon2 with longitudeToCut = 2.0
polygon2 = [(-1.0, 1.0), (0.0, 1.0), (0.0, -1.0), (1.0, -1.0), (1.0, 2.0), (-2.0, 2.0), (-2.0, -3.0), (3.0, -3.0),
            (3.0, 4.0), (-2.0, 4.0), (-2.0, 3.0), (2.0, 3.0), (2.0, -2.0), (-1.0, -2.0)]

left_expected3 = [[(3.0, 2.0), (3.0, -3.0), (-2.0, -3.0), (-2.0, 2.0), (1.0, 2.0), (1.0, -1.0), (0.0, -1.0), 
                   (0.0, 1.0), (-1.0, 1.0), (-1.0, -2.0), (2.0, -2.0), (2.0, 2.0), (3.0, 2.0)]]

right_expected3 = [[(2.0, 2.0), (2.0, 3.0), (-2.0, 3.0), (-2.0, 4.0), (3.0, 4.0), (3.0, 2.0), (2.0, 2.0)]]

cut_line3 = 2.0
cut_points3 = [(3.0, 2.0), (-2.0, 2.0), (1.0, 2.0), (2.0, 2.0)]

# Plot test case 3
plot_polygon(3, 2, polygon2, left_expected3, right_expected3, cut_line3, cut_points3, composed=False)

