package com.dasbikash.android_image_utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.util.*

/**
 * Helper class to take photo using camera.
 *
 * #### Example code
 *
 * ##### To launch camera
 * ```
 *  // From activity
 *  CameraUtils.launchCameraForImage(launcherActivity, requestCode)
 *  // or From Fragment
 *  CameraUtils.launchCameraForImage(fragment, requestCode)
 * ```
 *
 * ##### To get photo
 * ```
 * // To access provided image from activity/fragment, "onActivityResult" call
 *  CameraUtils.processResultDataForFile(context: Context, doWithFile)
 *  //or
 *  CameraUtils.processResultDataForBitmap(context: Context, doWithBitmap)
 * ```
 * */
class CameraUtils {

    companion object {

        lateinit var mPhotoFile: File

        private fun resetPhotoFile(context: Context) {
            mPhotoFile = File.createTempFile(
                UUID.randomUUID().toString(),
                ImageUtils.JPG_FILE_EXT, context.filesDir
            )
        }

        private fun getAuthority(context: Context) =
            "${context.applicationContext.packageName}.fileprovider"

        private fun cameraLaunchPreProcess(
            context: Context
        ): Intent? {
            resetPhotoFile(context)
            val uri = FileProvider.getUriForFile(
                context, getAuthority(context), mPhotoFile
            )
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            val cameraActivities = context.getPackageManager().queryIntentActivities(
                captureIntent, PackageManager.MATCH_DEFAULT_ONLY
            )
            if (cameraActivities.isNotEmpty()) {
                for (activity in cameraActivities) {
                    (context as ContextWrapper).grantUriPermission(
                        activity.activityInfo.packageName,
                        uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                return captureIntent
            }
            return null
        }

        @JvmStatic
        fun launchCameraForImage(launcherActivity: AppCompatActivity, requestCode: Int): Boolean {
            cameraLaunchPreProcess(launcherActivity)?.let {
                launcherActivity.startActivityForResult(it, requestCode)
                return true
            }
            return false
        }

        @JvmStatic
        fun launchCameraForImage(fragment: Fragment, requestCode: Int): Boolean {
            fragment.context?.let {
                cameraLaunchPreProcess(it)?.let {
                    fragment.startActivityForResult(it, requestCode)
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun processResultDataForFile(context: Context, doWithFile: ((File) -> Unit)?) {
            revokeUriPermission(context)
            doWithFile?.let { it(mPhotoFile) }
        }

        @JvmStatic
        fun processResultDataForBitmap(context: Context, doWithBitmap: ((Bitmap) -> Unit)?) {
            revokeUriPermission(context)
            ImageCompressionUtils
                .getBitmapImageFromFile(context.applicationContext, mPhotoFile)?.apply {
                    doWithBitmap?.let { it(this) }
                }
        }

        private fun revokeUriPermission(context: Context) {
            val uri = FileProvider.getUriForFile(
                context, getAuthority(context), mPhotoFile
            )
            context.revokeUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }
}

fun AppCompatActivity.launchCameraForImage(requestCode: Int): Boolean =
    CameraUtils.launchCameraForImage(this,requestCode)

fun Fragment.launchCameraForImage(requestCode: Int): Boolean =
    CameraUtils.launchCameraForImage(this,requestCode)