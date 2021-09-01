package agnusin.com.conferenceroom

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.properties.Delegates

class PermissionDelegate {

    @Suppress("PrivatePropertyName")
    private val REQUEST_CODE = 4000

    private var permissionCoroutine: CancellableContinuation<Boolean>? = null
    private var contextCoroutine: CancellableContinuation<PermissionActivity>? = null

    private var context by Delegates.observable<PermissionActivity?>(null) { _, old, new ->
        if (old == null && new != null) {
            contextCoroutine?.let {
                if (it.isActive) {
                    it.resumeWith(Result.success(new))
                }
            }
        }
    }

    private suspend fun getContext(): PermissionActivity =
        context.let { c ->
            c
                ?: suspendCancellableCoroutine {
                    contextCoroutine = it
                    it.invokeOnCancellation { contextCoroutine = null }
                }
        }

    fun checkPermissions(vararg permissions: String): Boolean =
        context?.run {
            permissions
                .filterNot { p ->
                    ActivityCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
                }
                .isEmpty()
        } ?: throw Exception("There is not context")

    suspend fun checkAndRequestPermissions(vararg permissions: String): Boolean {
        val isAllow = checkPermissionsInCoroutine(*permissions)
        return if (isAllow) true
        else requestPermissions(*permissions)
    }

    private suspend fun checkPermissionsInCoroutine(vararg permissions: String): Boolean =
        permissions
            .filterNot { p ->
                ActivityCompat.checkSelfPermission(
                    getContext(),
                    p
                ) == PackageManager.PERMISSION_GRANTED
            }
            .isEmpty()

    private fun handleRequestResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean =
        if (requestCode == REQUEST_CODE) {
            permissionCoroutine = permissionCoroutine?.let { coroutine ->
                val isAllow =
                    permissions.isNotEmpty() &&
                            grantResults
                                .filterNot { it == PackageManager.PERMISSION_GRANTED }
                                .isEmpty()
                coroutine.finish(isAllow)
                null
            }
            true
        } else false

    private fun bindActivity(activity: PermissionActivity) {
        context = activity
    }

    private fun unbindActivity() {
        context = null
        permissionCoroutine?.cancel()
        contextCoroutine?.cancel()
    }

    private suspend fun requestPermissions(vararg permissions: String): Boolean {
        permissionCoroutine?.cancel()
        return getContext().let {
            suspendCancellableCoroutine { coroutine ->
                permissionCoroutine = coroutine
                coroutine.invokeOnCancellation {
                    permissionCoroutine = null
                }
                ActivityCompat.requestPermissions(it, permissions, REQUEST_CODE)
            }
        }
    }

    private fun CancellableContinuation<Boolean>.finish(result: Boolean? = null) {
        result?.run { resumeWith(Result.success(this)) } ?: cancel()
    }

    abstract class PermissionActivity : AppCompatActivity() {

        protected abstract val permissionDelegate: PermissionDelegate

        override fun onStart() {
            super.onStart()
            permissionDelegate.bindActivity(this)
        }

        override fun onStop() {
            super.onStop()
            permissionDelegate.unbindActivity()
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            if (!permissionDelegate.handleRequestResult(requestCode, permissions, grantResults)) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
}