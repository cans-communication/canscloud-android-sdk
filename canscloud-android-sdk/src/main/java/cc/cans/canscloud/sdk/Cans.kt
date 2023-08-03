package cc.cans.canscloud.sdk

import UserService
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import cc.cans.canscloud.sdk.CansCloudApplication.Companion.coreContext
import cc.cans.canscloud.sdk.CansCloudApplication.Companion.corePreferences
import cc.cans.canscloud.sdk.callback.ContextCallback
import cc.cans.canscloud.sdk.core.CoreContext
import com.google.gson.Gson
import org.linphone.core.AccountCreator
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.ProxyConfig
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.core.tools.Log
import java.io.IOException
import java.io.InputStream
import java.util.Locale

class Cans {
    companion object {
        private lateinit var core: Core
        private var proxyConfigToCheck: ProxyConfig? = null
        private lateinit var accountCreator: AccountCreator
        private var useGenericSipAccount: Boolean = false
        var packageManager : PackageManager? = null
        var packageName : String = ""

        fun config(
            activity: Activity,
            packageManager: PackageManager,
            packageName: String,
            companyKey: String,
            callback: () -> Unit
        ) {
            CansCloudApplication.ensureCoreExists(activity.applicationContext)
            Companion.packageManager = packageManager
            Companion.packageName = packageName

            if (username().isEmpty()) {
                register(activity)
            }
            callback()

//            val factory = Factory.instance()
//            factory.setDebugMode(true, "Hello Linphone")
//            core = factory.createCore(null, null, context)
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

        /*fun login(activity: Activity) {
            var username = "50104"
            var password = "p50104CNS"
            var domain = "test.cans.cc:8446"
            val transportType = TransportType.Tcp
            val authInfo = Factory.instance().createAuthInfo(username, null, password, null, null, domain, null)

            val params = core.createAccountParams()
            val identity = Factory.instance().createAddress("sip:$username@$domain")
            params.identityAddress = identity

            val address = Factory.instance().createAddress("sip:$domain")
            address?.transport = transportType
            params.serverAddress = address
            params.isRegisterEnabled = true
            val account = core.createAccount(params)

            core.addAuthInfo(authInfo)
            core.addAccount(account)

            // Asks the CaptureTextureView to resize to match the captured video's size ratio
            //core.config.setBool("video", "auto_resize_preview_to_keep_ratio", true)

            core.defaultAccount = account
            core.addListener(coreListener)
            core.start()

            // We will need the RECORD_AUDIO permission for video call
            if (packageManager?.checkPermission(Manifest.permission.RECORD_AUDIO, packageName) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
                return
            }
        }*/

        private fun register(activity: Activity) {
            val fileName = "json/get_user.json"
            val jsonString = loadJSONFromAsset(context = activity.applicationContext, fileName)

            jsonString?.let {
                val gson = Gson()
                val user = gson.fromJson(it, UserService::class.java)

                val domain = "${user.domain}:${user.port}"

                val accountCreator = getAccountCreator(true)
                coreContext.core.addListener(coreListener)

                accountCreator.username = user.username
                accountCreator.password = user.password
                accountCreator.domain = domain
                accountCreator.displayName = ""

                if (user.transport.lowercase() == "tcp") {
                    accountCreator.transport = TransportType.Tcp
                } else {
                    accountCreator.transport = TransportType.Udp
                }

                val proxyConfig: ProxyConfig? = accountCreator.createProxyConfig()
                proxyConfigToCheck = proxyConfig

                if (proxyConfig == null) {
                    Log.e("[Assistant] [Generic Login] Account creator couldn't create proxy config")
                    coreContext.core.removeListener(coreListener)
                    //  onErrorEvent.value = Event("Error: Failed to create account object")
                    //waitForServerAnswer.value = false
                    return
                }

                Log.i("[Assistant] [Generic Login] Proxy config created")
                // The following is required to keep the app alive
                // and be able to receive calls while in background
                if (domain != corePreferences.defaultDomain) {
                    Log.i(
                        "[Assistant] [Generic Login] Background mode with foreground service automatically enabled"
                    )
                    //corePreferences.keepServiceAlive = true
                    coreContext.notificationsManager.startForeground()
                }

                if (packageManager?.checkPermission(Manifest.permission.RECORD_AUDIO, packageName) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
                    return
                }
            }
        }

        fun username(): String {
            coreContext.core.defaultAccount?.params?.identityAddress?.let {
                return "${it.username}@${it.domain}:${it.port}"
            }
            return ""
        }

        private fun getAccountCreator(genericAccountCreator: Boolean = false): AccountCreator {
            coreContext.core.loadConfigFromXml(corePreferences.linphoneDefaultValuesPath)
            accountCreator =
                coreContext.core.createAccountCreator(corePreferences.xmlRpcServerUrl)
            accountCreator.language = Locale.getDefault().language

            if (genericAccountCreator != useGenericSipAccount) {
                accountCreator.reset()
                accountCreator.language = Locale.getDefault().language

                if (genericAccountCreator) {
                    Log.i("[Assistant] Loading default values")
                    coreContext.core.loadConfigFromXml(corePreferences.defaultValuesPath)
                } else {
                    Log.i("[Assistant] Loading linphone default values")
                    coreContext.core.loadConfigFromXml(corePreferences.linphoneDefaultValuesPath)
                }
                useGenericSipAccount = genericAccountCreator
            }
            return accountCreator
        }


        fun getCountCalls(): Int {
            val call = coreContext.core.callsNb
            Log.i("[Application] getCountCalls : $call")
            return call
        }

        fun startCall(addressToCall: String) {

            android.util.Log.d("MainActivity : ", "startCall")
            //coreContext.outgoingCall()
            val addressToCall = addressToCall
            if (addressToCall.isNotEmpty()) {
                coreContext.startCall(addressToCall)
                //   eraseAll()
            } else {
                //setLastOutgoingCallAddress()
            }
        }


        fun terminateCall() {
            if (coreContext.core.callsNb == 0) return

            // If the call state isn't paused, we can get it using core.currentCall
            val call = if (coreContext.core.currentCall != null) coreContext.core.currentCall else coreContext.core.calls[0]
            call ?: return

            // Terminating a call is quite simple
            call.terminate()
        }

        fun durationTime() : Int? {
            val durationTime = coreContext.core.currentCall?.duration
            return durationTime
        }

        private fun setLastOutgoingCallAddress() {
            val callLog = coreContext.core.lastOutgoingCallLog
            if (callLog != null) {
                //  enteredUri.value = LinphoneUtils.getDisplayableAddress(callLog.remoteAddress).substringBefore("@").substringAfter("sip:")
            }
        }

        private val coreListener = object : CoreListenerStub() {
            override fun onRegistrationStateChanged(
                core: Core,
                cfg: ProxyConfig,
                state: RegistrationState,
                message: String
            ) {
                if (cfg == proxyConfigToCheck) {
                    Log.i("[Assistant] [Generic Login] Registration state is $state: $message")
                    if (state == RegistrationState.Ok) {
//                        waitForServerAnswer.value = false
//                        leaveAssistantEvent.value = Event(true)
                        core.removeListener(this)
                    } else if (state == RegistrationState.Failed) {
//                        waitForServerAnswer.value = false
//                        invalidCredentialsEvent.value = Event(true)
                        core.removeListener(this)
                    }
                }
            }
        }

        fun registerListenerCall(listener: ContextCallback){
            CoreContext.CallbackListeners.registerListener(listener)
        }
    }
}