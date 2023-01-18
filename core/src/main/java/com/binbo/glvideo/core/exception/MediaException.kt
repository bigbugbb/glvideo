package com.binbo.glvideo.core.exception

open class MediaException(message: String = "", cause: Throwable? = null) : Exception(message, cause) {
}

class OutOfSharedTextureException(width: Int, height: Int, countToGet: Int) : MediaException(
    message = """
        Can't get $countToGet shared textures to use, please close some or 
        increase the max allowed texture count for dimension[${width}x${height}]
    """.trimIndent()
)
