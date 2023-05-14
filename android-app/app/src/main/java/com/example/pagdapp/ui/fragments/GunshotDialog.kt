package com.example.pagdapp.ui.fragments

import android.R
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.data.model.networkModels.GunshotNetworkModel
import com.example.pagdapp.databinding.DialogGunshotBinding
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

class GunshotDialog(
    private val weapons: List<String>,
    private val location: LatLng,
    private val elevation: String,
    private val listener: GunshotListener
) :
    DialogFragment(),
    DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {


    private lateinit var binding: DialogGunshotBinding
    private var calendar = Calendar.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // Create the dialog builder
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Fill in info for gunshot")
            .setPositiveButton("Ok", null)
            .setNegativeButton("Cancel", null)
        isCancelable = false

        // Inflate the dialog layout using data binding
        binding = DialogGunshotBinding.inflate(layoutInflater)

        // Find the spinner view using the binding object
        val spinner = binding.spinner

        // Set up the spinner adapter
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, weapons)
        spinner.adapter = adapter

        // Set up the date/time button
        val dateTimeButton = binding.dateTimeButton
        dateTimeButton.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnPlus.isVisible = false
        binding.btnMinus.isVisible = false

        builder.setPositiveButton("OK") { dialog, _ ->

            // Get the selected weapon
            val selectedWeapon = spinner.selectedItem
            val gunshotID = binding.etGunshotID.text.toString()
            val reportID = binding.etReportID.text.toString()


            if (selectedWeapon == null || calendar.timeInMillis <= 0 || gunshotID == "" ) {
                // Show an error message if no weapon has been selected
                Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()

            } else {
                selectedWeapon as String
                val shotsFired = binding.etShotsFired.text.toString()

                // Create a new report
                val gunshot = GunshotNetworkModel(
                    gunshotID.toInt(),
                    reportID.toIntOrNull(),
                    calendar.timeInMillis,
                    location.latitude.toFloat(),
                    location.longitude.toFloat(),
                    elevation.toFloat(),
                    selectedWeapon,
                    shotsFired.toInt()
                )


                listener.onGunshotSelected(gunshot)
                dialog.dismiss()
                Log.e("onCreateDialog", calendar.timeInMillis.toString())
            }

        }

        // Set the view of the dialog to the root view of the binding object
        builder.setView(binding.root)

        return builder.create()
    }

    private fun showDateTimePicker() {
        // Create a new instance of the DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            this,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Show the dialog
        datePickerDialog.show()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        // Set the selected date on the calendar
        calendar.set(year, month, dayOfMonth)

        // Create a new instance of the TimePickerDialog
        val timePickerDialog = TimePickerDialog(
            requireContext(), this,
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        )

        // Show the dialog
        timePickerDialog.show()
    }


    // Update the date/time button text
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {

        // Set the selected time on the calendar
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

        // Update the date/time button text
        val dateTimeButton = binding.dateTimeButton
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss", Locale.getDefault())
        dateTimeButton.text = dateFormat.format(calendar.time)

        binding.btnPlus.isVisible = true
        binding.btnMinus.isVisible = true

        binding.btnPlus.setOnClickListener {
            calendar.timeInMillis += 1000
            dateTimeButton.text = dateFormat.format(calendar.time)
        }

        binding.btnMinus.setOnClickListener {
            calendar.timeInMillis -= 1000
            dateTimeButton.text = dateFormat.format(calendar.time)
        }
    }

    interface GunshotListener {
        fun onGunshotSelected(gunshot: GunshotNetworkModel)
    }

}
