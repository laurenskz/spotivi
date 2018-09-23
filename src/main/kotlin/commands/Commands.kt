package commands

import java.util.ArrayList
import java.util.regex.Pattern

interface CommandHandler {
    fun handle(command: Command)
}

interface CommandProducer {
    var commandSink: (Command) -> Unit
}

object Commands {
    private val STRING_SPLIT_REGEX = Pattern.compile("([^\"]\\S*|\".+?\")\\s*")

    private fun splitOnSpaces(string: String): List<String> {
        val list = mutableListOf<String>()
        val matcher = STRING_SPLIT_REGEX.matcher(string)
        while (matcher.find())
            list.add(matcher.group(1))
        return list.map { it.filter { it != '"' } }
    }

    fun commandFromString(input: String): Command {
        val commands = splitOnSpaces(input)
        return when (commands[0]) {
            "playpause" -> TogglePlayCommand()
            "next-track" -> NextTrackCommand()
            "previous-track" -> PreviousTrackCommand()
            "play-album-current" -> PlayAlbumFromCurrentSong()
            "radio-from-current-song" -> RadioFromCurrentSong()
            "search" -> SearchCommand(SearchCategory.byName(commands[1]), commands[2])
            "start" -> PlayCommand(commands[1], commands[2])
            "up" -> UpCommand()
            "down" -> DownCommand()
            "left" -> LeftCommand()
            "right" -> RightCommand()
            else -> throw IllegalArgumentException("Unsupported command")
        }
    }

}

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

data class SearchCommand(val category: SearchCategory, val query: String) : Command()

data class PlayCommand(val context: String, val offset: String?) : Command()

class TogglePlayCommand : Command()

class UpCommand : Command()
class DownCommand : Command()
class LeftCommand : Command()
class RightCommand : Command()
class NextTrackCommand : Command()
class PreviousTrackCommand : Command()
class PlayAlbumFromCurrentSong : Command()
class RadioFromCurrentSong : Command()
class PauseCurrentSong : Command()