package com.ferngames.travelguideapp.ui.planner

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.local.TravelDatabase
import com.ferngames.travelguideapp.data.model.TripPlan
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlannerFragment : Fragment() {

    private lateinit var database: TravelDatabase
    private lateinit var adapter: TripPlanAdapter
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_planner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TravelDatabase.getDatabase(requireContext())

        val rvTripPlans = view.findViewById<RecyclerView>(R.id.rvTripPlans)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)
        val btnAddTrip = view.findViewById<ImageButton>(R.id.btnAddTrip)

        adapter = TripPlanAdapter(
            emptyList(),
            onDeleteClick = { tripPlan ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Trip")
                    .setMessage("Are you sure you want to delete this trip?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            database.tripPlanDao().deleteTripPlan(tripPlan)
                            Toast.makeText(requireContext(),
                                "Trip deleted!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onEditClick = { tripPlan ->
                showEditTripDialog(tripPlan)
            }
        )
        rvTripPlans.layoutManager = LinearLayoutManager(requireContext())
        rvTripPlans.adapter = adapter

        database.tripPlanDao().getAllTripPlans()
            .observe(viewLifecycleOwner) { tripPlans ->
                adapter.updateList(tripPlans)
                if (tripPlans.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvTripPlans.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvTripPlans.visibility = View.VISIBLE
                }
            }

        btnAddTrip.setOnClickListener {
            showAddTripDialog()
        }
    }

    private fun showAddTripDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_trip, null)

        val etDestination = dialogView.findViewById<EditText>(R.id.etTripDestination)
        val etStartDate = dialogView.findViewById<EditText>(R.id.etStartDate)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etEndDate)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        etStartDate.setOnClickListener { showDatePicker(etStartDate) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate) }
        etStartDate.isFocusable = false
        etEndDate.isFocusable = false

        AlertDialog.Builder(requireContext())
            .setTitle("➕ Plan New Trip")
            .setView(dialogView)
            .setPositiveButton("Save Trip") { _, _ ->
                val destination = etDestination.text.toString().trim()
                val startDate = etStartDate.text.toString().trim()
                val endDate = etEndDate.text.toString().trim()
                val budget = etBudget.text.toString().trim()
                val notes = etNotes.text.toString().trim()

                when {
                    destination.isEmpty() -> Toast.makeText(requireContext(),
                        "Please enter a destination!", Toast.LENGTH_SHORT).show()
                    startDate.isEmpty() -> Toast.makeText(requireContext(),
                        "Please select a start date!", Toast.LENGTH_SHORT).show()
                    endDate.isEmpty() -> Toast.makeText(requireContext(),
                        "Please select an end date!", Toast.LENGTH_SHORT).show()
                    else -> {
                        // Calculate trip duration
                        val start = sdf.parse(startDate)
                        val end = sdf.parse(endDate)
                        if (end != null && start != null && end.before(start)) {
                            Toast.makeText(requireContext(),
                                "End date must be after start date!",
                                Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        val tripPlan = TripPlan(
                            destinationName = destination,
                            startDate = startDate,
                            endDate = endDate,
                            budget = budget.toDoubleOrNull() ?: 0.0,
                            notes = notes
                        )
                        viewLifecycleOwner.lifecycleScope.launch {
                            database.tripPlanDao().insertTripPlan(tripPlan)
                            Toast.makeText(requireContext(),
                                "Trip planned! 📅", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTripDialog(tripPlan: TripPlan) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_trip, null)

        val etDestination = dialogView.findViewById<EditText>(R.id.etTripDestination)
        val etStartDate = dialogView.findViewById<EditText>(R.id.etStartDate)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etEndDate)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        // Pre-fill existing data
        etDestination.setText(tripPlan.destinationName)
        etStartDate.setText(tripPlan.startDate)
        etEndDate.setText(tripPlan.endDate)
        etBudget.setText(if (tripPlan.budget > 0) tripPlan.budget.toString() else "")
        etNotes.setText(tripPlan.notes)

        etStartDate.setOnClickListener { showDatePicker(etStartDate) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate) }
        etStartDate.isFocusable = false
        etEndDate.isFocusable = false

        AlertDialog.Builder(requireContext())
            .setTitle("✏️ Edit Trip")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val destination = etDestination.text.toString().trim()
                val startDate = etStartDate.text.toString().trim()
                val endDate = etEndDate.text.toString().trim()

                if (destination.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Please enter a destination!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedTrip = tripPlan.copy(
                    destinationName = destination,
                    startDate = startDate,
                    endDate = endDate,
                    budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0,
                    notes = etNotes.text.toString().trim()
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    database.tripPlanDao().updateTripPlan(updatedTrip)
                    Toast.makeText(requireContext(),
                        "Trip updated! ✅", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()

        // If field already has a date, pre-select it
        val currentText = editText.text.toString()
        if (currentText.isNotEmpty()) {
            try {
                val date = sdf.parse(currentText)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // use current date
            }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                editText.setText("$day/${month + 1}/$year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}