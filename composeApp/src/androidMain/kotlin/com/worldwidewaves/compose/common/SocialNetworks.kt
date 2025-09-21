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
import com.worldwidewaves.shared.WWWGlobals.Companion.Common
import com.worldwidewaves.shared.WWWGlobals.Companion.Dimensions
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.instagram_icon
import com.worldwidewaves.theme.commonBoldStyle
import com.worldwidewaves.theme.commonTextStyle
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

/** Displays clickable Instagram account & hashtag with logo; opens external URI on tap. */
@Composable
fun WWWSocialNetworks(
    modifier: Modifier = Modifier,
    instagramAccount: String,
    instagramHashtag: String,
) {
    val uriHandler = LocalUriHandler.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(Res.drawable.instagram_icon),
            contentDescription = stringResource(MokoRes.strings.instagram_logo_description),
            modifier = Modifier.width(Common.SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH.dp),
        )
        Column(
            modifier = Modifier.padding(start = 10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                modifier =
                    Modifier.clickable(onClick = {
                        try {
                            val uri = "Urls.INSTAGRAM_BASE${instagramAccount.removePrefix("@")}"
                            uriHandler.openUri(uri)
                        } catch (e: SecurityException) {
                            Log.e("AboutWWWSocialNetworks", "Security error opening Instagram URI", e)
                        } catch (e: IllegalArgumentException) {
                            Log.e("AboutWWWSocialNetworks", "Invalid Instagram URI format", e)
                        } catch (e: UnsupportedOperationException) {
                            Log.e("AboutWWWSocialNetworks", "Unsupported URI operation", e)
                        }
                    }),
                text = instagramAccount,
                style =
                    commonBoldStyle(Common.SOCIALNETWORKS_ACCOUNT_FONTSIZE).copy(
                        textDecoration = TextDecoration.Underline,
                    ),
            )
            Text(
                text = instagramHashtag,
                style = commonTextStyle(Common.SOCIALNETWORKS_HASHTAG_FONTSIZE),
            )
        }
    }
    Spacer(modifier = Modifier.size(Dimensions.SPACER_MEDIUM.dp))
}
