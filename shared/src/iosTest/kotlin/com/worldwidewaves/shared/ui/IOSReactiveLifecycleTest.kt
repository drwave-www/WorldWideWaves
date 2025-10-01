package com.worldwidewaves.shared.ui

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for iOS reactive pattern lifecycle management and leak prevention.
 *
 * These tests verify that subscriptions are properly tracked and cleaned up
 * to prevent memory leaks on iOS.
 */
class IOSReactiveLifecycleTest {
    @Test
    fun `IOSStateFlowObservable subscription is active after observe`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()

            val subscription = observable.observe { }

            assertTrue(subscription.isActive, "Subscription should be active after observe")
        }

    @Test
    fun `IOSStateFlowObservable subscription is inactive after dispose`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()

            val subscription = observable.observe { }
            subscription.dispose()

            assertFalse(subscription.isActive, "Subscription should be inactive after dispose")
        }

    @Test
    fun `IOSStateFlowObservable dispose is idempotent`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()

            val subscription = observable.observe { }
            subscription.dispose()
            subscription.dispose() // Should not throw
            subscription.dispose() // Should not throw

            assertFalse(subscription.isActive, "Subscription should remain inactive")
        }

    @Test
    fun `IOSStateFlowObservable multiple subscriptions can coexist`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()

            val subscription1 = observable.observe { }
            val subscription2 = observable.observe { }
            val subscription3 = observable.observe { }

            assertTrue(subscription1.isActive, "Subscription 1 should be active")
            assertTrue(subscription2.isActive, "Subscription 2 should be active")
            assertTrue(subscription3.isActive, "Subscription 3 should be active")
        }

    @Test
    fun `IOSStateFlowObservable disposing one subscription does not affect others`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()

            val subscription1 = observable.observe { }
            val subscription2 = observable.observe { }
            val subscription3 = observable.observe { }

            subscription2.dispose()

            assertTrue(subscription1.isActive, "Subscription 1 should remain active")
            assertFalse(subscription2.isActive, "Subscription 2 should be disposed")
            assertTrue(subscription3.isActive, "Subscription 3 should remain active")
        }

    @Test
    fun `IOSStateFlowObservable cleanup disposes all active subscriptions`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()

            val subscription1 = observable.observe { }
            val subscription2 = observable.observe { }
            val subscription3 = observable.observe { }

            observable.cleanup()

            assertFalse(subscription1.isActive, "Subscription 1 should be disposed after cleanup")
            assertFalse(subscription2.isActive, "Subscription 2 should be disposed after cleanup")
            assertFalse(subscription3.isActive, "Subscription 3 should be disposed after cleanup")
        }

    @Test
    fun `IOSStateFlowObservable cleanup is idempotent`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()

            val subscription = observable.observe { }

            observable.cleanup()
            observable.cleanup() // Should not throw
            observable.cleanup() // Should not throw

            assertFalse(subscription.isActive, "Subscription should remain disposed")
        }

    @Test
    fun `IOSStateFlowObservable no leaks after multiple subscribe and unsubscribe cycles`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()

            // Simulate multiple iOS view lifecycle cycles
            repeat(100) {
                val subscription = observable.observe { }
                subscription.dispose()
            }

            assertEquals(
                0,
                observable.activeSubscriptionCount,
                "Should have no active subscriptions after all disposed",
            )
        }

    @Test
    fun `IOSStateFlowObservable callback receives updates after subscription`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()
            val receivedValues = mutableListOf<String>()

            observable.observe { value -> receivedValues.add(value) }

            stateFlow.value = "updated1"
            stateFlow.value = "updated2"

            delay(100) // Allow coroutines to process

            assertTrue(receivedValues.contains("updated1"), "Should receive updated1")
            assertTrue(receivedValues.contains("updated2"), "Should receive updated2")
        }

    @Test
    fun `IOSStateFlowObservable callback does not receive updates after dispose`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()
            var callbackInvocations = 0

            val subscription = observable.observe { callbackInvocations++ }
            subscription.dispose()

            stateFlow.value = "updated1"
            stateFlow.value = "updated2"

            delay(100) // Allow coroutines to process

            assertTrue(
                callbackInvocations <= 1,
                "Callback should not be invoked after dispose (got $callbackInvocations invocations)",
            )
        }

    @Test
    fun `IOSFlowObservable subscription is active after observe`() =
        runTest {
            val flow =
                flow {
                    emit("value1")
                    emit("value2")
                }
            val observable = flow.toIOSObservableFlow()

            val subscription = observable.observe { }

            assertTrue(subscription.isActive, "Subscription should be active after observe")
        }

    @Test
    fun `IOSFlowObservable subscription is inactive after dispose`() =
        runTest {
            val flow =
                flow {
                    emit("value1")
                    emit("value2")
                }
            val observable = flow.toIOSObservableFlow()

            val subscription = observable.observe { }
            subscription.dispose()

            assertFalse(subscription.isActive, "Subscription should be inactive after dispose")
        }

    @Test
    fun `IOSFlowObservable cleanup disposes all active subscriptions`() =
        runTest {
            val flow =
                flow {
                    emit("value1")
                    emit("value2")
                }
            val observable = flow.toIOSObservableFlow()

            val subscription1 = observable.observe { }
            val subscription2 = observable.observe { }

            observable.cleanup()

            assertFalse(subscription1.isActive, "Subscription 1 should be disposed after cleanup")
            assertFalse(subscription2.isActive, "Subscription 2 should be disposed after cleanup")
        }

    @Test
    fun `IOSFlowObservable no leaks after multiple subscribe and unsubscribe cycles`() =
        runTest {
            val flow =
                flow {
                    emit("value1")
                    emit("value2")
                }
            val observable = flow.toIOSObservableFlow()

            // Simulate multiple iOS view lifecycle cycles
            repeat(100) {
                val subscription = observable.observe { }
                subscription.dispose()
            }

            assertEquals(
                0,
                observable.activeSubscriptionCount,
                "Should have no active subscriptions after all disposed",
            )
        }

    @Test
    fun `IOSReactiveSubscriptionManager tracks all added subscriptions`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()
            val manager = IOSReactiveSubscriptionManagerImpl()

            val subscription1 = observable.observe { }
            val subscription2 = observable.observe { }
            val subscription3 = observable.observe { }

            manager.addSubscription(subscription1)
            manager.addSubscription(subscription2)
            manager.addSubscription(subscription3)

            assertEquals(
                3,
                manager.activeSubscriptionCount,
                "Manager should track all active subscriptions",
            )
        }

    @Test
    fun `IOSReactiveSubscriptionManager disposeAll cleans up all subscriptions`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()
            val manager = IOSReactiveSubscriptionManagerImpl()

            val subscription1 = observable.observe { }
            val subscription2 = observable.observe { }
            val subscription3 = observable.observe { }

            manager.addSubscription(subscription1)
            manager.addSubscription(subscription2)
            manager.addSubscription(subscription3)

            manager.disposeAll()

            assertEquals(0, manager.activeSubscriptionCount, "All subscriptions should be disposed")
            assertFalse(subscription1.isActive, "Subscription 1 should be disposed")
            assertFalse(subscription2.isActive, "Subscription 2 should be disposed")
            assertFalse(subscription3.isActive, "Subscription 3 should be disposed")
        }

    @Test
    fun `IOSLifecycleObserver onViewDeinit triggers cleanup`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")
            val observable = stateFlow.toIOSObservable()
            val manager = IOSReactiveSubscriptionManagerImpl()
            val lifecycleObserver = IOSLifecycleObserverImpl(manager)

            val subscription1 = observable.observe { }
            val subscription2 = observable.observe { }

            manager.addSubscription(subscription1)
            manager.addSubscription(subscription2)

            lifecycleObserver.onViewDeinit()

            assertEquals(
                0,
                manager.activeSubscriptionCount,
                "All subscriptions should be disposed after deinit",
            )
        }
}
