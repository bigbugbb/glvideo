package com.binbo.glvideo.core.graph.base

import com.binbo.glvideo.core.graph.interfaces.IMediaData
import com.binbo.glvideo.core.graph.interfaces.IMediaSource

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 18:22
 */
abstract class BaseMediaSource<D : IMediaData> : BaseMediaObject<D>(), IMediaSource<D>