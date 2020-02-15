# android-image-utils

<h3>Library containing utility classes for most common <i>Image</i> and <i>Camera</i> related android tasks.</h3>

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
- Image [`capture`](https://github.com/dasBikash84/android-image-utils/blob/master/android_image_utils/src/main/java/com/dasbikash/android_image_utils/CameraUtils.kt) using camera api made very simple (`don't need User permission`).
- Remote [`image downloading and display`](https://github.com/dasBikash84/android-image-utils/blob/master/android_image_utils/src/main/java/com/dasbikash/android_image_utils/ImageUtils.kt) made very simple.
- Provides both synchronous(`blocking`) and asynchronous (`Coroutine suspend`/`Rx-java Observable`) options for image downloading.
- And [`utility methods`](https://github.com/dasBikash84/android-image-utils/blob/master/android_image_utils/src/main/java/com/dasbikash/android_image_utils/Extensions.kt) for general image related operations.

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
