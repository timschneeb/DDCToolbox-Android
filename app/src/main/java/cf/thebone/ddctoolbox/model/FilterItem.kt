package cf.thebone.ddctoolbox.model

import Biquad
import java.io.Serializable

class FilterItem() : Serializable {
    var filter: Biquad = Biquad()
    constructor(type: FilterType = FilterType.INVALID,
                frequency: Int?, bandwidthOrSlope: Double?, gain: Double?,
                custom441: CustomFilterUnit?, custom48: CustomFilterUnit?) : this(){
        if(filter.type == FilterType.CUSTOM)
            filter.RefreshFilter(custom441!!,custom48!!)
        else
            filter.RefreshFilter(type,gain,frequency,48000.0,bandwidthOrSlope)
    }
    constructor(type: FilterType) : this(type,null,null,null,CustomFilterUnit(),CustomFilterUnit())
    constructor(type: FilterType, frequency: Int?) : this(type,frequency,null,null,CustomFilterUnit(),CustomFilterUnit())
    constructor(type: FilterType, gain: Double?) : this(type,null,null,gain,CustomFilterUnit(),CustomFilterUnit())
    constructor(type: FilterType, frequency: Int?, bandwidth: Double?) : this(type,frequency,bandwidth,null,CustomFilterUnit(),CustomFilterUnit())
    constructor(type: FilterType, frequency: Int?, bandwidth: Double?, gain: Double?): this(type,frequency,bandwidth,gain,CustomFilterUnit(),CustomFilterUnit())
    constructor(type: FilterType = FilterType.CUSTOM, custom441: CustomFilterUnit, custom48: CustomFilterUnit) : this(type,null,null,null,custom441,custom48)

    fun handleNullStates(){
        when{
            filter.frequency == null -> filter.frequency = 0
            filter.bandwidthOrSlope == null -> filter.bandwidthOrSlope = 0.0
            filter.gain == null -> filter.gain = 0.0
        }
    }

    fun buildDescription(): String{
        val typeString = FilterType.toString(filter.type)
        return when (filter.type){
            FilterType.PEAKING -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW, ${filter.gain}dB)"
            FilterType.LOWPASS -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW)"
            FilterType.HIGHPASS -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW)"
            FilterType.BANDPASS -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW)"
            FilterType.BANDPASS2 -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW)"
            FilterType.NOTCH -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW)"
            FilterType.ALLPASS -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW)"
            FilterType.LOWSHELF -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW, ${filter.gain}dB)"
            FilterType.HIGHSHELF -> "${filter.frequency}Hz: $typeString (${filter.bandwidthOrSlope} BW, ${filter.gain}dB)"
            FilterType.UNITYGAIN -> "$typeString (${filter.gain}dB)"
            FilterType.ONEPOLE_LOWPASS -> "${filter.frequency}Hz: $typeString"
            FilterType.ONEPOLE_HIGHPASS -> "${filter.frequency}Hz: $typeString"
            else -> typeString
        }
    }
    fun isValid(): Boolean{
        if ((filter.type == FilterType.INVALID) or (filter.type == null)) return false
        else if(getSpecification().requiresFrequency && filter.frequency == null) return false
        else if(getSpecification().requiresBandwidth && filter.bandwidthOrSlope == null) return false
        else if(getSpecification().requiresGain && filter.gain == null) return false
        return true
    }
    fun getSpecification(): FilterSpecification {
        return FilterSpecification(filter.type)
    }
}