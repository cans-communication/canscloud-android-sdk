package cc.cans.canscloud.demoappinsdk.core

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import cc.cans.canscloud.demoappinsdk.call.CallActivity
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity
import cc.cans.canscloud.demoappinsdk.call.OutgoingActivity
import cc.cans.canscloud.demoappinsdk.notifaication.NotificationsManager
import cc.cans.canscloud.sdk.utils.AudioRouteUtils
import cc.cans.canscloud.sdk.utils.PermissionHelper
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.telecom.TelecomHelper
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.Cans.Companion.core
import cc.cans.canscloud.sdk.Cans.Companion.corePreferences
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState
import kotlinx.coroutines.cancel
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.R
import org.linphone.mediastream.Version
import java.io.File
import cc.cans.canscloud.sdk.compatibility.PhoneStateInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.linphone.core.Factory
import org.linphone.core.LogLevel
import org.linphone.core.LoggingService
import org.linphone.core.LoggingServiceListenerStub
import org.linphone.core.Reason

class CoreContext(
    val context: Context,
    ) : LifecycleOwner, ViewModelStoreOwner {
    private val _lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = _lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    val notificationsManager: NotificationsManager by lazy {
        NotificationsManager(context)
    }
    private lateinit var phoneStateListener: PhoneStateInterface
    var stopped = false

    private var previousCallState = CallState.Idle
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val loggingService = Factory.instance().loggingService

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[SharedMainViewModel]","onRegistration ${state}")
        }

        override fun onUnRegister() {
            Log.i("[Context]","onUnRegistration")
        }

        override fun onCallState(core: Core, call: Call,state: CallState, message: String?) {
            Log.i("[Context] onCallState: ","$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> {
                    if (declineCallDueToGsmActiveCall()) {
                        call.decline(Reason.Busy)
                        return
                    }

                    onIncomingReceived()
                }
                CallState.StartCall -> {
                    // OutgoingInit
                    onOutgoingStarted()
                }
                CallState.CallOutgoing -> {
                    if (core.callsNb == 1 && corePreferences.routeAudioToBluetoothIfAvailable) {
                        AudioRouteUtils.routeAudioToBluetooth(call)
                    }
                }
                CallState.Connected -> {
                    onCallStarted()
                }
                CallState.StreamsRunning -> {
                    if (core.callsNb == 1 && previousCallState == CallState.Connected) {
                        if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToHeadset(call)
                        } else if (AudioRouteUtils.isBluetoothAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToBluetooth(call)
                        }
                    }
                }
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {}
                CallState.Unknown -> {}
            }
            previousCallState = state
        }

        override fun onLastCallEnded() {
            Log.i("[Context]", "onLastCallEnded")
        }

        override fun onAudioDeviceChanged() {
            Log.i("[Context onAudioUpdate]", "onAudioDeviceChanged")
        }

        override fun onAudioDevicesListUpdated() {
            Log.i("[Context onAudioUpdate]", "onAudioDevicesListUpdated")
        }
    }

    private val loggingServiceListener = object : LoggingServiceListenerStub() {
        override fun onLogMessageWritten(
            logService: LoggingService,
            domain: String,
            level: LogLevel,
            message: String,
        ) {
            if (corePreferences.logcatLogsOutput) {
                when (level) {
                    LogLevel.Error -> Log.e(domain, message)
                    LogLevel.Warning -> Log.w(domain, message)
                    LogLevel.Message -> Log.i(domain, message)
                    LogLevel.Fatal -> Log.wtf(domain, message)
                    else -> Log.d(domain, message)
                }
            }
        }
    }

    init {
        Cans.addListener(listener)
        stopped = false
        _lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        Log.i("[Context]","Ready")
    }

    fun start(isPush: Boolean = false) {

        Cans.addListener(listener)
        // CoreContext listener must be added first!
        if (Version.sdkAboveOrEqual(Version.API26_O_80) && corePreferences.useTelecomManager) {
            if (Compatibility.hasTelecomManagerPermissions(context)) {
                Log.i(
                    "[Context]","Creating Telecom Helper, disabling audio focus requests in AudioHelper"
                )
                core.config.setBool("audio", "android_disable_audio_focus_requests", true)
                val telecomHelper = TelecomHelper.required(context)
                Log.i(
                    "[Context]","Telecom Helper created, account is ${if (telecomHelper.isAccountEnabled()) "enabled" else "disabled"}"
                )
            } else {
                Log.i("[Context]","Can't create Telecom Helper, permissions have been revoked")
                corePreferences.useTelecomManager = false
            }
        }

        if (isPush) {
            org.linphone.core.tools.Log.i("[Context] Push received, assume in background")
            core.enterBackground()
        }

        configureCore()

        _lifecycleRegistry.currentState = Lifecycle.State.CREATED
        core.start()
        _lifecycleRegistry.currentState = Lifecycle.State.STARTED

        initPhoneStateListener()

        notificationsManager.onCoreReady()

        _lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        Log.i("[Context]"," Started")
    }

    fun stop() {
        Log.i("[Context]"," Stopping")
        coroutineScope.cancel()

        if (::phoneStateListener.isInitialized) {
            phoneStateListener.destroy()
        }

        if (TelecomHelper.exists()) {
            Log.i("[Context]"," Destroying telecom helper")
            TelecomHelper.get().destroy()
            TelecomHelper.destroy()
        }

        core.stop()
        Cans.removeListener(listener)
        stopped = true
        loggingService.removeListener(loggingServiceListener)

        _lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    private fun configureCore() {
        Log.i("[Context]"," Configuring Core")

        core.staticPicture = corePreferences.staticPicturePath

        // Migration code
        if (core.config.getBool("app", "incoming_call_vibration", true)) {
            core.isVibrationOnIncomingCallEnabled = true
            core.config.setBool("app", "incoming_call_vibration", false)
        }

        // Disable Telecom Manager on Android < 10 to prevent crash due to OS bug in Android 9
        if (Version.sdkStrictlyBelow(Version.API29_ANDROID_10)) {
            if (corePreferences.useTelecomManager) {
                org.linphone.core.tools.Log.w("[Context] Android < 10 detected, disabling telecom manager to prevent crash due to OS bug")
            }
            corePreferences.useTelecomManager = false
            corePreferences.manuallyDisabledTelecomManager = true
        }

        initUserCertificates()

        computeUserAgent()

        for (account in core.accountList) {
            if (account.params.identityAddress?.domain == corePreferences.defaultDomain) {
                // Ensure conference URI is set on sip.linphone.org proxy configs
                if (account.params.conferenceFactoryUri == null) {
                    val params = account.params.clone()
                    val uri = corePreferences.conferenceServerUri
                    org.linphone.core.tools.Log.i("[Context] Setting conference factory on proxy config ${params.identityAddress?.asString()} to default value: $uri")
                    params.conferenceFactoryUri = uri
                    account.params = params
                }

                // Ensure LIME server URL is set if at least one sip.linphone.org proxy
                if (core.limeX3DhAvailable()) {
                    var url: String? = core.limeX3DhServerUrl
                    if (url == null || url.isEmpty()) {
                        url = corePreferences.limeX3dhServerUrl
                        org.linphone.core.tools.Log.i("[Context] Setting LIME X3Dh server url to default value: $url")
                        core.limeX3DhServerUrl = url
                    }
                }

                // Ensure we allow CPIM messages in basic chat rooms
                val newParams = account.params.clone()
                newParams.isCpimInBasicChatRoomEnabled = true
                account.params = newParams
                org.linphone.core.tools.Log.i("[Context] CPIM allowed in basic chat rooms for account ${newParams.identityAddress?.asStringUriOnly()}")
            }
        }

        Log.i("[Context]", "Core configured")
    }

    private fun computeUserAgent() {
        val deviceName: String = corePreferences.deviceName
        val appName: String = context.resources.getString(R.string.linphone_sdk_branch)
        val androidVersion = cc.cans.canscloud.demoappinsdk.BuildConfig.VERSION_NAME
        val userAgent = "$appName/$androidVersion ($deviceName) LinphoneSDK"
        val sdkVersion = context.getString(org.linphone.core.R.string.linphone_sdk_version)
        val sdkBranch = context.getString(org.linphone.core.R.string.linphone_sdk_branch)
        val sdkUserAgent = "$sdkVersion ($sdkBranch)"
        core.setUserAgent(userAgent, sdkUserAgent)
    }

    private fun initUserCertificates() {
        val userCertsPath = corePreferences.userCertificatesPath
        val f = File(userCertsPath)
        if (!f.exists() && !f.mkdir()) {
            org.linphone.core.tools.Log.e("[Context]", "$userCertsPath can't be created.")
        }
        core.userCertificatesPath = userCertsPath
    }

    /* Call related functions */
    fun initPhoneStateListener() {
        if (PermissionHelper.required(context).hasReadPhoneStatePermission()) {
            try {
                phoneStateListener =
                    Compatibility.createPhoneListener(context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            } catch (exception: SecurityException) {
                val hasReadPhoneStatePermission =
                    PermissionHelper.get().hasReadPhoneStateOrPhoneNumbersPermission()
                Log.e("[Context]"," Failed to create phone state listener: $exception, READ_PHONE_STATE permission status is $hasReadPhoneStatePermission")
            }
        } else {
            Log.w("[Context]","   Can't create phone state listener, READ_PHONE_STATE permission isn't granted")
        }
    }

    fun declineCallDueToGsmActiveCall(): Boolean {
        if (!corePreferences.useTelecomManager) { // Can't use the following call with Telecom Manager API as it will "fake" GSM calls
            var gsmCallActive = false
            if (::phoneStateListener.isInitialized) {
                gsmCallActive = phoneStateListener.isInCall()
            }

            if (gsmCallActive) {
                Log.w("[Context]"," Refusing the call with reason busy because a GSM call is active")
                return true
            }
        } else {
            if (TelecomHelper.exists()) {
                if (!TelecomHelper.get().isIncomingCallPermitted() ||
                    TelecomHelper.get().isInManagedCall()
                ) {
                    Log.w("[Context]"," Refusing the call with reason busy because Telecom Manager will reject the call")
                    return true
                }
            } else {
                Log.e("[Context]"," Telecom Manager singleton wasn't created!")
            }
        }
        return false
    }

    /* Start call related activities */

    private fun onIncomingReceived() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the incoming call screen")
            return
        }

        Log.i("[Context]","Starting IncomingCallActivity")
        val intent = Intent(context, IncomingActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun onOutgoingStarted() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the outgoing call screen")
            return
        }

        Log.i("[Context]","Starting OutgoingCallActivity")
        val intent = Intent(context, OutgoingActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onCallStarted() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the call screen")
            return
        }

        Log.i("[Context]","Starting CallActivity")
        val intent = Intent(context, CallActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(intent)
    }

}
