package cc.cans.canscloud.sdk.data.retrofit

import cc.cans.canscloud.sdk.data.service.CallLogCdrService
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiInterface {
    @POST("{path}")
    fun getService(@Path("path") path: String, @Body body: JsonObject): Call<ResponseBody>

    @PUT("{path}")
    fun updateService(@Path("path") path: String, @Body body: JsonObject): Call<ResponseBody>

    @Headers("mock:true")
    @GET("https://{path}/call-logs")
    fun getCallLogCdr(@Path("path") path: String): Call<CallLogCdrService>

}
