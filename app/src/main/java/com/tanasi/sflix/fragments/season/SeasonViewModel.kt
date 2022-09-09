package com.tanasi.sflix.fragments.season

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Episode
import com.tanasi.sflix.models.Server
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class SeasonViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.LoadingEpisodes)
    val state: LiveData<State> = _state

    sealed class State {
        object LoadingEpisodes : State()
        data class SuccessLoadingEpisodes(
            val seasonId: String,
            val episodes: List<Episode>,
        ) : State()
        data class FailedLoadingEpisodes(val error: Exception) : State()
    }


    fun getSeasonEpisodes(seasonId: String) = viewModelScope.launch {
        _state.value = State.LoadingEpisodes

        _state.value = try {
            State.SuccessLoadingEpisodes(
                seasonId = seasonId,
                episodes = sflixService.fetchSeasonEpisode(seasonId)
                    .select("div.flw-item.film_single-item.episode-item.eps-item")
                    .mapIndexed { episodeNumber, episodeElement ->
                        val episodeId = episodeElement.attr("data-id")
                        Episode(
                            id = episodeId,
                            number = episodeElement
                                .selectFirst("div.episode-number")
                                ?.text()
                                ?.substringAfter("Episode ")
                                ?.substringBefore(":")
                                ?.toIntOrNull()
                                ?: episodeNumber,
                            title = episodeElement
                                .selectFirst("h3.film-name")
                                ?.text()
                                ?: "",
                            poster = episodeElement.selectFirst("img")
                                ?.attr("src") ?: "",

                            servers = sflixService.fetchEpisodeServers(episodeId)
                                .select("a")
                                .map {
                                    Server(
                                        id = it.attr("data-id"),
                                        name = it.selectFirst("span")?.text()
                                            ?.trim() ?: "",
                                    )
                                },
                        )
                    }
            )
        } catch (e: Exception) {
            State.FailedLoadingEpisodes(e)
        }
    }
}