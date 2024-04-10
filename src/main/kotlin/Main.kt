import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

fun main(args: Array<String>) {
    run(arrayOf("google.com", "80", "56666", "true"))
}

fun run(args: Array<String>) {
    val remoteServerPort = args.getOrNull(1)?.toInt()
        ?: throw IllegalArgumentException("\"remote server port\" in second arg expected")
    if (remoteServerPort !in 0..65535) throw IllegalStateException("Server port $remoteServerPort should be in range in 0..65535")
    val proxyServerPort = args.getOrNull(2)?.toInt()
        ?: throw IllegalArgumentException("Please provide the proxy server port as the third argument")
    if (proxyServerPort !in 0..65535) throw IllegalStateException("Proxy port $proxyServerPort should be in range in 0..65535")
    runBlocking {
        launch {
            withContext(Dispatchers.IO) {
                ProxyServer(args.getOrNull(0) ?: throw IllegalArgumentException("\"remote server ip\" in first arg expected"), remoteServerPort, proxyServerPort).start()
            }
        }
        launch {
            if (args.getOrNull(3)?.toBoolean() != true) return@launch
            delay(100)
            println("Attempt to GET /index.html")
            val socket = Socket("127.0.0.1", proxyServerPort)
            val writer = PrintWriter(OutputStreamWriter(socket.outputStream), true)
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            writer.println("GET /index.html")
            println("Answer from server received")
            reader.readLines().joinToString("\n").also { println(it) }
        }
    }
}