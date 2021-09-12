package cf.thebone.ddctoolbox.file.io

import java.io.File
import java.io.IOException

object GenericFileIO {
    fun read(path: String): String?{
        return try{
            File(path).readText()
        } catch(e: IOException){
            null
        }
    }
}