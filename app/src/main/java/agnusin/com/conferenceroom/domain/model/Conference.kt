package agnusin.com.conferenceroom.domain.model

import java.util.*

data class Conference(
    val id: ConferenceId,
    val name: String,
    val participants: List<Participant>
)