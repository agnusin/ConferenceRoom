package agnusin.com.conferenceroom.streamservers

import agnusin.com.conferenceroom.ScriptC_yuv420888
import android.graphics.*
import android.media.Image
import android.util.Size
import androidx.renderscript.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import kotlin.experimental.inv
import kotlin.math.max


fun Bitmap.rotateAndClear(matrix: Matrix): Bitmap {
    val btm = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    this.recycle()
    return btm
}

fun Bitmap.cropAndClear(size: Size): Bitmap {
    val x = width / 2 - size.width / 2
    val y = height / 2 - size.height / 2
    val btm = Bitmap.createBitmap(this, x, y, size.width, size.height, null, true)
    this.recycle()
    return btm
}

fun Bitmap.scaleAndClear(size: Size): Bitmap {
    val sideSize = max(size.width, size.height)
    val ratio = width / height.toFloat()
    val (dstWidth, dstHeight) =
    if (width >= height) {
        (sideSize * ratio).toInt() to sideSize
    } else {
        sideSize to (sideSize / ratio).toInt()
    }

    val btm = Bitmap.createScaledBitmap(this, dstWidth, dstHeight, true)
    this.recycle()
    return btm
}

fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int): Bitmap {
    val out = ByteArrayOutputStream()
    val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val bytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun Image.YUV_420_888toNV21_6(): ByteArray {
    val ySize = width * height
    val uvSize = width * height/4

    val nv21 = ByteArray(ySize + uvSize * 2)

    val yBuffer = planes[0].buffer // Y
    val uBuffer = planes[1].buffer // U
    val vBuffer = planes[2].buffer // V

    var rowStride = planes[0].rowStride

    var pos = 0

    if (rowStride == width) { // likely
        yBuffer.get(nv21, 0, ySize);
        pos += ySize;
    }
    else {
        var yBufferPos = width - rowStride; // not an actual position
        while (pos<ySize) {
            yBufferPos += rowStride - width
            yBuffer.position(yBufferPos)
            yBuffer.get(nv21, pos, width)

            pos+=width
        }
    }

    rowStride = planes[2].rowStride
    val pixelStride = planes[2].pixelStride

    if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
        // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
        val savePixel = vBuffer.get(1)
        try {
            vBuffer.put(1, savePixel.inv())
            if (uBuffer.get(0) == savePixel.inv()) {
                vBuffer.put(1, savePixel);
                vBuffer.get(nv21, ySize, uvSize);

                return nv21; // shortcut
            }
        }
        catch (ex: ReadOnlyBufferException) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
        }

            // unfortunately, the check failed. We must save U and V pixel by pixel
        vBuffer.put(1, savePixel);
    }

    // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
    // but performance gain would be less significant

    for (row in 0 until height/2) {
        for (col in 0 until width/2) {
            val vuPos = col * pixelStride + row * rowStride
            nv21[pos++] = vBuffer.get(vuPos)
            nv21[pos++] = uBuffer.get(vuPos)
        }
    }

    return nv21
}

fun Image.YUV_420_888_toRGB(rs: RenderScript, width: Int, height: Int): Bitmap {
    // Get the three image planes
    val planes: Array<Image.Plane> = planes
    var buffer: ByteBuffer = planes[0].buffer
    val y = ByteArray(buffer.remaining())
    buffer.get(y)
    buffer = planes[1].buffer
    val u = ByteArray(buffer.remaining())
    buffer.get(u)
    buffer = planes[2].buffer
    val v = ByteArray(buffer.remaining())
    buffer.get(v)

    // get the relevant RowStrides and PixelStrides
    // (we know from documentation that PixelStride is 1 for y)
    val yRowStride: Int = planes[0].rowStride
    val uvRowStride: Int = planes[1].rowStride // we know from   documentation that RowStride is the same for u and v.
    val uvPixelStride: Int = planes[1].pixelStride // we know from   documentation that PixelStride is the same for u and v.


    val mYuv420 = ScriptC_yuv420888(rs)

    // Y,U,V are defined as global allocations, the out-Allocation is the Bitmap.
    // Note also that uAlloc and vAlloc are 1-dimensional while yAlloc is 2-dimensional.
    val typeUcharY = Type.Builder(rs, Element.U8(rs))
    //typeUcharY.setX(yRowStride).setY(height)
    typeUcharY.setX(yRowStride).setY(y.size / yRowStride)
    val yAlloc = Allocation.createTyped(rs, typeUcharY.create())
    yAlloc.copyFrom(y)
    mYuv420._ypsIn = yAlloc
    val typeUcharUV = Type.Builder(rs, Element.U8(rs))
    // note that the size of the u's and v's are as follows:
    //      (  (width/2)*PixelStride + padding  ) * (height/2)
    // =    (RowStride                          ) * (height/2)
    // but I noted that on the S7 it is 1 less...
    typeUcharUV.setX(u.size)
    val uAlloc = Allocation.createTyped(rs, typeUcharUV.create())
    uAlloc.copyFrom(u)
    mYuv420._uIn = uAlloc
    val vAlloc = Allocation.createTyped(rs, typeUcharUV.create())
    vAlloc.copyFrom(v)
    mYuv420._vIn = vAlloc

    // handover parameters
    mYuv420._picWidth = width.toLong()
    mYuv420._uvRowStride = uvRowStride.toLong()
    mYuv420._uvPixelStride = uvPixelStride.toLong()
    val outBitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )
    val outAlloc = Allocation.createFromBitmap(
        rs,
        outBitmap,
        Allocation.MipmapControl.MIPMAP_NONE,
        Allocation.USAGE_SCRIPT
    )
    val lo = Script.LaunchOptions()
    lo.setX(
        0,
        width
    ) // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
    lo.setY(0, height)
    mYuv420.forEach_doConvert(outAlloc, lo)
    outAlloc.copyTo(outBitmap)
    return outBitmap
}
