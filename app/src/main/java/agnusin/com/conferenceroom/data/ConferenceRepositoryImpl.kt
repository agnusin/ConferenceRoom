package agnusin.com.conferenceroom.data

import agnusin.com.conferenceroom.domain.model.Conference
import agnusin.com.conferenceroom.domain.model.Participant
import agnusin.com.conferenceroom.domain.model.StreamServer
import agnusin.com.conferenceroom.domain.repositories.ConferenceRepository
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class ConferenceRepositoryImpl(
    private val cameraStreamServer: StreamServer,
    private val remoteStreamServer: StreamServer
): ConferenceRepository {

    private val operationRandom = Random(1000)
    private val countRandom = Random(2000)

    private enum class Operation(var count: Int = 0) {
        ADD(),
        REMOVE()
    }

    override fun getConference(id: Int): Flow<Conference?> = flow {
        var conference = getInitialConference()
        emit(conference)
        while(true) {
            delay(3000)
            conference = nextOperation().invoke(conference)
            emit(conference)
        }
    }

    private fun getInitialConference() = Conference(
        1,
        "Test conference",
        (0 until INIT_PARTICIPANTS_IN_CONF).toList()
            .map {
                Participant.RemoteCaller(getId(), "", remoteStreamServer)
            } +
            Participant.DeviceOwner(getId(), "", cameraStreamServer)
    )

    private fun nextOperation(): Operation =
        when (operationRandom.nextInt(0, 200) % 2) {
            0 -> Operation.ADD
            else -> Operation.REMOVE
        }
            .apply {
                count =  if (MAX_CHANGE_PARTICIPANTS_IN_CONF > 1) countRandom.nextInt(1, MAX_CHANGE_PARTICIPANTS_IN_CONF) else 1
            }

    private fun Operation.invoke(conference: Conference): Conference {
        val participants = conference.participants.toMutableList()
        when (this) {
            Operation.ADD -> {
                repeat(count) {
                    if (participants.size < MAX_PARTICIPANTS_IN_CONF) {
                        participants.add(Participant.RemoteCaller(getId(), "", remoteStreamServer))
                    }
                }
            }
            Operation.REMOVE -> {
                val indexRandom = Random(System.currentTimeMillis())
                repeat(count) {
                    if (participants.size > 1) {
                        val index = indexRandom.nextInt(0, participants.size - 1)
                        if (participants[index] !is Participant.DeviceOwner) {
                            participants.removeAt(index)
                        }
                    }
                }
            }
        }
        return conference.copy(
            participants = participants
        )
    }

    companion object {

        private const val MAX_CHANGE_PARTICIPANTS_IN_CONF = 5
        private const val INIT_PARTICIPANTS_IN_CONF = 10
        private const val MAX_PARTICIPANTS_IN_CONF = 21

        private var idCounter = 0

        fun getId(): Int = ++idCounter
    }
}