package com.ferngames.travelguideapp.ui.planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.model.TripPlan

class TripPlanAdapter(
    private var tripPlans: List<TripPlan>,
    private val onDeleteClick: (TripPlan) -> Unit
) : RecyclerView.Adapter<TripPlanAdapter.TripPlanViewHolder>() {

    inner class TripPlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDestination: TextView = itemView.findViewById(R.id.tvTripDestination)
        val tvDates: TextView = itemView.findViewById(R.id.tvTripDates)
        val tvBudget: TextView = itemView.findViewById(R.id.tvTripBudget)
        val tvNotes: TextView = itemView.findViewById(R.id.tvTripNotes)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTrip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripPlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_plan, parent, false)
        return TripPlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripPlanViewHolder, position: Int) {
        val tripPlan = tripPlans[position]

        holder.tvDestination.text = "📍 ${tripPlan.destinationName}"
        holder.tvDates.text = "📅 ${tripPlan.startDate} → ${tripPlan.endDate}"
        holder.tvBudget.text = if (tripPlan.budget > 0)
            "💰 Budget: $${tripPlan.budget}"
        else
            "💰 Budget: Not set"
        holder.tvNotes.text = if (tripPlan.notes.isNotEmpty())
            tripPlan.notes
        else
            "No notes added"

        holder.btnDelete.setOnClickListener {
            onDeleteClick(tripPlan)
        }
    }

    override fun getItemCount() = tripPlans.size

    fun updateList(newList: List<TripPlan>) {
        tripPlans = newList
        notifyDataSetChanged()
    }
}