/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.resourcepicker.screen.data

import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.passbolt.mobile.android.common.urimatcher.AutofillUriMatcher
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcesPaginatedUseCase
import com.passbolt.mobile.android.mappers.ResourcePickerMapper
import com.passbolt.mobile.android.resourcepicker.screen.ResourcePickerViewModel.Companion.SELECTABLE_RESOURCE_TYPES_SLUGS
import com.passbolt.mobile.android.supportedresourceTypes.SupportedContentTypes.allSlugs
import com.passbolt.mobile.android.ui.ResourcePickerListItem
import com.passbolt.mobile.android.ui.ResourceUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ResourcePickerDataProvider(
    private val getLocalResourcesPaginatedUseCase: GetLocalResourcesPaginatedUseCase,
    private val resourcePickerMapper: ResourcePickerMapper,
    private val autofillUriMatcher: AutofillUriMatcher,
) {
    suspend fun provideData(
        searchQuery: String?,
        suggestionUri: String?,
    ): ResourcePickerData {
        val resources =
            getLocalResourcesPaginatedUseCase
                .execute(
                    GetLocalResourcesPaginatedUseCase.Input(
                        slugs = allSlugs,
                        searchQuery = searchQuery,
                        enablePlaceholders = true,
                    ),
                ).pagedResourcesFlow
                .mapToPickerItems()

        val suggestedResources =
            if (!suggestionUri.isNullOrBlank() && searchQuery.isNullOrBlank()) {
                getLocalResourcesPaginatedUseCase
                    .execute(GetLocalResourcesPaginatedUseCase.Input(slugs = allSlugs))
                    .pagedResourcesFlow
                    .mapToPickerItems()
                    .map { pagingData ->
                        pagingData.filter {
                            autofillUriMatcher.isMatching(
                                suggestionUri,
                                it.resourceModel.metadataJsonModel.uris
                                    .orEmpty() +
                                    it.resourceModel.metadataJsonModel.uri
                                        .orEmpty(),
                            )
                        }
                    }
            } else {
                settledEmptyPagingData<ResourcePickerListItem>()
            }

        return ResourcePickerData(
            suggestedResources = suggestedResources,
            resources = resources,
        )
    }

    private fun Flow<PagingData<ResourceUiModel>>.mapToPickerItems() =
        map { pagingData ->
            pagingData.map { resourcePickerMapper.map(it, SELECTABLE_RESOURCE_TYPES_SLUGS) }
        }
}
