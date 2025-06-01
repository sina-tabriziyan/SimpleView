package com.sina.spview.enms

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

interface OperationItem {
    val icon: String
    val resNameId: Int
}



