/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

#import "WWWMapViewBridge.h"

@implementation WWWMapViewBridge

+ (UIViewController *)createMapViewControllerWithStyleURL:(NSString *)styleURL
                                                 latitude:(double)latitude
                                                longitude:(double)longitude
                                                     zoom:(double)zoom {
    // Return a placeholder UIViewController
    // The iOS app will override this by providing MapViewBridge.swift implementation
    // which creates UIHostingController with EventMapView
    UIViewController *viewController = [[UIViewController alloc] init];

    // Set background color so we can see the placeholder
    viewController.view.backgroundColor = [UIColor colorWithRed:0.2 green:0.2 blue:0.3 alpha:1.0];

    // Add a label indicating this is a placeholder
    UILabel *label = [[UILabel alloc] init];
    label.text = @"Map Placeholder - Override in iOS App";
    label.textColor = [UIColor whiteColor];
    label.textAlignment = NSTextAlignmentCenter;
    label.translatesAutoresizingMaskIntoConstraints = NO;
    [viewController.view addSubview:label];

    [NSLayoutConstraint activateConstraints:@[
        [label.centerXAnchor constraintEqualToAnchor:viewController.view.centerXAnchor],
        [label.centerYAnchor constraintEqualToAnchor:viewController.view.centerYAnchor]
    ]];

    return viewController;
}

@end
