package cf.thebone.ddctoolbox.model

enum class FilterType {
    PEAKING,
    LOWPASS,
    HIGHPASS,
    BANDPASS,
    BANDPASS2,
    NOTCH,
    ALLPASS,
    LOWSHELF,
    HIGHSHELF,
    UNITYGAIN,
    ONEPOLE_LOWPASS,
    ONEPOLE_HIGHPASS,
    CUSTOM,
    INVALID;
    companion object {
        fun toString(type: FilterType?): String {
            return when (type){
                PEAKING -> "Peaking"
                LOWPASS -> "Low Pass"
                HIGHPASS -> "High Pass"
                BANDPASS -> "Band Pass"
                BANDPASS2 -> "Band Pass (peak gain = bw)"
                NOTCH -> "Notch"
                ALLPASS -> "All Pass"
                LOWSHELF -> "Low Shelf"
                HIGHSHELF -> "High Shelf"
                UNITYGAIN -> "Unity Gain"
                ONEPOLE_LOWPASS -> "One-Pole Low Pass"
                ONEPOLE_HIGHPASS -> "One-Pole High Pass"
                CUSTOM -> "Custom"
                INVALID -> "Invalid"
                null -> ""
            }
        }
        fun toFilter(str: String): FilterType {
            return when (str){
                "Peaking" -> PEAKING
                "Low Pass" -> LOWPASS
                "High Pass" -> HIGHPASS
                "Band Pass" -> BANDPASS
                "Band Pass (peak gain = bw)" -> BANDPASS2
                "Notch" -> NOTCH
                "All Pass" -> ALLPASS
                "Low Shelf" -> LOWSHELF
                "High Shelf" -> HIGHSHELF
                "Unity Gain" -> UNITYGAIN
                "One-Pole Low Pass" -> ONEPOLE_LOWPASS
                "One-Pole High Pass" -> ONEPOLE_HIGHPASS
                "Custom" -> CUSTOM
                else -> INVALID
            }
        }
    }
}