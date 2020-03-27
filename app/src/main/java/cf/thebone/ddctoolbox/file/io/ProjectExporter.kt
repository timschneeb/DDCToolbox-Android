package cf.thebone.ddctoolbox.file.io

import cf.thebone.ddctoolbox.model.FilterItem
import java.io.File
import java.io.IOException

class ProjectExporter {

    fun writeFile(path: String, items: ArrayList<FilterItem>): Boolean{
        try{
            val file = File(path)
            val contents = writeString(items)
            file.writeText(contents)
        }
        catch(e: IOException){
            return false
        }
        return true
    }

    fun writeString(items: ArrayList<FilterItem>): String{
        if(items.isEmpty()) return ""

        var lines = "SR_44100:"
        for((counter, item) in items.withIndex()){
            lines += writeSingleFilter(item,44100.0)
            if(counter != items.size - 1)
                lines += ","
        }

        lines += "\nSR_48000:"
        for((counter, item) in items.withIndex()){
            lines += writeSingleFilter(item,48000.0)
            if(counter != items.size - 1)
                lines += ","
        }
        return lines
    }

    fun writeSingleFilter(item: FilterItem, samplerate: Double): String{
        var str = ""
        val coeffs = item.filter.ExportCoeffs(samplerate)
        for((counter, coeff) in coeffs.withIndex()) {
            str += coeff.toString()
            if(counter != coeffs.size - 1)
                str += ","
        }
        return str
    }

}