package com.example.pagdapp.ui.adapters

import android.view.LayoutInflater

import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.example.pagdapp.databinding.ItemAudioFileBinding


class AudioFilesAdapter(var audioFilesList: List<String>) :
    RecyclerView.Adapter<AudioFilesAdapter.AudioFilesViewHolder>() {


    private var selectedItems = mutableSetOf<String>()

    inner class AudioFilesViewHolder(val binding : ItemAudioFileBinding) : RecyclerView.ViewHolder(binding.root) {

        val checkBox = binding.cbsendfile

        init {
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val fileName = audioFilesList[adapterPosition]
                if (isChecked) {
                    selectedItems.add(fileName)
                } else {
                    selectedItems.remove(fileName)
                }
            }
        }


    }

    fun getSelectedItems(): Set<String> {
        return selectedItems
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioFilesViewHolder {
        return AudioFilesViewHolder(
            ItemAudioFileBinding.inflate(LayoutInflater.from(parent.context),
                parent,
                false)
        )
    }

    override fun onBindViewHolder(holder: AudioFilesViewHolder, position: Int) {
        val fileName = audioFilesList[position]
        holder.binding.apply {
            tvfilename.text = fileName
        }

    }

    override fun getItemCount(): Int {
        return audioFilesList.size
    }

    fun submitList(newList: List<String>){
         audioFilesList = newList
        notifyItemInserted(audioFilesList.size-1)
    }
}
