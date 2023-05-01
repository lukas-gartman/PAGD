package com.example.pagdapp.ui.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import com.example.pagdapp.databinding.SettingsBottomSheetBinding
import com.example.pagdapp.ui.viewModels.SettingsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider


class SettingsBottomSheetFragment : BottomSheetDialogFragment() {

    lateinit var binding: SettingsBottomSheetBinding
    private lateinit var thresholdSlider: Slider
    private lateinit var delaySlider: Slider
    private val settingsViewModel: SettingsViewModel by activityViewModels()



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsBottomSheetBinding.inflate(inflater, container, false)
        thresholdSlider = binding.thresholdSlider
        delaySlider = binding.delaySlider

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextThreshold()
        editTextDelay()
        sliderSettings()

        val bottomSheet = view.parent as View
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun sliderSettings() {
        settingsViewModel.threshold.observe(viewLifecycleOwner) { value ->
            thresholdSlider.value = value
            binding.etThreshold.setText(value.toString())
        }
        thresholdSlider.addOnChangeListener { _, value, _ ->
            settingsViewModel.setThreshold(value)
            settingsViewModel.updateThreshold()
        }

        settingsViewModel.delay.observe(viewLifecycleOwner) { delay ->
            delaySlider.value = delay.toFloat()
            binding.etDelay.setText(delay.toString())
        }
        delaySlider.addOnChangeListener { _, value, _ ->
            settingsViewModel.setDelay(value.toLong())
            settingsViewModel.updateDelay()
        }
    }


    private fun editTextThreshold() {
        binding.etThreshold.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                // Hide the keyboard
                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(
                    binding.etThreshold.windowToken,
                    0
                )


                binding.etThreshold.clearFocus()

                // Update the threshold value
                val textValue = binding.etThreshold.text.toString()
                if (textValue.isNotEmpty()) {
                    val thresholdValue = textValue.toFloatOrNull()
                    if (thresholdValue != null && thresholdValue in 0.0..1.0) {
                        settingsViewModel.setThreshold(thresholdValue)
                        settingsViewModel.updateThreshold()
                    } else {
                        binding.etThreshold.setText(settingsViewModel.threshold.value.toString())
                    }
                } else {
                    binding.etThreshold.setText(settingsViewModel.threshold.value.toString())
                }
                return@setOnEditorActionListener false
            } else {
                false
            }
        }
    }

    private fun editTextDelay() {
        binding.etDelay.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {

                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(
                    binding.etDelay.windowToken,
                    0
                )


                binding.etDelay.clearFocus()


                val textValue = binding.etDelay.text.toString()
                if (textValue.isNotEmpty()) {
                    val thresholdValue = textValue.toLongOrNull()
                    if (thresholdValue != null && thresholdValue in 0..2000
                        && thresholdValue.toInt() % 100 == 0) {
                        settingsViewModel.setDelay(thresholdValue)
                        settingsViewModel.updateDelay()
                    } else {
                        binding.etDelay.setText(settingsViewModel.delay.value.toString())
                    }
                } else {
                    binding.etDelay.setText(settingsViewModel.delay.value.toString())
                }
                return@setOnEditorActionListener true
            } else {
                return@setOnEditorActionListener false
            }
        }
    }

    companion object {
        const val TAG = "SettingsBottomSheetFragment"
    }
}