package com.example.pagdapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.pagdapp.R
import com.example.pagdapp.ui.adapters.AudioFilesAdapter
import com.example.pagdapp.databinding.FragmentAudiolistBinding

import com.example.pagdapp.ui.viewModels.AudioFilesViewModel


class AudioListFragment : Fragment() {

    private lateinit var binding: FragmentAudiolistBinding
    private lateinit var viewModel: AudioFilesViewModel
    private lateinit var audioAdapter: AudioFilesAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AudioFilesViewModel::class.java]


        // TODO: Use the ViewModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_audiolist, container, false)


        // Initialize RecyclerView and adapter
        val recyclerView: RecyclerView = view.findViewById(R.id.rvAudioList)
        audioAdapter = viewModel.getAudioFiles().value?.let { AudioFilesAdapter(it) }!!
        recyclerView.adapter = audioAdapter



        // Observe changes to the audio file list
        viewModel.getAudioFiles().observe(viewLifecycleOwner) { audioFiles ->
            audioAdapter.submitList(audioFiles)
            audioAdapter.notifyItemInserted((viewModel.getAudioFiles().value?.size ?: 0) - 1)
        }

        return view
    }


}
