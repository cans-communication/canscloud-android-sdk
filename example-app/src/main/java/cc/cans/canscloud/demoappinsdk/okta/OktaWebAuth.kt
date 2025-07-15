package cc.cans.canscloud.demoappinsdk.okta

import android.app.Activity
import android.content.Context
import cc.cans.canscloud.demoappinsdk.model.LoginInfo
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.utils.CansUtils
import com.google.gson.Gson
import com.okta.oidc.AuthorizationStatus
import com.okta.oidc.OIDCConfig
import com.okta.oidc.Okta
import com.okta.oidc.RequestCallback
import com.okta.oidc.ResultCallback
import com.okta.oidc.clients.web.WebAuthClient
import com.okta.oidc.net.response.IntrospectInfo
import com.okta.oidc.storage.SharedPreferenceStorage
import com.okta.oidc.util.AuthorizationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.mediastream.Log
import java.net.URI

class OktaWebAuth {
    companion object {
        var TAG = "OktaWebAuth: "
        lateinit var webAuth: WebAuthClient
        var pageCurrent = ""
        private var isCheckedSession: Boolean = false
//        var corePreferences = cansCenter().corePreferences.dis

        interface TokenRefreshCallback {
            fun onTokenRefreshed(isAuthenticated: Boolean)
        }

        private fun getBaseUrl(url: String): String {
            val uri = URI(url)
            return "${uri.scheme}://${uri.host}"
        }

        fun setupWebAuth(context: Context, discoveryURL: String, clientID: String) {
            Log.i("OKTA","discoveryURL : $discoveryURL , clientID : $clientID")
            val discoveryUri = getBaseUrl(discoveryURL)
            Log.i("OKTA","discoveryUri : $discoveryUri")
            val oidcConfig = OIDCConfig.Builder()
                .clientId(clientID)
//                .redirectUri(BuildConfig.SIGN_IN_REDIRECT_URI)
//                .endSessionRedirectUri(BuildConfig.SIGN_OUT_REDIRECT_URI)
                .redirectUri("cc.cans.canscloud:/callback")
                .endSessionRedirectUri("cc.cans.canscloud:/logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri(discoveryUri)
                .create()

            Log.i("OKTA","oidcConfig : $oidcConfig")
            webAuth = Okta.WebAuthBuilder()
                .withConfig(oidcConfig)
                .withContext(context)
                .withStorage(SharedPreferenceStorage(context))
                .setRequireHardwareBackedKeyStore(false)
                .create()

            Log.i("OKTA","webAuth : $webAuth")
        }

        fun getLoginInfo(): LoginInfo? {
            val config = cansCenter().core.config
            val json = config.getString("app", "login_info", "")

            val loginInfo = if (json.isNullOrEmpty()) {
                LoginInfo("", "", "", "")
            } else {
                try {
                    Gson().fromJson(json, LoginInfo::class.java)
                } catch (e: Exception) {
                    LoginInfo("", "", "", "")
                }
            }

            return loginInfo
        }

        fun setupWebAuthCallback(
            requireActivity: Activity,
            webAuth: WebAuthClient,
            callback: TokenRefreshCallback
        ) {
            val callbackAuth = object : ResultCallback<AuthorizationStatus, AuthorizationException> {
                override fun onSuccess(status: AuthorizationStatus) {
                    if (status == AuthorizationStatus.AUTHORIZED) {
                        val accessToken = webAuth.sessionClient.tokens?.accessToken.orEmpty()

                        // Read & copy your LoginInfo
                        val current = getLoginInfo()
                        if (current != null) {
                            val updated = current.copy(tokenOkta = accessToken)
                            val json = Gson().toJson(updated)
                            cansCenter().core.config.setString("app", "login_info", json)
                        }

                        Log.d(TAG, "AUTHORIZED: Access Token = $accessToken")
                        callback.onTokenRefreshed(true)
                    } else {
                        // Handle signed-out or any other non-authorized status
                        Log.d(TAG, "SIGNED_OUT or OTHER")
                        callback.onTokenRefreshed(false)
                    }
                }

                override fun onCancel() {
                    Log.d(TAG, "CANCELED")
                    callback.onTokenRefreshed(false)
                }

                override fun onError(msg: String?, error: AuthorizationException?) {
                    Log.d(TAG, "onError: $msg", error)
                    callback.onTokenRefreshed(false)
                }
            }

            // Now register the callback—this is outside the object literal
            webAuth.registerCallback(callbackAuth, requireActivity)
        }


        fun checkSession(page: String, activity: Activity, callback: (Boolean) -> Unit) {
            if (pageCurrent == page || isCheckedSession) return
            if (webAuth.isInProgress) return

            pageCurrent = page
            isCheckedSession = true
            val loginInfo = getLoginInfo()

            try {
                webAuth.sessionClient.introspectToken(loginInfo?.tokenOkta, "access_token",
                    object : RequestCallback<IntrospectInfo, AuthorizationException> {
                        override fun onSuccess(introspectInfo: IntrospectInfo) {
                            if (introspectInfo.isActive) {
                                println("Token is active. User ID: ${introspectInfo.sub}")
                                callback(false)
                                isCheckedSession = false
                            } else {
                                println("Token is inactive or expired.")
                                signOut(activity) {
                                    callback(it)
                                }
                                isCheckedSession = false
                            }
                        }

                        override fun onError(
                            error: String?,
                            exception: AuthorizationException?
                        ) {
                            println("Token introspection failed: ${error}")
                            callback(false)
                            isCheckedSession = false
                        }
                    }
                )
            } catch (e: Exception) {
                callback(false)
                isCheckedSession = false
            }
        }


        private fun signOut(activity: Activity, callback: (Boolean) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {

                cansCenter().core.defaultAccount?.let {
                    val authInfo = it.findAuthInfo()
                    if (authInfo != null) {
                        Log.i("[Account Settings] Found auth info $authInfo, removing it.")
                        cansCenter().core.removeAuthInfo(authInfo)
                    } else {
                        Log.w("[Account Settings] Couldn't find matching auth info...")
                    }

                    cansCenter().core.removeAccount(it)
                }

                // Sign out the user on error (execute on main thread)
                CansUtils.isSignOutLoading = true
                webAuth.signOut(activity, object : RequestCallback<Int, AuthorizationException> {
                    override fun onSuccess(result: Int) {
                        Log.d("OktaAuth1", "Successfully signed out")

                        val current = getLoginInfo()
                        if (current != null) {
                            val updated = current.copy(logInType = "", tokenSignIn = "", tokenOkta = "")
                            val json = Gson().toJson(updated)
                            cansCenter().core.config.setString("app", "login_info", json)
                        }

//                        corePreferences.loginInfo = corePreferences.loginInfo?.copy(logInType = "", tokenSignIn = "", tokenOkta = "")
                        CansUtils.isSignOutLoading = false

                        // Call the callback after sign-out
                        callback(true)
                        isCheckedSession = false
                    }

                    override fun onError(error: String?, exception: AuthorizationException?) {
                        Log.e("OktaAuth1", "Sign out failed: $error", exception)
                        CansUtils.isSignOutLoading = false
                        callback(false)
                        isCheckedSession = false
                    }
                })
            }
        }
    }
}