package cc.cans.canscloud.sdk.core

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import cc.cans.canscloud.sdk.utils.AudioRouteUtils
import cc.cans.canscloud.sdk.utils.PermissionHelper
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.telecom.TelecomHelper
import cc.cans.canscloud.sdk.CansCenter
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState
import kotlinx.coroutines.cancel
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

class CoreContextSDK(
    val context: Context,
    ) : LifecycleOwner, ViewModelStoreOwner {

    companion object {
        var cans: Cans = CansCenter()
    }

    private val _lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = _lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

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

        override fun onCallState(state: CallState, message: String?) {
            Log.i("[Context] onCallState: ","$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> {
                    if (declineCallDueToGsmActiveCall()) {
                        cans.callCans.decline(Reason.Busy)
                        return
                    }
                }
                CallState.StartCall -> {}
                CallState.CallOutgoing -> {
                    if (cans.core.callsNb == 1 && cans.corePreferences.routeAudioToBluetoothIfAvailable) {
                        AudioRouteUtils.routeAudioToBluetooth(cans.callCans)
                    }
                }
                CallState.Connected -> {}
                CallState.StreamsRunning -> {
                    if (cans.core.callsNb == 1 && previousCallState == CallState.Connected) {
                        if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToHeadset(cans.callCans)
                        } else if (AudioRouteUtils.isBluetoothAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToBluetooth(cans.callCans)
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
            if (cans.corePreferences.logcatLogsOutput) {
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
        cans.addListener(listener)
        stopped = false
        _lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        Log.i("[Context]","Ready")
    }

    fun start(isPush: Boolean = false) {

        cans.addListener(listener)
        // CoreContext listener must be added first!
//        if (Version.sdkAboveOrEqual(Version.API26_O_80) && cans.corePreferences.useTelecomManager) {
//            if (Compatibility.hasTelecomManagerPermissions(context)) {
//                Log.i(
//                    "[Context]","Creating Telecom Helper, disabling audio focus requests in AudioHelper"
//                )
//                cans.core.config.setBool("audio", "android_disable_audio_focus_requests", true)
//                val telecomHelper = TelecomHelper.required(context)
//                Log.i(
//                    "[Context]","Telecom Helper created, account is ${if (telecomHelper.isAccountEnabled()) "enabled" else "disabled"}"
//                )
//            } else {
//                Log.i("[Context]","Can't create Telecom Helper, permissions have been revoked")
//                cans.corePreferences.useTelecomManager = false
//            }
//        }
//
//        if (isPush) {
//            org.linphone.core.tools.Log.i("[Context] Push received, assume in background")
//            cans.core.enterBackground()
//        }

        configureCore()

        _lifecycleRegistry.currentState = Lifecycle.State.CREATED
        cans.core.start()
        _lifecycleRegistry.currentState = Lifecycle.State.STARTED

        initPhoneStateListener()

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

        cans.core.stop()
        cans.removeListener(listener)
        stopped = true
        loggingService.removeListener(loggingServiceListener)

        _lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    private fun configureCore() {
        Log.i("[Context]"," Configuring Core")

        cans.core.staticPicture = cans.corePreferences.staticPicturePath

        // Migration code
        if (cans.core.config.getBool("app", "incoming_call_vibration", true)) {
            cans.core.isVibrationOnIncomingCallEnabled = true
            cans.core.config.setBool("app", "incoming_call_vibration", false)
        }

        // Disable Telecom Manager on Android < 10 to prevent crash due to OS bug in Android 9
        if (Version.sdkStrictlyBelow(Version.API29_ANDROID_10)) {
            if (cans.corePreferences.useTelecomManager) {
                org.linphone.core.tools.Log.w("[Context] Android < 10 detected, disabling telecom manager to prevent crash due to OS bug")
            }
            cans.corePreferences.useTelecomManager = false
            cans.corePreferences.manuallyDisabledTelecomManager = true
        }

        initUserCertificates()

        for (account in cans.core.accountList) {
            if (account.params.identityAddress?.domain == cans.corePreferences.defaultDomain) {
                // Ensure conference URI is set on sip.linphone.org proxy configs
                if (account.params.conferenceFactoryUri == null) {
                    val params = account.params.clone()
                    val uri = cans.corePreferences.conferenceServerUri
                    org.linphone.core.tools.Log.i("[Context] Setting conference factory on proxy config ${params.identityAddress?.asString()} to default value: $uri")
                    params.conferenceFactoryUri = uri
                    account.params = params
                }

                // Ensure LIME server URL is set if at least one sip.linphone.org proxy
                if (cans.core.limeX3DhAvailable()) {
                    var url: String? = cans.core.limeX3DhServerUrl
                    if (url == null || url.isEmpty()) {
                        url = cans.corePreferences.limeX3dhServerUrl
                        org.linphone.core.tools.Log.i("[Context] Setting LIME X3Dh server url to default value: $url")
                        cans.core.limeX3DhServerUrl = url
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

    private fun initUserCertificates() {
        val userCertsPath = cans.corePreferences.userCertificatesPath
        val f = File(userCertsPath)
        if (!f.exists() && !f.mkdir()) {
            org.linphone.core.tools.Log.e("[Context]", "$userCertsPath can't be created.")
        }
        cans.core.userCertificatesPath = userCertsPath
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
        if (!cans.corePreferences.useTelecomManager) { // Can't use the following call with Telecom Manager API as it will "fake" GSM calls
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
}
