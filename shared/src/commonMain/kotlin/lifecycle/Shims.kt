package lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow

@Composable
expect fun <T> Flow<T>.collectAsStateLifecycle(initial: T): State<T>

@Composable
expect fun isLifecycleAtLeastStarted(): Boolean
