package com.dasbikash.android_image_utils.exceptions

import java.lang.RuntimeException

class ImageDownloadException:RuntimeException {
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}