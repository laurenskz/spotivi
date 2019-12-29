package utils

import java.io.File

object FileUtils {
    val homedir = System.getProperty("user.home");

    fun spotiviDir(): String {
        return "$homedir/spotivi"
    }

    fun refreshToken() = File("${spotiviDir()}/refresh.token")
    fun deviceCache() = File("${spotiviDir()}/devices.cache")
    fun savedContext() = File("${spotiviDir()}/context.cache")
}