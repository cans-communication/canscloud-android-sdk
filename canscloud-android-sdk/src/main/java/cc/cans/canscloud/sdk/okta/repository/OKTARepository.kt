package cc.cans.canscloud.sdk.okta.repository

import android.app.Activity
import android.content.Context
import android.os.Build
import cc.cans.canscloud.sdk.BuildConfig
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.okta.models.OKTAClientResponseData
import cc.cans.canscloud.sdk.okta.models.SignInOKTAResponseData
import cc.cans.canscloud.sdk.okta.service.OKTAInterceptor
import cc.cans.canscloud.sdk.okta.service.OKTAService
import cc.cans.canscloud.sdk.okta.service.OktaWebAuth.Companion.webAuth
import com.okta.oidc.RequestCallback
import com.okta.oidc.util.AuthorizationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

object OKTARepository {
    //    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
//        .addInterceptor(OKTAInterceptor(BuildConfig.OKTA_API_USER, BuildConfig.OKTA_API_PASSWORD))
//        .addNetworkInterceptor(
//            OKTAInterceptor(BuildConfig.OKTA_API_USER, BuildConfig.OKTA_API_PASSWORD),
//        )
//        .build()
//
//    private val retrofit: Retrofit = Retrofit.Builder()
//        .client(okHttpClient)
//        .addConverterFactory(GsonConverterFactory.create())
//        .baseUrl(BuildConfig.OKTA_API_URL)
//        .build()
//
//    private val oktaService: OKTAService =
//        retrofit.create(OKTAService::class.java)
//

    private fun createOKTAService(apiURL: String):OKTAService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                OKTAInterceptor(
                    BuildConfig.OKTA_API_USER,
                    BuildConfig.OKTA_API_PASSWORD
                )
            )
            .addNetworkInterceptor(
                OKTAInterceptor(BuildConfig.OKTA_API_USER, BuildConfig.OKTA_API_PASSWORD),
            )
            .build()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(apiURL)
            .build()


        return retrofit.create(OKTAService::class.java)
    }

    fun fetchOKTAClient(
        apiURL: String,
        inputDomain: String,
        context: Context,
        callback: (OKTAClientResponseData.Data?) -> Unit,
    ) {
        val oktaService = createOKTAService(apiURL)

        val bodyJson = JSONObject().apply {
            put("domain_name", inputDomain)
            put("device_os", "android")
            put("language", Locale.getDefault().language)
            put("current_version", getAppVersion(context))
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
                            response.body()?.let { result ->
                                if (result.code == 200) {
                                    if (result.data.domainName == inputDomain) {
                                        callback(result.data)
                                    }
                                } else {
                                    callback(null)
                                }
                            }
                        } else {
                            callback(null)
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<OKTAClientResponseData?>,
                        t: Throwable,
                    ) {
                        callback(null)
                    }
                },
            )
        }
    }

    fun fetchSignInOKTA(
        context: Context,
        apiURL: String,
        token: String,
        domain: String,
        callback: (SignInOKTAResponseData?) -> Unit
    ) {
        val oktaService = createOKTAService(apiURL)

        val bodyJson = JSONObject().apply {
            put("device_os", "android")
            put("language", Locale.getDefault().language)
            put("current_version", getAppVersion(context))
            put("device_model", Build.MANUFACTURER)
            put("domain_name", domain)
            put("access_token", token)
        }

        val requestBody = bodyJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val signInOKTAResponseData: retrofit2.Call<SignInOKTAResponseData?>? =
            oktaService.fetchSignInOKTA("/mobile/login/login-okta", requestBody)


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
                                        callback(null)
                                    }

                                    else -> callback(null)
                                }
                            }
                        } else {
                            callback(null)
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<SignInOKTAResponseData?>,
                        t: Throwable,
                    ) {
                        callback(null)
                    }
                },
            )
        }
    }

    fun signOutOKTA(activity: Activity, callback: (Boolean) -> Unit) {
        webAuth.signOut(activity, object :
            RequestCallback<Int, AuthorizationException> {
            override fun onSuccess(result: Int) {
                val loginInfo = cansCenter().corePreferences.loginInfo
                val newLoginInfo = loginInfo.copy(
                    logInType = "",
                    tokenSignIn = "",
                    tokenOkta = ""
                )
                cansCenter().corePreferences.loginInfo = newLoginInfo
                cansCenter().corePreferences.isSignInOKTANotConnected = false
                callback(true)
            }

            override fun onError(error: String?, exception: AuthorizationException?) {
                callback(false)
            }
        })
    }

    private fun getAppVersion(context: Context): Pair<String, Long> {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: ""
        val versionCode =
            packageInfo.longVersionCode
        return versionName to versionCode
    }
}