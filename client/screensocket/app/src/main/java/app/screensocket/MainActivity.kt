package app.screensocket

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.NetworkInterface
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {

    private var udpSocket: DatagramSocket? = null
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        udpSocket = DatagramSocket().apply { broadcast = true }
        udpSocket?.soTimeout = 5000

        connect.setOnClickListener {
            Toast.makeText(this@MainActivity, "Connecting...", Toast.LENGTH_SHORT).show()
            connect.isEnabled = false

            // Connect to server
            Thread(Runnable {
                val buffer = ByteArray(480)
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    udpSocket?.receive(packet)
                    val url = String(buffer, 0, packet.length, Charsets.UTF_8)
                    handler.post {
                        connect.isEnabled = true
                        startActivity(Intent(this, ScreenActivity::class.java).apply {
                            putExtra("url", url)
                        })
                    }
                } catch (e: SocketTimeoutException) {
                    handler.post {
                        Toast.makeText(
                            this@MainActivity, "Failed to discover server after 5 seconds",
                            Toast.LENGTH_SHORT
                        ).show()
                        connect.isEnabled = true
                    }
                }
            }).start()

            // Start discovery
            Thread(Runnable { startDiscover() }).start()
        }
    }

    /**
     * Send UDP broadcast to find server
     */
    private fun startDiscover() {
        val ack = String("screensocket".toByteArray(), Charsets.UTF_8).toByteArray()
        NetworkInterface.getNetworkInterfaces().iterator().forEach { iface ->
            iface.interfaceAddresses.forEach { addr ->
                addr.broadcast?.let { udpSocket?.send(DatagramPacket(ack, 0, ack.size, it, 7332)) }
            }
        }
    }
}
