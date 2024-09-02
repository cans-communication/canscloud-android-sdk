package cc.cans.canscloud.sdk

import UserService
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telecom.CallAudioState
import android.telephony.TelephonyManager.NETWORK_TYPE_EDGE
import android.telephony.TelephonyManager.NETWORK_TYPE_GPRS
import android.telephony.TelephonyManager.NETWORK_TYPE_IDEN
import android.widget.Toast
import cc.cans.canscloud.sdk.callback.CallCallback
import cc.cans.canscloud.sdk.callback.RegisterCallback
import cc.cans.canscloud.sdk.core.CorePreferences
import cc.cans.canscloud.sdk.utils.CansUtils
import com.google.gson.Gson
import org.linphone.core.Account
import org.linphone.core.Address
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Event
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.ProxyConfig
import org.linphone.core.Reason
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.core.tools.Log
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

class Cans {

    companion object {
        lateinit var core: Core
        lateinit var callCans: Call

        @SuppressLint("StaticFieldLeak")
        lateinit var corePreferences: CorePreferences
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        var packageManager: PackageManager? = null
        var packageName: String = ""
        val callListeners = ArrayList<CallCallback>()
        val registerListeners = ArrayList<RegisterCallback>()
        var isMicrophoneMuted: Boolean = false
        var isSpeakerSelected: Boolean = false

        private val coreListener = object : CoreListenerStub() {
            override fun onRegistrationStateChanged(
                core: Core,
                cfg: ProxyConfig,
                state: RegistrationState,
                message: String
            ) {
                Log.i("[Assistant] [Generic Login] Registration state is $state: $message")
                if (state == RegistrationState.Ok) {
                    registerListeners.forEach { it.onRegistrationOk() }
                } else if (state == RegistrationState.Failed) {
                    registerListeners.forEach { it.onRegistrationFail(message) }
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

                when (state) {
                    Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                        callCans = call
                        callListeners.forEach { it.onInComingCall() }
                    }

                    Call.State.OutgoingInit -> {
                        callListeners.forEach { it.onStartCall() }
                    }

                    Call.State.OutgoingProgress -> {
                        callListeners.forEach { it.onCallOutGoing() }
                    }

                    Call.State.Connected -> {
                        callListeners.forEach { it.onConnected() }
                    }

                    Call.State.StreamsRunning -> {
                    }

                    Call.State.Paused -> {
                        // When you put a call in pause, it will became Paused
                    }

                    Call.State.Error -> {
                        callListeners.forEach { it.onError(message) }
                    }

                    Call.State.End -> {
                        callListeners.forEach { it.onCallEnd() }
                    }

                    else -> {}
                }
            }

            override fun onLastCallEnded(core: Core) {
                super.onLastCallEnded(core)
                callListeners.forEach { it.onLastCallEnd() }
            }
        }

        fun registerCallListener(listener: CallCallback) {
            callListeners.add(listener)
        }

        fun unCallListener(listener: CallCallback) {
            callListeners.remove(listener)
        }

        fun registersListener(listener: RegisterCallback) {
            registerListeners.add(listener)
        }

        fun unRegisterListener(listener: RegisterCallback) {
            core.defaultAccount?.let { it -> deleteAccount(it) }
            registerListeners.forEach { it.onUnRegister() }
            registerListeners.remove(listener)
        }

        fun config(
            activity: Activity,
            packageManager: PackageManager,
            packageName: String,
            callback: () -> Unit
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
            core.addListener(coreListener)

            callback()
        }

        private fun loadJSONFromAsset(context: Context, fileName: String): String? {
            return try {
                val inputStream: InputStream = context.assets.open(fileName)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                String(buffer, Charsets.UTF_8)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        fun register(activity: Activity, keyCompany: String) {
            val fileName = "json/$keyCompany.json"
            val jsonString = loadJSONFromAsset(context = activity.applicationContext, fileName)

            jsonString?.let {
                val gson = Gson()
                val user = gson.fromJson(it, UserService::class.java)
                if (usernameRegister.isEmpty() || (user.username != usernameRegister) || (user.domain != domainRegister)) {
                    core.defaultAccount?.let { it -> deleteAccount(it) }
                    val username = user.username
                    val password = user.password
                    val domain = "${user.domain}:${user.port}"
                    val transportType = if (user.transport.lowercase() == "tcp") {
                        TransportType.Tcp
                    } else {
                        TransportType.Udp
                    }

                    val authInfo = Factory.instance()
                        .createAuthInfo(username, null, password, null, null, domain, null)

                    val params = core.createAccountParams()
                    val identity = Factory.instance().createAddress("sip:$username@$domain")
                    params.identityAddress = identity

                    val address = Factory.instance().createAddress("sip:$domain")
                    address?.transport = transportType
                    params.serverAddress = address
                    params.isRegisterEnabled = true

                    val createAccount = core.createAccount(params)
                    core.addAuthInfo(authInfo)
                    core.addAccount(createAccount)

                    // Asks the CaptureTextureView to resize to match the captured video's size ratio
                    //core.config.setBool("video", "auto_resize_preview_to_keep_ratio", true)

                    core.defaultAccount = createAccount
                    core.addListener(coreListener)
                    core.start()

                    // We will need the RECORD_AUDIO permission for video call
                    if (packageManager?.checkPermission(
                            Manifest.permission.RECORD_AUDIO,
                            packageName
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
                        return
                    }
                }
            }
        }

        fun registerByUser(
            activity: Activity,
            username: String,
            password: String,
            domain: String,
            port: String,
            transport: String
        ) {
            if ((username != usernameRegister) || (domain != domainRegister)) {
                core.defaultAccount?.let { it -> deleteAccount(it) }
                val domainApp = "${domain}:${port}"
                val transportType = if (transport.lowercase() == "tcp") {
                    TransportType.Tcp
                } else {
                    TransportType.Udp
                }

                val authInfo = Factory.instance()
                    .createAuthInfo(username, null, password, null, null, domainApp, null)

                val params = core.createAccountParams()
                val identity = Factory.instance().createAddress("sip:$username@$domainApp")
                params.identityAddress = identity

                val address = Factory.instance().createAddress("sip:$domainApp")
                address?.transport = transportType
                params.serverAddress = address
                params.isRegisterEnabled = true

                val createAccount = core.createAccount(params)
                core.addAuthInfo(authInfo)
                core.addAccount(createAccount)

                // Asks the CaptureTextureView to resize to match the captured video's size ratio
                //core.config.setBool("video", "auto_resize_preview_to_keep_ratio", true)

                core.defaultAccount = createAccount
                core.addListener(coreListener)
                core.start()

                // We will need the RECORD_AUDIO permission for video call
                if (packageManager?.checkPermission(
                        Manifest.permission.RECORD_AUDIO,
                        packageName
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
                    return
                }
            }
        }

        val accountRegister: String
            get() {
                core.defaultAccount?.params?.identityAddress?.let {
                    return "${it.username}@${it.domain}:${it.port}"
                }
                return ""
            }

        val usernameRegister: String
            get() {
                core.defaultAccount?.params?.identityAddress?.let {
                    return "${it.username}"
                }
                return ""
            }


        val domainRegister: String
            get() {
                core.defaultAccount?.params?.identityAddress?.let {
                    return "${it.domain}"
                }
                return ""
            }

        val portRegister: String
            get() {
                core.defaultAccount?.params?.identityAddress?.let {
                    return "${it.port}"
                }
                return ""
            }

        private fun deleteAccount(account: Account) {
            val authInfo = account.findAuthInfo()
            if (authInfo != null) {
                Log.i("[Account Settings] Found auth info $authInfo, removing it.")
                core.removeAuthInfo(authInfo)
            } else {
                Log.w("[Account Settings] Couldn't find matching auth info...")
            }
            core.removeAccount(account)
        }


        fun getCountCalls(): Int {
            val call = core.callsNb
            Log.i("[Application] getCountCalls : $call")
            return call
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

            core.addListener(coreListener)
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

        fun remoteAddressCall(): String {
            return callCans.remoteAddress.asStringUriOnly()
        }

        fun usernameCall(): String {
            return callCans.remoteAddress.username ?: ""
        }

        fun startAnswerCall() {
            val remoteSipAddress = remoteAddressCall()
            if (remoteSipAddress == null) {
                android.util.Log.e(
                    "[Notification Broadcast Receiver]",
                    "Remote SIP address is null for notification"
                )
                return
            }


            val remoteAddress = core.interpretUrl(remoteSipAddress)
            val call =
                if (remoteAddress != null) core.getCallByRemoteAddress2(remoteAddress) else null
            if (call == null) {
                android.util.Log.e(
                    "[Notification Broadcast Receiver]",
                    "Couldn't find call from remote address $remoteSipAddress"
                )
                return
            }
            Toast.makeText(context, "Call Answered", Toast.LENGTH_SHORT).show()
            answerCall(call)
        }

        fun answerCall(call: Call) {
            Log.i("[Context] Answering call $call")
            val params = core.createCallParams(call)
            if (CansUtils.checkIfNetworkHasLowBandwidth(context)) {
                Log.w("[Context] Enabling low bandwidth mode!")
                params?.isLowBandwidthEnabled = true
            }
            call.acceptWithParams(params)
        }

        fun durationTime(): Int? {
            val durationTime = core.currentCall?.duration
            return durationTime
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
            updateMicState()
        }

        fun updateMicState() {
            isMicrophoneMuted = core.currentCall?.microphoneMuted == true
        }

        fun toggleSpeaker() {
            if (isSpeakerAudio()) {
                forceEarpieceAudioRoute()
            } else {
                forceSpeakerAudioRoute()
            }
            updateSpeakerState()
        }

        fun updateSpeakerState() {
            isSpeakerSelected = isSpeakerAudio()
        }

        private fun isSpeakerAudio(call: Call? = null): Boolean {
            val currentCall = if (core.callsNb > 0) {
                call ?: core.currentCall ?: core.calls[0]
            } else {
                Log.w("[Audio Route Helper] No call found, checking audio route on Core")
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
            Log.i(
                "[Audio Route Helper] Playback audio device currently in use is [${audioDevice.deviceName} (${audioDevice.driverName}) ${audioDevice.type}]"
            )
            return audioDevice.type == AudioDevice.Type.Speaker
        }

        private fun isHeadsetAudioRouteAvailable(): Boolean {
            for (audioDevice in core.audioDevices) {
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

        private fun forceEarpieceAudioRoute() {
            if (isHeadsetAudioRouteAvailable()) {
                Log.i("[Call Controls] Headset found, route audio to it instead of earpiece")
                routeAudioToHeadset()
            } else {
                routeAudioToEarpiece()
            }
        }

        private fun forceSpeakerAudioRoute() {
            routeAudioToSpeaker()
        }

        private fun routeAudioToSpeaker(call: Call? = null, skipTelecom: Boolean = false) {
            routeAudioTo(call, arrayListOf(AudioDevice.Type.Speaker), skipTelecom)
        }

        private fun routeAudioToHeadset(call: Call? = null, skipTelecom: Boolean = false) {
            routeAudioTo(
                call,
                arrayListOf(AudioDevice.Type.Headphones, AudioDevice.Type.Headset),
                skipTelecom
            )
        }

        private fun routeAudioTo(
            call: Call?,
            types: List<AudioDevice.Type>,
            skipTelecom: Boolean = false
        ) {
            val currentCall = call ?: core.currentCall ?: core.calls.firstOrNull()
            applyAudioRouteChange(currentCall, types)
        }

        fun routeAudioToEarpiece(call: Call? = null, skipTelecom: Boolean = false) {
            routeAudioTo(call, arrayListOf(AudioDevice.Type.Earpiece), skipTelecom)
        }

        private fun applyAudioRouteChange(
            call: Call?,
            types: List<AudioDevice.Type>,
            output: Boolean = true
        ) {
            val currentCall = if (core.callsNb > 0) {
                call ?: core.currentCall ?: core.calls[0]
            } else {
                Log.w("[Audio Route Helper] No call found, setting audio route on Core")
                null
            }
            val conference = core.conference
            val capability = if (output) {
                AudioDevice.Capabilities.CapabilityPlay
            } else {
                AudioDevice.Capabilities.CapabilityRecord
            }
            val preferredDriver = if (output) {
                core.defaultOutputAudioDevice?.driverName
            } else {
                core.defaultInputAudioDevice?.driverName
            }

            val extendedAudioDevices = core.extendedAudioDevices
            Log.i(
                "[Audio Route Helper] Looking for an ${if (output) "output" else "input"} audio device with capability [$capability], driver name [$preferredDriver] and type [$types] in extended audio devices list (size ${extendedAudioDevices.size})"
            )
            val foundAudioDevice = extendedAudioDevices.find {
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
            if (conference != null && conference.isIn) {
                Log.i(
                    "[Audio Route Helper] Found [${audioDevice.type}] ${if (output) "playback" else "recorder"} audio device [${audioDevice.deviceName} (${audioDevice.driverName})], routing conference audio to it"
                )
                if (output) {
                    conference.outputAudioDevice = audioDevice
                } else {
                    conference.inputAudioDevice = audioDevice
                }
            } else if (currentCall != null) {
                Log.i(
                    "[Audio Route Helper] Found [${audioDevice.type}] ${if (output) "playback" else "recorder"} audio device [${audioDevice.deviceName} (${audioDevice.driverName})], routing call audio to it"
                )
                if (output) {
                    currentCall.outputAudioDevice = audioDevice
                } else {
                    currentCall.inputAudioDevice = audioDevice
                }
            } else {
                Log.i(
                    "[Audio Route Helper] Found [${audioDevice.type}] ${if (output) "playback" else "recorder"} audio device [${audioDevice.deviceName} (${audioDevice.driverName})], changing core default audio device"
                )
                if (output) {
                    core.outputAudioDevice = audioDevice
                } else {
                    core.inputAudioDevice = audioDevice
                }
            }
        }
    }
}