package com.sina.spview.smpview.views

import android.graphics.Canvas
import android.icu.lang.UCharacter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
interface SwipableItem {
    val isUserMessage: Boolean // Or any property that helps determine swipe direction
}
data class ChatMessage(
    // ... other properties
    val userId: String,
    val currentUserId: String // Assuming you have a way to identify the current user
) : SwipableItem {
    override val isUserMessage: Boolean
        get() = userId == currentUserId
}
class ItemTouchHelperSwipeCallback<T : SwipableItem, VH : RecyclerView.ViewHolder>(
    private val adapter: ListAdapter<T, VH>,
    private val onSwipeDetected: (position: Int, direction: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false // No move support for this callback


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.absoluteAdapterPosition

        if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
            // Notify the swipe action
            onSwipeDetected(position, direction)

            // Reset the item immediately to its original position by notifying the adapter.
            // This causes the view to be rebound, clearing the swipe effect.
            adapter.notifyItemChanged(position)
        }
    }


    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val position = viewHolder.absoluteAdapterPosition

        if (position == RecyclerView.NO_POSITION || position >= adapter.currentList.size) {
            return 0 // Disable swipe for invalid positions or out of bounds
        }

        val item = adapter.currentList[position]

        // Allow swipe direction based on the item's property
        return if (item.isUserMessage) {
            ItemTouchHelper.LEFT // User's messages can only swipe left
        } else {
            ItemTouchHelper.RIGHT // Others' messages can only swipe right
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Limit swipe distance to 30% of the item's width
            val maxSwipeDistance = viewHolder.itemView.width * 0.3f
            val clampedDx = dX.coerceIn(-maxSwipeDistance, maxSwipeDistance)

            super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive)
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // Explicitly reset translationX to ensure the view is perfectly aligned after swipe.
        // This is particularly useful if custom drawing modified translationX.
        viewHolder.itemView.translationX = 0f
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        // The fraction of the view's width or height that needs to be swiped
        // to trigger the onSwiped callback.
        return 0.5f // Require at least 50% drag to trigger swipe
    }
}