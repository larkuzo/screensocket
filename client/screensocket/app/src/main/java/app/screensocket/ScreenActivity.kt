package app.screensocket

import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_screen.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class ScreenActivity : AppCompatActivity() {

    private var websocket: WebSocket? = null
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar?.hide()
        setContentView(R.layout.activity_screen)

        val url = intent.getStringExtra("url")
        connectToServer("ws://$url")
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
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            handler.post {
                screen.stop()
                finish()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            handler.post {
                screen.stop()
                finish()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            handler.post {
                screen.stop()
                websocket?.close(1000, null)
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
            webSocket.send("OK")
			screen.updateImage(bytes.toByteArray())
            // screen.updateArray(bytes.toByteArray())
        }
    }

    override fun onBackPressed() {
        screen.stop()
        websocket?.close(1000, null)
        super.onBackPressed()
    }
}
