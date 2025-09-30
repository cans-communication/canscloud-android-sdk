package cc.cans.canscloud.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Vibrator
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import cc.cans.canscloud.data.AESFactory
import cc.cans.canscloud.data.ProvisioningData
import cc.cans.canscloud.data.ProvisioningInterceptor
import cc.cans.canscloud.data.ProvisioningResult
import cc.cans.canscloud.data.ProvisioningService
import cc.cans.canscloud.sdk.bcrypt.LoginBcryptService
import cc.cans.canscloud.sdk.bcrypt.models.LoginCpanelRequest
import cc.cans.canscloud.sdk.bcrypt.models.LoginCpanelResponse
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.callback.CansRegisterAccountListenerStub
import cc.cans.canscloud.sdk.callback.CansRegisterListenerStub
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.core.CoreContextSDK
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.core.CorePreferences
import cc.cans.canscloud.sdk.core.CoreService
import cc.cans.canscloud.sdk.data.GroupedCallLogData
import cc.cans.canscloud.sdk.models.CallModel
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.CansAddress
import cc.cans.canscloud.sdk.models.CansTransport
import cc.cans.canscloud.sdk.models.ConferenceState
import cc.cans.canscloud.sdk.models.HistoryModel
import cc.cans.canscloud.sdk.models.RegisterState
import cc.cans.canscloud.sdk.okta.models.LogInType
import cc.cans.canscloud.sdk.okta.models.SignIn
import cc.cans.canscloud.sdk.okta.models.SignInOKTAResponseData
import cc.cans.canscloud.sdk.okta.repository.OKTARepository
import cc.cans.canscloud.sdk.okta.service.OktaWebAuth
import cc.cans.canscloud.sdk.okta.service.OktaWebAuth.Companion.webAuth
import cc.cans.canscloud.sdk.telecom.TelecomHelper
import cc.cans.canscloud.sdk.utils.AudioRouteUtils
import cc.cans.canscloud.sdk.utils.CansUtils
import cc.cans.canscloud.sdk.utils.PermissionHelper
import cc.cans.canscloud.sdk.utils.SecureUtils
import cc.cans.canscloud.sdk.utils.TimestampUtils
import com.google.gson.Gson
import com.okta.oidc.AuthenticationPayload
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import org.linphone.core.Account
import org.linphone.core.AccountCreator
import org.linphone.core.AccountListenerStub
import org.linphone.core.Address
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.CallLog
import org.linphone.core.Conference
import org.linphone.core.ConferenceListenerStub
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.LogCollectionState
import org.linphone.core.LogLevel
import org.linphone.core.MediaEncryption
import org.linphone.core.Participant
import org.linphone.core.ProxyConfig
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.core.tools.compatibility.DeviceUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

data class Notifiable(val notificationId: Int) {
    var remoteAddress: String? = null
}

class CansCenter() : Cans {
    override lateinit var core: Core
    override lateinit var callCans: Call
    override lateinit var mVibrator: Vibrator
    override lateinit var callState: CallState
    override var coreService = CoreService()
    override var appName: String = ""
    var audioRoutesEnabled: Boolean = false
    var destinationCall: String = ""
    private var TAG = "CansCenter"
    private lateinit var accountDefault: Account
    private lateinit var accountCreator: AccountCreator

    override lateinit var coreContext: CoreContextSDK
    private var proxyConfigToCheck: ProxyConfig? = null

    @SuppressLint("StaticFieldLeak")
    override lateinit var corePreferences: CorePreferences

    @SuppressLint("StaticFieldLeak")
    override lateinit var context: Context
    private var audioManager: AudioManager? = null

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private var listeners = mutableListOf<CansListenerStub>()
    private var registerListeners = mutableListOf<CansRegisterListenerStub>()
    private var registerAccountListeners = mutableListOf<CansRegisterAccountListenerStub>()
    override val missedCallLogs = ArrayList<GroupedCallLogData>()

    override lateinit var conferenceCore: Conference

    override var isInConference: Boolean = false

    override var isMeConferenceFocus: Boolean = false

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

    override val proxy: String
        get() {
            core.defaultAccount?.params?.serverAddress?.asStringUriOnly()?.let {
                return it
            }
            return ""
        }

    override val defaultStateRegister: RegisterState
        get() {
            val state: RegistrationState
            val defaultAccount = cansCenter().core.defaultAccount
            if (defaultAccount != null) {
                state = defaultAccount.state

                return when (state) {
                    RegistrationState.Ok -> RegisterState.OK
                    RegistrationState.None -> RegisterState.NONE
                    else -> RegisterState.FAIL
                }
            }
            return RegisterState.NONE
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
        get() = !core.isMicEnabled

    override val isSpeakerState: Boolean
        get() = AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()

    override val isBluetoothState: Boolean
        get() = AudioRouteUtils.isBluetoothAudioRouteCurrentlyUsed()

    override val isBluetoothAudioRouteAvailable: Boolean
        get() = AudioRouteUtils.isBluetoothAudioRouteAvailable()

    override val isHeadsetState: Boolean
        get() = AudioRouteUtils.isHeadsetAudioRouteAvailable()

    private var accountToDelete: Account? = null

    private val accountListener: AccountListenerStub = object : AccountListenerStub() {
        override fun onRegistrationStateChanged(
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            Log.i("[$TAG: onRegistrationStateChanged]", "Registration state is $state: $message")
            Log.d("SDK", "Registration state is $state: $message")
            if (state == RegistrationState.Cleared && account == accountToDelete) {
                deleteAccount(account)
                registerAccountListeners.forEach { it.onRegistration(RegisterState.CLEARED) }
            } else {
                if (state == RegistrationState.Ok) {
                    registerAccountListeners.forEach { it.onRegistration(RegisterState.OK, message) }
                } else if (state == RegistrationState.Failed) {
                    registerAccountListeners.forEach { it.onRegistration(RegisterState.FAIL, message) }
                }
            }
        }
    }

    private var coreListenerStub = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            Log.i("[$TAG: onAccount]", "Registration state is $state: $message")
            if (account == core.defaultAccount) {
                registerListeners.forEach { it.onUpdateAccountRegistration(RegisterState.OK, message) }
            } else if (core.accountList.isEmpty()) {
                registerListeners.forEach { it.onUpdateAccountRegistration(RegisterState.NONE, message) }
            }
        }

        override fun onRegistrationStateChanged(
            core: Core,
            cfg: ProxyConfig,
            state: RegistrationState,
            message: String,
        ) {
            Log.i("$TAG: onRegistration", "${cansCenter().defaultStateRegister}")
            if (cfg == proxyConfigToCheck) {
                Log.i(TAG,"[Account Login] Registration state is $state: $message")
                if (state == RegistrationState.Ok) {
                    registerListeners.forEach { it.onRegistration(RegisterState.OK, message) }
                } else if (state == RegistrationState.Failed) {
                    registerListeners.forEach { it.onRegistration(RegisterState.FAIL, message) }
                }
            }
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            callCans = call
            destinationCall = call.remoteAddress.username ?: ""

            Log.w("CansSDK: onCallStateChanged: ", "$state : $message")

            when (state) {
                Call.State.IncomingEarlyMedia, Call.State.IncomingReceived -> {
                    vibrator()
                }

                Call.State.StreamsRunning -> {
                    mVibrator.cancel()
                }

                Call.State.Error -> {
                    updateMissedCallLogs()
                    mVibrator.cancel()

                    setListenerCall(CallState.Error)
                }

                Call.State.End -> {
                    updateMissedCallLogs()
                    mVibrator.cancel()
                }

                else -> {
                }
            }

            if (state != null) {
                setListenerCall(mapStatusCall(state))
            }
        }

        override fun onLastCallEnded(core: Core) {
            super.onLastCallEnded(core)
            Log.w("CansSDK: onLastCallEnded:", "")
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

        override fun onConferenceStateChanged(
            core: Core,
            conference: Conference,
            state: Conference.State,
        ) {
            Log.i("[Conference VM]", " Conference state changed: $state")
            if (state == Conference.State.Instantiated) {
                conference.addListener(conferenceListener)
                listeners.forEach { it.onConferenceState(ConferenceState.Instantiated) }
            } else if (state == Conference.State.Created) {
                isMeConferenceFocus = conference.me.isFocus
                listeners.forEach { it.onConferenceState(ConferenceState.Created) }
            } else if (state == Conference.State.Terminated || state == Conference.State.TerminationFailed) {
                conference.removeListener(conferenceListener)
                isInConference = false
                listeners.forEach { it.onConferenceState(ConferenceState.Terminated) }
            }
        }
    }

    private val conferenceListener = object : ConferenceListenerStub() {
        @WorkerThread
        override fun onParticipantAdded(conference: Conference, participant: Participant) {
            if (conference.isMe(participant.address)) {
                Log.i("[Conference VM]", "Entered conference")
            } else {
                Log.i("[Conference VM]", " Participant added")
                conferenceCore = conference
                isInConference = conference.participantList.isNotEmpty()
            }
        }

        override fun onParticipantRemoved(conference: Conference, participant: Participant) {
            if (conference.isMe(participant.address)) {
                Log.i("[Conference VM]", "Left conference")
            } else {
                Log.i("[Conference VM]", "Participant removed")
                conferenceCore = conference
                isInConference = conference.participantList.isNotEmpty()

                val participants = conferenceCore.participantList
                Log.i("callingLogs1: SP2", "${participants.size}")
            }
        }

        override fun onParticipantAdminStatusChanged(
            conference: Conference,
            participant: Participant,
        ) {
            Log.i("[Conference VM]", "Participant admin status changed")
            conferenceCore = conference
            isInConference = conference.participantList.isNotEmpty()
        }

    }


    private fun mapStatusCall(state: Call.State): CallState {
        return when (state) {
            Call.State.IncomingEarlyMedia, Call.State.IncomingReceived -> CallState.IncomingCall
            Call.State.OutgoingInit -> CallState.StartCall
            Call.State.OutgoingProgress, Call.State.OutgoingEarlyMedia -> CallState.CallOutgoing
            Call.State.StreamsRunning -> CallState.StreamsRunning
            Call.State.Connected -> CallState.Connected
            Call.State.Paused, Call.State.Pausing -> CallState.Pause
            Call.State.Resuming -> CallState.Resuming
            Call.State.Error -> CallState.Error
            Call.State.End -> CallState.CallEnd
            Call.State.Released -> CallState.MissCall
            else -> CallState.Unknown
        }
    }

    override fun getCallLog() : ArrayList<CallModel> {
        val list: ArrayList<CallModel> = arrayListOf()
        val calls = cansCenter().core.calls.toList()
        calls.mapTo(list)  { call ->
            CallModel(
                call.callLog.callId.orEmpty(),
                call.remoteAddress.username.orEmpty(),
                call.remoteAddress.displayName.orEmpty(),
                call.state == Call.State.Paused,
                mapStatusCall(call.state),
                call.duration.toString()
            )
        }

        return list
    }

    private fun setListenerCall(callState: CallState) {
        Log.i("setListenerCall: ", "$callState")
        this.callState = callState
        listeners.forEach { it.onCallState(callState) }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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

        core.removeListener(coreListenerStub)
        core.addListener(coreListenerStub)
        core.start()
        createNotificationChannels(context, notificationManager)

        core.ring = null
        core.isVibrationOnIncomingCallEnabled = true
        core.isNativeRingingEnabled = true
        mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        Compatibility.setupAppStartupListener(context)
        coreContext = CoreContextSDK(context)
        coreContext.start()
        computeUserAgent()
    }

    private fun createNotificationChannels(
        context: Context,
        notificationManager: NotificationManagerCompat,
    ) {
        createServiceChannel(context, notificationManager)
        createMissedCallChannel(context, notificationManager)
        createIncomingCallChannel(context, notificationManager)
    }

    private fun createServiceChannel(
        context: Context,
        notificationManager: NotificationManagerCompat
    ) {
        // Create service notification channel
        val id = context.getString(R.string.notification_channel_service_id)
        val name = "$appName ${context.getString(R.string.notification_channel_service_name)}"
        val description =
            "$appName ${context.getString(R.string.notification_channel_service_name)}"
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

    private fun getAccountCreator(): AccountCreator {
        core.loadConfigFromXml(corePreferences.linphoneDefaultValuesPath)
        accountCreator = core.createAccountCreator(corePreferences.xmlRpcServerUrl)
        accountCreator.language = Locale.getDefault().language

        accountCreator.reset()
        accountCreator.language = Locale.getDefault().language

        core.loadConfigFromXml(corePreferences.defaultValuesPath)
        return accountCreator
    }

    override fun register(
        username: String,
        password: String,
        domain: String,
        port: String,
        transport: CansTransport
    ) {
        if (username.isEmpty() || password.isEmpty() || domain.isEmpty() || port.isEmpty()) {
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        val serverAddress = "${domain}:${port}"
        val transportType = if (transport.name.lowercase() == "tcp") {
            TransportType.Tcp
        } else {
            TransportType.Udp
        }

        accountCreator = getAccountCreator()
        accountCreator.username = username
        accountCreator.password = password
        accountCreator.domain = serverAddress
        accountCreator.displayName = ""
        accountCreator.transport = transportType

        val proxyConfig : ProxyConfig? = accountCreator.createProxyConfig()
        proxyConfigToCheck = proxyConfig

        if (proxyConfig == null) {
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        corePreferences.keepServiceAlive = true
        coreContext.notificationsManager.startForeground()
    }

    override fun removeInvalidProxyConfig() {
        val cfg = proxyConfigToCheck
        cfg ?: return
        val authInfo = cfg.findAuthInfo()
        if (authInfo != null) core.removeAuthInfo(authInfo)
        core.removeProxyConfig(cfg)
        proxyConfigToCheck = null
    }

    private fun computeUserAgent() {
        val deviceName: String = corePreferences.deviceName
        val appNameS: String = "${appName}: Android"
        val userAgent = "$appNameS/ ($deviceName) LinphoneSDK"
        val sdkVersion = context.getString(org.linphone.core.R.string.linphone_sdk_version)
        val sdkBranch = context.getString(org.linphone.core.R.string.linphone_sdk_branch)
        val sdkUserAgent = "$sdkVersion ($sdkBranch)"
        core.setUserAgent(userAgent, sdkUserAgent)
    }

    private fun deleteAccount(account: Account) {
        val authInfo = account.findAuthInfo()
        if (authInfo != null) {
            Log.i("[Account Settings] Found auth info $authInfo", " removing it.")
            core.removeAuthInfo(authInfo)
        } else {
            Log.w("[Account Settings]", "Couldn't find matching auth info...")
        }

        core.removeAccount(account)
        Log.i("[Account Removal]", "Removed account: ${account.params.identityAddress?.asString()}")
    }

    override fun removeAccount(index: Int) {
        val account = core.accountList[index]
        accountToDelete = account

        val registered = account.state == RegistrationState.Ok

        if (core.defaultAccount == account) {
            Log.i("[Account Settings]", "Account was default, let's look for a replacement")
            for (accountIterator in core.accountList) {
                if (account != accountIterator) {
                    core.defaultAccount = accountIterator
                    Log.i("[Account Settings]","New default account is $accountIterator")
                    break
                }
            }
        }

        val params = account.params.clone()
        params.isRegisterEnabled = false
        account.params = params

        if (!registered) {
            Log.w("[Account Settings]"," Account isn't registered, don't unregister before removing it")
            deleteAccount(account)
        }
    }


    override fun removeAccountAll() {
        core.accountList.forEach { account ->
            accountToDelete = account
            Log.i(
                "[Account Removal]",
                "Removed account: ${account.params.identityAddress?.asString()}"
            )

            val registered = account.state == RegistrationState.Ok

            if (core.defaultAccount == account) {
                Log.i("[Account Settings]", "Account was default, let's look for a replacement")
                for (accountIterator in core.accountList) {
                    if (account != accountIterator) {
                        core.defaultAccount = accountIterator
                        Log.i("[Account Settings]", "New default account is $accountIterator")
                        break
                    }
                }
            }

            val params = account.params.clone()
            params.isRegisterEnabled = false
            account.params = params

            if (!registered) {
                Log.w(
                    "[Account Settings]",
                    "Account isn't registered, don't unregister before removing it"
                )
                deleteAccount(account)
            }
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

        for (payload in core.audioPayloadTypes) {
            when (payload.mimeType.uppercase()) {
                "PCMU" -> {
                    payload.enable(true)
                }

                "PCMA" -> {
                    payload.enable(true)
                }
            }
        }

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

    override fun terminateAllCalls() {
        if (core.callsNb == 0) return
        core.terminateAllCalls()
    }

    override fun pause(index: Int, addressToCall: String) {
        val call = cansCenter().core.calls[index]
        if (call.remoteAddress.username == addressToCall) {
            call.pause()
        }
    }

    override fun resume(index: Int, addressToCall: String) {
        val call = cansCenter().core.calls[index]
        if (call.remoteAddress.username == addressToCall) {
            call.resume()
        }
    }

    override fun terminate(index: Int, addressToCall: String) {
        val call = cansCenter().core.calls[index]
        if (call.remoteAddress.username == addressToCall) {
            call.terminate()
        }
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
        answerCall(call)
    }

    override fun accountList(): ArrayList<String> {
        val accounts = cansCenter().core.accountList
        return ArrayList(
            accounts.map { it.params.identityAddress?.username.orEmpty() }
        )
    }

    override fun defaultAccount(index: Int, phoneNumber: String) {
        val account = cansCenter().core.accountList[index]
        if (account.params.identityAddress?.username == phoneNumber) {
            cansCenter().core.defaultAccount = account
        }
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

    override fun isPauseState(): Boolean {
        return core.currentCall?.state == Call.State.Paused || core.currentCall?.state == Call.State.Pausing || core.currentCall?.state == Call.State.PausedByRemote
    }

    override fun isOutgoingState(): Boolean {
        return core.currentCall?.state == Call.State.OutgoingRinging || core.currentCall?.state == Call.State.OutgoingProgress || core.currentCall?.state == Call.State.OutgoingInit || core.currentCall?.state == Call.State.OutgoingEarlyMedia
    }

    override fun toggleSpeaker() {
        if (AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()) {
            forceEarpieceAudioRoute()
        } else {
            routeAudioToSpeaker()
        }
    }

    override fun refreshRegister() {
        core.refreshRegisters()
    }

    override fun registerAccount(username: String, password: String, domain: String) {
        if (username.isEmpty() || password.isEmpty() || domain.isEmpty()) {
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        accountCreator = getAccountCreator()
        val result = accountCreator.setUsername(username)
        if (result != AccountCreator.UsernameStatus.Ok) {
            Log.e(
                "[Assistant]",
                " [Account Login] Error [${result.name}] setting the username: ${username}"
            )
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }
        Log.i("[Assistant]", "[Account Login] Username is ${accountCreator.username}")

        val result2 = accountCreator.setPassword(password)
        if (result2 != AccountCreator.PasswordStatus.Ok) {
            Log.e("[Assistant]", " [Account Login] Error [${result2.name}] setting the password")
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        val result3 = accountCreator.setDomain(domain)
        if (result3 != AccountCreator.DomainStatus.Ok) {
            Log.e("[Assistant]", " [Account Login] Error [${result3.name}] setting the domain")
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(ProvisioningInterceptor())
            .addNetworkInterceptor(
                ProvisioningInterceptor(),
            )
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://voxxycloud.com/Cpanel/")
            .build()

        val provisioningService: ProvisioningService =
            retrofit.create(ProvisioningService::class.java)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("idToken", "")
            .addFormDataPart("action", "provision")
            .addFormDataPart("username", username)
            .addFormDataPart("password", password)
            .build()

        val url = "https://" + domain + "/Cpanel/provision/mobile/"

        val callProvisioningData: retrofit2.Call<ProvisioningData?>? =
            provisioningService.getProvisioningData(url, requestBody)

        callProvisioningData?.let { callProvisioning ->
            callProvisioning.enqueue(
                object : retrofit2.Callback<ProvisioningData?> {
                    override fun onResponse(
                        call: retrofit2.Call<ProvisioningData?>,
                        response: retrofit2.Response<ProvisioningData?>,
                    ) {
                        Log.i("Response success", response.message())
                        if (response.isSuccessful) {
                            response.body().let { body ->
                                val provisioningData: ProvisioningData? = body
                                provisioningData?.let { provisioning ->
                                    val results: List<ProvisioningResult> = provisioning.results
                                    if (results.isNotEmpty()) {
                                        val result = results[0]
                                        accountCreator.username = result.extension?.trim()
                                        accountCreator.password = result.secret?.trim()
                                        accountCreator.domain = result.domain?.trim()
                                        if (result.transport?.lowercase() == "tcp") {
                                            accountCreator.transport = TransportType.Tcp
                                        } else {
                                            accountCreator.transport = TransportType.Udp
                                        }

                                        Log.i("[registerAccount]", "[Account Login] Username is 3333")

                                        if (!createProxyConfig()) {
                                            Log.i(
                                                "createProxyConfig",
                                                "Error: Failed to create account object"
                                            )
                                            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
                                        }
                                    }
                                }
                            }
                        } else {
                            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
                            return
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<ProvisioningData?>,
                        t: Throwable,
                    ) {
                        registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
                        Log.e("Response fail", "${t.message}")
                    }
                },
            )
        }
    }

    private fun createProxyConfig(): Boolean {
        val proxyConfig: ProxyConfig? = accountCreator.createProxyConfig()
        proxyConfigToCheck = proxyConfig

        if (proxyConfig == null) {
            Log.e("[Assistant]", "[Account Login] Account creator couldn't create proxy config")
            //  onErrorEvent.value = Event("Error: Failed to create account object")
            return false
        }

        proxyConfig.isPushNotificationAllowed = true

        Log.i("[Assistant]", " [Account Login] Proxy config created")
        return true
    }

    override fun toggleMuteMicrophone() {
        if (!PermissionHelper.singletonHolder().get().hasRecordAudioPermission()) {
            return
        }

        val micEnabled = core.isMicEnabled
        core.isMicEnabled = !micEnabled
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
        if (!isBluetoothState && corePreferences.routeAudioToBluetoothIfAvailable) {
            val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter.isEnabled) {
                if (ActivityCompat.checkSelfPermission(
                        coreContext.context,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val connectedDevices =
                        bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                    if (connectedDevices == BluetoothProfile.STATE_CONNECTED) {
                        println("[bluetoothAdapter] Audio devices list updated ${bluetoothAdapter.name}")
                        audioManager?.startBluetoothSco()
                        audioManager?.isBluetoothScoOn = true
                        AudioRouteUtils.routeAudioToBluetooth()
                    } else {
                        if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToHeadset()
                        }
                        println("[bluetoothAdapter] Audio devices list updated no connect")
                    }
                }
            }
        } else if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
            AudioRouteUtils.routeAudioToHeadset()
        }
        updateAudioRoutesState()
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
                                        callCans.callLog.status == Call.Status.EarlyAborted ||
                                        callCans.callLog.status == Call.Status.Declined
                                )
                )
    }

    fun isCallLogMissed(callLog: CallLog): Boolean {
        return (
                callLog.dir == Call.Dir.Incoming &&
                        (
                                callLog.status == Call.Status.Missed ||
                                        callLog.status == Call.Status.Aborted ||
                                        callLog.status == Call.Status.EarlyAborted ||
                                        callLog.status == Call.Status.Declined
                                )
                )
    }

    override fun requestPermissionPhone(activity: Activity) {
        if (!PermissionHelper.singletonHolder().get().hasReadPhoneStatePermission()) {
            Log.i("[$TAG]", "Asking for READ_PHONE_STATE permission")
            activity.requestPermissions(
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECORD_AUDIO
                ), 0
            )
        } else if (!PermissionHelper.singletonHolder().get().hasPostNotificationsPermission()) {
            // Don't check the following the previous permission is being asked
            Log.i("[$TAG]", "Asking for POST_NOTIFICATIONS permission")
            Compatibility.requestPostNotificationsPermission(activity, 2)
        }

        // See https://developer.android.com/about/versions/14/behavior-changes-14#fgs-types
        if (Build.VERSION.SDK_INT >= (Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            val fullScreenIntentPermission = Compatibility.hasFullScreenIntentPermission(
                activity
            )
            if (!fullScreenIntentPermission) {
                Compatibility.requestFullScreenIntentPermission(activity)
            }
        }
    }

    override fun requestPermissionAudio(activity: Activity) {
        val permissionsRequiredList = arrayListOf<String>()

        if (!PermissionHelper.singletonHolder().get().hasRecordAudioPermission()) {
            Log.i("[$TAG]", "Asking for RECORD_AUDIO permission")
            permissionsRequiredList.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= (Build.VERSION_CODES.S) && !PermissionHelper.singletonHolder()
                .get().hasBluetoothConnectPermission()
        ) {
            Log.i("[$TAG]", "Asking for BLUETOOTH_CONNECT permission")
            permissionsRequiredList.add(Compatibility.BLUETOOTH_CONNECT)
        }

        if (permissionsRequiredList.isNotEmpty()) {
            val permissionsRequired = arrayOfNulls<String>(permissionsRequiredList.size)
            permissionsRequiredList.toArray(permissionsRequired)
            activity.requestPermissions(permissionsRequired, 3)
        }
    }

    override fun enableTelecomManager(activity: Activity) {
        Log.i("[$TAG]", " Telecom Manager permissions granted")
        if (!TelecomHelper.singletonHolder().exists()) {
            Log.i("[$TAG]", " Creating Telecom Helper")
            if (Compatibility.hasTelecomManagerFeature(activity)) {
                TelecomHelper.singletonHolder().create(activity)
            } else {
                Log.e(
                    "[$TAG]",
                    " Telecom Helper can't be created, device doesn't support connection service!"
                )
            }
        } else {
            Log.e("[$TAG]", " Telecom Manager was already created ?!")
        }
        cansCenter().corePreferences.useTelecomManager = true
    }

    override fun checkTelecomManagerPermissions(activity: Activity) {
        if (!cansCenter().corePreferences.useTelecomManager) {
            Log.i("[$TAG]", "Telecom Manager feature is disabled")
            if (cansCenter().corePreferences.manuallyDisabledTelecomManager) {
                Log.w("[$TAG]", " User has manually disabled Telecom Manager feature")
            } else {
                if (Compatibility.hasTelecomManagerPermissions(activity)) {
                    enableTelecomManager(activity)
                } else {
                    Log.i("[$TAG]", " Asking for Telecom Manager permissions")
                    Compatibility.requestTelecomManagerPermissions(activity, 1)
                }
            }
        } else {
            Log.i("[$TAG]", " Telecom Manager feature is already enabled")
        }
    }

    override fun callLogsAll() : ArrayList<GroupedCallLogData> {
        val list = arrayListOf<GroupedCallLogData>()
        var previousCallLogGroup: GroupedCallLogData? = null

        for (callLog in core.callLogs) {
            val historyModel = HistoryModel(
                phoneNumber = callLog.remoteAddress.username ?: "",
                name = callLog.remoteAddress.displayName ?: "",
                state = onCallState(callLog),
                time = TimestampUtils.convertMillisToTime(callLog.startDate),
                date = TimestampUtils.formatDate(context, callLog.startDate),
                startDate = callLog.startDate,
                duration = duration(callLog),
                callID = callLog.callId.toString(),
                localAddress = callLog.localAddress,
                remoteAddress = callLog.remoteAddress
            )

            if (previousCallLogGroup == null) {
                previousCallLogGroup = GroupedCallLogData(historyModel)
            } else if (previousCallLogGroup.lastCallLog.localAddress.weakEqual(callLog.localAddress) &&
                previousCallLogGroup.lastCallLog.remoteAddress.weakEqual(callLog.remoteAddress)
            ) {
                val previousStatus = previousCallLogGroup.lastCallLog.state
                if (previousStatus == onCallState(callLog)) {
                    if (TimestampUtils.isSameDay(
                            previousCallLogGroup.lastCallLog.startDate,
                            callLog.startDate,
                        )
                    ) {
                        previousCallLogGroup.callLogs.add(historyModel)
                        previousCallLogGroup.lastCallLog = historyModel
                    } else {
                        list.add(previousCallLogGroup)
                        previousCallLogGroup = GroupedCallLogData(historyModel)
                    }
                } else {
                    list.add(previousCallLogGroup)
                    previousCallLogGroup = GroupedCallLogData(historyModel)
                }
            } else {
                list.add(previousCallLogGroup)
                previousCallLogGroup = GroupedCallLogData(historyModel)
            }
        }

        if (previousCallLogGroup != null && !list.contains(previousCallLogGroup)) {
            list.add(previousCallLogGroup)
        }

        return list
    }


    override fun updateMissedCallLogs() {
        val missedList: ArrayList<GroupedCallLogData> = arrayListOf()
        var previousMissedCallLogGroup: GroupedCallLogData? = null
        missedCallLogs.clear()

        for (callLog in core.callLogs) {
            val historyModel = HistoryModel(
                phoneNumber = callLog.remoteAddress.username ?: "",
                name = callLog.remoteAddress.displayName ?: "",
                state = onCallState(callLog),
                time = TimestampUtils.convertMillisToTime(callLog.startDate),
                date = TimestampUtils.formatDate(context, callLog.startDate),
                startDate = callLog.startDate,
                duration = duration(callLog),
                callID = callLog.callId.toString(),
                localAddress = callLog.localAddress,
                remoteAddress = callLog.remoteAddress
            )

            if (historyModel.state == CallState.MissCall) {
                if (previousMissedCallLogGroup == null) {
                    previousMissedCallLogGroup = GroupedCallLogData(historyModel)
                } else if (previousMissedCallLogGroup.lastCallLog.localAddress.weakEqual(callLog.localAddress) &&
                    previousMissedCallLogGroup.lastCallLog.remoteAddress.weakEqual(callLog.remoteAddress)
                ) {
                    if (TimestampUtils.isSameDay(
                            previousMissedCallLogGroup.lastCallLog.startDate,
                            callLog.startDate
                        )
                    ) {
                        previousMissedCallLogGroup.callLogs.add(historyModel)
                        previousMissedCallLogGroup.lastCallLog = historyModel
                    } else {
                        missedList.add(previousMissedCallLogGroup)
                        previousMissedCallLogGroup = GroupedCallLogData(historyModel)
                    }
                } else {
                    missedList.add(previousMissedCallLogGroup)
                    previousMissedCallLogGroup = GroupedCallLogData(historyModel)
                }
            }
        }

        if (previousMissedCallLogGroup != null && !missedList.contains(previousMissedCallLogGroup)) {
            missedList.add(previousMissedCallLogGroup)
        }

        missedCallLogs.addAll(missedList)

        val callLogs: ArrayList<HistoryModel> = arrayListOf()
        missedCallLogs.forEach {
            it.lastCallLog.listCall = it.callLogs.size
            callLogs.add(it.lastCallLog)
        }
    }

    private fun duration(it: CallLog): String {
        return if (isCallLogMissed(it)) {
            ""
        } else {
            TimestampUtils.durationCallingTime(it.duration)
        }
    }

    private fun onCallState(callLog: CallLog): CallState {
        return if (callLog.dir == Call.Dir.Incoming) {
            if (isCallLogMissed(callLog)) {
                CallState.MissCall
            } else {
                CallState.IncomingCall
            }
        } else {
            CallState.CallOutgoing
        }
    }

    private fun transport(transport: TransportType): CansTransport {
        return when (transport) {
            TransportType.Tcp -> CansTransport.TCP
            TransportType.Udp -> CansTransport.UDP
            TransportType.Tls -> CansTransport.TLS
            TransportType.Dtls -> CansTransport.TCP
        }
    }

    private fun addressEqual(address1: CansAddress, address2: CansAddress): Boolean {
        return address1.domain == address2.domain &&
                address1.password == address2.password &&
                address1.displayName == address2.displayName &&
                address1.transport == address2.transport &&
                address1.username == address2.username
    }

    override fun transferNow(phoneNumber: String): Boolean {
        val currentCall = core.currentCall ?: core.calls.firstOrNull()
        if (currentCall == null) {
            Log.e("[Context]", "Couldn't find a call to transfer")
            return false
        } else {
            val address = core.interpretUrl(phoneNumber)
            if (address != null) {
                Log.i("[Context]", "Transferring current call to $phoneNumber")
                currentCall.transferTo(address)
                return true
            } else {
                return false
            }
        }
    }

    override fun askFirst(phoneNumber: String): Boolean {
        if (core.callsNb == 2) {
            val calls = core.calls
            val firstCall = calls.first()
            val secondCall = calls[1]
            firstCall.transferToAnother(secondCall)
            return true
        } else {
            return false
        }
    }

    override fun dtmfKey(key: String) {
        val keyDtmf = key.single()
        core.playDtmf(keyDtmf, 1)
        core.currentCall?.sendDtmf(keyDtmf)

        if (mVibrator.hasVibrator() && corePreferences.dtmfKeypadVibration) {
            Compatibility.eventVibration(mVibrator)
        }
    }

    override fun mergeCallsIntoConference() {
        Thread {
            try {
                val callsCount = core.callsNb
                val defaultAccount = CansUtils.getDefaultAccount()
                val subject =
                    if (defaultAccount != null && defaultAccount.params.audioVideoConferenceFactoryAddress != null) {
                        Log.i(TAG, "Merging [$callsCount] calls into a remotely hosted conference")
                        coreContext.context.getString(R.string.conference_remotely_hosted_title)
                    } else {
                        Log.i(TAG, "Merging [$callsCount] calls into a locally hosted conference")
                        coreContext.context.getString(R.string.conference_locally_hosted_title)
                    }

                Log.i(TAG, "Merging [$callsCount] createGroupCall")
                val conference = CansUtils.createGroupCall(defaultAccount, subject)
                if (conference == null) {
                    Log.e(TAG, "Failed to create conference!")
                } else {
                    Log.i(TAG, "Merging [$callsCount] [${core.calls}] conference")
                    conference.addParticipants(core.calls)
                }
            } catch (e: Exception) {
                Log.i(TAG, "Merging: $e")
            }
        }.start()
    }

    override fun splitConference() {
        Thread {
            val participants =  conferenceCore.participantList
            for (i in 0 until participants.size.minus(1)) {
                Log.i("splitConference:", "${participants[i]?.address?.username}")
                conferenceCore.removeParticipant(participants[i])
            }
        }.start()
    }


    override fun signOutOKTADomain(
        activity: Activity,
        callback: (Boolean) -> Unit
    ) {
        OKTARepository.signOutOKTA(
            activity = activity
        ){
            resultCallback ->
            if (resultCallback){
                removeAccountAll()
            }
            callback(resultCallback)
        }
    }

    override fun signInOKTADomain(
        apiURL: String,
        domain: String,
        activity: Activity,
        onResult: (Int) -> Unit
    ) {
        var usernameOKTA: String
        var passwordOKTA: String
        var domainNameOKTA: String
        var transportOKTA: TransportType?

        var isWaitingWebView = false

        OKTARepository.fetchOKTAClient(
            apiURL = apiURL,
            inputDomain = domain,
            context = context,
        ) { response ->
            response?.let { data ->
                cansCenter().corePreferences.discoveryURL =
                    data.discoveryUrl
                cansCenter().corePreferences.clientID = data.clientId

                // Save Domain
                val loginInfo = cansCenter().corePreferences.loginInfo
                val newLoginInfo = loginInfo.copy(
                    domainOKTACurrent = data.domainName,
                )
                cansCenter().corePreferences.loginInfo = newLoginInfo

                OktaWebAuth.setupWebAuth(
                    context,
                    data.discoveryUrl,
                    data.clientId
                )

                OktaWebAuth.setupWebAuthCallback(
                    activity,
                    webAuth,
                    object :
                        OktaWebAuth.Companion.TokenRefreshCallback {
                        override fun onTokenRefreshed(
                            isAuthenticated: Boolean
                        ) {
                            val token =
                                cansCenter().corePreferences.loginInfo.tokenOkta
                                    ?: ""
                            val domainOKTA = data.domainName

                            if (isAuthenticated) {
                                fetchSignInOKTA(
                                    apiURL,
                                ) { signInResponse ->
                                    if (signInResponse != null) {
                                        if (signInResponse.data != null) {
                                            decryptLogin(signInResponse.data) { decodedPassword, signInData ->
                                                if (decodedPassword != null && signInData != null) {
                                                    usernameOKTA = signInData.sip_username ?: ""
                                                    passwordOKTA = decodedPassword
                                                    domainNameOKTA = signInResponse.data.domain_name
                                                    transportOKTA = TransportType.Tcp

//                                                    removeAccount()
                                                    removeAccountAll()
                                                    register(
                                                        usernameOKTA,
                                                        passwordOKTA,
                                                        domainNameOKTA,
                                                        "8446",
                                                        CansTransport.TCP
                                                    )
                                                } else {
                                                    registerListeners.forEach {
                                                        it.onRegistration(
                                                            RegisterState.FAIL
                                                        )
                                                    }
                                                }
                                            }
                                            onResult(signInResponse.code)
                                            cansCenter().corePreferences.isSignInOKTANotConnected =
                                                false
                                        } else { // ELSE signInResponse.data != null
                                            when (signInResponse.code) {
                                                301 -> {
                                                    cansCenter().corePreferences.isSignInOKTANotConnected =
                                                        true
                                                }

                                                else -> {
                                                    cansCenter().corePreferences.isSignInOKTANotConnected =
                                                        false
                                                }
                                            }
                                            onResult(signInResponse.code)
                                            registerListeners.forEach {
                                                it.onRegistration(
                                                    RegisterState.FAIL
                                                )
                                            }
                                        }
                                    } else { // ELSE signInResponse != null
                                        onResult(-1)
                                        registerListeners.forEach {
                                            it.onRegistration(
                                                RegisterState.FAIL
                                            )
                                        }
                                    }
                                }
                            } else { // ELSE isAuthenticated
                                onResult(-1)
                                registerListeners.forEach {
                                    it.onRegistration(
                                        RegisterState.FAIL
                                    )
                                }
                            }
                        }
                    })

                val payload =
                    AuthenticationPayload.Builder().build()
                webAuth.signIn(activity, payload)
                isWaitingWebView = true

            } ?: run {
                if (!isWaitingWebView) {
                    onResult(-1)
                    registerListeners.forEach {
                        it.onRegistration(
                            RegisterState.FAIL
                        )
                    }
                }
            }
        }
    }

    override fun fetchSignInOKTA(apiURL: String, callback: (SignInOKTAResponseData?) -> Unit){
        OKTARepository.fetchSignInOKTA(
            context,
            apiURL,
            cansCenter().corePreferences.loginInfo.tokenOkta ?: "",
            cansCenter().corePreferences.loginInfo.domainOKTACurrent ?: ""
        ) { signInResponse ->
            if (signInResponse != null) {
                callback(signInResponse)
            } else {
                callback(null)
            }
        }
    }

    private fun decryptLogin(
        dataLogin: SignInOKTAResponseData.Data,
        callback: (String?, SignIn?) -> Unit
    ) {
        val jsonString = AESFactory.decrypt(dataLogin.user_credentials)

        if (jsonString != null) {
            val dataSignIn = Gson().fromJson(jsonString, SignIn::class.java)
            val loginInfo = cansCenter().corePreferences.loginInfo
            val newLoginInfo = loginInfo.copy(
                tokenSignIn = dataLogin.access_token,
                logInType = LogInType.OKTA.value,
                domainOKTACurrent = dataLogin.domain_name,
            )
            cansCenter().corePreferences.loginInfo = newLoginInfo

            if (dataSignIn.sip_password != null) {
                decodeBase64(dataSignIn.sip_password) { passwordDecode ->
                    callback(passwordDecode, dataSignIn)
                }
            } else {
                callback(null, null)
            }
        } else {
            callback(null, null)
        }
    }

    private fun decodeBase64(encodedString: String, callback: (String?) -> Unit) {
        try {
            val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
            callback(String(decodedBytes, Charsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            callback(null)
        }
    }

    override fun isSignInOKTANotConnected(): Boolean {
        return corePreferences.isSignInOKTANotConnected
    }

    override fun checkSessionOKTAExpire(activity: Activity, callback: (Boolean) -> Unit) {
        if (core.callsNb == 0) {
            if (corePreferences.loginInfo?.logInType == LogInType.OKTA.value) {
                if (OktaWebAuth.isWebAuthInitialized()) {
                    // webAuth is ready to use
                    OktaWebAuth.checkSession(activity) { isSessionValid ->
                        if(isSessionValid){
//                            removeAccount()
                            removeAccountAll()
                        }
                        callback(isSessionValid)
                    }
                } else {
                    // webAuth is NOT initialized
                    callback(false)
                }
            }else {
                callback(false)
            }
        } else {
            callback(false)
        }
    }

    override fun addCansRegisterListener(listener: CansRegisterListenerStub) {
        registerListeners.add(listener)
    }

    override fun removeCansRegisterListener(listener: CansRegisterListenerStub) {
        registerListeners.remove(listener)
    }

    override fun addCansCallListener(listener: CansListenerStub) {
        listeners.add(listener)
    }

    override fun removeCansCallListener(listener: CansListenerStub) {
        listeners.remove(listener)
    }

    override fun addCansRegisterAccountListener(
        indexAccount: Int,
        listener: CansRegisterAccountListenerStub
    ) {
        val account = core.accountList[indexAccount]
        accountDefault = account
        accountDefault.addListener(accountListener)
        registerAccountListeners.add(listener)
    }

    override fun removeCansRegisterAccountListener(
        listener: CansRegisterAccountListenerStub
    ) {
        accountDefault.removeListener(accountListener)
        registerAccountListeners.remove(listener)
    }


    override fun removeAllListener() {
        listeners.clear()
        registerListeners.clear()
    }

    override fun testRegisterBcrypt(
        username: String,
        password: String,
        domain: String,
        port: String,
        transport: CansTransport
    ) {
        if (username.isEmpty() || password.isEmpty() || domain.isEmpty() || port.isEmpty()) {
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        val serverAddress = "${domain}:${port}"
        val transportType = if (transport.name.lowercase() == "tcp") {
            TransportType.Tcp
        } else {
            TransportType.Udp
        }

        Log.d("SDK","start create account by factory")
        val sipServer = "sip:$domain:$port;transport=${transportType.name.lowercase()}"

        val factory = Factory.instance()
        val sUsername = "1001"                      // จาก provisioning หรือ input
        val sDomain   = "sitmms.cans.cc"
//        val ha1Hex   = SecureUtils.md5("$username:$domain:$password")  // MD5(username:realm:password)

        Log.d("SDK","start createAuthInfo by factory")
        val auth = factory.createAuthInfo(
            sUsername,
            null,       // userid
            null,       // passwd (null เพราะใช้ ha1)
            "48d789f52af9bde24818bb3542320ccd",     // ha1
            sDomain,
            sDomain,
            null        // algorithm (MD5 default)
        )
        core.addAuthInfo(auth)

        Log.d("SDK","getAccountCreator start")
        accountCreator = getAccountCreator()
        Log.d("SDK","getAccountCreator ok accountCreator : $accountCreator")

        val resultUsername = accountCreator.setUsername(username)
        if (resultUsername != AccountCreator.UsernameStatus.Ok) {
            Log.e(
                "[Assistant]",
                " [Account Login] Error [${resultUsername.name}] setting the username: ${username}"
            )
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }
        Log.i("[Assistant]", "[Account Login] Username is ${accountCreator.username}")

        val resultDomain = accountCreator.setDomain(domain)
        if (resultDomain != AccountCreator.DomainStatus.Ok) {
            Log.e("[Assistant]", " [Account Login] Error [${resultDomain.name}] setting the domain")
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }
        accountCreator.transport = transportType

        Log.d("SDK","start createProxyConfig ")
        Log.d("SDK", "BEFORE accountList size = ${core.accountList.size}")
        val proxyConfig : ProxyConfig? = accountCreator.createProxyConfig()
        Log.d("SDK","start proxyConfig : $proxyConfig")
//        Log.d("SDK", "AFTER accountList size = ${core.accountList.size}")
        proxyConfigToCheck = proxyConfig

        if (proxyConfig == null) {
            Log.d("SDK","RegisterState.FAIL from Null ProxyConfig")
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        // สำคัญ: ใส่พอร์ตและทรานสปอร์ตให้ proxy/server
        Log.d("SDK","ใส่พอร์ตและทรานสปอร์ตให้ proxy/server")
        proxyConfig.edit()
        proxyConfig.serverAddr = sipServer
        proxyConfig.isRegisterEnabled = true
        proxyConfig.done()

        // เพิ่มเข้า core + ตั้ง default
        Log.d("SDK","เพิ่มเข้า core + ตั้ง default")
        if (!core.proxyConfigList.contains(proxyConfig)) {
            core.addProxyConfig(proxyConfig)
        }
        core.defaultProxyConfig = proxyConfig

        core.refreshRegisters()
        Log.d("SDK", "after add: accountList size = ${core.accountList.size}")

        corePreferences.keepServiceAlive = true
        coreContext.notificationsManager.startForeground()
    }

    override fun registerSIPBcrypt(
        username: String,
        password: String,
        domain: String,
        port: String,
        transport: CansTransport
    ) {
        if (username.isEmpty() || password.isEmpty() || domain.isEmpty() || port.isEmpty()) {
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        val transportType = if (transport.name.lowercase() == "tcp") {
            TransportType.Tcp
        } else {
            TransportType.Udp
        }

        Log.d("SDK","SIPBcrypt start create account by factory")
        val sipServer = "sip:$domain:$port;transport=${transportType.name.lowercase()}"

        val factory = Factory.instance()
        val ha1Hex   = SecureUtils.md5("$username:$domain:$password")  // MD5(username:realm:password)

        Log.d("SDK","SIPBcrypt start createAuthInfo by factory")
        val auth = factory.createAuthInfo(
            username,
            null,       // userid
            null,       // passwd (null เพราะใช้ ha1)
            ha1Hex,     // ha1
            domain,
            domain,
            null        // algorithm (MD5 default)
        )
        core.addAuthInfo(auth)

        Log.d("SDK","SIPBcrypt getAccountCreator start")
        accountCreator = getAccountCreator()
        Log.d("SDK","SIPBcrypt getAccountCreator ok accountCreator : $accountCreator")

        val resultUsername = accountCreator.setUsername(username)
        if (resultUsername != AccountCreator.UsernameStatus.Ok) {
            Log.e(
                "[Assistant]",
                " [Account Login] Error [${resultUsername.name}] setting the username: ${username}"
            )
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }
        Log.i("[Assistant]", "[Account Login] Username is ${accountCreator.username}")

        val resultDomain = accountCreator.setDomain(domain)
        if (resultDomain != AccountCreator.DomainStatus.Ok) {
            Log.e("[Assistant]", " [Account Login] Error [${resultDomain.name}] setting the domain")
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }
        accountCreator.transport = transportType

        Log.d("SDK","SIPBcrypt start createProxyConfig ")
        Log.d("SDK", "SIPBcrypt BEFORE accountList size = ${core.accountList.size}")
        val proxyConfig : ProxyConfig? = accountCreator.createProxyConfig()
        Log.d("SDK","SIPBcrypt start proxyConfig : $proxyConfig")
//        Log.d("SDK", "AFTER accountList size = ${core.accountList.size}")
        proxyConfigToCheck = proxyConfig

        if (proxyConfig == null) {
            Log.d("SDK","SIPBcrypt RegisterState.FAIL from Null ProxyConfig")
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        // สำคัญ: ใส่พอร์ตและทรานสปอร์ตให้ proxy/server
        Log.d("SDK","SIPBcrypt ใส่พอร์ตและทรานสปอร์ตให้ proxy/server")
        proxyConfig.edit()
        proxyConfig.serverAddr = sipServer
        proxyConfig.isRegisterEnabled = true
        proxyConfig.done()

        // เพิ่มเข้า core + ตั้ง default
        Log.d("SDK","SIPBcrypt เพิ่มเข้า core + ตั้ง default")
        if (!core.proxyConfigList.contains(proxyConfig)) {
            core.addProxyConfig(proxyConfig)
        }
        core.defaultProxyConfig = proxyConfig

        core.refreshRegisters()
        Log.d("SDK", "SIPBcrypt after add: accountList size = ${core.accountList.size}")

        corePreferences.keepServiceAlive = true
        coreContext.notificationsManager.startForeground()
    }

    override fun registerAccountBcrypt(username: String, password: String, domain: String) {
        if (username.isEmpty() || password.isEmpty() || domain.isEmpty()) {
            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

//        accountCreator = getAccountCreator()
//        val result = accountCreator.setUsername(username)
//        if (result != AccountCreator.UsernameStatus.Ok) {
//            Log.e(
//                "[Assistant]",
//                " [Account Login] Error [${result.name}] setting the username: ${username}"
//            )
//            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
//            return
//        }
//        Log.i("[Assistant]", "[Account Login] Username is ${accountCreator.username}")

//        val result2 = accountCreator.setPassword(password)
//        if (result2 != AccountCreator.PasswordStatus.Ok) {
//            Log.e("[Assistant]", " [Account Login] Error [${result2.name}] setting the password")
//            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
//            return
//        }

//        val result3 = accountCreator.setDomain(domain)
//        if (result3 != AccountCreator.DomainStatus.Ok) {
//            Log.e("[Assistant]", " [Account Login] Error [${result3.name}] setting the domain")
//            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
//            return
//        }

        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(ProvisioningInterceptor())
            .addNetworkInterceptor(
                ProvisioningInterceptor(),
            )
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://voxxycloud.com/Cpanel/")
            .build()

        val provisioningService: ProvisioningService =
            retrofit.create(ProvisioningService::class.java)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("idToken", "")
            .addFormDataPart("action", "provision")
            .addFormDataPart("username", username)
            .addFormDataPart("password", password)
            .build()

        val url = "https://" + domain + "/Cpanel/provision/mobile/"

        val callProvisioningData: retrofit2.Call<ProvisioningData?>? =
            provisioningService.getProvisioningData(url, requestBody)

        callProvisioningData?.let { callProvisioning ->
            callProvisioning.enqueue(
                object : retrofit2.Callback<ProvisioningData?> {
                    override fun onResponse(
                        call: retrofit2.Call<ProvisioningData?>,
                        response: retrofit2.Response<ProvisioningData?>,
                    ) {
                        Log.i("Response success", response.message())
                        if (response.isSuccessful) {
                            response.body().let { body ->
                                val provisioningData: ProvisioningData? = body
                                provisioningData?.let { provisioning ->
                                    val results: List<ProvisioningResult> = provisioning.results
                                    if (results.isNotEmpty()) {
                                        val result = results[0]

                                        val resUsername = result.extension?.trim() ?: ""
                                        val resPassword = result.secret?.trim() ?: ""
                                        val resDomain = result.domain?.trim() ?: ""
                                        val resTransport = if (result.transport?.lowercase() == "tcp") {
                                            TransportType.Tcp
                                        } else {
                                            TransportType.Udp
                                        }

                                        Log.d("SDK","registerAccountBcrypt resUsername : $resUsername")
                                        Log.d("SDK","registerAccountBcrypt resPassword : $resPassword")
                                        Log.d("SDK","registerAccountBcrypt resDomain : $resDomain")
                                        Log.d("SDK","registerAccountBcrypt resTransport : $resTransport")

                                        val factory = Factory.instance()
//                                        val ha1Hex   = SecureUtils.md5("$resUsername:$resDomain:$resPassword")
                                        val sipServer = "sip:$resDomain;transport=${resTransport.name.lowercase()}"
                                        Log.d("SDK","registerAccountBcrypt sipServer : $sipServer")

                                        // แยก host/port ให้ชัด
                                        val host: String
                                        if (resDomain.contains(":")) {
                                            val parts = resDomain.split(":")
                                            host = parts[0]
                                        } else {
                                            host = resDomain
                                        }
                                        Log.d("SDK","registerAccountBcrypt host : $host")

                                        val ha1Hex   = SecureUtils.md5("$resUsername:$host:$resPassword")

                                        Log.d("SDK","registerAccountBcrypt start createAuthInfo by factory")
                                        val auth = factory.createAuthInfo(
                                            resUsername,
                                            null,
                                            null,
                                            ha1Hex,
                                            host,
                                            host,
                                            null
                                        )
                                        core.addAuthInfo(auth)

                                        Log.d("SDK","registerAccountBcrypt getAccountCreator start")
                                        accountCreator = getAccountCreator()
                                        Log.d("SDK","registerAccountBcrypt getAccountCreator ok accountCreator : $accountCreator")

                                        val resultUsername = accountCreator.setUsername(username)
                                        if (resultUsername != AccountCreator.UsernameStatus.Ok) {
                                            Log.e(
                                                "[Assistant]",
                                                " [Account Login] Error [${resultUsername.name}] setting the username: ${username}"
                                            )
                                            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
                                            return
                                        }
                                        Log.i("[Assistant]", "[Account Login] Username is ${accountCreator.username}")

                                        val resultDomain = accountCreator.setDomain(domain)
                                        if (resultDomain != AccountCreator.DomainStatus.Ok) {
                                            Log.e("[Assistant]", " [Account Login] Error [${resultDomain.name}] setting the domain")
                                            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
                                            return
                                        }
                                        accountCreator.transport = resTransport

                                        Log.d("SDK","registerAccountBcrypt start createProxyConfig ")
                                        Log.d("SDK", "registerAccountBcrypt BEFORE accountList size = ${core.accountList.size}")
                                        val proxyConfig : ProxyConfig? = accountCreator.createProxyConfig()
                                        Log.d("SDK","registerAccountBcrypt start proxyConfig : $proxyConfig")
                                        proxyConfigToCheck = proxyConfig

                                        if (proxyConfig == null) {
                                            Log.d("SDK","registerAccountBcrypt RegisterState.FAIL from Null ProxyConfig")
                                            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
                                            return
                                        }

                                        // สำคัญ: ใส่พอร์ตและทรานสปอร์ตให้ proxy/server
                                        Log.d("SDK","registerAccountBcrypt ใส่พอร์ตและทรานสปอร์ตให้ proxy/server")
                                        proxyConfig.edit()
                                        proxyConfig.serverAddr = sipServer
                                        proxyConfig.isRegisterEnabled = true
                                        proxyConfig.done()

                                        // เพิ่มเข้า core + ตั้ง default
                                        Log.d("SDK","registerAccountBcrypt เพิ่มเข้า core + ตั้ง default")
                                        if (!core.proxyConfigList.contains(proxyConfig)) {
                                            core.addProxyConfig(proxyConfig)
                                        }
                                        core.defaultProxyConfig = proxyConfig

                                        core.refreshRegisters()
                                        Log.d("SDK", "registerAccountBcrypt after add: accountList size = ${core.accountList.size}")

                                        corePreferences.keepServiceAlive = true
                                        coreContext.notificationsManager.startForeground()
                                    }
                                }
                            }
                        } else {
                            registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
                            return
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<ProvisioningData?>,
                        t: Throwable,
                    ) {
                        registerListeners.forEach { it.onRegistration(RegisterState.FAIL) }
                        Log.e("Response fail", "${t.message}")
                    }
                },
            )
        }
    }
}
