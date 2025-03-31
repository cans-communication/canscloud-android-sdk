package cc.cans.canscloud.sdk.data.repository

import android.content.Context
import android.os.Build
import cc.cans.canscloud.sdk.CansCenter
import cc.cans.canscloud.sdk.data.service.CallLogCdrService
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Credentials
import java.util.ArrayList
import java.util.Locale

object CallLogRepository {

    fun fetchCallLog(
        context: Context,
        callId: ArrayList<String>,
        callback: (CallLogCdrService.CallLogs?) -> Unit,
    ) {
        val body = JsonObject()
        body.addProperty("extension", CansCenter().username)
        body.addProperty("domain", CansCenter().domain)
        body.addProperty("device_os", "android")
        body.addProperty("language", Locale.getDefault().language)
        body.addProperty("current_version", "1.8.0")
        body.addProperty("device_model", Build.MANUFACTURER)

        val callIds = Gson().toJsonTree(callId)
        body.add("call_ids", callIds)

        RetrofitClient.credential = Credentials.basic(username = "forceupdate", password = "3v^jHw6NrM2kn*gqfP9KLcY@")

        CansServiceBaseRepository.fetch(
            context,
            "/mobile/v2/call-logs",
            body,
            CallLogCdrService::class.java,
            onCompletionSuccess = { response ->
                if (response.code == 200 && response.success) {
                    callback(response.data)
                } else {
                    callback(null)
                }
            },
            onCompletionFailure = {
                callback(null)
            },
        )
    }
}
