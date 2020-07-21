package cf.thebone.ddctoolbox.fragments

import android.annotation.SuppressLint
import android.content.Context
import cf.thebone.ddctoolbox.R
import android.os.Bundle
import android.os.Environment
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import kotlinx.android.synthetic.main.bottom_sheet_deploy_content.*
import kotlinx.android.synthetic.main.bottom_sheet_save_content.*
import kotlinx.android.synthetic.main.bottom_sheet_save_content.projectNameInput
import kotlinx.android.synthetic.main.bottom_sheet_save_content.saveNow
import kotlinx.android.synthetic.main.bottom_sheet_save_fragment.*
import kotlinx.android.synthetic.main.custom_header.*
import kotlinx.android.synthetic.main.custom_header.view.*
import kotlinx.android.synthetic.main.custom_header.view.projectName
import java.io.File


class DeployFileFragment(private val ctx: Context) : BottomSheetDialogFragment() {

    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null

    @SuppressLint("ShowToast")
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
            header.text = getString(R.string.menu_deploy_vdc)

            header.setOnClickListener { dismiss() }

            projectNameInput.hint = getString(R.string.vdc_name)
            projectNameInput.text = SpannableStringBuilder(
                StringUtils.stripExtension(ctx.projectManager.currentProjectName,".vdcprj")
            )

            saveNow.setOnClickListener {
                if(projectNameInput.text.isEmpty()){
                    projectNameInput.error = getString(R.string.prompt_enter_project_name)
                    return@setOnClickListener
                }

                val filters = arrayListOf<FilterItem>()
                for(i in 0 until ctx.listView.adapter.count)
                    filters.add(ctx.listView.adapter.getItem(i) as FilterItem)

                val targets: ArrayList<File> = arrayListOf()
                if(target_jamesdsp.isChecked){
                    targets.add(File(Environment.getExternalStorageDirectory(),"JamesDSP/DDC"))
                }
                if(target_viper.isChecked){
                    targets.add(File(Environment.getExternalStorageDirectory(),"Android/data/com.pittvandewitt.viperfx/files/DDC"))
                }
                if(target_viper_legacy.isChecked){
                    targets.add(File(Environment.getExternalStorageDirectory(),"ViPER4Android/DDC"))
                }

                if(targets.isEmpty()){
                    val builder = AlertDialog.Builder(context!!)
                    builder.setMessage(getString(R.string.deploy_no_target_desc))
                    builder.setTitle(getString(R.string.deploy_no_target))
                    builder.setIcon(R.drawable.ic_error_outline)
                    builder.setCancelable(true)
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.create().show()
                    return@setOnClickListener;
                }

                for(target in targets){
                    if(!target.exists())
                        target.mkdirs()

                    val file = File(target,projectNameInput.text.toString())
                    ctx.projectManager.exportVDC(file.absolutePath,filters)
                }

                Toast.makeText(context, "Deployed", Toast.LENGTH_SHORT).show()
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
        return inflater.cloneInContext(contextThemeWrapper).inflate(R.layout.bottom_sheet_deploy_fragment
            , container, false)
    }
}