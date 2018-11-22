package auth

import com.github.salomonbrys.kodein.Kodein
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials
import commands.BasicHandlers
import commands.ContextHandlers
import commands.PlayAlbumFromCurrentSong
import commands.PreviousContext
import kotlinx.coroutines.experimental.runBlocking
import model.Model
import model.RegularContext
import org.apache.http.impl.cookie.BasicCommentHandler
import utils.FileUtils
import utils.toJsonArray
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class Login {
    val permissions = listOf(
            "playlist-read-private", "playlist-read-collaborative", "playlist-modify-public",
            "playlist-modify-private", "streaming", "ugc-image-upload", "user-follow-modify",
            "user-follow-read", "user-library-read", "user-library-modify", "user-top-read",
            "user-read-playback-state", "user-modify-playback-state", "user-read-currently-playing",
            "user-read-recently-played"
    )
    val clientId = "b60bfeb9c4b64a68b369763769d120ee"
    val clientSecret = "31278e818d5a4dc9b86f2d08e26168f8"
    val redirectUrl = URI("http://localhost:51123/callback")
    private val spotifyApi = SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRedirectUri(redirectUrl).build()
    private var authenticatedUntil: Long = 0
    private var refreshToken: String? = null


    private fun refreshToken(): String? {
        if (refreshToken != null) {
            return refreshToken
        }
        return FileUtils.refreshToken()
                .takeIf { it.exists() }
                ?.readText()

    }

    private fun authenticateWithoutRefreshToken() {
        val url = spotifyApi.authorizationCodeUri()
                .scope(permissions.joinToString(" "))
                .show_dialog(true)
                .build()
                .execute()
        val accepter = RedirectAccepter()
        try {
            Desktop.getDesktop().browse(url)
        } catch (e: Exception) {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url.toString()))
        }
        val code = runBlocking { accepter.listenForCode() }
        val authorizationCodeRequest = spotifyApi.authorizationCode(code).build()
        val authorizationCodeCredentials = authorizationCodeRequest.execute();
        saveAuthorizationState(authorizationCodeCredentials)
    }

    private fun saveAuthorizationState(authorizationCodeCredentials: AuthorizationCodeCredentials) {
        authenticatedUntil = System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(authorizationCodeCredentials.expiresIn.toLong())
        spotifyApi.accessToken = authorizationCodeCredentials.accessToken;
        spotifyApi.refreshToken = authorizationCodeCredentials.refreshToken;
        authorizationCodeCredentials.refreshToken?.also(this::cacheRefreshToken)
    }

    private fun cacheRefreshToken(token: String) {
        refreshToken = token
        FileUtils.refreshToken()
                .also {
                    if (!it.exists()) {
                        it.parentFile.mkdirs()
                        it.createNewFile()
                    }
                }
                .writeText(token)
    }

    private fun authenticateWithRefreshToken(token: String) {
        spotifyApi.refreshToken = token
        val authorizationCodeCredentials = spotifyApi.authorizationCodeRefresh().build()
                .execute()
        saveAuthorizationState(authorizationCodeCredentials)
    }

    private fun authenticate() {
        val refreshToken = refreshToken()
        when (refreshToken) {
            null -> authenticateWithoutRefreshToken()
            else -> authenticateWithRefreshToken(refreshToken)
        }
    }

    fun createAuthenticatedApi(): AuthenticatedSpotifyApi {
        return object : AuthenticatedSpotifyApi {
            override val wrapped: SpotifyApi
                get() {
                    if (authenticatedUntil <= System.currentTimeMillis()) {
                        authenticate()
                    }
                    return spotifyApi
                }
        }
    }
}

fun main(args: Array<String>) {
    val spotifyApi = Login().createAuthenticatedApi()
    val handler = BasicHandlers(spotifyApi)
//    handler.radioFromCurrentSong()
//            .build().execute()
    val handler2 = ContextHandlers(spotifyApi)
    val model = Model(mutableListOf(), RegularContext(spotifyApi.wrapped.informationAboutUsersCurrentPlayback.build().execute()))
    handler2.handle(PlayAlbumFromCurrentSong(), model)
    handler2.handle(PreviousContext(),model)
    return
    spotifyApi.wrapped.informationAboutUsersCurrentPlayback
            .build()
            .execute()
            .also { println(it) }
            .also { println(it.device.name) }
    return
    spotifyApi.wrapped.usersAvailableDevices.build().execute().forEach {
        println("Id ${it.id} name ${it.name}")
    }
    val topTracks = spotifyApi.wrapped.usersTopTracks.limit(30).offset(20).time_range("short_term").build().execute().items
    spotifyApi.wrapped.startResumeUsersPlayback()

            .uris(topTracks.toJsonArray())
//            .context_uri("spotify:album:5zT1JLIj9E57p3e1rFm9Uq")
            .build().execute()

}