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
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.util.ComponentKey
import com.patrykmichalik.opto.core.setBlocking

/**
 * Persistence for the favorites rail membership + order.
 *
 * **MVP: Prefs-backed Favorites IDs (architecture Option C).**
 *
 * - Empty order + membership not initialized → seed from hotseat + desktop, then lock.
 * - After membership is initialized → only listed Favorites ids appear (add/remove explicit).
 * - [favorites_rail_pending_keys] holds ComponentKey strings for apps queued onto the
 *   workspace that are not yet bound; resolved on the next refresh.
 */
class FavoritesRailOrderStore(
    private val prefs2: PreferenceManager2,
) {

    fun readOrderedIds(): List<Int> = parseIds(prefs2.favoritesRailOrder.firstCached())

    fun writeOrderedIds(ids: List<Int>) {
        prefs2.favoritesRailOrder.setBlocking(serializeIds(ids))
    }

    fun readPendingKeys(): List<String> = parseKeys(prefs2.favoritesRailPendingKeys.firstCached())

    fun writePendingKeys(keys: List<String>) {
        prefs2.favoritesRailPendingKeys.setBlocking(serializeKeys(keys))
    }

    fun containsId(id: Int): Boolean {
        if (id == ItemInfo.NO_ID) return false
        val saved = readOrderedIds()
        val membershipInitialized = prefs2.favoritesRailMembershipInitialized.firstCached()
        return if (!membershipInitialized && saved.isEmpty()) {
            true
        } else {
            id in saved
        }
    }

    fun removeId(id: Int) {
        if (id == ItemInfo.NO_ID) return
        writeOrderedIds(readOrderedIds().filterNot { it == id })
        prefs2.favoritesRailMembershipInitialized.setBlocking(true)
    }

    fun appendId(id: Int) {
        if (id == ItemInfo.NO_ID) return
        val saved = readOrderedIds().toMutableList()
        if (id !in saved) {
            saved.add(id)
            writeOrderedIds(saved)
        }
        prefs2.favoritesRailMembershipInitialized.setBlocking(true)
    }

    fun enqueuePendingKey(key: ComponentKey) {
        val keyStr = key.toString()
        val pending = readPendingKeys().toMutableList()
        if (keyStr !in pending) {
            pending.add(keyStr)
            writePendingKeys(pending)
        }
        prefs2.favoritesRailMembershipInitialized.setBlocking(true)
    }

    fun containsComponentKey(key: ComponentKey): Boolean {
        val keyStr = key.toString()
        if (keyStr in readPendingKeys()) return true
        // Membership by id only; callers that need live rail match should check current items.
        return false
    }

    /**
     * Orders / filters [items] for the rail.
     */
    fun applyOrder(items: List<ItemInfo>, persistIfChanged: Boolean = true): List<ItemInfo> {
        val membershipInitialized = prefs2.favoritesRailMembershipInitialized.firstCached()
        val saved = readOrderedIds()

        if (items.isEmpty()) {
            if (persistIfChanged && saved.isNotEmpty()) {
                writeOrderedIds(emptyList())
            }
            return emptyList()
        }

        val byId = LinkedHashMap<Int, ItemInfo>(items.size)
        val byKey = LinkedHashMap<String, ItemInfo>(items.size)
        for (item in items) {
            if (item.id != ItemInfo.NO_ID) {
                byId[item.id] = item
            }
            componentKeyOf(item)?.let { byKey[it.toString()] = item }
        }

        val pending = readPendingKeys()
        val resolvedPendingIds = ArrayList<Int>()
        val stillPending = ArrayList<String>()
        for (keyStr in pending) {
            val item = byKey[keyStr]
            if (item != null && item.id != ItemInfo.NO_ID) {
                resolvedPendingIds.add(item.id)
            } else {
                stillPending.add(keyStr)
            }
        }
        if (persistIfChanged && stillPending != pending) {
            writePendingKeys(stillPending)
        }

        if (!membershipInitialized && saved.isEmpty()) {
            val ordered = items.sortedWith(DEFAULT_COMPARATOR)
            val nextIds = ordered.mapNotNull { info ->
                info.id.takeIf { it != ItemInfo.NO_ID }
            }
            if (persistIfChanged) {
                writeOrderedIds(nextIds)
                prefs2.favoritesRailMembershipInitialized.setBlocking(true)
            }
            return ordered
        }

        val ordered = ArrayList<ItemInfo>(saved.size + resolvedPendingIds.size)
        val used = HashSet<Int>()

        for (id in saved) {
            val item = byId[id] ?: continue
            ordered.add(item)
            used.add(id)
        }
        for (id in resolvedPendingIds) {
            if (id in used) continue
            val item = byId[id] ?: continue
            ordered.add(item)
            used.add(id)
        }

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
        prefs2.favoritesRailMembershipInitialized.setBlocking(true)
    }

    companion object {
        val DEFAULT_COMPARATOR: Comparator<ItemInfo> = compareBy(
            { if (it.container == com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT) 0 else 1 },
            { it.screenId },
            { it.cellY },
            { it.cellX },
        )

        fun parseIds(raw: String): List<Int> {
            if (raw.isBlank()) return emptyList()
            return raw.split(',')
                .mapNotNull { token -> token.trim().toIntOrNull() }
        }

        fun serializeIds(ids: List<Int>): String = ids.joinToString(separator = ",")

        fun parseKeys(raw: String): List<String> {
            if (raw.isBlank()) return emptyList()
            return raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        }

        fun serializeKeys(keys: List<String>): String = keys.joinToString(separator = "\n")

        fun componentKeyOf(item: ItemInfo): ComponentKey? {
            val component = item.targetComponent ?: return null
            return ComponentKey(component, item.user)
        }

        @Suppress("UNUSED")
        fun componentKeyOf(info: WorkspaceItemInfo): ComponentKey? = componentKeyOf(info as ItemInfo)
    }
}
