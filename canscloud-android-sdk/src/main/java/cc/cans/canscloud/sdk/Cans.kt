package cc.cans.canscloud.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Vibrator
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CorePreferences
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.CansTransport
import cc.cans.canscloud.sdk.models.RegisterState
import cc.cans.canscloud.sdk.utils.CansUtils
import org.linphone.core.Address
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.ProxyConfig
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import android.util.Log
import cc.cans.canscloud.sdk.models.AudioState
import org.linphone.core.tools.compatibility.DeviceUtils

class Cans {

    companion object {
        lateinit var core: Core
        lateinit var callCans: Call
        lateinit var mVibrator: Vibrator

        @SuppressLint("StaticFieldLeak")
        lateinit var corePreferences: CorePreferences

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        private val notificationManager: NotificationManagerCompat by lazy {
            NotificationManagerCompat.from(context)
        }

        private var packageManager: PackageManager? = null
        private var packageName: String = ""
        private var listeners = mutableListOf<CansListenerStub>()

        val account: String
            get() {
                core.defaultAccount?.params?.identityAddress?.let {
                    return "${it.username}@${it.domain}:${it.port}"
                }
                return ""
            }

        val username: String
            get() {
                core.defaultAccount?.params?.identityAddress?.let {
                    return "${it.username}"
                }
                return ""
            }


        val domain: String
            get() {
                core.defaultAccount?.params?.identityAddress?.let {
                    return "${it.domain}"
                }
                return ""
            }

        val port: String
            get() {
                core.defaultAccount?.params?.identityAddress?.let {
                    return "${it.port}"
                }
                return ""
            }

        val destinationRemoteAddress: String
            get() {
                return callCans.remoteAddress.asStringUriOnly()
            }

        val destinationUsername: String
            get() {
                return callCans.remoteAddress.username ?: ""
            }

        val durationTime: Int?
            get() {
                val durationTime = core.currentCall?.duration
                return durationTime
            }

        val missedCallsCount: Int
            get() {
                return core.missedCallsCount
            }

        val countCalls: Int
            get() {
                val call = core.callsNb
                Log.i("[CansSDK]","getCountCalls : $call")
                return call
            }

        val isMicState: Boolean
            get() {
                return core.currentCall?.microphoneMuted == true
            }

        val isSpeakerState: Boolean
            get() {
                return isSpeakerAudio()
            }

//        val isBluetoothState: Boolean
//            get() {
//                return AudioRouteUtils.isBluetoothAudioRouteAvailable()
//            }

        private var coreListenerStub = object : CoreListenerStub() {
            override fun onRegistrationStateChanged(
                core: Core,
                cfg: ProxyConfig,
                state: RegistrationState,
                message: String
            ) {
                Log.i("[CansSDK]", "Registration state is $state: $message")
                if (state == RegistrationState.Ok) {
                    listeners.forEach { it.onRegistration(RegisterState.OK, message) }
                } else if (state == RegistrationState.Failed) {
                    listeners.forEach { it.onRegistration(RegisterState.FAIL, message) }
                }
            }

            override fun onCallStateChanged(
                core: Core,
                call: Call,
                state: Call.State?,
                message: String
            ) {
                // This function will be called each time a call state changes,
                // which includes new incoming/outgoing calls
                callCans = call
                mVibrator.cancel()

                when (state) {
                    Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                        vibrator()
                        listeners.forEach { it.onCallState(core, call, CallState.IncomingCall) }
                    }

                    Call.State.OutgoingInit -> {
                        listeners.forEach { it.onCallState(core, call, CallState.StartCall) }
                    }

                    Call.State.OutgoingProgress -> {
                        listeners.forEach { it.onCallState(core, call, CallState.CallOutgoing) }
                    }

                    Call.State.StreamsRunning -> {
                        listeners.forEach { it.onCallState(core, call, CallState.StreamsRunning) }
                    }

                    Call.State.Connected -> {
                        listeners.forEach { it.onCallState(core, call, CallState.Connected) }
                    }

                    Call.State.Error -> {
                        listeners.forEach { it.onCallState(core, call, CallState.Error) }
                    }

                    Call.State.End -> {
                        listeners.forEach { it.onCallState(core, call, CallState.CallEnd) }
                    }

                    Call.State.Released -> {
                        listeners.forEach { it.onCallState(core, call, CallState.MissCall) }
                    }

                    else -> {
                        listeners.forEach { it.onCallState(core, call, CallState.Unknown) }
                    }
                }
            }

            override fun onLastCallEnded(core: Core) {
                super.onLastCallEnded(core)
                if (!core.isMicEnabled) {
                    Log.w("[CansSDK]","Mic was muted in Core, enabling it back for next call")
                    core.isMicEnabled = true
                }
                mVibrator.cancel()
                listeners.forEach { it.onLastCallEnded() }
            }

            override fun onAudioDeviceChanged(core: Core, audioDevice: AudioDevice) {
                listeners.forEach { it.onAudioDeviceChanged() }
            }

            override fun onAudioDevicesListUpdated(core: Core) {
                Log.i("[CansSDK Controls]", "Audio devices list updated")
                listeners.forEach { it.onAudioDevicesListUpdated() }
            }
        }

        fun config(
            activity: Activity,
            packageManager: PackageManager,
            packageName: String
        ) {
            Companion.packageManager = packageManager
            Companion.packageName = packageName
            context = activity

            corePreferences = CorePreferences(activity)
            corePreferences.copyAssetsFromPackage()

            val config = Factory.instance().createConfigWithFactory(
                corePreferences.configPath,
                corePreferences.factoryConfigPath
            )
            corePreferences.config = config
            core = Factory.instance().createCoreWithConfig(config, activity)
            core.start()
            core.addListener(coreListenerStub)
            createNotificationChannels(context, notificationManager)

            core.ring = null
            core.isVibrationOnIncomingCallEnabled = true
            core.isNativeRingingEnabled = true
            mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        }

        private fun createNotificationChannels(
            context: Context,
            notificationManager: NotificationManagerCompat,
        ) {
            createMissedCallChannel(context, notificationManager)
            createIncomingCallChannel(context, notificationManager)
        }

        private fun createMissedCallChannel(
            context: Context,
            notificationManager: NotificationManagerCompat,
        ) {
            val id = context.getString(R.string.notification_channel_missed_call_id)
            val name = context.getString(R.string.notification_channel_missed_call_name)
            val description = context.getString(R.string.notification_channel_missed_call_name)
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
            channel.description = description
            channel.lightColor = context.getColor(R.color.notification_led_color)
            channel.enableVibration(true)
            channel.enableLights(true)
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }

        private fun createIncomingCallChannel(
            context: Context,
            notificationManager: NotificationManagerCompat,
        ) {
            val id = context.getString(R.string.notification_channel_incoming_call_id)
            val name = context.getString(R.string.notification_channel_incoming_call_name)
            val description = context.getString(R.string.notification_channel_incoming_call_name)
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            channel.description = description
            channel.lightColor = context.getColor(R.color.notification_led_color)
            channel.enableVibration(true)
            channel.enableLights(true)
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }

        fun register(
            username: String,
            password: String,
            domain: String,
            port: String,
            transport: CansTransport
        ) {
            if ((username != this.username) || (domain != this.domain)) {
                removeAccount()
                val serverAddress = "${domain}:${port}"
                val transportType = if (transport.name.lowercase() == "tcp") {
                    TransportType.Tcp
                } else {
                    TransportType.Udp
                }

                val authInfo = Factory.instance()
                    .createAuthInfo(username, null, password, null, null, serverAddress, null)

                val params = core.createAccountParams()
                val identity = Factory.instance().createAddress("sip:$username@$serverAddress")
                params.identityAddress = identity

                val address = Factory.instance().createAddress("sip:$serverAddress")
                address?.transport = transportType
                params.serverAddress = address
                params.isRegisterEnabled = true

                val createAccount = core.createAccount(params)
                core.addAuthInfo(authInfo)
                core.addAccount(createAccount)

                // Asks the CaptureTextureView to resize to match the captured video's size ratio
                //core.config.setBool("video", "auto_resize_preview_to_keep_ratio", true)

                core.defaultAccount = createAccount
                core.addListener(coreListenerStub)
                core.start()
            }
        }

        fun removeAccount() {
            core.defaultAccount?.let { account ->
                val authInfo = account.findAuthInfo()
                if (authInfo != null) {
                    Log.i("[CansSDK]","Found auth info $authInfo, removing it.")
                    core.removeAuthInfo(authInfo)
                } else {
                    Log.w("[CansSDK]","Couldn't find matching auth info...")
                }
                core.removeAccount(account)
                listeners.forEach { it.onUnRegister() }
            }
        }

        fun startCall(addressToCall: String) {
            val remoteAddress: Address? = core.interpretUrl(addressToCall)
            remoteAddress
                ?: return // If address parsing fails, we can't continue with outgoing call process

            // We also need a CallParams object
            // Create call params expects a Call object for incoming calls, but for outgoing we must use null safely
            val params = core.createCallParams(null)
            params ?: return // Same for params

            // We can now configure it
            // Here we ask for no encryption but we could ask for ZRTP/SRTP/DTLS
            params.mediaEncryption = MediaEncryption.None
            // If we wanted to start the call with video directly
            //params.enableVideo(true)

            core.addListener(coreListenerStub)
            core.start()

            // Finally we start the call
            core.inviteAddressWithParams(remoteAddress, params)
            // Call process can be followed in onCallStateChanged callback from core listener
        }

        fun terminateCall() {
            if (core.callsNb == 0) return

            // If the call state isn't paused, we can get it using core.currentCall
            val call = if (core.currentCall != null) core.currentCall else core.calls[0]
            call ?: return

            // Terminating a call is quite simple
            call.terminate()
        }

        fun startAnswerCall() {
            val remoteSipAddress = destinationRemoteAddress
            val remoteAddress = core.interpretUrl(remoteSipAddress)
            val call =
                if (remoteAddress != null) core.getCallByRemoteAddress2(remoteAddress) else null
            if (call == null) {
                Log.e("[CansSDK]", "Couldn't find call from remote address $remoteSipAddress")
                return
            }
            Toast.makeText(context, "Call Answered", Toast.LENGTH_SHORT).show()
            answerCall(call)
        }

        private fun answerCall(call: Call) {
            Log.i("[CansSDK]","Answering call $call")
            val params = core.createCallParams(call)
            if (CansUtils.checkIfNetworkHasLowBandwidth(context)) {
                Log.w("[CansSDK]","Enabling low bandwidth mode!")
                params?.isLowBandwidthEnabled = true
            }
            call.acceptWithParams(params)
        }

        fun toggleMuteMicrophone() {
            if (packageManager?.checkPermission(
                    Manifest.permission.RECORD_AUDIO,
                    packageName
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val call = core.currentCall
            if (call != null && call.conference != null) {
                val micMuted = call.conference?.microphoneMuted ?: false
                call.conference?.microphoneMuted = !micMuted
            } else {
                val micMuted = call?.microphoneMuted ?: false
                call?.microphoneMuted = !micMuted
            }
        }

        private fun isSpeakerAudio(call: Call? = null): Boolean {
            val currentCall = if (core.callsNb > 0) {
                call ?: core.currentCall ?: core.calls[0]
            } else {
                Log.w("[CansSDK Audio Route Helper]","No call found, checking audio route on Core")
                null
            }
            val conference = core.conference

            val audioDevice = if (conference != null && conference.isIn) {
                conference.outputAudioDevice
            } else if (currentCall != null) {
                currentCall.outputAudioDevice
            } else {
                core.outputAudioDevice
            }

            if (audioDevice == null) return false
            Log.i("[CansSDK Audio]","Playback audio device currently in use is [${audioDevice.deviceName} (${audioDevice.driverName}) ${audioDevice.type}]"
            )
            return audioDevice.type == AudioDevice.Type.Speaker
        }

        private fun vibrator() {
            if (mVibrator.hasVibrator()) {
                DeviceUtils.vibrate(mVibrator)
            }
        }

        fun isCallLogMissed(): Boolean {
            return (
                    callCans.callLog.dir == Call.Dir.Incoming &&
                            (
                                    callCans.callLog.status == Call.Status.Missed ||
                                            callCans.callLog.status == Call.Status.Aborted ||
                                            callCans.callLog.status == Call.Status.EarlyAborted ||
                                            callCans.callLog.status == Call.Status.Declined
                                    )
                    )
        }

        fun addListener(listener: CansListenerStub) {
            listeners.add(listener)
        }

        fun removeListener(listener: CansListenerStub) {
            listeners.remove(listener)
        }
    }
}