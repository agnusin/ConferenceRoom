package agnusin.com.conferenceroom.ui.conference

import agnusin.com.conferenceroom.domain.model.Conference
import agnusin.com.conferenceroom.ui.widgets.roomview.RoomView
import androidx.databinding.BindingAdapter

@BindingAdapter("data")
fun RoomView.bindData(data: Conference?) {
    with(adapter) {
        if (this is ParticipantsAdapter) {
            data?.let {
                this.setData(it.participants)
            }
        }
    }
}