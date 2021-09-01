package agnusin.com.conferenceroom.domain.model

import android.graphics.Bitmap

sealed class Participant(
    val id: ParticipantId,
    val name: String,
    val streamServer: StreamServer
) {

    fun isSame(p: Participant) =
        id == p.id

    fun isContentSame(p: Participant): Boolean {
        return id == p.id && name == p.name
    }

    class DeviceOwner(id: ParticipantId, name: String, streamServer: StreamServer) : Participant(id, name, streamServer)

    class RemoteCaller(id: ParticipantId, name: String, streamServer: StreamServer) : Participant(id, name, streamServer)
}