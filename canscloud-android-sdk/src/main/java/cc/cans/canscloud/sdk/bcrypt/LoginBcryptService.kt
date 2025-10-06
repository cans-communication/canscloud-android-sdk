package cc.cans.canscloud.sdk.bcrypt

import cc.cans.canscloud.sdk.bcrypt.models.LoginCpanelRequest
import cc.cans.canscloud.sdk.bcrypt.models.LoginCpanelResponse
import cc.cans.canscloud.sdk.bcrypt.models.LoginSipCredentialsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
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
}