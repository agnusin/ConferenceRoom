package agnusin.com.conferenceroom

import agnusin.com.conferenceroom.di.appModule
import agnusin.com.conferenceroom.di.domainModule
import agnusin.com.conferenceroom.di.repoModule
import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ConferenceRoomApp: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {

            androidContext(this@ConferenceRoomApp)

            androidLogger()

            modules(
                appModule,
                domainModule,
                repoModule
            )
        }
    }
}