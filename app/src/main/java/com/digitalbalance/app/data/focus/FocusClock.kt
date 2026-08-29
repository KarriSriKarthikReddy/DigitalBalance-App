package com.digitalbalance.app.data.focus

import android.os.SystemClock
import com.digitalbalance.app.domain.focus.FocusTimeSnapshot

fun interface FocusClock {
    fun snapshot(): FocusTimeSnapshot
}

class AndroidFocusClock : FocusClock {
    override fun snapshot() = FocusTimeSnapshot(
        epochMillis = System.currentTimeMillis(),
        elapsedRealtimeMillis = SystemClock.elapsedRealtime()
    )
}
