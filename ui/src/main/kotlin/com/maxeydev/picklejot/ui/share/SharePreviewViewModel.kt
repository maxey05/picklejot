@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.share

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.photo.primaryPhoto
import com.maxeydev.picklejot.domain.profile.ProfileRepository
import com.maxeydev.picklejot.domain.share.CardDetail
import com.maxeydev.picklejot.domain.share.CardFormat
import com.maxeydev.picklejot.domain.share.CardFormatStore
import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme
import com.maxeydev.picklejot.ui.PicklejotDependencies
import com.maxeydev.picklejot.ui.navigation.MATCH_ID_ARGUMENT
import com.maxeydev.picklejot.ui.paywall.UpgradeReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val HIDDEN_DETAILS_KEY = "share_hidden_details"

class SharePreviewViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val profileRepository: ProfileRepository,
    private val formatStore: CardFormatStore,
    private val photoFile: (String) -> File,
    private val renderer: CardRendering,
    private val labels: CardLabels,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val matchId: Uuid =
        Uuid.parse(
            requireNotNull(savedStateHandle.get<String>(MATCH_ID_ARGUMENT)) {
                "The share screen was opened without a match id."
            },
        )

    private val mutableUiState = MutableStateFlow(SharePreviewUiState(hiddenDetails = restoreHiddenDetails()))
    val uiState: StateFlow<SharePreviewUiState> = mutableUiState.asStateFlow()

    private var renderJob: Job? = null

    init {
        viewModelScope.launch {
            render(formatStore.observeFormat().first())
        }
    }

    private fun restoreHiddenDetails(): Set<CardDetail> {
        val names = savedStateHandle.get<ArrayList<String>>(HIDDEN_DETAILS_KEY).orEmpty()
        return CardDetail.entries.filter { it.name in names }.toSet()
    }

    fun reportShareFailed() {
        mutableUiState.update { it.copy(hasShareFailed = true) }
    }

    fun retry() {
        if (mutableUiState.value.isRendering) {
            return
        }
        render(mutableUiState.value.format)
    }

    fun selectRatio(ratio: CardRatio) {
        changeFormat(mutableUiState.value.format.copy(ratio = ratio))
    }

    fun selectTheme(theme: CardTheme) {
        viewModelScope.launch {
            val isPro = profileRepository.observeProfile().first().entitlement.isPro
            if (theme.requiresPro && !isPro) {
                mutableUiState.update { it.copy(upgradeReason = UpgradeReason.PRO_THEME) }
            } else {
                changeFormat(mutableUiState.value.format.copy(theme = theme))
            }
        }
    }

    fun dismissUpgrade() {
        mutableUiState.update { it.copy(upgradeReason = null) }
    }

    fun selectLayout(layout: CardLayout) {
        val override = if (layout == CardLayout.NO_PHOTO) CardLayout.NO_PHOTO else null
        changeFormat(mutableUiState.value.format.copy(layoutOverride = override))
    }

    fun setDetailShown(
        detail: CardDetail,
        isShown: Boolean,
    ) {
        val current = mutableUiState.value.hiddenDetails
        val updated = if (isShown) current - detail else current + detail
        if (updated == current) {
            return
        }
        savedStateHandle[HIDDEN_DETAILS_KEY] = ArrayList(updated.map { it.name })
        render(mutableUiState.value.format, updated)
    }

    private fun changeFormat(format: CardFormat) {
        if (format == mutableUiState.value.format) {
            return
        }
        viewModelScope.launch { formatStore.saveFormat(format) }
        render(format)
    }

    private fun render(
        format: CardFormat,
        hidden: Set<CardDetail> = mutableUiState.value.hiddenDetails,
    ) {
        renderJob?.cancel()
        mutableUiState.update {
            it.copy(
                isRendering = true,
                hasFailed = false,
                hasShareFailed = false,
                format = format,
                hiddenDetails = hidden,
            )
        }
        renderJob =
            viewModelScope.launch {
                val match = matchRepository.observeById(matchId).first()
                if (match == null) {
                    mutableUiState.update { it.copy(isRendering = false, isGone = true) }
                    return@launch
                }
                val profile = profileRepository.observeProfile().first()
                val isPro = profile.entitlement.isPro
                val shown = format.forEntitlement(isPro)
                val primary = match.photos.primaryPhoto()
                val layout = shown.layoutFor(hasPhoto = primary != null)
                val photo =
                    primary
                        ?.takeIf { layout == CardLayout.PHOTO }
                        ?.let { loadPhotoDataUri(photoFile(it.relativePath)) }
                val data =
                    buildCardData(
                        match = match,
                        displayName = profile.displayName,
                        photoDataUri = photo,
                        labels = labels,
                        format = shown,
                        hidden = hidden,
                        showWordmark = !isPro,
                    )
                val result = renderer.render(data)
                mutableUiState.update { current ->
                    when (result) {
                        is CardRenderResult.Rendered ->
                            current.copy(
                                isRendering = false,
                                format = shown,
                                isPro = isPro,
                                card = result.bitmap,
                                cardDescription = describe(data),
                                hasPhoto = primary != null,
                                availableDetails = match.cardDetails(),
                            )
                        is CardRenderResult.Failed ->
                            current.copy(
                                isRendering = false,
                                card = null,
                                hasFailed = true,
                                hasPhoto = primary != null,
                                availableDetails = match.cardDetails(),
                                format = shown,
                                isPro = isPro,
                            )
                    }
                }
            }
    }

    private suspend fun loadPhotoDataUri(file: File): String? =
        withContext(ioDispatcher) {
            try {
                "data:image/jpeg;base64," + Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            } catch (missing: IOException) {
                null
            }
        }

    private fun describe(data: CardData): String =
        listOfNotNull(
            data.displayName.takeIf { it.isNotEmpty() },
            data.meta,
            data.partner?.spoken(),
            data.time?.spoken(),
            data.opponents?.spoken(),
            data.games?.spoken(),
            data.location,
        ).joinToString(separator = ". ")

    private fun CardEntry.spoken(): String = "$caption ${values.joinToString(", ")}"

    companion object {
        fun factory(
            dependencies: PicklejotDependencies,
            labels: CardLabels,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SharePreviewViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        matchRepository = dependencies.matchRepository,
                        profileRepository = dependencies.profileRepository,
                        formatStore = dependencies.cardFormatStore,
                        photoFile = dependencies::photoFile,
                        renderer = dependencies.cardRenderer,
                        labels = labels,
                        ioDispatcher = dependencies.ioDispatcher,
                    )
                }
            }
    }
}
