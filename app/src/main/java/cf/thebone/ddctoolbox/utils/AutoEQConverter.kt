package cf.thebone.ddctoolbox.utils

import android.util.Log
import cf.thebone.ddctoolbox.model.FilterItem
import cf.thebone.ddctoolbox.model.FilterType
import com.google.code.regexp.Pattern
import java.lang.Math.pow
import kotlin.math.*

data class AEQConversionResult(val data: Any, val failedCount: Int);

object AutoEQConverter {

    fun toFilterItems(input: String): AEQConversionResult{
        val filters = ArrayList<FilterItem>()
        var failed = 0

        for( line in input.lines()){
            if(line.isBlank() || line.trimStart().startsWith("#"))
                continue

            val p = Pattern.compile("""(Filter\s\d+[\s\S][^PK]*PK\s+Fc\s+(?<hz>\d+)\s*Hz\s*Gain\s*(?<db>-?\d*.?\d+)\s*dB\s*Q\s*(?<q>-?\d*.?\d+))""")
            val m = p.matcher(line)

            if(!m.matches()){
                Log.w("AutoEQConverter", "Does not match with regex template: '${line}'")
                failed += 1
                continue
            }

            val freq = m.group("hz").toIntOrNull()
            val gain = m.group("db").toDoubleOrNull()
            val q = m.group("q").toDoubleOrNull()

            if( freq == null || gain == null || q == null){
                Log.w("AutoEQConverter", "Unable to parse values: '${line}'")
                failed += 1
                continue;
            }

            if(freq < 0) {
                Log.w("AutoEQConverter", "Abnormal frequency value: '${line}'")
                failed += 1
                continue
            }

            /* Q to Bandwidth */
            val qq1st = ((2 * q * q) + 1) / (2 * q * q);
            val qq2nd = pow(2 * qq1st, 2.0) / 4;
            val bw = (1000000 * ln(qq1st+sqrt(qq2nd - 1)) / ln(2.0)).roundToInt() / 1000000.0;

            filters.add(FilterItem(FilterType.PEAKING, freq, bw, gain));
        }

        return AEQConversionResult(filters, failed)
    }
}