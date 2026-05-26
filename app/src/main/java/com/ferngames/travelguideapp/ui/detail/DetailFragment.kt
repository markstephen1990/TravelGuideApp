package com.ferngames.travelguideapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.local.TravelDatabase
import com.ferngames.travelguideapp.data.model.Destination
import com.ferngames.travelguideapp.data.model.JournalEntry
import com.ferngames.travelguideapp.data.model.TripPlan
import com.ferngames.travelguideapp.data.remote.DestinationEnricher
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailFragment : Fragment() {

    private val args: DetailFragmentArgs by navArgs()
    private lateinit var database: TravelDatabase
    private var currentDestination: Destination? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TravelDatabase.getDatabase(requireContext())

        // Load destination
        viewLifecycleOwner.lifecycleScope.launch {
            val destination = database.destinationDao()
                .getDestinationById(args.destinationId)
            destination?.let {
                currentDestination = it
                displayDestination(view, it)
            }
        }

        // Back button
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun displayDestination(view: View, destination: Destination) {
        // Image
        Glide.with(this)
            .load(destination.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(view.findViewById<ImageView>(R.id.ivDestinationImage))

        // Text fields
        view.findViewById<TextView>(R.id.tvDestinationName).text = destination.name
        view.findViewById<TextView>(R.id.tvCountry).text = "📍 ${destination.country}"
        view.findViewById<TextView>(R.id.tvRating).text = "⭐ ${destination.rating}"
        view.findViewById<TextView>(R.id.tvCategory).text = destination.category
        view.findViewById<TextView>(R.id.tvBestTime).text = destination.bestTimeToVisit
        view.findViewById<TextView>(R.id.tvCurrency).text = destination.currency
        view.findViewById<TextView>(R.id.tvLanguage).text = destination.language
        view.findViewById<TextView>(R.id.tvDescription).text = destination.description

        // Weather & Country info views
        val weatherCard = view.findViewById<CardView>(R.id.weatherCard)
        val tvWeatherEmoji = view.findViewById<TextView>(R.id.tvWeatherEmoji)
        val tvWeatherTemp = view.findViewById<TextView>(R.id.tvWeatherTemp)
        val tvWeatherDesc = view.findViewById<TextView>(R.id.tvWeatherDesc)
        val tvWeatherWind = view.findViewById<TextView>(R.id.tvWeatherWind)
        val tvPopulation = view.findViewById<TextView>(R.id.tvPopulation)

        // Fetch live weather and country info
        viewLifecycleOwner.lifecycleScope.launch {
            val enricher = DestinationEnricher()

            // Get live weather
            val weather = enricher.getWeather(
                destination.latitude,
                destination.longitude
            )
            weather?.let {
                weatherCard.visibility = View.VISIBLE
                tvWeatherEmoji.text = it.emoji
                tvWeatherTemp.text = "${it.temperature}°C"
                tvWeatherDesc.text = it.description
                tvWeatherWind.text = "${it.windSpeed}"
            }

            // Get country info
            val countryInfo = enricher.getCountryInfo(destination.country)
            countryInfo?.let {
                tvPopulation.text = "👥 Population: ${
                    String.format("%,d", it.population)
                }"
            }
        }

        // Wishlist button
        val btnWishlist = view.findViewById<ImageButton>(R.id.btnWishlist)
        btnWishlist.setImageResource(
            if (destination.isWishlisted) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
        btnWishlist.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val newStatus = !destination.isWishlisted
                database.destinationDao()
                    .updateWishlistStatus(destination.id, newStatus)
                btnWishlist.setImageResource(
                    if (newStatus) android.R.drawable.btn_star_big_on
                    else android.R.drawable.btn_star_big_off
                )
                val msg = if (newStatus) "Added to wishlist! ⭐"
                else "Removed from wishlist"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Add to Planner button
        view.findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener {
            showAddToPlannerDialog(destination)
        }

        // Add Journal button
        view.findViewById<Button>(R.id.btnAddJournal).setOnClickListener {
            showAddJournalDialog(destination.name)
        }
    }

    private fun showAddToPlannerDialog(destination: Destination) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_trip, null)

        val etDestination = dialogView.findViewById<EditText>(R.id.etTripDestination)
        val etStartDate = dialogView.findViewById<EditText>(R.id.etStartDate)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etEndDate)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        etDestination.setText(destination.name)
        etDestination.isEnabled = false

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etStartDate.setOnClickListener { showDatePicker(etStartDate, sdf) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate, sdf) }
        etStartDate.isFocusable = false
        etEndDate.isFocusable = false

        AlertDialog.Builder(requireContext())
            .setTitle("📅 Add to Trip Planner")
            .setView(dialogView)
            .setPositiveButton("Save Trip") { _, _ ->
                val startDate = etStartDate.text.toString().trim()
                val endDate = etEndDate.text.toString().trim()

                when {
                    startDate.isEmpty() -> Toast.makeText(requireContext(),
                        "Please select a start date!", Toast.LENGTH_SHORT).show()
                    endDate.isEmpty() -> Toast.makeText(requireContext(),
                        "Please select an end date!", Toast.LENGTH_SHORT).show()
                    else -> {
                        val start = sdf.parse(startDate)
                        val end = sdf.parse(endDate)
                        if (end != null && start != null && end.before(start)) {
                            Toast.makeText(requireContext(),
                                "End date must be after start date!",
                                Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        val tripPlan = TripPlan(
                            destinationName = destination.name,
                            startDate = startDate,
                            endDate = endDate,
                            budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0,
                            notes = etNotes.text.toString().trim()
                        )
                        viewLifecycleOwner.lifecycleScope.launch {
                            database.tripPlanDao().insertTripPlan(tripPlan)
                            Toast.makeText(requireContext(),
                                "Added to Trip Planner! 📅",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddJournalDialog(destinationName: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_journal, null)

        val etDestination = dialogView.findViewById<EditText>(R.id.etJournalDestination)
        val etTitle = dialogView.findViewById<EditText>(R.id.etJournalTitle)
        val etContent = dialogView.findViewById<EditText>(R.id.etJournalContent)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        etDestination.setText(destinationName)
        etDestination.isEnabled = false

        AlertDialog.Builder(requireContext())
            .setTitle("📔 Write Journal Entry")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Please enter a title!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val entry = JournalEntry(
                    destinationName = destinationName,
                    title = title,
                    content = content,
                    rating = ratingBar.rating,
                    date = sdf.format(Date())
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    database.journalDao().insertEntry(entry)
                    Toast.makeText(requireContext(),
                        "Journal entry saved! 📔",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(
        editText: EditText,
        sdf: SimpleDateFormat
    ) {
        val calendar = java.util.Calendar.getInstance()
        val currentText = editText.text.toString()
        if (currentText.isNotEmpty()) {
            try {
                val date = sdf.parse(currentText)
                if (date != null) calendar.time = date
            } catch (e: Exception) { }
        }
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                editText.setText("$day/${month + 1}/$year")
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }
}