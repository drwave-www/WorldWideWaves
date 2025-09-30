/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>

@class MapCameraCallbackWrapper;

/**
 * Objective-C protocol that MapLibreViewWrapper conforms to.
 * This allows Kotlin/Native to see and call the wrapper methods.
 */
@protocol MapLibreWrapperProtocol <NSObject>

// Map Setup
- (void)setStyle:(NSString *)styleURL completion:(void (^)(void))completion;

// Dimensions
- (double)getWidth;
- (double)getHeight;

// Camera Position
- (double)getCameraCenterLatitude;
- (double)getCameraCenterLongitude;
- (double)getCameraZoom;
- (NSArray<NSNumber *> *)getVisibleBounds;

// Camera Movement
- (void)moveCameraWithLatitude:(double)latitude longitude:(double)longitude zoom:(NSNumber *_Nullable)zoom;
- (void)animateCameraWithLatitude:(double)latitude
                        longitude:(double)longitude
                             zoom:(NSNumber *_Nullable)zoom
                         callback:(MapCameraCallbackWrapper *_Nullable)callback;
- (void)animateCameraToBoundsWithSwLat:(double)swLat
                                 swLng:(double)swLng
                                 neLat:(double)neLat
                                 neLng:(double)neLng
                               padding:(int)padding
                              callback:(MapCameraCallbackWrapper *_Nullable)callback;

// Camera Constraints
- (void)setBoundsForCameraTargetWithSwLat:(double)swLat
                                     swLng:(double)swLng
                                     neLat:(double)neLat
                                     neLng:(double)neLng;
- (void)setMinZoom:(double)minZoom;
- (void)setMaxZoom:(double)maxZoom;
- (double)getMinZoom;

// Wave Polygons
- (void)addWavePolygons:(NSArray<NSArray<NSValue *> *> *)polygons clearExisting:(BOOL)clearExisting;
- (void)clearWavePolygons;

// Override BBox
- (void)drawOverrideBboxWithSwLat:(double)swLat
                             swLng:(double)swLng
                             neLat:(double)neLat
                             neLng:(double)neLng;

// Event Listeners
- (void)setOnMapClickListener:(void (^)(double latitude, double longitude))listener;
- (void)setOnCameraIdleListener:(void (^)(void))listener;

@end
