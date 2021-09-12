package cf.thebone.ddctoolbox.utils

import android.content.Context

object SizeUtils {
    fun dp2px(context: Context, dpValue: Float): Int {
        val scale: Float = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun dp2px(context: Context, dpValue: Int): Int {
        return dp2px(context, dpValue.toFloat())
    }
}