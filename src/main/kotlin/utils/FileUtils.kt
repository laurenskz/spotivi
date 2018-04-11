package utils

import java.io.File

object FileUtils {
    val homedir = System.getProperty("user.home");

    fun spotiviDir(): String {
        return "$homedir/spotivi"
    }

    fun refreshToken() = File("${spotiviDir()}/refresh.token")
}