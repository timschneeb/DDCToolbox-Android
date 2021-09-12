package cf.thebone.ddctoolbox.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import cf.thebone.ddctoolbox.R
import cf.thebone.ddctoolbox.adapter.AutoEqResultAdapter
import cf.thebone.ddctoolbox.api.AutoEqAPI
import cf.thebone.ddctoolbox.api.model.AEQError
import cf.thebone.ddctoolbox.api.model.AEQSearchResult
import cf.thebone.ddctoolbox.utils.DialogUtils
import kotlinx.android.synthetic.main.activity_auto_eq_selection.*


class AutoEqSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AutoEqAPI.instance.context = applicationContext

        setContentView(R.layout.activity_auto_eq_selection)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val llm = LinearLayoutManager(this)
        llm.orientation = LinearLayoutManager.VERTICAL
        search_recyclerview.layoutManager = llm

        search_recyclerview.addItemDecoration(
            DividerItemDecoration(this, llm.orientation)
        )

        val adapter = AutoEqResultAdapter(arrayListOf())
        adapter.onClickListener = {
            Log.d("AEQSelectionActivity", "Item clicked: " + it.model)

            val builder = AlertDialog.Builder(this)
            builder.setTitle("${it.model} (${it.group})")
            builder.setItems(arrayOf<CharSequence>(
                "Import",
                "Find alternatives",
                "View on GitHub"
            )) { _, which ->
                when (which) {
                    0 -> {
                        showLoaderUntilUpdate()
                        AutoEqAPI.instance.getDetails(it)
                    }
                    1 -> {
                        search.setQuery(it.model, true);
                    }
                    2 -> {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(it.web_url))
                        )
                    }
                }
            }
            builder.create().show()

        }

        search_recyclerview.adapter = adapter

        AutoEqAPI.instance.onErrorListener = { callType: AutoEqAPI.CallType, error: AEQError ->
            runOnUiThread {
                DialogUtils.showDialog(
                    this,
                    error.title,
                    error.description,
                    R.drawable.ic_error_outline
                )
                when (callType) {
                    /* Show empty view with error message */
                    AutoEqAPI.CallType.Query -> cancelLoader(error.title)
                    /* Do not show empty view */
                    AutoEqAPI.CallType.Details -> cancelLoader(null)
                }
            }
        }

        AutoEqAPI.instance.onSearchResultsAvailable = {
            updateResults(it)
        }

        AutoEqAPI.instance.onDetailsAvailable = {
            runOnUiThread {
                val data = Intent()
                data.putExtra("result", it)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        search.setIconifiedByDefault(false)
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if(query != null) {
                    if(query.isBlank() || query.length < 2) {
                        Toast.makeText(
                            this@AutoEqSelectionActivity,
                            "Please enter at least two characters",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else {
                        if(!isLoaderShown()) {
                            showLoaderUntilUpdate()
                            AutoEqAPI.instance.query(query)
                        }
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateResults(it: ArrayList<AEQSearchResult>) {
        runOnUiThread {
            search_recyclerview.adapter ?: return@runOnUiThread

            val adapter = search_recyclerview.adapter!! as AutoEqResultAdapter

            adapter.resultList.clear()
            adapter.resultList.addAll(it)
            adapter.notifyDataSetChanged()

            search_progress.visibility = View.GONE
            search_recyclerview.visibility = if (it.size < 1) View.GONE else View.VISIBLE

            search_emptyview.visibility = if (it.size < 1) View.VISIBLE else View.GONE
            search_emptyview_text.text = getString(R.string.no_results_found)
        }
    }

    private fun isLoaderShown(): Boolean{
        return search_progress.visibility == View.VISIBLE
    }

    private fun showLoaderUntilUpdate() {
        runOnUiThread{
            search_progress.visibility = View.VISIBLE

            search_recyclerview.visibility = View.GONE
            search_emptyview.visibility = View.GONE
        }
    }

    private fun cancelLoader(reason: String?) {
        runOnUiThread{
            search_progress.visibility = View.GONE

            /* If no reason is provided, do not return to empty view */
            if(reason == null){
                search_recyclerview.visibility = View.VISIBLE
                search_emptyview.visibility = View.GONE
            }
            else {
                search_recyclerview.visibility = View.GONE
                search_emptyview.visibility = View.VISIBLE
                search_emptyview_text.text = reason
            }
        }
    }

}