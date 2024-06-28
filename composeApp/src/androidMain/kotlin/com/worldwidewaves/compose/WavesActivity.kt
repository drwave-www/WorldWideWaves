package com.worldwidewaves.compose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.R
import com.worldwidewaves.shared.WWWEvent
import com.worldwidewaves.shared.WWWEvents
import com.worldwidewaves.ui.AppTheme
import com.worldwidewaves.ui.extendedLight
import kotlinx.coroutines.coroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import worldwidewaves.composeapp.generated.resources.Res
import worldwidewaves.composeapp.generated.resources.events_select_all
import worldwidewaves.composeapp.generated.resources.events_select_starred
import java.nio.charset.StandardCharsets

class WavesActivity : AppCompatActivity() {

    @OptIn(ExperimentalResourceApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface {

                    // Load WorldWideWaves events
                    var events : List<WWWEvent> by remember { mutableStateOf(emptyList()) }
                    LaunchedEffect(Unit) {
                        val eventsConf = Res.readBytes("files/events.json")
                        events = WWWEvents(eventsConf.decodeToString()).events()
                    }

                    Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).fillMaxSize()) {
                        Column {
                            FavoritesSelector()
                            Spacer(modifier = Modifier.size(20.dp))
                            Events(events)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesSelector(starredSelected: Boolean = false, modifier: Modifier = Modifier) {
    val allColor = if (starredSelected) extendedLight.quaternary else extendedLight.quinary
    val starredColor = if (starredSelected) extendedLight.quinary else extendedLight.quaternary

    val allWeight = if(starredSelected) FontWeight.Normal else FontWeight.Bold
    val starredWeight = if(starredSelected) FontWeight.Bold else FontWeight.Normal

    Box(modifier = modifier
        .clip(RoundedCornerShape(25.dp))
        .background(extendedLight.quaternary.color)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier
                .clip(RoundedCornerShape(25.dp))
                .height(50.dp).fillMaxWidth(.5f)
                .background(allColor.color),
                contentAlignment = Alignment.Center) {
                Text(color = allColor.onColor, fontWeight = allWeight, fontSize = 16.sp,
                    text = stringResource(Res.string.events_select_all))
            }
            Box(modifier = Modifier
                .clip(RoundedCornerShape(25.dp))
                .height(50.dp).fillMaxWidth()
                .background(starredColor.color),
                contentAlignment = Alignment.Center) {
                Text(color = starredColor.onColor, fontWeight = starredWeight, fontSize = 16.sp,
                    text = stringResource(Res.string.events_select_starred))
            }
        }
    }
}

@Composable
fun Events(events: List<WWWEvent>, modifier: Modifier = Modifier) {
    Surface(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            events.forEach { event ->
                Event(event)
            }
        }
    }
}

@Composable
fun Event(event: WWWEvent, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Text(
            text = event.location,
            modifier = modifier.padding(24.dp)
        )
    }
}

