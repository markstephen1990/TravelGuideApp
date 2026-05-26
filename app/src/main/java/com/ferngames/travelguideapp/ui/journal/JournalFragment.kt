package com.ferngames.travelguideapp.ui.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.local.TravelDatabase
import com.ferngames.travelguideapp.data.model.JournalEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalFragment : Fragment() {

    private lateinit var database: TravelDatabase
    private lateinit var adapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TravelDatabase.getDatabase(requireContext())

        val rvJournal = view.findViewById<RecyclerView>(R.id.rvJournal)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)
        val btnAddEntry = view.findViewById<ImageButton>(R.id.btnAddEntry)

        // Setup adapter
        adapter = JournalAdapter(
            emptyList(),
            onDeleteClick = { entry ->
                viewLifecycleOwner.lifecycleScope.launch {
                    database.journalDao().deleteEntry(entry)
                    Toast.makeText(requireContext(),
                        "Entry deleted!", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvJournal.layoutManager = LinearLayoutManager(requireContext())
        rvJournal.adapter = adapter

        // Observe journal entries
        database.journalDao().getAllJournalEntries()
            .observe(viewLifecycleOwner) { entries ->
                adapter.updateList(entries)
                if (entries.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvJournal.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvJournal.visibility = View.VISIBLE
                }
            }

        // Add entry button
        btnAddEntry.setOnClickListener {
            showAddEntryDialog()
        }
    }

    private fun showAddEntryDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_journal, null)

        val etDestination = dialogView.findViewById<EditText>(R.id.etJournalDestination)
        val etTitle = dialogView.findViewById<EditText>(R.id.etJournalTitle)
        val etContent = dialogView.findViewById<EditText>(R.id.etJournalContent)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        AlertDialog.Builder(requireContext())
            .setTitle("📔 New Journal Entry")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val destination = etDestination.text.toString().trim()
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (destination.isEmpty() || title.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Please fill in destination and title!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val entry = JournalEntry(
                    destinationName = destination,
                    title = title,
                    content = content,
                    rating = ratingBar.rating,
                    date = sdf.format(Date())
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    database.journalDao().insertEntry(entry)
                    Toast.makeText(requireContext(),
                        "Journal entry saved! 📔", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}