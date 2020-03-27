package cf.thebone.ddctoolbox.file.io

import android.content.Context
import android.util.Log
import cf.thebone.ddctoolbox.model.CustomFilterUnit
import cf.thebone.ddctoolbox.model.FilterItem
import cf.thebone.ddctoolbox.model.FilterType
import java.io.File
import java.io.IOException
import java.lang.NumberFormatException

class ProjectParser(private val context: Context) {

    fun parseFile(path: String): ArrayList<FilterItem>?{
        try{
            val file = File(path)
            val contents = file.readText()
            if(contents.isNotEmpty())
                return parseMultipleLines(contents)
        }
        catch(e: IOException){
            return null
        }
        return null
    }

    fun parseMultipleLines(lines: String): ArrayList<FilterItem>{
        val list: ArrayList<FilterItem> = arrayListOf()
        for(line in lines.lines()){
            val parsedLine = parseSingleLine(line.trim()) ?: continue
            if(!parsedLine.isValid()) continue
            list.add(parsedLine)
        }
        return list
    }

    fun parseSingleLine(line: String): FilterItem?{
        val item = FilterItem()
        var isCustomLine = false
        val baseParameters: List<String>

        if(line.isNotEmpty() && !line.startsWith("#")){
            try {
                //Store freq, bw,and gain to string array "baseParameters"
                if (line.contains(";")) {
                    isCustomLine = true
                    baseParameters = line.split(";")[0].split(",")
                } else
                    baseParameters = line.split(",")

                //Check if "baseParameters" is filled out correctly
                if (baseParameters.isEmpty()) return null

                //Handle legacy file formats
                if (baseParameters.size == 3) {
                    item.filter.RefreshFilter(
                        FilterType.PEAKING,
                        baseParameters[2].toDouble(),
                        baseParameters[0].toInt(),
                        48000.0,
                        baseParameters[1].toDouble()
                    )
                }
                //Handle standard v4 file format
                else if (baseParameters.size == 4) {
                    //Handle custom filter
                    if (isCustomLine) {
                        val coeffs = line.split(";")[1]
                        val c441 = CustomFilterUnit()
                        val c48 = CustomFilterUnit()

                        for ((counter, coeff) in coeffs.split(",").withIndex()) {
                            when (counter) {
                                0 -> c441.a0 = coeff.toDouble()
                                1 -> c441.a1 = coeff.toDouble()
                                2 -> c441.a2 = coeff.toDouble()
                                3 -> c441.b0 = coeff.toDouble()
                                4 -> c441.b1 = coeff.toDouble()
                                5 -> c441.b2 = coeff.toDouble()
                                6 -> c48.a0 = coeff.toDouble()
                                7 -> c48.a1 = coeff.toDouble()
                                8 -> c48.a2 = coeff.toDouble()
                                9 -> c48.b0 = coeff.toDouble()
                                10 -> c48.b1 = coeff.toDouble()
                                11 -> c48.b2 = coeff.toDouble()
                            }
                        }

                        item.filter.RefreshFilter(
                            c441,
                            c48
                        )
                    }
                    //Handle any other filter type
                    else {
                        item.filter.RefreshFilter(
                            FilterType.toFilter(baseParameters[3].trim()),
                            baseParameters[2].toDouble(),
                            baseParameters[0].toInt(),
                            48000.0,
                            baseParameters[1].toDouble()
                        )
                    }
                }
                //Invalid format, return null
                else return null
            }
            catch (e: NumberFormatException){
                Log.e("ProjectParser","Failed to parse line: '$line'")
                e.printStackTrace()
                return null
            }
        }
        //Empty or commented line
        else
            return null

        return item
    }
}