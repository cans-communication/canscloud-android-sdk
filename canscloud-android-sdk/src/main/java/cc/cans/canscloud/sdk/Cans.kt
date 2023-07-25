package cc.cans.canscloud.sdk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import cc.cans.canscloud.sdk.CansCloudApplication.Companion.coreContextCansBase
import cc.cans.canscloud.sdk.CansCloudApplication.Companion.corePreferences
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

data class UserService(
    val username: String,
    val password: String,
    val domain: String,
    val port: String,
    val transport: String
)

class Cans {
    companion object {
        private lateinit var core: Core
        private var proxyConfigToCheck: ProxyConfig? = null
        private lateinit var accountCreator: AccountCreator
        private var useGenericSipAccount: Boolean = false
        var packageManager : PackageManager? = null
        var packageName : String = ""

        fun config(context: Context, packageManager: PackageManager, packageName: String, companyKey: String) {
            CansCloudApplication.ensureCoreExists(context)
            Companion.packageManager = packageManager
            Companion.packageName = packageName
            print("companyKey: $companyKey")
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

        fun register(activity: Activity) {
            val fileName = "config.json"
            val jsonString = loadJSONFromAsset(context = activity.applicationContext, fileName)

            jsonString?.let {
                val gson = Gson()
                val user = gson.fromJson(it, UserService::class.java)

                val domain = "${user.domain}:${user.port}"

                var accountCreator = getAccountCreator(true)
                coreContextCansBase.core.addListener(coreListener)

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
                    coreContextCansBase.core.removeListener(coreListener)
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
                    coreContextCansBase.notificationsManager.startForeground()
                }

                if (packageManager?.checkPermission(Manifest.permission.RECORD_AUDIO, packageName) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
                    return
                }
            }
        }

        fun username(): String {
            val defaultAccount =
                coreContextCansBase.core.defaultAccount?.params?.identityAddress?.username.toString()
            val domain =
                coreContextCansBase.core.defaultAccount?.params?.identityAddress?.domain.toString()

            return "$defaultAccount $domain"
        }

        private fun getAccountCreator(genericAccountCreator: Boolean = false): AccountCreator {

            coreContextCansBase.core.loadConfigFromXml(corePreferences.linphoneDefaultValuesPath)
            accountCreator =
                coreContextCansBase.core.createAccountCreator(corePreferences.xmlRpcServerUrl)
            accountCreator.language = Locale.getDefault().language

            if (genericAccountCreator != useGenericSipAccount) {
                accountCreator.reset()
                accountCreator.language = Locale.getDefault().language

                if (genericAccountCreator) {
                    Log.i("[Assistant] Loading default values")
                    coreContextCansBase.core.loadConfigFromXml(corePreferences.defaultValuesPath)
                } else {
                    Log.i("[Assistant] Loading linphone default values")
                    coreContextCansBase.core.loadConfigFromXml(corePreferences.linphoneDefaultValuesPath)
                }
                useGenericSipAccount = genericAccountCreator
            }
            return accountCreator
        }


        fun getCountCalls(): Int {
            val call = coreContextCansBase.core.callsNb
            Log.i("[Application] getCountCalls : $call")
            return call
        }

        fun startCall(addressToCall: String) {

            android.util.Log.d("MainActivity : ", "startCall")
            //coreContextCansBase.outgoingCall()
            val addressToCall = addressToCall
            if (addressToCall.isNotEmpty()) {
                coreContextCansBase.startCall(addressToCall)
                //   eraseAll()
            } else {
                //setLastOutgoingCallAddress()
            }
        }


        fun terminateCall() {
            if (core.callsNb == 0) return

            // If the call state isn't paused, we can get it using core.currentCall
            val call = if (core.currentCall != null) core.currentCall else core.calls[0]
            call ?: return

            // Terminating a call is quite simple
            call.terminate()
        }

        private fun setLastOutgoingCallAddress() {
            val callLog = coreContextCansBase.core.lastOutgoingCallLog
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
    }
}