package me.dylmye.isa.billing

import android.util.Base64
import com.android.billingclient.api.Purchase
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Verifies the signature Google attaches to a Billing purchase.
 *
 * The public key proves that [Purchase.originalJson] was signed by Google; it is not a secret and
 * does not prevent a modified APK from bypassing the check. A backend using the Play Developer API
 * would be stronger, but this app deliberately has no backend and the F-Droid build is free.
 */
internal class PlayPurchaseVerifier(
  base64PublicKey: String,
  private val decode: (String) -> ByteArray = {
    Base64.decode(it, Base64.DEFAULT)
  },
) {
  private val publicKey: PublicKey? =
    runCatching {
      val cleaned = base64PublicKey.filterNot { it.isWhitespace() }
      KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decode(cleaned)))
    }
      .getOrNull()

  /** False when the placeholder key is still in place, so callers can report a configuration error. */
  val isConfigured: Boolean
    get() = publicKey != null

  fun verify(purchase: Purchase): Boolean = verify(purchase.originalJson, purchase.signature)

  fun verify(originalJson: String?, signature: String?): Boolean {
    val key = publicKey ?: return false
    if (originalJson.isNullOrBlank() || signature.isNullOrBlank()) return false
    return runCatching {
      Signature.getInstance("SHA1withRSA").run {
        initVerify(key)
        update(originalJson.toByteArray(Charsets.UTF_8))
        verify(decode(signature))
      }
    }
      .getOrDefault(false)
  }
}
