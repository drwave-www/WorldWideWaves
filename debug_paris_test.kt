// Quick debug test for Paris area detection
import com.worldwidewaves.shared.events.utils.Position

fun main() {
    val parisCoords = Position(48.8566, 2.3522)
    println("Testing Paris coordinates: $parisCoords")

    // This would need to be tested with actual event loading...
    // But we can at least check the coordinates are valid
    println("Latitude valid: ${parisCoords.lat in -90.0..90.0}")
    println("Longitude valid: ${parisCoords.lng in -180.0..180.0}")
}