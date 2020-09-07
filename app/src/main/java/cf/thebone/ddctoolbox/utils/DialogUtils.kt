package cf.thebone.ddctoolbox.utils

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog

object DialogUtils {
    fun showDialog(context: Context, title: String, message: String, @DrawableRes iconId: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
        builder.setTitle(title)
        builder.setIcon(iconId)
        builder.setCancelable(true)
        builder.setPositiveButton(android.R.string.ok, null)
        builder.create().show()
    }
}