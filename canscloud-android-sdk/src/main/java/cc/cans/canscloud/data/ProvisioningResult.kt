package cc.cans.canscloud.data

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ProvisioningResult {
    @SerializedName("extension")
    @Expose
    var extension: String? = null

    @SerializedName("secret")
    @Expose
    var secret: String? = null

    @SerializedName("domain")
    @Expose
    var domain: String? = null

    @SerializedName("proxy")
    @Expose
    var proxy: String? = null

    @SerializedName("transport")
    @Expose
    var transport: String? = null
}
