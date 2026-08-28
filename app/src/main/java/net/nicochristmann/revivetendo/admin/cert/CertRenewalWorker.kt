package net.nicochristmann.revivetendo.admin.cert

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.nicochristmann.revivetendo.admin.net.ApiClient
import java.util.concurrent.TimeUnit

/**
 * Certs rotate every 14 days with a 28-day validity window (2 valid certs
 * overlap at all times server-side), so renewing once a day whenever fewer
 * than [RENEW_THRESHOLD_DAYS] remain leaves a wide safety margin even if the
 * app isn't opened for a while.
 */
class CertRenewalWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        ClientCertStore.init(applicationContext)
        val info = ClientCertStore.getCertInfo() ?: return@withContext Result.failure()
        if (info.daysRemaining() > RENEW_THRESHOLD_DAYS) return@withContext Result.success()
        try {
            renewNow()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val RENEW_THRESHOLD_DAYS = 5L
        private const val UNIQUE_WORK_NAME = "cert-renewal-check"

        fun renewNow() {
            ApiClient.getRawResponse("${ApiClient.ADMIN_BASE}/client-cert.p12").use { response ->
                if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                val bytes = response.body?.bytes() ?: throw IllegalStateException("empty response")
                ClientCertStore.importPkcs12(bytes)
                ApiClient.invalidateClient()
            }
        }

        fun schedulePeriodicChecks(context: Context) {
            val request = PeriodicWorkRequestBuilder<CertRenewalWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
