package lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow

@Composable
actual fun <T> Flow<T>.collectAsStateLifecycle(initial: T): State<T> = this.collectAsState(initial)

@Composable
actual fun isLifecycleAtLeastStarted(): Boolean = true
