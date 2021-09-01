package agnusin.com.conferenceroom.ui.conference

import agnusin.com.conferenceroom.domain.model.StreamServer
import agnusin.com.conferenceroom.ui.widgets.roomview.RoomView
import android.graphics.*
import android.view.MotionEvent

sealed class ParticipantItem(
    private val streamServer: StreamServer
) : RoomView.RoomItem() {

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val roundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }

    private val borderLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.0f
            color = Color.parseColor("#c5c5c5")
        }

    private val borderFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.STROKE
            strokeWidth = 10.0f
            color = Color.parseColor("#ababab")
        }

    override fun draw(canvas: Canvas, rect: Rect) {
        canvas.drawBitmap(getRoundFrame(rect.width(), rect.height()), null, rect, paint)
    }

    private fun getRoundFrame(width: Int, height: Int): Bitmap {
        val widthBorder = (borderFillPaint.strokeWidth + borderLinePaint.strokeWidth).toInt()
        val sizeBitmap = width + widthBorder * 2
        val roundBitmap = Bitmap.createBitmap(sizeBitmap, sizeBitmap, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(roundBitmap)
            .apply {
                val rect = Rect(widthBorder, widthBorder, width + widthBorder, width + widthBorder)
                drawOval(RectF(rect), paint)
            }
        val originalBitmap = streamServer.getFrame(width, height)
        canvas.drawBitmap(originalBitmap, widthBorder.toFloat(), widthBorder.toFloat(), roundPaint)
        canvas.drawBorder(sizeBitmap / 2.0f, borderLinePaint, borderFillPaint)
        return roundBitmap
    }

    private fun Canvas.drawBorder(radius: Float, paintLine: Paint, paintFill: Paint) {
        drawCircle(radius, radius, radius - paintFill.strokeWidth / 2, paintFill)
        drawCircle(
            radius,
            radius,
            radius - paintLine.strokeWidth / 2 - paintFill.strokeWidth,
            paintLine
        )
    }

    class DeviceOwnerItem(streamServer: StreamServer) : ParticipantItem(streamServer), Moved {

        private var nextPosition: Pair<Int, Int>? = null

        override fun getLayoutLevel(): Int = 1

        override fun getNextPosition(): Pair<Int, Int>? {
            synchronized(this) {
                val temp = nextPosition
                nextPosition = null
                return temp
            }
        }

        override fun handleTouchEvent(event: MotionEvent): Boolean {
            return if (event.action == MotionEvent.ACTION_UP) {
                synchronized(this) {
                    nextPosition = Pair(event.x.toInt(), event.y.toInt())
                }
                true
            } else false
        }
    }

    class RemoteCallerItem(streamServer: StreamServer) : ParticipantItem(streamServer)
}