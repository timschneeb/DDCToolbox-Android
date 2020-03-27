package cf.thebone.ddctoolbox.file.io

import cf.thebone.ddctoolbox.model.FilterItem
import cf.thebone.ddctoolbox.model.FilterType
import java.io.File
import java.io.IOException

class ProjectWriter {

    fun isBackwardsCompatible(items: ArrayList<FilterItem>): Boolean{
        for (item in items)
            if(item.filter.type != FilterType.PEAKING)
                return false
        return true
    }

    fun writeFile(path: String, items: ArrayList<FilterItem>): Boolean{
        try{
            val file = File(path)
            val contents = writeMultipleLines(items,true)
            file.writeText(contents)
        }
        catch(e: IOException){
            return false
        }
        return true
    }

    fun writeMultipleLines(items: ArrayList<FilterItem>, addHeaderFooter: Boolean): String{
        if(items.isEmpty()) return ""
        val filetypeVersion = if(isBackwardsCompatible(items)) "1.0.0.0a" else "4.0.0.0a"
        var lines = if(addHeaderFooter) "# DDCToolbox Project File, v${filetypeVersion} (@ThePBone)\n" else ""
        for((counter, item) in items.withIndex())
            lines += writeSingleLine(item,counter,isBackwardsCompatible(items))
        lines += if(addHeaderFooter) "# File End" else ""
        return lines
    }

    fun writeSingleLine(item: FilterItem, count: Int, backwardsCompatible: Boolean = false): String {
        item.handleNullStates()
        return when {
            backwardsCompatible -> "# Calibration Point $count\n" +
                    "${item.filter.frequency},${item.filter.bandwidthOrSlope},${item.filter.gain}\n"
            item.filter.type == FilterType.CUSTOM -> "# Calibration Point $count\n" +
                    "0,0,0,${FilterType.toString(item.filter.type)};" +
                    "${item.filter.custom441!!.a0},${item.filter.custom441!!.a1},${item.filter.custom441!!.a2}," +
                    "${item.filter.custom441!!.b0},${item.filter.custom441!!.b1},${item.filter.custom441!!.b2}," +
                    "${item.filter.custom48!!.a0},${item.filter.custom48!!.a1},${item.filter.custom48!!.a2}," +
                    "${item.filter.custom48!!.b0},${item.filter.custom48!!.b1},${item.filter.custom48!!.b2}\n"
            else -> "# Calibration Point $count\n" +
                    "${item.filter.frequency},${item.filter.bandwidthOrSlope},${item.filter.gain},${FilterType.toString(
                        item.filter.type
                    )}\n"
        }
    }
}