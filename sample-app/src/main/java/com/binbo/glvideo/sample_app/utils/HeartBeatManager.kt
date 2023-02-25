package com.binbo.glvideo.sample_app.utils

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit

data class HeartBeatEvent(val timing: Int) { // millisecond
    inline fun isSecondInterval() = (timing % 1000 == 0 && timing > 0)
    inline fun isTwoSecondsInterval() = (timing % 2000 == 0 && timing > 0)
    inline fun isHalfSecondInterval() = (timing % 500 == 0 && timing > 0)
    inline fun isMinuteInterval() = (timing % 60000 == 0 && timing > 0)
    inline fun isTargetInterval(interval: Int) = (timing % interval == 0 && timing > 0)
}

object HeartBeatManager {

    private const val TAG = "HeartBeatManager"

    private const val period = 250 // 每250ms心跳一次

    var timing: Int = 0
        private set

    init {
        Observable.interval(period.toLong(), TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .doOnNext {
                timing = try {
                    Math.addExact(timing, period)
                } catch (e: ArithmeticException) {
                    0 // reset timing when overflow occurs
                }
            }
            .subscribe {
                RxBus.getDefault().send(HeartBeatEvent(timing))
            }
    }

    fun bootstrap() {}
}