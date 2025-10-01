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

// Forward declarations
@class MLNMapView;

/**
 * Objective-C bridge to create map view controllers.
 * This class is accessible from Kotlin via cinterop and creates
 * UIViewControllers containing MapLibre maps.
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
