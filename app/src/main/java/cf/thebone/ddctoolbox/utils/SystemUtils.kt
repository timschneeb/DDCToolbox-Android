package cf.thebone.ddctoolbox.utils

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import kotlin.math.pow
import kotlin.math.roundToInt

object SystemUtils {
    val screenOrientation: Int
        get() = Resources.getSystem().configuration.orientation

    fun findAbsoluteLocation(view: View): IntArray{
        val centeredLocation = IntArray(2)
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        centeredLocation[0] = viewLocation[0] + (view.width / 2)
        centeredLocation[1] = viewLocation[1] + (view.height / 2)
        return centeredLocation
    }

    fun findViewBounds(view: View, rightOffset: Int = 0, bottomOffset: Int = 0): Rect{
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        return Rect(
            viewLocation[0],
            viewLocation[1],
            viewLocation[0] + view.width + rightOffset,
            viewLocation[1] + view.height + bottomOffset
        )
    }

    fun roundToDecimals(number: Double, numDecimalPlaces: Int): Double {
        val factor = 10.0.pow(numDecimalPlaces.toDouble())
        return (number * factor).roundToInt() / factor
    }
}

