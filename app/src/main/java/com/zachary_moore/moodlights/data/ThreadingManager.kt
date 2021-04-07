package com.zachary_moore.moodlights.data

import android.os.Handler
import android.os.HandlerThread

class ThreadingManager {

    private val lightHandler = Handler(HandlerThread(LIGHT_THREAD_NAME).also {
        it.start()
    }.looper)

    private var currentLightRunnable: Runnable? = null

    /**
     * Clear any state and kill any running tasks
     */
    fun cleanUp() {
        currentLightRunnable = null
        lightHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Kill running tasks but maintain state for the ability to resume
     */
    fun pauseLightTasks() {
        lightHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Re-post our existing light tasks
     */
    fun resumeLightTasks() {
        currentLightRunnable?.let(lightHandler::post)
    }

    /**
     * Start running a light task on a background thread.
     * Maintain the state for pausing and resuming
     */
    fun startLightTask(lightRunnable: Runnable) {
        this.currentLightRunnable = lightRunnable
        lightHandler.post(lightRunnable)
    }

    /**
     * Take the current task in state and post it with a delay amount, do this in
     * order to not expose underlying handler directly
     */
    fun postDelayedCurrentLightTask(delayAmount: Long) {
        checkNotNull(currentLightRunnable) {
            "Trying to post a delayed task held in state, but none exists"
        }.let {
            lightHandler.postDelayed(it, delayAmount)
        }
    }

    companion object {
        private const val LIGHT_THREAD_NAME = "LightsThread"
    }
}