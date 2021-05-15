package cf.thebone.ddctoolbox.fragments

import cf.thebone.ddctoolbox.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import cf.thebone.ddctoolbox.model.FilterItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.bottom_sheet_edit_content.*
import cf.thebone.ddctoolbox.utils.IntRangeInputFilter
import android.text.InputFilter
import android.text.SpannableStringBuilder
import cf.thebone.ddctoolbox.utils.DoubleRangeInputFilter
import kotlinx.android.synthetic.main.bottom_sheet_edit_fragment.*
import androidx.appcompat.app.AlertDialog
import cf.thebone.ddctoolbox.model.FilterSpecification
import cf.thebone.ddctoolbox.model.FilterType
import android.widget.AdapterView

class FilterEditorFragment : BottomSheetDialogFragment() {

    var filterItem: FilterItem? = null
    var localizedTitle: String? = null
        set(value) {
            field = value
            if(header != null) header.text = field
        }

    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null

    var savedListener: (()->Unit)? = null

    override fun onStart() {
        super.onStart()
        view?.run {
            val parent = view?.parent as View
            parent.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_bottomsheet)
        }

        if (dialog != null) {
            val bottomSheet = dialog!!.findViewById<View>(R.id.design_bottom_sheet)

            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            header.text = localizedTitle
            header.setOnClickListener { dismiss() }

            filterType.setMetTextColor(resources.getColor(R.color.primaryText))
            filterType.setTextColor(resources.getColor(R.color.primaryText))
            filterType.setHintTextColor(resources.getColor(R.color.colorHint))

            frequencyInput.filters = arrayOf(IntRangeInputFilter(0, 24000))
            bandwidthInput.filters = arrayOf<InputFilter>(DoubleRangeInputFilter(0.0, 100.0))
            gainInput.filters = arrayOf<InputFilter>(DoubleRangeInputFilter(-40.0, 40.0))

            if(filterItem != null){
                val specs = FilterSpecification(filterItem!!.filter.type)
                filterType.text = SpannableStringBuilder(FilterType.toString(filterItem!!.filter.type))

                if(specs.requiresFrequency)
                    frequencyInput.text = SpannableStringBuilder(filterItem!!.filter.frequency.toString())
                if(specs.requiresBandwidth)
                    bandwidthInput.text = SpannableStringBuilder(filterItem!!.filter.bandwidthOrSlope.toString())
                if(specs.requiresGain)
                    gainInput.text = SpannableStringBuilder(filterItem!!.filter.gain.toString())
            }

            val adapter = ArrayAdapter<String>(
                context!!,
                android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.filter_types)
            )
            filterType.setAdapter(adapter)
            filterType.onItemClickListener =
                AdapterView.OnItemClickListener { parent, view, position, id ->
                    updateInputState()
                    if(FilterType.toFilter(filterType.text.toString()) == FilterType.CUSTOM) {
                        val builder = AlertDialog.Builder(context!!)
                        builder.setMessage(getString(R.string.custom_filter_notice))
                        builder.setTitle(getString(R.string.feature_unavailable))
                        builder.setIcon(R.drawable.ic_error_outline)
                        builder.setCancelable(true)
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.create().show()
                    }
                }

            saveFilter.setOnClickListener{
                var valid = true

                //Validity checks
                if( filterType.text.isEmpty()){
                    val builder = AlertDialog.Builder(context!!)
                    builder.setMessage(getString(R.string.editor_invalid_filter_dialog))
                    builder.setTitle(getString(R.string.editor_invalid_filter_dialog_title))
                    builder.setIcon(R.drawable.ic_error_outline)
                    builder.setCancelable(true)
                    builder.setPositiveButton(android.R.string.ok,null)
                    builder.create().show()
                }
                else {
                    val specs = FilterSpecification(
                        FilterType.toFilter(filterType.text.toString())
                    )

                    if (specs.requiresFrequency && frequencyInput.text.isEmpty()) {
                        frequencyInput.error = getString(R.string.editor_missing_input)
                        valid = false
                    } else if (specs.requiresBandwidth && bandwidthInput.text.isEmpty()) {
                        bandwidthInput.error = getString(R.string.editor_missing_input)
                        valid = false
                    } else if (specs.requiresGain && (gainInput.text.toString() == "-" || gainInput.text.isEmpty())) {
                        gainInput.error = getString(R.string.editor_invalid_input)
                        valid = false
                    }

                    if (valid){
                        var bandwidthOrSlopeOrNull: Double? = null
                        if(specs.requiresBandwidth)
                            bandwidthOrSlopeOrNull = bandwidthInput.text.toString().toDouble()

                        filterItem = FilterItem(FilterType.toFilter(filterType.text.toString()),
                            frequencyInput.text.toString().toIntOrNull(),
                            bandwidthOrSlopeOrNull,
                            gainInput.text.toString().toDoubleOrNull())

                        savedListener?.invoke()
                        dismiss()
                    }

                }
            }
            if(filterType.text.isEmpty())
                updateInputState(FilterType.INVALID)
            else
                updateInputState()
        }

        view?.post {
            val parent = view?.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            mBottomSheetBehavior = behavior as BottomSheetBehavior<*>?
            mBottomSheetBehavior?.peekHeight = view?.measuredHeight!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        return inflater.cloneInContext(contextThemeWrapper).inflate(R.layout.bottom_sheet_edit_fragment
            , container, false)
    }

    private fun updateInputState(filter: FilterType = FilterType.toFilter(filterType.text.toString())){
        val specs = FilterSpecification(filter)
        frequencyInput.isEnabled = specs.requiresFrequency
        bandwidthInput.isEnabled = specs.requiresBandwidth
        gainInput.isEnabled = specs.requiresGain

        frequencyInput.setHintTextColor(resources.getColor(if (specs.requiresFrequency) R.color.colorHint else R.color.midText))
        bandwidthInput.setHintTextColor(resources.getColor(if (specs.requiresBandwidth) R.color.colorHint else R.color.midText))
        gainInput.setHintTextColor(resources.getColor(if (specs.requiresGain) R.color.colorHint else R.color.midText))
    }
}