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
    NSLog(@"[WWWMapViewBridge] Creating map view controller with style: %@", styleURL);

    // Create MapLibre map view
    MLNMapView *mapView = [[MLNMapView alloc] initWithFrame:CGRectZero];
    mapView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

    // Set style URL
    if (styleURL && styleURL.length > 0) {
        NSURL *url = [NSURL URLWithString:styleURL];
        if (url) {
            mapView.styleURL = url;
            NSLog(@"[WWWMapViewBridge] Style URL set: %@", styleURL);
        } else {
            NSLog(@"[WWWMapViewBridge] Invalid style URL: %@", styleURL);
        }
    }

    // Set initial camera position
    CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);
    [mapView setCenterCoordinate:center zoomLevel:zoom animated:NO];
    NSLog(@"[WWWMapViewBridge] Camera set: lat=%.4f, lng=%.4f, zoom=%.1f", latitude, longitude, zoom);

    // Create view controller with map
    UIViewController *viewController = [[UIViewController alloc] init];
    viewController.view = mapView;

    NSLog(@"[WWWMapViewBridge] Map view controller created successfully");
    return viewController;
}

@end
