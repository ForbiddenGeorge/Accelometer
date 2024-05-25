package com.example.accelometer

import android.app.AlertDialog
import android.content.Context

/**
*
* Class for showing important info
*
*/
class CustomDialog {
    companion object {
        fun showMessage(context: Context, title: String, message: String) {
            val builder = AlertDialog.Builder(context, R.style.CustomDialogStyle)
            //Populate the dialog with received data
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog: AlertDialog = builder.create()

            // Set the dialog to not be dismissible by touching outside of the dialog
            dialog.setCanceledOnTouchOutside(true)

            // Show the dialog
            dialog.show()
        }
    }
}