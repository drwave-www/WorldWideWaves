/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <Foundation/Foundation.h>

// This header exposes MapLibre Swift wrapper to Objective-C for Kotlin/Native interop
// The actual implementation is in MapLibreViewWrapper.swift (via @objc annotations)

NS_ASSUME_NONNULL_BEGIN

@interface MapLibreViewWrapper : NSObject
- (instancetype)init;
- (void)setMapView:(id)mapView;
- (void)setStyleWithStyleURL:(NSString *)styleURL completion:(void (^)(void))completion;
- (double)getWidth;
- (double)getHeight;
- (void)moveCameraWithLatitude:(double)latitude longitude:(double)longitude zoom:(NSNumber * _Nullable)zoom;
- (void)animateCameraWithLatitude:(double)latitude
                        longitude:(double)longitude
                             zoom:(NSNumber * _Nullable)zoom
                         callback:(id _Nullable)callback;
- (void)animateCameraToBoundsWithSwLat:(double)swLat
                                 swLng:(double)swLng
                                 neLat:(double)neLat
                                 neLng:(double)neLng
                               padding:(NSInteger)padding
                              callback:(id _Nullable)callback;
- (void)setBoundsForCameraTargetWithSwLat:(double)swLat swLng:(double)swLng neLat:(double)neLat neLng:(double)neLng;
- (void)setMinZoom:(double)minZoom;
- (void)setMaxZoom:(double)maxZoom;
- (double)getMinZoom;
- (void)setAttributionMarginsWithLeft:(NSInteger)left top:(NSInteger)top right:(NSInteger)right bottom:(NSInteger)bottom;
- (void)addWavePolygons:(NSArray<NSArray<id> *> *)polygons clearExisting:(BOOL)clearExisting;
- (void)clearWavePolygons;
- (void)drawOverrideBboxWithSwLat:(double)swLat swLng:(double)swLng neLat:(double)neLat neLng:(double)neLng;
- (void)setOnMapClickListener:(void (^)(double latitude, double longitude))listener;
- (void)setOnCameraIdleListener:(void (^)(void))listener;
@end

@interface MapCameraCallbackWrapper : NSObject
- (instancetype)initWithOnFinish:(void (^)(void))onFinish onCancel:(void (^)(void))onCancel;
- (void)onFinish;
- (void)onCancel;
@end

NS_ASSUME_NONNULL_END
