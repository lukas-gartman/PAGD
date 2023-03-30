package com.example.pagdapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pagdapp.R
import com.example.pagdapp.adapters.CategoryAdapter
import com.example.pagdapp.models.Category
import com.example.pagdapp.viewModels.MainViewModel
import com.google.android.material.slider.Slider

/**
 * A fragment representing a list of Items.
 */
class SettingsFragment : Fragment(), CategoryAdapter.OnCheckedChangeListener {


    private val sharedMainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter : CategoryAdapter
    private lateinit var thresholdSlider: Slider
    private lateinit var cbCheckAll: CheckBox


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings_list, container, false)
        thresholdSlider = view.findViewById(R.id.thresholdSlider)
        cbCheckAll = view.findViewById(R.id.cbCheckAll)
        checkBoxSettings()
        sliderSettings()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        val recyclerView = view.findViewById<RecyclerView>(R.id.settings_recycler_view)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)
        adapter = CategoryAdapter(sharedMainViewModel.categories, this@SettingsFragment)
        recyclerView.adapter = adapter

    }

    private fun sliderSettings(){
        thresholdSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being stopped
            }
        })

        sharedMainViewModel.threshold.observe(viewLifecycleOwner) { value ->
            thresholdSlider.value = value
        }
        thresholdSlider.addOnChangeListener { _, value, _ ->
            sharedMainViewModel.setThreshold(value)
            sharedMainViewModel.updateThreshold()
        }
    }

    private fun checkBoxSettings() {
        sharedMainViewModel.checkAllCategories.observe(viewLifecycleOwner) { value->
            cbCheckAll.isChecked = value
        }
        cbCheckAll.setOnClickListener {
            sharedMainViewModel.updateAllCategories()
            adapter.checkAll()

        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()

    }

    override fun onCheckedChanged(category: Category) {
        sharedMainViewModel.updateCategory(category)
    }
}