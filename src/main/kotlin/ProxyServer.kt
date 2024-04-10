import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class ProxyServer(private val target: String, targetPort: Int, port: Int = targetPort) : Closeable {
    init {
        require(targetPort >= 0)
        require(port >= 0)
    }
    private val serverSocket = ServerSocket(port)
    private val socket = Socket(target, targetPort)

    suspend fun start() = coroutineScope {
        if (socket.isClosed) return@coroutineScope
        println("Server is running on port ${serverSocket.localPort}")
        val clientSocket = serverSocket.accept()
        println("Connected ${clientSocket.inetAddress}:${clientSocket.port}")
        launch(Dispatchers.IO) {
            BufferedReader(InputStreamReader(clientSocket.inputStream)).use { clientReader ->
                PrintWriter(OutputStreamWriter(clientSocket.outputStream), true).use { clientWriter ->
                    BufferedReader(InputStreamReader(socket.inputStream)).use { remoteReader ->
                        PrintWriter(OutputStreamWriter(socket.outputStream), true).use { remoteWriter ->
                            val clientRequest = clientReader.readLine() ?: return@launch
                            println("Client request is $clientRequest")
                            val cacheDirectory = File(
                                javaClass.classLoader.getResource("fileForParentPath")?.toURI() ?: return@launch
                            ).parentFile
                            val requestedFile = File(cacheDirectory, clientRequest.split("\\s+".toRegex())[1])
                            if (requestedFile.isFile) {
                                println("$requestedFile in cache")
                                println("$requestedFile returning directly")
                                clientWriter.println(makeFileResponse(requestedFile.readText()))
                            } else {
                                println("$requestedFile !in cache")
                                println("Asking $target:${socket.port} for index.html")
                                remoteWriter.println(clientRequest)
                                val content = remoteReader.readLines().dropWhile { it.isNotBlank() }.joinToString("\n")
                                requestedFile.writeText(content)
                                println("$requestedFile saved in cache")
                                clientWriter.println(makeFileResponse(content))
                            }
                        }
                    }
                }
            }
        }
    }
    private fun makeFileResponse(content: String) = """
HTTP/1.1 200 OK

$content
""".trimIndent()

    override fun close() {
        if (!socket.isClosed) socket.close()
        if (!serverSocket.isClosed) serverSocket.close()
    }
}
