package com.dasbikash.android_image_utils

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
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