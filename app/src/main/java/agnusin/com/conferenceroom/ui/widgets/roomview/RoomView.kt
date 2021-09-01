package agnusin.com.conferenceroom.ui.widgets.roomview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.util.forEach
import kotlin.collections.ArrayList


class RoomView @JvmOverloads constructor(
    context: Context,
    val attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Adapter.ChangingObserver {

    init {
        holder.addCallback(this)
    }

    private var viewBackgroundDrawable: Drawable? = null

    private var drawThread: RoomDrawThread? = null
    private lateinit var layoutManager: LayoutManager

    private val pendingItem = SparseArray<RoomItem>()

    var adapter: Adapter<RoomItem>? = null
    set(value) {
        value?.addChangingObserver(this)
        field = value
    }

    private var items: ArrayList<RoomItem> = arrayListOf()

    fun setLayoutManager(lm: LayoutManager) {
        layoutManager = lm
            .apply {
                setRoomView(this@RoomView)
            }
    }

    override fun setBackgroundColor(color: Int) {}

    override fun setBackground(background: Drawable?) {
        viewBackgroundDrawable = background
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            items.any { it.handleTouchEvent(event) } || layoutManager.handleTouchEvent(event)
        }
        return true
    }

    override fun notifyItemAdd(position: Int) {
        synchronized(items) {
            adapter?.createRoomItem(position)
                ?.let { item ->
                    items.add(position, item)
                }
        }
    }

    override fun notifyItemRemoved(position: Int) {
        synchronized(items) {
            items.removeAt(position)
            layoutManager.removeItem(position)
        }
    }

    override fun notifyDataChanged() {
        layoutManager.clear()
        synchronized(items) {
            items = adapter
                ?.let { ad ->
                    arrayListOf(
                        *Array(ad.getCount()) { index ->
                            ad.createRoomItem(index)
                        }
                    )
                }
                ?: arrayListOf()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        layoutManager.setMeasureSpecs(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        viewBackgroundDrawable?.setBounds(0, 0, right - left, bottom - top)
    }


    override fun surfaceCreated(p0: SurfaceHolder?) {
        drawThread = RoomDrawThread(this)
        drawThread?.startRun()
        drawThread?.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {}

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        var retry: Boolean = true
        drawThread?.stopRun()
        while (retry) {
            try {
                drawThread?.join();
                retry = false;
            } catch (e: InterruptedException) {
            }
        }
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if (canvas != null) {
            viewBackgroundDrawable?.draw(canvas)
            synchronized(items) {

                items
                    .forEachIndexed { position, value ->
                        if (value.getLayoutLevel() > 0) {
                            pendingItem.put(position, value)
                        } else {
                            canvas.drawItem(position, value)
                        }
                    }

                pendingItem
                    .forEach { position: Int, roomItem: RoomItem ->
                        canvas.drawItem(position, roomItem)
                    }

                pendingItem.clear()
            }
        }
    }

    private fun Canvas.drawItem(position: Int, item: RoomItem) {
        if (item is RoomItem.Moved) {
            item.getNextPosition()
                ?.let {
                    layoutManager.translateItem(it.first, it.second, position)
                }
        }

        layoutManager.getItemRect(position)
            ?.let { rect ->
                item.draw(this, rect)
            }
    }

    abstract class RoomItem() {

        interface Moved {

            fun getNextPosition(): Pair<Int, Int>?
        }

        val id: Int = getId()

        open fun getLayoutLevel(): Int = 0

        abstract fun draw(canvas: Canvas, rect: Rect)

        open fun handleTouchEvent(event: MotionEvent): Boolean {
            return false
        }

        companion object {

            @Volatile
            var counter = -1

            fun getId(): Int = ++counter
        }
    }
}