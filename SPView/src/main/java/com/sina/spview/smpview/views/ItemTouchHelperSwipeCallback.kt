package com.sina.spview.smpview.views

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * A generic ItemTouchHelper.SimpleCallback for providing swipe visual feedback and detection.
 * It does not make assumptions about item types or swipe directions beyond what is configured.
 *
 * @param allowedSwipeDirections A function that takes a ViewHolder and returns the allowed
 *                               swipe directions for that item (e.g., ItemTouchHelper.LEFT).
 *                               Return 0 to disable swipe for an item.
 * @param onSwipeDetected A lambda function that is invoked when a swipe action is detected.
 *                        It receives the position of the swiped item and the swipe direction.
 */
class ItemTouchHelperSwipeCallback(
    private val adapter: RecyclerView.Adapter<*>, // Generic adapter
    private val allowedSwipeDirections: (viewHolder: RecyclerView.ViewHolder) -> Int,
    private val onSwipeDetected: (position: Int, direction: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) { // Default to both, but getSwipeDirs will override

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false // No move support

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.absoluteAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            onSwipeDetected(position, direction)
            // Crucially, ensure the adapter is notified to redraw the item.
            // This helps in resetting the view if the swipe doesn't remove the item.
            adapter.notifyItemChanged(position)
        }
    }

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val position = viewHolder.absoluteAdapterPosition
        return if (position != RecyclerView.NO_POSITION) {
            allowedSwipeDirections(viewHolder)
        } else {
            0 // No swipe if position is invalid
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
            val maxSwipeDistance = viewHolder.itemView.width * 0.3f // Max 30% of item width
            val newDx = if (getSwipeDirs(recyclerView, viewHolder) == ItemTouchHelper.LEFT) {
                dX.coerceAtMost(0f).coerceAtLeast(-maxSwipeDistance) // Allow only left swipe, clamp negative dX
            } else if (getSwipeDirs(recyclerView, viewHolder) == ItemTouchHelper.RIGHT) {
                dX.coerceAtLeast(0f).coerceAtMost(maxSwipeDistance) // Allow only right swipe, clamp positive dX
            } else {
                0f // If both or none, effectively clamp to not move beyond threshold quickly
            }
            super.onChildDraw(c, recyclerView, viewHolder, newDx, dY, actionState, isCurrentlyActive)
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.translationX = 0f // Explicitly reset translation
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.5f // Require at least 50% drag to trigger swipe
    }
}