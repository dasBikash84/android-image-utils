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
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileNotFoundException

object ImageCompressionUtils {

    private const val DEFAULT_MIN_WIDTH_QUALITY = 200 // min pixels
    private const val TAG = "ImageCompressionUtils"
    private const val TEMP_IMAGE_NAME = "tempImage"

    fun getBitmapImageFromUri(
        context: Context,
        selectedImage: Uri
    ): Bitmap? {
        var bm: Bitmap? = getImageResized(context, selectedImage)
        Log.d(TAG, "selectedImage: $selectedImage")
        val rotation = getRotation(context, selectedImage, true)
        bm = rotate(bm, rotation)
        return bm
    }

    fun getBitmapImageFromFile(context: Context, file: File?): Bitmap? {
        val selectedImage = Uri.fromFile(file)
        var bm: Bitmap? = getImageResized(context, selectedImage)
        Log.d(TAG, "selectedImage: $selectedImage")
        val rotation = getRotation(context, selectedImage, true)
        bm = rotate(bm, rotation)
        return bm
    }

    fun getImageFromResult(
        context: Context, resultCode: Int,
        imageReturnedIntent: Intent?
    ): Bitmap? {
        Log.d(
            TAG,
            "getImageFromResult, resultCode: $resultCode"
        )
        var bm: Bitmap? = null
        val imageFile = getTempFile(context)
        if (resultCode == Activity.RESULT_OK) {
            val selectedImage: Uri?
            val isCamera =
                imageReturnedIntent == null || imageReturnedIntent.data == null || imageReturnedIntent.data == Uri.fromFile(
                    imageFile
                )
            selectedImage = if (isCamera) {
                /** CAMERA  */
                Uri.fromFile(imageFile)
            } else {
                /** ALBUM  */
                imageReturnedIntent!!.data
            }
            Log.d(TAG, "selectedImage: $selectedImage")
            bm = getImageResized(context, selectedImage)
            val rotation = getRotation(context, selectedImage, isCamera)
            bm = rotate(bm, rotation)
        }
        return bm
    }

    private fun decodeBitmap(
        context: Context,
        theUri: Uri?,
        sampleSize: Int
    ): Bitmap {
        val options = BitmapFactory.Options()
        options.inSampleSize = sampleSize
        var fileDescriptor: AssetFileDescriptor? = null
        try {
            fileDescriptor = context.contentResolver.openAssetFileDescriptor(theUri!!, "r")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        val actuallyUsableBitmap = BitmapFactory.decodeFileDescriptor(
            fileDescriptor!!.fileDescriptor, null, options
        )
        Log.d(
            TAG,
            options.inSampleSize.toString() + " sample method bitmap ... " +
                    actuallyUsableBitmap.width + " " + actuallyUsableBitmap.height
        )
        return actuallyUsableBitmap
    }

    /**
     * Resize to avoid using too much memory loading big images (e.g.: 2560*1920)
     */
    private fun getImageResized(
        context: Context,
        selectedImage: Uri?
    ): Bitmap? {
        var bm: Bitmap? = null
        val sampleSizes = intArrayOf(11, 9, 7, 5, 3, 2, 1)
        var i = 0
        do {
            bm = decodeBitmap(context, selectedImage, sampleSizes[i])
            Log.d(
                TAG,
                "resizer: new bitmap width = " + bm.width
            )
            i++
        } while (bm!!.width < DEFAULT_MIN_WIDTH_QUALITY && i < sampleSizes.size)
        return bm
    }

    private fun getRotation(
        context: Context,
        imageUri: Uri?,
        isCamera: Boolean
    ): Int {
        val rotation: Int
        rotation = if (isCamera) {
            getRotationFromCamera(context, imageUri)
        } else {
            getRotationFromGallery(context, imageUri)
        }
        Log.d(TAG, "Image rotation: $rotation")
        return rotation
    }

    private fun getRotationFromCamera(
        context: Context,
        imageFile: Uri?
    ): Int {
        var rotate = 0
        try {
            context.contentResolver.notifyChange(imageFile!!, null)
            val exif =
                ExifInterface(imageFile.path!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
                ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
                ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rotate
    }

    fun getRotationFromGallery(context: Context, imageUri: Uri?): Int {
        val columns = arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cursor =
            context.contentResolver.query(imageUri!!, columns, null, null, null)
                ?: return 0
        cursor.moveToFirst()
        val orientationColumnIndex = cursor.getColumnIndex(columns[0])
        return cursor.getInt(orientationColumnIndex)
    }

    private fun rotate(bm: Bitmap?, rotation: Int): Bitmap? {
        if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            return Bitmap.createBitmap(bm!!, 0, 0, bm.width, bm.height, matrix, true)
        }
        return bm
    }

    private fun getTempFile(context: Context): File {
        val imageFile =
            File(context.externalCacheDir, TEMP_IMAGE_NAME)
        imageFile.parentFile.mkdirs()
        return imageFile
    }
}