package auth

import com.wrapper.spotify.SpotifyApi

interface AuthenticatedSpotifyApi {
    val wrapped: SpotifyApi
}