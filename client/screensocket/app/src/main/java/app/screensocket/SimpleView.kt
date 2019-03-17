package app.screensocket

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil

class SimpleView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    private var ready = false
    private var bitmap: Bitmap? = null
    private val lock = Object()

    private val paint = Paint().apply { flags = Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG }
    private val canvasMatrix = Matrix()

    private var imageWidth = 0
    private var imageHeight = 0

    private var previewWidth = 0
    private var previewHeight = 0

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?)
            : this(context, attrs, 0)

    constructor(context: Context)
            : this(context, null)

    override fun onDraw(canvas: Canvas?) {
        if (!ready) {
            canvas?.apply { drawColor(Color.TRANSPARENT) }
            return
        }

        synchronized(lock) {
            canvas?.apply {
                drawColor(Color.TRANSPARENT)
                drawMatrix(this)
            }
        }
    }

    private fun drawMatrix(canvas: Canvas) {
        bitmap?.let { canvas.drawBitmap(it, canvasMatrix, paint) }
    }

    fun setup(width: Int, height: Int) {
        imageWidth = width
        imageHeight = height

        val (w, h) = findRatio(imageWidth, imageHeight, this.width, this.height)

        previewWidth = w
        previewHeight = h

        // val rw = w / imageWidth.toFloat()
        // val rh = h / imageHeight.toFloat()
        // canvasMatrix.setScale(rw, rh)

//        canvasMatrix.setRotate(90f, 0f, 0f)
//        canvasMatrix.postTranslate(
//            this.width.toFloat() / 2 + h / 2,
//            this.height.toFloat() / 2 - w / 2
//        )

        ready = true
    }

    private fun findRatio(
        imageWidth: Int,
        imageHeight: Int,
        screenWidth: Int,
        screenHeight: Int
    ): Pair<Int, Int> {
        val ratioWidth = screenWidth / imageWidth.toFloat()
        val ratioHeight = screenHeight / imageHeight.toFloat()

        // Expand width first then height
        var newWidth = ratioWidth * imageWidth
        var newHeight = ratioWidth * imageHeight
        if (newHeight > screenHeight) {
            newWidth = ratioHeight * imageWidth
            newHeight = ratioHeight * imageHeight
        }

        return Pair(ceil(newWidth).toInt(), ceil(newHeight).toInt())
    }

    // Create bitmap data from RGB byte array
    fun updateArray(data: ByteArray) {
        synchronized(lock) {
            bitmap = Bitmap.createBitmap(
                data.asBitmapData(),
                imageWidth,
                imageHeight,
                Bitmap.Config.ARGB_8888
            )?.let {
				Bitmap.createScaledBitmap(it, previewWidth, previewHeight, true)
			}
            postInvalidate()
        }
    }

    // Create bitmap data from JPG
    fun updateImage(data: ByteArray) {
        synchronized(lock) {
			// val start = System.nanoTime()
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)?.let {
				Bitmap.createScaledBitmap(it, previewWidth, previewHeight, true)
			}
			// val end = System.nanoTime()
			// log("Elapsed: ${(end - start) / 1000000000.0f} seconds")
            postInvalidate()
        }
    }

    fun stop() {
        ready = false
        postInvalidate()
    }
}
