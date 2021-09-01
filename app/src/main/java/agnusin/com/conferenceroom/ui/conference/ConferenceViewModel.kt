package agnusin.com.conferenceroom.ui.conference

import agnusin.com.conferenceroom.domain.interactors.ConferenceInteractor
import agnusin.com.conferenceroom.domain.model.Conference
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ConferenceViewModel(
    private val conferenceInteractor: ConferenceInteractor
): ViewModel(), LifecycleObserver {

    private var requestJob: Job? = null

    private var _conference = MutableLiveData<Conference>()
    val conference: LiveData<Conference> = _conference

    fun requestConference(id: Int) {
        viewModelScope.launch {
            conferenceInteractor
                .getConference(id)
                .collect { conf ->
                    _conference.value = conf
                }
        }
    }
}