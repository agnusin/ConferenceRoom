package agnusin.com.conferenceroom.domain.interactors

import agnusin.com.conferenceroom.domain.model.Conference
import agnusin.com.conferenceroom.domain.repositories.ConferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class ConferenceInteractor(
    private val repository: ConferenceRepository
) {

    fun getConference(id: Int): Flow<Conference?> =
        repository.getConference(id)
            .flowOn(Dispatchers.IO)

}