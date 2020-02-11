/*
 * Copyright 2020 das.bikash.dev@gmail.com. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dasbikash.android_image_utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.provider.MediaStore
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Transformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ImageUtils {

    lateinit var mPhotoFile:File

    suspend fun urlToFile(link: String, fileName: String, context: Context): String? {
        try {
            val bitmap = suspendCoroutine<Bitmap> {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        it.resume(Picasso.get().load(link).get())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        it.resumeWithException(e)
                    }
                }
            }
//            val bitmap = Picasso.get().load(link).get()
            val imageFile = File(context.filesDir.absolutePath + fileName + ".jpg")
            val os = FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
            return imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun customLoader(imageView: ImageView, imageFile: File? = null, url: String? = null,
                     @DrawableRes placeHolderImageResourceId: Int,
                     @DrawableRes defaultImageResourceId: Int, callBack: (() -> Unit)? = null,
                     showLandscape:Boolean=false) {
        val picasso = Picasso.get()
        val requestCreator: RequestCreator

        if (imageFile != null) {
            requestCreator = picasso.load(imageFile)
        } else if(url != null) {
            if (url.startsWith("/data")) {
                requestCreator = picasso.load(File(url))
            } else {
                requestCreator = picasso.load(url)
            }
        } else {
            requestCreator = picasso.load(defaultImageResourceId)
        }
        requestCreator
                .error(defaultImageResourceId)
                .placeholder(placeHolderImageResourceId)
                .transform(object : Transformation{
                    override fun key(): String {
                        return UUID.randomUUID().toString()
                    }
                    override fun transform(source: Bitmap?): Bitmap {
                        if (showLandscape && (source!!.height>source.width)){
                            val result =
                                rotate(source, 270)
                            source.recycle()
                            return result
                        }else {
                            return source!!
                        }
                    }
                })
                .into(imageView, object : Callback {
                    override fun onSuccess() {
                        callBack?.let { callBack() }
                    }

                    override fun onError(e: java.lang.Exception?) {}
                })
    }

    fun cancelRequestForImageView(imageView: ImageView) {
        Picasso.get().cancelRequest(imageView)
    }

    fun resetPhotoFile(context: Context){
        mPhotoFile = File.createTempFile(UUID.randomUUID().toString(),".jpg",context.filesDir)
    }

    fun launchCameraForImage(launcherActivity:Activity, requestCode:Int, authority:String, fragment:Fragment?=null){
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

    fun processResultDataForFile(launcerActivity:Activity, authority:String,doOnExit:((File)->Unit)?){
        processResultData(launcerActivity, authority)
        doOnExit?.let { it(mPhotoFile) }
    }

    fun processResultDataForBitmap(launcerActivity:Activity, authority:String,doOnExit:((Bitmap)->Unit)?){
        processResultData(launcerActivity, authority)
        ImageCompressionUtils
            .getBitmapImageFromFile(launcerActivity.applicationContext, mPhotoFile)?.apply {
                doOnExit?.let { it(this) }
            }
    }

    fun processResultData(launcerActivity:Activity, authority:String){
        val uri = FileProvider.getUriForFile(
            launcerActivity,authority, mPhotoFile
        )

        launcerActivity.revokeUriPermission(
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun rotate(bm: Bitmap, rotation: Int): Bitmap {
        if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
        }
        return bm
    }
}