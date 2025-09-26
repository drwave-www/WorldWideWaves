package com.worldwidewaves.shared.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow

@Composable
expect fun <T> Flow<T>.collectAsStateSafe(initial: T): State<T>

@Composable
expect fun isStarted(): Boolean
