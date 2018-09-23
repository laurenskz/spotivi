package utils

import com.google.gson.JsonArray
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.model_objects.specification.TrackSimplified

fun Array<Track>.toJsonArray(): JsonArray {
    val jsonArray = JsonArray()
    this.map { it.uri }
            .forEach { jsonArray.add(it) }
    return jsonArray
}

fun <T> Array<T>.createJson(mapper: (T) -> String): JsonArray {
    val jsonArray = JsonArray()
    this.map(mapper)
            .forEach { jsonArray.add(it) }
    return jsonArray
}