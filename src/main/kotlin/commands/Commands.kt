package commands

import model.Model
import java.util.ArrayList
import java.util.regex.Pattern

interface CommandHandler {
    fun handle(command: Command, model: Model): Model
}

interface CommandProducer {
    var commandSink: (Command) -> Unit
}

object Commands {
    private val STRING_SPLIT_REGEX = Pattern.compile("([^\"]\\S*|\".+?\")\\s*")
    private val COMMAND_SPLIT_REGEX = Pattern.compile("[^\\\\]&")

    private fun splitOnSpaces(string: String): List<String> {
        val list = mutableListOf<String>()
        val matcher = STRING_SPLIT_REGEX.matcher(string)
        while (matcher.find())
            list.add(matcher.group(1))
        return list.map { it.filter { it != '"' } }
    }

    fun commandsSplitOnAmpersand(input: String): List<Command> {
        val commands = input.split(COMMAND_SPLIT_REGEX)
        return commands
                .map { it.replace("\\&", "&") }
                .mapNotNull { commandFromString(it) }
                .map { it }
    }

    fun commandFromString(input: String): Command? {
        val commands = splitOnSpaces(input).map(String::trim)
        return when (commands[0]) {
            "playpause" -> TogglePlayCommand()
            "stop-listening" -> StopListening()
            "resume-listening-on" -> ResumeListeningOn(commands[1])
            "next-track" -> NextTrackCommand()
            "previous-track" -> PreviousTrackCommand()
            "previous-context" -> PreviousContext()
            "next-album" -> NextAlbum()
            "previous-album" -> PreviousAlbum()
            "play-album-current" -> PlayAlbumFromCurrentSong()
            "add-current-to" -> AddCurrentToCommand(commands[1])
            "shuffle" -> ShuffleCommand()
            "radio-from-current-song" -> RadioFromCurrentSong()
            "current-artist-albums" -> CurrentArtistAlbumsCommand()
//            "search" -> SearchCommand(SearchCategory.byName(model[1]), model[2])
            "start" -> PlayCommand(commands[1], commands.getOrNull(2))
            "start-on" -> StartOnCommand(commands[1], commands[2])
            "start-album" -> StartAlbum(commands[1])
            "transfer" -> TransferCommand(commands[1])
            "change-volume" -> ChangeVolume(Integer.parseInt(commands[1]))
            "random-music" -> RandomMusicCommand()
//            "up" -> UpCommand()
//            "down" -> DownCommand()
//            "left" -> LeftCommand()
//            "right" -> RightCommand()
            else -> null
//            else -> throw IllegalArgumentException("Unsupported command")
        }
    }

}

class ShuffleCommand : Command()

sealed class Command


enum class SearchCategory(val externalName: String) {
    ARTIST("artist"),
    PLAYLIST("playlist"),
    TRACK("track"),
    ALBUM("album");

    companion object {
        fun byName(name: String): SearchCategory = values()
                .filter { it.externalName == name }
                .firstOrNull() ?: throw IllegalArgumentException("Unsupported searchcategory")
    }
}

data class TransferCommand(val deviceName: String) : Command()
data class StartOnCommand(val deviceId: String, val contextId: String) : Command()

data class SearchCommand(val category: SearchCategory, val query: String) : Command()

data class PlayCommand(val context: String, val offset: String?) : Command()

class TogglePlayCommand : Command()
data class AddCurrentToCommand(val playlistId: String) : Command()

class UpCommand : Command()
class DownCommand : Command()
class LeftCommand : Command()
class RightCommand : Command()
class NextTrackCommand : Command()
class RandomMusicCommand : Command()
class PreviousTrackCommand : Command()
class PlayAlbumFromCurrentSong : Command()
class CurrentArtistAlbumsCommand : Command()
class RadioFromCurrentSong : Command()
class PreviousContext : Command()
class NextAlbum : Command()
class PreviousAlbum : Command()
class StopListening : Command()
class ResumeListeningOn(val deviceName: String) : Command()
data class ChangeVolume(val change: Int) : Command()
data class StartAlbum(val playlist: String) : Command()