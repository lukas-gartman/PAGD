package com.example.pagdapp.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.SearchView
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pagdapp.R
import com.example.pagdapp.databinding.FragmentSettingsListBinding
import com.example.pagdapp.ui.adapters.CategoryAdapter
import com.example.pagdapp.ui.viewModels.SettingsViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint


/**
 * A fragment representing a list of settings for the AI-model.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {


    private val settingsViewModel: SettingsViewModel by activityViewModels()
    private lateinit var adapter: CategoryAdapter
    private lateinit var cbCheckAll: CheckBox
    private lateinit var searchView: SearchView
    private lateinit var binding: FragmentSettingsListBinding



    private val onCheckedChangeListener = object : CategoryAdapter.OnCheckedChangeListener {
        override fun onCheckedChanged(category: String, isChecked: Boolean) {
            settingsViewModel.updateCategory(category, isChecked)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSettingsListBinding.inflate(inflater, container, false)

        cbCheckAll = binding.itemSlidercheckbox.cbCheckAll
        searchView = binding.itemSlidercheckbox.searchView

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        val recyclerView = view.findViewById<RecyclerView>(R.id.settings_recycler_view)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)

        adapter = CategoryAdapter(onCheckedChangeListener)


        binding.itemSlidercheckbox.btnSettings.setOnClickListener {
            showBottomSheetDialog()
        }
        recyclerView.adapter = adapter
        checkBoxSettings()
        initAdapter()
        initSearchView()

    }

    private fun checkBoxSettings() {
        settingsViewModel.checkAllCategories.observe(viewLifecycleOwner) { value ->
            value?.let {
                cbCheckAll.isChecked = value
            }
        }
        cbCheckAll.setOnClickListener { view ->
            val cb = view as CheckBox
            settingsViewModel.updateAllCategories(cb.isChecked)
            adapter.checkAll(cb.isChecked)
        }
    }

    private fun initSearchView() {

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(string: String?): Boolean {
                adapter.filterList(string)
                return true
            }

        })


    }

    private fun initAdapter() {
        adapter.initList(settingsViewModel.categories.value!!)

        settingsViewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.setNewList(categories)
            adapter.refreshCategories(categories)

        }
    }



    private fun showBottomSheetDialog() {
        val settingsBottomSheetFragment = SettingsBottomSheetFragment()
        settingsBottomSheetFragment.show(childFragmentManager, SettingsBottomSheetFragment.TAG)
    }


    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()

    }


}