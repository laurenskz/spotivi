package commands

import auth.AuthenticatedSpotifyApi
import com.wrapper.spotify.requests.data.AbstractDataRequest
import com.wrapper.spotify.requests.data.player.StartResumeUsersPlaybackRequest
import utils.createJson

class BasicHandlers(private val api: AuthenticatedSpotifyApi) : CommandHandler {

    override fun handle(command: Command) {
        when (command) {
            is PlayAlbumFromCurrentSong -> playAlbumFromCurrentSong()
            is NextTrackCommand -> api.wrapped.skipUsersPlaybackToNextTrack()
            is PreviousTrackCommand -> api.wrapped.skipUsersPlaybackToPreviousTrack()
            is PauseCurrentSong -> api.wrapped.pauseUsersPlayback()
            is RadioFromCurrentSong -> radioFromCurrentSong()
            else -> null
        }?.build()?.execute<Any>()
    }

    fun currentSong() = api
            .wrapped
            .usersCurrentlyPlayingTrack.build().execute()
            .item

    fun radioFromCurrentSong() = currentSong().let {
        api.wrapped.recommendations.seed_tracks(it.id)
                .build()
                .execute()
                .tracks
                .let {
                    api.wrapped.startResumeUsersPlayback()
                            .uris(it.createJson { it.uri })
                }
    }

    fun playAlbumFromCurrentSong(): AbstractDataRequest.Builder<StartResumeUsersPlaybackRequest.Builder> =
            currentSong().let {
                api.wrapped.startResumeUsersPlayback()
                        .context_uri(it.album.uri)
            }
}