package com.sina.spview.smpview.dialog

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.bundle.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sina.simpleview.library.R
import com.sina.spview.enms.OperationItem

class ChatOperationsDialogFragment<T>(private val operations: List<T>, private val onOperationSelected: (T) -> Unit) : DialogFragment() where T : Enum<T>, T : OperationItem {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_chat_operations)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.operations_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = OperationsAdapter(operations) { operation ->
            onOperationSelected(operation)
            dismiss()
        }

        return dialog
    }

    inner class OperationsAdapter(
        private val operations: List<T>,
        private val onClick: (T) -> Unit
    ) : RecyclerView.Adapter<OperationsAdapter.OperationViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_operation, parent, false)
            return OperationViewHolder(view)
        }

        override fun onBindViewHolder(holder: OperationViewHolder, position: Int) {
            val operation = operations[position]
            holder.bind(operation)
        }

        override fun getItemCount(): Int = operations.size

        inner class OperationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconView: TextView = itemView.findViewById(R.id.operation_icon)
            private val nameView: TextView = itemView.findViewById(R.id.operation_name)

            fun bind(operation: T) {
                iconView.text = operation.icon
                nameView.setText(operation.resNameId)
                itemView.setOnClickListener { onClick(operation) }
            }
        }
    }
}
fun <T> showOperationPopup(anchorView: View, operations: List<T>, onOperationSelected: (T) -> Unit) where T : Enum<T>, T : OperationItem {
    val context = anchorView.context
    val inflater = LayoutInflater.from(context)
    val popupView = inflater.inflate(R.layout.popup_chat_operations, null)

    val recyclerView = popupView.findViewById<RecyclerView>(R.id.operations_recycler_view)
    val popupWindow = PopupWindow(
        popupView,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true
    )
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_operation, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val operation = operations[position]
            val iconView: TextView = holder.itemView.findViewById(R.id.operation_icon)
            val nameView: TextView = holder.itemView.findViewById(R.id.operation_name)
            iconView.text = operation.icon
            nameView.setText(operation.resNameId)
            holder.itemView.setOnClickListener {
                onOperationSelected(operation)
                popupWindow.dismiss()
            }
        }

        override fun getItemCount(): Int = operations.size
    }



    // Show the popup window offset to the anchor view
    popupWindow.showAsDropDown(anchorView, 0, 0)
}
