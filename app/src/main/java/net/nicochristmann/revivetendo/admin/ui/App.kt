package net.nicochristmann.revivetendo.admin.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.nicochristmann.revivetendo.admin.cert.ClientCertStore
import net.nicochristmann.revivetendo.admin.ui.screens.AdminWebViewScreen
import net.nicochristmann.revivetendo.admin.ui.screens.ImportCertScreen
import net.nicochristmann.revivetendo.admin.ui.theme.RevivetendoAdminTheme

/** True only when there's a cert AND it isn't already expired - a cert that
 *  merely exists but is expired should still route to re-import, same as
 *  no cert at all. */
private fun certReady(): Boolean =
    ClientCertStore.isCertInstalled() && (ClientCertStore.getCertInfo()?.daysRemaining() ?: -1) > 0

@Composable
fun RevivetendoAdminApp() {
    RevivetendoAdminTheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        val startDestination = remember { if (certReady()) "dashboard" else "import" }

        NavHost(navController = navController, startDestination = startDestination) {
            composable("import") {
                // A cert *file* existing but failing certReady() (expired,
                // or the "problem" path bounced here from the WebView) reads
                // as a replace, not a first-time import.
                ImportCertScreen(
                    isReimport = ClientCertStore.isCertInstalled(),
                    onImported = {
                        navController.navigate("dashboard") {
                            popUpTo("import") { inclusive = true }
                        }
                    },
                )
            }
            composable("dashboard") {
                AdminWebViewScreen(
                    onCertProblem = {
                        navController.navigate("import") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    onExit = { (context as? Activity)?.finish() },
                )
            }
        }
    }
}
