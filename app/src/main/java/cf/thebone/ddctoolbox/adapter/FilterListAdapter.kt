package cf.thebone.ddctoolbox.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import cf.thebone.ddctoolbox.MainActivity
import cf.thebone.ddctoolbox.R
import cf.thebone.ddctoolbox.fragments.FilterEditorFragment
import cf.thebone.ddctoolbox.model.FilterItem
import cf.thebone.ddctoolbox.model.FilterType
import cf.thebone.ddctoolbox.utils.FilterArrayUtils
import kotlinx.android.synthetic.main.filter_item.view.*
import kotlinx.android.synthetic.main.filter_item_content.view.*
import java.util.*

class FilterListAdapter(items: ArrayList<FilterItem>, private val ctx: Context) :
    ArrayAdapter<FilterItem>(ctx, R.layout.filter_item, items) {

    var onChangedListener: ((FilterListAdapter)->Unit)? = null

    fun isListEmpty(): Boolean {
        for(i in 0 until count)
            if((getItem(i) as FilterItem).filter.type == FilterType.INVALID)
                return true
        return false
    }

    override fun notifyDataSetChanged() {
        onChangedListener?.invoke(this)
        super.notifyDataSetChanged()
    }

    override fun getView(i: Int, _view: View?, viewGroup: ViewGroup): View {
        ctx as MainActivity

        var view = _view

        if (view == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.filter_item, viewGroup, false)

            view.header.setOnClickListener {
                if(getItem(i)?.filter?.type ?: FilterType.INVALID != FilterType.INVALID)
                    view.expandableLayout.toggle(true)
            }
        }

        var filterItem = getItem(i)
        view?.headerText?.text = filterItem?.buildDescription()
        
        val adapter = this
        if(filterItem?.filter?.type == FilterType.INVALID) {
            view?.expandableLayout?.collapse()
            view?.headerText?.text = context.getString(R.string.empty_project)
        }
        
        view?.infoFilter?.setOnClickListener {
            if (filterItem != null) {
                var stableMsg: String
                if(filterItem!!.filter.type == FilterType.CUSTOM) {
                    stableMsg = when (filterItem!!.filter.isStable) {
                        0 -> context.getString(
                            R.string.filter_stablity_pole_simple_outside_unit_circle,
                            FilterType.toString(filterItem!!.filter.type)
                        )
                        1 -> context.getString(R.string.filter_stablity_stable)
                        2 -> context.getString(
                            R.string.filter_stablity_pole_simple_approaching_unit_circle,
                            FilterType.toString(filterItem!!.filter.type)
                        )
                        else -> context.getString(R.string.filter_stablity_unknown)
                    }
                    stableMsg += "\n\n44100Hz:\n"
                    stableMsg += filterItem!!.filter.custom441?.buildDescription()
                    stableMsg += "\n48000Hz:\n"
                    stableMsg += filterItem!!.filter.custom48?.buildDescription()
                } else {
                    stableMsg = when (filterItem!!.filter.isStable) {
                        0 -> context.getString(
                            R.string.filter_stablity_pole_outside_unit_circle,
                            FilterType.toString(filterItem!!.filter.type),
                            filterItem!!.filter.frequency
                        )
                        1 -> context.getString(R.string.filter_stablity_stable)
                        2 -> context.getString(
                            R.string.filter_stablity_pole_approaching_unit_circle,
                            FilterType.toString(filterItem!!.filter.type),
                            filterItem!!.filter.frequency
                        )
                        else -> context.getString(R.string.filter_stablity_unknown)
                    }
                }
                val builder = AlertDialog.Builder(context)
                builder.setMessage(stableMsg)
                builder.setTitle(context.getString(R.string.filter_stability))
                builder.setIcon(R.drawable.ic_info_outline)
                builder.setCancelable(true)
                builder.setPositiveButton(android.R.string.ok, null)
                builder.create().show()
            }
        }
        view?.editFilter?.setOnClickListener {
            val dialog = FilterEditorFragment()
            dialog.localizedTitle = context.getString(R.string.edit_filter)
            dialog.filterItem = filterItem
            dialog.show(ctx.supportFragmentManager,dialog::javaClass.name)

            dialog.savedListener = {
                if(dialog.filterItem != null){
                    filterItem = dialog.filterItem
                    val prevState = FilterArrayUtils.bundleFilterItems(adapter)

                    adapter.remove(getItem(i))
                    adapter.insert(filterItem,i)

                    ctx.undoStack.pushCommand(
                        prevState,
                        FilterArrayUtils.bundleFilterItems(adapter),
                        context.getString(R.string.edit_filter))

                    view.headerText?.text = filterItem?.buildDescription()
                    adapter.remove(getItem(i))
                    adapter.insert(filterItem,i)
                    ctx.plotEngine.populatePlot(ctx.getSelectedPlotType())
                    adapter.sort(FilterComparator())
                }
            }
        }
        view?.deleteFilter?.setOnClickListener {
            val prevState = FilterArrayUtils.bundleFilterItems(adapter)

            adapter.remove(filterItem)

            ctx.undoStack.pushCommand(
                prevState,
                FilterArrayUtils.bundleFilterItems(adapter),
                context.getString(R.string.delete_filter))

            ctx.plotEngine.populatePlot(ctx.getSelectedPlotType())
            adapter.sort(FilterComparator())
        }

        return view!!
    }
}