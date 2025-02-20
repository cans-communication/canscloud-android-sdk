package cc.cans.canscloud.data

import java.io.IOException
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class ProvisioningInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val builder: Request.Builder = originalRequest
            .newBuilder()
            .header(
                "Authorization",
                Credentials.basic(
                    "provision", "AIzaSyC2ZpuUWO0QjkJXYpIXmxROuIdWPhY9Ub0"
                )
            )
        val newRequest: Request = builder.build()
        return chain.proceed(newRequest)
    }
}
