package cc.cans.canscloud.sdk.data.service

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class CallLogCdrService(
    @SerializedName("code")
    var code: Int,

    @SerializedName("success")
    var success: Boolean,

    @SerializedName("message")
    var message: String,

    @SerializedName("data")
    var data: CallLogs,
) {
    @Keep
    data class CallLogs(
        @SerializedName("call_logs")
        var callLogsDetail: ArrayList<CallLogsDetail>?,
    )

    @Keep
    data class CallLogsDetail(
        @SerializedName("call_id")
        var callID: String,

        @SerializedName("cdr_uuid")
        var cdrUUID: String,

        @SerializedName("domain_uuid")
        var domainUUID: String,

        @SerializedName("is_played")
        var isPlayed: Boolean,

        @SerializedName("tags")
        var tags: ArrayList<String>,
    ) : Serializable
}
