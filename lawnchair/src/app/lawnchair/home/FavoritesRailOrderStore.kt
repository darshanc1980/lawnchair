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

import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.firstCached
import com.android.launcher3.model.data.ItemInfo
import com.patrykmichalik.opto.core.setBlocking

/**
 * MVP persistence for the favorites rail order.
 *
 * **Choice: Prefs-backed ordered Favorites IDs (architecture Option C for MVP).**
 *
 * Option A (mutate `cellY` / reserved screen) would rewrite workspace coordinates and break
 * classic-grid round-trip. Option B (new Favorites container) needs broader Launcher3 model
 * changes. For MVP we keep icons/titles/launch/uninstall sync in [BgDataModel] / Favorites DB
 * and only store the **display order** as a comma-separated list of Favorites row ids in
 * DataStore (`favorites_rail_order`). Phase 2 can migrate to Option A/B once rail owns pins.
 */
class FavoritesRailOrderStore(
    private val prefs2: PreferenceManager2,
) {

    fun readOrderedIds(): List<Int> = parse(prefs2.favoritesRailOrder.firstCached())

    fun writeOrderedIds(ids: List<Int>) {
        prefs2.favoritesRailOrder.setBlocking(serialize(ids))
    }

    /**
     * Orders [items] by saved ids. Unknown/new items append in [fallbackSort] order.
     * Persists a cleaned list when the saved order was empty or contained stale ids / new items.
     */
    fun applyOrder(items: List<ItemInfo>, persistIfChanged: Boolean = true): List<ItemInfo> {
        if (items.isEmpty()) {
            if (persistIfChanged && readOrderedIds().isNotEmpty()) {
                writeOrderedIds(emptyList())
            }
            return emptyList()
        }

        val byId = LinkedHashMap<Int, ItemInfo>(items.size)
        for (item in items) {
            if (item.id != ItemInfo.NO_ID) {
                byId[item.id] = item
            }
        }

        val saved = readOrderedIds()
        val ordered = ArrayList<ItemInfo>(items.size)
        val used = HashSet<Int>(items.size)

        for (id in saved) {
            val item = byId[id] ?: continue
            ordered.add(item)
            used.add(id)
        }

        // Append items not in the saved order (new pins / first seed).
        val remainder = items.filter { it.id == ItemInfo.NO_ID || it.id !in used }
            .sortedWith(DEFAULT_COMPARATOR)
        ordered.addAll(remainder)

        val nextIds = ordered.mapNotNull { info ->
            info.id.takeIf { it != ItemInfo.NO_ID }
        }
        if (persistIfChanged && nextIds != saved) {
            writeOrderedIds(nextIds)
        }
        return ordered
    }

    fun persistFromItems(items: List<ItemInfo>) {
        writeOrderedIds(items.mapNotNull { info -> info.id.takeIf { it != ItemInfo.NO_ID } })
    }

    companion object {
        val DEFAULT_COMPARATOR: Comparator<ItemInfo> = compareBy(
            { if (it.container == com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT) 0 else 1 },
            { it.screenId },
            { it.cellY },
            { it.cellX },
        )

        fun parse(raw: String): List<Int> {
            if (raw.isBlank()) return emptyList()
            return raw.split(',')
                .mapNotNull { token -> token.trim().toIntOrNull() }
        }

        fun serialize(ids: List<Int>): String = ids.joinToString(separator = ",")
    }
}
