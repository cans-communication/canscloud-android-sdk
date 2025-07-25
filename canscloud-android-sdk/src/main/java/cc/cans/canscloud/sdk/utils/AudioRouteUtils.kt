/*
 * Copyright (c) 2010-2021 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cc.cans.canscloud.sdk.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.telecom.CallAudioState
import androidx.annotation.Keep
import androidx.core.app.ActivityCompat
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.tools.Log
import cc.cans.canscloud.sdk.telecom.TelecomHelper

class AudioRouteUtils {
    @Keep
    companion object {
        private fun applyAudioRouteChange(
            call: Call?,
            types: List<AudioDevice.Type>,
            output: Boolean = true
        ) {
            val currentCall = if (cansCenter().core.callsNb > 0) {
                call ?:  cansCenter().core.currentCall ?:  cansCenter().core.calls[0]
            } else {
                Log.w("[Audio Route Helper] No call found, setting audio route on Core")
                null
            }
            val capability = if (output) {
                AudioDevice.Capabilities.CapabilityPlay
            } else {
                AudioDevice.Capabilities.CapabilityRecord
            }
            val preferredDriver = if (output) {
                cansCenter().core.defaultOutputAudioDevice?.driverName
            } else {
                cansCenter().core.defaultInputAudioDevice?.driverName
            }

            val extendedAudioDevices =  cansCenter().core.extendedAudioDevices
            Log.i(
                "[Audio Route Helper] Looking for an ${if (output) "output" else "input"} audio device with capability [$capability], driver name [$preferredDriver] and type [$types] in extended audio devices list (size ${extendedAudioDevices.size})"
            )
            val foundAudioDevice = extendedAudioDevices.find {
                Log.w("[AudioRouteHelperDevice:] [${it.type}]")
                it.driverName == preferredDriver && types.contains(it.type) && it.hasCapability(
                    capability
                )
            }
            val audioDevice = if (foundAudioDevice == null) {
                Log.w(
                    "[Audio Route Helper] Failed to find an audio device with capability [$capability], driver name [$preferredDriver] and type [$types]"
                )
                extendedAudioDevices.find {
                    types.contains(it.type) && it.hasCapability(capability)
                }
            } else {
                foundAudioDevice
            }

            if (audioDevice == null) {
                Log.e(
                    "[Audio Route Helper] Couldn't find audio device with capability [$capability] and type [$types]"
                )
                for (device in extendedAudioDevices) {
                    // TODO: switch to debug?
                    Log.i(
                        "[Audio Route Helper] Extended audio device: [${device.deviceName} (${device.driverName}) ${device.type} / ${device.capabilities}]"
                    )
                }
                return
            }
            if (currentCall != null) {
                Log.i(
                    "[Audio Route Helper] Found [${audioDevice.type}] ${if (output) "playback" else "recorder"} audio device [${audioDevice.deviceName} (${audioDevice.driverName})], routing conference audio to it"
                )
                if (output) {
                    currentCall.outputAudioDevice = audioDevice
                } else {
                    currentCall.inputAudioDevice = audioDevice
                }
            } else {
                Log.i(
                    "[Audio Route Helper] Found [${audioDevice.type}] [${if (output) "playback" else "recorder"}] audio device [${audioDevice.deviceName} (${audioDevice.driverName})], changing core default audio device"
                )
                if (output) {
                    cansCenter().core.outputAudioDevice = audioDevice
                } else {
                    cansCenter().core.inputAudioDevice = audioDevice
                }
            }
        }

        private fun changeCaptureDeviceToMatchAudioRoute(call: Call?, types: List<AudioDevice.Type>) {
            when (types.first()) {
                AudioDevice.Type.Bluetooth -> {
                    if (isBluetoothAudioRecorderAvailable()) {
                        Log.i("[Audio Route Helper] Bluetooth device is able to record audio, also change input audio device")
                        applyAudioRouteChange(call, arrayListOf(AudioDevice.Type.Bluetooth), false)
                    }
                }
                AudioDevice.Type.Headset, AudioDevice.Type.Headphones -> {
                    if (isHeadsetAudioRecorderAvailable()) {
                        Log.i("[Audio Route Helper] Headphones/headset device is able to record audio, also change input audio device")
                        applyAudioRouteChange(call, (arrayListOf(AudioDevice.Type.Headphones, AudioDevice.Type.Headset)), false)
                    }
                }
                else -> {
                    Log.e("[AudioDevice] No type audio device")
                }
            }
        }

        fun routeAudio() {
            val audioManager = cansCenter().context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (isBluetoothAudioRouteCurrentlyUsed() && cansCenter().corePreferences.routeAudioToBluetoothIfAvailable) {
                val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter.isEnabled) {
                    if (ActivityCompat.checkSelfPermission(
                            cansCenter().context,
                            Manifest.permission.BLUETOOTH_CONNECT,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val connectedDevices =
                            bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                        if (connectedDevices == BluetoothProfile.STATE_CONNECTED) {
                            println("[bluetoothAdapter] Audio devices list updated ${bluetoothAdapter.name}")
                            audioManager.startBluetoothSco()
                            audioManager.isBluetoothScoOn = true
                        } else {
                            if (isHeadsetAudioRouteAvailable()) {
                                routeAudioToHeadset()
                            }
                            println("[bluetoothAdapter] Audio devices list updated no connect")
                        }
                    }
                }
            } else if (isHeadsetAudioRouteAvailable()) {
                routeAudioToHeadset()
            }
        }

        private fun routeAudioTo(
            call: Call?,
            types: List<AudioDevice.Type>,
            skipTelecom: Boolean = false
        ) {
            val currentCall = call ?:  cansCenter().core.currentCall ?:  cansCenter().core.calls.firstOrNull()
            if (currentCall != null && !skipTelecom && TelecomHelper.singletonHolder().exists()) {
                Log.i(
                    "[Audio Route Helper] Call provided & Telecom Helper exists, trying to dispatch audio route change through Telecom API"
                )
                val connection = TelecomHelper.singletonHolder().get().findConnectionForCallId(
                    currentCall.callLog.callId.orEmpty()
                )
                if (connection != null) {
                    val route = when (types.first()) {
                        AudioDevice.Type.Earpiece -> CallAudioState.ROUTE_EARPIECE
                        AudioDevice.Type.Speaker -> CallAudioState.ROUTE_SPEAKER
                        AudioDevice.Type.Headphones, AudioDevice.Type.Headset -> CallAudioState.ROUTE_WIRED_HEADSET
                        AudioDevice.Type.Bluetooth, AudioDevice.Type.BluetoothA2DP, AudioDevice.Type.HearingAid -> CallAudioState.ROUTE_BLUETOOTH
                        else -> CallAudioState.ROUTE_WIRED_OR_EARPIECE
                    }
                    Log.i(
                        "[Audio Route Helper] Telecom Helper & matching connection found, dispatching audio route change through it"
                    )
                    // We will be called here again by NativeCallWrapper.onCallAudioStateChanged()
                    // but this time with skipTelecom = true
                    if (!Compatibility.changeAudioRouteForTelecomManager(connection, route)) {
                        Log.w(
                            "[Audio Route Helper] Connection is already using this route internally, make the change!"
                        )
                        applyAudioRouteChange(currentCall, types)
                        changeCaptureDeviceToMatchAudioRoute(currentCall, types)
                    }
                } else {
                    Log.w("[Audio Route Helper] Telecom Helper found but no matching connection!")
                    applyAudioRouteChange(currentCall, types)
                    changeCaptureDeviceToMatchAudioRoute(currentCall, types)
                }
            } else {
                applyAudioRouteChange(call, types)
                changeCaptureDeviceToMatchAudioRoute(call, types)
            }
        }

        fun routeAudioToEarpiece(call: Call? = null, skipTelecom: Boolean = false) {
            routeAudioTo(call, arrayListOf(AudioDevice.Type.Earpiece), skipTelecom)
        }

        fun routeAudioToSpeaker(call: Call? = null, skipTelecom: Boolean = false) {
            routeAudioTo(call, arrayListOf(AudioDevice.Type.Speaker), skipTelecom)
        }

        fun routeAudioToBluetooth(call: Call? = null, skipTelecom: Boolean = false) {
            routeAudioTo(
                call,
                arrayListOf(AudioDevice.Type.Bluetooth, AudioDevice.Type.HearingAid),
                skipTelecom
            )
        }

        fun routeAudioToHeadset(call: Call? = null, skipTelecom: Boolean = false) {
            routeAudioTo(
                call,
                arrayListOf(AudioDevice.Type.Headphones, AudioDevice.Type.Headset),
                skipTelecom
            )
        }

        fun isSpeakerAudioRouteCurrentlyUsed(call: Call? = null): Boolean {
            val currentCall = if ( cansCenter().core.callsNb > 0) {
                call ?:  cansCenter().core.currentCall ?:  cansCenter().core.calls[0]
            } else {
                Log.w("[Audio Route Helper] No call found, checking audio route on Core")
                null
            }

            val audioDevice = if (call != null) {
                call.outputAudioDevice
            } else if (currentCall != null) {
                currentCall.outputAudioDevice
            } else {
                cansCenter().core.outputAudioDevice
            }

            if (audioDevice == null) return false
            Log.i(
                "[Audio Route Helper] Playback audio device currently in use is [${audioDevice.deviceName} (${audioDevice.driverName}) ${audioDevice.type}]"
            )
            return audioDevice.type == AudioDevice.Type.Speaker
        }

        fun isBluetoothAudioRouteCurrentlyUsed(call: Call? = null): Boolean {
            if (cansCenter().core.callsNb == 0) {
                Log.w("[Audio Route Helper] No call found, so bluetooth audio route isn't used")
                return false
            }
            val currentCall = call ?: cansCenter().core.currentCall ?: cansCenter().core.calls[0]

            val audioDevice = if (call != null) {
                call.outputAudioDevice
            } else {
                currentCall.outputAudioDevice
            }

            if (audioDevice == null) return false
            Log.i(
                "[Audio Route Helper] Playback audio device currently in use is [${audioDevice.deviceName} (${audioDevice.driverName}) ${audioDevice.type}]"
            )
            return audioDevice.type == AudioDevice.Type.Bluetooth
        }

        fun isBluetoothAudioRouteAvailable(): Boolean {
            for (audioDevice in cansCenter().core.audioDevices) {
                if ((audioDevice.type == AudioDevice.Type.Bluetooth || audioDevice.type == AudioDevice.Type.HearingAid) &&
                    audioDevice.hasCapability(AudioDevice.Capabilities.CapabilityPlay)
                ) {
                    Log.i(
                        "[Audio Route Helper] Found bluetooth audio device [${audioDevice.deviceName} (${audioDevice.driverName})]"
                    )
                    return true
                }
            }
            return false
        }

        private fun isBluetoothAudioRecorderAvailable(): Boolean {
            for (audioDevice in cansCenter().core.audioDevices) {
                if ((audioDevice.type == AudioDevice.Type.Bluetooth || audioDevice.type == AudioDevice.Type.HearingAid) &&
                    audioDevice.hasCapability(AudioDevice.Capabilities.CapabilityRecord)
                ) {
                    Log.i(
                        "[Audio Route Helper] Found bluetooth audio recorder [${audioDevice.deviceName} (${audioDevice.driverName})]"
                    )
                    return true
                }
            }
            return false
        }

        fun isHeadsetAudioRouteAvailable(): Boolean {
            for (audioDevice in cansCenter().core.audioDevices) {
                if ((audioDevice.type == AudioDevice.Type.Headset || audioDevice.type == AudioDevice.Type.Headphones) &&
                    audioDevice.hasCapability(AudioDevice.Capabilities.CapabilityPlay)
                ) {
                    Log.i(
                        "[Audio Route Helper] Found headset/headphones audio device [${audioDevice.deviceName} (${audioDevice.driverName})]"
                    )
                    return true
                }
            }
            return false
        }

        private fun isHeadsetAudioRecorderAvailable(): Boolean {
            for (audioDevice in cansCenter().core.audioDevices) {
                if ((audioDevice.type == AudioDevice.Type.Headset || audioDevice.type == AudioDevice.Type.Headphones) &&
                    audioDevice.hasCapability(AudioDevice.Capabilities.CapabilityRecord)
                ) {
                    Log.i(
                        "[Audio Route Helper] Found headset/headphones audio recorder [${audioDevice.deviceName} (${audioDevice.driverName})]"
                    )
                    return true
                }
            }
            return false
        }
    }
}
