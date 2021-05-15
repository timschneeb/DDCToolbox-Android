package cf.thebone.ddctoolbox.api

import android.content.Context
import android.util.Log
import cf.thebone.ddctoolbox.R
import cf.thebone.ddctoolbox.api.model.AEQDetails
import cf.thebone.ddctoolbox.api.model.AEQError
import cf.thebone.ddctoolbox.api.model.AEQSearchResult
import com.beust.klaxon.Json
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.code.regexp.Pattern
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.Closeable
import java.io.IOException

class AutoEqAPI {

    companion object {
        val instance: AutoEqAPI = AutoEqAPI()
    }

    enum class CallType {
        Query,
        Details
    }

    private val client: OkHttpClient = OkHttpClient()
    var context: Context? = null

    var onErrorListener: ((CallType, AEQError) -> Unit)? = null
    var onSearchResultsAvailable: ((ArrayList<AEQSearchResult>) -> Unit)? = null
    var onDetailsAvailable: ((AEQDetails) -> Unit)? = null

    fun query(str: String) {
        Thread {
            try {
                val execute: Closeable = client.newCall(
                    Request.Builder()
                        .url("https://github.com/jaakkopasanen/AutoEq/raw/master/results/INDEX.md")
                        .build()
                ).execute()

                val results = ArrayList<AEQSearchResult>()

                val lines = (execute as Response).body!!.string().lines()
                for (it in lines) {
                    if (it.trim().startsWith("-") &&
                        it.contains(str, true)) {
                        val p =
                            Pattern.compile("""\[(?<model>.*?)\]\((?<path>.*?)\)\s+by\s+(?<group>[^\s]+)""")
                        val m = p.matcher(it)
m
                        if (m.find()) {
                            val model = m.group("model").toString()

                            if (model.contains(str, true)) {
                                val group = m.group("group").toString()
                                val path = m.group("path").toString().replace(
                                    "./",
                                    "https://api.github.com/repos/jaakkopasanen/AutoEq/contents/results/"
                                )

                                results.add(
                                    AEQSearchResult(
                                        path,
                                        path.replace(
                                            "https://api.github.com/repos/jaakkopasanen/AutoEq/contents/results/",
                                            "https://github.com/jaakkopasanen/AutoEq/tree/master/results/"
                                        ),
                                        model,
                                        group
                                    )
                                )
                            }
                        }
                    }
                }

                onSearchResultsAvailable?.invoke(results)
                return@Thread

            } catch (e: Exception) {
                handleException(CallType.Query, e)
            }

        }.start()
    }

    fun getDetails(query: AEQSearchResult) {
        Thread {
            try {
                val execute: Closeable = client.newCall(
                    Request.Builder()
                        .url(query.directory_url)
                        .build()
                ).execute()

                val result = AEQDetails()
                val charStream = (execute as Response).body!!.charStream()
                val parser = Parser.default().parse(charStream)

                if(parser is JsonObject) {
                    val potentialErrorMsg = (parser as JsonObject).string("message")
                    if (potentialErrorMsg != null) {
                        if (potentialErrorMsg.contains("API rate limit exceeded")) {
                            onErrorListener?.invoke(
                                CallType.Details,
                                AEQError(
                                    context!!.getString(R.string.autoeq_error_rate_limit_title),
                                    context!!.getString(R.string.autoeq_error_rate_limit)
                                )
                            )
                            return@Thread
                        } else {
                            onErrorListener?.invoke(
                                CallType.Details,
                                AEQError(
                                    context!!.getString(R.string.autoeq_error_general_api_title),
                                    potentialErrorMsg
                                )
                            )
                            return@Thread
                        }
                    }
                }

                @Suppress("UNCHECKED_CAST")
                val json = parser as? JsonArray<JsonObject>
                if (json == null) {
                    onErrorListener?.invoke(
                        CallType.Details,
                        AEQError(
                            context!!.getString(R.string.autoeq_error_invalid_response_title),
                            context!!.getString(R.string.autoeq_error_invalid_response)
                        )
                    )
                    return@Thread
                }

                for (it in json) {
                    val filename = it.string("name").toString()
                    val url = it.string("download_url").toString()

                    if (filename.trimEnd().endsWith("ParametricEQ.txt")) {
                        val parametricRequest = Request.Builder().url(url).build()

                        val response = client.newCall(parametricRequest).execute()
                        if (!response.isSuccessful) {
                            onErrorListener?.invoke(
                                CallType.Details,
                                AEQError(
                                    context!!.getString(R.string.autoeq_error_download_failed_title),
                                    context!!.getString(R.string.autoeq_error_download_failed)
                                )
                            )
                            return@Thread
                        }

                        result.parametric_filename = filename
                        result.parametric_content = response.body!!.string()
                    }
                }

                if (result.parametric_filename.isEmpty() ||
                    result.parametric_content.isEmpty()
                ) {
                    onErrorListener?.invoke(
                        CallType.Details,
                        AEQError(
                            context!!.getString(R.string.autoeq_error_empty_response_title),
                            context!!.getString(R.string.autoeq_error_empty_response)
                        )
                    )
                    return@Thread
                }

                onDetailsAvailable?.invoke(result)
                return@Thread

            } catch (e: Exception) {
                handleException(CallType.Details, e)
            }

        }.start()
    }

    private fun handleException(type: CallType, e: Exception) {
        e.printStackTrace()
        if (e is IOException) {
            onErrorListener?.invoke(
                type,
                AEQError(context!!.getString(R.string.autoeq_error_network_title), e.message.toString())
            )
            return
        }
        throw e
    }

}