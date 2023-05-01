package com.example.pagdapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.pagdapp.databinding.ItemGunBinding
import com.example.pagdapp.databinding.ItemReportBinding
import com.example.pagdapp.data.model.dbModels.Gun
import com.example.pagdapp.data.model.networkModels.ReportNetworkModel
import java.text.SimpleDateFormat
import java.util.*

class ReportAndGunAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var reportsList = mutableListOf<ReportNetworkModel>()
    private var gunsList = mutableListOf<Gun>()
    var isShowingReports = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            VIEW_TYPE_GUN -> {
                GunViewHolder(
                    ItemGunBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                ReportViewHolder(
                    ItemReportBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ReportViewHolder ->  {
                val currentItem = reportsList[position]

                val locationString = "lat: ${currentItem.coord_lat}," +
                        "\nlng: ${currentItem.coord_long}," +
                        "\nalt: ${currentItem.coord_alt}"

                val timestamp = currentItem.timestamp;
                val date = Date(timestamp);
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateString = dateFormat.format(date)
                holder.binding.apply {
                    tvReportId.text = currentItem.report_id.toString()
                    tvTimestamp.text = dateString
                    tvCoord.text = locationString
                    tvGun.text = currentItem.gun
                }

            }
            is GunViewHolder -> {
                val currentItem = gunsList[position]
                holder.binding.apply {
                    tvGunType.text = currentItem.type
                    tvGunName.text = currentItem.name
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (isShowingReports) {
            reportsList.size
        } else {
            gunsList.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isShowingReports) {
            VIEW_TYPE_REPORT
        } else {
            VIEW_TYPE_GUN
        }
    }

    fun showReports() {
        if (!isShowingReports) {
            isShowingReports = true
            notifyItemRangeRemoved(0, gunsList.size)
            notifyItemRangeInserted(0, reportsList.size)
        }
    }

    fun showGuns() {
        if (isShowingReports) {
            isShowingReports = false
            notifyItemRangeRemoved(0, reportsList.size)
            notifyItemRangeInserted(0, gunsList.size)
        }
    }

    fun setReports(reportsList: List<ReportNetworkModel>?, ascending: Boolean = false) {
        val previousItemCount = itemCount
        this.reportsList.clear()
        this.reportsList.addAll(reportsList!!)
        notifyItemRangeRemoved(0, previousItemCount)
        notifyItemRangeInserted(0, itemCount)
    }

    fun setGuns(gunsList: List<Gun>?) {
        val previousItemCount = itemCount
        this.gunsList.clear()
        this.gunsList.addAll(gunsList!!)
        notifyItemRangeRemoved(0, previousItemCount)
        notifyItemRangeInserted(0, itemCount)
    }

    inner class ReportViewHolder(val binding: ItemReportBinding) : ViewHolder(binding.root)

    inner class GunViewHolder(val binding: ItemGunBinding) : ViewHolder(binding.root)


    fun sortReportsByTimestamp(ascending: Boolean = true) {
        reportsList.sortWith(compareBy { it.report_id })
        if (!ascending) {
            reportsList.reverse()
        }
        if (isShowingReports) {
            notifyDataSetChanged()
        }
    }

    fun sortGunsByName(ascending: Boolean = true) {
        gunsList.sortWith(compareBy { it.name })
        if (!ascending) {
            gunsList.reverse()
        }
        if (!isShowingReports) {
            notifyDataSetChanged()
        }
    }


    companion object {
        private const val VIEW_TYPE_REPORT = 1
        private const val VIEW_TYPE_GUN = 2
    }
}
