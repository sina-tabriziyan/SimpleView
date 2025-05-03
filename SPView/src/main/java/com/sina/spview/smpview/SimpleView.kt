package com.sina.spview.smpview

import com.sina.spview.smpview.btmsheetdialog.confirmation.ConfirmationBtmSheetFactory
import com.sina.spview.smpview.btmsheetdialog.normal.BtmSheetFactory
import com.sina.spview.smpview.dialog.factory.SimpleDialogFactory
import com.sina.spview.smpview.dialog.factory.SimpleRadioGroupFactory


object SimpleView {
    val ConfirmationBtmSheet = ConfirmationBtmSheetFactory
    val SimpleBtmSheet = BtmSheetFactory
    val SimpleDialog = SimpleDialogFactory
    val SimpleRadioGroupDialog = SimpleRadioGroupFactory
}

