package cc.cans.canscloud.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
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
import androidx.core.app.ActivityCompat
import cc.cans.canscloud.sdk.core.CoreContextSDK
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.utils.AudioRouteUtils
import cc.cans.canscloud.sdk.utils.PermissionHelper
import org.linphone.core.LogCollectionState
import org.linphone.core.LogLevel
import org.linphone.core.tools.compatibility.DeviceUtils

data class Notifiable(val notificationId: Int) {
    var remoteAddress: String? = null
}

class CansCenter : Cans {
    override lateinit var core: Core
    override lateinit var callCans: Call
    override lateinit var mVibrator: Vibrator
    override lateinit var callState: CallState
    var appName: String? = null
    var audioRoutesEnabled: Boolean = false
    var destinationCall : String = ""

    @SuppressLint("StaticFieldLeak")
    override lateinit var corePreferences: CorePreferences

    @SuppressLint("StaticFieldLeak")
    override lateinit var context: Context
    private var audioManager: AudioManager? = null

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private var listeners = mutableListOf<CansListenerStub>()

    override val account: String
        get() {
            core.defaultAccount?.params?.identityAddress?.let {
                return "${it.username}@${it.domain}:${it.port}"
            }
            return ""
        }

    override val username: String
        get() {
            core.defaultAccount?.params?.identityAddress?.let {
                return "${it.username}"
            }
            return ""
        }


    override val domain: String
        get() {
            core.defaultAccount?.params?.identityAddress?.let {
                return "${it.domain}"
            }
            return ""
        }

    override val port: String
        get() {
            core.defaultAccount?.params?.identityAddress?.let {
                return "${it.port}"
            }
            return ""
        }

    override val destinationRemoteAddress: String
        get() = callCans.remoteAddress.asStringUriOnly()

    override val destinationUsername: String
        get() = destinationCall

    override val lastOutgoingCallLog: String
        get() {
            val callLog = core.lastOutgoingCallLog
            if (callLog != null) {
                return CansUtils.getDisplayableAddress(callLog.remoteAddress).substringBefore("@")
                    .substringAfter("sip:")
            }
            return ""
        }

    override val durationTime: Int?
        get() = core.currentCall?.duration

    override val startDateCall: Int
        get() = core.currentCall?.callLog?.startDate?.toInt() ?: 0

    override val missedCallsCount: Int
        get() = core.missedCallsCount

    override val countCalls: Int
        get() = core.callsNb

    override val isBluetoothDevices: Boolean
        get() = cansCenter().core.extendedAudioDevices.any {
            it.type == AudioDevice.Type.Bluetooth
        }

    override val wasBluetoothPreviouslyAvailable: Boolean
        get() = audioRoutesEnabled

    override val isMicState: Boolean
        get() = core.currentCall?.microphoneMuted == true

    override val isSpeakerState: Boolean
        get() = AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()

    override val isBluetoothState: Boolean
        get() = audioManager?.isBluetoothScoOn == true

    override val isHeadsetState: Boolean
        get() = AudioRouteUtils.isHeadsetAudioRouteAvailable()

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
            destinationCall =  call.remoteAddress.username ?: ""

            Log.w("onCallStateChanged: ", "${state}")

            when (state) {
                Call.State.IncomingEarlyMedia, Call.State.IncomingReceived -> {
                    vibrator()
                    setListenerCall(CallState.IncomingCall)
                }

                Call.State.OutgoingInit -> {
                    setListenerCall(CallState.StartCall)
                }

                Call.State.OutgoingProgress -> {
                    setListenerCall(CallState.CallOutgoing)
                }

                Call.State.StreamsRunning -> {
                    setListenerCall(CallState.StreamsRunning)
                }

                Call.State.Connected -> {
                    setListenerCall(CallState.Connected)
                }

                Call.State.Error -> {
                    setListenerCall(CallState.Error)
                }

                Call.State.End -> {
                    setListenerCall(CallState.CallEnd)
                }

                Call.State.Released -> {
                    setListenerCall(CallState.MissCall)
                }

                else -> {
                    setListenerCall(CallState.Unknown)
                }
            }
        }

        override fun onLastCallEnded(core: Core) {
            super.onLastCallEnded(core)
            if (!core.isMicEnabled) {
                Log.w("[CansSDK]", "Mic was muted in Core, enabling it back for next call")
                core.isMicEnabled = true
            }
            mVibrator.cancel()
            listeners.forEach { it.onLastCallEnded() }
        }

        override fun onAudioDeviceChanged(core: Core, audioDevice: AudioDevice) {
            AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()
            AudioRouteUtils.isBluetoothAudioRouteCurrentlyUsed()
            listeners.forEach { it.onAudioDeviceChanged() }
        }

        override fun onAudioDevicesListUpdated(core: Core) {
            Log.i("[CansSDK Controls]", "Audio devices list updated")
            audioDevicesListUpdated()
            listeners.forEach { it.onAudioDevicesListUpdated() }
        }
    }

    private fun setListenerCall(callState: CallState) {
        this.callState = callState
        listeners.forEach { it.onCallState(callState) }
    }

    override fun config(
        context: Context,
        appName: String
    ) {

        this.context = context
        this.appName = appName

        Factory.instance().setLogCollectionPath(context.filesDir.absolutePath)
        Factory.instance().enableLogCollection(LogCollectionState.Enabled)

        corePreferences = CorePreferences(context)
        corePreferences.copyAssetsFromPackage()

        if (cansCenter().corePreferences.vfsEnabled) {
            CoreContextSDK.activateVFS()
        }

        val config = Factory.instance()
            .createConfigWithFactory(corePreferences.configPath, corePreferences.factoryConfigPath)
        corePreferences.config = config

        Factory.instance().setLoggerDomain(appName)
        Factory.instance().enableLogcatLogs(corePreferences.logcatLogsOutput)
        if (corePreferences.debugLogs) {
            Factory.instance().loggingService.setLogLevel(LogLevel.Message)
        }

        core = Factory.instance().createCoreWithConfig(config, context)
        core.start()
        core.addListener(coreListenerStub)
        createNotificationChannels(context, notificationManager)

        core.ring = null
        core.isVibrationOnIncomingCallEnabled = true
        core.isNativeRingingEnabled = true
        mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun createNotificationChannels(
        context: Context,
        notificationManager: NotificationManagerCompat,
    ) {
        createServiceChannel(context, notificationManager)
        createMissedCallChannel(context, notificationManager)
        createIncomingCallChannel(context, notificationManager)
    }

    private fun createServiceChannel(context: Context, notificationManager: NotificationManagerCompat) {
        // Create service notification channel
        val id = "$appName ${context.getString(R.string.notification_channel_service_id)}"
        val name = "$appName ${context.getString(R.string.notification_channel_service_name)}"
        val description = "$appName ${context.getString(R.string.notification_channel_service_name)}"
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
        channel.description = description
        channel.enableVibration(false)
        channel.enableLights(false)
        channel.setShowBadge(false)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createMissedCallChannel(
        context: Context,
        notificationManager: NotificationManagerCompat,
    ) {
        val id = "$appName ${context.getString(R.string.notification_channel_missed_call_id)}"
        val name = "$appName ${context.getString(R.string.notification_channel_missed_call_name)}"
        val description =
            "$appName ${context.getString(R.string.notification_channel_missed_call_name)}"
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
        val id = "$appName ${context.getString(R.string.notification_channel_incoming_call_id)}"
        val name = "$appName ${context.getString(R.string.notification_channel_incoming_call_name)}"
        val description =
            "$appName ${context.getString(R.string.notification_channel_incoming_call_name)}"
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
        channel.description = description
        channel.lightColor = context.getColor(R.color.notification_led_color)
        channel.enableVibration(true)
        channel.enableLights(true)
        channel.setShowBadge(true)
        notificationManager.createNotificationChannel(channel)
    }

    override fun register(
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

    override fun removeAccount() {
        core.defaultAccount?.let { account ->
            val authInfo = account.findAuthInfo()
            if (authInfo != null) {
                Log.i("[CansSDK]", "Found auth info $authInfo, removing it.")
                core.removeAuthInfo(authInfo)
            } else {
                Log.w("[CansSDK]", "Couldn't find matching auth info...")
            }
            core.removeAccount(account)
            listeners.forEach { it.onUnRegister() }
        }
    }

    override fun startCall(addressToCall: String) {
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

    override fun terminateCall() {
        if (core.callsNb == 0) return

        // If the call state isn't paused, we can get it using core.currentCall
        val call = if (core.currentCall != null) core.currentCall else core.calls[0]
        call ?: return

        // Terminating a call is quite simple
        call.terminate()
    }

    override fun startAnswerCall() {
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
        Log.i("[CansSDK]", "Answering call $call")
        val params = core.createCallParams(call)
        if (CansUtils.checkIfNetworkHasLowBandwidth(context)) {
            Log.w("[CansSDK]", "Enabling low bandwidth mode!")
            params?.isLowBandwidthEnabled = true
        }
        call.acceptWithParams(params)
    }

    override fun isPauseState() : Boolean {
        return core.currentCall?.state == Call.State.Paused || core.currentCall?.state == Call.State.Pausing || core.currentCall?.state == Call.State.PausedByRemote
    }

    override fun isOutgoingState() : Boolean {
        return core.currentCall?.state == Call.State.OutgoingRinging || core.currentCall?.state == Call.State.OutgoingProgress || core.currentCall?.state == Call.State.OutgoingInit || core.currentCall?.state == Call.State.OutgoingEarlyMedia
    }

    override fun toggleSpeaker() {
        if (AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()) {
            forceEarpieceAudioRoute()
        } else {
            routeAudioToSpeaker()
        }
    }

    override fun toggleMuteMicrophone() {
        if (!PermissionHelper.singletonHolder().get().hasRecordAudioPermission()) {
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

    override fun updateAudioRelated() {
        AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()
        AudioRouteUtils.isBluetoothAudioRouteCurrentlyUsed()
        updateAudioRoutesState()
    }

    override fun updateAudioRoutesState() {
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        audioRoutesEnabled = bluetoothAdapter.isEnabled
    }

    override fun forceEarpieceAudioRoute() {
        if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
            AudioRouteUtils.routeAudioToHeadset()
        } else {
            AudioRouteUtils.routeAudioToEarpiece()
        }
    }

    override fun routeAudioToHeadset() {
        AudioRouteUtils.routeAudioToHeadset()
    }

    override fun routeAudioToBluetooth() {
        AudioRouteUtils.routeAudioToBluetooth()
        audioManager?.startBluetoothSco()
        audioManager?.isBluetoothScoOn = true
    }

    override fun routeAudioToSpeaker() {
        AudioRouteUtils.routeAudioToSpeaker()
    }

    override fun audioDevicesListUpdated() {
        updateAudioRoutesState()
        if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
            AudioRouteUtils.routeAudioToHeadset()
        } else if (!isBluetoothState && corePreferences.routeAudioToBluetoothIfAvailable) {
            val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter.isEnabled) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val connectedDevices =
                        bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                    if (connectedDevices == BluetoothProfile.STATE_CONNECTED) {
                        println("[bluetoothAdapter] Audio devices list updated ${bluetoothAdapter.name}")
                        audioManager?.startBluetoothSco()
                        audioManager?.isBluetoothScoOn = true
                    }
                }
            }
        }
    }

    private fun vibrator() {
        if (mVibrator.hasVibrator()) {
            DeviceUtils.vibrate(mVibrator)
        }
    }

    override fun isCallLogMissed(): Boolean {
        return (
                callCans.callLog.dir == Call.Dir.Incoming &&
                        (
                                callCans.callLog.status == Call.Status.Missed ||
                                        callCans.callLog.status == Call.Status.Aborted ||
                                        callCans.callLog.status == Call.Status.EarlyAborted
                                )
                )
    }

    override fun addListener(listener: CansListenerStub) {
        listeners.add(listener)
    }

    override fun removeListener(listener: CansListenerStub) {
        listeners.remove(listener)
    }

    override fun removeAllListener() {
        listeners.clear()
    }
}