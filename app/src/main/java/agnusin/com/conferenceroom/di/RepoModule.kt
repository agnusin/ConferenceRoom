package agnusin.com.conferenceroom.di

import agnusin.com.conferenceroom.data.ConferenceRepositoryImpl
import agnusin.com.conferenceroom.domain.model.StreamServer
import agnusin.com.conferenceroom.domain.repositories.ConferenceRepository
import agnusin.com.conferenceroom.streamservers.CameraStreamServer
import agnusin.com.conferenceroom.streamservers.VideoStreamServer
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repoModule = module {

    single<StreamServer>(named("cameraStreamServer")) { CameraStreamServer(androidContext(), get()) }
    single<StreamServer>(named("videoStreamServer")) { VideoStreamServer(androidContext()) }

    single<ConferenceRepository> {
        ConferenceRepositoryImpl(
            get(named("cameraStreamServer")),
            get(named("videoStreamServer"))
        )
    }
}