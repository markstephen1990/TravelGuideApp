package com.ferngames.travelguideapp.ui.journal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.model.JournalEntry

class JournalAdapter(
    private var entries: List<JournalEntry>,
    private val onDeleteClick: (JournalEntry) -> Unit
) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    inner class JournalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvJournalTitle)
        val tvDestination: TextView = itemView.findViewById(R.id.tvJournalDestination)
        val tvDate: TextView = itemView.findViewById(R.id.tvJournalDate)
        val tvRating: TextView = itemView.findViewById(R.id.tvJournalRating)
        val tvContent: TextView = itemView.findViewById(R.id.tvJournalContent)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteEntry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val entry = entries[position]

        holder.tvTitle.text = entry.title
        holder.tvDestination.text = "📍 ${entry.destinationName}"
        holder.tvDate.text = entry.date
        holder.tvContent.text = entry.content

        // Show rating as stars
        val stars = "⭐".repeat(entry.rating.toInt())
        holder.tvRating.text = if (stars.isNotEmpty()) stars else "No rating"

        holder.btnDelete.setOnClickListener {
            onDeleteClick(entry)
        }
    }

    override fun getItemCount() = entries.size

    fun updateList(newList: List<JournalEntry>) {
        entries = newList
        notifyDataSetChanged()
    }
}