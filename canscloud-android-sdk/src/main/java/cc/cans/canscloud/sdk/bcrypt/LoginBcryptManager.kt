package cc.cans.canscloud.sdk.bcrypt

import android.util.Log
import cc.cans.canscloud.sdk.bcrypt.models.LoginCpanelRequest
import cc.cans.canscloud.sdk.bcrypt.models.LoginSipCredentialsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
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
    ): LoginSipCredentialsResponse = withContext(Dispatchers.IO) {

        val url = "${BASE_URL}${CPANEL_PREFIX}/api/v3/domains/$domainUuid/sip-credentials"

        val bearer = "Bearer $accessToken"

        val resp = api.getSipCredentials(url, bearer)

        if (!resp.isSuccessful) {
            throw HttpException(resp)
        }

        val body = resp.body() ?: throw IllegalStateException("Empty body for SIP credentials")

        body
    }
}