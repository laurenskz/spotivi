package commands

import auth.AuthenticatedSpotifyApi
import com.wrapper.spotify.model_objects.specification.Playlist
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.requests.IRequest
import com.wrapper.spotify.requests.data.AbstractDataRequest
import com.wrapper.spotify.requests.data.player.StartResumeUsersPlaybackRequest
import model.*
import utils.*
import java.util.*

class ContextHandlers(private val api: AuthenticatedSpotifyApi) : CommandHandler {
    private val random = Random()

    override fun handle(command: Command, model: Model): Model {
        when (command) {
            is PlayAlbumFromCurrentSong -> playAlbumFromCurrentSong(model)
            is RadioFromCurrentSong -> radioFromCurrentSong(model)
            is CurrentArtistAlbumsCommand -> currentArtistAlbums(model)
            is PreviousContext -> previousContext(model)
            is PlayCommand -> playFromContext(model, command.context, command.offset, null)
            is StartAlbum -> startAlbum(model, command.playlist)
            is NextAlbum -> playCurrentAlbumAfter(model) { it.seekNext() }
            is PreviousAlbum -> playCurrentAlbumAfter(model) { it.seekPrevious() }
            else -> null
        }?.build()?.execute<Any>()
        if (command is PreviousContext)
            model.currentContext.context?.progress_ms?.let {
                api.wrapped.seekToPositionInCurrentlyPlayingTrack(it)
            }?.build()?.execute()
        return model
    }

    private fun currentArtistAlbums(model: Model): IRequest.Builder? {
        currentSong()
                .artists
                .first()
                .let {
                    val context = BasicAlbumContext(artistToAlbums(api, it.id), null)
                    return playFromContext(model, context.currentAlbum().uri, null, context)
                }
    }

    private fun playCurrentAlbumAfter(model: Model, operation: (AlbumContext) -> Unit) =
            (model.currentContext as? AlbumContext)?.also(operation)?.currentAlbum()?.uri?.let {
                api.wrapped.startResumeUsersPlayback().context_uri(it)
            }


    private fun startAlbum(model: Model, playlistId: String): IRequest.Builder? {
        val context = BasicAlbumContext(playlistToAlbums(api, playlistId), null)
        return playFromContext(model, context.currentAlbum().uri, null, context)
    }

    private fun playFromContext(model: Model, context: String, offset: String?, currentContext: Context?): IRequest.Builder? {
        return api.wrapped.startResumeUsersPlayback()
                .context_uri(context)
                .also { builder ->
                    saveCurrentContext(model)
                    model.currentContext = currentContext ?: RegularContext(null)
                    offset?.let { uriToJsonTrack(it) }?.also { builder.offset(it) }
                }
    }

    private fun previousContext(model: Model): IRequest.Builder? {
        if (model.contextHistory.size == 0) return api.wrapped.startResumeUsersPlayback()
        val previousContext = model.contextHistory.removeAt(model.contextHistory.size - 1)
        model.currentContext = previousContext
        return when (previousContext) {
            is RegularContext -> api.wrapped.startResumeUsersPlayback()
                    .context_uri(previousContext.context?.context?.uri)
                    .offset(previousContext.context?.item?.toJson())
            is AlbumContext -> api.wrapped.startResumeUsersPlayback()
                    .context_uri(previousContext.context?.context?.uri)
                    .offset(previousContext.context?.item?.toJson())
            is QueueContext ->
                api.wrapped.startResumeUsersPlayback()
                        .uris(previousContext
                                .tracks
                                .dropWhile { it.uri != previousContext.context?.item?.uri }
                                .toTypedArray()
                                .createJson { it.uri })
        }
    }

    private fun currentSong(): Track = api
            .wrapped
            .usersCurrentlyPlayingTrack.build().execute()
            .item


    private fun radioFromCurrentSong(model: Model): StartResumeUsersPlaybackRequest.Builder = currentSong().let {
        api.wrapped.recommendations.seed_tracks(it.id)
                .build()
                .execute()
                .tracks
                .let {
                    saveCurrentContext(model)
                    model.currentContext = QueueContext(it, null)
                    api.wrapped.startResumeUsersPlayback()
                            .uris(it.createJson { it.uri })
                }
    }

    private fun saveCurrentContext(model: Model) {
        model.currentContext.context = api.wrapped.informationAboutUsersCurrentPlayback.build().execute()
        model.contextHistory.add(model.currentContext)
    }

    private fun playAlbumFromCurrentSong(model: Model): IRequest.Builder? =
            currentSong().let {
                playFromContext(model, it.album.uri, null, null)
            }
}