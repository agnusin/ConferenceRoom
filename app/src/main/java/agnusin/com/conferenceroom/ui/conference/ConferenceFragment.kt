package agnusin.com.conferenceroom.ui.conference

import agnusin.com.conferenceroom.databinding.FragmentConferenceBinding
import agnusin.com.conferenceroom.domain.model.StreamServer
import agnusin.com.conferenceroom.streamservers.CameraStreamServer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.qualifier.named

class ConferenceFragment: Fragment() {

    private val conferenceViewModel: ConferenceViewModel by viewModel()
    private val cameraStreamServer by inject<StreamServer>(named("cameraStreamServer"))

    val args: ConferenceFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentConferenceBinding.inflate(inflater, container, false)
            .also {
                (requireActivity() as? AppCompatActivity)?.setSupportActionBar(it.toolbar)
                it.roomView.setLayoutManager(RandomLayoutManager())
                it.roomView.adapter = ParticipantsAdapter()
                it.lifecycleOwner = viewLifecycleOwner
                it.conferenceVM = conferenceViewModel

                conferenceViewModel.conference.observe(viewLifecycleOwner, Observer { conf ->
                    (requireActivity() as? AppCompatActivity)?.title = conf.name
                })
            }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            conferenceViewModel.requestConference(args.conferenceId)
        }

        viewLifecycleOwner.lifecycle.addObserver(cameraStreamServer as CameraStreamServer)
    }
}