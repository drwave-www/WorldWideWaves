package com.worldwidewaves.shared.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
actual fun <T> Flow<T>.collectAsStateSafe(initial: T): State<T> = this.collectAsStateWithLifecycle(initial)

@Composable
actual fun isStarted(): Boolean =
    LocalLifecycleOwner.current.lifecycle.currentState
        .isAtLeast(Lifecycle.State.STARTED)
