import com.worldwidewaves.shared.WWWEvents

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

//expect fun getEvents() : WWWEvents