/**
 * Created by ST on 2/11/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.smpview.btmsheetdialog.normal

import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding

object BtmSheetFactory {
    inline fun <reified B : ViewBinding> create(
        fragmentManager: FragmentManager,
        noinline setup: (B) -> Unit
    ): BtmSheetFragment<B> {
        return BtmSheetFragment(B::class.java, setup)
    }
}

