package com.zachary_moore.moodlights.view.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.zachary_moore.moodlights.R
import com.zachary_moore.moodlights.data.PlayerViewModel
import com.zachary_moore.moodlights.databinding.PlayerFragmentBinding
import com.zachary_moore.moodlights.view.ads.AdManager
import com.zachary_moore.moodlights.view.ads.AdManager.Companion.addBannerAd


class PlayerFragment: Fragment() {

    private lateinit var playerFragmentBinding: PlayerFragmentBinding
    private lateinit var playerViewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerViewModel = ViewModelProvider(this).get()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        playerFragmentBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.player_fragment,
            container,
            false
        )

        playerFragmentBinding.moodlightBannerAdContainer.addBannerAd(requireActivity())

        return playerFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerFragmentBinding.lifecycleOwner = viewLifecycleOwner
        playerFragmentBinding.viewModel = playerViewModel

        // listen to slider and post updates to viewModel
        playerFragmentBinding.moodlightFrequencySlider.addOnChangeListener { _, value, _ ->
            playerViewModel.updateSliderValue(value)
        }

        // start listening to pause state and conditionally show an interstitial
        playerViewModel.spotifyFeature.isPaused().observe(viewLifecycleOwner, Observer { isPaused ->
            if (isPaused && playerViewModel.adManager.shouldShowPauseInterstitial()) {
                AdManager.showInterstitialAd(requireActivity())
            }
        })
    }

    override  fun onStart() {
        super.onStart()
        playerViewModel.spotifyFeature.disconnectRemote()
        playerViewModel.spotifyFeature.initializeRemote(requireContext())
        playerViewModel.hueDiscoveryFeature.loadSharedPreferences(requireContext())
    }

    override fun onStop() {
        super.onStop()
        playerViewModel.spotifyFeature.disconnectRemote()
    }
}