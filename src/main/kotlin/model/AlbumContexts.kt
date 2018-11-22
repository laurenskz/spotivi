package model

import auth.AuthenticatedSpotifyApi
import com.wrapper.spotify.enums.AlbumType
import com.wrapper.spotify.model_objects.specification.*
import com.wrapper.spotify.requests.data.AbstractDataRequest
import main.Main
import utils.iterateOver
import utils.toPlaylist


fun playlistToAlbums(api: AuthenticatedSpotifyApi, playlistUri: String): Iterable<AlbumSimplified> {
    val playlistFormatted = playlistUri.toPlaylist()
    return iterateOver {
        api.wrapped.getPlaylistsTracks(playlistFormatted.user, playlistFormatted.playlist)
                .offset(it)
                .build()
                .execute()
    }
            .asSequence()
            .filter { it.track.album.albumType == AlbumType.ALBUM }
            .map { it.track.album }
            .asIterable()
}

fun artistToAlbums(api: AuthenticatedSpotifyApi, artistId: String): Iterable<AlbumSimplified> {
    return iterateOver {
        api.wrapped.getArtistsAlbums(artistId)
                .album_type("album")
                .offset(it)
                .build()
                .execute()
    }
}

fun main(args: Array<String>) {
    val api = Main.spotifyApi
    BasicAlbumContext(playlistToAlbums(api, "spotify:user:spotify:playlist:37i9dQZF1DX4xuWVBs4FgJ"), null)
            .also { it.seekNext() }
            .currentAlbum()
            .name
            .also { println(it) }
}