package com.worldwidewaves.shared

import android.content.Context
import android.os.Build
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.e_community_europe
import com.worldwidewaves.shared.generated.resources.e_community_usa
import com.worldwidewaves.shared.generated.resources.e_country_brazil
import com.worldwidewaves.shared.generated.resources.e_country_france
import com.worldwidewaves.shared.generated.resources.e_location_paris_france
import com.worldwidewaves.shared.generated.resources.e_location_riodejaneiro_brazil
import com.worldwidewaves.shared.generated.resources.e_location_unitedstates
import com.worldwidewaves.shared.generated.resources.e_location_world
import com.worldwidewaves.shared.generated.resources.not_found
import java.lang.ref.WeakReference

// --- Platform-specific implementation of the WWWPlatform interface ---

object AndroidPlatform : WWWPlatform {
    private var _contextRef: WeakReference<Context>? = null

    private val context: Context
        get() = _contextRef?.get() ?: throw UninitializedPropertyAccessException("AndroidPlatform must be initialized with a context before use.")

    override val name: String
        get() = "Android ${Build.VERSION.SDK_INT}"

    override fun getContext(): Any = context

    fun initialize(context: Context) {
        if (_contextRef == null) {
            _contextRef = WeakReference(context.applicationContext)
        } else {
            throw IllegalStateException("AndroidPlatform can only be initialized once.")
        }
    }
}

// --- Platform-specific API ---

actual fun getPlatform(): WWWPlatform = AndroidPlatform

actual fun getImage(type: String, id: String): Any? {
    return when (type) {
        "location" -> when (id) {
            "paris_france" -> Res.drawable.e_location_paris_france
            "unitedstates" -> Res.drawable.e_location_unitedstates
            "riodejaneiro_brazil" -> Res.drawable.e_location_riodejaneiro_brazil
            "world" -> Res.drawable.e_location_world
            else -> Res.drawable.not_found
        }
        "community" -> when (id) {
            "europe" -> Res.drawable.e_community_europe
            "usa" -> Res.drawable.e_community_usa
            else -> Res.drawable.not_found
        }
        "country" -> when (id) {
            "brazil" -> Res.drawable.e_country_brazil
            "france" -> Res.drawable.e_country_france
            else -> Res.drawable.not_found
        }
        else -> Res.drawable.not_found
    }
}