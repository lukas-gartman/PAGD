package com.example.pagdapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.pagdapp.databinding.ItemGunBinding
import com.example.pagdapp.databinding.ItemReportBinding
import com.example.pagdapp.models.dbModels.Gun
import com.example.pagdapp.models.dbModels.Report

class ReportAndGunAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var reportsList = mutableListOf<Report>()
    private var gunsList = mutableListOf<Gun>()
    private var isShowingReports = true

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

                holder.binding.apply {
                    tvReportId.text = "id"
                    tvTimestamp.text = currentItem.timestamp.toString()
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

    fun setReports(reportsList: List<Report>?) {
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

    companion object {
        private const val VIEW_TYPE_REPORT = 1
        private const val VIEW_TYPE_GUN = 2
    }
}
