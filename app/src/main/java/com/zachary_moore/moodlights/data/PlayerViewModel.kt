package com.zachary_moore.moodlights.data

import android.graphics.Color
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.zachary_moore.huekontrollr.Kontrollr
import com.zachary_moore.huekontrollr.data.light.Light
import com.zachary_moore.huekontrollr.data.light.State
import com.zachary_moore.huekontrollr.util.getFromRGB

class PlayerViewModel : ViewModel() {

    val spotifyFeature = SpotifyFeature()
    val hueDiscoveryFeature = HueDiscoveryFeature()
    private val swatchStateManager = SwatchStateManager()
    private val threadingManager = ThreadingManager()

    private val kontrollrData = Transformations.distinctUntilChanged(hueDiscoveryFeature.getKontrollr())
    private val palette: LiveData<Palette> = Transformations.map(spotifyFeature.currentPlayingAlbumImage()) {
        Palette.from(it).generate()
    }
    private val lightMapAndPalette: LiveData<Pair<Map<Int, Light>?, Palette?>>
    private val lightMapAndPausedState: LiveData<Pair<Map<Int, Light>?, Boolean?>>
    private val lightMap: MutableLiveData<Map<Int, Light>> = MutableLiveData()

    private val isPausedObserver: Observer<Pair<Map<Int, Light>?, Boolean?>> = getIsPausedObserver()
    private val paletteObserver: Observer<Pair<Map< Int, Light>?, Palette?>> = getPaletteObserver()
    private val kontrollrObserver = Observer<Kontrollr> { kontrollr ->
        kontrollr?.lights?.getAll {
            // fuel is used internally in HueKontrollr, this makes requests on a background thread
            // need to post the value instead of set
            lightMap.postValue(it)
        }
    }

    init {
        lightMapAndPalette = getLightMapAndPaletteLiveData(lightMap)
        lightMapAndPausedState = getLightMapAndPausedStateLiveData(lightMap)

        lightMapAndPausedState.observeForever(isPausedObserver)
        lightMapAndPalette.observeForever(paletteObserver)
        kontrollrData.observeForever(kontrollrObserver)
    }

    /**
     * Zip together a [LiveData] for a batch get result of hue lights from [Kontrollr.lights]
     * and a [LiveData] of a [Palette] derived from [SpotifyFeature.currentPlayingAlbumImage]
     */
    private fun getLightMapAndPaletteLiveData(
        lightMapLiveData: LiveData<Map<Int, Light>>
    ): LiveData<Pair<Map<Int, Light>?, Palette?>> = Transformations.distinctUntilChanged(
        MediatorLiveData<Pair<Map<Int, Light>?, Palette?>>().apply {
            addSource(lightMapLiveData) { updatedMap ->
                if (updatedMap != null) {
                    value = value?.let {
                        updatedMap to it.second
                    } ?: updatedMap to null
                }
            }

            addSource(palette) { updatedPalette ->
                if (updatedPalette != null) {
                    value = value?.let {
                        it.first to updatedPalette
                    } ?: null to updatedPalette
                }
            }
        }
    )

    /**
     * Zip together a [LiveData] for a batch get result of hue lights from [Kontrollr.lights]
     * and a [LiveData] of user pause state derived from [SpotifyFeature.isPaused]
     */
    private fun getLightMapAndPausedStateLiveData(
        lightMapLiveData: LiveData<Map<Int, Light>>
    ): LiveData<Pair<Map<Int, Light>?, Boolean?>> = Transformations.distinctUntilChanged(
            MediatorLiveData<Pair<Map<Int, Light>?, Boolean?>>().apply {
                addSource(lightMapLiveData) { updatedMap ->
                    if (updatedMap != null) {
                        value = value?.let {
                            updatedMap to it.second
                        } ?: updatedMap to null
                    }
                }

                addSource(spotifyFeature.isPaused()) { updatedBoolean ->
                    value = value?.let {
                        Pair(it.first, updatedBoolean == true)
                    } ?: Pair(null, updatedBoolean == true)
                }
            }
        )

    override fun onCleared() {
        super.onCleared()
        shutdownLights()
        spotifyFeature.cleanup()
        threadingManager.cleanUp()
        swatchStateManager.reset()
        lightMapAndPausedState.removeObserver(isPausedObserver)
        lightMapAndPalette.removeObserver(paletteObserver)
        kontrollrData.removeObserver(kontrollrObserver)
    }

    fun updateSliderValue(updatedValue: Float) {
        swatchStateManager.modifyFrequency(updatedValue)
    }

    /**
     * Get an observer that listens to a map of lightIds to lights and a [Palette]
     *
     * This observer will setup our [SwatchStateManager] and then post a looping [Runnable]
     * to a background thread which will update lights based on [Palette.mSwatches]
     */
    private fun getPaletteObserver() = Observer<Pair<Map<Int, Light>?, Palette?>> {
        if (it.first != null && it.second != null) {
            // avoid bang operator but need to double null check due to smart casting issues
            // avoid nesting with double lets
            val lightIds = requireNotNull(it.first).keys
            val palette = requireNotNull(it.second)

            // If there is an existing looping task, kill it
            threadingManager.cleanUp()

            // load swatch manager
            swatchStateManager.loadPalette(palette)

            // Start panning lights on a background thread
            threadingManager.startLightTask(Runnable {
                val swatch = swatchStateManager.getNextSwatch()
                updateLights(
                    lightIds,
                    swatch
                )

                threadingManager.postDelayedCurrentLightTask(
                    swatchStateManager.getSwatchDisplayTime(swatch)
                )
            })
        }
    }

    /**
     * Simply call [applySwatchToLight] on all [lightIds]
     */
    private fun updateLights(
        lightIds: Set<Int>,
        swatch: Palette.Swatch
    ) {
        lightIds.forEach { lightId ->
            applySwatchToLight(swatch, lightId)
        }
    }

    /**
     * Take a [swatch], convert it to rgb, then feed to [getFromRGB] and [Kontrollr] to update
     * a light with a specified [lightId]
     */
    private fun applySwatchToLight(
        swatch: Palette.Swatch,
        lightId: Int
    ) {
        val rgb = swatch.rgb
        val r = Color.red(rgb)
        val g = Color.green(rgb)
        val b = Color.blue(rgb)
        kontrollrData.value?.lights?.putState(
            lightId,
            State(xy = getFromRGB(r, g, b))
        )
    }

    /**
     * Get an observer that listens to a map of lightIds to lights and a boolean indicating if music
     * is paused or not.
     *
     * This will turn on lights when music is not paused.
     * This will turn off lights when music is paused.
     */
    private fun getIsPausedObserver() = Observer<Pair<Map<Int, Light>?, Boolean?>> {
        if (it.first != null && it.second != null) {
            // avoid bang operator but need to double null check due to smart casting issues
            // avoid nesting with double lets
            val isPaused = requireNotNull(it.second)
            val lightMap = requireNotNull(it.first)

            if (isPaused) {
                threadingManager.pauseLightTasks()
            } else {
                threadingManager.resumeLightTasks()
            }

            // Turn on or off lights according to new pause state
            lightMap.keys.forEach { key ->
                kontrollrData.value?.lights?.putState(
                    key,
                    State(on = !isPaused)
                )
            }
        }
    }

    /**
     * Shutdown lights.
     * This fetches lights as well as shuts them down and should only be used in
     * a hard shutdown to guarantee all lights are shutdown.
     */
    private fun shutdownLights() {
        kontrollrData.value?.lights?.getAll { lightMap ->
            lightMap.keys.forEach { lightId ->
                kontrollrData.value?.lights?.putState(
                    lightId,
                    State(on = false)
                )
            }
        }
    }
}