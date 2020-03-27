package cf.thebone.ddctoolbox.model

import cf.thebone.ddctoolbox.utils.SystemUtils
import kotlin.random.Random

object FilterProvider {
    fun getTutorialProject(): ArrayList<FilterItem> {
        val dummyData = ArrayList(ArrayList<FilterItem>())
        dummyData.add(FilterItem(FilterType.PEAKING,37,2.16,5.70))
        dummyData.add(FilterItem(FilterType.PEAKING,195,4.89,-2.30))
        dummyData.add(FilterItem(FilterType.PEAKING,1702,0.55,0.90))
        dummyData.add(FilterItem(FilterType.PEAKING,3455,0.59,15.90))
        dummyData.add(FilterItem(FilterType.PEAKING,4000,0.16,3.70))
        dummyData.add(FilterItem(FilterType.PEAKING,5000,0.35,-3.70))
        dummyData.add(FilterItem(FilterType.PEAKING,9600,0.60,1.60))
        dummyData.add(FilterItem(FilterType.PEAKING,14506,0.31,-1.20))
        dummyData.add(FilterItem(FilterType.PEAKING,19910,0.70,-13.10))
        return dummyData
    }
    fun getDummyData(n: Int): ArrayList<FilterItem> {
        val dummyData = ArrayList(ArrayList<FilterItem>())
        for(i in 0 until n){
            dummyData.add(FilterItem(
                randomFilterType(),
                (1..24000).random(),
                SystemUtils.roundToDecimals(Random.nextDouble(0.1,5.0),2),
                SystemUtils.roundToDecimals(Random.nextDouble(-20.0,20.0),2)
            ))
        }
        return dummyData
    }
    fun randomFilterType(): FilterType{
        var filterType: FilterType? = null
        while(filterType == null ||
            filterType == FilterType.INVALID ||
            filterType == FilterType.CUSTOM){
            filterType = FilterType.values().random()
        }
        return filterType
    }
}