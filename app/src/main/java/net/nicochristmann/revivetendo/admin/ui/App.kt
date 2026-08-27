package net.nicochristmann.revivetendo.admin.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.nicochristmann.revivetendo.admin.cert.ClientCertStore
import net.nicochristmann.revivetendo.admin.ui.screens.AccessScreen
import net.nicochristmann.revivetendo.admin.ui.screens.BansScreen
import net.nicochristmann.revivetendo.admin.ui.screens.DashboardScreen
import net.nicochristmann.revivetendo.admin.ui.screens.ImportCertScreen
import net.nicochristmann.revivetendo.admin.ui.screens.RedirectsScreen
import net.nicochristmann.revivetendo.admin.ui.screens.ReviewScreen
import net.nicochristmann.revivetendo.admin.ui.screens.Spotpass3DSNotesScreen
import net.nicochristmann.revivetendo.admin.ui.screens.Spotpass3DSSysMsgScreen
import net.nicochristmann.revivetendo.admin.ui.screens.SpotpassWiiUScreen
import net.nicochristmann.revivetendo.admin.ui.screens.UsersScreen
import net.nicochristmann.revivetendo.admin.ui.theme.RevivetendoAdminTheme

@Composable
fun RevivetendoAdminApp() {
    RevivetendoAdminTheme {
        val navController = rememberNavController()
        val startDestination = if (ClientCertStore.isCertInstalled()) "dashboard" else "import"

        NavHost(navController = navController, startDestination = startDestination) {
            composable("import") {
                ImportCertScreen(onImported = {
                    navController.navigate("dashboard") {
                        popUpTo("import") { inclusive = true }
                    }
                })
            }
            composable("dashboard") {
                DashboardScreen(
                    onOpenSection = { route -> navController.navigate(route) },
                    onReimport = { navController.navigate("import") },
                )
            }
            composable("redirects") { RedirectsScreen(onBack = navController::popBackStack) }
            composable("users") { UsersScreen(onBack = navController::popBackStack) }
            composable("bans") { BansScreen(onBack = navController::popBackStack) }
            composable("access") { AccessScreen(onBack = navController::popBackStack) }
            composable("spotpass-wiiu") { SpotpassWiiUScreen(onBack = navController::popBackStack) }
            composable("spotpass-3ds") { Spotpass3DSNotesScreen(onBack = navController::popBackStack) }
            composable("spotpass-3ds-sysmsg") { Spotpass3DSSysMsgScreen(onBack = navController::popBackStack) }
            composable("review") { ReviewScreen(onBack = navController::popBackStack) }
        }
    }
}
