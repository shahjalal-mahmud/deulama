package com.appriyo.deulama.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.HealthApi
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.repository.AuthRepository
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.domain.repository.DramaSort
import com.appriyo.deulama.domain.repository.SortOrder
import com.appriyo.deulama.presentation.components.ConnectionStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One curated genre shelf on the homepage. [genreQuery] is what we send
 *  to the server; [labelKo] / [labelEn] are just display strings. */
data class GenreSection(
    val key: String,
    val labelKo: String,
    val labelEn: String,
    val genreQuery: String,
    val items: List<Drama> = emptyList(),
    val loading: Boolean = true,
)

private val GENRE_SECTIONS_SEED = listOf(
    GenreSection("romcom", "로맨스 코미디", "Rom-Com", "Romance"),
    GenreSection("historical", "사극", "Historical", "Historical"),
    GenreSection("thriller", "스릴러", "Thriller", "Thriller"),
    GenreSection("fantasy", "판타지", "Fantasy", "Fantasy"),
    GenreSection("action", "액션", "Action", "Action"),
    GenreSection("horror", "공포", "Horror", "Horror"),
    GenreSection("comedy", "코미디", "Comedy", "Comedy"),
)

/** The pills shown above Trending Now. */
val HOME_GENRE_FILTERS = listOf("Romance", "Historical", "Thriller", "Fantasy", "Action", "Horror", "Comedy")

data class HomeUiState(
    val status: ConnectionStatus = ConnectionStatus.LOADING,
    val message: String = "Checking backend connection…",
    val user: User? = null,

    val spotlight: List<Drama> = emptyList(),
    val spotlightLoading: Boolean = true,

    val selectedGenre: String? = null,
    val trending: List<Drama> = emptyList(),
    val trendingLoading: Boolean = true,

    // TODO: once liked/disliked/watched are tracked locally, swap this
    // for a real personalized call. Until then it's a top-rated shelf
    // so the section isn't empty on a fresh install — same graceful
    // fallback the web does via `recommendationSubtitle`.
    val recommendations: List<Drama> = emptyList(),
    val recommendationsLoading: Boolean = true,

    val genreSections: List<GenreSection> = GENRE_SECTIONS_SEED,

    val allDramaPreview: List<Drama> = emptyList(),
    val allDramaLoading: Boolean = true,
)

class HomeViewModel(
    private val healthApi: HealthApi,
    private val authRepository: AuthRepository,
    private val dramaRepository: DramaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionFlow
                .map { it?.user }
                .distinctUntilChanged()
                .collect { user -> _uiState.update { it.copy(user = user) } }
        }
        loadHome()
    }

    /** Fires all homepage shelves in parallel — mirrors the web's single
     *  DramaContext fetch, just split across independent calls so one
     *  slow/failing shelf doesn't block the rest of the page. */
    fun loadHome() {
        loadSpotlight()
        loadTrending(genre = _uiState.value.selectedGenre)
        loadRecommendations()
        loadGenreSections()
        loadAllDramaPreview()
    }

    fun onSelectGenre(genre: String?) {
        _uiState.update { it.copy(selectedGenre = genre) }
        loadTrending(genre)
    }

    fun retry() = loadHome()

    private fun loadSpotlight() {
        _uiState.update { it.copy(spotlightLoading = true) }
        viewModelScope.launch {
            val result = dramaRepository.listDramas(limit = 10, sort = DramaSort.IMDB_RATING, order = SortOrder.DESC)
            _uiState.update {
                it.copy(
                    spotlight = (result as? ApiResult.Success)?.value.orEmpty(),
                    spotlightLoading = false,
                )
            }
        }
    }

    private fun loadTrending(genre: String?) {
        _uiState.update { it.copy(trendingLoading = true) }
        viewModelScope.launch {
            val result = dramaRepository.listDramas(limit = 10, sort = DramaSort.IMDB_RATING, order = SortOrder.DESC, genre = genre)
            _uiState.update {
                it.copy(
                    trending = (result as? ApiResult.Success)?.value.orEmpty(),
                    trendingLoading = false,
                )
            }
        }
    }

    private fun loadRecommendations() {
        _uiState.update { it.copy(recommendationsLoading = true) }
        viewModelScope.launch {
            val result = dramaRepository.listDramas(limit = 4, sort = DramaSort.IMDB_RATING, order = SortOrder.DESC)
            _uiState.update {
                it.copy(
                    recommendations = (result as? ApiResult.Success)?.value.orEmpty(),
                    recommendationsLoading = false,
                )
            }
        }
    }

    private fun loadGenreSections() {
        viewModelScope.launch {
            val filled = coroutineScope {
                GENRE_SECTIONS_SEED.map { section ->
                    async {
                        val result = dramaRepository.listDramas(
                            limit = 12,
                            sort = DramaSort.IMDB_RATING,
                            order = SortOrder.DESC,
                            genre = section.genreQuery,
                        )
                        section.copy(
                            items = (result as? ApiResult.Success)?.value.orEmpty(),
                            loading = false,
                        )
                    }
                }.awaitAll()
            }
            _uiState.update { it.copy(genreSections = filled) }
        }
    }

    private fun loadAllDramaPreview() {
        _uiState.update { it.copy(allDramaLoading = true) }
        viewModelScope.launch {
            val result = dramaRepository.listDramas(limit = 10, sort = DramaSort.CREATED_AT, order = SortOrder.DESC)
            _uiState.update {
                it.copy(
                    allDramaPreview = (result as? ApiResult.Success)?.value.orEmpty(),
                    allDramaLoading = false,
                )
            }
        }
    }
}