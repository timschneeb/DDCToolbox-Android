package cf.thebone.ddctoolbox.utils

import android.text.InputFilter
import android.text.Spanned

class IntRangeInputFilter : InputFilter {

    private var min: Int = Int.MIN_VALUE
    private var max: Int = Int.MAX_VALUE

    constructor(min: Int, max: Int) {
        this.min = min
        this.max = max
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

            val input = Integer.parseInt(dest.toString() + source.toString())
            if (isInRange(min, max, input))
                return null
        } catch (nfe: NumberFormatException) {
        }

        return ""
    }

    private fun isInRange(_min: Int, _max: Int, c: Int): Boolean {
        return if (_max > _min) c in _min.._max else c in _max.._min
    }
}