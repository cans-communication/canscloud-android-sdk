package cc.cans.canscloud.data

import cc.cans.canscloud.data.ProvisioningResult
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ProvisioningData {
    @SerializedName("results")
    @Expose
    var results: List<ProvisioningResult> = ArrayList<ProvisioningResult>()

    @SerializedName("message")
    @Expose
    var message: String? = null
}
