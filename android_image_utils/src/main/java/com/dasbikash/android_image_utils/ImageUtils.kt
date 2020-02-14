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

    fun customLoader(imageView: ImageView, url: String,
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