package com.example.pagdapp.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.pagdapp.R
import com.example.pagdapp.databinding.AddGunFormBinding
import com.example.pagdapp.databinding.FragmentReportAndGunBinding
import com.example.pagdapp.ui.adapters.ReportAndGunAdapter
import com.example.pagdapp.ui.viewModels.ReportViewModel
import com.example.pagdapp.utils.NetworkResult
import kotlinx.coroutines.launch
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.widget.CheckBox
import androidx.core.content.ContextCompat
import com.google.android.material.radiobutton.MaterialRadioButton

class ReportAndGunFragment : Fragment() {

    private val reportViewModel: ReportViewModel by activityViewModels()
    private lateinit var adapter: ReportAndGunAdapter
    private lateinit var binding: FragmentReportAndGunBinding
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        @JvmStatic
        fun newInstance() = ReportAndGunFragment()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentReportAndGunBinding.inflate(inflater, container, false)
        swipeRefreshLayout = binding.swipeRefreshLayout


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

        initButtons()
        subscribeCollectors()
        initSortingButtons()

        swipeRefreshLayout.setOnRefreshListener {
            if (adapter.isShowingReports) {
                reportViewModel.fetchAllReports()
            } else {
                reportViewModel.fetchAllGuns()
            }
        }
    }


    private fun subscribeCollectors() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    reportViewModel.guns.collect { data ->
                        when (data) {
                            is NetworkResult.Loading -> {
                                swipeRefreshLayout.isRefreshing = true
                            }
                            is NetworkResult.Success -> {
                                adapter.setGuns(data.data)
                                reportViewModel.sortAscending.value?.let {
                                    adapter.sortGunsByName(it)
                                }

                                binding.progressBar.isVisible = false
                                swipeRefreshLayout.isRefreshing = false
                            }
                            is NetworkResult.Error -> {
                                binding.progressBar.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to fetch guns: " + data.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                swipeRefreshLayout.isRefreshing = false
                            }
                        }
                    }
                }
                launch {
                    reportViewModel.reports.collect { data ->
                        when (data) {
                            is NetworkResult.Loading -> {
                                swipeRefreshLayout.isRefreshing = true
                            }
                            is NetworkResult.Success -> {
                                adapter.setReports(data.data)
                                reportViewModel.sortAscending.value?.let {
                                    adapter.sortReportsByTimestamp(it)
                                }
                                binding.progressBar.isVisible = false
                                swipeRefreshLayout.isRefreshing = false

                            }
                            is NetworkResult.Error -> {
                                binding.progressBar.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to fetch reports: " + data.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                swipeRefreshLayout.isRefreshing = false
                            }
                        }
                    }
                }
            }
        }

    }

    private fun initButtons() {
        updateButtonAppearance()

        binding.gunsButton.setOnClickListener {
            adapter.showGuns()
            updateFabVisibility()
            updateButtonAppearance()
        }

        binding.reportsButton.setOnClickListener {
            adapter.showReports()
            updateFabVisibility()
            updateButtonAppearance()
        }

        binding.fabAdd.setOnClickListener {
            val addGunDialogFragment = AddGunDialogFragment(reportViewModel)
            addGunDialogFragment.show(parentFragmentManager, "add_gun")
        }
    }

    private fun updateFabVisibility() {
        binding.fabAdd.isVisible = !adapter.isShowingReports
    }

    private fun updateButtonAppearance() {
        val defaultButtonBackgroundColor = getPrimaryColorForButton()
        if (adapter.isShowingReports) {
            binding.reportsButton.setBackgroundColor(Color.GREEN)
            binding.gunsButton.setBackgroundColor(defaultButtonBackgroundColor)
        } else {
            binding.gunsButton.setBackgroundColor(Color.GREEN)
            binding.reportsButton.setBackgroundColor(defaultButtonBackgroundColor)
        }
    }

    private fun getPrimaryColorForButton(): Int {
        return ContextCompat.getColor(requireContext(), R.color.purple_500)
    }


    private fun initSortingButtons() {

        reportViewModel.sortAscending.observe(viewLifecycleOwner) {
            binding.rbAscending.isChecked = it
            binding.rbDescending.isChecked = !it

            adapter.sortReportsByTimestamp(it)
            adapter.sortGunsByName(it)

        }

        binding.rbAscending.setOnClickListener {
            val button = it as MaterialRadioButton
            reportViewModel.updateSorting(button.isChecked)
        }

        binding.rbDescending.setOnClickListener {
            val button = it as MaterialRadioButton
            reportViewModel.updateSorting(!button.isChecked)
        }
    }


    class AddGunDialogFragment(private val reportViewModel: ReportViewModel) :
        DialogFragment() {

        private lateinit var binding: AddGunFormBinding

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            binding = AddGunFormBinding.inflate(layoutInflater)

            val builder = AlertDialog.Builder(requireActivity())
            builder.setView(binding.root)



            binding.addGunButton.setOnClickListener {
                val gunName = binding.gunName.text.toString()
                val gunType = binding.gunType.text.toString()
                if (gunName.isNotEmpty()) {
                    reportViewModel.addGun(gunName, gunType)
                    initCollectors()
                } else {
                    binding.gunName.error = "Please enter a gun name"
                }
            }

            return builder.create()
        }

        private fun initCollectors() {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    reportViewModel.apiRequest.collect { data ->
                        when (data) {
                            is NetworkResult.Loading -> binding.progressBar2.isVisible =
                                true
                            is NetworkResult.Success -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Gun added: ${data.data.toString()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.progressBar2.isVisible = false
                                dismiss()
                            }
                            is NetworkResult.Error -> {
                                binding.progressBar2.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to add gun: ${data.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
}