package utils

import com.google.gson.JsonArray
import com.wrapper.spotify.model_objects.specification.Track

fun Array<Track>.toJsonArray(): JsonArray {
    val jsonArray = JsonArray()
    this.map { it.uri }
            .forEach { jsonArray.add(it) }
    return jsonArray
}

object SpotifyUtils {
    fun playTrack(track: Track) {

    }
}