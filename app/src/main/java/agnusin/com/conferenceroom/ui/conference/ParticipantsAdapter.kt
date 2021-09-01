package agnusin.com.conferenceroom.ui.conference

import agnusin.com.conferenceroom.domain.model.Participant
import agnusin.com.conferenceroom.ui.widgets.roomview.Adapter
import android.graphics.*
import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback

class ParticipantsAdapter : Adapter<ParticipantItem>(), ListUpdateCallback {

    private var data: MutableList<Participant> = mutableListOf()

    fun setData(list: List<Participant>) {
        val oldItems = mutableListOf<Participant>()
        oldItems.addAll(data)
        data.clear()
        data.addAll(list)
        DiffUtil
            .calculateDiff(ParticipantDiffUtilCallback(oldItems, data))
            .dispatchUpdatesTo(this)
    }

    override fun getCount(): Int =
        data.size

    override fun createRoomItem(position: Int): ParticipantItem =
        when (val item = data[position]) {
            is Participant.DeviceOwner -> ParticipantItem.DeviceOwnerItem(item.streamServer)
            is Participant.RemoteCaller -> ParticipantItem.RemoteCallerItem(item.streamServer)
        }

    override fun onInserted(position: Int, count: Int) {
        repeat((0 until count).count()) {
            notifyAddItem(position + it)
        }
    }

    override fun onRemoved(position: Int, count: Int) {
        repeat(count) {
            notifyRemoveItem(position)
        }
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {}

    override fun onMoved(fromPosition: Int, toPosition: Int) {}

    inner class ParticipantDiffUtilCallback(
        private val oldList: List<Participant>,
        private val newList: List<Participant>
    ) : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].isSame(newList[newItemPosition])

        override fun getOldListSize(): Int =
            oldList.size

        override fun getNewListSize(): Int =
            newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].isContentSame(newList[newItemPosition])
    }
}