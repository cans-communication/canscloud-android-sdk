package cc.cans.canscloud.sdk.okta.repository

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.lifecycle.MutableLiveData
import cc.cans.canscloud.data.AESFactory
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.core.CorePreferences
import cc.cans.canscloud.sdk.okta.models.OKTAClientResponseData
import cc.cans.canscloud.sdk.models.RegisterState
import cc.cans.canscloud.sdk.okta.models.SignIn
import cc.cans.canscloud.sdk.okta.models.SignInOKTAResponseData
import cc.cans.canscloud.sdk.okta.service.OKTAInterceptor
import cc.cans.canscloud.sdk.okta.service.OKTAService
import cc.cans.canscloud.sdk.okta.service.OktaWebAuth
import cc.cans.canscloud.sdk.okta.service.OktaWebAuth.Companion.webAuth
import com.google.gson.Gson
import com.okta.oidc.AuthenticationPayload
import com.okta.oidc.RequestCallback
import com.okta.oidc.util.AuthorizationException
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.linphone.core.Account
import org.linphone.core.AccountCreator
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.ProxyConfig
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.mediastream.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class OKTARepository(
    private val core: Core,
    private val corePreferences: CorePreferences,
    private val listeners: MutableList<CansListenerStub>,
    private val getAccountCreator: () -> AccountCreator
) {
    val username = MutableLiveData<String>()

    private val password = MutableLiveData<String>()

    val domain = MutableLiveData<String>()

    private val displayName = MutableLiveData<String>()

    val transport = MutableLiveData<TransportType>()

    private var proxyConfigToCheck: ProxyConfig? = null

    private val coreListener = object : CoreListenerStub() {
        override fun onRegistrationStateChanged(
            core: Core,
            cfg: ProxyConfig,
            state: RegistrationState,
            message: String
        ) {
            if (cfg == proxyConfigToCheck) {
                org.linphone.core.tools.Log.i("[Assistant] [Generic Login] Registration state is $state: $message")
                if (state == RegistrationState.Ok) {
                    Log.i("OKTARepository - coreListener RegistrationState.Ok")

//                    waitForServerAnswer.value = false
//                    leaveAssistantEvent.value = Event(true)

                    //TODO:Implement
//                    cansCenter().coreContext.context .newAccountConfigured(true)

                    if (cansCenter().core.isEchoCancellerCalibrationRequired) {
                        Log.i("OKTARepository - coreListener isEchoCancellerCalibrationRequired")
                        //TODO:Implement
//                        navigateToEchoCancellerCalibration()
                    } else {
                        Log.i("OKTARepository - coreListener not isEchoCancellerCalibrationRequired go to onboarding")
                        cansCenter().corePreferences.isRegister = true
//                        onboarding()
                    }

                    core.removeListener(this)
                } else if (state == RegistrationState.Failed) {
                    Log.i("OKTARepository - coreListener RegistrationState.Failed")
//                    waitForServerAnswer.value = false
//                    invalidCredentialsEvent.value = Event(true)
                    core.removeListener(this)
                }
            }
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(OKTAInterceptor())
        .addNetworkInterceptor(
            OKTAInterceptor(),
        )
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://sitdkmms.cans.cc")
        .build()

    private val oktaService: OKTAService =
        retrofit.create(OKTAService::class.java)

    fun fetchOKTAClient(
        inputDomain: String,
        context: Context,
        activity: Activity,
        onResult: (Int) -> Unit
    ) {
        Log.i("OKTARepository", "fetchOKTAClient by domain : $inputDomain")

        val bodyJson = JSONObject().apply {
            put("domain_name", inputDomain)
            put("device_os", "android")
            put("language", Locale.getDefault().language)
            put("current_version", "1.5.0")
            put("device_model", Build.MANUFACTURER)
        }

        val requestBody = bodyJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val oktaClientResponseData: retrofit2.Call<OKTAClientResponseData?>? =
            oktaService.fetchOKTAClient("/mobile/login/okta-client", requestBody)

        oktaClientResponseData?.let { oktaClient ->
            oktaClient.enqueue(
                object : retrofit2.Callback<OKTAClientResponseData?> {
                    override fun onResponse(
                        call: retrofit2.Call<OKTAClientResponseData?>,
                        response: retrofit2.Response<OKTAClientResponseData?>,
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let { client ->
                                client.data.let { data ->
                                    if (data.clientId.isNotEmpty() && data.domainName.isNotEmpty() && data.discoveryUrl.isNotEmpty()) {
                                        OktaWebAuth.setupWebAuth(
                                            context,
                                            data.discoveryUrl,
                                            data.clientId
                                        )
                                        OktaWebAuth.setupWebAuthCallback(
                                            activity,
                                            webAuth,
                                            object : OktaWebAuth.Companion.TokenRefreshCallback {
                                                override fun onTokenRefreshed(isAuthenticated: Boolean) {
                                                    Log.i("OKTARepository onTokenRefreshed")
                                                    val token =
                                                        corePreferences.loginInfo.tokenOkta ?: ""
//                                                val domainOKTA = loginIngo?.domainOKTACurrent ?: ""
                                                    val domainOKTA = data.domainName
                                                    fetchSignInOKTA(
                                                        token,
                                                        domainOKTA
                                                    ) { response ->
                                                        if (response != null) {
                                                            if (response.data != null) {
                                                                decryptLogin(response.data)
                                                            } else {
                                                                when (response.code) {
                                                                    301 -> {
                                                                        corePreferences.isSignInOKTANotConnected =
                                                                            true
                                                                        listeners.forEach {
                                                                            it.onRegistration(
                                                                                RegisterState.FAIL
                                                                            )
                                                                        }
                                                                    }

                                                                    400 -> {
                                                                        corePreferences.isSignInOKTANotConnected =
                                                                            true
                                                                        listeners.forEach {
                                                                            it.onRegistration(
                                                                                RegisterState.FAIL
                                                                            )
                                                                        }
                                                                    }

                                                                    else -> {
                                                                        listeners.forEach {
                                                                            it.onRegistration(
                                                                                RegisterState.FAIL
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            onResult(response.code)
                                                        } else {
                                                            onResult(-1)
                                                            listeners.forEach {
                                                                it.onRegistration(
                                                                    RegisterState.FAIL
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                        val payload = AuthenticationPayload.Builder().build()
                                        webAuth.signIn(activity, payload)
                                    } else {
                                        onResult(response.code())
                                        listeners.forEach { it.onRegistration(RegisterState.FAIL) }
                                    }
                                }
                            }
                        } else {
                            onResult(response.code())
                            listeners.forEach { it.onRegistration(RegisterState.FAIL) }
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<OKTAClientResponseData?>,
                        t: Throwable,
                    ) {
                        onResult(-1)
                        listeners.forEach { it.onRegistration(RegisterState.FAIL) }
                        android.util.Log.e("Response fail", "${t.message}")
                    }
                },
            )
        }
    }

    fun fetchSignInOKTA(
        token: String,
        domain: String,
        callback: (SignInOKTAResponseData?) -> Unit
    ) {
        Log.i("OKTARepository ", "fetchSignInOKTA by token : $token , domain : $domain")

        val bodyJson = JSONObject().apply {
            put("device_os", "android")
            put("language", Locale.getDefault().language)
            put("current_version", "1.5.0")
//            put("current_version", coreContext.appVersion.substringBefore("("))
            put("device_model", Build.MANUFACTURER)
            put("domain_name", domain)
            put("access_token", token)
        }

        val requestBody = bodyJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        Log.i("OKTARepository", "fetchSignInOKTA by requestBody : $requestBody")

        val signInOKTAResponseData: retrofit2.Call<SignInOKTAResponseData?>? =
            oktaService.fetchSignInOKTA("/mobile/login/login-okta", requestBody)


        Log.i("OKTARepository", "fetchSignInOKTA signInOKTAResponseData : $signInOKTAResponseData")
        signInOKTAResponseData?.let { signInOKTA ->
            signInOKTA.enqueue(
            object : retrofit2.Callback<SignInOKTAResponseData?> {
                override fun onResponse(
                    call: retrofit2.Call<SignInOKTAResponseData?>,
                    response: retrofit2.Response<SignInOKTAResponseData?>,
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { res ->
                            when (res.code) {
                                200, 301, 400 -> if (res.success) {
                                    callback(res)
                                } else {
                                    listeners.forEach { it.onRegistration(RegisterState.FAIL) }
                                }

                                else -> listeners.forEach { it.onRegistration(RegisterState.FAIL) }
                            }
                        }
                    } else {
                        listeners.forEach { it.onRegistration(RegisterState.FAIL) }
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<SignInOKTAResponseData?>,
                    t: Throwable,
                ) {
                    listeners.forEach { it.onRegistration(RegisterState.FAIL) }
                    android.util.Log.e("Response fail", "${t.message}")
                }
            },
        )
        }
    }

    private fun decryptLogin(dataLogin: SignInOKTAResponseData.Data) {
        val jsonString = AESFactory.decrypt(dataLogin.user_credentials)
        if (jsonString != null) {
            val dataSignIn = Gson().fromJson(jsonString, SignIn::class.java)
            val loginInfo = corePreferences.loginInfo
            val newLoginInfo = loginInfo.copy(tokenSignIn = dataLogin.access_token)
            corePreferences.loginInfo = newLoginInfo

            decodeBase64(dataSignIn.sip_password) { passwordDecode ->
                if (!passwordDecode.isNullOrEmpty()) {
                    password.value = passwordDecode.toString()
                    username.value = dataSignIn.sip_username
                    domain.value = "${dataLogin.domain_name}:8446"
                    transport.value = TransportType.Tcp

                    org.linphone.core.tools.Log.i(
                        "token: ",
                        "${dataSignIn.sip_password} ${dataSignIn.sip_username}"
                    )

                    val accountList = core.accountList

                    accountList.forEach {
                        deleteAccount(it)
                    }
                    createProxyConfig()
                }
            }
        } else {
            listeners.forEach { it.onRegistration(RegisterState.FAIL) }
        }
    }

    private fun decodeBase64(encodedString: String, callback: (String?) -> Unit) {
        try {
            val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
            callback(String(decodedBytes, Charsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            listeners.forEach { it.onRegistration(RegisterState.FAIL) }
        }
    }

    private fun deleteAccount(account: Account) {
        val authInfo = account.findAuthInfo()
        if (authInfo != null) {
            org.linphone.core.tools.Log.i("[Account Settings] Found auth info $authInfo, removing it.")
            core.removeAuthInfo(authInfo)
        } else {
            org.linphone.core.tools.Log.w("[Account Settings] Couldn't find matching auth info...")
        }

        core.removeAccount(account)
    }

    private fun createProxyConfig() {
//        CansAnalytics.loginSip()
        core.addListener(coreListener)

        var accountCreator = getAccountCreator()

        accountCreator.username = username.value
        accountCreator.password = password.value
        accountCreator.domain = domain.value
        accountCreator.displayName = displayName.value
        accountCreator.transport = transport.value

        val proxyConfig: ProxyConfig? = accountCreator.createProxyConfig()
        proxyConfigToCheck = proxyConfig

        if (proxyConfig == null) {
            Log.i("OKTARepository decodeBase64 token : proxyConfig == null Account creator couldn't create proxy config")

            org.linphone.core.tools.Log.e("[Assistant] [Generic Login] Account creator couldn't create proxy config")
            core.removeListener(coreListener)

            listeners.forEach { it.onRegistration(RegisterState.FAIL) }
            return
        }

        if (domain.value.orEmpty() != cansCenter().corePreferences.defaultDomain) {
            org.linphone.core.tools.Log.i("[Assistant] [Generic Login] Background mode with foreground service automatically enabled")
            cansCenter().corePreferences.keepServiceAlive = true
            cansCenter().coreContext.notificationsManager.startForeground()
        }
    }

    fun signOutOKTA(activity: Activity, onResult: (Int) -> Unit) {
        webAuth.signOut(activity, object :
            RequestCallback<Int, AuthorizationException> {
            override fun onSuccess(result: Int) {
                android.util.Log.d("OktaWebAuth", "Successfully signed out")

                val loginInfo = corePreferences.loginInfo
                val newLoginInfo = loginInfo.copy(
                    logInType = "",
                    tokenSignIn = "",
                    tokenOkta = ""
                )
                corePreferences.loginInfo = newLoginInfo
                corePreferences.isSignInOKTANotConnected = false
                onResult(200)
            }

            override fun onError(error: String?, exception: AuthorizationException?) {
                onResult(-1)
                android.util.Log.e("OktaWebAuth", "Sign out failed: $error", exception)
            }
        })
    }
}