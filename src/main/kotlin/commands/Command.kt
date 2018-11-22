package commands

import auth.AuthenticatedSpotifyApi

abstract class Command2(open val name: String) {

    abstract fun invoke(api: AuthenticatedSpotifyApi, arguments: Array<String>)
}

open class ShorthandCommand(override val name: String, private val action: (AuthenticatedSpotifyApi, Array<String>) -> Unit) : Command2(name) {
    override fun invoke(api: AuthenticatedSpotifyApi, arguments: Array<String>) {
        return action(api, arguments)
    }
}

object NextTrackCommand2 : ShorthandCommand("next-track", { api, arguments ->
    api.wrapped.skipUsersPlaybackToNextTrack()
})
