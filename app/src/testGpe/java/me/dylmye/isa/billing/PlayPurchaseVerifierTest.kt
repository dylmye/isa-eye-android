package me.dylmye.isa.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PlayPurchaseVerifierTest {
  private val keyPair: KeyPair =
    KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

  private val publicKey =
    Base64.getEncoder().encodeToString(keyPair.public.encoded)

  private fun verifier() = PlayPurchaseVerifier(
    base64PublicKey = publicKey,
    decode = { Base64.getDecoder().decode(it) },
  )

  @Test
  fun verify_acceptsJsonSignedByThePrivateKey() {
    val json = """{"productId":"full_access","purchaseState":0}"""
    val signature = sign(json)

    assertTrue(verifier().isConfigured)
    assertTrue(verifier().verify(json, signature))
  }

  @Test
  fun verify_rejectsTamperedJson() {
    val json = """{"productId":"full_access","purchaseState":0}"""
    val signature = sign(json)

    assertFalse(verifier().verify("""{"productId":"full_access","purchaseState":1}""", signature))
  }

  @Test
  fun verify_rejectsMissingOrMalformedData() {
    val verifier = verifier()

    assertFalse(verifier.verify(null, null))
    assertFalse(verifier.verify("", ""))
    assertFalse(verifier.verify("{}", "not base64"))
  }

  @Test
  fun verify_isNotConfiguredForAnInvalidPublicKey() {
    val verifier =
      PlayPurchaseVerifier(
        base64PublicKey = "not-a-public-key",
        decode = { Base64.getDecoder().decode(it) },
      )

    assertFalse(verifier.isConfigured)
    assertFalse(verifier.verify("{}", "AAAA"))
  }

  private fun sign(json: String): String = Signature.getInstance("SHA1withRSA").run {
    initSign(keyPair.private)
    update(json.toByteArray(Charsets.UTF_8))
    Base64.getEncoder().encodeToString(sign())
  }
}
