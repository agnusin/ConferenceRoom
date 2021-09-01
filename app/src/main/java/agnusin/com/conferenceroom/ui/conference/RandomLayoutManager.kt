package agnusin.com.conferenceroom.ui.conference

import agnusin.com.conferenceroom.ui.widgets.roomview.LayoutManager
import android.graphics.Rect
import android.view.MotionEvent
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class RandomLayoutManager : LayoutManager() {

    private val random = Random(System.currentTimeMillis())

    private val sizeRect by lazy { (min(width, height) * 0.2).roundToInt() }
    private val padding by lazy { (min(width, height) * 0.05).roundToInt() }
    private val maxVerticalBound by lazy { height - sizeRect - padding }
    private val maxHorizontalBound by lazy { width - sizeRect - padding }

    override fun createItemRect(): ItemContainer {
        var rect: Rect
        var attempt = 50
        synchronized(containers) {
            do {
                val top = random.nextInt(padding, maxVerticalBound)
                val left = random.nextInt(padding, maxHorizontalBound)
                rect = Rect(left, top, left + sizeRect, top + sizeRect)

                val isIntersectOtherRect = containers
                    .asSequence()
                    .filter { it != null }
                    .firstOrNull { Rect.intersects(it!!.getRect(), rect) } != null

                if (!isIntersectOtherRect) break
                else attempt--
            } while (attempt > 0)
        }
        return ItemContainer(rect)
    }

    override fun handleTouchEvent(event: MotionEvent): Boolean =
        if (event.action == MotionEvent.ACTION_UP) {
            event.x
            true
        } else false
}