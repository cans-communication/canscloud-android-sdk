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
package cc.cans.canscloud.sdk.telecom

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.StatusHints
import cc.cans.canscloud.sdk.R
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.utils.AudioRouteUtils
import org.linphone.core.Call
import org.linphone.core.tools.Log

@TargetApi(29)
class NativeCallWrapper(var callId: String) : Connection() {
    private var ringtonePlayer: MediaPlayer? = null
    init {
        val capabilities = connectionCapabilities or CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD or CAPABILITY_HOLD
        connectionCapabilities = capabilities
        audioModeIsVoip = true
        statusHints = StatusHints(
            "",
            Icon.createWithResource(cansCenter().context, R.drawable.ic_launcher_foreground),
            Bundle()
        )
    }

    override fun onStateChanged(state: Int) {
        Log.i("[Connection] Telecom state changed [$state] for call with id: $callId")
        super.onStateChanged(state)

        // Start ringing when state changes to RINGING
        if (state == STATE_RINGING) {
            playRingtone()
        }
        // Stop ringing if we move past the ringing phase
        else if (state != STATE_INITIALIZING) {
            stopRingtone()
        }
    }

    override fun onAnswer(videoState: Int) {
        Log.i("[Connection] Answering telecom call with id: $callId")
        stopRingtone()
        getCall()?.accept() ?: selfDestroy()
    }

    override fun onHold() {
        Log.i("[Connection] Pausing telecom call with id: $callId")
        getCall()?.pause() ?: selfDestroy()
        setOnHold()
    }

    override fun onUnhold() {
        Log.i("[Connection] Resuming telecom call with id: $callId")
        getCall()?.resume() ?: selfDestroy()
        setActive()
    }

    override fun onCallAudioStateChanged(state: CallAudioState) {
        Log.i("[Connection] Audio state changed: $state")

        val call = getCall()
        if (call != null) {
            call.microphoneMuted = state.isMuted
            when (state.route) {
                CallAudioState.ROUTE_EARPIECE -> AudioRouteUtils.routeAudioToEarpiece(call, true)
                CallAudioState.ROUTE_SPEAKER -> AudioRouteUtils.routeAudioToSpeaker(call, true)
                CallAudioState.ROUTE_BLUETOOTH -> AudioRouteUtils.routeAudioToBluetooth(call, true)
                CallAudioState.ROUTE_WIRED_HEADSET -> AudioRouteUtils.routeAudioToHeadset(call, true)
            }
        } else {
            selfDestroy()
        }
    }

    override fun onPlayDtmfTone(c: Char) {
        Log.i("[Connection] Sending DTMF [$c] in telecom call with id: $callId")
        getCall()?.sendDtmf(c) ?: selfDestroy()
    }

    override fun onDisconnect() {
        Log.i("[Connection] Terminating telecom call with id: $callId")
        stopRingtone()
        getCall()?.terminate() ?: selfDestroy()
    }

    override fun onAbort() {
        Log.i("[Connection] Aborting telecom call with id: $callId")
        stopRingtone()
        getCall()?.terminate() ?: selfDestroy()
    }

    override fun onReject() {
        Log.i("[Connection] Rejecting telecom call with id: $callId")
        stopRingtone()
        getCall()?.terminate() ?: selfDestroy()
    }

    override fun onSilence() {
        Log.i("[Connection] Call with id: $callId asked to be silenced")
        stopRingtone()
        cansCenter().core.stopRinging()
    }

    @SuppressLint("ServiceCast")
    private fun playRingtone() {
        if (ringtonePlayer != null) return
        cansCenter().core.stopRinging()

        try {
            val context = cansCenter().context
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            val isBluetoothConnected = audioManager.isBluetoothA2dpOn ||
                    audioManager.isBluetoothScoOn ||
                    (this.callAudioState?.supportedRouteMask?.and(android.telecom.CallAudioState.ROUTE_BLUETOOTH) != 0) ||
                    AudioRouteUtils.isBluetoothAudioRouteAvailable()

            Log.i("[Connection]", "isBluetoothConnected : $isBluetoothConnected")

            if (isBluetoothConnected) {
                try {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                } catch (e: Exception) {
                    Log.e("[Connection]", "Failed to force SCO for ringtone: ${e.message}")
                }
            }

            ringtonePlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)

                val attributes = AudioAttributes.Builder().apply {
                    if (isBluetoothConnected) {
                        Log.i("[Connection]", "Bluetooth active: Routing ringtone to SCO Voice Channel")
                        setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    } else {
                        Log.i("[Connection]", "No Bluetooth: Routing ringtone to Loud Speaker")
                        setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    }
                }.build()

                setAudioAttributes(attributes)
                isLooping = true
                setOnPreparedListener { mp ->
                    if (isBluetoothConnected) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                if (ringtonePlayer != null && !mp.isPlaying) {
                                    mp.start()
                                }
                            } catch (e: Exception) {
                                Log.e("[Connection]", "Error starting delayed ringtone: ${e.message}")
                            }
                        }, 2000)
                    } else {
                        mp.start()
                    }
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("[Connection]", "Error playing ringtone: ${e.message}")
        }
    }

    private fun stopRingtone() {
        try {
            if (ringtonePlayer?.isPlaying == true) {
                ringtonePlayer?.stop()
            }
            ringtonePlayer?.release()
            ringtonePlayer = null
        } catch (e: Exception) {
            Log.e("[Connection] Error stopping ringtone: ${e.message}")
        }
    }

    private fun getCall(): Call? {
        return cansCenter().core.getCallByCallid(callId)
    }

    private fun selfDestroy() {
        if (cansCenter().core.callsNb == 0) {
            Log.e("[Connection] No call in Core, destroy connection")
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()
        }
    }
}
