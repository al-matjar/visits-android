package com.hypertrack.android.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.util.ClipboardUtil
import com.hypertrack.android.ui.common.util.LocationUtils
import com.hypertrack.android.ui.common.util.isEmail
import com.hypertrack.logistics.android.github.R
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.ZoneId
import java.util.*

interface ResourceProvider {
    fun getQuantityString(@PluralsRes res: Int, number: Int): String
    fun drawableFromResource(@DrawableRes res: Int): Drawable?
    fun stringFromResource(@StringRes res: Int, vararg formatArgs: Any): String
    fun stringFromResource(@StringRes res: Int): String
    fun colorFromResource(@ColorRes res: Int): Int
    fun bitmapFromResource(@DrawableRes resource: Int): Bitmap
    fun bitmapDescriptorFromResource(@DrawableRes res: Int): BitmapDescriptor
    fun bitmapDescriptorFromVectorResource(
        @DrawableRes res: Int,
        @ColorRes color: Int? = null
    ): BitmapDescriptor

    fun getErrorMessage(e: Exception): ErrorMessage
}

public class OsUtilsProvider(
    private val context: Context,
    private val crashReportsProvider: CrashReportsProvider
) : ResourceProvider {

    val screenDensity: Float
        get() = Resources.getSystem().displayMetrics.density

    val cacheDir: File
        get() = MyApplication.context.cacheDir

    fun makeToast(@StringRes txtRes: Int) {
        makeToast(txtRes.stringFromResource())
    }

    fun makeToast(txt: String) {
        Toast.makeText(context, txt, Toast.LENGTH_LONG).show()
    }

    fun isEmail(str: String?): Boolean {
        return str.isEmail()
    }

    fun getTimeZoneId(): ZoneId = ZoneId.systemDefault()

    fun getClipboardContents(): String? {
        val manager =
            MyApplication.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        Log.e(TAG, manager.hasPrimaryClip().toString())
//        Log.e(TAG, manager.primaryClip?.getItemAt(0)?.text.toString())
//        Log.e(TAG, manager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN).toString())
        if (manager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true) {
            return manager.primaryClip?.getItemAt(0)?.text?.toString()
        } else {
            return null
        }
    }

    fun copyToClipboard(str: String) {
        ClipboardUtil.copyToClipboard(str)
    }

    override fun drawableFromResource(@DrawableRes res: Int): Drawable? {
        return ContextCompat.getDrawable(MyApplication.context, res)
    }

    override fun stringFromResource(@StringRes res: Int): String {
        return MyApplication.context.getString(res)
    }

    override fun stringFromResource(@StringRes res: Int, vararg formatArgs: Any): String {
        return MyApplication.context.getString(res, *formatArgs)
    }

    override fun colorFromResource(@ColorRes res: Int): Int {
        return ContextCompat.getColor(context, res)
    }

    override fun bitmapFromResource(@DrawableRes resource: Int): Bitmap {
        return ResourcesCompat.getDrawable(context.resources, resource, context.theme)!!.toBitmap()
    }

    @Throws(IOException::class)
    fun createTakePictureIntent(context: Context, file: File): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .also { takePictureIntent ->
                file.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        context,
                        "com.hypertrack.logistics.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }
            }
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp = "${Date().time}"
        val storageDir: File = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        return bitmap.toBase64()
    }

    fun decodeBase64Bitmap(base64thumbnail: String): Bitmap {
        return base64thumbnail.decodeBase64Bitmap()
    }

    fun decodeBase64(base64: String): String {
        return base64.decodeBase64()
    }

    fun shareText(text: String, title: String? = null) {
        val sharingTitle: String = context.getString(R.string.share_trip_via)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
        sendIntent.type = "text/plain"
        val intent = Intent(SHARE_BROADCAST_ACTION)
        intent.setPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val chooser = Intent.createChooser(sendIntent, sharingTitle, pendingIntent.intentSender)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun getDisplayMetrics(): DisplayMetrics {
        return context.resources.displayMetrics
    }

    override fun bitmapDescriptorFromResource(@DrawableRes res: Int): BitmapDescriptor {
        return BitmapDescriptorFactory.fromResource(res)
    }

    override fun bitmapDescriptorFromVectorResource(
        @DrawableRes res: Int,
        @ColorRes color: Int?
    ): BitmapDescriptor {
        val vectorDrawable = ResourcesCompat.getDrawable(context.resources, res, null)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        color?.let {
            DrawableCompat.setTint(vectorDrawable, colorFromResource(color))
        }
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun mailTo(activity: Activity, email: String, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this

        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        }
    }

    fun distanceMeters(latLng: LatLng, latLng1: LatLng): Int {
        return LocationUtils.distanceMeters(latLng, latLng1).apply {
            if (this == null) {
                crashReportsProvider.logException(IllegalStateException("distanceMeters == null, $latLng $latLng1"))
            }
        } ?: Int.MAX_VALUE
    }

    fun openUrl(activity: Activity, url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        activity.startActivity(intent)
    }

    fun getMapsIntent(latLng: LatLng): Intent? {
        return try {
//        val gmmIntentUri = Uri.parse("google.navigation:q=${geofence.value!!.latitude},${geofence.value!!.longitude}")

            val googleMapsUrl = "https://www.google.com/maps/dir/?api=1&" +
                    "destination=${latLng.latitude},${latLng.longitude}"

            val gmmIntentUri = Uri.parse(googleMapsUrl)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
//        mapIntent.setPackage("com.google.android.apps.maps")
            mapIntent
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
            null
        }
    }

    fun getBuildVersion(): String? {
        try {
            val pInfo = MyApplication.context.packageManager.getPackageInfo(
                MyApplication.context.packageName,
                0
            )
            return pInfo.versionName
        } catch (e: Exception) {
            return null
        }
    }

    override fun getQuantityString(@PluralsRes res: Int, number: Int): String {
        return context.resources.getQuantityString(res, number)
    }

    fun parseUri(link: String): Uri {
        return Uri.parse(link)
    }

    override fun getErrorMessage(e: Exception): ErrorMessage {
        //todo NonReportableException
        return when (e) {
            is HttpException -> {
                val errorBody = e.response()?.errorBody()?.string()
                if (MyApplication.DEBUG_MODE) {
                    Log.v("hypertrack-verbose", errorBody.toString())
                }
                val path = e.response()?.let { response ->
                    response.raw().request.let { request ->
                        "${request.method} ${response.code()} ${request.url.encodedPath}"
                    }
                }
                "${path.toString()}\n\n${errorBody.toString()}"
            }
            else -> {
                if (e.isNetworkError()) {
                    stringFromResource(R.string.network_error)
                } else {
                    if (MyApplication.DEBUG_MODE) {
                        e.printStackTrace()
                    }
                    e.format()
                }
            }
        }.let { ErrorMessage(it) }
    }

    companion object {
        const val TAG = "OsUtilsProvider"
        const val SHARE_BROADCAST_ACTION = "com.hypertrack.logistics.SHARE_TRIP"
    }
}

fun Exception.format(): String {
    return if (this is SimpleException) {
        message ?: javaClass.simpleName
    } else {
        "${javaClass.simpleName}${message?.let { ": $it" }.orEmpty()}"
    }
}

fun Int.stringFromResource(): String {
    return MyApplication.context.getString(this)
}

// todo change Exception to SimpleException
class SimpleException(message: String) : Exception(message)
