package com.appriyo.deulama.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.domain.repository.DramaSort
import com.appriyo.deulama.domain.repository.SortOrder
import kotlinx.coroutines.flow.Flow

/**
 * Phase 2 Discover: a list (rendered as a grid on the screen side) of
 * every drama in the catalog, newest-first. Phase 3 will swap this for
 * a recommendation-driven pager; the ViewModel surface stays the same.
 */
class DiscoverViewModel(
    dramaRepository: DramaRepository,
) : ViewModel() {

    /** Newest-first catalog. Cached on `viewModelScope` so scrolling
     *  and configuration changes don't restart from page 1. */
    val catalog: Flow<PagingData<Drama>> = dramaRepository
        .pagedCatalog(sort = DramaSort.CREATED_AT, order = SortOrder.DESC)
        .cachedIn(viewModelScope)
}
