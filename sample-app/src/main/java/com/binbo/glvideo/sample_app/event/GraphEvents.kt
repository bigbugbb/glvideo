package com.binbo.glvideo.sample_app.event

import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent

class TakePictureEvent : BaseGraphEvent<MediaData>()
class TakePictureCompleteEvent(url: String) : BaseGraphEvent<MediaData>()