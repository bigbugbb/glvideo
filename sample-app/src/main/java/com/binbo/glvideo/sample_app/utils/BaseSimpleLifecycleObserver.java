package com.binbo.glvideo.sample_app.utils;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public abstract class BaseSimpleLifecycleObserver implements DefaultLifecycleObserver {

    @NonNull
    private final LifecycleOwner mLifecycleOwner;

    /**
     * 绑定生命周期, 不需要主动调用removeObserver, 除非不希望再监听了
     */
    protected BaseSimpleLifecycleObserver(@NonNull LifecycleOwner owner) {
        mLifecycleOwner = owner;
        owner.getLifecycle().addObserver(this);
    }

    @CallSuper
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        mLifecycleOwner.getLifecycle().removeObserver(this);
    }
}