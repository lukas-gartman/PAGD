package com.example.pagdapp.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.pagdapp.databinding.ItemSettingBinding
import com.example.pagdapp.models.Category


/**
 * [RecyclerView.Adapter] that can display a [Category].
 */
open class CategoryAdapter(private val categories: List<Category>, private val onCheckedChangeListener: OnCheckedChangeListener)
    : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            ItemSettingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = categories[position]

        holder.binding.apply {
            category.text = currentItem.title
            cbCategory.isChecked = currentItem.isIncluded
            cbCategory.setOnCheckedChangeListener { _, isChecked ->
                currentItem.isIncluded = isChecked
                onCheckedChangeListener.onCheckedChanged(currentItem)
                holder.absoluteAdapterPosition
            }

        }

    }

    override fun getItemCount(): Int = categories.size

    fun checkAll() {
        for(i in categories.indices) {
            categories[i].isIncluded = !categories[i].isIncluded
            notifyItemChanged(i)
        }
    }

    inner class ViewHolder(val binding: ItemSettingBinding) : RecyclerView.ViewHolder(binding.root)


    interface OnCheckedChangeListener {
        fun onCheckedChanged(category: Category)
    }

}