package cc.cans.canscloud.sdk.bcrypt

import android.util.Log
import cc.cans.canscloud.sdk.bcrypt.models.LoginCpanelRequest
import cc.cans.canscloud.sdk.bcrypt.models.LoginSipCredentialsResponse
import cc.cans.canscloud.sdk.bcrypt.models.LoginV3Request
import cc.cans.canscloud.sdk.bcrypt.models.LoginV3Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LoginBcryptManager(url: String) {

    private val BASE_URL = url
    private val CPANEL_PREFIX = "api/resolver-api-v1/cpanel-api"

    private val acceptHeaderInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.9,th-TH;q=0.8,th;q=0.7")
            .header("Cache-Control", "no-cache")
            .build()
        chain.proceed(req)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(acceptHeaderInterceptor)
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    private val api = retrofit.create(LoginBcryptService::class.java)

    suspend fun getLoginAccessToken(username: String, password: String): String {

        val url = "${BASE_URL}${CPANEL_PREFIX}/login/cpanel/"

        Log.d("FIX_BUG","getLoginAccessToken url : $url")

        val request =
            LoginCpanelRequest(username = username, password = password, loginType = "account")

        val resp = api.loginCpanel(url, request)
        if (!resp.isSuccessful) throw IllegalStateException("login failed: ${resp.code()} ${resp.message()}")

        val body = resp.body() ?: throw IllegalStateException("empty body")
        val token = body.data?.accessToken
        return token ?: throw IllegalStateException("no access token")
    }

    suspend fun getLoginAccount(
        accessToken: String,
        domainUuid: String,
        isNewLoginVersion: Boolean
    ): retrofit2.Response<LoginSipCredentialsResponse> = withContext(Dispatchers.IO) {

        val url = if (isNewLoginVersion) {
            "${BASE_URL}api/v3/$domainUuid/sip-credentials"
        } else {
            "${BASE_URL}${CPANEL_PREFIX}/api/v3/domains/$domainUuid/sip-credentials"
        }

        Log.d("FIX_BUG","getLoginAccount url : $url")

        val bearer = "Bearer $accessToken"

        val resp = api.getSipCredentials(url, bearer)

        resp
    }

    suspend fun getLoginAccountV3(username: String, password: String): LoginV3Response {
        val url = "${BASE_URL}api/v3/sign-in/cc"

        Log.d("FIX_BUG","getLoginAccountV3 url : $url")

        val request = LoginV3Request(username = username, password = password)

        val resp = api.loginCANSCloudV3(url, request)

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string() ?: "Unknown Error"
            throw IllegalStateException("getLoginAccountV3 login failed: ${resp.code()} | ServerMsg: $errorBody")
        }

        return resp.body() ?: throw IllegalStateException("empty body")
    }

    suspend fun forceSetPassword(
        domainUuid: String,
        userId: String,
        token: String,
        newPassword: String
    ): Boolean {
        val url = "${BASE_URL}api/v3/$domainUuid/user/$userId/password-set"
        val bearer = "Bearer $token"
        val body = mapOf("password" to newPassword)

        val resp = api.forceSetPassword(url, bearer, body)

        if (!resp.isSuccessful) {
            throw IllegalStateException("Set password failed: ${resp.code()} ${resp.message()}")
        }
        return true
    }
}