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

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.ItemInfoWithIcon
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.touch.ItemClickHandler

/**
 * Phase-0 stub: vertical list of already-bound desktop/hotseat favorites.
 *
 * No DB migration, no reorder persistence — read-only overlay for [HomeLayoutMode.VERTICAL_RAIL].
 */
class FavoritesRailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val recyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        clipToPadding = false
        val density = resources.displayMetrics.density
        setPadding(
            paddingLeft,
            (24 * density).toInt(),
            paddingRight,
            (48 * density).toInt(),
        )
        isNestedScrollingEnabled = true
    }

    private val adapter = FavoritesRailAdapter()

    init {
        // Soft scrim so classic grid icons are not readable through the rail.
        setBackgroundColor(0x99000000.toInt())
        recyclerView.adapter = adapter
        addView(recyclerView)
        visibility = GONE
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.home_layout_mode_vertical_rail)
    }

    fun setItems(items: List<ItemInfo>) {
        adapter.submit(items)
    }

    private class FavoritesRailAdapter : RecyclerView.Adapter<FavoritesRailAdapter.Holder>() {
        private val items = mutableListOf<ItemInfo>()

        fun submit(newItems: List<ItemInfo>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.favorites_rail_item, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.favorites_rail_icon)
            private val label: TextView = itemView.findViewById(R.id.favorites_rail_label)

            fun bind(item: ItemInfo) {
                itemView.tag = item
                label.text = item.title ?: item.contentDescription ?: ""

                when (item) {
                    is ItemInfoWithIcon -> {
                        icon.setImageDrawable(BitmapDrawable(itemView.resources, item.bitmap.icon))
                    }
                    is FolderInfo -> {
                        icon.setImageDrawable(
                            ContextCompat.getDrawable(itemView.context, R.drawable.ic_folder),
                        )
                    }
                    else -> icon.setImageDrawable(null)
                }

                itemView.setOnClickListener(ItemClickHandler.INSTANCE)
                itemView.isEnabled = item is WorkspaceItemInfo || item is FolderInfo
            }
        }
    }
}
