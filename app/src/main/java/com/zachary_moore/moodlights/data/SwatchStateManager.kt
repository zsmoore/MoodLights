package com.zachary_moore.moodlights.data

import androidx.palette.graphics.Palette
import kotlin.math.absoluteValue

class SwatchStateManager {

    private var ticksSinceDominant = 0
    private var currentPalette: Palette? = null
    private var frequency = INITIAL_FREQUENCY.toFloat()

    fun modifyFrequency(frequency: Float) {
        this.frequency = frequency
    }

    fun reset() {
        ticksSinceDominant = 0
        currentPalette = null
    }

    /**
     * Load a new [Palette] to be referenced when computing swatch selection
     */
    fun loadPalette(palette: Palette) {
        currentPalette = palette
    }

    /**
     * Get next swatch state from the stored [Palette] in the manager
     *
     * This will select a swatch which forces a selection of [Palette.getDominantSwatch] if
     * one has not been given in too long.  Otherwise we will default to a random swatch from
     * [Palette.getSwatches]
     */
    fun getNextSwatch(): Palette.Swatch {
        val safePalette = checkNotNull(currentPalette) {
            "Trying to get a swatch with none set"
        }

        return requireNotNull(if (ticksSinceDominant > 2) {
            ticksSinceDominant = 0
            safePalette.dominantSwatch
        } else {
            ticksSinceDominant += 1
            safePalette.swatches.random().also {
                if (it == safePalette.dominantSwatch) {
                    ticksSinceDominant = 0
                }
            }
        }) {
            "Unable to get swatch from palette"
        }
    }

    /**
     * Get the corresponding display time for a swatch.
     * If the swatch is equal to [Palette.getDominantSwatch] we typically display it longer.
     */
    fun getSwatchDisplayTime(swatch: Palette.Swatch): Long {
        return scaleToIntensity(currentPalette?.let {
            if (swatch == it.dominantSwatch) {
                DOMINANT_DISPLAY_TIME_MS
            } else {
               NON_DOMINANT_DISPLAY_TIME_MS
            }
        } ?: NON_DOMINANT_DISPLAY_TIME_MS)
    }

    private fun scaleToIntensity(midPoint: Long): Long {
        val maxDelay = midPoint * 2
        return (((frequency * (maxDelay)) / MAX_FREQUENCY) - maxDelay).absoluteValue.toLong()
    }

    companion object {
        private const val DOMINANT_DISPLAY_TIME_MS = 10000L
        private const val NON_DOMINANT_DISPLAY_TIME_MS = 2500L
        const val MAX_FREQUENCY = 10
        const val FREQUENCY_STEP_COUNT = 1
        const val MIN_FREQUENCY = 1
        const val INITIAL_FREQUENCY = 5
    }
}