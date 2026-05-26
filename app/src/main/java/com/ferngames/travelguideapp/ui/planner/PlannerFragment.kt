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
import java.util.Calendar

class PlannerFragment : Fragment() {

    private lateinit var database: TravelDatabase
    private lateinit var adapter: TripPlanAdapter

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

        // Setup adapter
        adapter = TripPlanAdapter(
            emptyList(),
            onDeleteClick = { tripPlan ->
                viewLifecycleOwner.lifecycleScope.launch {
                    database.tripPlanDao().deleteTripPlan(tripPlan)
                    Toast.makeText(requireContext(),
                        "Trip deleted!", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvTripPlans.layoutManager = LinearLayoutManager(requireContext())
        rvTripPlans.adapter = adapter

        // Observe trip plans
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

        // Add trip button
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

        // Date pickers
        etStartDate.setOnClickListener { showDatePicker(etStartDate) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate) }

        AlertDialog.Builder(requireContext())
            .setTitle("➕ Add Trip Plan")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val destination = etDestination.text.toString().trim()
                if (destination.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Please enter a destination!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val tripPlan = TripPlan(
                    destinationName = destination,
                    startDate = etStartDate.text.toString().trim(),
                    endDate = etEndDate.text.toString().trim(),
                    budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0,
                    notes = etNotes.text.toString().trim()
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    database.tripPlanDao().insertTripPlan(tripPlan)
                    Toast.makeText(requireContext(),
                        "Trip added! 📅", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
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