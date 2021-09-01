package agnusin.com.conferenceroom.domain.repositories

import agnusin.com.conferenceroom.domain.model.Conference
import kotlinx.coroutines.flow.Flow

interface ConferenceRepository {

    fun getConference(id: Int): Flow<Conference?>
}