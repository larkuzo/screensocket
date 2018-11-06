package app.screensocket

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private var udpSocket: DatagramSocket? = null
    private var websocket: WebSocket? = null
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        udpSocket = DatagramSocket().apply { broadcast = true }

        connect.setOnClickListener {
            Toast.makeText(this@MainActivity, "Connecting...", Toast.LENGTH_SHORT).show()

            Thread(Runnable {
                val buffer = ByteArray(480)
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)

                val url = String(buffer, 0, packet.length, Charsets.UTF_8)
                handler.post { connectToServer("ws://$url") }
            }).start()

            Thread(Runnable { startDiscover() }).start()
        }

        disconnect.setOnClickListener {
            Toast.makeText(this@MainActivity, "Disconnecting...", Toast.LENGTH_SHORT).show()
            websocket?.close(1000, null)

            connect.isEnabled = true
            disconnect.isEnabled = false
        }

        disconnect.isEnabled = false
    }

    private fun startDiscover() {
        val ack = String("screensocket".toByteArray(), Charsets.UTF_8).toByteArray()
        NetworkInterface.getNetworkInterfaces().iterator().forEach { iface ->
            iface.interfaceAddresses.forEach { addr ->
                addr.broadcast?.let { udpSocket?.send(DatagramPacket(ack, 0, ack.size, it, 7332)) }
            }
        }
    }

    private fun connectToServer(url: String) {
        val request = Request.Builder().url(url).build()
        val listener = Client()
        websocket = OkHttpClient().newWebSocket(request, listener)
    }

    inner class Client : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            handler.post {
                Toast.makeText(applicationContext, "Connected", Toast.LENGTH_SHORT).show()
                disconnect.isEnabled = true
                connect.isEnabled = false
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            handler.post {
                Toast.makeText(applicationContext, "Disconnected", Toast.LENGTH_SHORT).show()
                connect.isEnabled = true
                disconnect.isEnabled = false
                screen.stop()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            handler.post {
                screen.stop()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            handler.post {
                Toast.makeText(applicationContext, "Disconnected", Toast.LENGTH_SHORT).show()
                connect.isEnabled = true
                disconnect.isEnabled = false
                screen.stop()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handler.post {
                val dimension = text.split("x")
                val width = dimension[0].toInt()
                val height = dimension[1].toInt()
                screen.setup(width, height)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            // screen.updateArray(bytes.toByteArray())
            screen.updateImage(bytes.toByteArray())
            webSocket.send("OK")
        }
    }
}
