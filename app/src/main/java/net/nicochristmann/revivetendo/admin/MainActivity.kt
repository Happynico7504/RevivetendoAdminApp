package net.nicochristmann.revivetendo.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.nicochristmann.revivetendo.admin.cert.CertRenewalWorker
import net.nicochristmann.revivetendo.admin.ui.RevivetendoAdminApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CertRenewalWorker.schedulePeriodicChecks(applicationContext)
        setContent { RevivetendoAdminApp() }
    }
}
