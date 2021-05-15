package cf.thebone.ddctoolbox.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cf.thebone.ddctoolbox.R
import cf.thebone.ddctoolbox.api.model.AEQSearchResult
import java.util.ArrayList

class AutoEqResultAdapter(
    val resultList: ArrayList<AEQSearchResult>
) :
    RecyclerView.Adapter<AutoEqResultAdapter.AutoEqResultViewHolder>() {
    var onClickListener: ((AEQSearchResult) -> Unit)? = null

    override fun getItemCount(): Int {
        return resultList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AutoEqResultViewHolder {
        val layoutView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_autoeq, parent, false)
        return AutoEqResultViewHolder(layoutView)
    }

    override fun onBindViewHolder(
        holder: AutoEqResultViewHolder,
        position: Int
    ) {
        holder.title!!.text = resultList[position].model
        holder.subtitle!!.text = resultList[position].group
        holder.container.setOnClickListener{
            onClickListener?.invoke(resultList[position])
        }
    }

    inner class AutoEqResultViewHolder(
        itemView: View
    ) :
        RecyclerView.ViewHolder(itemView) {
        var container: LinearLayout = itemView as LinearLayout
        var subtitle: TextView? = itemView.findViewById<View>(R.id.subtitle) as TextView
        var title: TextView? = itemView.findViewById<View>(R.id.title) as TextView
    }

}