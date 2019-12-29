package commands

import auth.AuthenticatedSpotifyApi
import com.google.gson.JsonParser
import com.wrapper.spotify.requests.data.AbstractDataRequest
import com.wrapper.spotify.requests.data.player.StartResumeUsersPlaybackRequest
import model.Model
import utils.createJson
import utils.toPlaylist
import utils.uriToJsonTrack
import java.util.concurrent.TimeUnit

class BasicHandlers(private val api: AuthenticatedSpotifyApi) : CommandHandler {

    override fun handle(command: Command, model: Model): Model {
        when (command) {
            is NextTrackCommand -> api.wrapped.skipUsersPlaybackToNextTrack()
            is PreviousTrackCommand -> api.wrapped.skipUsersPlaybackToPreviousTrack()
            is TogglePlayCommand -> api.wrapped.pauseUsersPlayback()
                    .takeIf {
                        api.wrapped.informationAboutUsersCurrentPlayback.build()
                                .execute()
                                .is_playing
                    }
                    ?: api.wrapped.startResumeUsersPlayback()
            is TransferCommand -> api.wrapped.usersAvailableDevices.build().execute()
                    .also { it.forEach { println(it.name) } }
                    .firstOrNull { it.name.trim().equals(command.deviceName.trim(), ignoreCase = true) }
                    ?.takeIf {
                        api.wrapped.informationAboutUsersCurrentPlayback
                                .build().execute()
                                .device.id != it.id
                    }
                    ?.also { println("Found device $it") }
                    ?.let { api.wrapped.transferUsersPlayback(JsonParser().parse("[\"${it.id}\"]").asJsonArray) }
            is AddCurrentToCommand -> api.wrapped.informationAboutUsersCurrentPlayback.build().execute()
                    .item
                    .let {
                        val playlist = command.playlistId.toPlaylist()
                        api.wrapped.removeTracksFromPlaylist(playlist.user, playlist.playlist,
                                JsonParser().parse("[{\"uri\":\"${it.uri}\"}]").asJsonArray
                        )
                                .build()
                                .execute()

                        api.wrapped.addTracksToPlaylist(playlist.user, playlist.playlist, arrayOf(it.uri))

                    }
            is ChangeVolume -> api.wrapped.informationAboutUsersCurrentPlayback.build().execute()
                    .device
                    .let {
                        api.wrapped.setVolumeForUsersPlayback(it.volume_percent + command.change)
                    }
            is ShuffleCommand -> api.wrapped.toggleShuffleForUsersPlayback(true)
            is StartOnCommand -> {
                api.wrapped.usersAvailableDevices
                        .build()
                        .execute()
                        .firstOrNull { it.name.trim().equals(command.deviceId.trim(), ignoreCase = true) }
                        ?.let {
                            api.wrapped.startResumeUsersPlayback()
                                    .device_id(it.id)
                                    .context_uri(command.contextId)
                        }
            }
            else -> null
        }
                ?.build()
                ?.execute()
        return model
    }

}