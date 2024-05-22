import android.app.AlertDialog
import android.content.Context
import com.example.accelometer.R

/*
* Class for showing important info
* */
class CustomDialog {
    companion object {
        fun showMessage(context: Context, title: String, message: String) {
            val builder = AlertDialog.Builder(context, R.style.CustomDialogStyle)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog: AlertDialog = builder.create()

            // Set the dialog to not be dismissable by touching outside of the dialog
            dialog.setCanceledOnTouchOutside(true)

            // Show the dialog
            dialog.show()
            print("Zobrazeno dialogové okno")
        }
    }
}