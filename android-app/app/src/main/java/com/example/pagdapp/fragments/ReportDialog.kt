package com.example.pagdapp.fragments


import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.pagdapp.databinding.DialogReportBinding
import com.example.pagdapp.models.dbModels.Report
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

val TAG = "TESTREPORT"

class ReportDialog(private val weapons: List<String>,
                   private val location: LatLng,
                   private val elevation: String,
                   private val listener: ReportDialogListener)
    : DialogFragment(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {


    private lateinit var binding: DialogReportBinding
    private var calendar = Calendar.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // Create the dialog builder
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Select weapon and date/time")
            .setPositiveButton("Ok", null)
            .setNegativeButton("Cancel", null)
        isCancelable = false

        // Inflate the dialog layout using data binding
        binding = DialogReportBinding.inflate(layoutInflater)

        // Find the spinner view using the binding object
        val spinner = binding.spinner

        // Set up the spinner adapter
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weapons)
        spinner.adapter = adapter

        // Set up the date/time button
        val dateTimeButton = binding.dateTimeButton
        dateTimeButton.setOnClickListener {
            showDateTimePicker()
        }

        builder.setPositiveButton("OK") { dialog, _ ->

            // Get the selected weapon
            val selectedWeapon = spinner.selectedItem

            if (selectedWeapon == null || calendar.timeInMillis <= 0) {
                // Show an error message if no weapon has been selected
                Toast.makeText(context, "Select weapon first", Toast.LENGTH_SHORT).show()

            } else {
                selectedWeapon as String

                // Create a new report
                val report = Report(
                    calendar.timeInMillis,
                    location.latitude.toFloat(),
                    location.longitude.toFloat(),
                    elevation.toFloat(),
                    selectedWeapon
                )


                listener.onReportSelected(report)
                dialog.dismiss()
            }

        }

        // Set the view of the dialog to the root view of the binding object
        builder.setView(binding.root)

        return builder.create()
    }



    interface ReportDialogListener {
        fun onReportSelected(report: Report)
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
    }

}