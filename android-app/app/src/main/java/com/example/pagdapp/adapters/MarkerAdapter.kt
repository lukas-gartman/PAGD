package com.example.pagdapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.pagdapp.R
import com.example.pagdapp.models.dbModels.Gun
import com.example.pagdapp.models.dbModels.Report

class MarkerAdapter(private val markerList: HashMap<String, Report>, private val weaponsList: MutableList<Gun>) :
    RecyclerView.Adapter<MarkerAdapter.MarkerViewHolder>() {

    class MarkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeStamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val coord: TextView = itemView.findViewById(R.id.tvCoord)
        val weaponSpinner: Spinner = itemView.findViewById(R.id.marker_weapon_spinner)
        val loadingSpinner: ProgressBar = itemView.findViewById(R.id.loading_spinner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.gunshot_marker, parent, false)
        return MarkerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
        /*
        val marker = markerList[position.toString()]
        holder.timeStamp.text = marker.timestamp.toString()
        holder.coord.text = marker.coord_lat.toString() + marker.coord_long.toString() + marker.coord_alt.toString()

        if (weaponsList.isEmpty()) {
            // Show the loading spinner if the weapons list is empty
            holder.loadingSpinner.visibility = View.VISIBLE
            holder.weaponSpinner.visibility = View.GONE
        } else {
            // Hide the loading spinner and populate the weapons spinner
            holder.loadingSpinner.visibility = View.GONE
            holder.weaponSpinner.visibility = View.VISIBLE

            val adapter = ArrayAdapter(
                holder.itemView.context,
                android.R.layout.simple_spinner_dropdown_item,
                weaponsList)

            holder.weaponSpinner.adapter = adapter
            holder.weaponSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    marker.gun = adapter.getItem(position).toString()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

         */
    }

    override fun getItemCount(): Int {
        return markerList.size
    }
}
