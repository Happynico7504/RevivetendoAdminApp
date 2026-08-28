package net.nicochristmann.revivetendo.admin.cert

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Holds the admin mTLS client certificate + private key in this app's own
 * AndroidKeyStore namespace (alias [ALIAS]), never Android's system-wide
 * "Trusted credentials" store. A normal app can't manage that store silently:
 * installing into it always shows a system confirmation dialog, and removing
 * an old entry from it needs device-owner/MDM privileges no ordinary app has.
 * Keeping the cert app-private means check/install/replace/delete are all
 * fully automatic with zero dialogs - it just means the cert is only usable
 * by this app's own network calls, which is all it's ever needed for.
 */
object ClientCertStore {
    private const val ALIAS = "revivetendo_admin_client_cert"
    private const val KEYSTORE_TYPE = "AndroidKeyStore"

    class InvalidCertException(message: String, cause: Throwable? = null) : Exception(message, cause)

    data class CertInfo(
        val subject: String,
        val issuedAt: Date,
        val expiresAt: Date,
    ) {
        fun daysRemaining(): Long {
            val millis = expiresAt.time - Date().time
            return millis / (1000L * 60 * 60 * 24)
        }
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }

    fun isCertInstalled(): Boolean =
        try {
            keyStore().containsAlias(ALIAS)
        } catch (e: Exception) {
            false
        }

    fun getCertInfo(): CertInfo? {
        val ks = keyStore()
        if (!ks.containsAlias(ALIAS)) return null
        val cert = ks.getCertificate(ALIAS) as? X509Certificate ?: return null
        return CertInfo(
            subject = cert.subjectX500Principal.name,
            issuedAt = cert.notBefore,
            expiresAt = cert.notAfter,
        )
    }

    fun importPkcs12(bytes: ByteArray, password: CharArray = CharArray(0)) {
        importPkcs12(ByteArrayInputStream(bytes), password)
    }

    /** Deletes any existing entry, then imports the given PKCS#12 bundle. */
    fun importPkcs12(input: InputStream, password: CharArray = CharArray(0)) {
        val p12 = KeyStore.getInstance("PKCS12")
        try {
            p12.load(input, password)
        } catch (e: Exception) {
            throw InvalidCertException("This file isn't a valid client certificate bundle (.p12).", e)
        }

        val alias = p12.aliases().asSequence().firstOrNull { p12.isKeyEntry(it) }
            ?: throw InvalidCertException("The certificate bundle contains no private key entry.")

        val key = p12.getKey(alias, password) as? PrivateKey
            ?: throw InvalidCertException("Couldn't read the private key from the certificate bundle.")
        val chain: Array<Certificate> = p12.getCertificateChain(alias)
            ?: throw InvalidCertException("The certificate bundle has no certificate chain.")

        val protection = KeyProtection.Builder(KeyProperties.PURPOSE_SIGN)
            .setDigests(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA384,
                KeyProperties.DIGEST_SHA512,
            )
            // TLS 1.3's CertificateVerify requires RSA-PSS, not PKCS1 - without
            // authorizing PSS here, AndroidKeyStore refuses to sign with it and
            // BoringSSL surfaces that as a generic "RSA routines: internal
            // error" deep in conscrypt rather than a clear permission error.
            .setSignaturePaddings(
                KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                KeyProperties.SIGNATURE_PADDING_RSA_PSS,
            )
            .build()

        val ks = keyStore()
        if (ks.containsAlias(ALIAS)) {
            ks.deleteEntry(ALIAS)
        }
        ks.setEntry(ALIAS, KeyStore.PrivateKeyEntry(key, chain), protection)
    }

    fun deleteCert() {
        val ks = keyStore()
        if (ks.containsAlias(ALIAS)) {
            ks.deleteEntry(ALIAS)
        }
    }

    /** Builds a fresh SSLContext presenting this app's stored client cert for mTLS. */
    fun buildSslContext(): Pair<SSLContext, X509TrustManager> {
        val ks = keyStore()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, null)

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val trustManager = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, arrayOf(trustManager), null)
        return sslContext to trustManager
    }
}
