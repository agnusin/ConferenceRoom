package agnusin.com.conferenceroom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.koin.android.ext.android.inject

class MainActivity : PermissionDelegate.PermissionActivity() {

    override val permissionDelegate: PermissionDelegate by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle(R.string.empty)

       /* val streamServer = object : StreamServer {
            override fun getFrame(width: Int, height: Int): Bitmap {
                val colors = IntArray(width * height)
                    .apply {
                        fill(Color.RED, 0, size)
                    }

                return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
            }
        }*/
    }
}
