package com.worldwidewaves.compose.common

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOCIALNETWORKS_ACCOUNT_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOCIALNETWORKS_HASHTAG_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_COMMON_SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_SPACER_MEDIUM
import com.worldwidewaves.shared.WWWGlobals.Companion.URL_BASE_INSTAGRAM
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.instagram_icon
import com.worldwidewaves.theme.commonBoldStyle
import com.worldwidewaves.theme.commonTextStyle
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

@Composable
/** Displays clickable Instagram account & hashtag with logo; opens external URI on tap. */
fun WWWSocialNetworks(
    modifier: Modifier = Modifier,
    instagramAccount: String,
    instagramHashtag: String
) {
    val uriHandler = LocalUriHandler.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(Res.drawable.instagram_icon),
            contentDescription = stringResource(MokoRes.strings.instagram_logo_description),
            modifier = Modifier.width(DIM_COMMON_SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH.dp)
        )
        Column(
            modifier = Modifier.padding(start = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.clickable(onClick = {
                    try {
                        val uri = "$URL_BASE_INSTAGRAM${instagramAccount.removePrefix("@")}"
                        uriHandler.openUri(uri)
                    } catch (e: Exception) {
                        Log.e("AboutWWWSocialNetworks", "Failed to open URI", e)
                    }
                }),
                text = instagramAccount,
                style = commonBoldStyle(DIM_COMMON_SOCIALNETWORKS_ACCOUNT_FONTSIZE).copy(
                    textDecoration = TextDecoration.Underline
                )
            )
            Text(
                text = instagramHashtag,
                style = commonTextStyle(DIM_COMMON_SOCIALNETWORKS_HASHTAG_FONTSIZE)
            )
        }
    }
    Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_MEDIUM.dp))
}
