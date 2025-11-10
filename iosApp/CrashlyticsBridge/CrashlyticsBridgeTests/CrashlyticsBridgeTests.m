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

#import <XCTest/XCTest.h>
#import "CrashlyticsBridge.h"
#import <FirebaseCrashlytics/FirebaseCrashlytics.h>

/// Unit tests for CrashlyticsBridge static library
///
/// These tests verify the Objective-C interface exists and has the correct selectors.
/// They do NOT test Firebase functionality (which requires app initialization context).
/// Integration testing happens via iOS app + existing Kotlin tests.
@interface CrashlyticsBridgeTests : XCTestCase
@end

@implementation CrashlyticsBridgeTests

- (void)setUp {
    [super setUp];
    // Note: Firebase not initialized in unit tests (would require app context)
}

- (void)testClassExists {
    // Verify class can be loaded
    Class bridgeClass = NSClassFromString(@"CrashlyticsBridge");
    XCTAssertNotNil(bridgeClass, @"CrashlyticsBridge class should exist");
}

- (void)testRecordExceptionMethod {
    // Verify method selector exists
    SEL selector = @selector(recordExceptionWithMessage:tag:stackTrace:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"recordExceptionWithMessage:tag:stackTrace: should exist");
}

- (void)testLogMethod {
    SEL selector = @selector(logWithMessage:tag:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"logWithMessage:tag: should exist");
}

- (void)testSetCustomKeyMethod {
    SEL selector = @selector(setCustomKeyWithKey:value:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"setCustomKeyWithKey:value: should exist");
}

- (void)testSetUserIdMethod {
    SEL selector = @selector(setUserId:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"setUserId: should exist");
}

- (void)testTestCrashMethod {
    SEL selector = @selector(testCrash);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"testCrash should exist");
}

- (void)testIsCrashlyticsCollectionEnabledMethod {
    SEL selector = @selector(isCrashlyticsCollectionEnabled);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"isCrashlyticsCollectionEnabled should exist");
}

- (void)testSetCrashlyticsCollectionEnabledMethod {
    SEL selector = @selector(setCrashlyticsCollectionEnabled:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"setCrashlyticsCollectionEnabled: should exist");
}

// Integration tests (require Firebase initialization)
// These would need to be run in an app context or with mocked Firebase
// Full integration testing happens via iOS app + existing Kotlin tests

@end
