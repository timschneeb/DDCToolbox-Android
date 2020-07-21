package cf.thebone.ddctoolbox.fragments

import android.content.Context
import cf.thebone.ddctoolbox.R
import android.os.Bundle
import android.os.Environment
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import cf.thebone.ddctoolbox.MainActivity
import cf.thebone.ddctoolbox.adapter.FilterListAdapter
import cf.thebone.ddctoolbox.file.ProjectManager
import cf.thebone.ddctoolbox.file.io.ProjectWriter
import cf.thebone.ddctoolbox.model.FilterItem
import cf.thebone.ddctoolbox.utils.StringUtils
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.view.FilePickerDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_save_content.*
import kotlinx.android.synthetic.main.bottom_sheet_save_fragment.*
import kotlinx.android.synthetic.main.custom_header.*
import kotlinx.android.synthetic.main.custom_header.view.*
import kotlinx.android.synthetic.main.custom_header.view.projectName
import java.io.File


class SaveAsFileFragment(private val ctx: Context, private val mode: Mode) : BottomSheetDialogFragment() {

    enum class Mode{
        SaveAs,
        ExportVDC
    }

    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null

    override fun onStart() {
        super.onStart()
        view?.run {
            val parent = view?.parent as View
            parent.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_bottomsheet)
        }

        if (dialog != null) {
            (ctx as MainActivity)
            (ctx.listView.adapter as FilterListAdapter)

            val bottomSheet = dialog!!.findViewById<View>(R.id.design_bottom_sheet)

            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            header.text = if(mode == Mode.SaveAs) getString(R.string.menu_save_as)
            else getString(R.string.menu_export_as_vdc)

            header.setOnClickListener { dismiss() }

            val defaultVDCdir = File(Environment.getExternalStorageDirectory(),"ViPER4Android/DDC")
            if(mode == Mode.ExportVDC){
                directoryInput.text = SpannableStringBuilder(if(defaultVDCdir.exists()) defaultVDCdir.toString()
                else Environment.getExternalStorageDirectory().toString())

                projectNameInput.hint = getString(R.string.vdc_name)
                projectNameInput.text = SpannableStringBuilder(
                    StringUtils.stripExtension(ctx.projectManager.currentProjectName,".vdcprj")
                )
            }
            else {
                directoryInput.text =
                    SpannableStringBuilder(ctx.projectManager.currentDirectoryName)
                projectNameInput.hint = getString(R.string.project_name)
                projectNameInput.text = SpannableStringBuilder(ctx.projectManager.currentProjectName)
            }

            browseOutDirectory.setOnClickListener{
                val properties = DialogProperties()
                properties.selection_mode = DialogConfigs.SINGLE_MODE
                properties.selection_type = DialogConfigs.DIR_SELECT
                properties.root = Environment.getExternalStorageDirectory()
                properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
                properties.offset = File(DialogConfigs.DEFAULT_DIR)
                properties.show_hidden_files = false
                properties.extensions = null

                val dialog = FilePickerDialog(activity, properties)
                dialog.setTitle(getString(R.string.select_directory))

                dialog.setDialogSelectionListener{
                    if(it.isEmpty()) return@setDialogSelectionListener
                    directoryInput.text = SpannableStringBuilder(it.first())
                }
                dialog.show()
            }
            saveNow.setOnClickListener {
                if(directoryInput.text.isEmpty()){
                    directoryInput.error = getString(R.string.invalid_path)
                    return@setOnClickListener
                }

                val directory = File(directoryInput.text.toString())
                if(!directory.exists() || !directory.isDirectory){
                    directoryInput.error = getString(R.string.directory_not_found)
                    return@setOnClickListener
                }
                if(projectNameInput.text.isEmpty()){
                    projectNameInput.error = getString(R.string.prompt_enter_project_name)
                    return@setOnClickListener
                }

                val file = File(directoryInput.text.toString(),projectNameInput.text.toString())

                val filters = arrayListOf<FilterItem>()
                for(i in 0 until ctx.listView.adapter.count)
                    filters.add(ctx.listView.adapter.getItem(i) as FilterItem)

                if(mode == Mode.SaveAs){
                    ctx.projectManager.saveAs(file.absolutePath,filters)
                    ctx.customHeader.projectName.text = ctx.projectManager.currentProjectName
                }
                else ctx.projectManager.exportVDC(file.absolutePath,filters)
                dismiss()
            }
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
        return inflater.cloneInContext(contextThemeWrapper).inflate(R.layout.bottom_sheet_save_fragment
            , container, false)
    }

}