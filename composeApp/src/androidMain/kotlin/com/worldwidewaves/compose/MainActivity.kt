package com.worldwidewaves.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worldwidewaves.ui.AppTheme
import java.util.Timer
import kotlin.concurrent.timerTask
import com.worldwidewaves.shared.R as CR


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.background)
                ) {
                    Box {
                        Image(
                            painter = painterResource(CR.drawable.background),
                            contentDescription = stringResource(CR.string.background_description),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterStart)
                                .offset(y = (-55).dp) // TODO: how to solve image to top without fixed offset ?
                        )
                        Image(
                            painter = painterResource(CR.drawable.www_logo_transparent),
                            contentDescription = stringResource(CR.string.logo_description),
                            modifier = Modifier
                                .width(200.dp)
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        )
                    }
                }
            }
        }

        val activity = this
        Timer().schedule(timerTask {
            val intent = Intent(activity, WavesActivity::class.java)
            startActivity(intent)
        }, 2000)

    }
}
