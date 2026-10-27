package me.dylmye.isa.billing

import me.dylmye.isa.BuildConfig

/** The permanent one-time product that removes the Google Play account limit. */
internal const val FULL_ACCESS_PRODUCT_ID = "full_access"

/**
 * Base64 X.509 RSA public key from Play Console -> Monetization setup -> Licensing.
 *
 * This is the licensing key, not the upload keystore certificate or the Play App Signing
 * certificate. It is supplied at build time through `ISA_EYE_PLAY_LICENSING_PUBLIC_KEY`, the matching
 * Gradle property, or `playLicensingPublicKey` in `keystore.properties`. The generated BuildConfig
 * field exists only in the GPE flavour; the F-Droid build has no key and no Billing client.
 */
internal val PLAY_LICENSING_PUBLIC_KEY: String = BuildConfig.PLAY_LICENSING_PUBLIC_KEY
