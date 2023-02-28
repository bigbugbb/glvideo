package com.binbo.glvideo.sample_app.utils.player

import android.os.Bundle
import com.kk.taurus.playerbase.event.OnPlayerEventListener

class CompositeOnPlayerEventListener : OnPlayerEventListener {

    private val listeners: MutableSet<OnPlayerEventListener> = mutableSetOf()

    fun addListener(listener: OnPlayerEventListener?) {
        listener?.let { listeners += it }
    }

    fun removeListener(listener: OnPlayerEventListener?) {
        listener?.let { listeners -= listener }
    }

    fun clear() = listeners.clear()

    override fun onPlayerEvent(eventCode: Int, bundle: Bundle?) {
        listeners.forEach { it.onPlayerEvent(eventCode, bundle) }
    }
}