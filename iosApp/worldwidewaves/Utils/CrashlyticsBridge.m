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

#import "CrashlyticsBridge.h"
#import <FirebaseCrashlytics/FirebaseCrashlytics.h>

/// Objective-C bridge for Crashlytics that can be called from Kotlin/Native via cinterop.
///
/// This class provides a stable Objective-C interface for Kotlin/Native to access Firebase Crashlytics.
/// Unlike Swift @objc classes, pure Objective-C classes guarantee the OBJC_CLASS symbol exists in
/// the final binary, even with aggressive optimizations and dead code stripping.
///
/// ## Why Objective-C Instead of Swift?
///
/// Swift's @objc annotation creates Swift symbols, not true Objective-C symbols.
/// The linker's -dead_strip flag removes "unused" Objective-C metadata from Swift classes.
/// Result: Kotlin cinterop crashes with "symbol not found _OBJC_CLASS_$_CrashlyticsBridge"
///
/// Pure Objective-C implementation guarantees the symbol exists for Kotlin/Native cinterop.
@implementation CrashlyticsBridge

+ (void)recordExceptionWithMessage:(NSString *)message
                               tag:(NSString *)tag
                        stackTrace:(NSString * _Nullable)stackTrace {
    // Build error domain from tag
    NSString *domain = [NSString stringWithFormat:@"com.worldwidewaves.%@", tag];

    // Create userInfo dictionary
    NSDictionary *userInfo = @{
        NSLocalizedDescriptionKey: message,
        @"tag": tag,
        @"stackTrace": stackTrace ?: @"No stack trace available"
    };

    // Create NSError and record to Crashlytics
    NSError *error = [NSError errorWithDomain:domain code:-1 userInfo:userInfo];
    [[FIRCrashlytics crashlytics] recordError:error];

    // Log locally for debugging
    NSLog(@"[CrashlyticsBridge] Recorded exception from Kotlin: [%@] %@", tag, message);
}

+ (void)logWithMessage:(NSString *)message tag:(NSString *)tag {
    // Format message with tag
    NSString *formattedMessage = [NSString stringWithFormat:@"[%@] %@", tag, message];

    // Log to Crashlytics (appears as breadcrumb in crash reports)
    [[FIRCrashlytics crashlytics] log:formattedMessage];
}

+ (void)setCustomKeyWithKey:(NSString *)key value:(NSString *)value {
    // Set custom key-value pair for crash reports
    [[FIRCrashlytics crashlytics] setCustomValue:value forKey:key];
}

+ (void)setUserId:(NSString *)userId {
    // Set user ID for crash reports
    [[FIRCrashlytics crashlytics] setUserID:userId];
}

+ (void)testCrash {
    // Log warning before crash
    NSLog(@"[CrashlyticsBridge] TEST CRASH TRIGGERED - App will terminate");
    [[FIRCrashlytics crashlytics] log:@"TEST CRASH: This is a deliberate crash for testing Crashlytics"];

    // Force crash after brief delay to allow log to be recorded
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.1 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        @throw [NSException exceptionWithName:@"CrashlyticsTestCrash"
                                       reason:@"TEST CRASH: Crashlytics test crash triggered by user"
                                     userInfo:nil];
    });
}

+ (BOOL)isCrashlyticsCollectionEnabled {
    return [[FIRCrashlytics crashlytics] isCrashlyticsCollectionEnabled];
}

+ (void)setCrashlyticsCollectionEnabled:(BOOL)enabled {
    [[FIRCrashlytics crashlytics] setCrashlyticsCollectionEnabled:enabled];

    NSString *status = enabled ? @"enabled" : @"disabled";
    NSLog(@"[CrashlyticsBridge] Crashlytics collection %@", status);
}

@end
