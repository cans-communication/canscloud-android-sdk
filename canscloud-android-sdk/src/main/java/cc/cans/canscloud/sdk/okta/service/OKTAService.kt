package cc.cans.canscloud.sdk.okta.service

import cc.cans.canscloud.sdk.okta.models.OKTAClientResponseData
import cc.cans.canscloud.sdk.okta.models.SignInOKTAResponseData
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface OKTAService {
    @POST
    fun fetchOKTAClient(@Url url: String?, @Body body: RequestBody?): Call<OKTAClientResponseData?>?

    @POST
    fun fetchSignInOKTA(@Url url: String?, @Body body: RequestBody?): Call<SignInOKTAResponseData?>?
}