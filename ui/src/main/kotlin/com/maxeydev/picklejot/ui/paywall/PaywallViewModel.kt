package com.maxeydev.picklejot.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.billing.PriceResult
import com.maxeydev.picklejot.domain.billing.ProStore
import com.maxeydev.picklejot.domain.billing.PurchaseOutcome
import com.maxeydev.picklejot.domain.billing.RestoreOutcome
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import com.maxeydev.picklejot.domain.stats.BasicStats
import com.maxeydev.picklejot.domain.streak.StreakEngine
import com.maxeydev.picklejot.ui.PicklejotDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val matchRepository: MatchRepository,
    private val streakEngine: StreakEngine,
    private val proStore: ProStore,
    private val entitlementRepository: EntitlementRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch { loadRecord() }
        viewModelScope.launch { loadPrice() }
        viewModelScope.launch {
            entitlementRepository.observeEntitlement().collect { entitlement ->
                if (entitlement.isPro) {
                    mutableUiState.update { it.copy(isUnlocked = true) }
                }
            }
        }
    }

    fun buy() {
        if (!mutableUiState.value.canBuy) {
            return
        }
        mutableUiState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            val message =
                when (val outcome = proStore.purchase()) {
                    PurchaseOutcome.Unlocked -> StoreMessage.UNLOCKED
                    PurchaseOutcome.Canceled -> null
                    PurchaseOutcome.Pending -> StoreMessage.PENDING
                    is PurchaseOutcome.Failed -> StoreMessage.of(outcome.problem)
                }
            mutableUiState.update { it.copy(isWorking = false, message = message) }
        }
    }

    fun restore() {
        if (mutableUiState.value.isWorking) {
            return
        }
        mutableUiState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            val message = restoreMessage(proStore.restore())
            mutableUiState.update { it.copy(isWorking = false, message = message) }
        }
    }

    fun retryPrice() {
        if (mutableUiState.value.isLoadingPrice) {
            return
        }
        viewModelScope.launch { loadPrice() }
    }

    private suspend fun loadPrice() {
        mutableUiState.update { it.copy(isLoadingPrice = true) }
        when (val price = proStore.loadPrice()) {
            is PriceResult.Available ->
                mutableUiState.update { it.copy(isLoadingPrice = false, price = price.formattedPrice) }
            is PriceResult.Unavailable ->
                mutableUiState.update {
                    it.copy(isLoadingPrice = false, price = null, message = StoreMessage.of(price.problem))
                }
        }
    }

    private suspend fun loadRecord() {
        val lines = matchRepository.observeStatLines(FilterState.NONE).first()
        val stats = BasicStats.from(lines)
        val streak = streakEngine.compute(lines.map { it.date })
        mutableUiState.update {
            it.copy(wins = stats.overall.wins, losses = stats.overall.losses, streakWeeks = streak.current)
        }
    }

    companion object {
        fun restoreMessage(outcome: RestoreOutcome): StoreMessage =
            when (outcome) {
                RestoreOutcome.Restored -> StoreMessage.RESTORED
                RestoreOutcome.Pending -> StoreMessage.PENDING
                RestoreOutcome.NothingToRestore -> StoreMessage.NOTHING_TO_RESTORE
                is RestoreOutcome.CouldNotCheck -> StoreMessage.of(outcome.problem)
            }

        fun factory(dependencies: PicklejotDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    PaywallViewModel(
                        matchRepository = dependencies.matchRepository,
                        streakEngine = StreakEngine(dependencies.clock, dependencies::currentTimeZone),
                        proStore = dependencies.proStore,
                        entitlementRepository = dependencies.entitlementRepository,
                    )
                }
            }
    }
}
