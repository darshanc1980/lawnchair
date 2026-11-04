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

import android.view.View
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.firstCached
import com.android.launcher3.CellLayout
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.model.ItemInstallQueue
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.util.ComponentKey
import com.android.launcher3.util.Executors

/**
 * Wires [FavoritesRailView] show/hide against [HomeLayoutMode] and owns add/remove/reorder.
 *
 * Rail lists a membership subset of desktop + hotseat apps/folders. Order + membership
 * live in [FavoritesRailOrderStore]. Widgets are skipped.
 */
class FavoritesRailController(
    private val launcher: LawnchairLauncher,
) {
    private val prefs2 = PreferenceManager2.getInstance(launcher)
    private val mode: HomeLayoutMode = prefs2.homeLayoutMode.firstCached()
    private val orderStore = FavoritesRailOrderStore(prefs2)

    private val rail: FavoritesRailView?
        get() = launcher.findViewById(R.id.favorites_rail)

    private val pageIndicator: View?
        get() = launcher.findViewById(R.id.page_indicator)

    private val stateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            syncVisibility(toState)
        }

        override fun onStateTransitionComplete(finalState: LauncherState) {
            syncVisibility(finalState)
        }
    }

    fun attach() {
        rail?.setOrderChangedListener { items ->
            orderStore.persistFromItems(items)
        }
        rail?.setRemoveListener { item ->
            removeFromRail(item)
        }
        launcher.stateManager.addStateListener(stateListener)
        syncVisibility()
        if (mode == HomeLayoutMode.VERTICAL_RAIL) {
            refreshFromModel()
        }
    }

    fun detach() {
        rail?.setOrderChangedListener(null)
        rail?.setRemoveListener(null)
        launcher.stateManager.removeStateListener(stateListener)
    }

    fun isRailMode(): Boolean = mode == HomeLayoutMode.VERTICAL_RAIL

    fun isOnRail(item: ItemInfo): Boolean {
        if (!isRailMode()) return false
        val membershipInitialized = prefs2.favoritesRailMembershipInitialized.firstCached()
        val orderedIds = orderStore.readOrderedIds()

        if (item.id != ItemInfo.NO_ID && item.id in orderedIds) return true

        val key = FavoritesRailOrderStore.componentKeyOf(item)
        if (key != null) {
            if (orderStore.containsComponentKey(key)) return true
            val keyStr = key.toString()
            if (rail?.currentItems()?.any {
                    FavoritesRailOrderStore.componentKeyOf(it)?.toString() == keyStr
                } == true
            ) {
                return true
            }
        }

        if (!membershipInitialized && orderedIds.isEmpty()) {
            return item.container == CONTAINER_DESKTOP || item.container == CONTAINER_HOTSEAT
        }
        return false
    }

    fun addToRail(item: ItemInfo) {
        if (!isRailMode()) {
            Toast.makeText(launcher, R.string.favorites_rail_switch_to_vertical, Toast.LENGTH_SHORT).show()
            return
        }
        if (item.id != ItemInfo.NO_ID &&
            (item.container == CONTAINER_DESKTOP || item.container == CONTAINER_HOTSEAT)
        ) {
            orderStore.appendId(item.id)
            refreshFromModel()
            Toast.makeText(launcher, R.string.favorites_rail_added, Toast.LENGTH_SHORT).show()
            return
        }

        val component = item.targetComponent
        val packageName = component?.packageName
        if (packageName.isNullOrEmpty()) {
            Toast.makeText(launcher, R.string.favorites_rail_add_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val key = ComponentKey(component, item.user)

        val existing = findWorkspaceItem(key)
        if (existing != null && existing.id != ItemInfo.NO_ID) {
            orderStore.appendId(existing.id)
            refreshFromModel()
            Toast.makeText(launcher, R.string.favorites_rail_added, Toast.LENGTH_SHORT).show()
            return
        }

        orderStore.enqueuePendingKey(key)
        ItemInstallQueue.INSTANCE.get(launcher).queueItem(packageName, item.user)
        refreshFromModel()
        Executors.MODEL_EXECUTOR.execute {
            try {
                Thread.sleep(450)
            } catch (_: InterruptedException) {
            }
            Executors.MAIN_EXECUTOR.execute {
                if (!launcher.isDestroyed) refreshFromModel()
            }
        }
        Toast.makeText(launcher, R.string.favorites_rail_added, Toast.LENGTH_SHORT).show()
    }

    fun removeFromRail(item: ItemInfo) {
        if (item.id != ItemInfo.NO_ID) {
            orderStore.removeId(item.id)
        }
        FavoritesRailOrderStore.componentKeyOf(item)?.let { key ->
            orderStore.writePendingKeys(
                orderStore.readPendingKeys().filterNot { it == key.toString() },
            )
        }
        refreshFromModel()
        Toast.makeText(launcher, R.string.favorites_rail_removed, Toast.LENGTH_SHORT).show()
    }

    fun refreshFromModel() {
        if (mode != HomeLayoutMode.VERTICAL_RAIL) return
        LauncherAppState.getInstance(launcher).model.loadAsync { dataModel ->
            if (dataModel == null || launcher.isDestroyed) return@loadAsync
            val items = synchronized(dataModel) {
                dataModel.itemsIdMap
                    .asSequence()
                    .filter { info ->
                        (info.container == CONTAINER_DESKTOP || info.container == CONTAINER_HOTSEAT) &&
                            (info is WorkspaceItemInfo || info is FolderInfo)
                    }
                    .toList()
            }
            val ordered = orderStore.applyOrder(items, persistIfChanged = true)
            Executors.MAIN_EXECUTOR.execute {
                if (launcher.isDestroyed) return@execute
                rail?.setItems(ordered)
                syncVisibility()
            }
        }
    }

    private fun findWorkspaceItem(key: ComponentKey): ItemInfo? {
        rail?.currentItems()?.firstOrNull {
            FavoritesRailOrderStore.componentKeyOf(it)?.toString() == key.toString()
        }?.let { return it }

        val keyStr = key.toString()
        val workspace = launcher.workspace
        if (workspace != null) {
            for (i in 0 until workspace.childCount) {
                val screen = workspace.getChildAt(i) as? CellLayout ?: continue
                findInCellLayout(screen, keyStr)?.let { return it }
            }
        }
        launcher.hotseat?.let { hotseat ->
            for (page in hotseat.pageLayouts) {
                findInCellLayout(page, keyStr)?.let { return it }
            }
        }
        return null
    }

    private fun findInCellLayout(layout: CellLayout, keyStr: String): ItemInfo? {
        val container = layout.shortcutsAndWidgets ?: return null
        for (j in 0 until container.childCount) {
            val info = container.getChildAt(j).tag as? ItemInfo ?: continue
            if (FavoritesRailOrderStore.componentKeyOf(info)?.toString() == keyStr) {
                return info
            }
        }
        return null
    }

    private fun syncVisibility(state: LauncherState = launcher.stateManager.state) {
        val railView = rail ?: return
        val workspace = launcher.workspace ?: return
        val railMode = mode == HomeLayoutMode.VERTICAL_RAIL

        if (railMode) {
            val showRail = state == LauncherState.NORMAL
            railView.visibility = if (showRail) View.VISIBLE else View.GONE
            workspace.visibility = if (showRail) View.INVISIBLE else View.VISIBLE
            pageIndicator?.visibility = View.GONE
        } else {
            railView.visibility = View.GONE
            workspace.visibility = View.VISIBLE
            pageIndicator?.visibility = View.VISIBLE
        }
    }
}
