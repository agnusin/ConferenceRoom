package agnusin.com.conferenceroom.streamservers

import agnusin.com.conferenceroom.domain.model.StreamServer
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.*
import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.renderscript.RenderScript


class VideoStreamServer(
    context: Context
): StreamServer {

    private var defaultFrame: Bitmap? = null
    private var streamFrame: Bitmap? = null
    private var sizeFrame = Size(0, 0)

    private val handler = Handler(Looper.getMainLooper())
    private val renderScript = RenderScript.create(context)

    private val mediaPlayer = MediaPlayer()
        .apply {
            val afd: AssetFileDescriptor = context.assets.openFd("video.mp4")
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }
    private var imageReader: ImageReader? = null

    override fun getFrame(width: Int, height: Int): Bitmap {
        if (width > sizeFrame.width || height > sizeFrame.height) {
            sizeFrame = Size(width, height)
            defaultFrame = createDefaultFrame(sizeFrame)

            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            configStreamFrames(sizeFrame)
            mediaPlayer.start()
        }
        return streamFrame ?: defaultFrame!!
    }

    private fun configStreamFrames(frameSize: Size) {
        val onFrameAvailableListener = ImageReader.OnImageAvailableListener { reader ->
            reader?.acquireLatestImage()?.use { image ->
                streamFrame = image.YUV_420_888_toRGB(renderScript, frameSize.width, frameSize.height)//NV21toJPEG(bytes, frameSize.width, frameSize.height)
                //val bytes = image.YUV_420_888toNV21_6()
                //streamFrame = NV21toJPEG(bytes, sizeFrame.width, sizeFrame.height)
            }
        }

        imageReader = ImageReader.newInstance(frameSize.width, frameSize.height, ImageFormat.YUV_420_888,30)
            .apply {
                setOnImageAvailableListener(onFrameAvailableListener, handler)
                mediaPlayer.setSurface(surface)
            }
        mediaPlayer.prepare()
    }

    private fun createDefaultFrame(size: Size): Bitmap =
        IntArray(size.width * size.height)
            .apply {
                fill(Color.BLACK, 0, this.size)
            }
            .let { colors ->
                Bitmap.createBitmap(colors, size.width, size.height, Bitmap.Config.ARGB_8888)
            }
}