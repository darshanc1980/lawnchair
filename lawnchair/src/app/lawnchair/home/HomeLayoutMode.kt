/*
 * Copyright 2026, Velo / Lawnchair contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.home

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import com.android.launcher3.R

/**
 * Hybrid home layout modes for Velo.
 *
 * Persistence values: [VERTICAL_RAIL] → `"vertical_rail"`, [CLASSIC_GRID] → `"classic_grid"`.
 */
enum class HomeLayoutMode(
    @StringRes val labelResourceId: Int,
) {
    VERTICAL_RAIL(
        labelResourceId = R.string.home_layout_mode_vertical_rail,
    ),
    CLASSIC_GRID(
        labelResourceId = R.string.home_layout_mode_classic_grid,
    ),
    ;

    override fun toString(): String = when (this) {
        VERTICAL_RAIL -> VALUE_VERTICAL_RAIL
        CLASSIC_GRID -> VALUE_CLASSIC_GRID
    }

    companion object {
        const val VALUE_VERTICAL_RAIL = "vertical_rail"
        const val VALUE_CLASSIC_GRID = "classic_grid"

        fun fromString(value: String?): HomeLayoutMode = when (value) {
            VALUE_CLASSIC_GRID -> CLASSIC_GRID
            else -> VERTICAL_RAIL
        }

        fun valuesList(): List<HomeLayoutMode> = listOf(VERTICAL_RAIL, CLASSIC_GRID)

        fun entries(): List<ListPreferenceEntry<HomeLayoutMode>> = valuesList().map {
            ListPreferenceEntry(value = it) { stringResource(id = it.labelResourceId) }
        }
    }
}
