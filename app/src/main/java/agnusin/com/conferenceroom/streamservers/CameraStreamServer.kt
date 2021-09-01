package agnusin.com.conferenceroom.streamservers

import agnusin.com.conferenceroom.PermissionDelegate
import agnusin.com.conferenceroom.domain.model.StreamServer
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


class CameraStreamServer(
    context: Context,
    private val permissionDelegate: PermissionDelegate
) : StreamServer, LifecycleObserver {

    private val permissions = arrayOf(
        Manifest.permission.CAMERA
    )

    private val handler = Handler(Looper.getMainLooper())

    private val cameraManager =
        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraScope: CoroutineScope? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var defaultFrame: Bitmap? = null
    private var streamFrame: Bitmap? = null
    private var frameSize = Size(0, 0)
    private var rotationMatrix = Matrix()
    private val cameraOrientationHelper = CameraOrientationHelper(context)

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onOpenStream() {
        cameraScope = CoroutineScope(Dispatchers.Default)
            .also {
                it.launch {
                    initFrontCamera()
                }
            }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onCloseStream() {
       close()
    }

    override fun getFrame(width: Int, height: Int): Bitmap {
        if (width > frameSize.width || height > frameSize.height) {
            frameSize = Size(width, height)
            defaultFrame = createDefaultFrame(frameSize)
            configStreamFrames()
        }

        return streamFrame ?: defaultFrame!!
    }

    @SuppressLint("MissingPermission")
    private suspend fun initFrontCamera() {
        for (cameraId in cameraManager.cameraIdList) {
            val cc = cameraManager.getCameraCharacteristics(cameraId)
            val faceing = cc.get(CameraCharacteristics.LENS_FACING)
            if (faceing == CameraCharacteristics.LENS_FACING_FRONT) {
                if (permissionDelegate.checkAndRequestPermissions(*permissions)) {
                    try {
                        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                            override fun onOpened(camera: CameraDevice) {
                                cameraDevice = camera
                                configStreamFrames()
                            }

                            override fun onClosed(camera: CameraDevice) {
                                super.onClosed(camera)
                                close()
                            }

                            override fun onDisconnected(camera: CameraDevice) {
                                streamFrame = null
                                close()
                            }

                            override fun onError(camera: CameraDevice, error: Int) {
                                Log.w(TAG, "Camera open error - $error")
                                close()
                            }
                        }, handler)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun configStreamFrames() {
        val onFrameAvailableListener = ImageReader.OnImageAvailableListener { reader ->
            reader?.acquireLatestImage()?.use { image ->
                image.planes?.get(0)
                    ?.let { plane ->
                        val buffer: ByteBuffer = plane.buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)

                        val nextFrame = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                            .rotateAndClear(rotationMatrix)
                            .cropAndClear(frameSize)
                        streamFrame = nextFrame
                    }
            }
        }

        val cameraId =
            cameraDevice?.id
                ?.let {
                    if (frameSize.width == 0 || frameSize.height == 0) null else it
                } ?: return
        val map: StreamConfigurationMap = cameraManager.getCameraCharacteristics(cameraId).get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        ) ?: return

        val fitSize = map.getOutputSizes(ImageFormat.JPEG)
            .asSequence()
            .map { if ((it.width * it.height) - (frameSize.width * frameSize.height) > 0) it else null }
            .filterNotNull()
            .minBy {
                it.width * it.height
            } ?: frameSize

        imageReader = ImageReader.newInstance(fitSize.width, fitSize.height, ImageFormat.JPEG, 30)
            .apply {
                setOnImageAvailableListener(onFrameAvailableListener, null)

                val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    ?.apply {
                        addTarget(surface)
                    }

                if (previewRequestBuilder != null) {
                    cameraDevice?.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigureFailed(session: CameraCaptureSession) {}

                            override fun onConfigured(session: CameraCaptureSession) {
                                captureSession = session
                                try {
                                    previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                    )

                                    val previewRequest = previewRequestBuilder.build()
                                    captureSession?.setRepeatingRequest(
                                        previewRequest,
                                        null, null
                                    )
                                } catch (e: CameraAccessException) {
                                    e.printStackTrace()
                                }
                            }
                        }, null
                    )

                    val rotationAngle = cameraOrientationHelper.getJpegOrientation(cameraDevice?.id)
                    rotationMatrix.setRotate(rotationAngle)
                }
            }
    }

    private fun createDefaultFrame(size: Size): Bitmap =
        IntArray(size.width * size.height)
            .apply {
                fill(Color.BLACK, 0, this.size)
            }
            .let { colors ->
                Bitmap.createBitmap(colors, size.width, size.height, Bitmap.Config.ARGB_8888)
            }

    private fun close() {
        imageReader?.close()
        imageReader = null
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        cameraScope?.cancel()
    }

    class CameraOrientationHelper(context: Context) {
        private val ORIENTATIONS = SparseIntArray()
            .apply {
                append(Surface.ROTATION_0, 0)
                append(Surface.ROTATION_90, 90)
                append(Surface.ROTATION_180, 180)
                append(Surface.ROTATION_270, 270)
            }

        private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager


        fun getJpegOrientation(cameraId: String?): Float {
            if (cameraId == null) return 0f

            var deviceOrientation = windowManager.defaultDisplay.rotation
            val sensorOrientation = cameraManager.getCameraCharacteristics(cameraId)[CameraCharacteristics.SENSOR_ORIENTATION]
            if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN || sensorOrientation == null) return 0f
            else deviceOrientation = ORIENTATIONS[deviceOrientation]

            return (sensorOrientation - (-deviceOrientation) + 360) % 360f
        }

    }

    companion object {

        const val TAG = "[CameraStreamServer]"
    }
}