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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.android.launcher3.R
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.ItemInfoWithIcon
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.touch.ItemClickHandler

/**
 * Vertical favorites rail for [HomeLayoutMode.VERTICAL_RAIL].
 *
 * MVP: long-press drag-reorder; order persistence is owned by [FavoritesRailController]
 * via [FavoritesRailOrderStore] (Prefs-backed Favorites ids).
 */
class FavoritesRailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    fun interface OrderChangedListener {
        fun onRailOrderChanged(items: List<ItemInfo>)
    }

    private var orderChangedListener: OrderChangedListener? = null

    private val adapter = FavoritesRailAdapter { items ->
        orderChangedListener?.onRailOrderChanged(items)
    }

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
        itemAnimator = null
    }

    private val itemTouchHelper = ItemTouchHelper(DragCallback())

    init {
        // Soft scrim so classic grid icons are not readable through the rail.
        setBackgroundColor(0x99000000.toInt())
        recyclerView.adapter = adapter
        itemTouchHelper.attachToRecyclerView(recyclerView)
        addView(recyclerView)
        visibility = GONE
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.home_layout_mode_vertical_rail)
    }

    fun setOrderChangedListener(listener: OrderChangedListener?) {
        orderChangedListener = listener
    }

    fun setItems(items: List<ItemInfo>) {
        adapter.submit(items)
    }

    fun currentItems(): List<ItemInfo> = adapter.snapshot()

    private inner class DragCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0,
    ) {
        override fun isLongPressDragEnabled(): Boolean = true

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (from == NO_POSITION || to == NO_POSITION) return false
            return adapter.moveItem(from, to)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // No swipe dismiss in MVP.
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            resetDragView(viewHolder.itemView)
            adapter.notifyOrderCommitted()
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            val itemView = viewHolder?.itemView ?: return
            when (actionState) {
                ItemTouchHelper.ACTION_STATE_DRAG -> {
                    itemView.animate()
                        .scaleX(1.04f)
                        .scaleY(1.04f)
                        .translationZ(8f)
                        .setDuration(120)
                        .start()
                }
                else -> resetDragView(itemView)
            }
        }
    }

    private fun resetDragView(view: View) {
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationZ(0f)
            .setDuration(120)
            .start()
    }

    private class FavoritesRailAdapter(
        private val onOrderCommitted: (List<ItemInfo>) -> Unit,
    ) : RecyclerView.Adapter<FavoritesRailAdapter.Holder>() {
        private val items = mutableListOf<ItemInfo>()
        private var pendingCommit = false

        fun submit(newItems: List<ItemInfo>) {
            items.clear()
            items.addAll(newItems)
            pendingCommit = false
            notifyDataSetChanged()
        }

        fun snapshot(): List<ItemInfo> = items.toList()

        fun moveItem(from: Int, to: Int): Boolean {
            if (from == to || from !in items.indices || to !in items.indices) return false
            val item = items.removeAt(from)
            items.add(to, item)
            notifyItemMoved(from, to)
            pendingCommit = true
            return true
        }

        fun notifyOrderCommitted() {
            if (!pendingCommit) return
            pendingCommit = false
            onOrderCommitted(snapshot())
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
                itemView.contentDescription = buildString {
                    append(label.text)
                    append(". ")
                    append(itemView.context.getString(R.string.favorites_rail_reorder_hint))
                }
            }
        }
    }
}
