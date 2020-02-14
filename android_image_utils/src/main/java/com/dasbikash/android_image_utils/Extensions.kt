package com.dasbikash.android_image_utils

import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


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

/**
 * Extension function for loading image File on Subject ImageView.
 *
 * @param file Subject image file
 * */
fun ImageView.setImageFile(file: File){
    if (file.exists()){
        this.setImageBitmap(ImageUtils.getBitmapFromFile(file))
    }
}

/**
 * Extension function for downloading image asynchronously for given url and display on subject ImageView.
 *
 * @param url String Image url
 * @param lifecycleOwner LifecycleOwner hook, Activity/Fragment based on from where method is called
 * @param placeHolderImageResourceId Place-holder image resource ID
 * @param defaultImageResourceId Image resource Id which will be loaded in case of download error
 * @param callBack optional callback which will be called on download success
 * @param showLandscape flag to force land-scape display
 * */
fun ImageView.setImageUrl(url: String, lifecycleOwner: LifecycleOwner,
                          @DrawableRes placeHolderImageResourceId: Int?=null,
                          @DrawableRes defaultImageResourceId: Int?=null,
                          callBack: (() -> Unit)? = null,
                          showLandscape:Boolean=false){
    ImageUtils
        .setImageUrl(
            this,url,lifecycleOwner,placeHolderImageResourceId,defaultImageResourceId, callBack, showLandscape)
}