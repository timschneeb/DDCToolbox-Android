package cf.thebone.ddctoolbox.utils

import android.text.InputFilter
import android.text.Spanned
import android.util.Log

class DoubleRangeInputFilter : InputFilter {

    private var min: Double = Double.MIN_VALUE
    private var max: Double = Double.MAX_VALUE

    constructor(min: Double, max: Double) {
        this.min = min
        this.max = max
    }

    constructor(min: Int, max: Int) {
        this.min = min.toDouble()
        this.max = max.toDouble()
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            //Allow single minus sign
            if(source.toString() == "-")
                return null

            val input = (dest.toString() + source.toString()).toDouble()
            if (isInRange(min, max, input))
                return null
        } catch (nfe: NumberFormatException) {
        }

        return ""
    }

    private fun isInRange(_min: Double, _max: Double, c: Double): Boolean {
        return if (_max > _min) c in _min.._max else c in _max.._min
    }
}