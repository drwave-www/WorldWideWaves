package lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
actual fun <T> Flow<T>.collectAsStateLifecycle(initial: T): State<T> = collectAsStateWithLifecycle(initialValue = initial)

@Composable
actual fun isLifecycleAtLeastStarted(): Boolean =
    LocalLifecycleOwner.current.lifecycle.currentState.isAtLeast(
        androidx.lifecycle.Lifecycle.State.STARTED,
    )
