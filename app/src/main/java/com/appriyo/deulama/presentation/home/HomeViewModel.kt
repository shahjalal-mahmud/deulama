package com.appriyo.deulama.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.HealthApi
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.repository.AuthRepository
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.domain.repository.DramaSort
import com.appriyo.deulama.domain.repository.SortOrder
import com.appriyo.deulama.presentation.components.ConnectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One curated genre shelf on the homepage. [genreQuery] is matched
 *  case-insensitively against each drama's `genres` list client-side
 *  (see [HomeViewModel.loadGenreSections]); [labelKo] / [labelEn]
 *  are just display strings. */
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
)

class HomeViewModel(
    private val healthApi: HealthApi,
    private val authRepository: AuthRepository,
    private val dramaRepository: DramaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Paged, newest-first catalog used by the home screen's "All Dramas"
     * section. Driven by [DramaRepository.pagedCatalog] (which talks to
     * `GET /api/dramas` via `DramaPagingSource`) and `cachedIn`-ed so the
     * scroll position survives configuration changes and tab switches.
     *
     * Lives on the VM (not in [HomeUiState]) because Paging 3 owns its
     * own snapshot / load-state machine — trying to mirror it into a
     * generic UiState would either drop scroll fidelity or race the
     * paging internals.
     */
    val allDrama: Flow<PagingData<Drama>> = dramaRepository
        .pagedCatalog(sort = DramaSort.CREATED_AT, order = SortOrder.DESC)
        .cachedIn(viewModelScope)

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
        // The paged "All Dramas" stream is started by [allDrama]'s
        // collector in the screen — nothing to do here.
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
            // The backend's `/api/dramas` endpoint doesn't honour a
            // `genre` query param (see docs/API.md § "GET /api/dramas"
            // — only `page`, `limit`, `sort`, `order` are accepted),
            // so we fetch the full top-rated catalog and filter by the
            // drama's local `genres` list. Cheap for small catalogs;
            // move filtering server-side once this list grows.
            val result = dramaRepository.listDramas(
                limit = TRENDING_FETCH_LIMIT,
                sort = DramaSort.IMDB_RATING,
                order = SortOrder.DESC,
            )
            val all = (result as? ApiResult.Success)?.value.orEmpty()
            val filtered = if (genre.isNullOrBlank()) {
                all.take(TRENDING_SHELF_SIZE)
            } else {
                all.filter { drama -> drama.genres.any { it.equals(genre, ignoreCase = true) } }
                    .take(TRENDING_SHELF_SIZE)
            }
            _uiState.update {
                it.copy(
                    trending = filtered,
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
            // The backend's `/api/dramas` endpoint doesn't accept a
            // `genre` query param (see docs/API.md § "GET /api/dramas"
            // — only `page`, `limit`, `sort`, `order` are whitelisted),
            // so we fetch the full top-rated catalog ONCE and split it
            // into per-genre shelves client-side. Cheap for small
            // catalogs; move filtering server-side once this list grows.
            val result = dramaRepository.listDramas(
                limit = GENRE_SECTION_FETCH_LIMIT,
                sort = DramaSort.IMDB_RATING,
                order = SortOrder.DESC,
            )
            val all = (result as? ApiResult.Success)?.value.orEmpty()
            val filled = GENRE_SECTIONS_SEED.map { section ->
                section.copy(
                    items = all
                        .filter { drama -> drama.genres.any { it.equals(section.genreQuery, ignoreCase = true) } }
                        .take(GENRE_SECTION_SHELF_SIZE),
                    loading = false,
                )
            }
            _uiState.update { it.copy(genreSections = filled) }
        }
    }

    private companion object {
        /** How many dramas to show per Trending rail after filtering. */
        const val TRENDING_SHELF_SIZE = 10

        /** How many top-rated dramas to fetch before filtering for Trending.
         *  Small catalog (< 100) so we just pull the whole list and slice. */
        const val TRENDING_FETCH_LIMIT = 100

        /** How many dramas to show per genre shelf after filtering. */
        const val GENRE_SECTION_SHELF_SIZE = 12

        /** How many top-rated dramas to fetch before splitting into per-genre
         *  shelves. Same rationale as [TRENDING_FETCH_LIMIT]. */
        const val GENRE_SECTION_FETCH_LIMIT = 100
    }
}