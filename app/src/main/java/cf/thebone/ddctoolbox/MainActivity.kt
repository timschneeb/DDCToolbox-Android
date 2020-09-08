package cf.thebone.ddctoolbox

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import cf.thebone.ddctoolbox.activity.AutoEqSelectionActivity
import cf.thebone.ddctoolbox.adapter.FilterComparator
import cf.thebone.ddctoolbox.adapter.FilterListAdapter
import cf.thebone.ddctoolbox.api.model.AEQDetails
import cf.thebone.ddctoolbox.editor.UndoStack
import cf.thebone.ddctoolbox.file.ProjectManager
import cf.thebone.ddctoolbox.file.io.GenericFileIO
import cf.thebone.ddctoolbox.fragments.DeployFileFragment
import cf.thebone.ddctoolbox.fragments.FilterEditorFragment
import cf.thebone.ddctoolbox.fragments.SaveAsFileFragment
import cf.thebone.ddctoolbox.model.FilterItem
import cf.thebone.ddctoolbox.model.FilterProvider
import cf.thebone.ddctoolbox.model.FilterType
import cf.thebone.ddctoolbox.model.PlotType
import cf.thebone.ddctoolbox.model.instance.MainDataInstance
import cf.thebone.ddctoolbox.utils.*
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.view.FilePickerDialog
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.LibsConfiguration
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.crossfader.Crossfader
import com.mikepenz.crossfader.util.UIUtils
import com.mikepenz.crossfader.view.CrossFadeSlidingPaneLayout
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.MiniDrawer
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.SectionDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.Nameable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.custom_header.view.*
import java.io.File
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private lateinit var result: Drawer
    private lateinit var miniResult: MiniDrawer
    private lateinit var crossFader: Crossfader<*>
    lateinit var customHeader: View
    lateinit var projectManager: ProjectManager
    lateinit var plotEngine: PlotEngine
    var showTutorialOnNextWindowFocus = false
    var undoStack: UndoStack = UndoStack()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.PermissionRequests.writeStorage -> {
                // If request is cancelled, the result arrays are empty.
                if (!grantResults.isNotEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    val dlgAlert = AlertDialog.Builder(this)
                    dlgAlert.setMessage(getString(R.string.permission_missing_description))
                    dlgAlert.setTitle(getString(R.string.permission_missing))
                    dlgAlert.setPositiveButton(
                        getString(android.R.string.ok)
                    ) { _, _ ->
                        moveTaskToBack(true);
                        exitProcess(-1)
                    }
                    dlgAlert.setCancelable(false)
                    dlgAlert.create().show()
                }
                // permission was granted
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Check permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                val dlgAlert = AlertDialog.Builder(this)
                dlgAlert.setMessage(getString(R.string.permission_rationale_description))
                dlgAlert.setTitle(getString(R.string.permission_rationale))
                dlgAlert.setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        Constants.PermissionRequests.writeStorage
                    )
                }
                dlgAlert.setCancelable(false)
                dlgAlert.create().show()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Constants.PermissionRequests.writeStorage
                )
            }
        }


        listView.visibility = View.INVISIBLE

        projectManager = ProjectManager(this)

        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getString(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val tempAdapter = FilterListAdapter(arrayListOf(), this)
        tempAdapter.sort(FilterComparator())
        tempAdapter.setNotifyOnChange(true)
        tempAdapter.onChangedListener = { adapter: FilterListAdapter ->
            val emptyListNotice = FilterItem(FilterType.INVALID)
            if (adapter.count < 1) {
                adapter.add(emptyListNotice)
            } else if (adapter.count > 1) {
                for (i in 0 until adapter.count)
                    if (adapter.getItem(i)?.filter?.type == FilterType.INVALID) {
                        adapter.remove(adapter.getItem(i)!!)
                        break
                    }
            }

        }
        tempAdapter.onChangedListener?.invoke(tempAdapter)
        listView.adapter = tempAdapter
        listView.divider = null

        plotEngine = PlotEngine(plot, listView.adapter as FilterListAdapter)
        plotEngine.initializePlot(
            getString(R.string.plot_axis_frequency),
            getString(R.string.plot_axis_gain)
        )

        customHeader = layoutInflater.inflate(R.layout.custom_header, null)

        result = DrawerBuilder()
            .withActivity(this)
            .withToolbar(toolbar)
            .withTranslucentStatusBar(false)
            .withHeader(customHeader)
            .withSelectedItem(100) /* Select magnitude response plot */
            .addDrawerItems(
                PrimaryDrawerItem().withName(getString(R.string.add_filter))
                    .withIcon(GoogleMaterial.Icon.gmd_add).withIdentifier(1).withSelectable(false)
                    .withTag(0),
                PrimaryDrawerItem().withName(getString(R.string.undo))
                    .withIcon(GoogleMaterial.Icon.gmd_undo).withIdentifier(2).withSelectable(false),
                PrimaryDrawerItem().withName(getString(R.string.redo))
                    .withIcon(GoogleMaterial.Icon.gmd_redo).withIdentifier(3).withSelectable(false),

                PrimaryDrawerItem().withName(getString(R.string.check_filter_stability))
                    .withIcon(CommunityMaterial.Icon.cmd_alert_circle_check_outline).withSelectable(false)
                    .withIdentifier(4),

                SectionDrawerItem().withName(getString(R.string.category_plots)),
                PrimaryDrawerItem().withName(getString(R.string.magnitude_response))
                    .withIcon(GoogleMaterial.Icon.gmd_graphic_eq).withIdentifier(100),
                PrimaryDrawerItem().withName(getString(R.string.phase_response))
                    .withIcon(CommunityMaterial.Icon.cmd_chart_bell_curve).withIdentifier(101),
                PrimaryDrawerItem().withName(getString(R.string.group_delay))
                    .withIcon(GoogleMaterial.Icon.gmd_timer).withIdentifier(102),
                PrimaryDrawerItem().withName(getString(R.string.plot_none))
                    .withIcon(CommunityMaterial.Icon.cmd_border_none_variant).withIdentifier(103),

                SectionDrawerItem().withName(getString(R.string.section_about)),
                SecondaryDrawerItem().withName(getString(R.string.credits))
                    .withIcon(GoogleMaterial.Icon.gmd_info_outline).withSelectable(false)
                    .withIdentifier(1000)
            )
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(
                    view: View?,
                    position: Int,
                    drawerItem: IDrawerItem<*>
                ): Boolean {
                    if (drawerItem is Nameable<*>) when (drawerItem.identifier) {
                        1L -> {
                            if (crossFader.isCrossFaded())
                                crossFader.crossFade()

                            val dialog = FilterEditorFragment()
                            dialog.localizedTitle = getString(R.string.add_filter)
                            dialog.filterItem = FilterItem()
                            dialog.show(
                                this@MainActivity.supportFragmentManager,
                                dialog::javaClass.name
                            )

                            dialog.savedListener = {
                                if (dialog.filterItem != null) {
                                    val prevState =
                                        FilterArrayUtils.bundleFilterItems(listView.adapter as FilterListAdapter)
                                    (listView.adapter as FilterListAdapter).add(dialog.filterItem!!)

                                    undoStack.pushCommand(
                                        prevState,
                                        FilterArrayUtils.bundleFilterItems(listView.adapter as FilterListAdapter),
                                        getString(R.string.add_filter)
                                    )

                                    plotEngine.populatePlot(getSelectedPlotType())
                                    (listView.adapter as FilterListAdapter).sort(FilterComparator())
                                }
                            }
                        }
                        2L -> {
                            val state = undoStack.pullPrevCommand()
                            if (state == null)
                                Toast.makeText(
                                    applicationContext,
                                    getString(R.string.nothing_to_undo),
                                    Toast.LENGTH_SHORT
                                ).show()
                            else
                                FilterArrayUtils.restoreFilterItems(
                                    listView.adapter as FilterListAdapter,
                                    state
                                )
                            plotEngine.populatePlot(getSelectedPlotType())
                        }
                        3L -> {
                            val state = undoStack.pullNextCommand()
                            if (state == null)
                                Toast.makeText(
                                    applicationContext,
                                    getString(R.string.nothing_to_redo),
                                    Toast.LENGTH_SHORT
                                ).show()
                            else
                                FilterArrayUtils.restoreFilterItems(
                                    listView.adapter as FilterListAdapter,
                                    state
                                )
                            plotEngine.populatePlot(getSelectedPlotType())
                        }
                        4L -> {
                            showFilterStabilityReport()
                        }
                        100L -> {
                            plot?.gridLabelRenderer?.verticalAxisTitle =
                                getString(R.string.plot_axis_gain)
                            plotEngine.populatePlot(getSelectedPlotType())
                            plotcard?.visibility = View.VISIBLE
                        }
                        101L -> {
                            plot?.gridLabelRenderer?.verticalAxisTitle =
                                getString(R.string.plot_axis_phase)
                            plotEngine.populatePlot(getSelectedPlotType())
                            plotcard?.visibility = View.VISIBLE
                        }
                        102L -> {
                            plot?.gridLabelRenderer?.verticalAxisTitle =
                                getString(R.string.plot_axis_delay)
                            plotEngine.populatePlot(getSelectedPlotType())
                            plotcard?.visibility = View.VISIBLE
                        }
                        103L -> {
                            plotcard?.visibility = View.GONE
                        }
                        1000L -> {
                            showCredits()
                        }
                        else -> {
                        }

                    }
                    return false
                }
            })
            .withGenerateMiniDrawer(true)
            .withSavedInstance(savedInstanceState)
            .buildView()

        customHeader.openProject.setOnClickListener {
            requestLoadMenu()
        }

        customHeader.saveProject.setOnClickListener {
            requestSaveMenu()
        }

        customHeader.closeProject.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.close_project_notice))
                .setTitle(getString(R.string.close_project))
                .setPositiveButton(getString(android.R.string.yes)) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    projectManager.close()
                    customHeader.projectName.text = getString(R.string.untitled)
                    (listView.adapter as FilterListAdapter).clear()

                    plotEngine.populatePlot(getSelectedPlotType())
                    undoStack.clearStack()

                    if (crossFader.isCrossFaded())
                        crossFader.crossFade()
                }
                .setNegativeButton(getString(android.R.string.no)) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    if (crossFader.isCrossFaded())
                        crossFader.crossFade()
                }.show()
        }
        miniResult = result.miniDrawer!!
        miniResult.withEnableSelectedMiniDrawerItemBackground(true)

        val firstWidth = UIUtils.convertDpToPixel(300f, this).toInt()
        val secondWidth = UIUtils.convertDpToPixel(72f, this).toInt()
        crossFader = Crossfader<CrossFadeSlidingPaneLayout>()
            .withContent(findViewById<View>(R.id.crossfade_content))
            .withFirst(result.slider, firstWidth)
            .withSecond(miniResult.build(this), secondWidth)
            .withSavedInstance(savedInstanceState)
            .withGmailStyleSwiping()
            .build()

        miniResult.withCrossFader(CrossfadeWrapper(crossFader))

        if (savedInstanceState != null) {
            val instance: MainDataInstance = savedInstanceState.get("MainData") as MainDataInstance
            projectManager.restoreInstance(instance.projectManagerDataInstance)
            customHeader.projectName.text = projectManager.currentProjectName

            FilterArrayUtils.restoreFilterItems(
                listView.adapter as FilterListAdapter,
                instance.filterItems
            )
            (listView.adapter as FilterListAdapter).sort(FilterComparator())
            plotEngine.populatePlot(getSelectedPlotType())

            undoStack.restoreInstance(instance.undoStackDataInstance)
        } else {
            plotEngine.populatePlot(PlotType.MAGNITUDE_RESPONSE)
        }

        val hasIntent = handleFileIntent(intent)

        val mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!hasIntent && !mPrefs.getBoolean(Constants.PrefKeys.tutorialShown, false)) {
            showTutorialOnNextWindowFocus = true

            val editor = mPrefs.edit()
            editor.putBoolean(Constants.PrefKeys.tutorialShown, true);
            editor.apply()
        }

        listView.visibility = View.VISIBLE
    }

    fun openNewTabWindow(urls: String, context: Context) {
        val uris = Uri.parse(urls)
        val intents = Intent(Intent.ACTION_VIEW, uris)
        val b = Bundle()
        b.putBoolean("new_window", true)
        intents.putExtras(b)
        context.startActivity(intents)
    }

    fun getSelectedPlotType(): PlotType {
        return when (result.currentSelection) {
            100L -> PlotType.MAGNITUDE_RESPONSE
            101L -> PlotType.PHASE_RESPONSE
            102L -> PlotType.GROUP_DELAY
            else -> PlotType.NONE
        }
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        //add the values which need to be saved from the drawer to the bundle
        if (::result.isInitialized) {
            outState = result.saveInstanceState(outState)
        }
        //add the values which need to be saved from the crossFader to the bundle
        if (::crossFader.isInitialized) {
            outState = crossFader.saveInstanceState(outState)
        }

        val filters = arrayListOf<FilterItem>()
        for (i in 0 until listView.adapter.count)
            filters.add(listView.adapter.getItem(i) as FilterItem)

        outState.putSerializable(
            "MainData",
            MainDataInstance(
                projectManager.saveInstance(),
                undoStack.saveInstance(),
                filters
            )
        )

        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (crossFader.isCrossFaded()) {
            crossFader.crossFade()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                crossFader.crossFade()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (showTutorialOnNextWindowFocus)
            showTutorial()
        showTutorialOnNextWindowFocus = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Constants.ActivityReturnCodes.autoEqSelection -> {
                if (resultCode == Activity.RESULT_OK) {
                    val result = data?.getSerializableExtra("result") as AEQDetails
                    importAutoEQFile(
                        result.parametric_filename,
                        result.parametric_content
                    )
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun selectProjectFile(){
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = Environment.getExternalStorageDirectory()
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)
        properties.show_hidden_files = false
        properties.extensions = arrayOf(".vdcprj")

        val dialog = FilePickerDialog(this@MainActivity, properties)
        dialog.setTitle(getString(R.string.load_project_file))

        dialog.setDialogSelectionListener {
            if (it.isEmpty()) return@setDialogSelectionListener
            val state = projectManager.load(it.first())
            customHeader.projectName.text = projectManager.currentProjectName
            FilterArrayUtils.restoreFilterItems(
                listView.adapter as FilterListAdapter,
                state
            )
            (listView.adapter as FilterListAdapter).sort(FilterComparator())
            plotEngine.populatePlot(getSelectedPlotType())
            undoStack.clearStack()
            if (crossFader.isCrossFaded())
                crossFader.crossFade()
        }
        dialog.show()
    }

    private fun selectAutoEQFile(){
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = Environment.getExternalStorageDirectory()
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)
        properties.show_hidden_files = false
        properties.extensions = arrayOf(".txt")

        val dialog = FilePickerDialog(this@MainActivity, properties)
        dialog.setTitle(getString(R.string.select_autoeq_file_dialog_title))

        dialog.setDialogSelectionListener {
            if (it.isEmpty()) return@setDialogSelectionListener

            importAutoEQFile(
                it.first().substring(it.first().lastIndexOf("/") + 1),
                GenericFileIO.read(it.first())
            )

            if (crossFader.isCrossFaded())
                crossFader.crossFade()
        }
        dialog.show()
    }

    private fun importAutoEQFile(name: String, content: String?){
        val result = AutoEQConverter.toFilterItems(content ?: "")
        val filters = result.data as ArrayList<FilterItem>

        if(filters.count() < 1){
            DialogUtils.showDialog(this,
                getString(R.string.autoeq_import_no_data_title),
                getString(R.string.autoeq_import_no_data, result.failedCount),
                R.drawable.ic_error_outline
            )
        }
        else if(result.failedCount > 0){
            DialogUtils.showDialog(this,
                getString(R.string.autoeq_import_skipped_warning_title),
                getString(R.string.autoeq_import_skipped_warning, result.failedCount),
                R.drawable.ic_error_outline
            )
        }

        projectManager.close()

        FilterArrayUtils.restoreFilterItems(
            listView.adapter as FilterListAdapter,
            filters
        )
        (listView.adapter as FilterListAdapter).sort(FilterComparator())
        plotEngine.populatePlot(getSelectedPlotType())
        undoStack.clearStack()

        projectManager.currentProjectName = name.replace("ParametricEQ.txt", "")
            .replace("FixedBandEQ.txt", "").replace(".txt", "").trim()
        customHeader.projectName.text = projectManager.currentProjectName

    }

    private fun handleFileIntent(intent: Intent): Boolean {
        val action = intent.action ?: return false

        if (action.compareTo(Intent.ACTION_VIEW) == 0) {
            val uri = intent.data ?: return false

            val result: String = FileUtil(this).getPath(uri)

            Log.d(
                "IntentHandler",
                "Content/file intent detected: " + action + " : " + intent.dataString + " : " + intent.type + " : " + result
            );

            /* Verify VDCPRJ file */
            val cut = result.lastIndexOf('/')
            if (cut != -1) {
                if(!result.substring(cut + 1).contains(".vdcprj")){
                    DialogUtils.showDialog(this,
                        "Invalid file",
                        "This file is not a VDC project. Make sure to only select files with the file extension '*.vdcprj'",
                        R.drawable.ic_error_outline
                    )
                    return true;
                }
            }

            /* Load project file */
            val state = projectManager.load(result)
            customHeader.projectName.text = projectManager.currentProjectName
            FilterArrayUtils.restoreFilterItems(
                listView.adapter as FilterListAdapter,
                state
            )
            (listView.adapter as FilterListAdapter).sort(FilterComparator())
            plotEngine.populatePlot(getSelectedPlotType())
            undoStack.clearStack()

            return true;
        }
        return false;
    }

    private fun requestLoadMenu(){
        val popup = PopupMenu(this, customHeader.openProject).apply {
            setOnMenuItemClickListener {

                if (crossFader.isCrossFaded())
                    crossFader.crossFade()

                return@setOnMenuItemClickListener when (it.itemId) {
                    R.id.load -> {
                        selectProjectFile()
                        true
                    }
                    R.id.importAutoEq -> {
                        selectAutoEQFile()
                        true
                    }
                    R.id.downloadAutoEq -> {
                        startActivityForResult(
                            Intent(this@MainActivity, AutoEqSelectionActivity::class.java),
                            Constants.ActivityReturnCodes.autoEqSelection
                        )
                        true
                    }
                    else -> false
                }
            }
        }
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.load_menu, popup.menu)
        popup.show()
    }

    private fun requestSaveMenu(){
        val popup = PopupMenu(this, customHeader.saveProject).apply {
            setOnMenuItemClickListener {
                if ((listView.adapter as FilterListAdapter).isListEmpty()) {
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setMessage(getString(R.string.note_empty_project))
                        .setTitle(getString(R.string.no_filters))
                        .setNegativeButton(getString(android.R.string.ok)) { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }
                        .show()
                    return@setOnMenuItemClickListener true
                }

                if (crossFader.isCrossFaded())
                    crossFader.crossFade()

                return@setOnMenuItemClickListener when (it.itemId) {
                    R.id.save -> {
                        val filters = arrayListOf<FilterItem>()
                        for (i in 0 until listView.adapter.count)
                            filters.add(listView.adapter.getItem(i) as FilterItem)

                        //Attempt quick save
                        val result = projectManager.save(filters)
                        when {
                            result == null -> {
                                //Output path not yet specified -> display SaveAsFragment
                                val dialog = SaveAsFileFragment(
                                    this@MainActivity,
                                    SaveAsFileFragment.Mode.SaveAs
                                )
                                dialog.show(
                                    this@MainActivity.supportFragmentManager,
                                    dialog::javaClass.name
                                )
                            }
                            //result is true
                            result -> Toast.makeText(
                                this@MainActivity,
                                getString(R.string.toast_project_saved),
                                Toast.LENGTH_SHORT
                            )
                            //result is false
                            else -> Toast.makeText(
                                this@MainActivity,
                                getString(R.string.toast_project_not_saved), Toast.LENGTH_SHORT
                            )
                        }
                        true
                    }
                    R.id.saveAs -> {
                        val dialog = SaveAsFileFragment(
                            this@MainActivity,
                            SaveAsFileFragment.Mode.SaveAs
                        )
                        dialog.show(
                            this@MainActivity.supportFragmentManager,
                            dialog::javaClass.name
                        )
                        true
                    }
                    R.id.exportVDC -> {
                        val dialog = SaveAsFileFragment(
                            this@MainActivity,
                            SaveAsFileFragment.Mode.ExportVDC
                        )
                        dialog.show(
                            this@MainActivity.supportFragmentManager,
                            dialog::javaClass.name
                        )
                        true
                    }
                    R.id.deployVDC -> {
                        val dialog = DeployFileFragment(this@MainActivity)
                        dialog.show(
                            this@MainActivity.supportFragmentManager,
                            dialog::javaClass.name
                        )
                        true
                    }
                    else -> false
                }
            }
        }
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.save_menu, popup.menu)
        popup.show()
    }

    private fun showFilterStabilityReport(){
        if ((listView.adapter as FilterListAdapter).isListEmpty()) {
            DialogUtils.showDialog(
                this@MainActivity,
                getString(R.string.no_filters),
                getString(R.string.note_empty_project))
            return
        }

        if (crossFader.isCrossFaded())
            crossFader.crossFade()

        var result = ""
        for (i in 0 until listView.adapter.count) {
            val filter = (listView.adapter.getItem(i) as FilterItem).filter
            result += when (filter.isStable) {
                0 -> getString(
                    R.string.filter_stablity_pole_outside_unit_circle,
                    FilterType.toString(filter.type),
                    filter.frequency
                ) + "\n"
                2 -> getString(
                    R.string.filter_stablity_pole_approaching_unit_circle,
                    FilterType.toString(filter.type),
                    filter.frequency
                ) + "\n"
                else -> ""
            }
        }

        if(result.isEmpty())
            result = getString(R.string.filter_stability_all_stable);

        DialogUtils.showDialog(
            this@MainActivity,
            getString(R.string.filter_stability_report_title),
            result)
    }

    private fun showTutorial(){
        val listView = findViewById<ListView>(R.id.listView)

        FilterArrayUtils.restoreFilterItems(
            listView.adapter as FilterListAdapter,
            FilterProvider.getTutorialProject()
        )
        plotEngine.populatePlot(getSelectedPlotType())

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        val generalActionSection = result.recyclerView.layoutManager?.findViewByPosition(2)?: result.recyclerView.rootView
        val plotActionSection = result.recyclerView.layoutManager?.findViewByPosition(6)?: result.recyclerView.rootView

        TapTargetSequence(this)
            .targets(
                TapTarget.forBounds(
                    Rect(
                        width,20,0,0
                    ), getString(R.string.tutorial_step1_title), getString(R.string.tutorial_step1))
                    .textColor(android.R.color.primary_text_dark)
                    .transparentTarget(true)
                    .cancelable(true)
                    .targetRadius(40)
                    .id(0),
                TapTarget.forBounds(
                    Rect(
                        SystemUtils.findViewBounds(listView)
                    ), getString(R.string.tutorial_step2_title), getString(R.string.tutorial_step2))
                    .textColor(android.R.color.primary_text_dark)
                    .transparentTarget(true)
                    .cancelable(true)
                    .targetRadius(60)
                    .id(1),
                TapTarget.forBounds(
                    Rect(
                        SystemUtils.findViewBounds(customHeader.saveProject)
                    ), getString(R.string.tutorial_step3_title), getString(R.string.tutorial_step3))
                    .textColor(android.R.color.primary_text_dark)
                    .transparentTarget(true)
                    .cancelable(true)
                    .targetRadius(80)
                    .id(2),
                TapTarget.forBounds(
                    Rect(
                        SystemUtils.findViewBounds(
                            generalActionSection,
                            -(generalActionSection.width / 2)
                        )
                    ), getString(R.string.tutorial_step4_title), getString(R.string.tutorial_step4))
                    .textColor(android.R.color.primary_text_dark)
                    .transparentTarget(true)
                    .cancelable(true)
                    .targetRadius(100)
                    .id(3),
                TapTarget.forBounds(
                    Rect(
                        SystemUtils.findViewBounds(
                            plotActionSection,
                            -(plotActionSection.width / 3)
                        )
                    ), getString(R.string.tutorial_step5_title), getString(R.string.tutorial_step5))
                    .textColor(android.R.color.primary_text_dark)
                    .transparentTarget(true)
                    .cancelable(true)
                    .targetRadius(140)
                    .id(3),
                TapTarget.forBounds(
                    Rect(
                        SystemUtils.findViewBounds(
                            listView
                        )
                    ), getString(R.string.tutorial_end_title), getString(R.string.tutorial_end))
                    .textColor(android.R.color.primary_text_dark)
                    .transparentTarget(true)
                    .cancelable(true)
                    .targetRadius(0)
                    .id(4)
            )
            .continueOnCancel(true)
            .listener(object:TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    if(crossFader.isCrossFaded())
                        crossFader.crossFade()
                    FilterArrayUtils.restoreFilterItems(
                        listView.adapter as FilterListAdapter,
                        null
                    )
                    plotEngine.populatePlot(getSelectedPlotType())
                }
                override fun onSequenceStep(lastTarget:TapTarget, targetClicked: Boolean) {
                    when(lastTarget.id()){
                        1 -> {
                            if(!crossFader.isCrossFaded())
                                crossFader.crossFade()
                        }
                    }
                }
                override fun onSequenceCanceled(lastTarget:TapTarget) {
                }
            }).start()
    }

    private fun showCredits(){
        LibsBuilder()
            .withAboutAppName(getString(R.string.app_name))
            .withActivityTitle(getString(R.string.credits))
            .withAboutIconShown(true)
            .withAboutVersionShown(true)
            .withAboutDescription(getString(R.string.credits_description))
            .withAboutSpecial1("GitHub")
            .withAboutSpecial2("Telegram")
            .withAboutSpecial3(getString(R.string.license))
            .withListener(object : LibsConfiguration.LibsListener {
                override fun onExtraClicked(
                    v: View,
                    specialButton: Libs.SpecialButton
                ): Boolean {
                    when (specialButton.ordinal) {
                        0 -> openNewTabWindow(
                            "https://github.com/ThePBone/DDCToolbox-Android",
                            this@MainActivity
                        )
                        1 -> openNewTabWindow(
                            "https://t.me/ThePBone",
                            this@MainActivity
                        )
                        2 -> openNewTabWindow(
                            "https://github.com/ThePBone/DDCToolbox-Android/blob/master/LICENSE",
                            this@MainActivity
                        )
                    }
                    Log.i("tt", specialButton.ordinal.toString())
                    return true
                }

                override fun onIconClicked(v: View) {}
                override fun onIconLongClicked(v: View): Boolean {
                    return true
                }

                override fun onLibraryAuthorClicked(
                    v: View,
                    library: Library
                ): Boolean {
                    return false
                }

                override fun onLibraryAuthorLongClicked(
                    v: View,
                    library: Library
                ): Boolean {
                    return true
                }

                override fun onLibraryBottomClicked(
                    v: View,
                    library: Library
                ): Boolean {
                    return false
                }

                override fun onLibraryBottomLongClicked(
                    v: View,
                    library: Library
                ): Boolean {
                    return true
                }

                override fun onLibraryContentClicked(
                    v: View,
                    library: Library
                ): Boolean {
                    return false
                }

                override fun onLibraryContentLongClicked(
                    v: View,
                    library: Library
                ): Boolean {
                    return true
                }
            })
            .start(this@MainActivity)
    }

}
