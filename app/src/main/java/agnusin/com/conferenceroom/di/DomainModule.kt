package agnusin.com.conferenceroom.di

import agnusin.com.conferenceroom.domain.interactors.ConferenceInteractor
import org.koin.dsl.module

val domainModule = module {
    factory { ConferenceInteractor(get()) }
}