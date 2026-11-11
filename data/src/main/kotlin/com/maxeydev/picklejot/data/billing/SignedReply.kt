package com.maxeydev.picklejot.data.billing

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

internal object SignedReply {
    fun holds(
        publicKey: String,
        payload: String,
        signature: String,
    ): Boolean =
        try {
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))
            val key = KeyFactory.getInstance("EC").generatePublic(keySpec)
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(key)
                update(payload.toByteArray(Charsets.UTF_8))
                verify(Base64.getUrlDecoder().decode(signature))
            }
        } catch (failure: GeneralSecurityException) {
            false
        } catch (failure: IllegalArgumentException) {
            false
        }
}
