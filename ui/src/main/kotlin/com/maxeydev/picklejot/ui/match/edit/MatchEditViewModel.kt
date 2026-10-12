@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.maxeydev.picklejot.ui.match.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.entitlement.CanAddMatch
import com.maxeydev.picklejot.domain.entitlement.CanAddPhoto
import com.maxeydev.picklejot.domain.match.FreeTextField
import com.maxeydev.picklejot.domain.match.LastUsedFormatStore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.recentFreeText
import com.maxeydev.picklejot.domain.match.suggestFreeText
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.person.PersonRepository
import com.maxeydev.picklejot.domain.person.normalizePersonName
import com.maxeydev.picklejot.domain.person.suggestPeople
import com.maxeydev.picklejot.domain.photo.PhotoImportQueue
import com.maxeydev.picklejot.domain.photo.PhotoImportState
import com.maxeydev.picklejot.domain.photo.PhotoSource
import com.maxeydev.picklejot.ui.PicklejotDependencies
import com.maxeydev.picklejot.ui.common.SuggestionUiState
import com.maxeydev.picklejot.ui.navigation.LOG_ANOTHER_FROM_ARGUMENT
import com.maxeydev.picklejot.ui.navigation.MATCH_ID_ARGUMENT
import com.maxeydev.picklejot.ui.paywall.UpgradeReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val DRAFT_KEY = "match_edit_draft"

private const val SAVE_AFTER_UNLOCK_KEY = "match_edit_save_after_unlock"

private val draftJson = Json { ignoreUnknownKeys = true }

const val SUGGESTION_DEBOUNCE_MILLIS = 150L

private data class SuggestionInput(
    val target: SuggestionTarget,
    val text: String,
    val excludedIds: Set<String>,
    val isDismissed: Boolean,
)

private data class SuggestionResult(
    val target: SuggestionTarget,
    val people: List<Person> = emptyList(),
    val values: List<String> = emptyList(),
)

class MatchEditViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val personRepository: PersonRepository,
    private val lastUsedFormatStore: LastUsedFormatStore,
    private val photoImportQueue: PhotoImportQueue,
    private val canAddMatch: CanAddMatch,
    private val canAddPhoto: CanAddPhoto,
    private val photoFile: (String) -> File,
    private val clock: Clock,
    private val timeZone: () -> TimeZone,
    private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val restoredDraft: MatchDraft? =
        savedStateHandle.get<String>(DRAFT_KEY)?.let { encoded ->
            draftJson.decodeFromString(MatchDraft.serializer(), encoded)
        }
    private var draft: MatchDraft? = restoredDraft ?: readyNewMatchDraft()
    private var isSaving = false
    private var isFinished = false
    private var savedNewMatchId: String? = null
    private var didSave = false
    private val suggestionInput = MutableStateFlow<SuggestionInput?>(null)
    private var suggestionResult: SuggestionResult? = null
    private var recentLocations: List<String> = emptyList()
    private var hasPhotoError = false
    private var showResultError = false
    private var resultErrorAttempts = 0
    private var upgradePrompt: UpgradeReason? = null
    private var isPaywallRequested = false
    private val photoWatchers = mutableMapOf<String, Job>()

    private val mutableUiState = MutableStateFlow(renderState())
    val uiState: StateFlow<MatchEditUiState> = mutableUiState.asStateFlow()

    init {
        val ready = draft
        when {
            restoredDraft != null -> resumePendingPhotos()
            ready != null -> publish(ready)
            else -> viewModelScope.launch { loadInitialDraft() }
        }
        viewModelScope.launch {
            val waitingForUnlock = savedStateHandle.getStateFlow(SAVE_AFTER_UNLOCK_KEY, false)
            combine(waitingForUnlock, canAddMatch.observe()) { waiting, allowed -> waiting && allowed }
                .filter { it }
                .collect {
                    savedStateHandle[SAVE_AFTER_UNLOCK_KEY] = false
                    save()
                }
        }
        viewModelScope.launch {
            matchRepository
                .observePriorValues(FreeTextField.LOCATION)
                .map { usages -> recentFreeText(usages) }
                .flowOn(defaultDispatcher)
                .collect { values ->
                    recentLocations = values
                    mutableUiState.value = renderState()
                }
        }
        viewModelScope.launch {
            observeSuggestions().collect { result ->
                suggestionResult = result
                mutableUiState.value = renderState()
            }
        }
    }

    fun selectFormat(format: MatchFormat) = updateDraft { it.withFormat(format) }

    fun selectResult(result: MatchResult) {
        showResultError = false
        updateDraft { it.copy(result = result) }
    }

    fun selectDate(date: AppDate) = updateDraft { it.copy(date = date.toString()) }

    fun changeStartTime(time: AppTime?) = updateDraft { it.copy(startTime = time?.toString()) }

    fun changeEndTime(time: AppTime?) = updateDraft { it.copy(endTime = time?.toString()) }

    fun changePersonName(
        slot: PersonSlot,
        name: String,
    ) = updateDraft { it.withPersonName(slot, name) }

    fun focusSuggestionTarget(target: SuggestionTarget) {
        val current = draft ?: return
        suggestionInput.value = suggestionInputFor(target, current, isDismissed = false)
        mutableUiState.value = renderState()
    }

    fun leaveSuggestionTarget(target: SuggestionTarget) {
        if (suggestionInput.value?.target != target) {
            return
        }
        suggestionInput.value = null
        mutableUiState.value = renderState()
    }

    fun selectSuggestion(
        target: SuggestionTarget,
        suggestion: SuggestionUiState,
    ) {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        val slot = target.personSlot
        val next =
            if (slot != null) {
                val person =
                    suggestionResult
                        ?.takeIf { it.target == target }
                        ?.people
                        ?.firstOrNull { it.id.toString() == suggestion.key }
                        ?: return
                current.withPersonSelected(slot, person)
            } else {
                when (requireNotNull(target.freeTextField)) {
                    FreeTextField.LOCATION -> current.copy(location = suggestion.label)
                    FreeTextField.PADDLE -> current.copy(paddle = suggestion.label)
                }
            }
        suggestionInput.value = suggestionInputFor(target, next, isDismissed = true)
        publish(next)
    }

    fun addGame() = updateDraft { it.withGameAdded() }

    fun removeGame(index: Int) = updateDraft { it.withGameRemoved(index) }

    fun changeGameScores(
        index: Int,
        myScore: String,
        opponentScore: String,
    ) = updateDraft { it.withGameScores(index, myScore, opponentScore) }

    fun changeLocation(location: String) = updateDraft { it.copy(location = location) }

    fun changePaddle(paddle: String) = updateDraft { it.copy(paddle = paddle) }

    fun changeNotes(notes: String) = updateDraft { it.copy(notes = notes) }

    fun addPickedPhotos(uris: List<String>) {
        addPhotos(uris.map { PhotoSource(uri = it, isTemporaryCapture = false) })
    }

    fun addCapturedPhoto(uri: String) {
        addPhotos(listOf(PhotoSource(uri = uri, isTemporaryCapture = true)))
    }

    fun removePhoto(key: String) {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        val photo = current.photos.firstOrNull { it.key == key } ?: return
        photoWatchers.remove(key)?.cancel()
        if (!photo.isPersisted) {
            photoImportQueue.discard(key)
        }
        publish(current.withPhotoRemoved(key))
    }

    fun movePhoto(
        key: String,
        offset: Int,
    ) = updateDraft { it.withPhotoMoved(key, offset) }

    fun dismissPhotoError() {
        hasPhotoError = false
        mutableUiState.value = renderState()
    }

    fun dismissUpgradePrompt() {
        upgradePrompt = null
        mutableUiState.value = renderState()
    }

    fun paywallOpened() {
        isPaywallRequested = false
        mutableUiState.value = renderState()
    }

    fun save() {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        if (current.result == null && current.canSaveIgnoringResult()) {
            showResultError = true
            resultErrorAttempts += 1
            mutableUiState.value = renderState()
            return
        }
        if (!current.canSave()) {
            return
        }
        isSaving = true
        mutableUiState.value = renderState()
        viewModelScope.launch {
            if (current.isNew && !canAddMatch()) {
                isSaving = false
                isPaywallRequested = true
                savedStateHandle[SAVE_AFTER_UNLOCK_KEY] = true
                mutableUiState.value = renderState()
                return@launch
            }
            val matchId = Uuid.parse(current.matchId)
            matchRepository.saveMatch(
                buildMatch(current),
                removedPhotoIds = current.removedPhotoIds.map(Uuid::parse).toSet(),
            )
            current.photos
                .filter { it.isReady && !it.isPersisted }
                .forEach { photoImportQueue.release(it.key) }
            photoImportQueue.attachWhenReady(matchId, current.pendingPhotoKeys())
            if (current.isNew) {
                lastUsedFormatStore.recordLastUsedFormat(current.format)
            }
            isSaving = false
            isFinished = true
            didSave = true
            if (current.isNew) {
                savedNewMatchId = current.matchId
            }
            mutableUiState.value = renderState()
        }
    }

    private suspend fun loadInitialDraft() {
        val editingId = savedStateHandle.get<String>(MATCH_ID_ARGUMENT)
        if (editingId == null) {
            publish(newMatchDraft())
            return
        }
        val existing = matchRepository.observeById(Uuid.parse(editingId)).first()
        if (existing == null) {
            isFinished = true
            mutableUiState.value = renderState()
        } else {
            publish(MatchDraft.fromMatch(existing))
        }
    }

    private fun readyNewMatchDraft(): MatchDraft? {
        val isPlainNewMatch =
            savedStateHandle.get<String>(MATCH_ID_ARGUMENT) == null &&
                savedStateHandle.get<String>(LOG_ANOTHER_FROM_ARGUMENT) == null
        if (!isPlainNewMatch) {
            return null
        }
        return lastUsedFormatStore.cachedLastUsedFormat()?.let(::freshDraft)
    }

    private fun freshDraft(format: MatchFormat): MatchDraft {
        val now = clock.now().toLocalDateTime(timeZone())
        return MatchDraft.forNewMatch(
            matchId = Uuid.random().toString(),
            format = format,
            date = now.date,
            startTime = AppTime(now.hour, now.minute),
        )
    }

    private suspend fun newMatchDraft(): MatchDraft {
        val source =
            savedStateHandle.get<String>(LOG_ANOTHER_FROM_ARGUMENT)?.let { sourceId ->
                matchRepository.observeById(Uuid.parse(sourceId)).first()
            }
        if (source == null) {
            return freshDraft(lastUsedFormatStore.lastUsedFormat())
        }
        val now = clock.now().toLocalDateTime(timeZone())
        return logAnotherDraft(
            source = source,
            matchId = Uuid.random().toString(),
            startTime = AppTime(now.hour, now.minute),
        )
    }

    private fun addPhotos(sources: List<PhotoSource>) {
        if (sources.isEmpty()) {
            return
        }
        viewModelScope.launch {
            val current = draft ?: return@launch
            if (isSaving || isFinished) {
                return@launch
            }
            val remaining = canAddPhoto.remainingFor(current.photos.size)
            val accepted = sources.take(remaining)
            if (accepted.size < sources.size) {
                upgradePrompt =
                    if (canAddPhoto.hasPro()) {
                        UpgradeReason.PRO_PHOTO_LIMIT
                    } else {
                        UpgradeReason.PHOTO_LIMIT
                    }
            }
            if (accepted.isEmpty()) {
                mutableUiState.value = renderState()
                return@launch
            }
            val added = accepted.map(PhotoDraft::importing)
            publish(current.withPhotosAdded(added))
            added.forEach(::startImport)
        }
    }

    private fun resumePendingPhotos() {
        draft
            ?.photos
            ?.filter { !it.isReady && !it.isPersisted }
            ?.forEach(::startImport)
    }

    private fun startImport(photo: PhotoDraft) {
        val source = photo.source ?: return
        photoImportQueue.ensureStarted(photo.key, source)
        photoWatchers[photo.key] =
            viewModelScope.launch {
                val finished = photoImportQueue.observe(photo.key).first { it !is PhotoImportState.Importing }
                photoWatchers.remove(photo.key)
                onImportFinished(photo.key, finished)
            }
    }

    private fun onImportFinished(
        key: String,
        finished: PhotoImportState,
    ) {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        when (finished) {
            is PhotoImportState.Ready -> publish(current.withPhotoImported(key, finished.photo))
            else -> {
                photoImportQueue.discard(key)
                hasPhotoError = true
                publish(current.withPhotoRemoved(key))
            }
        }
    }

    override fun onCleared() {
        if (!isFinished) {
            draft
                ?.photos
                ?.filterNot { it.isPersisted }
                ?.forEach { photoImportQueue.discard(it.key) }
        }
        super.onCleared()
    }

    private fun updateDraft(transform: (MatchDraft) -> MatchDraft) {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        publish(transform(current))
    }

    private fun publish(next: MatchDraft) {
        draft = next
        savedStateHandle[DRAFT_KEY] = draftJson.encodeToString(MatchDraft.serializer(), next)
        suggestionInput.value?.let { input ->
            val refreshed = suggestionInputFor(input.target, next, isDismissed = false)
            suggestionInput.value = refreshed.copy(isDismissed = input.isDismissed && input.text == refreshed.text)
        }
        mutableUiState.value = renderState()
    }

    private fun renderState(): MatchEditUiState =
        draft?.toUiState(
            isSaving = isSaving,
            isFinished = isFinished,
            suggestionTarget = suggestionInput.value?.target,
            suggestions = visibleSuggestions(),
            recentLocations = recentLocations,
            savedNewMatchId = savedNewMatchId,
            didSave = didSave,
            photoFilePath = { photoFile(it).path },
            hasPhotoError = hasPhotoError,
            showResultError = showResultError,
            resultErrorAttempts = resultErrorAttempts,
            upgradePrompt = upgradePrompt,
            isPaywallRequested = isPaywallRequested,
        ) ?: MatchEditUiState(isLoading = !isFinished, isFinished = isFinished)

    private fun visibleSuggestions(): List<SuggestionUiState> {
        val input = suggestionInput.value ?: return emptyList()
        if (input.isDismissed || input.text.isBlank() || isSaving || isFinished) {
            return emptyList()
        }
        val result = suggestionResult?.takeIf { it.target == input.target } ?: return emptyList()
        return result.people.map { SuggestionUiState(key = it.id.toString(), label = it.displayName) } +
            result.values.map { SuggestionUiState(key = it, label = it) }
    }

    private fun suggestionInputFor(
        target: SuggestionTarget,
        source: MatchDraft,
        isDismissed: Boolean,
    ): SuggestionInput {
        val slot = target.personSlot
        return if (slot != null) {
            SuggestionInput(
                target = target,
                text = source.nameIn(slot),
                excludedIds = source.boundIdsOutside(slot) + listOfNotNull(source.idIn(slot)),
                isDismissed = isDismissed,
            )
        } else {
            val text =
                when (requireNotNull(target.freeTextField)) {
                    FreeTextField.LOCATION -> source.location
                    FreeTextField.PADDLE -> source.paddle
                }
            SuggestionInput(target = target, text = text, excludedIds = emptySet(), isDismissed = isDismissed)
        }
    }

    private fun observeSuggestions(): Flow<SuggestionResult?> =
        suggestionInput
            .map { it?.target }
            .distinctUntilChanged()
            .flatMapLatest { target ->
                if (target == null) {
                    flowOf(null)
                } else {
                    suggestionsFor(target)
                }
            }.flowOn(defaultDispatcher)

    private fun suggestionsFor(target: SuggestionTarget): Flow<SuggestionResult> {
        val typed =
            suggestionInput
                .filterNotNull()
                .filter { it.target == target }
                .debounce(SUGGESTION_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
        val field = target.freeTextField
        return if (field == null) {
            combine(personRepository.observeRecentlyUsed(), typed) { usage, input ->
                SuggestionResult(
                    target = target,
                    people =
                        if (input.isDismissed) {
                            emptyList()
                        } else {
                            suggestPeople(input.text, usage, input.excludedIds.map(Uuid::parse).toSet())
                        },
                )
            }
        } else {
            combine(matchRepository.observePriorValues(field), typed) { values, input ->
                SuggestionResult(
                    target = target,
                    values =
                        if (input.isDismissed) {
                            emptyList()
                        } else {
                            suggestFreeText(input.text, values)
                        },
                )
            }
        }
    }

    private suspend fun buildMatch(source: MatchDraft): Match {
        val id = Uuid.parse(source.matchId)
        val now = clock.now()
        val existing =
            if (source.isNew) {
                null
            } else {
                requireNotNull(matchRepository.observeById(id).first()) {
                    "The match being edited no longer exists."
                }
            }
        return Match(
            id = id,
            format = source.format,
            date = AppDate.parse(source.date),
            result = requireNotNull(source.result) { "A match cannot be saved without a result." },
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            startTime = source.startTime?.let(AppTime::parse),
            endTime = source.endTime?.let(AppTime::parse),
            location = source.location.trim().ifEmpty { null },
            opponents =
                source
                    .visibleSlots()
                    .filter { it != PersonSlot.PARTNER }
                    .mapNotNull { resolvePerson(source, it) },
            partner =
                if (PersonSlot.PARTNER in source.visibleSlots()) {
                    resolvePerson(source, PersonSlot.PARTNER)
                } else {
                    null
                },
            games = source.completeGames(),
            paddle = source.paddle.trim().ifEmpty { null },
            notes = source.notes.trim().ifEmpty { null },
            photos = source.readyPhotoRefs(),
        )
    }

    private suspend fun resolvePerson(
        source: MatchDraft,
        slot: PersonSlot,
    ): Person? {
        val name = source.nameIn(slot)
        if (normalizePersonName(name).isEmpty()) {
            return null
        }
        val bound = source.idIn(slot)?.let { personRepository.findById(Uuid.parse(it)) }
        return bound ?: personRepository.findOrCreatePerson(name)
    }

    companion object {
        fun factory(dependencies: PicklejotDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MatchEditViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        matchRepository = dependencies.matchRepository,
                        personRepository = dependencies.personRepository,
                        lastUsedFormatStore = dependencies.lastUsedFormatStore,
                        photoImportQueue = dependencies.photoImportQueue,
                        canAddMatch = CanAddMatch(dependencies.matchRepository, dependencies.entitlementRepository),
                        canAddPhoto = CanAddPhoto(dependencies.entitlementRepository),
                        photoFile = dependencies::photoFile,
                        clock = dependencies.clock,
                        timeZone = dependencies::currentTimeZone,
                        defaultDispatcher = dependencies.defaultDispatcher,
                    )
                }
            }
    }
}
