package cc.cans.canscloud.sdk.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Credentials
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object CansServiceBaseRepository {

    fun <T> fetch(
        context: Context,
        urlService: String,
        request: JsonObject,
        responseModel: Class<T>,
        onCompletionSuccess: (T) -> Unit,
        onCompletionFailure: () -> Unit,
    ) {

        RetrofitClientCenter.credential =
            Credentials.basic(username = "forceupdate", password = "3v^jHw6NrM2kn*gqfP9KLcY@")

        RetrofitClientCenter.apiInterface.getService(urlService, request)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>,
                ) {
                    if (response.body() != null) {
                        response.body()?.let {
                            val result = Gson().fromJson(it.string(), responseModel)
                            onCompletionSuccess(result)
                        }
                    } else {
                        onCompletionFailure()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    onCompletionFailure()
                }
            })
    }


    fun <T> fetchUpdate(
        context: Context,
        urlService: String,
        request: JsonObject,
        responseModel: Class<T>,
        onCompletionSuccess: (T) -> Unit,
        onCompletionFailure: () -> Unit,
    ) {

        RetrofitClientCenter.credential =
            Credentials.basic(username = "forceupdate", password = "3v^jHw6NrM2kn*gqfP9KLcY@")

        RetrofitClientCenter.apiInterface.updateService(urlService, request)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>,
                ) {
                    if (response.body() != null) {
                        response.body()?.let {
                            val result = Gson().fromJson(it.string(), responseModel)
                            onCompletionSuccess(result)
                        }
                    } else {
                        onCompletionFailure()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    onCompletionFailure()
                }
            })
    }
}
