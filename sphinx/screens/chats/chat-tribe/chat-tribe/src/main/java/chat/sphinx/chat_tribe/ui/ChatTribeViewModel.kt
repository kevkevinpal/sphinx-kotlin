package chat.sphinx.chat_tribe.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.chat_common.ui.ChatSideEffect
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.route.isRouteAvailable
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.*
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import chat.sphinx.podcast_player.objects.toPodcast
import chat.sphinx.resources.getRandomColor
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val ChatTribeFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class ChatTribeViewModel @Inject constructor(
    app: Application,
    dispatchers: CoroutineDispatchers,
    memeServerTokenHandler: MemeServerTokenHandler,
    chatNavigator: TribeChatNavigator,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    networkQueryLightning: NetworkQueryLightning,
    mediaCacheHandler: MediaCacheHandler,
    savedStateHandle: SavedStateHandle,
    cameraViewModelCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    LOG: SphinxLogger,
    private val mediaPlayerServiceController: MediaPlayerServiceController
): ChatViewModel<ChatTribeFragmentArgs>(
    app,
    dispatchers,
    memeServerTokenHandler,
    chatNavigator,
    chatRepository,
    contactRepository,
    messageRepository,
    networkQueryLightning,
    mediaCacheHandler,
    savedStateHandle,
    cameraViewModelCoordinator,
    LOG,
), MediaPlayerServiceController.MediaServiceListener
{
    override val args: ChatTribeFragmentArgs by savedStateHandle.navArgs()
    override val chatId: ChatId = args.chatId
    override val contactId: ContactId?
        get() = null

    val podcastViewStateContainer: ViewStateContainer<PodcastViewState> by lazy {
        ViewStateContainer(PodcastViewState.Idle)
    }

    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    var podcast: Podcast? = null

    override val headerInitialHolderSharedFlow: SharedFlow<InitialHolderViewState> = flow {
        chatSharedFlow.collect { chat ->
            chat?.photoUrl?.let {
                emit(
                    InitialHolderViewState.Url(it)
                )
            } ?: chat?.name?.let {
                emit(
                    InitialHolderViewState.Initials(
                        it.value.getInitials(),
                        headerInitialsTextViewColor
                    )
                )
            } ?: emit(
                InitialHolderViewState.None
            )
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    override suspend fun getChatNameIfNull(): ChatName? {
        return null
    }

    override suspend fun getInitialHolderViewStateForReceivedMessage(
        message: Message
    ): InitialHolderViewState {
        return message.senderPic?.let { url ->
            InitialHolderViewState.Url(url)
        } ?: message.senderAlias?.let { alias ->
            InitialHolderViewState.Initials(alias.value.getInitials(), app.getRandomColor())
        } ?: InitialHolderViewState.None
    }

    override val checkRoute: Flow<LoadResponse<Boolean, ResponseError>> = flow {
        networkQueryLightning.checkRoute(chatId).collect { response ->
            @Exhaustive
            when (response) {
                is LoadResponse.Loading -> {
                    emit(response)
                }
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emit(Response.Success(response.value.isRouteAvailable))
                }
            }
        }
    }

    override fun readMessages() {
        viewModelScope.launch(mainImmediate) {
            messageRepository.readMessages(chatId)
        }
    }

    override fun sendMessage(builder: SendMessage.Builder): SendMessage? {
        builder.setChatId(chatId)
        return super.sendMessage(builder)
    }

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId != chatId) {
                return
            }
        }

        podcast?.let { podcast ->
            @Exhaustive
            when (serviceState) {
                is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                    podcast.playingEpisodeUpdate(serviceState.episodeId, serviceState.currentTime)
                    podcastViewStateContainer.updateViewState(PodcastViewState.MediaStateUpdate(podcast))
                }
                is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                    podcast.pauseEpisodeUpdate()
                    podcastViewStateContainer.updateViewState(PodcastViewState.MediaStateUpdate(podcast))
                }
                is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                    podcast.endEpisodeUpdate(serviceState.episodeId)
                    podcastViewStateContainer.updateViewState(PodcastViewState.MediaStateUpdate(podcast))
                }
                is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                    podcastViewStateContainer.updateViewState(PodcastViewState.ServiceLoading)
                }
                is MediaPlayerServiceState.ServiceInactive -> {
                    podcast.pauseEpisodeUpdate()
                    podcastViewStateContainer.updateViewState(PodcastViewState.ServiceInactive)
                }
            }
        }
    }

    init {
        mediaPlayerServiceController.addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerServiceController.removeListener(this)
    }

    suspend fun loadTribeAndPodcastData(): Podcast? {
        chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
            chatRepository.updateTribeInfo(chat)?.let { podcastDto ->
                podcast = podcastDto.toPodcast()

                chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
//                    val pricePerMessage = chat.pricePerMessage?.value ?: 0
//                    val escrowAmount = chat.escrowAmount?.value ?: 0
//
//                    submitSideEffect(
//                        ChatSideEffect.Notify(
//                            "Price per message: $pricePerMessage\n Amount to Stake: $escrowAmount"
//                        )
//                    )

                    chat.metaData?.let { metaData ->
                        podcast?.setMetaData(metaData)
                    }
                }
            }
        }

        return podcast
    }

    fun goToPodcastPlayerScreen() {
        podcast?.let { podcast ->
            viewModelScope.launch(mainImmediate) {
                (chatNavigator as TribeChatNavigator).toPodcastPlayerScreen(chatId, podcast)
            }
        }
    }

    fun playPausePodcast() {
        podcast?.let { podcast ->
            podcast.getCurrentEpisode()?.let { currentEpisode ->
                if (currentEpisode.playing) {
                    pauseEpisode(currentEpisode)
                } else {
                    playEpisode(currentEpisode, podcast.currentTime)
                }
            }
        }
    }

    private fun playEpisode(episode: PodcastEpisode, startTime: Int) {
        viewModelScope.launch(mainImmediate) {
            podcast?.let { podcast ->
                withContext(io) {
                    podcast.didStartPlayingEpisode(episode, startTime)
                }

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Play(
                        chatId,
                        episode.id,
                        episode.enclosureUrl,
                        Sat(0),
                        podcast.speed,
                        startTime,
                    )
                )
            }
        }
    }

    private fun pauseEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            podcast?.let { podcast ->
                podcast.didPausePlayingEpisode(episode)

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Pause(chatId, episode.id)
                )
            }
        }
    }

    fun seekTo(time: Int) {
        viewModelScope.launch(mainImmediate) {
            podcast?.let { podcast ->
                podcast.didSeekTo(podcast.currentTime + time)

                val metaData = podcast.getMetaData()

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Seek(chatId, metaData)
                )
            }
        }
    }


    fun exitTribeGetUserConfirmation() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(
                ChatSideEffect.AlertConfirmExitTribe {
                    exitTribeUserConfirmed()
                }
            )
        }
    }

    private var exitTribeJob: Job? = null
    private fun exitTribeUserConfirmed() {
        if (exitTribeJob?.isActive == true) {
            return
        }

        exitTribeJob = viewModelScope.launch(mainImmediate) {

            try {
                chatSharedFlow.collect { chat ->
                    if (chat != null) {

                        val response = chatRepository.exitTribe(chat)

                        @Exhaustive
                        when (response) {
                            is Response.Error -> {

                                submitSideEffect(
                                    ChatSideEffect.Notify(
                                        if (response.exception == null) {
                                            response.message
                                        } else {
                                            "Failed to leave tribe"
                                        }
                                    )
                                )

                                LOG.d(TAG, "Failed to leave tribe")
                                LOG.e(TAG, response.message, response.exception)
                            }
                            is Response.Success -> {
                                chatNavigator.popBackStack()
                            }
                        }

                        throw Exception()
                    }
                }
            } catch (e: Exception) {}

        }
    }
}
