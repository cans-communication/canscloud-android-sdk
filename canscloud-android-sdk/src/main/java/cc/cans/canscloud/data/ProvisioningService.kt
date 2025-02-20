package cc.cans.canscloud.data

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ProvisioningService {
    @POST
    fun getProvisioningData(@Url url: String?, @Body body: RequestBody?): Call<ProvisioningData?>?
}
