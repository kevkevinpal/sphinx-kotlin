package chat.sphinx.chat_tribe.ui

import chat.sphinx.podcast_player.objects.Podcast
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PodcastViewState: ViewState<PodcastViewState>() {
    object Idle: PodcastViewState()

    object ServiceLoading: PodcastViewState()
    
    object ServiceInactive: PodcastViewState()

    class MediaStateUpdate(
        val podcast: Podcast,
    ): PodcastViewState()

}
