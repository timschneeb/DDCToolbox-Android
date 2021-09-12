import java.util.*

import cf.thebone.ddctoolbox.model.CustomFilterUnit
import cf.thebone.ddctoolbox.model.FilterSpecification
import cf.thebone.ddctoolbox.model.FilterType
import cf.thebone.ddctoolbox.utils.RefObject
import java.io.Serializable

class Biquad : Serializable {

    private var internalBiquadCoeffs = DoubleArray(5)
    var bandwidthOrSlope: Double? = null
    var frequency: Int? = null
    var gain: Double? = null
    var type: FilterType? = null
        private set
    var isStable: Int = 3
        private set
    var custom441: CustomFilterUnit? = null
        private set
    var custom48: CustomFilterUnit? = null
        private set

    init {
        internalBiquadCoeffs[0] = 0.0
        internalBiquadCoeffs[1] = 0.0
        internalBiquadCoeffs[2] = 0.0
        internalBiquadCoeffs[3] = 0.0
        internalBiquadCoeffs[4] = 0.0
    }

    fun RefreshFilter(
        newType: FilterType,
        dbGain: Double?,
        centreFreq: Int?,
        fs: Double,
        dBandwidthOrS: Double?
    ) {
        type = newType
        gain = dbGain
        frequency = centreFreq
        bandwidthOrSlope = dBandwidthOrS

        val specs = FilterSpecification(type)

        var d: Double? = null
        if (specs.requiresGain && (type == FilterType.PEAKING || type == FilterType.LOWSHELF || type == FilterType.HIGHSHELF)) {
            d = Math.pow(10.0, dbGain!! / 40.0)
        } else if (specs.requiresGain){
            d = Math.pow(10.0, dbGain!! / 20.0)
        }

        var a: Double? = null
        var sn: Double? = null
        var cs: Double? = null
        if(specs.requiresFrequency){
            a = 6.2831853071795862 * centreFreq!! / fs //omega
            sn = Math.sin(a)
            cs = Math.cos(a)
        }

        var alpha: Double? = null
        if (specs.requiresFrequency && (type == FilterType.HIGHSHELF || type == FilterType.LOWSHELF) && specs.requiresGain) { // S
            //alpha = sn!! / 2 * Math.sqrt((d!! + 1 / d) * (1 / bandwidthOrSlope!! - 1) + 2)
                var q: Float = Math.round(1000000f*Math.pow(2.0,bandwidthOrSlope!! * 0.5)/(Math.pow(
                    2.0,bandwidthOrSlope!!)-1))/1000000f; // convert bw to q
                alpha = sn!! / 2 * Math.sqrt((d!! + 1 / d) * (1 / q - 1) + 2);
        }
        else if(specs.requiresFrequency && specs.requiresBandwidth) // BW
            alpha = sn!! * Math.sinh(0.693147180559945309417 / 2 * bandwidthOrSlope!! * a!! / sn)

        var beta: Double? = null
        if(specs.requiresFrequency && specs.requiresBandwidth && specs.requiresGain)
            beta = 2.0 * Math.sqrt(d!!) * alpha!!


        var B0 = 0.0
        var B1 = 0.0
        var B2 = 0.0
        var A0 = 0.0
        var A1 = 0.0
        var A2 = 0.0

        when (type) {
            FilterType.LOWPASS -> {
                B0 = (1.0 - cs!!) / 2.0
                B1 = 1.0 - cs
                B2 = (1.0 - cs) / 2.0
                A0 = 1.0 + alpha!!
                A1 = -2.0 * cs
                A2 = 1.0 - alpha
            }
            FilterType.HIGHPASS -> {
                B0 = (1.0 + cs!!) / 2.0
                B1 = -(1.0 + cs)
                B2 = (1.0 + cs) / 2.0
                A0 = 1.0 + alpha!!
                A1 = -2.0 * cs
                A2 = 1.0 - alpha
            }
            FilterType.BANDPASS -> {
                //BPF, constant skirt gain (peak gain = BW)
                B0 = bandwidthOrSlope!! * alpha!! // sn / 2;
                B1 = 0.0
                B2 = -bandwidthOrSlope!! * alpha //-sn / 2;
                A0 = 1 + alpha
                A1 = -2 * cs!!
                A2 = 1 - alpha
            }
            FilterType.BANDPASS2 -> {
                //BPF, constant 0dB peak gain
                B0 = alpha!!
                B1 = 0.0
                B2 = -alpha
                A0 = 1.0 + alpha
                A1 = -2.0 * cs!!
                A2 = 1.0 - alpha
            }
            FilterType.NOTCH -> {
                B0 = 1.0
                B1 = -2.0 * cs!!
                B2 = 1.0
                A0 = 1.0 + alpha!!
                A1 = -2.0 * cs
                A2 = 1.0 - alpha
            }
            FilterType.ALLPASS -> {
                B0 = 1.0 - alpha!!
                B1 = -2.0 * cs!!
                B2 = 1.0 + alpha
                A0 = 1.0 + alpha
                A1 = -2.0 * cs
                A2 = 1.0 - alpha
            }
            FilterType.PEAKING -> {
                B0 = 1.0 + alpha!! * d!!
                B1 = -2.0 * cs!!
                B2 = 1.0 - alpha * d
                A0 = 1.0 + alpha / d
                A1 = -2.0 * cs
                A2 = 1.0 - alpha / d
            }
            FilterType.LOWSHELF -> {
                B0 = d!! * (d + 1.0 - (d - 1.0) * cs!! + beta!!)
                B1 = 2.0 * d * (d - 1.0 - (d + 1.0) * cs)
                B2 = d * (d + 1.0 - (d - 1.0) * cs - beta)
                A0 = d + 1.0 + (d - 1.0) * cs + beta
                A1 = -2.0 * (d - 1.0 + (d + 1.0) * cs)
                A2 = d + 1.0 + (d - 1.0) * cs - beta
            }
            FilterType.HIGHSHELF -> {
                B0 = d!! * (d + 1.0 + (d - 1.0) * cs!! + beta!!)
                B1 = -2.0 * d * (d - 1.0 + (d + 1.0) * cs)
                B2 = d * (d + 1.0 + (d - 1.0) * cs - beta)
                A0 = d + 1.0 - (d - 1.0) * cs + beta
                A1 = 2.0 * (d - 1.0 - (d + 1.0) * cs)
                A2 = d + 1.0 - (d - 1.0) * cs - beta
            }
            FilterType.UNITYGAIN -> {
                B0 = d!!
                B1 = 0.0
                B2 = 0.0
                A0 = 1.0
                A1 = 0.0
                A2 = 0.0
            }
            FilterType.ONEPOLE_LOWPASS -> {
                B0 = Math.tan(Math.PI * centreFreq!! / fs * 0.5)
                A1 = -(1.0 - B0) / (1.0 + B0)
                B0 = B0 / (1.0 + B0)
                B1 = B0
                B2 = 0.0
                A0 = 1.0
                A2 = 0.0
            }
            FilterType.ONEPOLE_HIGHPASS -> {
                B0 = Math.tan(Math.PI * centreFreq!! / fs * 0.5)
                A1 = -(1.0 - B0) / (1.0 + B0)
                B0 = 1.0 - B0 / (1.0 + B0)
                B2 = 0.0
                B1 = -B0
                A0 = 1.0
                A2 = 0.0
            }
        }


        //Check if filter is stable/usable
        internalBiquadCoeffs[0] = B0 / A0
        internalBiquadCoeffs[1] = B1 / A0
        internalBiquadCoeffs[2] = B2 / A0
        internalBiquadCoeffs[3] = -A1 / A0
        internalBiquadCoeffs[4] = -A2 / A0
        val roots = DoubleArray(4)
        iirroots(-internalBiquadCoeffs[3], -internalBiquadCoeffs[4], roots)
        val pole1Magnitude = Math.sqrt(roots[0] * roots[0] + roots[1] * roots[1])
        val pole2Magnitude = Math.sqrt(roots[2] * roots[2] + roots[3] * roots[3])
        isStable = 0 // Assume all pole is unstable
        if (pole1Magnitude < 1.0 && pole2Magnitude < 1.0)
            if (1.0 - pole1Magnitude < 8e-14 || 1.0 - pole2Magnitude < 8e-14)
                isStable =
                    2 // Not so stable, due to our tool text formatting OR V4A string inaccuracy
            else
                isStable = 1 // Perfectly stable
    }

    fun RefreshFilter(c441: CustomFilterUnit, c48: CustomFilterUnit) {
        custom441 = c441
        custom48 = c48
        type = FilterType.CUSTOM

        internalBiquadCoeffs[0] = custom48!!.b0 / custom48!!.a0
        internalBiquadCoeffs[1] = custom48!!.b1 / custom48!!.a0
        internalBiquadCoeffs[2] = custom48!!.b2 / custom48!!.a0
        internalBiquadCoeffs[3] = -custom48!!.a1 / custom48!!.a0
        internalBiquadCoeffs[4] = -custom48!!.a2 / custom48!!.a0

        //Check if filter is stable/usable
        val roots = DoubleArray(4)
        iirroots(-internalBiquadCoeffs[3], -internalBiquadCoeffs[4], roots)
        val pole1Magnitude = Math.sqrt(roots[0] * roots[0] + roots[1] * roots[1])
        val pole2Magnitude = Math.sqrt(roots[2] * roots[2] + roots[3] * roots[3])
        isStable = 0 // Assume all pole is unstable
        if (pole1Magnitude < 1.0 && pole2Magnitude < 1.0)
            if (1.0 - pole1Magnitude < 8e-14 || 1.0 - pole2Magnitude < 8e-14)
                isStable =
                    2 // Not so stable, due to our tool text formatting OR V4A string inaccuracy
            else
                isStable = 1 // Perfectly stable
    }

    fun ExportCoeffs(
        type: FilterType?,
        dbGain: Double?,
        centreFreq: Int?,
        fs: Double,
        dBandwidthOrQOrS: Double?
    ): LinkedList<Double> {
        val specs = FilterSpecification(type)

        if(specs.requiresFrequency)
            if (centreFreq!! <= 2.2204460492503131e-016 || fs <= 2.2204460492503131e-016)
                return LinkedList()
            else if(fs <= 2.2204460492503131e-016)
                return LinkedList()

        var d: Double? = null
        if (specs.requiresGain && (type == FilterType.PEAKING || type == FilterType.LOWSHELF || type == FilterType.HIGHSHELF)) {
            d = Math.pow(10.0, dbGain!! / 40.0)
        } else if (specs.requiresGain){
            d = Math.pow(10.0, dbGain!! / 20.0)
        }

        var a: Double? = null
        var sn: Double? = null
        var cs: Double? = null
        if(specs.requiresFrequency){
            a = 6.2831853071795862 * centreFreq!! / fs //omega
            sn = Math.sin(a)
            cs = Math.cos(a)
        }

        var alpha: Double? = null
        if (specs.requiresFrequency && (type == FilterType.HIGHSHELF || type == FilterType.LOWSHELF) && specs.requiresGain) // S
            alpha = sn!! / 2 * Math.sqrt((d!! + 1 / d) * (1 / dBandwidthOrQOrS!! - 1) + 2)
        else if(specs.requiresFrequency && specs.requiresBandwidth) // BW
            alpha = sn!! * Math.sinh(0.693147180559945309417 / 2 * dBandwidthOrQOrS!! * a!! / sn)

        var beta: Double? = null
        if(specs.requiresFrequency && specs.requiresBandwidth && specs.requiresGain)
            beta = 2.0 * Math.sqrt(d!!) * alpha!!

        var B0 = 0.0
        var B1 = 0.0
        var B2 = 0.0
        var A0 = 0.0
        var A1 = 0.0
        var A2 = 0.0

        when (type) {
            FilterType.LOWPASS -> {
                B0 = (1.0 - cs!!) / 2.0
                B1 = 1.0 - cs
                B2 = (1.0 - cs) / 2.0
                A0 = 1.0 + alpha!!
                A1 = -2.0 * cs
                A2 = 1.0 - alpha
            }
            FilterType.HIGHPASS -> {
                B0 = (1.0 + cs!!) / 2.0
                B1 = -(1.0 + cs)
                B2 = (1.0 + cs) / 2.0
                A0 = 1.0 + alpha!!
                A1 = -2.0 * cs
                A2 = 1.0 - alpha
            }
            FilterType.BANDPASS -> {
                //BPF, constant skirt gain (peak gain = BW)
                B0 = dBandwidthOrQOrS!! * alpha!! // sn / 2;
                B1 = 0.0
                B2 = -dBandwidthOrQOrS * alpha //-sn / 2;
                A0 = 1 + alpha
                A1 = -2 * cs!!
                A2 = 1 - alpha
            }
            FilterType.BANDPASS2 -> {
                //BPF, constant 0dB peak gain
                B0 = alpha!!
                B1 = 0.0
                B2 = -alpha
                A0 = 1.0 + alpha
                A1 = -2.0 * cs!!
                A2 = 1.0 - alpha
            }
            FilterType.NOTCH -> {
                B0 = 1.0
                B1 = -2.0 * cs!!
                B2 = 1.0
                A0 = 1.0 + alpha!!
                A1 = -2.0 * cs
                A2 = 1.0 - alpha
            }
            FilterType.ALLPASS -> {
                B0 = 1.0 - alpha!!
                B1 = -2.0 * cs!!
                B2 = 1.0 + alpha
                A0 = 1.0 + alpha
                A1 = -2.0 * cs
                A2 = 1.0 - alpha
            }
            FilterType.PEAKING -> {
                B0 = 1.0 + alpha!! * d!!
                B1 = -2.0 * cs!!
                B2 = 1.0 - alpha * d
                A0 = 1.0 + alpha / d
                A1 = -2.0 * cs
                A2 = 1.0 - alpha / d
            }
            FilterType.LOWSHELF -> {
                B0 = d!! * (d + 1.0 - (d - 1.0) * cs!! + beta!!)
                B1 = 2.0 * d * (d - 1.0 - (d + 1.0) * cs)
                B2 = d * (d + 1.0 - (d - 1.0) * cs - beta)
                A0 = d + 1.0 + (d - 1.0) * cs + beta
                A1 = -2.0 * (d - 1.0 + (d + 1.0) * cs)
                A2 = d + 1.0 + (d - 1.0) * cs - beta
            }
            FilterType.HIGHSHELF -> {
                B0 = d!! * (d + 1.0 + (d - 1.0) * cs!! + beta!!)
                B1 = -2.0 * d * (d - 1.0 + (d + 1.0) * cs)
                B2 = d * (d + 1.0 + (d - 1.0) * cs - beta)
                A0 = d + 1.0 - (d - 1.0) * cs + beta
                A1 = 2.0 * (d - 1.0 - (d + 1.0) * cs)
                A2 = d + 1.0 - (d - 1.0) * cs - beta
            }
            FilterType.UNITYGAIN -> {
                B0 = d!!
                B1 = 0.0
                B2 = 0.0
                A0 = 1.0
                A1 = 0.0
                A2 = 0.0
            }
            FilterType.ONEPOLE_LOWPASS -> {
                B0 = Math.tan(Math.PI * centreFreq!! / fs * 0.5)
                A1 = -(1.0 - B0) / (1.0 + B0)
                B0 = B0 / (1.0 + B0)
                B1 = B0
                B2 = 0.0
                A0 = 1.0
                A2 = 0.0
            }
            FilterType.ONEPOLE_HIGHPASS -> {
                B0 = Math.tan(Math.PI * centreFreq!! / fs * 0.5)
                A1 = -(1.0 - B0) / (1.0 + B0)
                B0 = 1.0 - B0 / (1.0 + B0)
                B2 = 0.0
                B1 = -B0
                A0 = 1.0
                A2 = 0.0
            }
        }

        val result = LinkedList<Double>()
        result.addLast(B0 / A0)
        result.addLast(B1 / A0)
        result.addLast(B2 / A0)
        result.addLast(-A1 / A0)
        result.addLast(-A2 / A0)
        return result
    }

    fun ExportCoeffs(dSamplingRate: Double): LinkedList<Double> {
        if (type == FilterType.CUSTOM){
            return if (dSamplingRate.toInt() == 44100)
                ExportCoeffs(type, custom441!!)
            else if (dSamplingRate.toInt() == 48000)
                ExportCoeffs(type, custom48!!)
            else LinkedList()
        }
        else if(type == FilterType.INVALID)
            return LinkedList()

        return ExportCoeffs(
            type,
            gain,
            frequency,
            dSamplingRate,
            bandwidthOrSlope
        )
    }

    fun ExportCoeffs(type: FilterType?, coeffs: CustomFilterUnit): LinkedList<Double> {
        val result = LinkedList<Double>()
        val A0 = coeffs.a0
        result.addLast(coeffs.b0 / A0)
        result.addLast(coeffs.b1 / A0)
        result.addLast(coeffs.b2 / A0)
        result.addLast(-coeffs.a1 / A0)
        result.addLast(-coeffs.a2 / A0)
        return result
    }

    // Provided by: James34602
    fun iirroots(b: Double, c: Double, roots: DoubleArray) {
        val delta = b * b - 4.0 * c
        if (delta >= 0.0) {
            roots[0] = (-b - Math.sqrt(delta)) / 2.0
            roots[1] = 0.0
            roots[2] = (-b + Math.sqrt(delta)) / 2.0
            roots[3] = 0.0
        } else {
            roots[2] = -b / 2.0
            roots[0] = roots[2]
            roots[1] = Math.sqrt(-delta) / 2.0
            roots[3] = -Math.sqrt(-delta) / 2.0
        }
    }

    fun complexResponse(
        centreFreq: Double,
        fs: Double,
        HofZReal: RefObject<Double>,
        HofZImag: RefObject<Double>
    ): Int {
        val Arg: Double
        val z1Real: Double
        val z1Imag: Double
        var z2Real: Double = 0.0
        var z2Imag: Double = 0.0
        val DenomReal: Double
        val DenomImag: Double
        val tmpReal: Double
        val tmpImag: Double
        Arg = Math.PI * centreFreq / (fs * 0.5)
        z1Real = Math.cos(Arg)
        z1Imag = -Math.sin(Arg) // z = e^(j*omega)
        val tempRef_z2Real = RefObject(z2Real)
        val tempRef_z2Imag = RefObject(z2Imag)
        complexMultiplicationRI(
            tempRef_z2Real,
            tempRef_z2Imag,
            z1Real,
            z1Imag,
            z1Real,
            z1Imag
        ) // z squared
        z2Imag = tempRef_z2Imag.argValue
        z2Real = tempRef_z2Real.argValue
        HofZReal.argValue = 1.0
        HofZImag.argValue = 0.0
        tmpReal =
            internalBiquadCoeffs[0] + internalBiquadCoeffs[1] * z1Real + internalBiquadCoeffs[2] * z2Real
        tmpImag = internalBiquadCoeffs[1] * z1Imag + internalBiquadCoeffs[2] * z2Imag
        complexMultiplicationRI(
            HofZReal,
            HofZImag,
            HofZReal.argValue,
            HofZImag.argValue,
            tmpReal,
            tmpImag
        )
        DenomReal = 1.0 + -internalBiquadCoeffs[3] * z1Real + -internalBiquadCoeffs[4] * z2Real
        DenomImag = -internalBiquadCoeffs[3] * z1Imag + -internalBiquadCoeffs[4] * z2Imag
        return if (Math.sqrt(DenomReal * DenomReal + DenomImag * DenomImag) < Math.ulp(1.0)) {
            0 // Division by zero, you know what to do
        } else {
            complexDivisionRI(
                HofZReal,
                HofZImag,
                HofZReal.argValue,
                HofZImag.argValue,
                DenomReal,
                DenomImag
            )
            1
        }
    }

    fun GainAt(centreFreq: Double, fs: Double): Double {
        var HofZReal = 0.0
        var HofZImag = 0.0
        val tempRef_HofZReal = RefObject(HofZReal)
        val tempRef_HofZImag = RefObject(HofZImag)
        val divZero = complexResponse(centreFreq, fs, tempRef_HofZReal, tempRef_HofZImag)
        HofZImag = tempRef_HofZImag.argValue
        HofZReal = tempRef_HofZReal.argValue
        return if (divZero == 0) {
            0.0
        } else {
            20.0 * Math.log10(Math.sqrt(HofZReal * HofZReal + HofZImag * HofZImag + Math.ulp(1.0)))
        }
    }

    fun PhaseResponseAt(centreFreq: Double, fs: Double): Double {
        var HofZReal = 0.0
        var HofZImag = 0.0
        val tempRef_HofZReal = RefObject(HofZReal)
        val tempRef_HofZImag = RefObject(HofZImag)
        val divZero = complexResponse(centreFreq, fs, tempRef_HofZReal, tempRef_HofZImag)
        HofZImag = tempRef_HofZImag.argValue
        HofZReal = tempRef_HofZReal.argValue
        return if (divZero == 0) {
            0.0
        } else {
            Math.atan2(HofZImag, HofZReal) * 180.0 / Math.PI
        }
    }

    // Simplified Shpak group delay algorithm
    // The algorithm only valid when first order / second order IIR filters is provided
    // You must break down high order transfer function into N-SOS in order apply the Shpak algorithm
    // which is out-of-scope here, since break down high order transfer function require find roots of polynomials
    // Root finder may often require the computation of eigenvalue of companion matrix of polynomials
    // Which will bloat 1000+ lines of code, and perhaps not the main purpose here.
    // We just need to calculate group delay of a bunch of second order IIR filters, so the following code already do the job
    // Provided by: James34602
    fun toMs(sample: Double, fs: Double): Double {
        return sample / (fs / 1000.0)
    }

    fun GroupDelayAt(centreFreq: Double, fs: Double): Double {
        val Arg = Math.PI * centreFreq / (fs * 0.5)
        val cw = Math.cos(Arg)
        val cw2 = Math.cos(2.0 * Arg)
        val sw = Math.sin(Arg)
        val sw2 = Math.sin(2.0 * Arg)
        val b1 = internalBiquadCoeffs[0]
        var b2 = internalBiquadCoeffs[1]
        var b3 = internalBiquadCoeffs[2]
        var u = b1 * sw2 + b2 * sw
        var v = b1 * cw2 + b2 * cw + b3
        var du = 2.0 * b1 * cw2 + b2 * cw
        var dv = -(2.0 * b1 * sw2 + b2 * sw)
        var u2v2 =
            b1 * b1 + b2 * b2 + b3 * b3 + 2.0 * (b1 * b2 + b2 * b3) * cw + 2.0 * (b1 * b3) * cw2
        val gdB = 2.0 - (v * du - u * dv) / u2v2
        b2 = -internalBiquadCoeffs[3]
        b3 = -internalBiquadCoeffs[4]
        u = sw2 + b2 * sw
        v = cw2 + b2 * cw + b3
        du = 2.0 * cw2 + b2 * cw
        dv = -(2.0 * sw2 + b2 * sw)
        u2v2 = 1.0 + b2 * b2 + b3 * b3 + 2.0 * (b2 + b2 * b3) * cw + 2.0 * b3 * cw2
        return toMs(gdB - (2.0 - (v * du - u * dv) / u2v2), fs)
    }

    private fun complexMultiplicationRI(
        zReal: RefObject<Double>,
        zImag: RefObject<Double>,
        xReal: Double,
        xImag: Double,
        yReal: Double,
        yImag: Double
    ) {
        zReal.argValue = xReal * yReal - xImag * yImag
        zImag.argValue = xReal * yImag + xImag * yReal
    }

    private fun complexDivisionRI(
        zReal: RefObject<Double>,
        zImag: RefObject<Double>,
        xReal: Double,
        xImag: Double,
        yReal: Double,
        yImag: Double
    ) {
        zReal.argValue = (xReal * yReal + xImag * yImag) / (yReal * yReal + yImag * yImag)
        zImag.argValue = (xImag * yReal - xReal * yImag) / (yReal * yReal + yImag * yImag)
    }
}