package agnusin.com.conferenceroom.ui.widgets.roomview

abstract class Adapter<out I : RoomView.RoomItem> {

    interface ChangingObserver {

        fun notifyDataChanged()

        fun notifyItemAdd(position: Int)

        fun notifyItemRemoved(position: Int)
    }

    private var observers = ArrayList<ChangingObserver>()

    fun addChangingObserver(o: ChangingObserver) {
        observers.add(o)
    }

    fun notifyAddItem(position: Int) {
        observers.forEach { it.notifyItemAdd(position) }
    }

    fun notifyRemoveItem(position: Int) {
        observers.forEach { it.notifyItemRemoved(position) }
    }

    fun notifyDataChanged() {
        observers.forEach { it.notifyDataChanged() }
    }

    abstract fun getCount(): Int

    abstract fun createRoomItem(position: Int): I
}