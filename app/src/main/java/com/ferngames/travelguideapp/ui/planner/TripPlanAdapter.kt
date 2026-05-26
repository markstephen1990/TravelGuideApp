package com.ferngames.travelguideapp.ui.planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.model.TripPlan
import java.text.SimpleDateFormat
import java.util.Locale

class TripPlanAdapter(
    private var tripPlans: List<TripPlan>,
    private val onDeleteClick: (TripPlan) -> Unit,
    private val onEditClick: (TripPlan) -> Unit
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

        // Calculate duration
        if (tripPlan.startDate.isNotEmpty() && tripPlan.endDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val start = sdf.parse(tripPlan.startDate)
                val end = sdf.parse(tripPlan.endDate)
                if (start != null && end != null) {
                    val days = ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt() + 1
                    holder.tvDates.text = "📅 ${tripPlan.startDate} → ${tripPlan.endDate} ($days days)"
                }
            } catch (e: Exception) {
                holder.tvDates.text = "📅 ${tripPlan.startDate} → ${tripPlan.endDate}"
            }
        } else {
            holder.tvDates.text = "📅 Dates not set"
        }

        holder.tvBudget.text = if (tripPlan.budget > 0)
            "💰 Budget: $${String.format("%.2f", tripPlan.budget)}"
        else
            "💰 Budget: Not set"

        holder.tvNotes.text = if (tripPlan.notes.isNotEmpty())
            "📝 ${tripPlan.notes}"
        else
            "No notes added"

        holder.btnDelete.setOnClickListener { onDeleteClick(tripPlan) }
        holder.itemView.setOnClickListener { onEditClick(tripPlan) }
    }

    override fun getItemCount() = tripPlans.size

    fun updateList(newList: List<TripPlan>) {
        tripPlans = newList
        notifyDataSetChanged()
    }
}