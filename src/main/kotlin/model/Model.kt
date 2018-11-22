package model

import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext
import com.wrapper.spotify.model_objects.specification.Album
import com.wrapper.spotify.model_objects.specification.AlbumSimplified
import com.wrapper.spotify.model_objects.specification.TrackSimplified
import java.util.*
import kotlin.math.max
import kotlin.math.min

data class Model(
        var contextHistory: MutableList<Context> = mutableListOf<Context>(),
        var currentContext: Context
)

sealed class Context(
        open var context: CurrentlyPlayingContext?
)

data class RegularContext(override var context: CurrentlyPlayingContext?) : Context(context)

abstract class AlbumContext(override var context: CurrentlyPlayingContext?) : Context(context) {

    abstract fun currentAlbum(): AlbumSimplified
    abstract fun seekNext()
    abstract fun seekPrevious()
}

class BasicAlbumContext(private val albums: Iterable<AlbumSimplified>, override var context: CurrentlyPlayingContext?) : AlbumContext(context) {

    private var iterator = albums.iterator()
    private var history = mutableListOf(iterator.next())
    private var index = 0

    override fun currentAlbum(): AlbumSimplified = history[index]

    override fun seekNext() {
        if (index < (history.size - 1)) {
            index += 1
            return
        }
        if (iterator.hasNext()) {
            history.add(iterator.next())
            index += 1
        }
    }

    override fun seekPrevious() {
        index = max(0, index - 1)
    }
}

data class QueueContext(
        val tracks: Array<out TrackSimplified>,
        override var context: CurrentlyPlayingContext?
) : Context(context) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueueContext

        if (!Arrays.equals(tracks, other.tracks)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(tracks)
    }
}