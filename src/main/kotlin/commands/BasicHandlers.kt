package commands

import auth.AuthenticatedSpotifyApi
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.wrapper.spotify.model_objects.miscellaneous.Device
import com.wrapper.spotify.requests.data.AbstractDataRequest
import com.wrapper.spotify.requests.data.player.StartResumeUsersPlaybackRequest
import model.Model
import utils.*
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
                deviceNameToId(command.deviceId)
                        ?.let {
                            api.wrapped.startResumeUsersPlayback()
                                    .device_id(it)
                                    .context_uri(command.contextId)
                        }
            }
            is StopListening -> {
                val context = api.wrapped.informationAboutUsersCurrentPlayback.build().execute()
                val gson = Gson()
                FileUtils.savedContext().printWriter().use {
                    it.println(context.context.uri)
                    it.println(gson.toJson(context.item.toJson()))
                }
                api.wrapped.pauseUsersPlayback().build().execute()
                null
            }
            is ResumeListeningOn -> {
                if (!FileUtils.savedContext().exists()) {
                    null
                } else {
                    val lines = FileUtils.savedContext().readLines()
                    deviceNameToId(command.deviceName)
                            ?.let { deviceId ->
                                api.wrapped.startResumeUsersPlayback()
                                        .context_uri(lines[0])
                                        .offset(JsonParser().parse(lines[1]).asJsonObject)
                                        .device_id(deviceId)
                            }
                }
            }
            else -> null
        }
                ?.build()
                ?.execute()
        return model
    }

    private fun deviceNameToId(deviceName: String): String? {
        val devices = api.wrapped.usersAvailableDevices
                .build()
                .execute()
                .let { getAndCacheDevices(it) }
        val deviceId = devices[deviceName.trim().toLowerCase()]
        return deviceId
    }

    private fun getAndCacheDevices(newDevices: Array<Device>): Map<String, String> {
        val mostRecentDevices = cachedDevices() + newDevices(newDevices)
        persistDevices(mostRecentDevices)
        return mostRecentDevices
    }

    private fun cachedDevices(): Map<String, String> {
        val cache = FileUtils.deviceCache()
        if (!cache.exists()) {
            return emptyMap()
        }
        return cache.readLines()
                .associate { it.substringBefore("=") to it.substringAfter("=") }
    }

    private fun newDevices(response: Array<Device>): Map<String, String> {
        return response.associate { it.name.trim().toLowerCase() to it.id }
    }

    private fun persistDevices(devices: Map<String, String>) {
        FileUtils.deviceCache().printWriter().use { pw ->
            devices.forEach {
                pw.println("${it.key}=${it.value}")
            }
        }
    }

}