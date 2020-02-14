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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.dasbikash.android_image_utils.exceptions.ImageDownloadException
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

/**
 * Helper class for Image related operations on Android devices.
 *
 * @author Bikash Das(das.bikash.dev@gmail.com)
 * */
object ImageUtils {

    internal const val JPG_FILE_EXT = ".jpg"
    internal const val PNG_FILE_EXT = ".png"

    private fun getFileExtension(fileFormat:Bitmap.CompressFormat):String{
        return when{
            fileFormat == Bitmap.CompressFormat.JPEG -> JPG_FILE_EXT
            else -> PNG_FILE_EXT
        }
    }

    /**
     * Will download image asynchronously for given url and inject image as Bitmap to given 'doOnSuccess' function parameter.
     * 'doOnSuccess' will be called on UI thread only if given 'LifecycleOwner' is not destroyed.
     * On error 'doOnFailure' will be called on UI thread only if given 'LifecycleOwner' is not destroyed.
     *
     * @param url String Image url
     * @param lifecycleOwner LifecycleOwner hook, Activity/Fragment based on from where method is called
     * @param doOnSuccess function to be called on image download success.
     * @param doOnFailure function to be called on image download failure.
     * @exception throws ImageDownloadException wrapping cause.
     * */
    fun getBitmapFromUrlAndProceed(url: String,lifecycleOwner: LifecycleOwner,
                                    doOnSuccess:(Bitmap)->Any?,doOnFailure:(Throwable)->Any?) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                getBitmapFromUrlSuspended(url).apply {
                    lifecycleOwner.runIfNotDestroyed { runOnMainThread({doOnSuccess(this)}) }
                }
            }catch (ex:Throwable){
                ex.printStackTrace()
                lifecycleOwner.runIfNotDestroyed { runOnMainThread({doOnFailure(ex)}) }
            }
        }
    }

    /**
     * Will download image asynchronously for given url and inject image as File to given 'doOnSuccess' function parameter.
     * 'doOnSuccess' will be called on UI thread only if given 'LifecycleOwner' is not destroyed.
     * On error 'doOnFailure' will be called on UI thread only if given 'LifecycleOwner' is not destroyed.
     *
     * @param url String Image url
     * @param lifecycleOwner LifecycleOwner hook, Activity/Fragment based on from where method is called
     * @param doOnSuccess function to be called on image download success.
     * @param doOnFailure function to be called on image download failure.
     * @exception throws ImageDownloadException wrapping cause.
     * */
    fun getFileFromUrlAndProceed(url: String,lifecycleOwner: LifecycleOwner,context: Context,
                                doOnSuccess:(File)->Any?,doOnFailure:(Throwable)->Any?,
                                fileName: String? = null) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                getBitmapFromUrlSuspended(url).apply {
                    val nameOnDisk = fileName ?: UUID.randomUUID().toString()
                    getPngFromBitmapSuspended(this, nameOnDisk, context).apply {
                        lifecycleOwner.runIfNotDestroyed {
                            runOnMainThread({doOnSuccess(this)})
                        }
                    }
                }
            }catch (ex:Throwable){
                ex.printStackTrace()
                lifecycleOwner.runIfNotDestroyed { runOnMainThread({doOnFailure(ex)}) }
            }
        }
    }

    /**
     * Will download image synchronously for given url.
     *
     * @param url String Image url
     * @exception throws ImageDownloadException wrapping cause.
     * @return Downloaded image Bitmap
     * */
    fun getBitmapFromUrl(url: String):Bitmap {
        try {
            return BitmapFactory.decodeStream(URL(url).openStream())
        }catch (ex:Throwable){
            throw ImageDownloadException(ex)
        }
    }


    /**
     * Will download image being suspended for given url.
     *
     * @param url String Image url
     * @exception throws ImageDownloadException wrapping cause.
     * @return Downloaded image Bitmap
     * */
    suspend fun getBitmapFromUrlSuspended(url: String):Bitmap =
        runSuspended { getBitmapFromUrl(url)}


    /**
     * Will return Observable which downloads image upon subscription for given url.
     *
     * @param url String Image url
     * @exception throws ImageDownloadException wrapping cause.
     * @return Observable for image download
     * */
    fun getBitmapFromUrlObservable(url: String):Observable<Bitmap> {
        return Observable.just(url)
            .subscribeOn(Schedulers.io())
            .map {
                getBitmapFromUrl(it)
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

    /**
     * Synchronous Bitmap to PNG converter.
     *
     * @param bitmap Subject image Bitmap
     * @param fileName name of return file
     * @param context Android context
     * @return PNG image file
     * */
    fun getPngFromBitmap(bitmap: Bitmap, fileName: String, context: Context):File =
        getFileFromBitmap(bitmap,fileName, context, Bitmap.CompressFormat.PNG)

    /**
     * Synchronous Bitmap to JPEG converter.
     *
     * @param bitmap Subject image Bitmap
     * @param fileName name of return file
     * @param context Android context
     * @return JPEG image file
     * */
    fun getJpegFromBitmap(bitmap: Bitmap, fileName: String, context: Context):File =
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

    /**
     * Suspended Bitmap to PNG converter.
     *
     * @param bitmap Subject image Bitmap
     * @param fileName name of return file
     * @param context Android context
     * @return PNG image file
     * */
    suspend fun getPngFromBitmapSuspended(bitmap: Bitmap, fileName: String, context: Context):File =
        getFileFromBitmapSuspended(bitmap,fileName, context, Bitmap.CompressFormat.PNG)

    /**
     * Suspended Bitmap to JPEG converter.
     *
     * @param bitmap Subject image Bitmap
     * @param fileName name of return file
     * @param context Android context
     * @return JPEG image file
     * */
    suspend fun getJpgFromBitmapSuspended(bitmap: Bitmap, fileName: String, context: Context):File =
        getFileFromBitmapSuspended(bitmap,fileName, context, Bitmap.CompressFormat.JPEG)

    /**
     * File to Bitmap converter.
     *
     * @param File Subject image file
     * @return Image file if exists else null.
     * */
    fun getBitmapFromFile(file: File):Bitmap? = BitmapFactory.decodeFile(file.path)

    /**
     * Sets image File to given Image View.
     *
     * @param file Subject image file
     * @param imageView Subject Image View
     * */
    fun setImageFile(imageView: ImageView,file: File){
        if (file.exists()){
            imageView.setImageBitmap(getBitmapFromFile(file))
        }
    }

    /**
     * Will download image asynchronously for given url and display on given ImageView.
     *
     * @param imageView Subject Image View
     * @param url String Image url
     * @param lifecycleOwner LifecycleOwner hook, Activity/Fragment based on from where method is called
     * @param placeHolderImageResourceId Place-holder image resource ID
     * @param defaultImageResourceId Image resource Id which will be loaded in case of download error
     * @param callBack optional callback which will be called on download success
     * @param showLandscape flag to force land-scape display
     * */
    fun setImageUrl(imageView: ImageView, url: String,
                     lifecycleOwner: LifecycleOwner,
                     @DrawableRes placeHolderImageResourceId: Int?=null,
                     @DrawableRes defaultImageResourceId: Int?=null,
                     callBack: (() -> Unit)? = null,
                     showLandscape:Boolean=false) {
        placeHolderImageResourceId?.let { imageView.setImageResource(it)}
        getBitmapFromUrlAndProceed(url,lifecycleOwner,
            doOnSuccess = {
                if (showLandscape && (it.height>it.width)){
                    rotate(it, 270)
                }else {
                    it
                }.apply {
                    imageView.setImageBitmap(this)
                }
                callBack?.invoke()
            },
            doOnFailure = {
                it.printStackTrace()
                defaultImageResourceId?.let { imageView.setImageResource(it)}
            }
        )
    }

    private fun rotate(bm: Bitmap, rotation: Int): Bitmap {
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

internal fun LifecycleOwner.runIfNotDestroyed(task:()->Any?){
    if (this.lifecycle.currentState != Lifecycle.State.DESTROYED){
        task()
    }
}

fun ImageView.setImageFile(file: File){
    if (file.exists()){
        this.setImageBitmap(ImageUtils.getBitmapFromFile(file))
    }
}

fun ImageView.setImageUrl(url: String,lifecycleOwner: LifecycleOwner,
                            @DrawableRes placeHolderImageResourceId: Int?=null,
                            @DrawableRes defaultImageResourceId: Int?=null,
                            callBack: (() -> Unit)? = null,
                            showLandscape:Boolean=false){
    ImageUtils
        .setImageUrl(
        this,url,lifecycleOwner,placeHolderImageResourceId,defaultImageResourceId, callBack, showLandscape)
}