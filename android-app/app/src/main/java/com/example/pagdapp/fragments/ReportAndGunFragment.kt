package com.example.pagdapp.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pagdapp.R
import com.example.pagdapp.adapters.ReportAndGunAdapter
import com.example.pagdapp.databinding.AddGunFormBinding
import com.example.pagdapp.databinding.FragmentReportAndGunBinding
import com.example.pagdapp.viewModels.MainViewModel


class ReportAndGunFragment : Fragment() {

    private val sharedMainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter : ReportAndGunAdapter
    private lateinit var binding: FragmentReportAndGunBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentReportAndGunBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView and adapter
        val layoutManager = LinearLayoutManager(context)
        val recyclerView: RecyclerView = view.findViewById(R.id.rvReportAndGun)
        recyclerView.layoutManager = layoutManager

        adapter = ReportAndGunAdapter()
        recyclerView.adapter = adapter


        sharedMainViewModel.guns.observe(viewLifecycleOwner) {guns->
            adapter.setGuns(guns)
        }

        sharedMainViewModel.reports.observe(viewLifecycleOwner) {reports ->
            adapter.setReports(reports)
        }

        binding.gunsButton.setOnClickListener {
            adapter.showGuns()
        }

        binding.reportsButton.setOnClickListener {
            adapter.showReports()
        }

        binding.fabAdd.setOnClickListener {
            val addGunDialogFragment = AddGunDialogFragment(sharedMainViewModel)
            addGunDialogFragment.show(parentFragmentManager, "add_gun")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReportAndGunFragment()

    }

     class AddGunDialogFragment(private val viewModel: MainViewModel) : DialogFragment() {

        private lateinit var binding: AddGunFormBinding

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            binding = AddGunFormBinding.inflate(layoutInflater)

            val builder = AlertDialog.Builder(requireActivity())
            builder.setView(binding.root)

            binding.addGunButton.setOnClickListener {
                val gunName = binding.gunName.text.toString()
                val gunType = binding.gunType.text.toString()
                if (gunName.isNotEmpty()) {
                    viewModel.addGun(gunName, gunType)
                    dismiss()
                } else {
                    binding.gunName.error = "Please enter a gun name"
                }
            }

            return builder.create()
        }


    }
}