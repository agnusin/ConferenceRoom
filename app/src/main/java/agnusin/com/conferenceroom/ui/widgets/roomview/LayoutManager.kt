package agnusin.com.conferenceroom.ui.widgets.roomview

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.View.MeasureSpec
import android.view.animation.BounceInterpolator
import android.view.animation.Interpolator
import kotlin.math.abs

abstract class LayoutManager(
    val size: Int = 10
) {

    private lateinit var roomView: RoomView

    protected var width: Int = 0
    protected var height: Int = 0

    protected var containers = ArrayList<ItemContainer?>(size)

    var translateInterpolator: Interpolator = BounceInterpolator()
    var translateDuration: Long = 700L

    abstract fun createItemRect(): ItemContainer

    open fun handleTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    fun setRoomView(view: RoomView) {
        roomView = view
        width = roomView.width
        height = roomView.height
    }

    fun setMeasureSpecs(wSpec: Int, hSpec: Int) {
        width = MeasureSpec.getSize(wSpec)
        height = MeasureSpec.getSize(hSpec)
    }

    fun translateItem(x: Int, y: Int, position: Int) {

        val container = synchronized(containers) {
            if (position < containers.size) {
                containers[position]
            } else null
        }

        if (container != null) {
            val oldRect = container.getRect()
            val tRect = Rect(
                    x - (oldRect.width() / 2),
                    y - (oldRect.height() / 2),
                    x + (oldRect.width() / 2),
                    y + (oldRect.width() / 2)
            )
            val newRect = if (tRect.inBounds()) tRect
            else fitRectToBounds(tRect)
            container.translate(newRect.centerX(), newRect.centerY())
        }
    }

    fun removeItem(position: Int) {
        synchronized(containers) {
            if (position < containers.size) {
                containers[position]?.destroy()
                containers.removeAt(position)
            }
        }
    }

    fun getItemRect(position: Int): Rect? =
        synchronized(containers) {
            if (position < containers.size) {
                containers[position]?.getRect()
                    ?: createItemRect()
                        .also {
                            containers[position] = it
                        }
                        .getRect()
            } else {
                containers = arrayListOf(
                    *Array(position + 1) { index -> if (index < containers.size) containers[index] else null }
                )

                createItemRect()
                    .also { containers[position] = it }
                    .getRect()
            }
        }

    fun clear() {
        synchronized(containers) {
            for (c in containers) {
                c?.destroy()
            }
            containers.clear()
        }
    }

    private fun fitRectToBounds(rect: Rect): Rect {
        var fitRect = rect
        do {
            fitRect = when {
                fitRect.left < 0 -> Rect(0, fitRect.top, fitRect.right + abs(fitRect.left), fitRect.bottom)
                fitRect.top < 0 -> Rect(fitRect.left, 0, fitRect.right, fitRect.bottom + abs(fitRect.top))
                fitRect.right > width -> Rect(fitRect.left - (fitRect.right - width), fitRect.top, width, fitRect.bottom)
                fitRect.bottom > height -> Rect(fitRect.left, fitRect.top - (fitRect.bottom - height), fitRect.right, height)
                else -> fitRect
            }
        } while (!fitRect.inBounds())
        return fitRect
    }

    private fun Rect.inBounds(): Boolean =
        top >= 0 && left >= 0 && right <= width && bottom <= height

    inner class ItemContainer(
        private val rect: Rect
    ) {

        private var animator: Animator? = null

        fun centerX(): Int = getRect().centerX()

        fun centerY(): Int = getRect().centerY()

        fun getRect(): Rect =
            synchronized(rect) {
                rect
            }

        fun translate(x: Int, y: Int) {
            roomView.handler.post {
                animator?.cancel()
                animator = ValueAnimator()
                    .apply {
                        setValues(
                                PropertyValuesHolder.ofInt("x", centerX(), x),
                                PropertyValuesHolder.ofInt("y", centerY(), y)
                        )
                        duration = this@LayoutManager.translateDuration
                        interpolator = this@LayoutManager.translateInterpolator
                        addUpdateListener(createTranslationListener())
                    }
                animator?.start()
            }
        }

        fun destroy() {
            roomView.handler.post { animator?.cancel() }
        }

        private fun createTranslationListener() = ValueAnimator.AnimatorUpdateListener { anim ->
            val x = anim.getAnimatedValue("x") as Int
            val y = anim.getAnimatedValue("y") as Int
            synchronized(rect) {
                val widthHalf = rect.width() / 2
                val heightHalf = rect.height() / 2
                with(rect) {
                    top = y - heightHalf
                    left = x - widthHalf
                    right = x + widthHalf
                    bottom = y + heightHalf
                }
            }
        }

    }
}