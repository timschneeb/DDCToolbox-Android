package cf.thebone.ddctoolbox.model

import java.io.Serializable

class CustomFilterUnit(
    var a0: Double = 1.0,
    var a1: Double = 0.0,
    var a2: Double = 0.0,
    var b0: Double = 1.0,
    var b1: Double = 0.0,
    var b2: Double = 0.0
) : Serializable {

    fun buildDescription(): String{
        var str = ""
        str += "a0: $a0, a1: $a1, a2: $a2\n"
        str += "b0: $b0, b1: $b1, b2: $b2\n"
        return str
    }

}