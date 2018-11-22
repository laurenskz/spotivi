package utils

import auth.AuthenticatedSpotifyApi
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.Paging
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.model_objects.specification.TrackSimplified
import com.wrapper.spotify.requests.AbstractRequest
import com.wrapper.spotify.requests.data.AbstractDataRequest
import main.Main

fun Array<Track>.toJsonArray(): JsonArray {
    val jsonArray = JsonArray()
    this.map { it.uri }
            .forEach { jsonArray.add(it) }
    return jsonArray
}

fun <T> iterateOver(pager: (Int) -> Paging<T>): Iterable<T> {

    return object: Iterable<T> {
        override fun iterator() = object : Iterator<T> {
            var index = 0
            var current = nextItems()

            private fun nextItems() = pager(index)
                    .also {
                        index += it.items.size
                    }
                    .items.iterator()

            override fun hasNext(): Boolean {
                if (current.hasNext()) {
                    return true
                }
                current = nextItems()
                return current.hasNext()
            }

            override fun next(): T {
                return current.next()
            }
        }
    }

}

fun main(args: Array<String>) {
    iterateOver {
        val playlist = "spotify:user:11102248483:playlist:1yVw55DWxO4CduLDamzUMh".toPlaylist()
        Main.spotifyApi.wrapped.getPlaylistsTracks(playlist.user, playlist.playlist)
                .offset(it)
                .build()
                .execute()
    }.forEach {
        println(it.track.name)
    }
}

fun <T> Array<T>.createJson(mapper: (T) -> String): JsonArray {
    val jsonArray = JsonArray()
    this.map(mapper)
            .forEach { jsonArray.add(it) }
    return jsonArray
}

fun Track.toJson(): JsonObject {
    return uriToJsonTrack(this.uri)
}

fun uriToJsonTrack(uri: String): JsonObject {
    return JsonParser().parse("{\"uri\":\"$uri\"}").asJsonObject
}

data class Playlist(val user: String, val playlist: String)

fun String.toPlaylist(): Playlist {
    val values = this.split(":")
            .windowed(2, 1, false) {
                it[0] to it[1]
            }.toMap()
    val user = values["user"] ?: throw IllegalArgumentException("Malformed playlist: no user found")
    val playlist = values["playlist"] ?: throw IllegalArgumentException("Malformed playlist: no playlist found")
    return Playlist(user, playlist)
}