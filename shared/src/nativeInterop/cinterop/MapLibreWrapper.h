/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

#ifndef MapLibreWrapper_h
#define MapLibreWrapper_h

#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>

/**
 * Objective-C protocol for MapLibre wrapper interface.
 * The iOS app's MapLibreViewWrapper.swift implements this protocol.
 * Kotlin code in IOSMapLibreAdapter calls these methods via cinterop.
 */
@protocol WWWMapLibreWrapperProtocol <NSObject>

// Map Setup
- (void)setStyle:(NSString *)styleURL completion:(void (^)(void))completion;

// Dimensions
- (double)getWidth;
- (double)getHeight;

// Camera Position
- (double)getCameraCenterLatitude;
- (double)getCameraCenterLongitude;
- (double)getCameraZoom;
- (NSArray *)getVisibleBounds;

// Camera Movement
- (void)moveCameraWithLatitude:(double)latitude longitude:(double)longitude zoom:(NSNumber *)zoom;

// Camera Constraints
- (void)setMinZoom:(double)minZoom;
- (void)setMaxZoom:(double)maxZoom;
- (double)getMinZoom;

// Wave Polygons - simplified for cinterop
- (void)clearWavePolygons;

@end

#endif /* MapLibreWrapper_h */
