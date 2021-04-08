package com.zachary_moore.moodlights.data

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zachary_moore.huekontrollr.Kontrollr
import com.zachary_moore.moodlights.R

class HueDiscoveryFeature {

    private val kontrollrLiveData: MutableLiveData<Kontrollr> = MutableLiveData()

    fun getKontrollr(): LiveData<Kontrollr> = kontrollrLiveData

    fun loadSharedPreferences(context: Context) {
        // Avoid unnecessary calls and just return if we already have a value
        if (kontrollrLiveData.value != null) {
            return
        }

        val sharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCES_FILE,
                Context.MODE_PRIVATE
        )

        val ip = sharedPreferences.getString(IP_KEY, null)
        val username = sharedPreferences.getString(USERNAME_KEY, null)

        val alertDialog = AlertDialog.Builder(context)
                .setTitle(R.string.moodlight_press_bridge_title)
                .setMessage(R.string.moodlight_press_bridge_description)
                .setNegativeButton(R.string.moodlight_press_bridge_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
        if (ip == null && username == null) {
            alertDialog.setPositiveButton(R.string.moodlight_press_bridge_continue) { dialog, _ ->
                Kontrollr.createWithAutoIpAndUsername(
                        APPLICATION_NAME,
                        DEVICE_NAME
                ) { kontrollr ->
                    sharedPreferences.edit().putString(IP_KEY, kontrollr.bridgeIpAddress).apply()
                    sharedPreferences.edit().putString(USERNAME_KEY, kontrollr.userName).apply()
                    kontrollrLiveData.postValue(kontrollr)
                    dialog.dismiss()
                }
            }
            alertDialog.show()
        } else if (username == null && ip != null) {
            alertDialog.setPositiveButton(R.string.moodlight_press_bridge_continue) { dialog, _ ->
                Kontrollr.createWithIpAndAutoCreateUsername(
                        ip,
                        APPLICATION_NAME,
                        DEVICE_NAME
                ) { kontrollr ->
                    sharedPreferences.edit().putString(IP_KEY, kontrollr.bridgeIpAddress).apply()
                    sharedPreferences.edit().putString(USERNAME_KEY, kontrollr.userName).apply()
                    kontrollrLiveData.postValue(kontrollr)
                    dialog.dismiss()
                }
            }
            alertDialog.show()
        } else if (username != null && ip != null) {
            kontrollrLiveData.postValue(Kontrollr.createFromIpAndUser(ip, username))
        }
    }

    fun resetSavedDiscoveryCredentials(context: Context) {
        AlertDialog.Builder(context)
                .setTitle(R.string.moodlight_reset_bridge_title)
                .setMessage(R.string.moodlight_reset_brdige_description)
                .setPositiveButton(R.string.moodlight_reset_bridge_reset) { dialog, _ ->
                    dialog.dismiss()
                    doResetDiscoveryCredentials(context)
                }
                .setNegativeButton(R.string.moodlight_reset_bridge_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
    }

    private fun doResetDiscoveryCredentials(context: Context) {
        val sharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCES_FILE,
                Context.MODE_PRIVATE
        )

        sharedPreferences.edit().remove(USERNAME_KEY).apply()
        sharedPreferences.edit().remove(IP_KEY).apply()
        kontrollrLiveData.postValue(null)

        AlertDialog.Builder(context)
                .setTitle(R.string.moodlight_saved_bridge_cleared_title)
                .setMessage(R.string.moodlight_saved_bridge_cleared_description)
                .setPositiveButton(R.string.moodlight_saved_bridge_cleared_ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
    }

    companion object {
        private const val SHARED_PREFERENCES_FILE = "HueDiscoveryInformation"
        private const val IP_KEY = "HueIpKey"
        private const val USERNAME_KEY = "HueUsernameKey"
        private const val APPLICATION_NAME = "MoodLights"
        private const val DEVICE_NAME = "MoodLights_Android"
    }
}