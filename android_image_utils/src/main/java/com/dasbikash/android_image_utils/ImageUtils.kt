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
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.dasbikash.android_image_utils.exceptions.ImageDownloadException
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Transformation
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ImageUtils {

    private const val JPG_FILE_EXT = ".jpg"
    private const val PNG_FILE_EXT = ".png"

    lateinit var mPhotoFile:File

    private fun getFileExtension(fileFormat:Bitmap.CompressFormat):String{
        return when{
            fileFormat == Bitmap.CompressFormat.JPEG -> JPG_FILE_EXT
            else -> PNG_FILE_EXT
        }
    }

    /**
     * Will download image(if not already downloaded) suspended for given url and save on disk.
     *
     * @author Bikash Das
     * @param url Image url
     * @param fileName name of file on local disk
     * @param context android context
     * @return disk file path for success or null for failure.
     * */
    /*suspend fun fetchImage(url: String, fileName: String, context: Context): String? {
        try {
            val bitmap = runSuspended { getBitmapFromUrl(url) }
            val imageFile = getFileFromBitmapSuspended(bitmap,fileName, context,Bitmap.CompressFormat.PNG)
            return imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }*/

    fun getBitmapFromUrlAndProceed(url: String,lifecycleOwner: LifecycleOwner,
                                    doOnSuccess:(Bitmap)->Any?,doOnFailure:(Throwable)->Any?) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                getBitmapFromUrlSuspended(url).apply {
                    lifecycleOwner.runIfResumed { runOnMainThread({doOnSuccess(this)}) }
                }
            }catch (ex:Throwable){
                ex.printStackTrace()
                lifecycleOwner.runIfResumed { runOnMainThread({doOnFailure(ex)}) }
            }
        }
    }

    fun getFileFromUrlAndProceed(url: String,lifecycleOwner: LifecycleOwner,context: Context,
                                doOnSuccess:(File)->Any?,doOnFailure:(Throwable)->Any?,
                                fileName: String? = null) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                getBitmapFromUrlSuspended(url).apply {
                    val nameOnDisk = fileName ?: UUID.randomUUID().toString()
                    getPngFromBitmapSuspended(this, nameOnDisk, context).apply {
                        lifecycleOwner.runIfResumed {
                            runOnMainThread({doOnSuccess(this)})
                        }
                    }
                }
            }catch (ex:Throwable){
                ex.printStackTrace()
                lifecycleOwner.runIfResumed { runOnMainThread({doOnFailure(ex)}) }
            }
        }
    }

    fun getBitmapFromUrl(url: String):Bitmap {
        try {
            return BitmapFactory.decodeStream(URL(url).openStream())
        }catch (ex:Throwable){
            throw ImageDownloadException(ex)
        }
    }

    suspend fun getBitmapFromUrlSuspended(url: String):Bitmap =
        runSuspended { getBitmapFromUrl(url)}

    fun getBitmapFromUrlObservable(url: String):Observable<Bitmap> {
        return Observable.just(true)
            .subscribeOn(Schedulers.io())
            .map {
                getBitmapFromUrl(url)
            }
    }

    private fun getFileFromBitmap(bitmap: Bitmap, fileName: String, context: Context,
                          fileFormat:Bitmap.CompressFormat):File{
        val imageFile = File(context.filesDir.absolutePath + fileName + getFileExtension(fileFormat))
        val os = FileOutputStream(imageFile)
        bitmap.compress(fileFormat, 100, os)
        os.flush()
        os.close()
        return imageFile
    }

    fun getPngFromBitmap(bitmap: Bitmap, fileName: String, context: Context):File =
        getFileFromBitmap(bitmap,fileName, context, Bitmap.CompressFormat.PNG)

    fun getJpgFromBitmap(bitmap: Bitmap, fileName: String, context: Context):File =
        getFileFromBitmap(bitmap,fileName, context, Bitmap.CompressFormat.JPEG)

    private suspend fun getFileFromBitmapSuspended(bitmap: Bitmap, fileName: String, context: Context,
                                           fileFormat:Bitmap.CompressFormat):File{
        val imageFile = File(context.filesDir.absolutePath + fileName + getFileExtension(fileFormat))
        val os = FileOutputStream(imageFile)
        bitmap.compress(fileFormat, 100, os)
        runSuspended {os.flush()}
        runSuspended {os.close()}
        return imageFile
    }

    suspend fun getPngFromBitmapSuspended(bitmap: Bitmap, fileName: String, context: Context):File =
        getFileFromBitmapSuspended(bitmap,fileName, context, Bitmap.CompressFormat.PNG)

    suspend fun getJpgFromBitmapSuspended(bitmap: Bitmap, fileName: String, context: Context):File =
        getFileFromBitmapSuspended(bitmap,fileName, context, Bitmap.CompressFormat.JPEG)

    fun getBitmapFromFile(file: File):Bitmap? = BitmapFactory.decodeFile(file.path)

    fun setImageFile(imageView: ImageView,file: File){
        if (file.exists()){
            imageView.setImageBitmap(getBitmapFromFile(file))
        }
    }

    fun setImageUrl(imageView: ImageView,url: String, lifecycleOwner: LifecycleOwner){
        getBitmapFromUrlAndProceed(
            url,lifecycleOwner,{imageView.setImageBitmap(it)},{it.printStackTrace()}
        )
    }

    /**
     * Will download image(if not already downloaded) for given url and save on disk.
     *
     * @author Bikash Das
     * @param url Image url
     * @param fileName name of file on local disk
     * @param context android context
     * @return disk file path for success or null for failure.
     * */
    /*fun getBitmapFromUrl(url: String, fileName: String, context: Context): String? {
        try {
            val bitmap = getBitmapFromUrl(url)
            val imageFile = getFileFromBitmap(bitmap,fileName, context,Bitmap.CompressFormat.PNG)
            return imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is NetworkOnMainThreadException){
                throw e
            }
            return null
        }
    }*/

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
        mPhotoFile = File.createTempFile(UUID.randomUUID().toString(), JPG_FILE_EXT,context.filesDir)
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

internal suspend fun <T:Any> runSuspended(task:()->T):T {
    coroutineContext().let {
        return withContext(it) {
            return@withContext async(Dispatchers.IO) { task() }.await()
        }
    }
}

internal suspend fun coroutineContext(): CoroutineContext = suspendCoroutine { it.resume(it.context) }

internal fun isOnMainThread() = (Thread.currentThread() == Looper.getMainLooper().thread)

internal fun runOnMainThread(task: () -> Any?,delayMs:Long=0L){
    Handler(Looper.getMainLooper()).postDelayed( { task() },delayMs)
}

internal fun LifecycleOwner.runIfResumed(task:()->Any?){
    if (this.lifecycle.currentState != Lifecycle.State.DESTROYED){
        task()
    }
}

fun ImageView.setImageFile(file: File){
    if (file.exists()){
        this.setImageBitmap(ImageUtils.getBitmapFromFile(file))
    }
}

fun ImageView.setImageUrl(url: String, lifecycleOwner: LifecycleOwner){
    ImageUtils.getBitmapFromUrlAndProceed(
        url,lifecycleOwner,{this.setImageBitmap(it)},{it.printStackTrace()}
    )
}