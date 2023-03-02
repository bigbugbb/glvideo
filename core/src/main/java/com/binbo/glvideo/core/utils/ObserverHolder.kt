package com.binbo.glvideo.core.utils

import java.lang.ref.WeakReference

interface ObserverHolder<T> {
    val observers: List<T>
    fun registerObserver(observer: T)
    fun unregisterObserver(observer: T)
}

open class ObserverHolderDelegate<T> : ObserverHolder<T> {
    private val observersWrap = mutableListOf<WeakReference<T>>()

    override val observers: List<T>
        @Synchronized get() = observersWrap.mapNotNull { it.get() }

    @Synchronized
    override fun registerObserver(observer: T) {
        observersWrap.removeIf { it.get() == null || it.get() == observer }
        observersWrap.add(WeakReference(observer))
    }

    @Synchronized
    override fun unregisterObserver(observer: T) {
        observersWrap.removeIf { it.get() == null || it.get() == observer }
    }
}