package cc.cans.canscloud.sdk.bcrypt

import cc.cans.canscloud.sdk.bcrypt.models.LoginCpanelRequest
import cc.cans.canscloud.sdk.bcrypt.models.LoginCpanelResponse
import cc.cans.canscloud.sdk.bcrypt.models.LoginSipCredentialsResponse
import cc.cans.canscloud.sdk.bcrypt.models.LoginV3Request
import cc.cans.canscloud.sdk.bcrypt.models.LoginV3Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Url

interface LoginBcryptService {
    @POST
    suspend fun loginCpanel(
        @Url url: String,
        @Body request: LoginCpanelRequest
    ): retrofit2.Response<LoginCpanelResponse>

    @GET
    suspend fun getSipCredentials(
        @Url url: String?,
        @Header("Authorization") bearer: String
    ): retrofit2.Response<LoginSipCredentialsResponse>

    @POST
    suspend fun loginCANSCloudV3(
        @Url url: String,
        @Body request: LoginV3Request
    ): retrofit2.Response<LoginV3Response>

    @PATCH
    suspend fun forceSetPassword(
        @Url url: String,
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String>
    ): retrofit2.Response<com.google.gson.JsonObject>
}