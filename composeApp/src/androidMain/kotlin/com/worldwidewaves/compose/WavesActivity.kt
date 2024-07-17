package com.worldwidewaves.compose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.WWWEvent
import com.worldwidewaves.shared.WWWEvents
import com.worldwidewaves.shared.isDone
import com.worldwidewaves.shared.isRunning
import com.worldwidewaves.shared.isSoon
import com.worldwidewaves.ui.AppTheme
import com.worldwidewaves.ui.extendedLight
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import worldwidewaves.composeapp.generated.resources.Res
import worldwidewaves.composeapp.generated.resources.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.jetbrains.compose.resources.painterResource as composePainterResource

// ----------------------------

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

                    Box(modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                        .fillMaxSize()) {
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

// ----------------------------

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
                .height(50.dp)
                .fillMaxWidth(.5f)
                .background(allColor.color),
                contentAlignment = Alignment.Center) {
                Text(color = allColor.onColor, fontWeight = allWeight, fontSize = 16.sp,
                    text = stringResource(Res.string.events_select_all))
            }
            Box(modifier = Modifier
                .clip(RoundedCornerShape(25.dp))
                .height(50.dp)
                .fillMaxWidth()
                .background(starredColor.color),
                contentAlignment = Alignment.Center) {
                Text(color = starredColor.onColor, fontWeight = starredWeight, fontSize = 16.sp,
                    text = stringResource(Res.string.events_select_starred))
            }
        }
    }
}

// ----------------------------

@Composable
fun Events(events: List<WWWEvent>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
    ) {
        items(events) { event ->
            Event(event)
        }
    }
}

@Composable
fun Event(event: WWWEvent, modifier: Modifier = Modifier) {
    Column(modifier = Modifier) {
        EventOverlay(event)
        EventLocationAndDate(event)
    }
}

@Composable
private fun EventOverlay(event: WWWEvent, modifier: Modifier = Modifier) {
    val heightModifier = Modifier.height(159.dp)

    // Main event space with image and layers
    Box(modifier = heightModifier) {
        // Main Image
        Box(modifier = heightModifier) {
            Image(
                modifier = modifier,
                contentScale = ContentScale.FillWidth,
                painter = getPainterFromDrawableName(event.id, "location")!!,
                contentDescription = event.location
            )
        }

        EventOverlayCountryAndCommunityFlags(event, heightModifier)
        EventOverlaySoonOrRunning(event)
        EventOverlayDone(event)
    }
}

@Composable
private fun EventOverlayDone(event: WWWEvent, modifier: Modifier = Modifier) {
    if (event.isDone()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Surface(
                color = Color.run { White.copy(alpha = 0.5f) },
                modifier = Modifier.fillMaxSize()
            ) { }
            Image(
                painter = composePainterResource(Res.drawable.event_done),
                contentDescription = stringResource(Res.string.event_done),
                modifier = Modifier.width(130.dp),
            )
        }
    }
}

@Composable
private fun EventOverlaySoonOrRunning(event: WWWEvent, modifier: Modifier = Modifier) {
    if (event.isSoon() || event.isRunning()) {
        val backgroundColor =
            if (event.isSoon()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
        val textId = if (event.isSoon()) Res.string.event_soon else Res.string.event_running

        Box(modifier = modifier.fillMaxWidth().offset(y = (-5).dp), contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .padding(top = 15.dp, end = 15.dp)
                    .size(width = 115.dp, height = 26.dp)
                    .background(backgroundColor)
                    .padding(end = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(textId),
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun EventOverlayCountryAndCommunityFlags(event: WWWEvent, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        event.community?.let {
            displayEntityImage(entityName = event.community!!, entityType = "community",
                modifier = Modifier.width(50.dp).padding(start = 10.dp, top = 10.dp).border(1.dp, Color.White))
        }
        event.country?.let {
            displayEntityImage(entityName = event.country!!, entityType = "country",
                modifier = Modifier.width(50.dp).padding(start = 10.dp, bottom = 10.dp).border(1.dp, Color.White))
        }
    }
}

// ----------------------------

@Composable
private fun EventLocationAndDate(event: WWWEvent, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
    ) {
        Column {

            // Location and date
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Text(
                        text = event.location.uppercase(),
                        style = TextStyle(
                            color = extendedLight.quinary.color,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontSize = 28.sp
                        ),
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                    Text(
                        text = LocalDate.parse(event.date).format(DateTimeFormatter.ofPattern("dd/MM")),
                        modifier = Modifier.padding(end = 2.dp),
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        )
                    )
            }

            // Country if present
            Text(
                text = event.country?.lowercase()?.replaceFirstChar(Char::titlecaseChar) ?: "",
                style = TextStyle(
                    color = extendedLight.quinary.color,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    fontSize = 14.sp
                ),
                modifier = Modifier.offset(y = (-8).dp)
            )

        }
    }
}

// ----------------------------

@Composable
fun displayEntityImage(entityName: String, modifier: Modifier = Modifier, entityType: String) {
    getPainterFromDrawableName(entityName, entityType)?.let { painter ->
        Image(
            modifier = modifier,
            painter = painter,
            contentDescription = entityName
        )
    }
}

@Composable
fun getPainterFromDrawableName(drawableName: String, entityType: String): Painter? {
    val context = LocalContext.current
    val resourceId = context.resources.getIdentifier(
        "e_${entityType}_${drawableName}", "drawable", context.packageName
    )

    return if (resourceId != 0) {
        painterResource(id = resourceId)
    } else {
        null
    }
}