/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

#import "WWWMapViewBridge.h"
#import <MapLibre/MapLibre.h>

@implementation WWWMapViewBridge

+ (UIViewController *)createMapViewControllerWithStyleURL:(NSString *)styleURL
                                                 latitude:(double)latitude
                                                longitude:(double)longitude
                                                     zoom:(double)zoom {
    // Create a simple UIViewController
    UIViewController *viewController = [[UIViewController alloc] init];

    // Create MapLibre map view
    MLNMapView *mapView = [[MLNMapView alloc] initWithFrame:CGRectZero];
    mapView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

    // Set style URL
    if (styleURL && styleURL.length > 0) {
        NSURL *url = [NSURL URLWithString:styleURL];
        if (url) {
            mapView.styleURL = url;
        }
    }

    // Set initial camera position
    CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);
    [mapView setCenterCoordinate:center zoomLevel:zoom animated:NO];

    // Add map view to view controller
    viewController.view = mapView;

    return viewController;
}

@end
