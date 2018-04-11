package auth

import org.apache.http.client.utils.URLEncodedUtils
import spark.kotlin.ignite
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

class RedirectAccepter(val port: Int = 51123) {

    private var continuation: WrappedContinuation<String>? = null
    private val urlRegex = Pattern.compile("""^GET\s+\/callback\?(.*)\s+HTTP\/1\.1""")

    suspend fun listenForCode(): String = suspendCoroutineW { d ->
        val getRequest = handleRequest()
        val path = urlRegex.matcher(getRequest).let {
            it.find()
            it.toMatchResult().group(1)
        }
        val parsedUrl = URLEncodedUtils.parse(path, StandardCharsets.UTF_8)
        parsedUrl
                .filter { it.name == "code" }
                .firstOrNull()
                ?.also { d.resume(it.value) }
    }

    /**
     * Returns the request that was handled
     */
    private fun handleRequest(): String {
        val server = ServerSocket(port)
        val connection = server.accept()
        val getRequest = connection.getInputStream().bufferedReader()
                .lineSequence()
                .takeWhile { !it.isBlank() }
                .joinToString("\n")
        writeResponse(connection)
        connection.close()
        server.close()
        return getRequest
    }

    private fun writeResponse(connection: Socket) {
        connection.getOutputStream().let { PrintWriter(it) }.also {
            val response = javaClass.getResourceAsStream("response.html")
                    .bufferedReader()
                    .use { it.readText() }
            it.write("HTTP/1.0 200 OK\r\n");
            it.write("Server: Simpleserver/0.1\r\n");
            it.write("Content-Type: text/html\r\n");
            it.write("Content-Length: ${response.length}\r\n");
            it.write("\r\n");
            it.write(response);
            it.flush()
        }
    }
}

class WrappedContinuation<in T>(private val c: Continuation<T>) : Continuation<T> {
    private var isResolved = false
    override val context: CoroutineContext
        get() = c.context

    override fun resume(value: T) {
        if (!isResolved) {
            isResolved = true
            c.resume(value)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        if (!isResolved) {
            isResolved = true
            c.resumeWithException(exception)
        }
    }
}

inline suspend fun <T> suspendCoroutineW(crossinline block: (WrappedContinuation<T>) -> Unit): T =
        suspendCoroutine { c ->
            val wd = WrappedContinuation(c)
            block(wd)
        }
