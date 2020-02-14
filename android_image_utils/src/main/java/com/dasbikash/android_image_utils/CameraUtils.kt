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


    fun resetPhotoFile(context: Context){
        mPhotoFile = File.createTempFile(
            UUID.randomUUID().toString(),
            ImageUtils.JPG_FILE_EXT,context.filesDir)
    }

    fun launchCameraForImage(launcherActivity: Activity, requestCode:Int, authority:String, fragment: Fragment?=null){
        resetPhotoFile(launcherActivity)
        val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val uri = FileProvider.getUriForFile(
            launcherActivity,authority, mPhotoFile
        )

        captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        val cameraActivities = launcherActivity.getPackageManager().queryIntentActivities(
            captureImage, PackageManager.MATCH_DEFAULT_ONLY
        )
        for (activity in cameraActivities) {
            (launcherActivity as ContextWrapper).grantUriPermission(
                activity.activityInfo.packageName,
                uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        if (fragment!=null) {
            fragment.startActivityForResult(captureImage, requestCode)
        }else{
            launcherActivity.startActivityForResult(captureImage, requestCode)
        }
    }

    fun processResultDataForFile(launcerActivity: Activity, authority:String, doOnExit:((File)->Unit)?){
        processResultData(launcerActivity, authority)
        doOnExit?.let { it(mPhotoFile) }
    }

    fun processResultDataForBitmap(launcerActivity: Activity, authority:String, doOnExit:((Bitmap)->Unit)?){
        processResultData(launcerActivity, authority)
        ImageCompressionUtils
            .getBitmapImageFromFile(launcerActivity.applicationContext, mPhotoFile)?.apply {
                doOnExit?.let { it(this) }
            }
    }

    fun processResultData(launcerActivity: Activity, authority:String){
        val uri = FileProvider.getUriForFile(
            launcerActivity,authority, mPhotoFile
        )

        launcerActivity.revokeUriPermission(
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}