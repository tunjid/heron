/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.compose.gifs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Gif
import com.tunjid.heron.data.core.models.displayUri
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.automirrored.TrendingUp
import com.tunjid.heron.ui.icons.regular.Celebration
import com.tunjid.heron.ui.icons.regular.FavoriteBorder
import com.tunjid.heron.ui.icons.regular.Mood
import com.tunjid.heron.ui.icons.regular.MoodBad
import com.tunjid.heron.ui.icons.regular.Search
import com.tunjid.heron.ui.icons.regular.SearchOff
import com.tunjid.heron.ui.icons.regular.ThumbUp
import com.tunjid.heron.ui.modifiers.gridColumnCount
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.retainSheetStateHolder
import com.tunjid.heron.ui.shapes.roundedRectangle
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.mutator.compose.produceState
import com.tunjid.mutator.invoke
import com.tunjid.tiler.compose.PivotedTilingEffect
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.gif_category_happy
import heron.feature.compose.generated.resources.gif_category_love
import heron.feature.compose.generated.resources.gif_category_party
import heron.feature.compose.generated.resources.gif_category_sad
import heron.feature.compose.generated.resources.gif_category_trending
import heron.feature.compose.generated.resources.gif_category_yes
import heron.feature.compose.generated.resources.gif_search_placeholder
import heron.feature.compose.generated.resources.gifs_empty_description
import heron.feature.compose.generated.resources.gifs_empty_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaneScaffoldState.rememberGifPickerSheetState(
    onGifSelected: (Gif) -> Unit,
): GifPickerSheetState =
    GifPickerSheetState.rememberUpdatedGifPickerSheetState(
        stateHolder = retainSheetStateHolder<GifPickerStateHolder>(),
        onGifSelected = onGifSelected,
    )

@Stable
class GifPickerSheetState internal constructor(
    scope: BottomSheetScope,
    internal val stateHolder: GifPickerStateHolder,
) : BottomSheetState(scope) {

    fun showGifPicker() {
        // Trending GIFs change throughout the day, so start fresh each time the sheet opens.
        stateHolder(GifPickerAction.SelectCategory(GifCategory.Trending))
        show()
    }

    override fun onHidden() = Unit

    companion object {
        @Composable
        fun rememberUpdatedGifPickerSheetState(
            stateHolder: GifPickerStateHolder,
            onGifSelected: (Gif) -> Unit,
        ): GifPickerSheetState {
            val state = rememberBottomSheetState(
                stateHolder = stateHolder,
                block = ::GifPickerSheetState,
            )
            GifPickerBottomSheet(
                state = state,
                onGifSelected = onGifSelected,
            )
            return state
        }
    }
}

@Composable
private fun GifPickerBottomSheet(
    state: GifPickerSheetState,
    onGifSelected: (Gif) -> Unit,
) {
    state.ModalBottomSheet {
        val pickerState = state.stateHolder.produceState()
        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = pickerState.searchText,
                onValueChange = { text ->
                    state.stateHolder(GifPickerAction.Search(text))
                },
                singleLine = true,
                shape = CircleShape,
                leadingIcon = {
                    Icon(
                        imageVector = HeronIcons.Regular.Search,
                        contentDescription = null,
                    )
                },
                placeholder = {
                    Text(text = stringResource(Res.string.gif_search_placeholder))
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
            )

            GifCategories(
                selectedCategory = pickerState.selectedCategory,
                onCategorySelected = { category ->
                    focusManager.clearFocus()
                    state.stateHolder(GifPickerAction.SelectCategory(category))
                },
            )

            GifGrid(
                modifier = Modifier
                    .weight(1f),
                pickerState = pickerState,
                onAction = state.stateHolder.accept,
                onGifClicked = { gif ->
                    onGifSelected(gif)
                    state.hide()
                },
            )
        }
    }
}

@Composable
private fun GifCategories(
    selectedCategory: GifCategory?,
    onCategorySelected: (GifCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GifCategory.entries.forEach { category ->
            FilledTonalIconToggleButton(
                checked = category == selectedCategory,
                onCheckedChange = {
                    onCategorySelected(category)
                },
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = stringResource(category.label),
                )
            }
        }
    }
}

@Composable
private fun GifGrid(
    modifier: Modifier = Modifier,
    pickerState: GifPickerState,
    onAction: (GifPickerAction) -> Unit,
    onGifClicked: (Gif) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    val density = LocalDensity.current

    if (pickerState.tiledItems.isEmpty()) EmptyContent(
        modifier = modifier,
        titleRes = Res.string.gifs_empty_title,
        descriptionRes = Res.string.gifs_empty_description,
        icon = HeronIcons.Regular.SearchOff,
    )
    else LazyVerticalStaggeredGrid(
        modifier = modifier
            .fillMaxWidth()
            .gridColumnCount(
                density = density,
                maxColumnWidth = GifCellMinWidth,
            ) { numColumns ->
                onAction(
                    GifPickerAction.Tile(
                        TilingState.Action.GridSize(numColumns = numColumns),
                    ),
                )
            },
        state = gridState,
        columns = StaggeredGridCells.Adaptive(GifCellMinWidth),
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = pickerState.tiledItems,
            key = Gif::id,
        ) { gif ->
            GifCell(
                modifier = Modifier
                    .animateItem(),
                gif = gif,
                onClick = {
                    onGifClicked(gif)
                },
            )
        }
    }

    gridState.PivotedTilingEffect(
        items = pickerState.tiledItems,
        onQueryChanged = { query ->
            onAction(
                GifPickerAction.Tile(
                    TilingState.Action.LoadAround(
                        query = query ?: pickerState.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

@Composable
private fun GifCell(
    modifier: Modifier = Modifier,
    gif: Gif,
    onClick: () -> Unit,
) {
    val media = gif.tinyGif ?: gif.gif
    AsyncImage(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(media.aspectRatio)
            .clip(GifCellShape)
            .clickable(onClick = onClick),
        args = remember(media, gif.description, gif.title) {
            ImageArgs(
                url = media.displayUri.uri,
                contentDescription = gif.description.ifBlank { gif.title },
                contentScale = ContentScale.Crop,
                shape = GifCellShape,
            )
        },
    )
}

private val GifCategory.icon: ImageVector
    get() = when (this) {
        GifCategory.Trending -> HeronIcons.AutoMirrored.TrendingUp
        GifCategory.Love -> HeronIcons.Regular.FavoriteBorder
        GifCategory.Happy -> HeronIcons.Regular.Mood
        GifCategory.Sad -> HeronIcons.Regular.MoodBad
        GifCategory.Party -> HeronIcons.Regular.Celebration
        GifCategory.Yes -> HeronIcons.Regular.ThumbUp
    }

private val GifCategory.label: StringResource
    get() = when (this) {
        GifCategory.Trending -> Res.string.gif_category_trending
        GifCategory.Love -> Res.string.gif_category_love
        GifCategory.Happy -> Res.string.gif_category_happy
        GifCategory.Sad -> Res.string.gif_category_sad
        GifCategory.Party -> Res.string.gif_category_party
        GifCategory.Yes -> Res.string.gif_category_yes
    }

private val GifCellMinWidth = 100.dp

private val GifCellShape = PolygonShape.roundedRectangle(percent = 0.08f)
