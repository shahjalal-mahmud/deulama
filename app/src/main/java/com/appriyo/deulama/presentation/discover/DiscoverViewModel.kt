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
 * Owns the newest-first paginated catalog stream. Interactive state
 * (active index, animation offsets, coach-mark visibility) lives on
 * the screen — this VM is just the catalog source.
 */
class DiscoverViewModel(
    dramaRepository: DramaRepository,
) : ViewModel() {

    val catalog: Flow<PagingData<Drama>> = dramaRepository
        .pagedCatalog(sort = DramaSort.CREATED_AT, order = SortOrder.DESC)
        .cachedIn(viewModelScope)
}
