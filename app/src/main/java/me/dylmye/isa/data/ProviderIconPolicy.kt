package me.dylmye.isa.data

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** The only host ISA Eye loads from; provider logos are static SVGs served over HTTPS. */
internal const val PROVIDER_ICON_HOST = "static-assets.isaeye.uk"

/**
 * Keeps Coil from becoming an exfiltration path if seed data or a database is tampered with. The
 * app has no other network traffic, so anything outside the provider-icon host is a bug or abuse.
 */
internal object ProviderIconPolicy : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    if (request.url.scheme != "https" || request.url.host != PROVIDER_ICON_HOST) {
      throw IOException("Blocked non-provider-image request to ${request.url.host}")
    }
    return chain.proceed(request)
  }
}
