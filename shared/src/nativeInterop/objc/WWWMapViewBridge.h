/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

#ifndef WWWMapViewBridge_h
#define WWWMapViewBridge_h

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

/**
 * Objective-C bridge to create map view controllers.
 * This class is accessible from Kotlin via cinterop.
 *
 * The implementation in shared module provides a placeholder.
 * The iOS app should override this by implementing MapViewBridge.swift
 * which creates UIHostingController with EventMapView (SwiftUI + MapLibre).
 */
@interface WWWMapViewBridge : NSObject

/**
 * Creates a UIViewController with a MapLibre map view.
 *
 * @param styleURL The map style URL (mbtiles:// or https://)
 * @param latitude Initial camera latitude
 * @param longitude Initial camera longitude
 * @param zoom Initial zoom level
 * @return UIViewController containing the map view
 */
+ (UIViewController *)createMapViewControllerWithStyleURL:(NSString *)styleURL
                                                 latitude:(double)latitude
                                                longitude:(double)longitude
                                                     zoom:(double)zoom;

@end

#endif /* WWWMapViewBridge_h */
