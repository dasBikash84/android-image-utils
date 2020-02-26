package com.dasbikash.android_image_utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.dasbikash.android_image_utils.exceptions.ImageDownloadException
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Helper class for Image related operations on Android devices.
 *
 * @author Bikash Das(das.bikash.dev@gmail.com)
 * */
class ImageUtils {
    companion object {
        private const val JPG_FILE_EXT = ".jpg"
        private const val PNG_FILE_EXT = ".png"

        private fun getFileExtension(fileFormat: Bitmap.CompressFormat): String {
            return when {
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
         * */
        @JvmStatic
        fun fetchImageBitmapFromUrl(
            url: String, lifecycleOwner: LifecycleOwner,
            doOnSuccess: (Bitmap) -> Any?, doOnFailure: (Throwable) -> Any?
        ) {
            ImageLoader(url, doOnSuccess, doOnFailure, lifecycleOwner).run()
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
         * */
        @JvmStatic
        fun fetchImageFromUrl(
            url: String, lifecycleOwner: LifecycleOwner, context: Context,
            doOnSuccess: (File) -> Any?, doOnFailure: (Throwable) -> Any?,
            fileName: String? = null
        ) {
            ImageLoader(url, {
                doOnSuccess(
                    getPngFromBitmap(
                        it,
                        fileName ?: "image_${System.currentTimeMillis()}",
                        context
                    )
                )
            }, doOnFailure, lifecycleOwner).run()
        }

        /**
         * Will download image synchronously for given url.
         *
         * @param url String Image url
         * @exception throws ImageDownloadException wrapping cause.
         * @return Downloaded image Bitmap
         * */
        @JvmStatic
        fun getBitmapFromUrl(url: String): Bitmap {
            try {
                return BitmapFactory.decodeStream(URL(url).openStream())
            } catch (ex: Throwable) {
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
        @JvmStatic
        suspend fun getBitmapFromUrlSuspended(url: String): Bitmap =
            runSuspended { getBitmapFromUrl(url) }


        /**
         * Will return Observable which downloads image upon subscription for given url.
         *
         * @param url String Image url
         * @exception throws ImageDownloadException wrapping cause.
         * @return Observable for image download
         * */
        @JvmStatic
        fun getBitmapObservableFromUrl(url: String): Observable<Bitmap> {
            return Observable.just(url)
                .subscribeOn(Schedulers.io())
                .map {
                    getBitmapFromUrl(it)
                }
        }

        private fun getFileFromBitmap(
            bitmap: Bitmap, fileName: String, context: Context,
            fileFormat: Bitmap.CompressFormat
        ): File {
            val imageFile =
                File(context.filesDir.absolutePath + fileName + getFileExtension(fileFormat))
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
        @JvmStatic
        fun getPngFromBitmap(bitmap: Bitmap, fileName: String, context: Context): File =
            getFileFromBitmap(bitmap, fileName, context, Bitmap.CompressFormat.PNG)

        /**
         * Synchronous Bitmap to JPEG converter.
         *
         * @param bitmap Subject image Bitmap
         * @param fileName name of return file
         * @param context Android context
         * @return JPEG image file
         * */
        @JvmStatic
        fun getJpegFromBitmap(bitmap: Bitmap, fileName: String, context: Context): File =
            getFileFromBitmap(bitmap, fileName, context, Bitmap.CompressFormat.JPEG)

        private suspend fun getFileFromBitmapSuspended(
            bitmap: Bitmap, fileName: String, context: Context,
            fileFormat: Bitmap.CompressFormat
        ): File {
            val imageFile =
                File(context.filesDir.absolutePath + fileName + getFileExtension(fileFormat))
            val os = FileOutputStream(imageFile)
            bitmap.compress(fileFormat, 100, os)
            runSuspended { os.flush() }
            runSuspended { os.close() }
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
        @JvmStatic
        suspend fun getPngFromBitmapSuspended(
            bitmap: Bitmap,
            fileName: String,
            context: Context
        ): File =
            getFileFromBitmapSuspended(bitmap, fileName, context, Bitmap.CompressFormat.PNG)

        /**
         * Suspended Bitmap to JPEG converter.
         *
         * @param bitmap Subject image Bitmap
         * @param fileName name of return file
         * @param context Android context
         * @return JPEG image file
         * */
        @JvmStatic
        suspend fun getJpgFromBitmapSuspended(
            bitmap: Bitmap,
            fileName: String,
            context: Context
        ): File =
            getFileFromBitmapSuspended(bitmap, fileName, context, Bitmap.CompressFormat.JPEG)

        /**
         * File to Bitmap converter.
         *
         * @param File Subject image file
         * @return Image file if exists else null.
         * */
        @JvmStatic
        fun getBitmapFromFile(file: File): Bitmap? = BitmapFactory.decodeFile(file.path)

        @JvmStatic
        suspend fun getBitmapFromFileSuspended(file: File): Bitmap? =
            runSuspended { BitmapFactory.decodeFile(file.path)}

        /**
         * Sets image File to given ImageView.
         *
         * @param file Subject image file
         * @param imageView Subject ImageView
         * */
        @JvmStatic
        fun displayImageFile(imageView: ImageView, file: File) {
            if (file.exists()) {
                imageView.setImageBitmap(getBitmapFromFile(file))
            }
        }

        /**
         * Will download image asynchronously for given url and display on given ImageView.
         * Image will be displayed only if given LifecycleOwner is not destroyed.
         *
         * @param imageView Subject Image View
         * @param url String Image url
         * @param lifecycleOwner LifecycleOwner hook, Activity/Fragment based on from where method is called
         * @param placeHolderImageResourceId Place-holder image resource ID
         * @param defaultImageResourceId Image resource Id which will be loaded in case of download error
         * @param doOnImageLoad optional callback which will be called on download success
         * @param showLandscape flag to force land-scape display
         * */
        @JvmStatic
        fun displayImageFromUrl(
            imageView: ImageView,
            url: String,
            lifecycleOwner: LifecycleOwner,
            @DrawableRes placeHolderImageResourceId: Int? = null,
            @DrawableRes defaultImageResourceId: Int? = null,
            doOnImageLoad: (() -> Unit)? = null,
            showLandscape: Boolean = false
        ) {
            placeHolderImageResourceId?.let { imageView.setImageResource(it) }
            ImageLoader(
                url,
                doOnSuccess = {
                    if (showLandscape && (it.height > it.width)) {
                        rotateBitmap(it, 270)
                    } else {
                        it
                    }.apply {
                        imageView.setImageBitmap(this)
                    }
                    doOnImageLoad?.invoke()
                },
                doOnFailure = {
                    it.printStackTrace()
                    defaultImageResourceId?.let { imageView.setImageResource(it) }
                },
                lifecycleOwner = lifecycleOwner
            ).run()
        }

        @JvmStatic
        fun rotateBitmap(bm: Bitmap, rotation: Int): Bitmap {
            if (rotation != 0 ) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
            }
            return bm
        }
    }

    internal class ImageLoader(private val url: String,
                              private val doOnSuccess: (Bitmap) -> Any?,
                              private val doOnFailure: ((Throwable) -> Any?)?=null,
                              lifecycleOwner: LifecycleOwner)
        : DefaultLifecycleObserver{
        private var active = true
        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            active = false
        }

        fun run(){
            GlobalScope.launch {
                try {
                    runSuspended { getBitmapFromUrl(url) }.let {
                        if (active){
                            runOnMainThread({doOnSuccess(it)})
                        }
                    }
                }catch (ex:Throwable){
                    if (active){
                        runOnMainThread({doOnFailure?.invoke(ex)})
                    }
                }
            }
        }
    }
}


/**
 * Extension function for loading image File on Subject ImageView.
 *
 * @param file Subject image file
 * */
fun ImageView.displayImageFile(file: File){
    if (file.exists()){
        this.setImageBitmap(ImageUtils.getBitmapFromFile(file))
    }
}

/**
 * Extension function for downloading image asynchronously for given url and display on subject ImageView.
 * Image will be displayed only if given LifecycleOwner is not destroyed.
 *
 * @param url String Image url
 * @param lifecycleOwner LifecycleOwner hook, Activity/Fragment based on from where method is called
 * @param placeHolderImageResourceId Place-holder image resource ID
 * @param defaultImageResourceId Image resource Id which will be loaded in case of download error
 * @param callBack optional callback which will be called on download success
 * @param showLandscape flag to force land-scape display
 * */
fun ImageView.displayImageFromUrl(url: String, lifecycleOwner: LifecycleOwner,
                                  @DrawableRes placeHolderImageResourceId: Int?=null,
                                  @DrawableRes defaultImageResourceId: Int?=null,
                                  callBack: (() -> Unit)? = null,
                                  showLandscape:Boolean=false){
    ImageUtils
        .displayImageFromUrl(
            this,url,lifecycleOwner,placeHolderImageResourceId,defaultImageResourceId, callBack, showLandscape)
}

/**
 * ```
 * Extension function on AppCompatActivity to download image asynchronously for given url
 * and inject image as Bitmap to given 'doOnSuccess' function parameter.
 * 'doOnSuccess' will be called on UI thread only if given 'AppCompatActivity' is not destroyed.
 * On error 'doOnFailure' will be called on UI thread only if given 'AppCompatActivity' is not destroyed.
 * ```
 *
 * @param url String Image url
 * @param doOnSuccess function to be called on image download success.
 * @param doOnFailure function to be called on image download failure.
 * */
fun AppCompatActivity.fetchImageBitmapFromUrl(
    url: String, doOnSuccess: (Bitmap) -> Any?, doOnFailure: (Throwable) -> Any?
) {
    ImageUtils.fetchImageBitmapFromUrl(url, this,doOnSuccess, doOnFailure)
}

/**
 * ```
 * Extension function on Fragment to download image asynchronously for given url
 * and inject image as Bitmap to given 'doOnSuccess' function parameter.
 * 'doOnSuccess' will be called on UI thread only if subject 'Fragment' is not destroyed.
 * On error 'doOnFailure' will be called on UI thread only if subject 'Fragment' is not destroyed.
 * ```
 *
 * @param url String Image url
 * @param doOnSuccess function to be called on image download success.
 * @param doOnFailure function to be called on image download failure.
 * */
fun Fragment.fetchImageBitmapFromUrl(
    url: String, doOnSuccess: (Bitmap) -> Any?, doOnFailure: (Throwable) -> Any?
) {
    ImageUtils.fetchImageBitmapFromUrl(url, this,doOnSuccess, doOnFailure)
}

/**
 * Extension function on AppCompatActivity to download image asynchronously for given url and
 * inject image as File to given 'doOnSuccess' function parameter.
 * 'doOnSuccess' will be called on UI thread only if subject 'AppCompatActivity' is not destroyed.
 * On error 'doOnFailure' will be called on UI thread only if subject 'AppCompatActivity' is not destroyed.
 *
 * @param url String Image url
 * @param doOnSuccess function to be called on image download success.
 * @param doOnFailure function to be called on image download failure.
 * @param fileName Optional filename
 * */
fun AppCompatActivity.fetchImageFromUrl(
    url: String,
    doOnSuccess: (File) -> Any?,
    doOnFailure: (Throwable) -> Any?,
    fileName: String? = null
) {
    ImageUtils.fetchImageFromUrl(
        url,this,this.applicationContext,
        doOnSuccess, doOnFailure, fileName
    )
}
/**
 * Extension function on Fragment to download image asynchronously for given url and
 * inject image as File to given 'doOnSuccess' function parameter.
 * 'doOnSuccess' will be called on UI thread only if subject 'Fragment' is not destroyed.
 * On error 'doOnFailure' will be called on UI thread only if subject 'Fragment' is not destroyed.
 *
 * @param url String Image url
 * @param doOnSuccess function to be called on image download success.
 * @param doOnFailure function to be called on image download failure.
 * @param fileName Optional filename
 * */
fun Fragment.fetchImageFromUrl(
    url: String,
    doOnSuccess: (File) -> Any?,
    doOnFailure: (Throwable) -> Any?,
    fileName: String? = null
) {
    ImageUtils.fetchImageFromUrl(
        url,this,this.context!!.applicationContext,
        doOnSuccess, doOnFailure, fileName
    )
}