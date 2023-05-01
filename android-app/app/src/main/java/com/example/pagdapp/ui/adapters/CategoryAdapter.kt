package com.example.pagdapp.ui.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import com.example.pagdapp.databinding.ItemSettingBinding
import com.example.pagdapp.data.model.audioclassifier.Category


/**
 * [RecyclerView.Adapter] that can display a [Category].
 */
open class CategoryAdapter(
    private val onCheckedChangeListener: OnCheckedChangeListener
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var categories: MutableList<Category> = mutableListOf()
    private var initList: List<Category> = mutableListOf()


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

            cbCategory.setOnClickListener { view ->
                val cb = view as CheckBox
                currentItem.isIncluded = cb.isChecked
                onCheckedChangeListener.onCheckedChanged(currentItem.title, cb.isChecked)


            }

        }

    }


    override fun getItemCount(): Int = categories.size

    fun checkAll(boolean: Boolean) {
        for (i in categories.indices) {
            categories[i].isIncluded = boolean
            notifyItemChanged(i)
        }

    }


    fun refreshCategories(update: List<Category>) {
        val oldList = categories
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            CategoryItemDiffCallback(
                oldList,
                update
            )
        )
        categories = update as MutableList<Category>
        diffResult.dispatchUpdatesTo(this)

    }

    fun filterList(string: String?) {
        val filteredList =
            initList.filter { category ->
                string == null || category.title.lowercase().contains(string, true)
            }
        refreshCategories(filteredList)
    }

    inner class ViewHolder(val binding: ItemSettingBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }


    interface OnCheckedChangeListener {
        fun onCheckedChanged(category: String, isChecked: Boolean)
    }

    class CategoryItemDiffCallback(
        var oldCategoryList: List<Category>,
        var newCategoryList: List<Category>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldCategoryList.size
        }

        override fun getNewListSize(): Int {
            return newCategoryList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (
                    oldCategoryList[oldItemPosition].title ==
                            newCategoryList[newItemPosition].title
                            &&
                            oldCategoryList[oldItemPosition].isIncluded ==
                            newCategoryList[newItemPosition].isIncluded
                    )
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (
                    oldCategoryList[oldItemPosition].title ==
                            newCategoryList[newItemPosition].title
                            &&
                            oldCategoryList[oldItemPosition].isIncluded ==
                            newCategoryList[newItemPosition].isIncluded
                    )
        }
    }

    fun initList(newList: List<Category>) {
        categories = newList as MutableList<Category>
        initList = newList

    }

    fun setNewList(newList: List<Category>) {
        initList = newList

    }

}