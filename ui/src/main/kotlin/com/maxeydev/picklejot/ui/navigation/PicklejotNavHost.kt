package com.maxeydev.picklejot.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.ui.PicklejotDependencies
import com.maxeydev.picklejot.ui.common.rememberIsResumed
import com.maxeydev.picklejot.ui.dashboard.DashboardViewModel
import com.maxeydev.picklejot.ui.dashboard.ExpandedStatsScreen
import com.maxeydev.picklejot.ui.match.detail.MatchDetailScreen
import com.maxeydev.picklejot.ui.match.detail.MatchDetailViewModel
import com.maxeydev.picklejot.ui.match.edit.MatchEditActions
import com.maxeydev.picklejot.ui.match.edit.MatchEditScreen
import com.maxeydev.picklejot.ui.match.edit.MatchEditViewModel
import com.maxeydev.picklejot.ui.match.edit.PhotoPickerActions
import com.maxeydev.picklejot.ui.match.list.FirstMatchSheet
import com.maxeydev.picklejot.ui.match.list.MatchListFilterActions
import com.maxeydev.picklejot.ui.match.list.MatchListScreen
import com.maxeydev.picklejot.ui.match.list.MatchListViewModel
import com.maxeydev.picklejot.ui.onboarding.OnboardingScreen
import com.maxeydev.picklejot.ui.onboarding.OnboardingViewModel
import com.maxeydev.picklejot.ui.paywall.CapWarningBanner
import com.maxeydev.picklejot.ui.paywall.PaywallScreen
import com.maxeydev.picklejot.ui.paywall.PaywallViewModel
import com.maxeydev.picklejot.ui.paywall.ProUnlockedSheet
import com.maxeydev.picklejot.ui.paywall.StoreMessage
import com.maxeydev.picklejot.ui.settings.AboutScreen
import com.maxeydev.picklejot.ui.settings.BackupActions
import com.maxeydev.picklejot.ui.settings.BackupSettingsScreen
import com.maxeydev.picklejot.ui.settings.BackupViewModel
import com.maxeydev.picklejot.ui.settings.ExportPromptBanner
import com.maxeydev.picklejot.ui.settings.ExportPromptViewModel
import com.maxeydev.picklejot.ui.settings.PrivacyPolicyScreen
import com.maxeydev.picklejot.ui.settings.SettingsActions
import com.maxeydev.picklejot.ui.settings.SettingsDrawer
import com.maxeydev.picklejot.ui.settings.SettingsDrawerHost
import com.maxeydev.picklejot.ui.settings.SettingsViewModel
import com.maxeydev.picklejot.ui.settings.SupportScreen
import com.maxeydev.picklejot.ui.settings.openStoreListing
import com.maxeydev.picklejot.ui.settings.shareApp
import com.maxeydev.picklejot.ui.share.ResourceCardLabels
import com.maxeydev.picklejot.ui.share.ShareIntentLauncher
import com.maxeydev.picklejot.ui.share.SharePreviewScreen
import com.maxeydev.picklejot.ui.share.SharePreviewViewModel
import com.maxeydev.picklejot.ui.share.VariantPickerActions
import com.maxeydev.picklejot.ui.sound.Cue
import com.maxeydev.picklejot.ui.sound.LocalSoundEffects
import com.maxeydev.picklejot.ui.streak.MissedSkipNotice
import com.maxeydev.picklejot.ui.streak.SkipUsedNotice
import com.maxeydev.picklejot.ui.streak.StreakMilestoneHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

@Composable
fun PicklejotNavHost(
    dependencies: PicklejotDependencies,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openLogging: Boolean = false,
    onOpenLoggingHandled: () -> Unit = {},
) {
    val requiresOnboarding by produceState<Boolean?>(initialValue = null, dependencies) {
        value = dependencies.onboarding.isRequired()
    }
    requiresOnboarding?.let { required ->
        CompositionLocalProvider(LocalSoundEffects provides dependencies.soundEffects) {
            PicklejotNavGraph(
                dependencies = dependencies,
                requiresOnboarding = required,
                modifier = modifier,
                navController = navController,
                openLogging = openLogging,
                onOpenLoggingHandled = onOpenLoggingHandled,
            )
        }
    }
}

@Composable
private fun PicklejotNavGraph(
    dependencies: PicklejotDependencies,
    requiresOnboarding: Boolean,
    modifier: Modifier,
    navController: NavHostController,
    openLogging: Boolean,
    onOpenLoggingHandled: () -> Unit,
) {
    val startDestination: Any = if (requiresOnboarding) OnboardingRoute else HomeRoute
    var proSheetRestored by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val sound = LocalSoundEffects.current
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { ScreenEnter },
        exitTransition = { ScreenExit },
        popEnterTransition = { ScreenPopEnter },
        popExitTransition = { ScreenPopExit },
        predictivePopEnterTransition = { ScreenPredictivePopEnter },
        predictivePopExitTransition = { ScreenPredictivePopExit },
    ) {
        composable<OnboardingRoute> {
            val viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isFinished) {
                if (state.isFinished) {
                    sound.play(Cue.WELCOME)
                    navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                }
            }
            OnboardingScreen(
                state = state,
                onNameChanged = viewModel::changeName,
                onContinue = viewModel::continueToApp,
            )
        }
        composable<HomeRoute>(
            exitTransition = { stayUnderSheet() },
            popEnterTransition = { returnFromSheet() },
        ) { backStackEntry ->
            val viewModel: MatchListViewModel =
                viewModel(factory = MatchListViewModel.factory(dependencies, backStackEntry.savedStateHandle))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val dashboardViewModel: DashboardViewModel =
                viewModel(factory = DashboardViewModel.factory(dependencies, backStackEntry.savedStateHandle))
            val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
            val exportPromptViewModel: ExportPromptViewModel =
                viewModel(factory = ExportPromptViewModel.factory(dependencies))
            val exportPromptState by exportPromptViewModel.uiState.collectAsStateWithLifecycle()
            var settingsOpen by rememberSaveable { mutableStateOf(false) }
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(dependencies))
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val isHomeResumed = rememberIsResumed(backStackEntry)
            val isFirstMatchSaved = state.savedMatchId != null && state.totalCount == 1
            LaunchedEffect(settingsState.isErased) {
                if (settingsState.isErased) {
                    settingsViewModel.erasedHandled()
                    settingsOpen = false
                    sound.play(Cue.ERASE_DONE)
                    navController.navigate(OnboardingRoute) { popUpTo<HomeRoute> { inclusive = true } }
                }
            }
            LaunchedEffect(settingsOpen) {
                if (settingsOpen) {
                    settingsViewModel.refreshCacheSize()
                }
            }
            var wasClearingCache by remember { mutableStateOf(false) }
            LaunchedEffect(settingsState.isClearingCache) {
                if (wasClearingCache && !settingsState.isClearingCache) {
                    sound.play(Cue.SWEEP)
                }
                wasClearingCache = settingsState.isClearingCache
            }
            val settingsActions =
                remember(settingsViewModel) {
                    SettingsActions(
                        onClose = { settingsOpen = false },
                        onSeePro = { navController.navigate(PaywallRoute) },
                        onOpenBackup = { navController.navigate(BackupRoute) },
                        onOpenPrivacy = { navController.navigate(PrivacyRoute) },
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        onRateUs = { openStoreListing(context) },
                        onOpenSupport = { navController.navigate(SupportRoute) },
                        onClearCache = settingsViewModel::clearCache,
                        onEnableReminder = {
                            sound.play(Cue.REMINDER_ON)
                            settingsViewModel.enableReminder()
                        },
                        onDisableReminder = {
                            sound.play(Cue.TOGGLE_OFF)
                            settingsViewModel.disableReminder()
                        },
                        onReminderTimeChanged = settingsViewModel::changeReminderTime,
                        onNameChanged = settingsViewModel::changeName,
                        onSaveName = settingsViewModel::saveName,
                        onNameEditCancelled = settingsViewModel::discardNameDraft,
                        onDarkThemeChanged = { dark ->
                            sound.play(if (dark) Cue.TOGGLE_ON else Cue.TOGGLE_OFF)
                            settingsViewModel.changeDarkTheme(dark)
                        },
                        onSoundEffectsChanged = { enabled ->
                            if (enabled) {
                                sound.preview(Cue.POCK)
                            }
                            settingsViewModel.changeSoundEffects(enabled)
                        },
                        onEraseConfirmed = settingsViewModel::eraseAll,
                        onEraseFailureDismissed = settingsViewModel::dismissEraseFailure,
                    )
                }
            SettingsDrawerHost(
                open = settingsOpen,
                onDismiss = { settingsOpen = false },
                drawer = { drawerModifier ->
                    SettingsDrawer(
                        state = settingsState,
                        versionName = dependencies.appVersionName,
                        actions = settingsActions,
                        modifier = drawerModifier,
                    )
                },
            ) {
                MatchListScreen(
                    state = if (isFirstMatchSaved) state.copy(savedMatchId = null) else state,
                    dashboard = dashboardState,
                    onOpenStats = { navController.navigate(StatsRoute) },
                    notices = {
                        Column {
                            CapWarningBanner(
                                warning = state.capWarning,
                                remainingFreeMatches = state.remainingFreeMatches,
                                onDismiss = viewModel::dismissCapWarning,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            )
                            ExportPromptBanner(
                                reason = exportPromptState.reason,
                                onExport = {
                                    exportPromptViewModel.dismiss()
                                    navController.navigate(BackupRoute)
                                },
                                onDismiss = exportPromptViewModel::dismiss,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            )
                            SkipUsedNotice(
                                skippedWeek = dashboardState.usedSkipWeek,
                                skipsHeld = dashboardState.skipsHeld,
                                onDismiss = dashboardViewModel::dismissUsedSkip,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            )
                            MissedSkipNotice(
                                opportunity = dashboardState.missedOpportunity,
                                onSeePro = { navController.navigate(PaywallRoute) },
                                onDismiss = dashboardViewModel::dismissMissedOpportunity,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            )
                        }
                    },
                    onNewMatch = {
                        sound.play(Cue.NEW_MATCH)
                        navController.navigate(MatchEditRoute())
                    },
                    onOpenMatch = { matchId -> navController.navigate(MatchDetailRoute(matchId)) },
                    onSortSelected = { sort ->
                        sound.play(Cue.SHUFFLE)
                        viewModel.selectSort(sort)
                    },
                    onLastVisibleIndexChanged = viewModel::loadMoreIfNeeded,
                    onLogAnother = { savedMatchId ->
                        sound.play(Cue.LOG_ANOTHER)
                        navController.navigate(MatchEditRoute(logAnotherFrom = savedMatchId))
                    },
                    onSavedConfirmationDismissed = viewModel::dismissSavedConfirmation,
                    onOpenSettings = { settingsOpen = true },
                    filterActions =
                        remember(viewModel) {
                            MatchListFilterActions(
                                onSearchChanged = viewModel::changeSearch,
                                onFilterChanged = { filter ->
                                    sound.play(Cue.TICK_SELECT)
                                    viewModel.changeFilter(filter)
                                },
                                onFilterCleared = { kind ->
                                    sound.play(Cue.TICK_CLEAR)
                                    viewModel.clearFilter(kind)
                                },
                                onAllFiltersCleared = {
                                    sound.play(Cue.SWEEP)
                                    viewModel.clearAllFilters()
                                },
                                onFiltersAndSearchCleared = {
                                    sound.play(Cue.SWEEP)
                                    viewModel.clearFiltersAndSearch()
                                },
                            )
                        },
                )
            }
            StreakMilestoneHost(
                streakWeeks = dashboardState.streak.current,
                isLoaded = !dashboardState.isLoading,
                isForeground = isHomeResumed,
            )
            if (isFirstMatchSaved && isHomeResumed) {
                FirstMatchSheet(
                    onDismiss = viewModel::dismissSavedConfirmation,
                    onLogAnother = {
                        state.savedMatchId?.let { savedMatchId ->
                            sound.play(Cue.LOG_ANOTHER)
                            viewModel.dismissSavedConfirmation()
                            navController.navigate(MatchEditRoute(logAnotherFrom = savedMatchId))
                        }
                    },
                )
            }
        }
        composable<StatsRoute> {
            val homeEntry = remember(it) { navController.getBackStackEntry<HomeRoute>() }
            val viewModel: DashboardViewModel =
                viewModel(
                    viewModelStoreOwner = homeEntry,
                    factory = DashboardViewModel.factory(dependencies, homeEntry.savedStateHandle),
                )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ExpandedStatsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onSeePro = { navController.navigate(PaywallRoute) },
            )
        }
        composable<ShareRoute> {
            val context = LocalContext.current
            val labels = remember(context) { ResourceCardLabels(context) }
            val viewModel: SharePreviewViewModel =
                viewModel(factory = SharePreviewViewModel.factory(dependencies, labels))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val launcher = remember { ShareIntentLauncher(dependencies.ioDispatcher) }
            val scope = rememberCoroutineScope()
            var soundedCard by remember { mutableStateOf(state.card) }
            LaunchedEffect(state.card) {
                val card = state.card
                if (card != null && card !== soundedCard) {
                    sound.play(if (soundedCard == null) Cue.CARD_READY else Cue.CARD_SWAP)
                    soundedCard = card
                }
            }
            LaunchedEffect(state.isGone) {
                if (state.isGone) {
                    navController.popBackStack()
                }
            }
            SharePreviewScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onShare = {
                    state.card?.let { card ->
                        sound.play(Cue.SHARE_SEND)
                        scope.launch {
                            try {
                                launcher.launch(context, launcher.prepare(context, card))
                            } catch (unwritable: IOException) {
                                viewModel.reportShareFailed()
                            }
                        }
                    }
                },
                onRetry = viewModel::retry,
                onDismissUpgrade = viewModel::dismissUpgrade,
                onSeePro = {
                    viewModel.dismissUpgrade()
                    navController.navigate(PaywallRoute)
                },
                variantActions =
                    remember(viewModel) {
                        VariantPickerActions(
                            onRatioSelected = viewModel::selectRatio,
                            onThemeSelected = viewModel::selectTheme,
                            onLayoutSelected = viewModel::selectLayout,
                            onDetailShownChanged = viewModel::setDetailShown,
                        )
                    },
            )
        }
        composable<PaywallRoute> {
            val viewModel: PaywallViewModel = viewModel(factory = PaywallViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isUnlocked) {
                if (state.isUnlocked) {
                    proSheetRestored = state.message == StoreMessage.RESTORED
                    navController.popBackStack()
                }
            }
            PaywallScreen(
                state = state,
                onBuy = viewModel::buy,
                onRestore = viewModel::restore,
                onRetryPrice = viewModel::retryPrice,
                onClose = { navController.popBackStack() },
            )
        }
        composable<PrivacyRoute> {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable<AboutRoute> {
            AboutScreen(
                versionName = dependencies.appVersionName,
                onBack = { navController.popBackStack() },
            )
        }
        composable<SupportRoute> {
            val context = LocalContext.current
            val homeEntry = remember(it) { navController.getBackStackEntry<HomeRoute>() }
            val settingsViewModel: SettingsViewModel =
                viewModel(viewModelStoreOwner = homeEntry, factory = SettingsViewModel.factory(dependencies))
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            SupportScreen(
                hasPro = settingsState.hasPro,
                onBack = { navController.popBackStack() },
                onSeePro = { navController.navigate(PaywallRoute) },
                onRateUs = { openStoreListing(context) },
                onShareApp = { shareApp(context) },
            )
        }
        composable<BackupRoute> {
            val viewModel: BackupViewModel = viewModel(factory = BackupViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val actions =
                remember(viewModel) {
                    BackupActions(
                        onBack = { navController.popBackStack() },
                        onExportRequested = viewModel::requestExport,
                        onExportConfirmed = viewModel::confirmExport,
                        onExportDialogDismissed = viewModel::dismissExportDialog,
                        onShareLaunched = viewModel::shareLaunched,
                        onShareFailed = viewModel::shareFailed,
                        onImportPicked = viewModel::importPicked,
                        onImportSummaryDismissed = viewModel::dismissImportSummary,
                        onMessageDismissed = viewModel::dismissMessage,
                    )
                }
            BackupSettingsScreen(state = state, actions = actions)
        }
        composable<MatchEditRoute>(
            enterTransition = { SheetEnter },
            popExitTransition = { SheetExit },
        ) { backStackEntry ->
            val viewModel: MatchEditViewModel = viewModel(factory = MatchEditViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            var hasSheetOpened by rememberSaveable { mutableStateOf(false) }
            val sheetProgress = remember { Animatable(if (hasSheetOpened) 1f else 0f) }
            val isLeaving = transition.targetState == EnterExitState.PostExit
            LaunchedEffect(isLeaving) {
                if (isLeaving) {
                    @SuppressLint("RestrictedApi")
                    val isPopped = navController.currentBackStack.value.none { entry -> entry.id == backStackEntry.id }
                    if (isPopped) {
                        sheetProgress.animateTo(0f, SheetCloseSpec)
                    }
                } else if (sheetProgress.value < 1f) {
                    withTimeoutOrNull(SHEET_READY_TIMEOUT_MILLIS) {
                        snapshotFlow { viewModel.uiState.value.isLoading }.first { isLoading -> !isLoading }
                    }
                    withFrameNanos { }
                    withFrameNanos { }
                    sheetProgress.animateTo(1f, SheetOpenSpec)
                    hasSheetOpened = true
                }
            }
            LaunchedEffect(state.isPaywallRequested) {
                if (state.isPaywallRequested) {
                    viewModel.paywallOpened()
                    navController.navigate(PaywallRoute)
                }
            }
            var soundedResultErrors by rememberSaveable { mutableIntStateOf(state.resultErrorAttempts) }
            LaunchedEffect(state.resultErrorAttempts) {
                if (state.resultErrorAttempts > soundedResultErrors) {
                    sound.play(Cue.SOFT_ERROR)
                }
                soundedResultErrors = state.resultErrorAttempts
            }
            val readyPhotoCount = state.photos.count { !it.isImporting }
            var knownPhotoCount by remember { mutableStateOf<Int?>(null) }
            LaunchedEffect(readyPhotoCount, state.isLoading) {
                if (!state.isLoading) {
                    val known = knownPhotoCount
                    if (known != null && readyPhotoCount > known) {
                        sound.play(Cue.PHOTO_ADDED)
                    }
                    knownPhotoCount = readyPhotoCount
                }
            }
            LaunchedEffect(state.isFinished) {
                if (state.isFinished) {
                    if (state.didSave) {
                        sound.play(if (state.savedNewMatchId != null) Cue.MATCH_SAVED else Cue.MATCH_UPDATED)
                    }
                    state.savedNewMatchId?.let { savedMatchId ->
                        navController.previousBackStackEntry
                            ?.takeIf { it.destination.hasRoute<HomeRoute>() }
                            ?.savedStateHandle
                            ?.set(JUST_SAVED_MATCH_ID_KEY, savedMatchId)
                    }
                    navController.popBackStack()
                }
            }
            val actions =
                remember(viewModel) {
                    MatchEditActions(
                        onFormatSelected = { format ->
                            sound.play(if (format == MatchFormat.SINGLES) Cue.FORMAT_SINGLES else Cue.FORMAT_DOUBLES)
                            viewModel.selectFormat(format)
                        },
                        onResultSelected = { result ->
                            sound.play(if (result == MatchResult.WIN) Cue.RESULT_WIN else Cue.RESULT_LOSS)
                            viewModel.selectResult(result)
                        },
                        onDateSelected = viewModel::selectDate,
                        onStartTimeChanged = viewModel::changeStartTime,
                        onEndTimeChanged = viewModel::changeEndTime,
                        onPersonNameChanged = viewModel::changePersonName,
                        onGameAdded = {
                            sound.play(Cue.POP_IN)
                            viewModel.addGame()
                        },
                        onGameRemoved = { index ->
                            sound.play(Cue.POP_OUT)
                            viewModel.removeGame(index)
                        },
                        onGameScoresChanged = viewModel::changeGameScores,
                        onLocationChanged = viewModel::changeLocation,
                        onPaddleChanged = viewModel::changePaddle,
                        onNotesChanged = viewModel::changeNotes,
                        onSuggestionFocusChanged = { target, isFocused ->
                            if (isFocused) {
                                viewModel.focusSuggestionTarget(target)
                            } else {
                                viewModel.leaveSuggestionTarget(target)
                            }
                        },
                        onSuggestionSelected = { target, suggestion ->
                            sound.play(Cue.SNAP)
                            viewModel.selectSuggestion(target, suggestion)
                        },
                        photoActions =
                            PhotoPickerActions(
                                newCaptureUri = dependencies::newCaptureUri,
                                onPhotosPicked = viewModel::addPickedPhotos,
                                onPhotoCaptured = viewModel::addCapturedPhoto,
                                onPhotoMoved = { key, offset ->
                                    sound.play(Cue.TICK_SELECT)
                                    viewModel.movePhoto(key, offset)
                                },
                                onPhotoRemoved = { key ->
                                    sound.play(Cue.WHISK)
                                    viewModel.removePhoto(key)
                                },
                                onPhotoErrorDismissed = viewModel::dismissPhotoError,
                            ),
                        onSave = viewModel::save,
                        onClose = { navController.popBackStack() },
                        onUpgradePromptDismissed = viewModel::dismissUpgradePrompt,
                        onSeePro = {
                            viewModel.dismissUpgradePrompt()
                            navController.navigate(PaywallRoute)
                        },
                    )
                }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = SHEET_SCRIM_ALPHA * sheetProgress.value }
                            .background(Color.Black),
                )
                MatchEditScreen(
                    state = state,
                    actions = actions,
                    modifier =
                        Modifier.graphicsLayer {
                            val remaining = 1f - sheetProgress.value
                            val corner = SHEET_CORNER * remaining
                            translationY = remaining * size.height
                            shape = RoundedCornerShape(topStart = corner, topEnd = corner)
                            clip = true
                        },
                )
            }
        }
        composable<MatchDetailRoute>(
            exitTransition = { stayUnderSheet() },
            popEnterTransition = { returnFromSheet() },
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<MatchDetailRoute>()
            val viewModel: MatchDetailViewModel = viewModel(factory = MatchDetailViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isGone) {
                if (state.isGone) {
                    navController.popBackStack()
                }
            }
            MatchDetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onEdit = {
                    sound.play(Cue.SHEET_UP)
                    navController.navigate(MatchEditRoute(route.matchId))
                },
                onShare = { navController.navigate(ShareRoute(route.matchId)) },
                onDeleteRequested = viewModel::requestDelete,
                onDeleteConfirmed = {
                    sound.play(Cue.DELETE)
                    viewModel.confirmDelete()
                },
                onDeleteDismissed = viewModel::dismissDelete,
            )
        }
    }
    proSheetRestored?.let { restored ->
        ProUnlockedSheet(restored = restored, onDismiss = { proSheetRestored = null })
    }
    LaunchedEffect(openLogging) {
        if (openLogging) {
            if (!requiresOnboarding) {
                navController.navigate(MatchEditRoute()) { launchSingleTop = true }
            }
            onOpenLoggingHandled()
        }
    }
}
