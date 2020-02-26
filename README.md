# android-image-utils

### Library containing utility classes for most common **Image** processing related tasks on android.

For android camera helper library visit [`here`](https://github.com/dasBikash84/android-camera-utils).

[![](https://jitpack.io/v/dasBikash84/android-image-utils.svg)](https://jitpack.io/#dasBikash84/android-image-utils)

## Dependency

Add this in your root `build.gradle` file (**not** your module `build.gradle` file):

```gradle
allprojects {
	repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Then, add the library to your module `build.gradle`
```gradle
dependencies {
    implementation 'com.github.dasBikash84:android-image-utils:latest.release.here'
}
```

## Features
- Remote [`image downloading and display`](https://github.com/dasBikash84/android-image-utils/blob/master/android_image_utils/src/main/java/com/dasbikash/android_image_utils/ImageUtils.kt) made very simple.
- Provides both synchronous(`blocking`) and asynchronous (`Coroutine suspend`/`Rx-java Observable`) options for image downloading.
- And conversion among different image formats.

## Few usage example

##### Loading image directly(asynchronous) into `ImageView` from url
Image will be displayed only if given LifecycleOwner(AppcompatActivity/fragment) is not destroyed.
```
    ImageUtils.displayImageUrl(imageView, url,appCompatActivity) //Loading on AppcompatActivity context
    
    //or
    
    ImageUtils.displayImageUrl(imageView, url,fragment) //Loading on Fragment context
    
    //or
    
    imageView.displayImageUrl(url,appCompatActivity) //Loading on AppcompatActivity context using kotlin extensions
    
    //or
    
    imageView.displayImageUrl(url,fragment) //Loading on Fragment context using kotlin extensions
```

##### Asynchronous Image download
```
    //
    // Downloading image in Bitmap format
    //
    //Provide "AppCompatActivity/Fragment" for "lifecycleOwner"
    //Will call doOnSuccess/doOnFailure only if given lifecycleOwner is not destroyed
    //Corresponding extension function is also provided on AppCompatActivity/Fragment for kotlin
    ImageUtils.fetchImageBitmapFromUrl(
        url, lifecycleOwner,
        doOnSuccess = {
            //Bitmap injected as it
        }, 
        doOnFailure = {
            //Throwable injected as it
        })
        
    // OR

    // 
    // Downloading image in PNG format
    //
    //Provide "AppCompatActivity/Fragment" for "lifecycleOwner"
    //Will call doOnSuccess/doOnFailure only if given lifecycleOwner is not destroyed
    //Corresponding extension function is also provided on AppCompatActivity/Fragment for kotlin
    
    ImageUtils.fetchImageFromUrl(
        url, lifecycleOwner,context,
        doOnSuccess = {
            //File injected as it
        }, 
        doOnFailure = {
            //Throwable injected as it
        })
    
```

##### Other image downloading options:
```
    //Blocking Image Bitmap download from Url 
    ImageUtils.getBitmapFromUrl(url)
    
    //Suspended Image Bitmap download from Url 
    ImageUtils.getBitmapFromUrlSuspended(url)
    
    //Asynchronous Image Bitmap download from Url for Rx-java users
    ImageUtils.getBitmapObservableFromUrl(url)
```

##### Conversion between different image format
```
    // Bitmap to PNG synchronous
    ImageUtils.getPngFromBitmap(bitmap,fileName,context)
    
    // Bitmap to JPeG synchronous
    ImageUtils.getJpegFromBitmap(bitmap,fileName,context)
    
    // Bitmap to PNG suspended
    ImageUtils.getPngFromBitmapSuspended(bitmap,fileName,context)
    
    // Bitmap to JPeG suspended
    ImageUtils.getJpgFromBitmapSuspended(bitmap,fileName,context)
    
    // File to bitmap synchronous
    ImageUtils.getBitmapFromFile(file)
    
    // File to bitmap suspended 
    ImageUtils.getBitmapFromFileSuspended(file)
```

License
--------

    Copyright 2020 Bikash Das(das.bikash.dev@gmail.com)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
