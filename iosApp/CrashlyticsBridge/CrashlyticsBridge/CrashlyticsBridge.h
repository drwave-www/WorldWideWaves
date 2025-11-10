/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// Objective-C header for CrashlyticsBridge
/// This allows Kotlin/Native to import and call the Swift implementation
@interface CrashlyticsBridge : NSObject

+ (void)recordExceptionWithMessage:(NSString *)message
                               tag:(NSString *)tag
                        stackTrace:(NSString * _Nullable)stackTrace;

+ (void)logWithMessage:(NSString *)message
                   tag:(NSString *)tag;

+ (void)setCustomKeyWithKey:(NSString *)key
                      value:(NSString *)value;

+ (void)setUserId:(NSString *)userId;

+ (void)testCrash;

+ (BOOL)isCrashlyticsCollectionEnabled;

+ (void)setCrashlyticsCollectionEnabled:(BOOL)enabled;

@end

NS_ASSUME_NONNULL_END
