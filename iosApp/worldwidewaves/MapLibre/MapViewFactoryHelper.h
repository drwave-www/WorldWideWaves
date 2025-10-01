/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

#ifndef MapViewFactoryHelper_h
#define MapViewFactoryHelper_h

#import <UIKit/UIKit.h>

/**
 * C function wrapper for WWWMapViewBridge.
 * This can be called directly from Kotlin without ObjC runtime complexity.
 */
UIViewController* WWW_createMapViewController(const char* styleURL, double latitude, double longitude, double zoom);

#endif /* MapViewFactoryHelper_h */
