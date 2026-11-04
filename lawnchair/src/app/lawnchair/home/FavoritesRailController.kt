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
import app.lawnchair.LawnchairLauncher
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.firstCached
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.util.Executors

/**
 * Wires [FavoritesRailView] show/hide against [HomeLayoutMode] and persists rail order.
 *
 * MVP: rail lists desktop + hotseat apps/folders; order lives in [FavoritesRailOrderStore]
 * (DataStore ids). Widgets on the rail are skipped. Preference home_layout_mode still
 * recreates the activity via PreferenceManager2.onSet.
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
        launcher.stateManager.addStateListener(stateListener)
        syncVisibility()
        if (mode == HomeLayoutMode.VERTICAL_RAIL) {
            refreshFromModel()
        }
    }

    fun detach() {
        rail?.setOrderChangedListener(null)
        launcher.stateManager.removeStateListener(stateListener)
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

    private fun syncVisibility(state: LauncherState = launcher.stateManager.state) {
        val railView = rail ?: return
        val workspace = launcher.workspace ?: return
        val railMode = mode == HomeLayoutMode.VERTICAL_RAIL

        if (railMode) {
            val showRail = state == LauncherState.NORMAL
            railView.visibility = if (showRail) View.VISIBLE else View.GONE
            // Keep workspace inflated for bind/overview; hide only while rail is home.
            workspace.visibility = if (showRail) View.INVISIBLE else View.VISIBLE
            pageIndicator?.visibility = View.GONE
        } else {
            railView.visibility = View.GONE
            workspace.visibility = View.VISIBLE
            pageIndicator?.visibility = View.VISIBLE
        }
    }
}
