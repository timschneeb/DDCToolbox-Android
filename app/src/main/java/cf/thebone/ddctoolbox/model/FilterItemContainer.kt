package cf.thebone.ddctoolbox.model

class FilterItemContainer(private val list: ArrayList<FilterItem>) {
    fun getMagnitudeResponseTable(bandCount: Int, sampleRate: Double): ArrayList<Double> {
        val table: ArrayList<Double> = ArrayList()
        if(bandCount < 1)
            return table;

        for (i in 0..bandCount)
        {
            val num3: Double = (sampleRate / 2.0) / bandCount
            var value = 0.0
            for (j in 0 until list.size) {
                if(list[j].isValid())
                    value += (list[j].filter.GainAt(num3 * (i + 1.0), sampleRate))
            }
            table.add(value)
        }
        return table
    }
    fun getPhaseResponseTable(bandCount: Int, sampleRate: Double): ArrayList<Double> {
        val table: ArrayList<Double> = ArrayList()
        if(bandCount < 1)
            return table;

        for (i in 0..bandCount)
        {
            val num3: Double = (sampleRate / 2.0) / bandCount
            var value = 0.0
            for (j in 0 until list.size)
                if(list[j].isValid())
                    value += (list[j].filter.PhaseResponseAt(num3 * (i + 1.0), sampleRate))
            table.add(value)
        }
        return table
    }
    fun getGroupDelayTable(bandCount: Int, sampleRate: Double): ArrayList<Double> {
        val table: ArrayList<Double> = ArrayList()
        if(bandCount < 1)
            return table;

        for (i in 0..bandCount)
        {
            val num3: Double = (sampleRate / 2.0) / bandCount
            var value = 0.0
            for (j in 0 until list.size)
                if(list[j].isValid())
                    value += (list[j].filter.GroupDelayAt(num3 * (i + 1.0), sampleRate))
            table.add(value)
        }
        return table
    }
}