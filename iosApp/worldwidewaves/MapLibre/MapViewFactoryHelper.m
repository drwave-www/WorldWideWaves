/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "WWWMapViewBridge.h"

/**
 * Simple C function that Kotlin can call directly (no ObjC runtime needed).
 * This wraps the WWWMapViewBridge ObjC class method for easy Kotlin access.
 */
UIViewController* WWW_createMapViewController(const char* styleURL, double latitude, double longitude, double zoom) {
    NSString *nsStyleURL = [NSString stringWithUTF8String:styleURL];
    return [WWWMapViewBridge createMapViewControllerWithStyleURL:nsStyleURL
                                                         latitude:latitude
                                                        longitude:longitude
                                                             zoom:zoom];
}
