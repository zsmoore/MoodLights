package com.zachary_moore.moodlights.view.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.zachary_moore.moodlights.R
import com.zachary_moore.moodlights.data.PlayerViewModel
import com.zachary_moore.moodlights.databinding.PlayerFragmentBinding

class PlayerFragment: Fragment() {

    private lateinit var playerFragmentBinding: PlayerFragmentBinding
    private lateinit var playerViewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerViewModel = ViewModelProvider(this).get()
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(playerViewModel.spotifyFeature.foregroundRefreshListener)
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
        return playerFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerFragmentBinding.lifecycleOwner = viewLifecycleOwner
        playerFragmentBinding.viewModel = playerViewModel
        playerFragmentBinding.moodlightFrequencySlider.addOnChangeListener { _, value, _ ->
            playerViewModel.updateSliderValue(value)
        }
    }

    override fun onStart() {
        super.onStart()
        playerViewModel.spotifyFeature.disconnectRemote()
        playerViewModel.spotifyFeature.initializeRemote(requireContext())
        playerViewModel.hueDiscoveryFeature.loadSharedPreferences(requireContext())
    }

    override fun onStop() {
        super.onStop()
        playerViewModel.spotifyFeature.disconnectRemote()
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get()
            .lifecycle
            .removeObserver(playerViewModel.spotifyFeature.foregroundRefreshListener)
    }
}