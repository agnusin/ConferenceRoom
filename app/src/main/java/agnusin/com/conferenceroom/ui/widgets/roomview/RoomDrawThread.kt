package agnusin.com.conferenceroom.ui.widgets.roomview

import android.graphics.Canvas

class RoomDrawThread(
    private val view: RoomView
) : Thread() {

    private var isRunning = false
    private var canvas: Canvas? = null

    fun startRun() {
        isRunning = true
    }

    fun stopRun() {
        isRunning = false
    }

    override fun run() {
        super.run()
        while (isRunning) {
            try {
                canvas = with(view.holder) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) lockHardwareCanvas()
                    else lockCanvas()
                }
                canvas?.let {
                    //view.updateObjects()
                    view.draw(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                canvas?.let {
                    view.holder.unlockCanvasAndPost(it)
                }
            }
        }
    }
}