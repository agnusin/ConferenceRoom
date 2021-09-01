package agnusin.com.conferenceroom.domain.model

import android.graphics.Bitmap

interface StreamServer {

    fun getFrame(width: Int, height: Int): Bitmap
}