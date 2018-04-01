package com.gmail.uwriegel.superfit.http

import com.gmail.uwriegel.superfit.sensor.ServiceCallback
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset

val UTF8_CHARSET = Charset.forName("UTF-8")

fun startServer(serviceCallback: ServiceCallback) {
    service = serviceCallback
    Thread {
        try {
            server = ServerSocket (9865, 0, InetAddress.getLoopbackAddress())
            while (true) {
                val client = server!!.accept()
                clientConnected(client)
            }
        }
        catch (err: Exception) {}
        return@Thread
    }.start()
}

fun stopServer() {
    server?.close()
    server = null
}

private fun clientConnected(client: Socket) {

//    fun getSendGpsActive(): ()->String {
//        var gpsActiveSent = false
//
//        return {
//            if (sendGpsActive && !gpsActiveSent) {
//                gpsActiveSent = true
//                """"gps": true,
//    """
//            } else ""
//        }
//    }
//
//    val sendGpsActive = getSendGpsActive()

    Thread {
        try {
            val istream = client.getInputStream()
            val buffer = ByteArray(2000)
            istream.read(buffer)
            val header = String(buffer, UTF8_CHARSET)
            val method = checkMethod(header)
            when (method) {
                "check" -> sendResponse(client, """{"connected": true}""")
                "stop" -> {
                    if (service != null) {
                        service!!.stopService()
                        service = null
                    }
                    sendResponse(client, """{"connected": false}""")
                }
                else -> sendResponse(client, """{"error": "method not implemented"}""")
            }
            client.close()
        }
        catch (err: Exception) {}
        return@Thread
    }.start()
}

private fun checkMethod(header: String): String {
    val posEnd = header.indexOf(" ", 5)
    return header.substring(5, posEnd)
}

//val responseBody =
//        """{
//    "heartRate": $heartRate,
//    "speed": $speed,
//    "distance": $distance,
//    "cadence": $cadence,
//    "maxSpeed": $maxSpeed,
//    "timeSpan": $timeSpan,
//    ${sendGpsActive()}"averageSpeed": $averageSpeed
//}"""
//

private fun sendResponse(client: Socket, payload: String) {
    val contentLength = payload.length
    val response = "HTTP/1.1 200 OK\r\nContent-Type: text/json; charset=UTF-8\r\nContent-Length: $contentLength\r\n\r\n$payload"
    val ostream = client.getOutputStream()
    ostream.write(response.toByteArray())
    ostream.flush()
}

private var server: ServerSocket? = null
private var service: ServiceCallback? = null