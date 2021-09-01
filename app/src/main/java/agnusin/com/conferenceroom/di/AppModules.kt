package agnusin.com.conferenceroom.di

import agnusin.com.conferenceroom.PermissionDelegate
import agnusin.com.conferenceroom.ui.conference.ConferenceViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { PermissionDelegate() }
    viewModel { ConferenceViewModel(get()) }
}
