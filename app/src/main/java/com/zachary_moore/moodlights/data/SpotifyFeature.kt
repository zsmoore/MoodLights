package com.zachary_moore.moodlights.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.Track
import com.zachary_moore.moodlights.BuildConfig

class SpotifyFeature {
    private val appRemote: MutableLiveData<SpotifyAppRemote> = MutableLiveData()
    private val isPaused: MutableLiveData<Boolean> = MutableLiveData()
    private val currentTrack: MutableLiveData<Track> = MutableLiveData()
    private val currentPlayingAlbumBitmap: MutableLiveData<Bitmap> = MutableLiveData()

    // Expose direct maps since they are immutable
    private val currentPlayingAlbumImage = Transformations.distinctUntilChanged(
        Transformations.map(currentTrack) {
            it.imageUri
        }
    )
    val currentTrackName = Transformations.distinctUntilChanged(
        Transformations.map(currentTrack) {
            it.name
        }
    )
    val currentArtistName = Transformations.distinctUntilChanged(
        Transformations.map(currentTrack) {
            it.artist.name
        }
    )
    val currentAlbumName = Transformations.distinctUntilChanged(
        Transformations.map(currentTrack) {
            it.album.name
        }
    )

    private val connectionParams = ConnectionParams.Builder(BuildConfig.spotifyClientId)
        .setRedirectUri(BuildConfig.spotifyRedirectUri)
        .showAuthView(true)
        .build()

    private val appRemoteObserver: Observer<SpotifyAppRemote> = getSpotifyAppRemoteObserver()
    private val currentAlbumImageObserver: Observer<ImageUri> = getCurrentPlayingAlbumImageObserver()

    init {
        appRemote.observeForever(appRemoteObserver)
        currentPlayingAlbumImage.observeForever(currentAlbumImageObserver)
    }

    fun cleanup() {
        appRemote.removeObserver(appRemoteObserver)
        currentPlayingAlbumImage.removeObserver(currentAlbumImageObserver)
    }

    fun isPaused(): LiveData<Boolean> = Transformations.distinctUntilChanged(isPaused)

    fun currentPlayingAlbumImage(): LiveData<Bitmap> = Transformations.distinctUntilChanged(currentPlayingAlbumBitmap)

    /**
     * Toggle pause / resume
     *
     * This method is assumed to be called after initial play instantiation and is not livedata
     * aware
     */
    fun togglePlay() {
        isPaused.value?.let { isPausedValue ->
            checkNotNull(appRemote.value) {
                "Attempting to access AppRemote before it exists"
            }.let { appRemote ->
                if (isPausedValue) {
                    appRemote.playerApi.resume()
                } else {
                    appRemote.playerApi.pause()
                }
            }
        }
    }

    /**
     * Start next song
     *
     * This method is assumed to be called after initial play instantiation and is not livedata
     * aware
     */
    fun next() {
        checkNotNull(appRemote.value) {
            "Attempting to access AppRemote before it exists"
        }.playerApi.skipNext()
    }

    /**
     * Start previous song
     *
     * This method is assumed to be called after initial play instantiation and is not livedata
     * aware
     */
    fun previous() {
        checkNotNull(appRemote.value) {
            "Attempting to access AppRemote before it exists"
        }.playerApi.skipPrevious()
    }

    /**
     * Initialize Spotify remote and setup connector callback
     */
    fun initializeRemote(context: Context) {
        SpotifyAppRemote.connect(
            context,
            connectionParams,
            object : Connector.ConnectionListener {
                override fun onFailure(throwable: Throwable?) {
                    Log.e(
                        TAG,
                        throwable?.message,
                        throwable
                    )
                }

                override fun onConnected(appRemote: SpotifyAppRemote) {
                    this@SpotifyFeature.appRemote.value = appRemote
                }

            }
        )
    }

    fun disconnectRemote() {
        appRemote.value?.let(SpotifyAppRemote::disconnect)
    }

    /**
     * Observer for a [SpotifyAppRemote].
     *
     * This will subscribe to the spotify player api, and on state changed dispatch updates to
     * [isPaused] for current pause state of user and [currentTrack] for the user's
     * current playing track.  This will trigger a bunch of side transformations to happen on
     * chained livedatas
     */
    private fun getSpotifyAppRemoteObserver() = Observer<SpotifyAppRemote> { appRemote ->
        appRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            // We are on a background thread, need to post values
            isPaused.postValue(playerState.isPaused)
            playerState.track?.let { currentTrack.postValue(it) }
        }
    }

    /**
     * Observer for current playing album image.
     *
     * This will request down and get a bitmap based off of the uri.
     *
     * This function assumes app remote has a value rather than mediating with it
     */
    private fun getCurrentPlayingAlbumImageObserver() = Observer<ImageUri> { currentPlayingAlbumImage ->
        checkNotNull(appRemote.value) {
            "App remote doesn't exist after track found while trying to get bitmap for album image"
        }.let {
            it.imagesApi.getImage(currentPlayingAlbumImage).setResultCallback { resultBitmap ->
                currentPlayingAlbumBitmap.postValue(resultBitmap)
            }
        }
    }

    companion object {
        private const val TAG = "SpotifyFeature"
    }
}