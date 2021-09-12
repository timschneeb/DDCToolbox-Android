package cf.thebone.ddctoolbox.utils

object StringUtils {
    fun stripExtension(input: String, extension: String): String{
        if(input.endsWith(extension))
            return input.substring(0, input.length - extension.length)
        return input
    }
}