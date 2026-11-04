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
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
 * Long-press drag-reorder; swipe start-to-end to remove. Persistence / membership owned by
 * [FavoritesRailController] via [FavoritesRailOrderStore].
 */
class FavoritesRailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    fun interface OrderChangedListener {
        fun onRailOrderChanged(items: List<ItemInfo>)
    }

    fun interface RemoveListener {
        fun onRailItemRemoved(item: ItemInfo)
    }

    private var orderChangedListener: OrderChangedListener? = null
    private var removeListener: RemoveListener? = null

    private val adapter = FavoritesRailAdapter(
        onOrderCommitted = { items -> orderChangedListener?.onRailOrderChanged(items) },
        onRemove = { item -> removeListener?.onRailItemRemoved(item) },
    )

    private val recyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        clipToPadding = false
        val density = resources.displayMetrics.density
        // Bottom-weighted padding for one-handed reach.
        setPadding(
            paddingLeft,
            (32 * density).toInt(),
            paddingRight,
            (72 * density).toInt(),
        )
        isNestedScrollingEnabled = true
        itemAnimator = null
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
    }

    private val itemTouchHelper = ItemTouchHelper(DragCallback())

    init {
        background = buildScrimBackground()
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

    fun setRemoveListener(listener: RemoveListener?) {
        removeListener = listener
    }

    fun setItems(items: List<ItemInfo>) {
        adapter.submit(items)
    }

    fun currentItems(): List<ItemInfo> = adapter.snapshot()

    private fun buildScrimBackground(): GradientDrawable {
        val surface = resolveColor(android.R.attr.colorBackground, 0xFF121212.toInt())
        val scrim = ColorUtils.setAlphaComponent(surface, 0xCC)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(scrim)
        }
    }

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                ContextCompat.getColor(context, tv.resourceId)
            } else {
                tv.data
            }
        } else {
            fallback
        }
    }

    private inner class DragCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.END,
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
            val pos = viewHolder.bindingAdapterPosition
            if (pos == NO_POSITION) return
            adapter.removeAt(pos)
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
                        .translationZ(12f)
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
        private val onRemove: (ItemInfo) -> Unit,
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

        fun removeAt(position: Int) {
            if (position !in items.indices) return
            val item = items.removeAt(position)
            notifyItemRemoved(position)
            onRemove(item)
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
