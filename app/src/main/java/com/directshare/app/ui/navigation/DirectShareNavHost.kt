package com.directshare.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.directshare.app.domain.model.TransferState
import com.directshare.app.ui.components.DirectShareBottomNavBar
import com.directshare.app.ui.components.LiquidGlassBackground
import com.directshare.app.ui.screens.ActiveTransferScreen
import com.directshare.app.ui.screens.DiagnosticsScreen
import com.directshare.app.ui.screens.DirectShareViewModel
import com.directshare.app.ui.screens.DiscoveryPairingScreen
import com.directshare.app.ui.screens.HomeScreen
import com.directshare.app.ui.screens.OnboardingScreen
import com.directshare.app.ui.screens.PrivacySecurityScreen
import com.directshare.app.ui.screens.ReceiveFilesScreen
import com.directshare.app.ui.screens.SendFilesScreen
import com.directshare.app.ui.screens.SettingsScreen
import com.directshare.app.ui.screens.TransferHistoryScreen

@Composable
fun DirectShareNavHost(
    viewModel: DirectShareViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsState()
    val transferState by viewModel.transferState.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val isHost by viewModel.isHostOfSession.collectAsState()
    val onlinePeers by viewModel.onlinePeers.collectAsState()
    val historyRecords by viewModel.historyRecords.collectAsState()
    val stats by viewModel.transferStats.collectAsState()
    val firebaseConnected by viewModel.firebaseConnected.collectAsState()
    val bannerMessage by viewModel.uiBannerMessage.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBannerMessage()
        }
    }

    LaunchedEffect(transferState) {
        when (transferState) {
            is TransferState.IncomingTransferPrompt,
            is TransferState.WaitingForReceiverApproval,
            is TransferState.Sending,
            is TransferState.Receiving,
            is TransferState.Completed,
            is TransferState.Failed,
            is TransferState.Cancelled -> {
                if (currentRoute != ScreenRoute.ActiveTransfer.route) {
                    navController.navigate(ScreenRoute.ActiveTransfer.route) {
                        launchSingleTop = true
                    }
                }
            }
            else -> Unit
        }
    }

    // Always launch with the Startup Loading & 3-Language File Sharing Guide screen before Home
    val startDestination = ScreenRoute.Onboarding.route

    val showBottomBar = currentRoute in listOf(
        ScreenRoute.Home.route,
        ScreenRoute.SendFiles.route,
        ScreenRoute.DiscoveryPairing.route,
        ScreenRoute.TransferHistory.route,
        ScreenRoute.Settings.route
    )

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    DirectShareBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { targetRoute ->
                            navController.navigate(targetRoute) {
                                popUpTo(ScreenRoute.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(ScreenRoute.Onboarding.route) {
                        OnboardingScreen(
                            onGetStarted = {
                                viewModel.completeOnboarding()
                                navController.navigate(ScreenRoute.Home.route) {
                                    popUpTo(ScreenRoute.Onboarding.route) { inclusive = true }
                                }
                            },
                            onOpenPrivacyDetails = {
                                navController.navigate(ScreenRoute.PrivacySecurity.route)
                            }
                        )
                    }

                    composable(ScreenRoute.Home.route) {
                        HomeScreen(
                            deviceName = settings.deviceDisplayName,
                            firebaseConnected = firebaseConnected,
                            activeSession = activeSession,
                            transferState = transferState,
                            onlinePeers = onlinePeers,
                            recentTransfers = historyRecords,
                            statistics = stats,
                            onNavigateSend = { navController.navigate(ScreenRoute.SendFiles.route) },
                            onNavigateReceive = { navController.navigate(ScreenRoute.ReceiveFiles.route) },
                            onNavigateConnect = { navController.navigate(ScreenRoute.DiscoveryPairing.route) },
                            onNavigateActiveTransfer = { navController.navigate(ScreenRoute.ActiveTransfer.route) },
                            onNavigateHistory = { navController.navigate(ScreenRoute.TransferHistory.route) },
                            onNavigateSettings = { navController.navigate(ScreenRoute.Settings.route) },
                            onNavigateDiagnostics = { navController.navigate(ScreenRoute.Diagnostics.route) },
                            onDisconnectSession = { viewModel.disconnectCurrentSession() }
                        )
                    }

                    composable(ScreenRoute.SendFiles.route) {
                        SendFilesScreen(
                            selectedFiles = selectedFiles,
                            transferState = transferState,
                            onFilesPicked = { viewModel.onFilesPicked(it) },
                            onRemoveFile = { viewModel.removeSelectedFile(it) },
                            onClearAll = { viewModel.clearSelectedFiles() },
                            onNavigateConnect = { navController.navigate(ScreenRoute.DiscoveryPairing.route) },
                            onConfirmSend = { viewModel.sendSelectedFilesToConnectedPeer() }
                        )
                    }

                    composable(ScreenRoute.ReceiveFiles.route) {
                        ReceiveFilesScreen(
                            activeSession = activeSession,
                            transferState = transferState,
                            onStartHostingSession = { viewModel.startHostingSession() },
                            onNavigatePairing = { navController.navigate(ScreenRoute.DiscoveryPairing.route) },
                            onAcceptTransfer = { viewModel.respondToIncomingTransfer(true) },
                            onDeclineTransfer = { viewModel.respondToIncomingTransfer(false) }
                        )
                    }

                    composable(ScreenRoute.DiscoveryPairing.route) {
                        DiscoveryPairingScreen(
                            onlinePeers = onlinePeers,
                            activeSession = activeSession,
                            isHost = isHost,
                            transferState = transferState,
                            onStartHosting = { viewModel.startHostingSession() },
                            onJoinByCode = { viewModel.joinSessionWithInput(it) },
                            onConnectPeer = { viewModel.connectToDiscoveredPeer(it) },
                            onAuthorizeGuest = { viewModel.respondToGuestAuthorization(it) },
                            onDisconnect = { viewModel.disconnectCurrentSession() }
                        )
                    }

                    composable(ScreenRoute.ActiveTransfer.route) {
                        ActiveTransferScreen(
                            transferState = transferState,
                            onCancelTransfer = { viewModel.cancelActiveTransfer() },
                            onAcceptIncoming = { viewModel.respondToIncomingTransfer(true) },
                            onDeclineIncoming = { viewModel.respondToIncomingTransfer(false) },
                            onOpenFile = { uri, mime -> viewModel.openReceivedFile(context, uri, mime) },
                            onShareFile = { uri, mime, name -> viewModel.shareReceivedFile(context, uri, mime, name) },
                            onDoneOrRetry = {
                                viewModel.resetTransferStateToReadyOrIdle()
                                navController.navigate(ScreenRoute.Home.route) {
                                    popUpTo(ScreenRoute.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(ScreenRoute.TransferHistory.route) {
                        TransferHistoryScreen(
                            records = historyRecords,
                            onOpenSavedFile = { uri, mime -> viewModel.openReceivedFile(context, uri, mime) },
                            onClearHistory = { viewModel.clearAllHistory() }
                        )
                    }

                    composable(ScreenRoute.Settings.route) {
                        SettingsScreen(
                            settings = settings,
                            onUpdateDeviceName = { viewModel.updateDeviceName(it) },
                            onUpdateSaveDestination = { viewModel.updateDefaultSaveDestination(context, it) },
                            onUpdateTheme = { viewModel.updateTheme(it) },
                            onUpdateSound = { viewModel.updateSoundEnabled(it) },
                            onUpdateVibration = { viewModel.updateVibrationEnabled(it) },
                            onUpdateAutoAccept = { viewModel.updateAutoAccept(it) },
                            onUpdateMaxConcurrent = { viewModel.updateMaxConcurrentTransfers(it) },
                            onUpdateChunkSize = { viewModel.updateChunkSizeBytes(it) },
                            onUpdateTurnServer = { url, user, pass -> viewModel.updateTurnConfiguration(url, user, pass) },
                            onClearHistory = { viewModel.clearAllHistory() },
                            onOpenPrivacyInfo = { navController.navigate(ScreenRoute.PrivacySecurity.route) },
                            onOpenDiagnostics = { navController.navigate(ScreenRoute.Diagnostics.route) },
                            onDisconnectAllSessions = { viewModel.disconnectAllSessions() }
                        )
                    }

                    composable(ScreenRoute.PrivacySecurity.route) {
                        PrivacySecurityScreen(onBack = { navController.popBackStack() })
                    }

                    composable(ScreenRoute.Diagnostics.route) {
                        DiagnosticsScreen(
                            diagnostics = diagnostics,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
