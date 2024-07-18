package com.worldwidewaves

import android.content.Context
import android.os.Build
import com.worldwidewaves.shared.WWWPlatform
import java.lang.ref.WeakReference

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

