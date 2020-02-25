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

object CameraUtils {

    lateinit var mPhotoFile:File

    private fun resetPhotoFile(context: Context){
        mPhotoFile = File.createTempFile(
            UUID.randomUUID().toString(),
            ImageUtils.JPG_FILE_EXT,context.filesDir)
    }

    private fun cameraLaunchPreProcess(
        context: Context,
        authority: String
    ): Intent? {
        resetPhotoFile(context)
        val uri = FileProvider.getUriForFile(
            context, authority, mPhotoFile
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

    fun launchCameraForImage(launcherActivity: Activity, requestCode:Int, authority:String):Boolean{
        cameraLaunchPreProcess(launcherActivity, authority)?.let {
            launcherActivity.startActivityForResult(it, requestCode)
            return true
        }
        return false
    }

    fun launchCameraForImage(fragment: Fragment, requestCode:Int, authority:String):Boolean{
        fragment.context?.let {
            cameraLaunchPreProcess(it, authority)?.let {
                fragment.startActivityForResult(it, requestCode)
                return true
            }
            /*val launcherActivity = it
            resetPhotoFile(launcherActivity)
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val uri = FileProvider.getUriForFile(
                launcherActivity, authority, mPhotoFile
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
            fragment.startActivityForResult(captureImage, requestCode)*/
        }
        return false
    }

    fun processResultDataForFile(context: Context, authority:String, doOnExit:((File)->Unit)?){
        processResultData(context, authority)
        doOnExit?.let { it(mPhotoFile) }
    }

    fun processResultDataForBitmap(context: Context, authority:String, doOnExit:((Bitmap)->Unit)?){
        processResultData(context, authority)
        ImageCompressionUtils
            .getBitmapImageFromFile(context.applicationContext, mPhotoFile)?.apply {
                doOnExit?.let { it(this) }
            }
    }

    private fun processResultData(context: Context, authority:String){
        val uri = FileProvider.getUriForFile(
            context,authority, mPhotoFile
        )

        context.revokeUriPermission(
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}