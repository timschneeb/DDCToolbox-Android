package cf.thebone.ddctoolbox.adapter

import cf.thebone.ddctoolbox.model.FilterItem

class FilterComparator : Comparator<FilterItem> {
    override fun compare(f1: FilterItem, f2: FilterItem): Int {
        val freq1 = f1.filter.frequency ?: 0
        val freq2 = f2.filter.frequency ?: 0
        return freq1.compareTo(freq2)
    }
}