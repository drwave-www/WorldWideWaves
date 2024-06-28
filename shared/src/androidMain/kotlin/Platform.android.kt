import android.os.Build
import com.worldwidewaves.shared.WWWEvents

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

//fun getJsonDataFromAsset(context: Context, fileName: String): String {
//    val inputStream = context.assets.open(fileName)
//    return inputStream.readText()
//}
//actual fun getEvents() : WWWEvents {
//    val json = getJsonDataFromAsset(context, "events.json")
//    return WWWEvents(json)
//}