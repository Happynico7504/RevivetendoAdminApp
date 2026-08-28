package net.nicochristmann.revivetendo.admin.cert

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyStore
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Holds the admin mTLS client certificate + private key app-privately, never
 * in Android's system-wide "Trusted credentials" store (a normal app can't
 * manage that silently - install always shows a system dialog, and removing
 * an old entry needs device-owner/MDM privileges no ordinary app has).
 *
 * The PKCS#12 bytes are encrypted at rest with a hardware-backed AES key
 * (Jetpack Security's EncryptedFile/MasterKey) but the TLS client-cert
 * signing itself is done by a plain in-memory "PKCS12" KeyStore, not
 * AndroidKeyStore. That's deliberate: importing the RSA key straight into
 * AndroidKeyStore and having conscrypt sign the TLS 1.3 CertificateVerify
 * through the device's real Keymaster/StrongBox HAL hit a
 * "RSA routines: internal error" on real hardware with BOTH PKCS1 and PSS
 * padding authorized - a known class of Keymaster HAL bug for raw RSA TLS
 * client-auth signing that doesn't affect AES encrypt/decrypt (a far
 * simpler, universally well-supported HAL operation). Doing the RSA sign in
 * pure software sidesteps that device-specific HAL risk entirely, at the
 * cost of the key briefly existing in process memory during a TLS handshake
 * - an acceptable trade for a personal admin tool.
 */
object ClientCertStore {
    private const val FILE_NAME = "admin_client_cert.p12.enc"
    private const val MASTER_KEY_ALIAS = "revivetendo_admin_master_key"
    private val EMPTY_PASSWORD = CharArray(0)

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

    private lateinit var appContext: Context

    /** Must be called once (MainActivity.onCreate / Worker.doWork) before any other member. */
    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }

    private fun certFile(): File = File(appContext.filesDir, FILE_NAME)

    private fun masterKey(): MasterKey =
        MasterKey.Builder(appContext, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private fun encryptedFile(file: File): EncryptedFile =
        EncryptedFile.Builder(appContext, file, masterKey(), EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()

    private fun readDecryptedBytes(): ByteArray? {
        val file = certFile()
        if (!file.exists()) return null
        return encryptedFile(file).openFileInput().use { it.readBytes() }
    }

    private fun loadPkcs12(bytes: ByteArray): KeyStore {
        val p12 = KeyStore.getInstance("PKCS12")
        try {
            p12.load(ByteArrayInputStream(bytes), EMPTY_PASSWORD)
        } catch (e: Exception) {
            throw InvalidCertException("This file isn't a valid client certificate bundle (.p12).", e)
        }
        return p12
    }

    fun isCertInstalled(): Boolean = certFile().exists()

    fun getCertInfo(): CertInfo? {
        val bytes = readDecryptedBytes() ?: return null
        val p12 = loadPkcs12(bytes)
        val alias = p12.aliases().asSequence().firstOrNull { p12.isKeyEntry(it) } ?: return null
        val cert = p12.getCertificate(alias) as? java.security.cert.X509Certificate ?: return null
        return CertInfo(
            subject = cert.subjectX500Principal.name,
            issuedAt = cert.notBefore,
            expiresAt = cert.notAfter,
        )
    }

    /** Validates the bundle, then atomically replaces any existing stored cert. */
    fun importPkcs12(bytes: ByteArray) {
        val p12 = loadPkcs12(bytes)
        if (p12.aliases().asSequence().none { p12.isKeyEntry(it) }) {
            throw InvalidCertException("The certificate bundle contains no private key entry.")
        }

        val file = certFile()
        if (file.exists()) {
            file.delete()
        }
        encryptedFile(file).openFileOutput().use { it.write(bytes) }
    }

    fun deleteCert() {
        certFile().delete()
    }

    /** Builds a fresh SSLContext presenting this app's stored client cert for mTLS. */
    fun buildSslContext(): Pair<SSLContext, X509TrustManager> {
        val bytes = readDecryptedBytes() ?: throw InvalidCertException("No certificate installed.")
        val p12 = loadPkcs12(bytes)

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(p12, EMPTY_PASSWORD)

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val trustManager = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, arrayOf(trustManager), null)
        return sslContext to trustManager
    }
}
