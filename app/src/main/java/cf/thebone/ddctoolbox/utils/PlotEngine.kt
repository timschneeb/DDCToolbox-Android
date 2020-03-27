package cf.thebone.ddctoolbox.utils

import cf.thebone.ddctoolbox.adapter.FilterListAdapter
import cf.thebone.ddctoolbox.model.FilterItem
import cf.thebone.ddctoolbox.model.FilterItemContainer
import cf.thebone.ddctoolbox.model.PlotType
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class PlotEngine(val plot: GraphView?, val adapter: FilterListAdapter){
    fun initializePlot(xAxis: String, yAxis: String){
        plot?.viewport?.setMinX(log10(20.0))
        plot?.viewport?.setMaxX(log10(24000.0))
        plot?.gridLabelRenderer?.numHorizontalLabels = 4
        plot?.gridLabelRenderer?.horizontalAxisTitle = xAxis
        plot?.gridLabelRenderer?.verticalAxisTitle = yAxis
        plot?.gridLabelRenderer?.setHumanRounding(true)
        plot?.gridLabelRenderer?.setHorizontalLabelsAngle(0)

        plot?.viewport?.isXAxisBoundsManual = true
        plot?.viewport?.isYAxisBoundsManual = true
        plot?.viewport?.isScalable = true

        plot?.gridLabelRenderer?.labelFormatter = (object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (!isValueX) {
                    super.formatLabel(value, isValueX)
                } else {
                    val xValue = super.formatLabel(value, isValueX).replace(",".toRegex(), ".")
                    val xLogValue =
                        floor(10.0.pow(xValue.toDouble()))
                            .toFloat()
                    if (value <= 0) "0" else xLogValue.toInt().toString()
                }
            }
        })

        plot?.viewport?.setMinimalViewport(log10(20.0), log10(24000.0),-20.0,20.0)
        plot?.invalidate()
    }

    fun populatePlot(type: PlotType){
        plot?.removeAllSeries()

        plot?.viewport?.setMinimalViewport(log10(20.0), log10(24000.0),-20.0,20.0)
        plot?.viewport?.setMinX(log10(20.0))
        plot?.viewport?.setMaxX(log10(24000.0))

        val allItems = ArrayList<FilterItem>()
        for(i in 0 until adapter.count)
            allItems.add(adapter.getItem(i)!!)

        val container = FilterItemContainer(allItems)
        val response = when(type) {
            PlotType.MAGNITUDE_RESPONSE ->
                container.getMagnitudeResponseTable(8192*2, 48000.0)
            PlotType.PHASE_RESPONSE ->
                container.getPhaseResponseTable(8192*2, 48000.0)
            PlotType.GROUP_DELAY ->
                container.getGroupDelayTable(8192*2, 48000.0)
            else -> return
        }

        val num3: Double = 48000.0 / 2.0 / (8192*2)
        val series = LineGraphSeries<DataPoint>()

        var minY = -10.0
        var maxY = 10.0

        for (i in 0 until response.size) {
            series.appendData(
                DataPoint(log10((num3 * (i + 1.0))), response[i]),
                true, response.size
            )
            if(response[i] > maxY)
                maxY = response[i]
            else if(response[i] < minY)
                minY = response[i]
        }
        plot?.viewport?.setMinY(ceil(minY))
        plot?.viewport?.setMaxY(ceil(maxY))

        plot?.addSeries(series)
        plot?.invalidate()
    }
}