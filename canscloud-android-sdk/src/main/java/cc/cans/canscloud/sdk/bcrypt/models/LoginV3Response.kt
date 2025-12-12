package cc.cans.canscloud.sdk.bcrypt.models

import com.google.gson.annotations.SerializedName

data class LoginV3Response(
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: LoginV3Data?,
    @SerializedName("message") val message: String?,
    @SerializedName("success") val success: Boolean
)

data class LoginV3Data(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: LoginV3User
)

data class LoginV3User(
    @SerializedName("user_id") val id: String,
    @SerializedName("domain_id") val domainId: String,
    @SerializedName("profile_image_url") val profileImageUrl: String?,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("extension_id") val extensionId: String?,
    @SerializedName("extension") val extensionNumber: String?,
    @SerializedName("last_login_date") val lastLoginDate: String?,
    @SerializedName("created_date") val createdDate: String?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_by_display_name") val createdByDisplayName: String?,
    @SerializedName("created_by_profile_image_url") val createdByProfileImageUrl: String?,
    @SerializedName("updated_date") val updatedDate: String?,
    @SerializedName("updated_by") val updatedBy: String?,
    @SerializedName("updated_by_display_name") val updatedByDisplayName: String?,
    @SerializedName("updated_by_profile_image_url") val updatedByProfileImageUrl: String?,
    @SerializedName("password_reset_required") val passwordResetRequired: Boolean,
    @SerializedName("is_active") val isActive: Boolean
)
