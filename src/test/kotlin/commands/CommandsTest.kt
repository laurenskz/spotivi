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

}