package commands

import org.junit.Test

import org.junit.Assert.*

class CommandsTest {
    @Test
    fun commandFromString() {
        assertEquals(Commands.commandFromString("search track \"The heart of life\""),
                SearchCommand(SearchCategory.TRACK, "The heart of life"))
        assertEquals(Commands.commandFromString("search track everglow"),
                SearchCommand(SearchCategory.TRACK, "everglow"))

    }

    @Test
    fun multipleCommands() {
        val desired = listOf(
                TransferCommand("lgtv"),
                StartAlbum("5487438")
        )
        val actual = Commands.commandsSplitOnAmpersand("transfer lgtv & start-album 5487438")
        assertEquals(desired, actual)
        assertEquals(
                listOf(TransferCommand("lg&tv"),
                        StartAlbum("5487438")),
                Commands.commandsSplitOnAmpersand("transfer lg\\&tv & start-album 5487438")
        )
    }

}