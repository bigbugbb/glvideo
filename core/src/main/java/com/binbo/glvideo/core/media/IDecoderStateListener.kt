package com.binbo.glvideo.core.media

interface IDecoderStateListener {
    fun onDecoderPrepare(decodeJob: BaseDecoder?)
    fun onDecoderReady(decodeJob: BaseDecoder?)
    fun onDecoderRunning(decodeJob: BaseDecoder?)
    fun onDecoderPause(decodeJob: BaseDecoder?)
    fun onDecoderBeginFlush(decoder: BaseDecoder?)
    fun onDecoderEndFlush(decoder: BaseDecoder?)
    suspend fun onPreConsumeDecodedFrame(decodeJob: BaseDecoder?, frame: Frame)
    suspend fun onPostConsumeDecodedFrame(decodeJob: BaseDecoder?, frame: Frame)
    fun onDecoderFinish(decodeJob: BaseDecoder?)
    fun onDecoderDestroy(decodeJob: BaseDecoder?)
    fun onDecoderError(decodeJob: BaseDecoder?, msg: String)
}

open class DefaultDecoderStateListener : IDecoderStateListener {

    override fun onDecoderPrepare(decodeJob: BaseDecoder?) {
    }

    override fun onDecoderReady(decodeJob: BaseDecoder?) {
    }

    override fun onDecoderRunning(decodeJob: BaseDecoder?) {
    }

    override fun onDecoderPause(decodeJob: BaseDecoder?) {
    }

    override fun onDecoderBeginFlush(decoder: BaseDecoder?) {
    }

    override fun onDecoderEndFlush(decoder: BaseDecoder?) {
    }

    override suspend fun onPreConsumeDecodedFrame(decodeJob: BaseDecoder?, frame: Frame) {
    }

    override suspend fun onPostConsumeDecodedFrame(decodeJob: BaseDecoder?, frame: Frame) {
    }

    override fun onDecoderFinish(decodeJob: BaseDecoder?) {
    }

    override fun onDecoderDestroy(decodeJob: BaseDecoder?) {
    }

    override fun onDecoderError(decodeJob: BaseDecoder?, msg: String) {
    }
}
