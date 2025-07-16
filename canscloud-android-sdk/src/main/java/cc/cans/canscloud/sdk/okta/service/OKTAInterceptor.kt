package cc.cans.canscloud.sdk.okta.service

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class OKTAInterceptor: Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val builder: Request.Builder = originalRequest
            .newBuilder()
            .header(
                "Authorization",
                Credentials.basic(
                    "forceupdate", "3v^jHw6NrM2kn*gqfP9KLcY@"
                )
            )
        val newRequest: Request = builder.build()
        return chain.proceed(newRequest)
    }
}
