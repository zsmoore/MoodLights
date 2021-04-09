package com.zachary_moore.moodlights.view.ads

import android.app.Activity
import android.util.DisplayMetrics
import android.view.Display
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.zachary_moore.moodlights.BuildConfig

class AdManager {

    private var pausesSinceLastAd = 0

    fun shouldShowPauseInterstitial(): Boolean {
        pausesSinceLastAd += 1
        return (pausesSinceLastAd > PAUSE_AD_THRESHOLD).also { willShowAd ->
            if (willShowAd) {
                pausesSinceLastAd = 0
            }
        }
    }

    companion object {
        private const val PAUSE_AD_THRESHOLD = 2

        fun ViewGroup.addBannerAd(
            activity: Activity
        ) {
            MobileAds.initialize(activity)

            val adView = AdView(activity)
            adView.adSize = getAdSize(activity)
            adView.adUnitId = BuildConfig.playerFragmentBannerAdId
            adView.loadAd(AdRequest.Builder().build())
            this.removeAllViews()
            this.addView(adView)
        }

        private fun getAdSize(activity: Activity): AdSize {
            // use deprecated methods due to sdk requirements
            val display: Display = activity.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics().also { display.getMetrics(it) }

            val adWidth = (outMetrics.widthPixels.toFloat() / outMetrics.density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }

        fun showInterstitialAd(activity: Activity) {
            InterstitialAd.load(
                activity,
                BuildConfig.playerFragmentPauseInterstitialAdId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        interstitialAd.show(activity)
                    }
                }
            )
        }
    }
}