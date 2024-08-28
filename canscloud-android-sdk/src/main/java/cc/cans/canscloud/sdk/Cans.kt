package cc.cans.canscloud.sdk

import UserService
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.RadioGroup
import cc.cans.canscloud.sdk.callback.ContextCallback
import cc.cans.canscloud.sdk.core.CorePreferences
import com.google.gson.Gson
import org.linphone.core.Account
import org.linphone.core.AccountCreator
import org.linphone.core.Address
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.LogLevel
import org.linphone.core.MediaEncryption
import org.linphone.core.ProxyConfig
import org.linphone.core.R
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.core.tools.Log
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Locale

class Cans {

    companion object {
        lateinit var core: Core
        lateinit var corePreferences: CorePreferences
        var packageManager : PackageManager? = null
        var packageName : String = ""
        val listeners = ArrayList<ContextCallback>()

        private val coreListener = object : CoreListenerStub() {
            override fun onRegistrationStateChanged(
                core: Core,
                cfg: ProxyConfig,
                state: RegistrationState,
                message: String
            ) {
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

            override fun onCallStateChanged(
                core: Core,
                call: Call,
                state: Call.State?,
                message: String
            ) {
                // This function will be called each time a call state changes,
                // which includes new incoming/outgoing calls

                when (state) {
                    Call.State.OutgoingInit -> {
                        // First state an outgoing call will go through
                    }
                    Call.State.OutgoingProgress -> {
                        // Right after outgoing init
                    }
                    Call.State.OutgoingRinging -> {
                        // This state will be reached upon reception of the 180 RINGING
                    }
                    Call.State.Connected -> {
                        listeners.forEach { it.onConnectedCall() }
                    }
                    Call.State.StreamsRunning -> {
                        // This state indicates the call is active.
                        // You may reach this state multiple times, for example after a pause/resume
                        // or after the ICE negotiation completes
                        // Wait for the call to be connected before allowing a call update
                    }
                    Call.State.Paused -> {
                        // When you put a call in pause, it will became Paused
                    }
                    Call.State.PausedByRemote -> {
                        // When the remote end of the call pauses it, it will be PausedByRemote
                    }
                    Call.State.Updating -> {
                        // When we request a call update, for example when toggling video
                    }
                    Call.State.UpdatedByRemote -> {
                        // When the remote requests a call update
                    }
                    Call.State.Released -> {
                        // Call state will be released shortly after the End state
                    }
                    Call.State.Error -> {

                    }

                    else -> {}
                }
            }
        }

        fun registerListenerCall(listener: ContextCallback){
            registerListener(listener)
        }

        private fun registerListener(listener: ContextCallback) {
            listeners.add(listener)
        }

        private fun unRegisterListener(listener: ContextCallback) {
            listeners.remove(listener)
        }

        fun config(
            activity: Activity,
            packageManager: PackageManager,
            packageName: String,
            companyKey: String,
            callback: () -> Unit
        ) {
            Companion.packageManager = packageManager
            Companion.packageName = packageName

            corePreferences = CorePreferences(activity)
            corePreferences.copyAssetsFromPackage()

            val config = Factory.instance().createConfigWithFactory(corePreferences.configPath, corePreferences.factoryConfigPath)
            corePreferences.config = config
            core = Factory.instance().createCoreWithConfig(config, activity)
            core.start()

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

        fun register(activity: Activity) {
            val fileName = "json/get_user.json"
            val jsonString = loadJSONFromAsset(context = activity.applicationContext, fileName)

            jsonString?.let {
                val gson = Gson()
                val user = gson.fromJson(it, UserService::class.java)
                val account = core.defaultAccount?.params
                if (username().isEmpty() || (user.username != account?.identityAddress?.username) || (user.domain != account.identityAddress?.domain)) {
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

                    val account = core.createAccount(params)
                    core.addAuthInfo(authInfo)
                    core.addAccount(account)

                    // Asks the CaptureTextureView to resize to match the captured video's size ratio
                    //core.config.setBool("video", "auto_resize_preview_to_keep_ratio", true)

                    core.defaultAccount = account
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

        fun registerByUser(activity: Activity, username: String , password: String, domain: String, port: String, transport: String ) {

            val account = core.defaultAccount?.params
            if ((username != account?.identityAddress?.username) || (domain != account.identityAddress?.domain)) {
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

                val account = core.createAccount(params)
                core.addAuthInfo(authInfo)
                core.addAccount(account)

                // Asks the CaptureTextureView to resize to match the captured video's size ratio
                //core.config.setBool("video", "auto_resize_preview_to_keep_ratio", true)

                core.defaultAccount = account
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

        fun username(): String {
            core.defaultAccount?.params?.identityAddress?.let {
                return "${it.username}@${it.domain}:${it.port}"
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
            remoteAddress ?: return // If address parsing fails, we can't continue with outgoing call process

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

        fun durationTime() : Int? {
            val durationTime = core.currentCall?.duration
            return durationTime
        }
    }
}