package cc.cans.canscloud.sdk.core

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.util.Pair
import androidx.annotation.Keep
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
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CoreContextSDK(
    val context: Context,
    ) : LifecycleOwner, ViewModelStoreOwner {

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

    val notificationsManager: NotificationsManager by lazy {
        NotificationsManager(context)
    }

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

    fun start() {

        cans.addListener(listener)
        //CoreContext listener must be added first!
        if (Version.sdkAboveOrEqual(Version.API26_O_80) && cans.corePreferences.useTelecomManager) {
            if (Compatibility.hasTelecomManagerPermissions(context)) {
                Log.i(
                    "[Context]","Creating Telecom Helper, disabling audio focus requests in AudioHelper"
                )
                cans.core.config.setBool("audio", "android_disable_audio_focus_requests", true)
                val telecomHelper = TelecomHelper.singletonHolder().required(context)
                Log.i(
                    "[Context]","Telecom Helper created, account is ${if (telecomHelper.isAccountEnabled()) "enabled" else "disabled"}"
                )
            } else {
                Log.i("[Context]","Can't create Telecom Helper, permissions have been revoked")
                cans.corePreferences.useTelecomManager = false
            }
        }

        configureCore()

        _lifecycleRegistry.currentState = Lifecycle.State.CREATED
        cans.core.start()
        _lifecycleRegistry.currentState = Lifecycle.State.STARTED

        initPhoneStateListener()

        notificationsManager.startForeground()

        _lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        Log.i("[Context]"," Started")
    }

    fun stop() {
        Log.i("[Context]"," Stopping")
        coroutineScope.cancel()

        if (::phoneStateListener.isInitialized) {
            phoneStateListener.destroy()
        }

        if (TelecomHelper.singletonHolder().exists()) {
            Log.i("[Context]"," Destroying telecom helper")
            TelecomHelper.singletonHolder().get().destroy()
            TelecomHelper.singletonHolder().destroy()
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
                    Log.i("[Context]", " Setting conference factory on proxy config ${params.identityAddress?.asString()} to default value: $uri")
                    params.conferenceFactoryUri = uri
                    account.params = params
                }

                // Ensure LIME server URL is set if at least one sip.linphone.org proxy
                if (cans.core.limeX3DhAvailable()) {
                    var url: String? = cans.core.limeX3DhServerUrl
                    if (url == null || url.isEmpty()) {
                        url = cans.corePreferences.limeX3dhServerUrl
                        Log.i("[Context]", " Setting LIME X3Dh server url to default value: $url")
                        cans.core.limeX3DhServerUrl = url
                    }
                }

                // Ensure we allow CPIM messages in basic chat rooms
                val newParams = account.params.clone()
                newParams.isCpimInBasicChatRoomEnabled = true
                account.params = newParams
                Log.i("[Context]", " CPIM allowed in basic chat rooms for account ${newParams.identityAddress?.asStringUriOnly()}")
            }
        }

        Log.i("[Context]", "Core configured")
    }

    private fun initUserCertificates() {
        val userCertsPath = cans.corePreferences.userCertificatesPath
        val f = File(userCertsPath)
        if (!f.exists() && !f.mkdir()) {
            Log.i("[Context]", " $userCertsPath can't be created.")
        }
        cans.core.userCertificatesPath = userCertsPath
    }

    /* Call related functions */
    fun initPhoneStateListener() {
        if (PermissionHelper.singletonHolder().required(context).hasReadPhoneStatePermission()) {
            try {
                phoneStateListener =
                    Compatibility.createPhoneListener(context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            } catch (exception: SecurityException) {
                val hasReadPhoneStatePermission =
                    PermissionHelper.singletonHolder().get().hasReadPhoneStateOrPhoneNumbersPermission()
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
            if (TelecomHelper.singletonHolder().exists()) {
                if (!TelecomHelper.singletonHolder().get().isIncomingCallPermitted() ||
                    TelecomHelper.singletonHolder().get().isInManagedCall()
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

    /* VFS */

    @Keep
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val ALIAS = "vfs"
        private const val LINPHONE_VFS_ENCRYPTION_AES256GCM128_SHA256 = 2
        private const val VFS_IV = "vfsiv"
        private const val VFS_KEY = "vfskey"

        private val cans: Cans = CansCenter()

        @JvmStatic
        fun cansCenter(): Cans {
            return cans
        }

        @Throws(java.lang.Exception::class)
        private fun generateSecretKey() {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            keyGenerator.generateKey()
        }

        @Throws(java.lang.Exception::class)
        private fun getSecretKey(): SecretKey? {
            val ks = KeyStore.getInstance(ANDROID_KEY_STORE)
            ks.load(null)
            val entry = ks.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        @Throws(java.lang.Exception::class)
        fun generateToken(): String {
            return sha512(UUID.randomUUID().toString())
        }

        @Throws(java.lang.Exception::class)
        private fun encryptData(textToEncrypt: String): Pair<ByteArray, ByteArray> {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            return Pair<ByteArray, ByteArray>(
                iv,
                cipher.doFinal(textToEncrypt.toByteArray(StandardCharsets.UTF_8)),
            )
        }

        @Throws(java.lang.Exception::class)
        private fun decryptData(encrypted: String?, encryptionIv: ByteArray): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, encryptionIv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val encryptedData = Base64.decode(encrypted, Base64.DEFAULT)
            return String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8)
        }

        @Throws(java.lang.Exception::class)
        fun encryptToken(string_to_encrypt: String): Pair<String?, String?> {
            val encryptedData = encryptData(string_to_encrypt)
            return Pair<String?, String?>(
                Base64.encodeToString(encryptedData.first, Base64.DEFAULT),
                Base64.encodeToString(encryptedData.second, Base64.DEFAULT),
            )
        }

        @Throws(java.lang.Exception::class)
        fun sha512(input: String): String {
            val md = MessageDigest.getInstance("SHA-512")
            val messageDigest = md.digest(input.toByteArray())
            val no = BigInteger(1, messageDigest)
            var hashtext = no.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            return hashtext
        }

        @Throws(java.lang.Exception::class)
        fun getVfsKey(sharedPreferences: SharedPreferences): String {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)
            return decryptData(
                sharedPreferences.getString(VFS_KEY, null),
                Base64.decode(sharedPreferences.getString(VFS_IV, null), Base64.DEFAULT),
            )
        }

        fun activateVFS() {
            try {
                Log.i("[Context]", " Activating VFS")
                val preferences = cansCenter().corePreferences.encryptedSharedPreferences
                if (preferences == null) {
                    Log.i("[Context]", " Can't get encrypted SharedPreferences, can't init VFS")
                    return
                }

                if (preferences.getString(VFS_IV, null) == null) {
                    generateSecretKey()
                    encryptToken(generateToken()).let { data ->
                        preferences
                            .edit()
                            .putString(VFS_IV, data.first)
                            .putString(VFS_KEY, data.second)
                            .commit()
                    }
                }
                Factory.instance().setVfsEncryption(
                    LINPHONE_VFS_ENCRYPTION_AES256GCM128_SHA256,
                    getVfsKey(preferences).toByteArray().copyOfRange(0, 32),
                    32,
                )

                Log.i("[Context]", " VFS activated")
            } catch (e: Exception) {
                Log.i("[Context]", " Unable to activate VFS encryption: $e")
            }
        }
    }
}
