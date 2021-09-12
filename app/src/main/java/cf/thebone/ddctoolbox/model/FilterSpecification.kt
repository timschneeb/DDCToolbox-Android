package cf.thebone.ddctoolbox.model

import java.io.Serializable

class FilterSpecification(
    type: FilterType?
) : Serializable {
    var requiresFrequency: Boolean = false
    var requiresBandwidth: Boolean = false
    var requiresGain: Boolean = false

    init{
        when (type) {
            FilterType.PEAKING -> { requiresFrequency = true; requiresBandwidth = true; requiresGain = true}
            FilterType.LOWPASS -> { requiresFrequency = true; requiresBandwidth = true }
            FilterType.HIGHPASS -> { requiresFrequency = true; requiresBandwidth = true }
            FilterType.BANDPASS -> { requiresFrequency = true; requiresBandwidth = true }
            FilterType.BANDPASS2 -> { requiresFrequency = true; requiresBandwidth = true }
            FilterType.NOTCH -> { requiresFrequency = true; requiresBandwidth = true }
            FilterType.ALLPASS -> { requiresFrequency = true; requiresBandwidth = true }
            FilterType.LOWSHELF -> { requiresFrequency = true; requiresBandwidth = true; requiresGain = true}
            FilterType.HIGHSHELF -> { requiresFrequency = true; requiresBandwidth = true; requiresGain = true}
            FilterType.UNITYGAIN -> requiresGain = true
            FilterType.ONEPOLE_LOWPASS -> requiresFrequency = true
            FilterType.ONEPOLE_HIGHPASS -> requiresFrequency = true
            else -> {}
        }
    }
}