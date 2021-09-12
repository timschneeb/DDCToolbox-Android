package cf.thebone.ddctoolbox.utils

import cf.thebone.ddctoolbox.adapter.FilterComparator
import cf.thebone.ddctoolbox.adapter.FilterListAdapter
import cf.thebone.ddctoolbox.model.FilterItem

object FilterArrayUtils {
    fun bundleFilterItems(adapter: FilterListAdapter): ArrayList<FilterItem>{
        if(adapter.count < 1) return ArrayList()
        val bundle: ArrayList<FilterItem> = arrayListOf()
        for(i in 0 until adapter.count)
            if(adapter.getItem(i) != null) bundle.add(adapter.getItem(i)!!)
        return bundle
    }
    fun restoreFilterItems(adapter: FilterListAdapter, items: ArrayList<FilterItem>?) {
        adapter.clear()
        if(items == null || items.size < 1) return
        adapter.addAll(items)
        adapter.sort(FilterComparator())
    }
}