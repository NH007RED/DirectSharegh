package com.directshare.app.ui.navigation

sealed class ScreenRoute(val route: String) {
    data object Onboarding : ScreenRoute("onboarding")
    data object Home : ScreenRoute("home")
    data object SendFiles : ScreenRoute("send_files")
    data object ReceiveFiles : ScreenRoute("receive_files")
    data object DiscoveryPairing : ScreenRoute("discovery_pairing")
    data object ActiveTransfer : ScreenRoute("active_transfer")
    data object TransferHistory : ScreenRoute("transfer_history")
    data object Settings : ScreenRoute("settings")
    data object PrivacySecurity : ScreenRoute("privacy_security")
    data object Diagnostics : ScreenRoute("diagnostics")
}
