package com.dasbikash.android_image_utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.util.*

object CameraUtils {

    lateinit var mPhotoFile:File

    private fun resetPhotoFile(context: Context){
        mPhotoFile = File.createTempFile(
            UUID.randomUUID().toString(),
            ImageUtils.JPG_FILE_EXT,context.filesDir)
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
        if (cameraActivities.isNotEmpty()){
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

    fun launchCameraForImage(launcherActivity: Activity, requestCode:Int):Boolean{
        cameraLaunchPreProcess(launcherActivity)?.let {
            launcherActivity.startActivityForResult(it, requestCode)
            return true
        }
        return false
    }

    fun launchCameraForImage(fragment: Fragment, requestCode:Int):Boolean{
        fragment.context?.let {
            cameraLaunchPreProcess(it)?.let {
                fragment.startActivityForResult(it, requestCode)
                return true
            }
        }
        return false
    }

    fun processResultDataForFile(context: Context, doOnExit:((File)->Unit)?){
        revokeUriPermission(context)
        doOnExit?.let { it(mPhotoFile) }
    }

    fun processResultDataForBitmap(context: Context, doOnExit:((Bitmap)->Unit)?){
        revokeUriPermission(context)
        ImageCompressionUtils
            .getBitmapImageFromFile(context.applicationContext, mPhotoFile)?.apply {
                doOnExit?.let { it(this) }
            }
    }

    private fun revokeUriPermission(context: Context){
        val uri = FileProvider.getUriForFile(
            context,getAuthority(context), mPhotoFile
        )
        context.revokeUriPermission(
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}